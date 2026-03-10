package main.java.com.ferdin.nes.joypad;

public class Joypad {

    public static final int BUTTON_A      = 0b00000001;
    public static final int BUTTON_B      = 0b00000010;
    public static final int SELECT        = 0b00000100;
    public static final int START         = 0b00001000;
    public static final int UP            = 0b00010000;
    public static final int DOWN          = 0b00100000;
    public static final int LEFT          = 0b01000000;
    public static final int RIGHT         = 0b10000000;

    private boolean strobe;
    private int buttonIndex;
    private int buttonStatus;

    public Joypad() {
        this.strobe = false;
        this.buttonIndex = 0;
        this.buttonStatus = 0;
    }

    public void write(int data) {
        strobe = (data & 1) == 1;
        if (strobe) {
            buttonIndex = 0;
        }
    }

    public int read() {
        if (buttonIndex > 7) {
            return 1;
        }
        int response = (buttonStatus >> buttonIndex) & 1;
        if (!strobe) {
            buttonIndex++;
        }
        return response;
    }

    public void setButtonPressed(int button, boolean pressed) {
        if (pressed) {
            buttonStatus |= button;
        } else {
            buttonStatus &= ~button;
        }
    }
}