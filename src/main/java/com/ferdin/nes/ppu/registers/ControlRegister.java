package main.java.com.ferdin.nes.ppu.registers;

public class ControlRegister {

    // Bit flag constants
    public static final int NAMETABLE1             = 0b00000001;
    public static final int NAMETABLE2             = 0b00000010;
    public static final int VRAM_ADD_INCREMENT     = 0b00000100;
    public static final int SPRITE_PATTERN_ADDR    = 0b00001000;
    public static final int BACKROUND_PATTERN_ADDR = 0b00010000;
    public static final int SPRITE_SIZE            = 0b00100000;
    public static final int MASTER_SLAVE_SELECT    = 0b01000000;
    public static final int GENERATE_NMI           = 0b10000000;

    // 7  bit  0
    // ---- ----
    // VPHB SINN
    // |||| ||||
    // |||| ||++- Base nametable address
    // |||| ||    (0 = $2000; 1 = $2400; 2 = $2800; 3 = $2C00)
    // |||| |+--- VRAM address increment per CPU read/write of PPUDATA
    // |||| |     (0: add 1, going across; 1: add 32, going down)
    // |||| +---- Sprite pattern table address for 8x8 sprites
    // ||||       (0: $0000; 1: $1000; ignored in 8x16 mode)
    // |||+------ Background pattern table address (0: $0000; 1: $1000)
    // ||+------- Sprite size (0: 8x8 pixels; 1: 8x16 pixels)
    // |+-------- PPU master/slave select
    // |          (0: read backdrop from EXT pins; 1: output color on EXT pins)
    // +--------- Generate an NMI at the start of the
    //            vertical blanking interval (0: off; 1: on)
    private int bits;

    public ControlRegister() {
        this.bits = 0b00000000;
    }

    // Check if specific flag(s) are set
    private boolean contains(int flag) {
        return (this.bits & flag) == flag;
    }

    public int nametableAddr() {
        return switch (this.bits & 0b11) {
            case 0 -> 0x2000;
            case 1 -> 0x2400;
            case 2 -> 0x2800;
            case 3 -> 0x2C00;
            default -> throw new IllegalStateException("Not possible");
        };
    }

    public int vramAddrIncrement() {
        return !contains(VRAM_ADD_INCREMENT) ? 1 : 32;
    }

    public int sprtPatternAddr() {
        return !contains(SPRITE_PATTERN_ADDR) ? 0 : 0x1000;
    }

    public int bkndPatternAddr() {
        return !contains(BACKROUND_PATTERN_ADDR) ? 0 : 0x1000;
    }

    public int spriteSize() {
        return !contains(SPRITE_SIZE) ? 8 : 16;
    }

    public int masterSlaveSelect() {
        return !contains(MASTER_SLAVE_SELECT) ? 0 : 1;
    }

    public boolean generateVblankNmi() {
        return contains(GENERATE_NMI);
    }

    public void update(int data) {
        this.bits = data & 0xFF; // Mask to 8 bits to simulate u8 behavior
    }

    // Optionally: expose bits as read-only
    public int getBits() {
        return this.bits;
    }
}
