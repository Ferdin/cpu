package main.java.com.ferdin.nes.ppu.registers;

public class StatusRegister {

    // 7  bit  0
    // ---- ----
    // VSO. ....
    // |||| ||||
    // |||+-++++- Least significant bits previously written into a PPU register
    // |||        (due to register not being updated for this address)
    // ||+------- Sprite overflow. The intent was for this flag to be set
    // ||         whenever more than eight sprites appear on a scanline, but a
    // ||         hardware bug causes the actual behavior to be more complicated
    // ||         and generate false positives as well as false negatives; see
    // ||         PPU sprite evaluation. This flag is set during sprite
    // ||         evaluation and cleared at dot 1 (the second dot) of the
    // ||         pre-render line.
    // |+-------- Sprite 0 Hit.  Set when a nonzero pixel of sprite 0 overlaps
    // |          a nonzero background pixel; cleared at dot 1 of the pre-render
    // |          line.  Used for raster timing.
    // +--------- Vertical blank has started (0: not in vblank; 1: in vblank).
    //            Set at dot 1 of line 241 (the line *after* the post-render
    //            line); cleared after reading $2002 and at dot 1 of the
    //            pre-render line.
    public static final int NOTUSED         = 0b00000001;
    public static final int NOTUSED2        = 0b00000010;
    public static final int NOTUSED3        = 0b00000100;
    public static final int NOTUSED4        = 0b00001000;
    public static final int NOTUSED5        = 0b00010000;
    public static final int SPRITE_OVERFLOW = 0b00100000;
    public static final int SPRITE_ZERO_HIT = 0b01000000;
    public static final int VBLANK_STARTED  = 0b10000000;

    private int bits;

    public StatusRegister() {
        this.bits = 0b00000000;
    }

    // Set or clear a specific flag based on a boolean status
    private void setFlag(int flag, boolean status) {
        if (status) {
            this.bits |= flag;          // set the bit
        } else {
            this.bits &= ~flag & 0xFF;  // clear the bit, masked to 8 bits
        }
    }

    private boolean contains(int flag) {
        return (this.bits & flag) == flag;
    }

    public void setVblankStatus(boolean status) {
        setFlag(VBLANK_STARTED, status);
    }

    public void setSpriteZeroHit(boolean status) {
        setFlag(SPRITE_ZERO_HIT, status);
    }

    public void setSpriteOverflow(boolean status) {
        setFlag(SPRITE_OVERFLOW, status);
    }

    public void resetVblankStatus() {
        setFlag(VBLANK_STARTED, false);
    }

    public boolean isInVblank() {
        return contains(VBLANK_STARTED);
    }

    public int snapshot() {
        return this.bits & 0xFF; // Mask to 8 bits to simulate u8 behavior
    }
}
