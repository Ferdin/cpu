package main.java.com.ferdin.nes.apu;

public class NoiseChannel {
    public int lengthCounter = 0;
    private boolean enabled = false;

    public void tickTimer()         {}
    public void tickEnvelope()      {}
    public void tickLengthCounter() {}
    public void writeControl(int data) {}
    public void writePeriod(int data)  {}
    public void writeLength(int data)  {}
    public void setEnabled(boolean e)  { enabled = e; if (!e) lengthCounter = 0; }
    public float getSample()           { return 0; }    
}
