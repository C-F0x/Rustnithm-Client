mod air;
mod pulse;
mod delivery;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jint, jbyteArray};
use std::net::{UdpSocket, SocketAddr, TcpStream};
use std::sync::atomic::{AtomicU32, AtomicU64, Ordering};
use std::sync::{Arc, Mutex, RwLock};
use std::thread;
use std::time::{Duration, Instant};
use once_cell::sync::Lazy;

pub(crate) static STATE_VALUE: AtomicU32 = AtomicU32::new(0);
pub(crate) static PROTOCOL_TYPE: AtomicU32 = AtomicU32::new(0);
pub(crate) static INTERVAL_NS: AtomicU64 = AtomicU64::new(1_000_000);

pub(crate) static TARGET_ADDR: RwLock<Option<SocketAddr>> = RwLock::new(None);
pub(crate) static SOCKET_HOLDER: RwLock<Option<UdpSocket>> = RwLock::new(None);

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
    pub flick_signal: AtomicU32,
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
    flick_signal: AtomicU32::new(0),
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

loop {
    let current_state = STATE_VALUE.load(Ordering::Acquire);
    let target_addr = TARGET_ADDR.read().unwrap().clone();

    if PROTOCOL_TYPE.load(Ordering::Relaxed) == 1 {
        if let Some(addr) = target_addr {
            let is_connected = delivery::TCP_STREAM
                .lock()
                .map(|g| g.is_some())
                .unwrap_or(false);
            if !is_connected {
                connect_tcp(addr);
                thread::sleep(Duration::from_millis(500));
                continue;
            }

            if last_flick_sample.elapsed() >= flick_interval {
                last_flick_sample = Instant::now();
                air::process_flick_sampling();
            }

            let dummy_socket = SOCKET_HOLDER.read().unwrap();
            if let Some(socket) = dummy_socket.as_ref() {
                delivery::handle_receive(socket, current_state);
            }

            if current_state == 2 {
                delivery::handle_sync_timeout();
            }

            let interval = Duration::from_nanos(INTERVAL_NS.load(Ordering::Acquire));
            if last_tick.elapsed() >= interval {
                last_tick = Instant::now();
                let dummy_socket = SOCKET_HOLDER.read().unwrap();
                if let Some(socket) = dummy_socket.as_ref() {
                    delivery::send_packet(socket, &addr, current_state);
                }
            }
        } else {
            thread::sleep(Duration::from_millis(50));
        }
    } else {
        let socket_opt = SOCKET_HOLDER.read().unwrap();
        if let (Some(addr), Some(socket)) = (target_addr, socket_opt.as_ref()) {
            if last_flick_sample.elapsed() >= flick_interval {
                last_flick_sample = Instant::now();
                air::process_flick_sampling();
            }

            delivery::handle_receive(socket, current_state);

            if current_state == 2 {
                delivery::handle_sync_timeout();
            }

            let interval = Duration::from_nanos(INTERVAL_NS.load(Ordering::Acquire));
            if last_tick.elapsed() >= interval {
                last_tick = Instant::now();
                delivery::send_packet(socket, &addr, current_state);
            }
        } else {
            thread::sleep(Duration::from_millis(50));
        }
    }

    std::hint::spin_loop();
}
}).expect("Failed to spawn RustNetEngine");
}

fn connect_tcp(addr: SocketAddr) -> bool {
    match TcpStream::connect_timeout(&addr, Duration::from_secs(3)) {
        Ok(stream) => {
            let _ = stream.set_read_timeout(Some(Duration::from_millis(100)));
            delivery::set_tcp_stream(Some(stream));
            true
        }
        Err(_) => {
            delivery::set_tcp_stream(None);
            false
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeTouchDown(
    _env: JNIEnv, _class: JClass, pid: jint, y: jint,
) {
    air::update_touch_down(pid, y as f32);
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeTouchUp(
    _env: JNIEnv, _class: JClass, pid: jint,
) {
    air::update_touch_up(pid);
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeUpdateFlickCoords(
    _env: JNIEnv, _class: JClass, pid: jint, y: jint,
) {
    air::update_touch_move(pid, y as f32);
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeTriggerFlick(
    _env: JNIEnv, _class: JClass,
) {
    DATA_POOL.flick_signal.store(1, Ordering::SeqCst);
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeGetState(
    _env: JNIEnv, _class: JClass,
) -> jint {
    STATE_VALUE.load(Ordering::Acquire) as jint
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeToggleClient(
    _env: JNIEnv, _class: JClass,
) {
    let current = STATE_VALUE.load(Ordering::Acquire);
    if current != 2 {
        STATE_VALUE.store(if current == 1 { 0 } else { 1 }, Ordering::SeqCst);
    }
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeToggleSync(
    _env: JNIEnv, _class: JClass,
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
    _env: JNIEnv, _class: JClass, freq: jint,
) {
    let ns = 1_000_000_000 / (freq.max(1) as u64);
    INTERVAL_NS.store(ns, Ordering::SeqCst);

    static ONCE: std::sync::Once = std::sync::Once::new();
    ONCE.call_once(start_permanent_loop);
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeUpdateConfig(
    mut env: JNIEnv, _class: JClass,
    ip: JString, port: jint, protocol_type: jint,
) {
    let ip_str: String = match env.get_string(&ip) {
        Ok(s) => s.into(),
        Err(_) => return,
    };
    let addr: SocketAddr = match format!("{}:{}", ip_str, port).parse() {
        Ok(a) => a,
        Err(_) => return,
    };

        if let Ok(mut guard) = TARGET_ADDR.write() { *guard = Some(addr); }
    PROTOCOL_TYPE.store(protocol_type as u32, Ordering::SeqCst);

    match protocol_type {
        1 => {
                                                delivery::set_tcp_stream(None);
            if let Ok(dummy) = UdpSocket::bind("0.0.0.0:0") {
                let _ = dummy.set_nonblocking(true);
                if let Ok(mut guard) = SOCKET_HOLDER.write() { *guard = Some(dummy); }
            }
                        thread::spawn(move || {
                connect_tcp(addr);
            });
        }
        _ => {
                        delivery::set_tcp_stream(None);
            if let Ok(socket) = UdpSocket::bind("0.0.0.0:0") {
                let _ = socket.set_nonblocking(true);
                if let Ok(mut guard) = SOCKET_HOLDER.write() { *guard = Some(socket); }
            }
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeMickeyButton(
    _env: JNIEnv, _class: JClass, enabled: jint,
) {
    DATA_POOL.mickey.store(enabled as u32, Ordering::Relaxed);
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeUpdateState(
    env: JNIEnv, _class: JClass,
    packet_type: jint, button_mask: jint, _air_byte: jint,
    slider_mask: jint, handshake_payload: jint,
    card_bcd: jbyteArray, air_mode: jint,
) {
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
                if let Ok(mut guard) = data.card_bcd.lock() {
                    guard.copy_from_slice(&bytes);
                }
            }
        }
    }
}