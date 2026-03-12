package main.java.com.ferdin.nes.apu;

public class APU {
      // -------------------------
    // Channels
    // -------------------------
    private final PulseChannel  pulse1  = new PulseChannel(1);
    private final PulseChannel  pulse2  = new PulseChannel(2);
    private final TriangleChannel triangle = new TriangleChannel();
    private final NoiseChannel  noise   = new NoiseChannel();
    private final DMCChannel    dmc     = new DMCChannel();

    // -------------------------
    // Frame counter
    // -------------------------
    private int  frameCounterMode = 0; // 0 = 4-step, 1 = 5-step
    private boolean irqInhibit    = false;
    //private int  frameCounter     = 0;
    private int  cycles           = 0;

    // -------------------------
    // IRQ
    // -------------------------
    private boolean frameIrq = false;

    // -------------------------
    // Audio output
    // -------------------------
    private AudioOutput audioOutput;
    //private int totalCycles = 0;

    public APU() {
        //audioOutput = new AudioOutput();
    }

    public APU(AudioOutput audioOutput) {
        this.audioOutput = audioOutput;
    }

    // Called every CPU cycle
    public void tick() {
        cycles++;
        //totalCycles++;

        // APU ticks every 2 CPU cycles
        if (cycles % 2 == 0) {
            tickChannels();
            // Output a sample every 2 CPU cycles
            if (audioOutput != null) {
                audioOutput.receiveSample(getSample(), 2);
            }
        }

        // Frame counter ticks
        tickFrameCounter();
    }

    private void tickChannels() {
        pulse1.tickTimer();
        pulse2.tickTimer();
        triangle.tickTimer();
        noise.tickTimer();
        dmc.tickTimer();
    }

    private void tickFrameCounter() {
        // 4-step sequence (NTSC)
        // Step 1: 7457  cycles → quarter frame
        // Step 2: 14913 cycles → quarter + half frame
        // Step 3: 22371 cycles → quarter frame
        // Step 4: 29829 cycles → quarter + half frame + IRQ
        // Reset:  29830 cycles

        // 5-step sequence (NTSC)
        // Step 1: 7457  cycles → quarter frame
        // Step 2: 14913 cycles → quarter + half frame
        // Step 3: 22371 cycles → quarter frame
        // Step 4: 29829 cycles → nothing
        // Step 5: 37281 cycles → quarter + half frame
        // Reset:  37282 cycles

        if (frameCounterMode == 0) {
            switch (cycles) {
                case 7457  -> quarterFrame();
                case 14913 -> { quarterFrame(); halfFrame(); }
                case 22371 -> quarterFrame();
                case 29829 -> {
                    quarterFrame();
                    halfFrame();
                    if (!irqInhibit) frameIrq = true;
                }
                case 29830 -> cycles = 0;
            }
        } else {
            switch (cycles) {
                case 7457  -> quarterFrame();
                case 14913 -> { quarterFrame(); halfFrame(); }
                case 22371 -> quarterFrame();
                case 37281 -> { quarterFrame(); halfFrame(); }
                case 37282 -> cycles = 0;
            }
        }
    }

    private void quarterFrame() {
        pulse1.tickEnvelope();
        pulse2.tickEnvelope();
        triangle.tickLinearCounter();
        noise.tickEnvelope();
    }

    private void halfFrame() {
        pulse1.tickLengthCounterAndSweep();
        pulse2.tickLengthCounterAndSweep();
        triangle.tickLengthCounter();
        noise.tickLengthCounter();
    }

    // -------------------------
    // CPU Register Writes
    // -------------------------
    public void writeRegister(int addr, int data) {

        switch (addr) {
            // Pulse 1
            case 0x4000 -> pulse1.writeControl(data);
            case 0x4001 -> pulse1.writeSweep(data);
            case 0x4002 -> pulse1.writeTimerLow(data);
            case 0x4003 -> pulse1.writeTimerHigh(data);
            // Pulse 2
            case 0x4004 -> pulse2.writeControl(data);
            case 0x4005 -> pulse2.writeSweep(data);
            case 0x4006 -> pulse2.writeTimerLow(data);
            case 0x4007 -> pulse2.writeTimerHigh(data);
            // Triangle
            case 0x4008 -> triangle.writeControl(data);
            case 0x400A -> triangle.writeTimerLow(data);
            case 0x400B -> triangle.writeTimerHigh(data);
            // Noise
            case 0x400C -> noise.writeControl(data);
            case 0x400E -> noise.writePeriod(data);
            case 0x400F -> noise.writeLength(data);
            // DMC
            case 0x4010 -> dmc.writeControl(data);
            case 0x4011 -> dmc.writeDirectLoad(data);
            case 0x4012 -> dmc.writeSampleAddress(data);
            case 0x4013 -> dmc.writeSampleLength(data);
            // Status
            case 0x4015 -> writeStatus(data);
            // Frame counter
            case 0x4017 -> writeFrameCounter(data);
        }
    }

    public int readStatus() {
        int status = 0;
        if (pulse1.lengthCounter  > 0) status |= 0x01;
        if (pulse2.lengthCounter  > 0) status |= 0x02;
        if (triangle.lengthCounter > 0) status |= 0x04;
        if (noise.lengthCounter   > 0) status |= 0x08;
        if (dmc.bytesRemaining    > 0) status |= 0x10;
        if (frameIrq)                  status |= 0x40;
        frameIrq = false; // reading clears IRQ flag
        return status;
    }

    private void writeStatus(int data) {
        pulse1.setEnabled((data & 0x01) != 0);
        pulse2.setEnabled((data & 0x02) != 0);
        triangle.setEnabled((data & 0x04) != 0);
        noise.setEnabled((data & 0x08) != 0);
        dmc.setEnabled((data & 0x10) != 0);
    }

    private void writeFrameCounter(int data) {
        frameCounterMode = (data >> 7) & 1;
        irqInhibit       = (data & 0x40) != 0;
        if (irqInhibit) frameIrq = false;
        // Writing resets the frame counter
        cycles = 0;
        // 5-step mode immediately clocks quarter+half frame
        if (frameCounterMode == 1) {
            quarterFrame();
            halfFrame();
        }
    }

    public float getSample() {
        float sP1  = pulse1.getSample();
        float sP2  = pulse2.getSample();
        float sTri = triangle.getSample();
        float sNoi = noise.getSample();
        float sDmc = dmc.getSample();

        return mix(sP1, sP2, sTri, sNoi, sDmc);
    }

    // NES APU non-linear mixing formula
    private float mix(float p1, float p2, float tri, float noi, float dmc) {
        float pulseOut = 0;
        if (p1 + p2 > 0) {
            pulseOut = 95.88f / ((8128.0f / (p1 + p2)) + 100.0f);
        }
        float tndOut = 0;
        if (tri + noi + dmc > 0) {
            tndOut = 159.79f / (1.0f / (tri / 8227.0f + noi / 12241.0f + dmc / 22638.0f) + 100.0f);
        }
        return pulseOut + tndOut;
    }

    public boolean isFrameIrq() {
        return frameIrq;
    }

    public void cleanup() {
        if (audioOutput != null) {
            audioOutput.cleanup();
        }
    }
    public void setAudioOutput(AudioOutput audioOutput) {
        this.audioOutput = audioOutput;
    }

    public PulseChannel   getPulse1()   { return pulse1;   }
    public PulseChannel   getPulse2()   { return pulse2;   }
    public TriangleChannel getTriangle() { return triangle; }
    public NoiseChannel   getNoise()    { return noise;    }
    public DMCChannel     getDmc()      { return dmc;      }
}
