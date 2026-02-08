use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jint, jbyteArray};
use std::net::{UdpSocket, SocketAddr};
use std::sync::atomic::{AtomicU32, AtomicU64, Ordering};
use std::sync::{Arc, Mutex, RwLock};
use std::thread;
use std::time::{Duration, Instant};
use once_cell::sync::Lazy;

static STATE_VALUE: AtomicU32 = AtomicU32::new(0);
static PROTOCOL_TYPE: AtomicU32 = AtomicU32::new(0);
static INTERVAL_NS: AtomicU64 = AtomicU64::new(2_000_000);

static TARGET_ADDR: RwLock<Option<SocketAddr>> = RwLock::new(None);
static SOCKET_HOLDER: RwLock<Option<UdpSocket>> = RwLock::new(None);

struct NetData {
    packet_type: AtomicU32,
    button_mask: AtomicU32,
    air_byte: AtomicU32,
    slider_mask: AtomicU32,
    handshake_storage: AtomicU32,
    card_bcd: Mutex<[u8; 10]>,
    sync_deadline: Mutex<Option<Instant>>,
    sync_target_state: AtomicU32,
    air_mode: AtomicU32,
    mickey: AtomicU32,
}

static DATA_POOL: Lazy<Arc<NetData>> = Lazy::new(|| Arc::new(NetData {
    packet_type: AtomicU32::new(16),
    button_mask: AtomicU32::new(0),
    air_byte: AtomicU32::new(0),
    slider_mask: AtomicU32::new(0),
    handshake_storage: AtomicU32::new(0),
    card_bcd: Mutex::new([0u8; 10]),
    sync_deadline: Mutex::new(None),
    sync_target_state: AtomicU32::new(0),
    air_mode: AtomicU32::new(1),
    mickey: AtomicU32::new(0),
}));

fn start_permanent_loop() {
    thread::Builder::new().name("RustNetEngine".into()).spawn(move || {
        #[cfg(target_os = "android")]
        unsafe {
            let tid = libc::gettid();
            let param = libc::sched_param { sched_priority: 99 };
            libc::sched_setscheduler(tid, libc::SCHED_FIFO, &param);
        }

        let mut last_tick = Instant::now();
        let mut mickey_frame: u32 = 0;
        let mut recv_buf = [0u8; 2];

        loop {
            let current_state = STATE_VALUE.load(Ordering::Acquire);
            let target_addr = TARGET_ADDR.read().unwrap().clone();
            let socket_opt = SOCKET_HOLDER.read().unwrap();

            if let (Some(addr), Some(socket)) = (target_addr, socket_opt.as_ref()) {
                while let Ok((size, _)) = socket.recv_from(&mut recv_buf) {
                    if size == 2 {
                        let header = recv_buf[0];
                        let payload = recv_buf[1];

                        let is_from_server = (header >> 6) & 1 == 1;
                        let is_handshake = (header & 0x30) == 0;

                        if is_from_server && is_handshake && current_state == 2 {
                            let server_target_confirm = (payload >> 4) & 1;
                            let my_target = DATA_POOL.sync_target_state.load(Ordering::Relaxed);
                            if (server_target_confirm as u32) == my_target {
                                STATE_VALUE.store(my_target, Ordering::SeqCst);
                                if let Ok(mut guard) = DATA_POOL.sync_deadline.lock() {
                                    *guard = None;
                                }
                            }
                        }
                    }
                }

                if current_state == 2 {
                    if let Ok(mut guard) = DATA_POOL.sync_deadline.lock() {
                        if let Some(deadline) = *guard {
                            if Instant::now() > deadline {
                                let target = DATA_POOL.sync_target_state.load(Ordering::Relaxed);
                                let fallback = if target == 1 { 0 } else { 1 };
                                STATE_VALUE.store(fallback, Ordering::SeqCst);
                                *guard = None;
                            }
                        }
                    }
                }

                let interval = Duration::from_nanos(INTERVAL_NS.load(Ordering::Acquire));
                if last_tick.elapsed() >= interval {
                    last_tick = Instant::now();
                    let p_type = if current_state == 2 {
                    0
                    } else if current_state == 1 {
                        DATA_POOL.packet_type.load(Ordering::Relaxed)
                    } else {
                        99
                    };

                    if p_type == 99 { continue; }

                    let mut buffer = [0u8; 11];
                    let protocol_bit = if PROTOCOL_TYPE.load(Ordering::Relaxed) == 1 { 0x80 } else { 0x00 };
                    let type_bits = match p_type {
                        0 => 0b00, 16 => 0b01, 32 => 0b10, 48 => 0b11, _ => 0b01,
                    };
                    buffer[0] = protocol_bit | (0 << 6) | (type_bits << 4);

                    let packet_len = match p_type {
                        0 => {
                            let target = DATA_POOL.sync_target_state.load(Ordering::Relaxed);
                            let mut p_byte = 0u8;
                            if target == 0 { p_byte |= 1 << 7; }
                            if target == 1 {
                                p_byte |= 1 << 5;
                                p_byte |= 1 << 4;
                            } else {
                            }
                            buffer[1] = p_byte;
                            2
                        },
                        16 => {
                            buffer[1] = DATA_POOL.button_mask.load(Ordering::Relaxed) as u8;
                            2
                        },
                        32 => {
                            let air_mode = DATA_POOL.air_mode.load(Ordering::Relaxed);
                            let is_mickey = DATA_POOL.mickey.load(Ordering::Relaxed) == 1;

                            let final_air_byte = if air_mode == 3 && is_mickey {
                                let mut byte = 0b00100001u8;
                                let slow_frame = mickey_frame / 20;
                                let running_bit = (slow_frame % 4) + 1;
                                byte |= 1 << running_bit;
                                mickey_frame = mickey_frame.wrapping_add(1);
                                byte
                            } else if air_mode == 1 {
                                DATA_POOL.air_byte.load(Ordering::Relaxed) as u8
                            } else {
                                mickey_frame = 0;
                                0
                            };

                            buffer[1] = final_air_byte;
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
                        let _ = socket.send_to(&buffer[..packet_len], &addr);
                    }
                }
            } else {
                thread::sleep(Duration::from_millis(50));
            }
            std::hint::spin_loop();
        }
    }).expect("Failed to spawn RustNetEngine");
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeGetState(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    STATE_VALUE.load(Ordering::Acquire) as jint
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeToggleClient(
    _env: JNIEnv,
    _class: JClass,
) {
    let current = STATE_VALUE.load(Ordering::Acquire);
    if current == 2 { return; }
    let next = if current == 1 { 0 } else { 1 };
    STATE_VALUE.store(next, Ordering::SeqCst);
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeToggleSync(
    _env: JNIEnv,
    _class: JClass,
) {
    let current = STATE_VALUE.load(Ordering::Acquire);
    if current == 2 { return; }

    let target = if current == 1 { 0 } else { 1 };
    DATA_POOL.sync_target_state.store(target, Ordering::Relaxed);

    if let Ok(mut guard) = DATA_POOL.sync_deadline.lock() {
        *guard = Some(Instant::now() + Duration::from_millis(500));
    }

    STATE_VALUE.store(2, Ordering::SeqCst);
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeInit(
    _env: JNIEnv,
    _class: JClass,
) {
    static ONCE: std::sync::Once = std::sync::Once::new();
    ONCE.call_once(|| {
        start_permanent_loop();
    });
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeUpdateConfig(
    mut env: JNIEnv,
    _class: JClass,
    ip: JString,
    port: jint,
    protocol_type: jint,
) {
    let ip_str: String = env.get_string(&ip).expect("Invalid IP").into();
    let addr_str = format!("{}:{}", ip_str, port);

    if let Ok(addr) = addr_str.parse::<SocketAddr>() {
        if let Ok(mut addr_guard) = TARGET_ADDR.write() {
            *addr_guard = Some(addr);
        }
        PROTOCOL_TYPE.store(protocol_type as u32, Ordering::SeqCst);
        if let Ok(socket) = UdpSocket::bind("0.0.0.0:0") {
            let _ = socket.set_nonblocking(true);
            if let Ok(mut sock_guard) = SOCKET_HOLDER.write() {
                *sock_guard = Some(socket);
            }
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeMickeyButton(
    _env: JNIEnv,
    _class: JClass,
    enabled: jint,
) {
    DATA_POOL.mickey.store(enabled as u32, Ordering::Relaxed);
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeUpdateState(
    env: JNIEnv,
    _class: JClass,
    packet_type: jint,
    button_mask: jint,
    air_byte: jint,
    slider_mask: jint,
    handshake_payload: jint,
    card_bcd: jbyteArray,
    air_mode: jint,
) {
    let data = &*DATA_POOL;
    data.packet_type.store(packet_type as u32, Ordering::Relaxed);
    data.button_mask.store(button_mask as u32, Ordering::Relaxed);
    data.air_byte.store(air_byte as u32, Ordering::Relaxed);
    data.slider_mask.store(slider_mask as u32, Ordering::Relaxed);
    data.air_mode.store(air_mode as u32, Ordering::Relaxed);
    data.handshake_storage.store(handshake_payload as u32, Ordering::Relaxed);

    if packet_type == 48 && !card_bcd.is_null() {
        let array_obj = unsafe { jni::objects::JByteArray::from_raw(card_bcd) };
        if let Ok(bytes) = env.convert_byte_array(&array_obj) {
            if bytes.len() == 10 {
                if let Ok(mut guard) = data.card_bcd.lock() {
                    guard.copy_from_slice(&bytes);
                }
            }
        }
    }
}