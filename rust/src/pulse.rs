use std::time::Instant;

pub struct PulseState {
    pub start_time: Option<Instant>,
}

impl PulseState {
    pub fn new() -> Self {
        Self { start_time: None }
    }

    pub fn trigger(&mut self) {
        self.start_time = Some(Instant::now());
    }

    pub fn get_air_byte(&mut self, is_auto: bool) -> u8 {
        let mut byte: u8 = 0b00100000;

        if let Some(start) = self.start_time {
            let elapsed = start.elapsed().as_millis();

            if elapsed < 10 {
                byte |= 1 << 0;
            } else if elapsed < 20 {
                byte |= 1 << 1;
            } else if elapsed < 30 {
                byte |= 1 << 2;
            } else if elapsed < 40 {
                byte |= 1 << 3;
            } else if elapsed < 50 {
                byte |= 1 << 4;
            } else {
                if is_auto {
                    self.start_time = Some(Instant::now());
                } else {
                    self.start_time = None;
                }
            }
        }

        byte
    }
}