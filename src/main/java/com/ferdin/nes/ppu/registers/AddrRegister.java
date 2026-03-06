package main.java.com.ferdin.nes.ppu.registers;

public class AddrRegister {

    // high byte = [0], low byte = [1]
    private int[] value;
    private boolean hiPtr;

    public AddrRegister() {
        this.value = new int[]{0, 0}; // high byte first, lo byte second
        this.hiPtr = true;
    }

    private void set(int data) {
        this.value[0] = (data >> 8) & 0xFF;  // high byte
        this.value[1] = data & 0xFF;          // low byte
    }

    public void update(int data) {
        if (hiPtr) {
            value[0] = data & 0xFF;
        } else {
            value[1] = data & 0xFF;
        }

        if (get() > 0x3FFF) { // mirror down addr above 0x3fff
            set(get() & 0b11111111111111);
        }

        hiPtr = !hiPtr;
    }

    public void increment(int inc) {
        int lo = value[1];
        value[1] = (value[1] + inc) & 0xFF; // wrapping add, masked to 8 bits

        if (lo > value[1]) { // overflow occurred
            value[0] = (value[0] + 1) & 0xFF; // wrapping add, masked to 8 bits
        }

        if (get() > 0x3FFF) { // mirror down addr above 0x3fff
            set(get() & 0b11111111111111);
        }
    }

    public void resetLatch() {
        this.hiPtr = true;
    }

    public int get() {
        return ((value[0] & 0xFF) << 8) | (value[1] & 0xFF);
    }
}