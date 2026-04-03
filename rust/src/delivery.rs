use std::net::{UdpSocket, SocketAddr, TcpStream};
use std::sync::atomic::Ordering;
use std::sync::Mutex;
use std::time::Instant;
use std::io::{Read, Write};
use once_cell::sync::Lazy;
use crate::{DATA_POOL, STATE_VALUE, PROTOCOL_TYPE};

pub(crate) static TCP_STREAM: Lazy<Mutex<Option<TcpStream>>> = Lazy::new(|| Mutex::new(None));

static TCP_READ_BUF: Lazy<Mutex<Vec<u8>>> = Lazy::new(|| Mutex::new(Vec::with_capacity(64)));

pub fn set_tcp_stream(stream: Option<TcpStream>) {
    if let Ok(mut guard) = TCP_STREAM.lock() {
        *guard = stream;
    }
    if let Ok(mut buf) = TCP_READ_BUF.lock() {
        buf.clear();
    }
}


pub fn handle_receive(socket: &UdpSocket, current_state: u32) {
    if PROTOCOL_TYPE.load(Ordering::Relaxed) == 1 {
        handle_receive_tcp(current_state);
    } else {
        handle_receive_udp(socket, current_state);
    }
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
            }
        }
    }
}


fn handle_receive_udp(socket: &UdpSocket, current_state: u32) {
    let mut recv_buf = [0u8; 2];
    while let Ok((size, _)) = socket.recv_from(&mut recv_buf) {
        if size == 2 {
            process_server_frame(&recv_buf[..2], current_state);
        }
    }
}

fn send_packet_udp(socket: &UdpSocket, addr: &SocketAddr, current_state: u32) {
    if let Some((buf, len)) = build_packet(current_state, false) {
        let _ = socket.send_to(&buf[..len], addr);
    }
}



fn handle_receive_tcp(current_state: u32) {
    let mut tmp = [0u8; 256];
    let n = {
        let mut guard = match TCP_STREAM.lock() {
            Ok(g) => g,
            Err(_) => return,
        };
        match guard.as_mut() {
            Some(stream) => match stream.read(&mut tmp) {
                Ok(n) if n > 0 => n,
                Ok(_) => {
                                        *guard = None;
                    return;
                }
                Err(ref e) if e.kind() == std::io::ErrorKind::WouldBlock
                    || e.kind() == std::io::ErrorKind::TimedOut => return,
                Err(_) => {
                    *guard = None;
                    return;
                }
            },
            None => return,
        }
    };

        if let Ok(mut buf) = TCP_READ_BUF.lock() {
        buf.extend_from_slice(&tmp[..n]);

                loop {
            if buf.len() < 2 { break; }
            let frame_len = u16::from_le_bytes([buf[0], buf[1]]) as usize;
            if buf.len() < 2 + frame_len { break; }

            let frame = buf[2..2 + frame_len].to_vec();
            buf.drain(..2 + frame_len);

            process_server_frame(&frame, current_state);
        }
    }
}

fn send_packet_tcp(current_state: u32) {
    let Some((payload_buf, payload_len)) = build_packet(current_state, true) else { return };

        let mut framed = Vec::with_capacity(2 + payload_len);
    let len_bytes = (payload_len as u16).to_le_bytes();
    framed.extend_from_slice(&len_bytes);
    framed.extend_from_slice(&payload_buf[..payload_len]);

    let mut guard = match TCP_STREAM.lock() {
        Ok(g) => g,
        Err(_) => return,
    };
    if let Some(stream) = guard.as_mut() {
        if stream.write_all(&framed).is_err() {
                        *guard = None;
        }
    }
}


fn process_server_frame(frame: &[u8], current_state: u32) {
    if frame.len() < 2 { return; }
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
        0  => 0b00,
        16 => 0b01,
        32 => 0b10,
        48 => 0b11,
        _  => 0b01,
    };
    buffer[0] = protocol_bit | (type_bits << 4);

    let packet_len = match p_type {
        0 => {
            let target = DATA_POOL.sync_target_state.load(Ordering::Relaxed);
            buffer[1] = if target == 0 { 1 << 7 } else { (1 << 5) | (1 << 4) };
            2
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