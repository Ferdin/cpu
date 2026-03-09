package main.java.com.ferdin.nes.render;

public class Frame {

    public static final int WIDTH = 256;
    public static final int HEIGHT = 240;

    public byte[] data;

    public Frame() {
        data = new byte[WIDTH * HEIGHT * 3];
    }

    public void setPixel(int x, int y, int r, int g, int b) {

        int base = y * 3 * WIDTH + x * 3;

        if (base + 2 < data.length) {
            data[base] = (byte) r;
            data[base + 1] = (byte) g;
            data[base + 2] = (byte) b;
        }
    }
}
