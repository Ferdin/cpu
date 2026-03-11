package main.java.com.ferdin.nes.apu;

public class TriangleChannel {
    public int lengthCounter = 0;
    private boolean enabled = false;

    public void tickTimer()          {}
    public void tickLinearCounter()  {}
    public void tickLengthCounter()  {}
    public void writeControl(int data)   {}
    public void writeTimerLow(int data)  {}
    public void writeTimerHigh(int data) {}
    public void setEnabled(boolean e)    { enabled = e; if (!e) lengthCounter = 0; }
    public float getSample()             { return 0; }    
}
