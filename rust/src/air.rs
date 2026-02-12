use std::sync::atomic::Ordering;
use std::sync::Mutex;
use once_cell::sync::Lazy;
use crate::{DATA_POOL, LIVE_TOUCH_Y};

const DRAIN_SPEED: f32 = 120.0;
const FLICK_THRESHOLD: f32 = 60.0;
const STAGE_FRAMES: i32 = 625;
const TOTAL_ANIM_FRAMES: i32 = STAGE_FRAMES * 2;

pub struct FlickPool {
    pub pointer_id: i32,
    pub water_level: f32,
    pub last_y: f32,
    pub active_step: i32,
    pub is_up: bool, 
}

static FLICK_POOLS: Lazy<Mutex<[FlickPool; 10]>> = Lazy::new(|| {
    Mutex::new(core::array::from_fn(|_| FlickPool {
        pointer_id: -1,
        water_level: 0.0,
        last_y: 0.0,
        active_step: 0,
        is_up: true,
    }))
});

pub fn process_flick_sampling() {
    let air_mode = DATA_POOL.air_mode.load(Ordering::Relaxed);
    let is_mickey_on = DATA_POOL.mickey.load(Ordering::Relaxed) == 1;

    if air_mode != 2 { return; }

    if let Ok(mut pools) = FLICK_POOLS.lock() {
        let mut final_byte: u8 = if is_mickey_on { 0b00100001 } else { 0x00 };
        let live_y = *LIVE_TOUCH_Y.read().unwrap();
        for pool in pools.iter_mut() {
            if pool.active_step > 0 {
                let stage = (TOTAL_ANIM_FRAMES - pool.active_step) / STAGE_FRAMES;
                if pool.is_up {
                    let bit = if stage == 0 { 2 } else { 3 };
                    final_byte |= 1 << (bit - 1);
                } else {
                    let bit = if stage == 0 { 5 } else { 4 };
                    final_byte |= 1 << (bit - 1);
                }

                pool.active_step -= 1;
                if pool.active_step == 0 && pool.pointer_id == -2 {
                    pool.pointer_id = -1;
                    pool.water_level = 0.0;
                }
                
                continue;
            }

            if pool.pointer_id >= 0 {
                let cur_y = live_y[pool.pointer_id as usize];
                let dy = cur_y - pool.last_y;
                pool.last_y = cur_y;
                pool.water_level += dy;

                let drain = DRAIN_SPEED / 625.0;
                if pool.water_level > 0.0 {
                    pool.water_level = (pool.water_level - drain).max(0.0);
                } else if pool.water_level < 0.0 {
                    pool.water_level = (pool.water_level + drain).min(0.0);
                }
                
                if pool.water_level >= FLICK_THRESHOLD {
                    pool.active_step = TOTAL_ANIM_FRAMES;
                    pool.is_up = true; 
                    pool.water_level = 0.0;
                } else if pool.water_level <= -FLICK_THRESHOLD {
                    pool.active_step = TOTAL_ANIM_FRAMES;
                    pool.is_up = false; 
                    pool.water_level = 0.0;
                }
            }
        }

        
        DATA_POOL.air_byte.store(final_byte as u32, Ordering::Relaxed);
    }
}

pub fn get_air_packet(mickey_frame: &mut u32) -> u8 {
    let air_mode = DATA_POOL.air_mode.load(Ordering::Relaxed);
    let is_mickey_on = DATA_POOL.mickey.load(Ordering::Relaxed) == 1;

    
    if air_mode == 3 && is_mickey_on {
        let mut byte = 0b00100001u8;
        let slow_frame = *mickey_frame / 20;
        let running_bit = (slow_frame % 4) + 1;
        byte |= 1 << running_bit;
        *mickey_frame = mickey_frame.wrapping_add(1);
        byte
    } else {
        *mickey_frame = 0;
        DATA_POOL.air_byte.load(Ordering::Relaxed) as u8
    }
}

pub fn update_touch_down(pid: i32, y: f32) {
    if let Ok(mut pools) = FLICK_POOLS.lock() {
        for pool in pools.iter_mut() {
            if pool.pointer_id == pid { return; }
        }
        for pool in pools.iter_mut() {
            if pool.pointer_id == -1 {
                pool.pointer_id = pid;
                pool.water_level = 0.0;
                pool.last_y = y;
                pool.active_step = 0;
                break;
            }
        }
    }
}

pub fn update_touch_move(pid: i32, y: f32) {
    
    
    if let Ok(mut pools) = FLICK_POOLS.lock() {
        for pool in pools.iter_mut() {
            if pool.pointer_id == pid {
                let mut live_y_guard = LIVE_TOUCH_Y.write().unwrap();
                live_y_guard[pid as usize] = y;
                break;
            }
        }
    }
}

pub fn update_touch_up(pid: i32) {
    if let Ok(mut pools) = FLICK_POOLS.lock() {
        for pool in pools.iter_mut() {
            if pool.pointer_id == pid {
                if pool.active_step > 0 {
                    pool.pointer_id = -2; 
                } else {
                    pool.pointer_id = -1;
                    pool.water_level = 0.0;
                }
                break;
            }
        }
    }
}