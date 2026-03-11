package main.java.com.ferdin.nes.apu;

public class DMCChannel {

    // -------------------------
    // Timer period lookup table (NTSC)
    // Indexed by bits 3-0 of $4010
    // -------------------------
    private static final int[] DMC_PERIOD_TABLE = {
        428, 380, 340, 320, 286, 254, 226, 214,
        190, 160, 142, 128, 106,  84,  72,  54
    };

    // -------------------------
    // Enable
    // -------------------------
    private boolean enabled = false;

    // -------------------------
    // Timer
    // -------------------------
    private int timerPeriod  = DMC_PERIOD_TABLE[0];
    private int timerCounter = 0;

    // -------------------------
    // Output level (7-bit, 0-127)
    // -------------------------
    private int outputLevel = 0;

    // -------------------------
    // Sample address and length
    // -------------------------
    private int sampleAddress        = 0xC000; // starting address in ROM
    private int sampleLength         = 0;      // total bytes to play
    public  int bytesRemaining       = 0;      // bytes left to play
    private int currentAddress       = 0;      // current read address

    // -------------------------
    // Sample buffer
    // -------------------------
    private int  sampleBuffer        = 0;
    private boolean sampleBufferEmpty = true;

    // -------------------------
    // Output shift register
    // -------------------------
    private int  shiftRegister       = 0;
    private int  bitsRemaining       = 0;
    private boolean silenceFlag      = true;

    // -------------------------
    // Flags
    // -------------------------
    private boolean irqEnabled  = false;
    private boolean loopFlag    = false;
    private boolean irqPending  = false;

    // -------------------------
    // Memory reader — set by Bus so DMC can fetch samples from ROM
    // -------------------------
    public interface MemoryReader {
        int read(int address);
    }
    private MemoryReader memoryReader = null;

    public void setMemoryReader(MemoryReader reader) {
        this.memoryReader = reader;
    }

    // -------------------------
    // Register Writes
    // -------------------------

    // $4010 — IRQ enable, loop, frequency
    public void writeControl(int data) {
        irqEnabled  = (data & 0x80) != 0;
        loopFlag    = (data & 0x40) != 0;
        timerPeriod = DMC_PERIOD_TABLE[data & 0x0F];
        if (!irqEnabled) {
            irqPending = false;
        }
    }

    // $4011 — Direct load (sets output level directly)
    public void writeDirectLoad(int data) {
        outputLevel = data & 0x7F;
    }

    // $4012 — Sample address
    // Address = $C000 + (data * 64)
    public void writeSampleAddress(int data) {
        sampleAddress = 0xC000 + (data * 64);
    }

    // $4013 — Sample length
    // Length = (data * 16) + 1 bytes
    public void writeSampleLength(int data) {
        sampleLength = (data * 16) + 1;
    }

    // -------------------------
    // Ticks
    // -------------------------

    // Called every APU clock (every 2 CPU cycles)
    public void tickTimer() {
        if (timerCounter == 0) {
            timerCounter = timerPeriod;
            tickOutput();
        } else {
            timerCounter--;
        }
    }

    private void tickOutput() {
        // Clock the output shift register
        if (!silenceFlag) {
            if ((shiftRegister & 1) == 1) {
                if (outputLevel <= 125) outputLevel += 2; // increment by 2, cap at 127
            } else {
                if (outputLevel >= 2)   outputLevel -= 2; // decrement by 2, floor at 0
            }
        }
        shiftRegister >>= 1;
        bitsRemaining--;

        // When shift register is empty, load next byte
        if (bitsRemaining == 0) {
            bitsRemaining = 8;
            if (sampleBufferEmpty) {
                silenceFlag = true; // no data — stay silent
            } else {
                silenceFlag   = false;
                shiftRegister = sampleBuffer;
                sampleBufferEmpty = true;
                fetchSampleByte(); // try to fill buffer again
            }
        }
    }

    // Fetch next byte from ROM into sample buffer
    private void fetchSampleByte() {
        if (bytesRemaining == 0 || memoryReader == null) return;

        sampleBuffer      = memoryReader.read(currentAddress);
        sampleBufferEmpty = false;

        // Advance address — wraps around from $FFFF to $8000
        currentAddress++;
        if (currentAddress > 0xFFFF) {
            currentAddress = 0x8000;
        }

        bytesRemaining--;

        if (bytesRemaining == 0) {
            if (loopFlag) {
                // Restart sample
                currentAddress = sampleAddress;
                bytesRemaining = sampleLength;
            } else if (irqEnabled) {
                irqPending = true;
            }
        }
    }

    // -------------------------
    // Enable / Disable
    // -------------------------
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        irqPending   = false;

        if (!enabled) {
            bytesRemaining = 0;
        } else if (bytesRemaining == 0) {
            // Restart sample from beginning
            currentAddress = sampleAddress;
            bytesRemaining = sampleLength;
            if (sampleBufferEmpty) {
                fetchSampleByte();
            }
        }
    }

    // -------------------------
    // Sample output
    // -------------------------
    public float getSample() {
        return outputLevel / 127.0f;
    }

    public boolean isIrqPending() {
        return irqPending;
    }

    public void clearIrq() {
        irqPending = false;
    }
}