package main.java.com.ferdin.nes.ppu.registers;

public class ScrollRegister {

    private int scrollX;
    private int scrollY;
    private boolean latch;

    public ScrollRegister() {
        this.scrollX = 0;
        this.scrollY = 0;
        this.latch = false;
    }

    public void write(int data) {
        if (!latch) {
            this.scrollX = data & 0xFF; // Mask to 8 bits to simulate u8 behavior
        } else {
            this.scrollY = data & 0xFF;
        }
        this.latch = !this.latch;
    }

    public void resetLatch() {
        this.latch = false;
    }

    public int getScrollX() {
        return scrollX;
    }

    public int getScrollY() {
        return scrollY;
    }

    public boolean isLatch() {
        return latch;
    }
}