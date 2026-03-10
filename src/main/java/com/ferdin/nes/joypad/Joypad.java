package main.java.com.ferdin.nes.joypad;

public class Joypad {

    public static final int BUTTON_A      = 0;
    public static final int BUTTON_B      = 1;
    public static final int BUTTON_SELECT = 2;
    public static final int BUTTON_START  = 3;
    public static final int BUTTON_UP     = 4;
    public static final int BUTTON_DOWN   = 5;
    public static final int BUTTON_LEFT   = 6;
    public static final int BUTTON_RIGHT  = 7;

    private boolean strobe;
    private int buttonIndex;
    private int buttonStatus;

    public Joypad() {
        this.strobe = false;
        this.buttonIndex = 0;
        this.buttonStatus = 0;
    }

    // CPU writes to $4016
    public void write(int data) {
        strobe = (data & 1) == 1;

        if (strobe) {
            buttonIndex = 0;
        }
    }

    // CPU reads from $4016
    public int read() {

        if (buttonIndex > 7) {
            return 1;
        }

        int response = (buttonStatus >> buttonIndex) & 1;

        if (!strobe && buttonIndex <= 7) {
            buttonIndex++;
        }

        return response;
    }

    // Set button pressed/released
    public void setButtonPressed(int button, boolean pressed) {

        if (pressed) {
            buttonStatus |= (1 << button);
        } else {
            buttonStatus &= ~(1 << button);
        }
    }
}
