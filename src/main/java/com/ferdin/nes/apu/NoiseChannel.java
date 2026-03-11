package main.java.com.ferdin.nes.apu;

public class NoiseChannel {

    // -------------------------
    // Timer period lookup table (NTSC)
    // Indexed by bits 3-0 of $400E
    // -------------------------
    private static final int[] NOISE_PERIOD_TABLE = {
        4, 8, 16, 32, 64, 96, 128, 160,
        202, 254, 380, 508, 762, 1016, 2034, 4068
    };

    // -------------------------
    // Length counter lookup — same as Pulse and Triangle
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
    private int timerPeriod  = NOISE_PERIOD_TABLE[0];
    private int timerCounter = 0;

    // -------------------------
    // LFSR — 15-bit shift register
    // Initialized to 1 on power up
    // -------------------------
    private int shiftRegister = 1;
    private boolean modeFlag  = false; // false = bit 1 feedback, true = bit 6 feedback

    // -------------------------
    // Length counter
    // -------------------------
    public int lengthCounter     = 0;
    private boolean lengthCounterHalt = false;

    // -------------------------
    // Envelope
    // -------------------------
    private boolean envelopeEnabled = true;
    private boolean envelopeLoop    = false;
    private boolean envelopeStart   = false;
    private int     envelopePeriod  = 0;
    private int     envelopeCounter = 0;
    private int     envelopeVolume  = 0;
    private int     constantVolume  = 0;

    // -------------------------
    // Register Writes
    // -------------------------

    // $400C — Envelope + length counter halt
    public void writeControl(int data) {
        lengthCounterHalt = (data & 0x20) != 0;
        envelopeLoop      = (data & 0x20) != 0;
        envelopeEnabled   = (data & 0x10) == 0;
        envelopePeriod    = data & 0x0F;
        constantVolume    = data & 0x0F;
        envelopeStart     = true;
    }

    // $400E — Mode flag + period index
    public void writePeriod(int data) {
        modeFlag    = (data & 0x80) != 0;
        timerPeriod = NOISE_PERIOD_TABLE[data & 0x0F];
    }

    // $400F — Length counter load
    public void writeLength(int data) {
        if (enabled) {
            lengthCounter = LENGTH_TABLE[(data >> 3) & 0x1F];
        }
        envelopeStart = true;
    }

    // -------------------------
    // Ticks
    // -------------------------

    // Called every APU clock (every 2 CPU cycles)
    public void tickTimer() {
        if (timerCounter == 0) {
            timerCounter = timerPeriod;
            clockShiftRegister();
        } else {
            timerCounter--;
        }
    }

    // LFSR clock — generates the next pseudo-random bit
    private void clockShiftRegister() {
        // Feedback bit is XOR of bit 0 and either bit 1 (mode=false) or bit 6 (mode=true)
        int bit0     = shiftRegister & 1;
        int feedback = modeFlag
            ? bit0 ^ ((shiftRegister >> 6) & 1)  // mode 1: bit 6
            : bit0 ^ ((shiftRegister >> 1) & 1);  // mode 0: bit 1

        // Shift right and put feedback into bit 14
        shiftRegister = (shiftRegister >> 1) | (feedback << 14);
    }

    // Called on quarter frame (240 Hz)
    public void tickEnvelope() {
        if (envelopeStart) {
            envelopeStart   = false;
            envelopeVolume  = 15;
            envelopeCounter = envelopePeriod;
        } else {
            if (envelopeCounter > 0) {
                envelopeCounter--;
            } else {
                envelopeCounter = envelopePeriod;
                if (envelopeVolume > 0) {
                    envelopeVolume--;
                } else if (envelopeLoop) {
                    envelopeVolume = 15;
                }
            }
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
        if (!enabled)           return 0;
        if (lengthCounter == 0) return 0;
        // Bit 0 of shift register — 0 means silence
        if ((shiftRegister & 1) == 0) return 0;

        int volume = envelopeEnabled ? envelopeVolume : constantVolume;
        return volume / 15.0f;
    }
}