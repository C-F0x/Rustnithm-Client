use std::io::{Read, Write};
use std::net::{SocketAddr, TcpStream, UdpSocket};
use std::os::unix::io::{AsRawFd, RawFd};
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Mutex;
use std::thread;
use std::time::{Duration, Instant};
use once_cell::sync::Lazy;
use crate::{DATA_POOL, PROTOCOL_TYPE, STATE_VALUE, PENDING_TOGGLE, TARGET_ADDR, SOCKET_HOLDER};

pub(crate) static TCP_STREAM: Lazy<Mutex<Option<TcpStream>>> = Lazy::new(|| Mutex::new(None));

static TCP_READ_BUF: Lazy<Mutex<Vec<u8>>> = Lazy::new(|| Mutex::new(Vec::with_capacity(64)));

/// Frames queued for the non-blocking TCP socket. A non-blocking `write` may
/// accept only part of a frame; leftover bytes stay here until flushed, so the
/// server never sees a torn (misaligned) frame stream.
static TCP_SEND_PENDING: Lazy<Mutex<Vec<u8>>> = Lazy::new(|| Mutex::new(Vec::new()));

/// Upper bound for the send queue. If a peer stops reading, we drop the
/// connection (and let the engine loop reconnect) instead of growing forever.
const MAX_TCP_SEND_PENDING: usize = 64 * 1024;

/// Guards against spawning more than one RX thread at a time.
///
/// Who resets this flag:
/// - the RX thread itself on self-initiated teardown (EOF / socket error),
///   BEFORE clearing `TCP_STREAM`, so a reconnect can spawn immediately;
/// - whoever clears the stream externally (`set_tcp_stream(None)`, or the
///   sender's write-error path), synchronously, so the flag can never stay
///   stuck at `true` while a sleeping RX thread is on its way out.
///
/// The RX thread's other exit paths (stream already `None`, poisoned mutex) do
/// NOT reset the flag: the external clearer already did, and resetting again
/// could wipe a freshly spawned successor's flag.
static TCP_RX_SPAWNED: AtomicBool = AtomicBool::new(false);

/// Lock order everywhere: TCP_SEND_PENDING -> TCP_STREAM -> TCP_READ_BUF.
/// `send_packet_tcp` and `set_tcp_stream` both follow it; `tcp_rx_loop` only
/// touches TCP_STREAM then TCP_READ_BUF, so no cycle is possible.
pub fn set_tcp_stream(stream: Option<TcpStream>) {
    let should_spawn_rx = stream.is_some();

    // External teardown: reset the spawn flag synchronously so a reconnect can
    // start a fresh RX thread right away, without waiting for the old (sleeping)
    // RX thread to notice the cleared stream.
    if !should_spawn_rx {
        TCP_RX_SPAWNED.store(false, Ordering::SeqCst);
    }

    if let Ok(mut pending) = TCP_SEND_PENDING.lock() {
        pending.clear();
    }
    if let Ok(mut guard) = TCP_STREAM.lock() {
        *guard = stream;
    }
    if let Ok(mut buf) = TCP_READ_BUF.lock() {
        buf.clear();
    }
    if should_spawn_rx {
        spawn_tcp_rx();
    }
}

fn spawn_tcp_rx() {
    if TCP_RX_SPAWNED.swap(true, Ordering::SeqCst) {
        return;
    }
    if thread::Builder::new()
        .name("RustTcpRx".into())
        .spawn(tcp_rx_loop)
        .is_err()
    {
        TCP_RX_SPAWNED.store(false, Ordering::SeqCst);
    }
}

enum RxAction {
    Data(usize),
    Eof,
    Idle,
    Error,
}

/// Dedicated receiver: keeps TCP reads out of the send loop so the configured
/// send frequency is never throttled by a blocking recv. Frames are parsed
/// (2-byte LE length prefix) and fed straight into `process_server_frame`,
/// which is safe to call from this thread (atomics + mutex only).
#[allow(unused_assignments)] // `my_fd` initial -1 is overwritten before first use, kept as a defensive sentinel
fn tcp_rx_loop() {
    let mut tmp = [0u8; 256];
    // Raw fd of the stream this thread is currently reading. Used on teardown
    // to make sure we only clear the connection we actually read, never a fresh
    // one installed by a reconnect while this thread was descheduled.
    let mut my_fd: RawFd = -1;

    loop {
        let action = {
            let mut guard = match TCP_STREAM.lock() {
                Ok(g) => g,
                Err(_) => break, // poisoned; nothing left to do
            };
            match guard.as_mut() {
                // Stream cleared externally; the clearer already reset the spawn
                // flag, so do NOT touch it here.
                None => break,
                Some(stream) => {
                    my_fd = stream.as_raw_fd();
                    match stream.read(&mut tmp) {
                        Ok(n) if n > 0 => RxAction::Data(n),
                        Ok(_) => RxAction::Eof,
                        Err(ref e)
                            if e.kind() == std::io::ErrorKind::WouldBlock
                                || e.kind() == std::io::ErrorKind::TimedOut =>
                        {
                            RxAction::Idle
                        }
                        Err(_) => RxAction::Error,
                    }
                }
            }
        };

        match action {
            RxAction::Data(n) => {
                if let Ok(mut buf) = TCP_READ_BUF.lock() {
                    buf.extend_from_slice(&tmp[..n]);

                    loop {
                        if buf.len() < 2 {
                            break;
                        }
                        let frame_len = u16::from_le_bytes([buf[0], buf[1]]) as usize;
                        if buf.len() < 2 + frame_len {
                            break;
                        }

                        let frame = buf[2..2 + frame_len].to_vec();
                        buf.drain(..2 + frame_len);

                        process_server_frame(&frame, STATE_VALUE.load(Ordering::Acquire), None);
                    }
                }
            }
            RxAction::Eof | RxAction::Error => {
                // Self-initiated teardown, done atomically under the TCP_STREAM
                // lock: reset the spawn flag and clear the stream together, and
                // only if the stream in the slot is still the one we read (fd
                // check). If a reconnect already installed a new connection, we
                // must not touch it — and must not wipe its RX thread's flag.
                if let Ok(mut guard) = TCP_STREAM.lock() {
                    if let Some(current) = guard.as_ref() {
                        if current.as_raw_fd() == my_fd {
                            *guard = None;
                            TCP_RX_SPAWNED.store(false, Ordering::SeqCst);
                        }
                    }
                }
                break;
            }
            RxAction::Idle => {}
        }

        thread::sleep(Duration::from_millis(1));
    }
}

pub fn handle_receive(socket: &UdpSocket, current_state: u32) {
    if PROTOCOL_TYPE.load(Ordering::Relaxed) == 1 {
        // TCP: incoming frames are handled by the dedicated RX thread.
        return;
    }
    handle_receive_udp(socket, current_state);
}

pub fn send_packet(socket: &UdpSocket, addr: &SocketAddr, current_state: u32) {
    if PROTOCOL_TYPE.load(Ordering::Relaxed) == 1 {
        send_packet_tcp(current_state);
    } else {
        send_packet_udp(socket, addr, current_state);
    }
}

pub fn handle_sync_timeout() {
    if let Ok(mut guard) = DATA_POOL.sync_deadline.lock() {
        if let Some(deadline) = *guard {
            if Instant::now() > deadline {
                let target = DATA_POOL.sync_target_state.load(Ordering::Relaxed);
                STATE_VALUE.store(if target == 1 { 0 } else { 1 }, Ordering::SeqCst);
                *guard = None;
                if let Ok(mut pending) = PENDING_TOGGLE.lock() { *pending = None; }
            }
        }
    }
}

fn handle_receive_udp(socket: &UdpSocket, current_state: u32) {
    let mut recv_buf = [0u8; 2048];
    while let Ok((size, source)) = socket.recv_from(&mut recv_buf) {
        if size > 0 {
            process_server_frame(&recv_buf[..size], current_state, Some(source));
        }
    }
}

fn send_packet_udp(socket: &UdpSocket, addr: &SocketAddr, current_state: u32) {
    if let Some((buf, len)) = build_packet(current_state, false) {
        let _ = socket.send_to(&buf[..len], addr);
    }
}

fn send_packet_tcp(current_state: u32) {
    let Some((payload_buf, payload_len)) = build_packet(current_state, true) else {
        return;
    };

    let mut framed = Vec::with_capacity(2 + payload_len);
    let len_bytes = (payload_len as u16).to_le_bytes();
    framed.extend_from_slice(&len_bytes);
    framed.extend_from_slice(&payload_buf[..payload_len]);

    let mut pending = match TCP_SEND_PENDING.lock() {
        Ok(p) => p,
        Err(_) => return,
    };
    pending.extend_from_slice(&framed);

    let mut guard = match TCP_STREAM.lock() {
        Ok(g) => g,
        Err(_) => return,
    };
    if guard.is_none() {
        pending.clear();
        return;
    }

    let stream = guard.as_mut().expect("checked is_some above");

    loop {
        match stream.write(&pending) {
            Ok(0) => break, // cannot make progress right now; retry next tick
            Ok(n) => {
                pending.drain(..n);
                if pending.is_empty() {
                    break;
                }
            }
            // Send buffer temporarily full: keep the rest queued for next tick.
            Err(ref e) if e.kind() == std::io::ErrorKind::WouldBlock => break,
            Err(_) => {
                TCP_RX_SPAWNED.store(false, Ordering::SeqCst);
                *guard = None;
                pending.clear();
                break;
            }
        }
    }

    // Peer stopped reading long enough that even after this flush the queue is
    // over budget: drop the connection — the engine loop reconnects and
    // set_tcp_stream clears the pending queue. Checked AFTER the flush so a
    // peer that just resumed reading is not torn down needlessly.
    if pending.len() > MAX_TCP_SEND_PENDING {
        TCP_RX_SPAWNED.store(false, Ordering::SeqCst);
        *guard = None;
        pending.clear();
    }
}

fn process_server_frame(frame: &[u8], current_state: u32, source: Option<SocketAddr>) {
    if let Some(actual) = source {
        if let Ok(expected) = TARGET_ADDR.read() {
            if let Some(expected) = *expected {
                if expected != actual { return; }
            }
        }
    }
    if frame.len() >= 2 && (frame[0] & 0x40) != 0 {
        let packet_type = (frame[0] >> 4) & 0x03;
        if packet_type == 0x01 {
            // Server slider LED frames contain 31 alternating zone/divider
            // records of [R,G,B,Brightness]. Keep accepting the older
            // 32-zone form, but reject malformed lengths before caching.
            let payload = &frame[1..];
            if payload.len() == 32 * 4 || payload.len() == 31 * 4 {
                crate::set_server_slider_led(payload);
            }
            return;
        }
        if packet_type == 0x02 || packet_type == 0x03 {
            // Tower and billboard are reserved for a later client phase.
            return;
        }
    }
    if frame.len() >= 8 && (frame[0] & 0x30) == 0 {
        let opcode = frame[1];
        let target = frame[2] != 0;
        let request_id = u32::from_le_bytes([frame[3], frame[4], frame[5], frame[6]]);
        let result = frame[7];
        if opcode == 1 {
            let current = STATE_VALUE.load(Ordering::Acquire);
            let busy = PENDING_TOGGLE.lock().ok().map(|g| g.is_some()).unwrap_or(true);
            let response = if busy {
                if let Ok(mut pending) = PENDING_TOGGLE.lock() { *pending = None; }
                if let Ok(mut deadline) = DATA_POOL.sync_deadline.lock() { *deadline = None; }
                if current == 2 { STATE_VALUE.store(if target { 0 } else { 1 }, Ordering::SeqCst); }
                4
            } else if current == target as u32 {
                2
            } else {
                STATE_VALUE.store(target as u32, Ordering::SeqCst);
                1
            };
            let packet = [0x40u8, 2, target as u8, frame[3], frame[4], frame[5], frame[6], response];
            if PROTOCOL_TYPE.load(Ordering::Relaxed) == 1 {
                if let Ok(mut guard) = TCP_STREAM.lock() { if let Some(stream) = guard.as_mut() { let len = (packet.len() as u16).to_le_bytes(); let _ = stream.write_all(&len); let _ = stream.write_all(&packet); } }
            } else if let (Ok(addr), Ok(guard)) = (TARGET_ADDR.read(), SOCKET_HOLDER.read()) {
                if let (Some(dest), Some(socket)) = (*addr, guard.as_ref()) { let _ = socket.send_to(&packet, dest); }
            }
            return;
        }
        if opcode == 2 {
            if let Ok(mut pending) = PENDING_TOGGLE.lock() {
                if let Some((id, target_state, _)) = *pending {
                    if id == request_id {
                        if result == 1 || result == 2 {
                            STATE_VALUE.store(target_state as u32, Ordering::SeqCst);
                        } else {
                            STATE_VALUE.store(if target_state { 0 } else { 1 }, Ordering::SeqCst);
                        }
                        if let Ok(mut deadline) = DATA_POOL.sync_deadline.lock() { *deadline = None; }
                        *pending = None;
                    }
                }
            }
            return;
        }
    }
    if frame.len() < 2 {
        return;
    }
    let header = frame[0];
    let payload = frame[1];

    if (header >> 6) & 1 == 1 && (header & 0x30) == 0 && current_state == 2 {
        let server_confirm = (payload >> 4) & 1;
        if (server_confirm as u32) == DATA_POOL.sync_target_state.load(Ordering::Relaxed) {
            STATE_VALUE.store(server_confirm as u32, Ordering::SeqCst);
            if let Ok(mut guard) = DATA_POOL.sync_deadline.lock() {
                *guard = None;
            }
        }
    }
}

fn build_packet(current_state: u32, is_tcp: bool) -> Option<([u8; 11], usize)> {
    let p_type = match current_state {
        2 => 0,
        1 => DATA_POOL.packet_type.load(Ordering::Relaxed),
        _ => return None,
    };

    let mut buffer = [0u8; 11];
    let protocol_bit = if is_tcp { 0x80u8 } else { 0x00u8 };
    let type_bits: u8 = match p_type {
        0 => 0b00,
        16 => 0b01,
        32 => 0b10,
        48 => 0b11,
        _ => 0b01,
    };
    buffer[0] = protocol_bit | (type_bits << 4);

    let packet_len = match p_type {
        0 => {
            let target = DATA_POOL.sync_target_state.load(Ordering::Relaxed);
            let request_id = PENDING_TOGGLE.lock().ok().and_then(|g| g.map(|v| v.0)).unwrap_or(0);
            buffer[1] = 1;
            buffer[2] = (target != 0) as u8;
            buffer[3..7].copy_from_slice(&request_id.to_le_bytes());
            buffer[7] = 0;
            8
        }
        16 => {
            buffer[1] = DATA_POOL.button_mask.load(Ordering::Relaxed) as u8;
            2
        }
        32 => {
            buffer[1] = DATA_POOL.air_byte.load(Ordering::Relaxed) as u8;
            let s_mask = DATA_POOL.slider_mask.load(Ordering::Relaxed);
            buffer[2..6].copy_from_slice(&s_mask.to_le_bytes());
            6
        }
        48 => {
            if let Ok(guard) = DATA_POOL.card_bcd.lock() {
                buffer[1..11].copy_from_slice(&*guard);
            }
            11
        }
        _ => return None,
    };

    Some((buffer, packet_len))
}
