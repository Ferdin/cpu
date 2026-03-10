package test.java.com.ferdin.nes;
import main.java.com.ferdin.nes.joypad.*;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JoypadTest {
    @Test
    void testStrobeMode() {
        Joypad joypad = new Joypad();
        joypad.write(1);
        joypad.setButtonPressed(Joypad.BUTTON_A, true);
        for (int i = 0; i < 10; i++) {
            assertEquals(1, joypad.read());
        }
    }

    @Test
    void testStrobeModeOnOff() {
        Joypad joypad = new Joypad();
        joypad.write(0);
        joypad.setButtonPressed(Joypad.RIGHT,    true);
        joypad.setButtonPressed(Joypad.LEFT,     true);
        joypad.setButtonPressed(Joypad.SELECT,   true);
        joypad.setButtonPressed(Joypad.BUTTON_B, true);

        for (int i = 0; i <= 1; i++) {
            assertEquals(0, joypad.read()); // A      - not pressed
            assertEquals(1, joypad.read()); // B      - pressed
            assertEquals(1, joypad.read()); // Select - pressed
            assertEquals(0, joypad.read()); // Start  - not pressed
            assertEquals(0, joypad.read()); // Up     - not pressed
            assertEquals(0, joypad.read()); // Down   - not pressed
            assertEquals(1, joypad.read()); // Left   - pressed
            assertEquals(1, joypad.read()); // Right  - pressed

            for (int x = 0; x < 10; x++) {
                assertEquals(1, joypad.read()); // past index 7, always 1
            }

            joypad.write(1); // reset index to 0
            joypad.write(0); // release strobe, ready to read again
        }
    }
}
