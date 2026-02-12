mod air;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jint, jbyteArray};
use std::net::{UdpSocket, SocketAddr};
use std::sync::atomic::{AtomicU32, AtomicU64, Ordering};
use std::sync::{Arc, Mutex, RwLock};
use std::thread;
use std::time::{Duration, Instant};
use once_cell::sync::Lazy;

pub(crate) static STATE_VALUE: AtomicU32 = AtomicU32::new(0);
pub(crate) static PROTOCOL_TYPE: AtomicU32 = AtomicU32::new(0);
pub(crate) static INTERVAL_NS: AtomicU64 = AtomicU64::new(2_000_000);

pub(crate) static TARGET_ADDR: RwLock<Option<SocketAddr>> = RwLock::new(None);
pub(crate) static SOCKET_HOLDER: RwLock<Option<UdpSocket>> = RwLock::new(None);

pub(crate) static LIVE_TOUCH_Y: RwLock<[f32; 10]> = RwLock::new([0.0; 10]);

pub(crate) struct NetData {
    pub packet_type: AtomicU32,
    pub button_mask: AtomicU32,
    pub air_byte: AtomicU32,
    pub slider_mask: AtomicU32,
    pub handshake_storage: AtomicU32,
    pub card_bcd: Mutex<[u8; 10]>,
    pub sync_deadline: Mutex<Option<Instant>>,
    pub sync_target_state: AtomicU32,
    pub air_mode: AtomicU32,
    pub mickey: AtomicU32,
}

pub(crate) static DATA_POOL: Lazy<Arc<NetData>> = Lazy::new(|| Arc::new(NetData {
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
        let mut last_flick_sample = Instant::now();
        let flick_interval = Duration::from_micros(1600); 

        let mut mickey_frame: u32 = 0;
        let mut recv_buf = [0u8; 2];

        loop {
            let current_state = STATE_VALUE.load(Ordering::Acquire);
            let target_addr = TARGET_ADDR.read().unwrap().clone();
            let socket_opt = SOCKET_HOLDER.read().unwrap();

            if let (Some(addr), Some(socket)) = (target_addr, socket_opt.as_ref()) {

            if last_flick_sample.elapsed() >= flick_interval {
                last_flick_sample = Instant::now();
                air::process_flick_sampling();
            }
                
            while let Ok((size, _)) = socket.recv_from(&mut recv_buf) {
                if size == 2 {
                    let header = recv_buf[0];
                    let payload = recv_buf[1];
                    if (header >> 6) & 1 == 1 && (header & 0x30) == 0 && current_state == 2 {
                        let server_confirm = (payload >> 4) & 1;
                        if (server_confirm as u32) == DATA_POOL.sync_target_state.load(Ordering::Relaxed) {
                            STATE_VALUE.store(server_confirm as u32, Ordering::SeqCst);
                            if let Ok(mut guard) = DATA_POOL.sync_deadline.lock() { *guard = None; }
                        }
                    }
                }
            }

                if current_state == 2 {
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

                let interval = Duration::from_nanos(INTERVAL_NS.load(Ordering::Acquire));
                if last_tick.elapsed() >= interval {
                    last_tick = Instant::now();
                    let p_type = if current_state == 2 { 0 } else if current_state == 1 {
                        DATA_POOL.packet_type.load(Ordering::Relaxed)
                    } else { 99 };

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
                            buffer[1] = if target == 0 { 1 << 7 } else { (1 << 5) | (1 << 4) };
                            2
                        },
                        16 => {
                            buffer[1] = DATA_POOL.button_mask.load(Ordering::Relaxed) as u8;
                            2
                        },
                        32 => {
                            buffer[1] = air::get_air_packet(&mut mickey_frame);
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
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeTouchDown(_env: JNIEnv, _class: JClass, pid: jint, y: jint) {
    air::update_touch_down(pid, y as f32);
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeTouchUp(_env: JNIEnv, _class: JClass, pid: jint) {
    air::update_touch_up(pid);
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeUpdateFlickCoords(_env: JNIEnv, _class: JClass, pid: jint, y: jint) {
    air::update_touch_move(pid, y as f32);
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeGetState(_env: JNIEnv, _class: JClass) -> jint {
    STATE_VALUE.load(Ordering::Acquire) as jint
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeToggleClient(_env: JNIEnv, _class: JClass) {
    let current = STATE_VALUE.load(Ordering::Acquire);
    if current != 2 { STATE_VALUE.store(if current == 1 { 0 } else { 1 }, Ordering::SeqCst); }
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeToggleSync(_env: JNIEnv, _class: JClass) {
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
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeInit(_env: JNIEnv, _class: JClass) {
    static ONCE: std::sync::Once = std::sync::Once::new();
    ONCE.call_once(start_permanent_loop);
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeUpdateConfig(mut env: JNIEnv, _class: JClass, ip: JString, port: jint, protocol_type: jint) {
    let ip_str: String = env.get_string(&ip).expect("Invalid IP").into();
    if let Ok(addr) = format!("{}:{}", ip_str, port).parse::<SocketAddr>() {
        if let Ok(mut guard) = TARGET_ADDR.write() { *guard = Some(addr); }
        PROTOCOL_TYPE.store(protocol_type as u32, Ordering::SeqCst);
        if let Ok(socket) = UdpSocket::bind("0.0.0.0:0") {
            let _ = socket.set_nonblocking(true);
            if let Ok(mut guard) = SOCKET_HOLDER.write() { *guard = Some(socket); }
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeMickeyButton(_env: JNIEnv, _class: JClass, enabled: jint) {
    DATA_POOL.mickey.store(enabled as u32, Ordering::Relaxed);
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeUpdateState(env: JNIEnv, _class: JClass, packet_type: jint, button_mask: jint, _air_byte: jint, slider_mask: jint, handshake_payload: jint, card_bcd: jbyteArray, air_mode: jint) {
    let data = &*DATA_POOL;
    data.packet_type.store(packet_type as u32, Ordering::Relaxed);
    data.button_mask.store(button_mask as u32, Ordering::Relaxed);

    data.air_byte.store(_air_byte as u32, Ordering::Relaxed);
    data.slider_mask.store(slider_mask as u32, Ordering::Relaxed);
    data.handshake_storage.store(handshake_payload as u32, Ordering::Relaxed);
    data.air_mode.store(air_mode as u32, Ordering::Relaxed);

    if packet_type == 48 && !card_bcd.is_null() {
        let array_obj = unsafe { jni::objects::JByteArray::from_raw(card_bcd) };
        if let Ok(bytes) = env.convert_byte_array(&array_obj) {
            if bytes.len() == 10 {
                if let Ok(mut guard) = data.card_bcd.lock() { guard.copy_from_slice(&bytes); }
            }
        }
    }
}