package main.java.com.ferdin.nes.apu;

public class PulseChannel {
    public int lengthCounter = 0;
    private boolean enabled = false;
    private final int channelNum;

    public PulseChannel(int channelNum) {
        this.channelNum = channelNum;
    }

    public void tickTimer()              {}
    public void tickEnvelope()           {}
    public void tickLengthCounterAndSweep() {}
    public void writeControl(int data)   {}
    public void writeSweep(int data)     {}
    public void writeTimerLow(int data)  {}
    public void writeTimerHigh(int data) {}
    public void setEnabled(boolean e)    { enabled = e; if (!e) lengthCounter = 0; }
    public float getSample()             { return 0; }
}
