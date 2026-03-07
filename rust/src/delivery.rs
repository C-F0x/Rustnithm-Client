use std::net::{UdpSocket, SocketAddr};
use std::sync::atomic::Ordering;
use std::time::Instant;
use crate::{DATA_POOL, STATE_VALUE, PROTOCOL_TYPE};

pub fn handle_receive(socket: &UdpSocket, current_state: u32) {
    let mut recv_buf = [0u8; 2];
    while let Ok((size, _)) = socket.recv_from(&mut recv_buf) {
        if size == 2 {
            let header = recv_buf[0];
            let payload = recv_buf[1];
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

pub fn send_packet(socket: &UdpSocket, addr: &SocketAddr, current_state: u32) {
    let p_type = if current_state == 2 {
        0
    } else if current_state == 1 {
        DATA_POOL.packet_type.load(Ordering::Relaxed)
    } else {
        99
    };

    if p_type == 99 { return; }

    let mut buffer = [0u8; 11];
    let protocol_bit = if PROTOCOL_TYPE.load(Ordering::Relaxed) == 1 { 0x80 } else { 0x00 };
    let type_bits = match p_type {
        0 => 0b00,
        16 => 0b01,
        32 => 0b10,
        48 => 0b11,
        _ => 0b01,
    };
    buffer[0] = protocol_bit | (0 << 6) | (type_bits << 4);

    let packet_len = match p_type {
        0 => {
            let target = DATA_POOL.sync_target_state.load(Ordering::Relaxed);
            buffer[1] = if target == 0 { 1 << 7 } else { (1 << 5) | (1 << 4) };
            2
        },
        16 => {
            buffer[1] = DATA_POOL.button_mask.load(Ordering::Relaxed) as u8;
            2
        },
        32 => {
            buffer[1] = DATA_POOL.air_byte.load(Ordering::Relaxed) as u8;
            let s_mask = DATA_POOL.slider_mask.load(Ordering::Relaxed);
            buffer[2..6].copy_from_slice(&s_mask.to_le_bytes());
            6
        },
        48 => {
            if let Ok(guard) = DATA_POOL.card_bcd.lock() {
                buffer[1..11].copy_from_slice(&*guard);
            }
            11
        },
        _ => 0,
    };

    if packet_len > 0 {
        let _ = socket.send_to(&buffer[..packet_len], addr);
    }
}