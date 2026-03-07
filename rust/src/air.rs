use std::sync::atomic::Ordering;
use std::sync::Mutex;
use once_cell::sync::Lazy;
use crate::{DATA_POOL, pulse::PulseState};

static PULSE_CONTROLLER: Lazy<Mutex<PulseState>> = Lazy::new(|| Mutex::new(PulseState::new()));

pub fn process_flick_sampling() {
    let air_mode = DATA_POOL.air_mode.load(Ordering::Relaxed);
    let mickey_on = DATA_POOL.mickey.load(Ordering::Relaxed) == 1;
    if air_mode == 1 || !mickey_on {
        return;
    }

    if let Ok(mut pulse) = PULSE_CONTROLLER.lock() {
        if air_mode == 2 {
            if DATA_POOL.flick_signal.swap(0, Ordering::SeqCst) == 1 {
                pulse.trigger();
            }
        }

        let is_auto = air_mode == 3;
        if is_auto && pulse.start_time.is_none() {
            pulse.trigger();
        }
        let bit_result = pulse.get_air_byte(is_auto);
        DATA_POOL.air_byte.store(bit_result as u32, Ordering::Relaxed);
    }
}

pub fn update_touch_down(_pid: i32, _y: f32) {}
pub fn update_touch_move(_pid: i32, _y: f32) {}
pub fn update_touch_up(_pid: i32) {}