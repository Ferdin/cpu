package main.java.com.ferdin.nes.apu;

public class DMCChannel {
    public int bytesRemaining = 0;
    private boolean enabled = false;

    public void tickTimer()                {}
    public void writeControl(int data)     {}
    public void writeDirectLoad(int data)  {}
    public void writeSampleAddress(int data) {}
    public void writeSampleLength(int data)  {}
    public void setEnabled(boolean e)      { enabled = e; if (!e) bytesRemaining = 0; }
    public float getSample()               { return 0; }   
}
