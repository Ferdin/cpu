package main.java.com.ferdin.nes.joypad;

import java.util.concurrent.atomic.AtomicInteger;

public class Joypad {

    public static final int BUTTON_A = 0b00000001;
    public static final int BUTTON_B = 0b00000010;
    public static final int SELECT   = 0b00000100;
    public static final int START    = 0b00001000;
    public static final int UP       = 0b00010000;
    public static final int DOWN     = 0b00100000;
    public static final int LEFT     = 0b01000000;
    public static final int RIGHT    = 0b10000000;

    private volatile boolean strobe;
    private final AtomicInteger buttonIndex  = new AtomicInteger(0);
    private final AtomicInteger buttonStatus = new AtomicInteger(0);

    public Joypad() {}

    public void write(int data) {
        strobe = (data & 1) == 1;
        if (strobe) {
            buttonIndex.set(0);
        }
    }

    public int read() {
        int index = buttonIndex.get();
        if (index > 7) {
            return 1;
        }
        int response = (buttonStatus.get() >> index) & 1;
        if (!strobe) {
            buttonIndex.incrementAndGet();
        }
        return response;
    }

    public void setButtonPressed(int button, boolean pressed) {
        if (pressed) {
            buttonStatus.updateAndGet(s -> s | button);
        } else {
            buttonStatus.updateAndGet(s -> s & ~button);
        }
    }

    public int getButtonStatus() {
        return buttonStatus.get();
    }

    public int getButtonIndex() {
        return buttonIndex.get();
    }
}