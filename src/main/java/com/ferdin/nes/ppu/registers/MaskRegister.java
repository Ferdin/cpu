package main.java.com.ferdin.nes.ppu.registers;

import java.util.ArrayList;
import java.util.List;

public class MaskRegister {

    // Bit flag constants
    // 7  bit  0
    // ---- ----
    // BGRs bMmG
    // |||| ||||
    // |||| |||+- Greyscale (0: normal color, 1: produce a greyscale display)
    // |||| ||+-- 1: Show background in leftmost 8 pixels of screen, 0: Hide
    // |||| |+--- 1: Show sprites in leftmost 8 pixels of screen, 0: Hide
    // |||| +---- 1: Show background
    // |||+------ 1: Show sprites
    // ||+------- Emphasize red
    // |+-------- Emphasize green
    // +--------- Emphasize blue
    public static final int GREYSCALE                = 0b00000001;
    public static final int LEFTMOST_8PXL_BACKGROUND = 0b00000010;
    public static final int LEFTMOST_8PXL_SPRITE     = 0b00000100;
    public static final int SHOW_BACKGROUND          = 0b00001000;
    public static final int SHOW_SPRITES             = 0b00010000;
    public static final int EMPHASISE_RED            = 0b00100000;
    public static final int EMPHASISE_GREEN          = 0b01000000;
    public static final int EMPHASISE_BLUE           = 0b10000000;

    public enum Color {
        RED,
        GREEN,
        BLUE
    }

    private int bits;

    public MaskRegister() {
        this.bits = 0b00000000;
    }

    private boolean contains(int flag) {
        return (this.bits & flag) == flag;
    }

    public boolean isGrayscale() {
        return contains(GREYSCALE);
    }

    public boolean leftmost8pxlBackground() {
        return contains(LEFTMOST_8PXL_BACKGROUND);
    }

    public boolean leftmost8pxlSprite() {
        return contains(LEFTMOST_8PXL_SPRITE);
    }

    public boolean showBackground() {
        return contains(SHOW_BACKGROUND);
    }

    public boolean showSprites() {
        return contains(SHOW_SPRITES);
    }

    public List<Color> emphasise() {
        List<Color> result = new ArrayList<>();

        if (contains(EMPHASISE_RED)) {
            result.add(Color.RED);
        }
        if (contains(EMPHASISE_BLUE)) {
            result.add(Color.BLUE);
        }
        if (contains(EMPHASISE_GREEN)) {
            result.add(Color.GREEN);
        }

        return result;
    }

    public void update(int data) {
        this.bits = data & 0xFF; // Mask to 8 bits to simulate u8 behavior
    }

    public int getBits() {
        return this.bits;
    }
}