package main.java.com.ferdin.nes.apu;

public class PulseChannel {

    // -------------------------
    // Duty cycle waveforms
    // Each row is one of 4 duty cycle settings (12.5%, 25%, 50%, 75%)
    // Each column is one step of the 8-step sequencer
    // -------------------------
    private static final int[][] DUTY_TABLE = {
        { 0, 1, 0, 0, 0, 0, 0, 0 }, // 12.5%
        { 0, 1, 1, 0, 0, 0, 0, 0 }, // 25%
        { 0, 1, 1, 1, 1, 0, 0, 0 }, // 50%
        { 1, 0, 0, 1, 1, 1, 1, 1 }, // 75% (inverted 25%)
    };

    // -------------------------
    // Length counter lookup table
    // Written to by bits 7-3 of $4003/$4007
    // -------------------------
    private static final int[] LENGTH_TABLE = {
        10, 254, 20,  2, 40,  4, 80,  6,
        160,   8, 60, 10, 14, 12, 26, 14,
        12,  16, 24, 18, 48, 20, 96, 22,
        192,  24, 72, 26, 16, 28, 32, 30
    };

    // -------------------------
    // Channel identity
    // -------------------------
    private final int channelNum; // 1 or 2 — affects sweep negate behavior

    // -------------------------
    // Enable
    // -------------------------
    private boolean enabled = false;

    // -------------------------
    // Timer (frequency)
    // -------------------------
    private int timerPeriod  = 0; // 11-bit reload value
    private int timerCounter = 0; // counts down to 0 then reloads

    // -------------------------
    // Sequencer
    // -------------------------
    private int dutyMode = 0; // which row of DUTY_TABLE to use
    private int dutyStep = 0; // current position in the 8-step sequence

    // -------------------------
    // Length counter
    // -------------------------
    public int lengthCounter = 0;
    private boolean lengthCounterHalt = false; // also doubles as envelope loop flag

    // -------------------------
    // Envelope
    // -------------------------
    private boolean envelopeEnabled  = true;  // true = use envelope, false = constant volume
    private boolean envelopeLoop     = false;
    private boolean envelopeStart    = false;
    private int     envelopePeriod   = 0;
    private int     envelopeCounter  = 0;
    private int     envelopeVolume   = 0;     // current envelope volume (0-15)
    private int     constantVolume   = 0;     // used when envelopeEnabled = false

    // -------------------------
    // Sweep unit
    // -------------------------
    private boolean sweepEnabled  = false;
    private int     sweepPeriod   = 0;
    private boolean sweepNegate   = false;
    private int     sweepShift    = 0;
    private boolean sweepReload   = false;
    private int     sweepCounter  = 0;

    public PulseChannel(int channelNum) {
        this.channelNum = channelNum;
    }

    // -------------------------
    // Register Writes
    // -------------------------

    // $4000 / $4004 — Duty, envelope, length counter halt
    public void writeControl(int data) {
        dutyMode           = (data >> 6) & 0b11;
        lengthCounterHalt  = (data & 0x20) != 0;
        envelopeLoop       = (data & 0x20) != 0;
        envelopeEnabled    = (data & 0x10) == 0; // bit 4 = 0 means envelope enabled
        envelopePeriod     = data & 0x0F;
        constantVolume     = data & 0x0F;
        envelopeStart      = true;
    }

    // $4001 / $4005 — Sweep unit
    public void writeSweep(int data) {
        sweepEnabled = (data & 0x80) != 0;
        sweepPeriod  = (data >> 4) & 0b111;
        sweepNegate  = (data & 0x08) != 0;
        sweepShift   = data & 0b111;
        sweepReload  = true;
    }

    // $4002 / $4006 — Timer low 8 bits
    public void writeTimerLow(int data) {
        timerPeriod = (timerPeriod & 0xFF00) | (data & 0xFF);
    }

    // $4003 / $4007 — Length counter load + timer high 3 bits
    public void writeTimerHigh(int data) {
        timerPeriod   = (timerPeriod & 0x00FF) | ((data & 0b111) << 8);
        if (enabled) {
            lengthCounter = LENGTH_TABLE[(data >> 3) & 0x1F];
        }
        dutyStep      = 0;       // reset sequencer phase
        envelopeStart = true;    // restart envelope
    }

    // -------------------------
    // Ticks — called by APU
    // -------------------------

    // Called every 2 CPU cycles (APU clock)
    public void tickTimer() {
        if (timerCounter == 0) {
            timerCounter = timerPeriod;
            dutyStep = (dutyStep + 1) % 8;
        } else {
            timerCounter--;
        }
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
                    envelopeVolume = 15; // loop back to full volume
                }
            }
        }
    }

    // Called on half frame (120 Hz)
    public void tickLengthCounterAndSweep() {
        // Length counter
        if (!lengthCounterHalt && lengthCounter > 0) {
            lengthCounter--;
        }

        // Sweep unit
        if (sweepReload) {
            sweepCounter = sweepPeriod;
            sweepReload  = false;
        } else if (sweepCounter > 0) {
            sweepCounter--;
        } else {
            sweepCounter = sweepPeriod;
            if (sweepEnabled && sweepShift > 0 && !isSweepMuting()) {
                int delta = timerPeriod >> sweepShift;
                if (sweepNegate) {
                    // Channel 1 uses one's complement, channel 2 uses two's complement
                    timerPeriod -= delta + (channelNum == 1 ? 1 : 0);
                } else {
                    timerPeriod += delta;
                }
            }
        }
    }

    // -------------------------
    // Mute conditions
    // -------------------------
    private boolean isSweepMuting() {
        // Mute if period is too low (< 8) or too high (> 0x7FF) after sweep
        int delta = timerPeriod >> sweepShift;
        int target = sweepNegate ? timerPeriod - delta : timerPeriod + delta;
        return timerPeriod < 8 || target > 0x7FF;
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
        // Mute conditions
        if (!enabled)                              return 0;
        if (lengthCounter == 0)                    return 0;
        if (DUTY_TABLE[dutyMode][dutyStep] == 0)   return 0;
        if (timerPeriod < 8)                       return 0;
        if (isSweepMuting())                       return 0;

        // Volume
        int volume = envelopeEnabled ? envelopeVolume : constantVolume;
        return volume / 15.0f;
    }
}