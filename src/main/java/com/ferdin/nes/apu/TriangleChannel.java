package main.java.com.ferdin.nes.apu;

public class TriangleChannel {

    // -------------------------
    // 32-step triangle sequence
    // Counts 15 down to 0 then 0 up to 15
    // -------------------------
    private static final int[] TRIANGLE_TABLE = {
        15, 14, 13, 12, 11, 10,  9,  8,
         7,  6,  5,  4,  3,  2,  1,  0,
         0,  1,  2,  3,  4,  5,  6,  7,
         8,  9, 10, 11, 12, 13, 14, 15
    };

    // -------------------------
    // Length counter lookup — same as Pulse
    // -------------------------
    private static final int[] LENGTH_TABLE = {
        10, 254, 20,  2, 40,  4, 80,  6,
       160,   8, 60, 10, 14, 12, 26, 14,
        12,  16, 24, 18, 48, 20, 96, 22,
       192,  24, 72, 26, 16, 28, 32, 30
    };

    // -------------------------
    // Enable
    // -------------------------
    private boolean enabled = false;

    // -------------------------
    // Timer
    // -------------------------
    private int timerPeriod  = 0;
    private int timerCounter = 0;

    // -------------------------
    // Sequencer
    // -------------------------
    private int sequencerStep = 0; // 0-31

    // -------------------------
    // Length counter
    // -------------------------
    public int lengthCounter  = 0;
    private boolean lengthCounterHalt = false;

    // -------------------------
    // Linear counter
    // The triangle has a linear counter in addition to the length counter
    // It allows finer-grained silencing
    // -------------------------
    private int     linearCounter       = 0;
    private int     linearCounterPeriod = 0;
    private boolean linearCounterReload = false;
    private boolean controlFlag         = false; // also doubles as length counter halt

    // -------------------------
    // Register Writes
    // -------------------------

    // $4008 — Linear counter control
    public void writeControl(int data) {
        controlFlag          = (data & 0x80) != 0;
        lengthCounterHalt    = (data & 0x80) != 0; // same bit
        linearCounterPeriod  = data & 0x7F;
    }

    // $400A — Timer low
    public void writeTimerLow(int data) {
        timerPeriod = (timerPeriod & 0xFF00) | (data & 0xFF);
    }

    // $400B — Timer high + length counter load
    public void writeTimerHigh(int data) {
        timerPeriod          = (timerPeriod & 0x00FF) | ((data & 0b111) << 8);
        if (enabled) {
            lengthCounter = LENGTH_TABLE[(data >> 3) & 0x1F];
        }
        linearCounterReload  = true; // trigger linear counter reload
    }

    // -------------------------
    // Ticks
    // -------------------------

    // Called every APU clock (every 2 CPU cycles)
    // Triangle timer ticks every CPU cycle (twice as fast as pulse)
    public void tickTimer() {
        if (timerCounter == 0) {
            timerCounter = timerPeriod;
            // Only advance sequencer if both counters are non-zero
            if (lengthCounter > 0 && linearCounter > 0) {
                sequencerStep = (sequencerStep + 1) % 32;
            }
        } else {
            timerCounter--;
        }
    }

    // Called on quarter frame (240 Hz)
    public void tickLinearCounter() {
        if (linearCounterReload) {
            linearCounter = linearCounterPeriod;
        } else if (linearCounter > 0) {
            linearCounter--;
        }
        // Clear reload flag unless control flag is set
        if (!controlFlag) {
            linearCounterReload = false;
        }
    }

    // Called on half frame (120 Hz)
    public void tickLengthCounter() {
        if (!lengthCounterHalt && lengthCounter > 0) {
            lengthCounter--;
        }
    }

    // -------------------------
    // Enable / Disable
    // -------------------------
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            lengthCounter = 0;
        }
    }

    // -------------------------
    // Sample output
    // -------------------------
    public float getSample() {
        if (!enabled)              return 0;
        if (lengthCounter == 0)    return 0;
        if (linearCounter == 0)    return 0;
        // Ultrasonic frequencies (timerPeriod < 2) cause popping — mute them
        if (timerPeriod < 2)       return 0;

        return TRIANGLE_TABLE[sequencerStep] / 15.0f;
    }
}