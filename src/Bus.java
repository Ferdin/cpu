//  _______________ $10000  _______________
// | PRG-ROM       |       |               |
// | Upper Bank    |       |               |
// |_ _ _ _ _ _ _ _| $C000 | PRG-ROM       |
// | PRG-ROM       |       |               |
// | Lower Bank    |       |               |
// |_______________| $8000 |_______________|
// | SRAM          |       | SRAM          |
// |_______________| $6000 |_______________|
// | Expansion ROM |       | Expansion ROM |
// |_______________| $4020 |_______________|
// | I/O Registers |       |               |
// |_ _ _ _ _ _ _ _| $4000 |               |
// | Mirrors       |       | I/O Registers |
// | $2000-$2007   |       |               |
// |_ _ _ _ _ _ _ _| $2008 |               |
// | I/O Registers |       |               |
// |_______________| $2000 |_______________|
// | Mirrors       |       |               |
// | $0000-$07FF   |       |               |
// |_ _ _ _ _ _ _ _| $0800 |               |
// | RAM           |       | RAM           |
// |_ _ _ _ _ _ _ _| $0200 |               |
// | Stack         |       |               |
// |_ _ _ _ _ _ _ _| $0100 |               |
// | Zero Page     |       |               |
// |_______________| $0000 |_______________|

public class Bus implements Mem {

    // =========================
    // NES Memory Map Constants
    // =========================

    private static final int RAM = 0x0000;
    private static final int RAM_MIRRORS_END = 0x1FFF;
    private static final int PPU_REGISTERS = 0x2000;
    private static final int PPU_REGISTERS_MIRRORS_END = 0x3FFF;

    // 2KB internal RAM
    private byte[] cpuVram;
    private Rom rom;

    public Bus() {
        this.rom = null;
        this.cpuVram = new byte[2048];
    }

    public Bus(Rom rom) {
        this.rom = rom;
        this.cpuVram = new byte[2048];
    }

    // Optional getters if needed
    public byte[] getCpuVram() {
        return cpuVram;
    }

    public Rom getRom() {
        return rom;
    }

    // =========================
    // Memory Read
    // =========================
    @Override
    public int memRead(int addr) {

        addr &= 0xFFFF;

        if (addr >= RAM && addr <= RAM_MIRRORS_END) {

            int mirrorDownAddr = addr & 0x07FF;
            return cpuVram[mirrorDownAddr];

        } else if (addr >= PPU_REGISTERS && addr <= PPU_REGISTERS_MIRRORS_END) {

            throw new UnsupportedOperationException("PPU not supported yet");

        } 
        else if (addr >= 0x8000 && addr <= 0xFFFF) {

            if (rom == null) {
                return 0; // or read from RAM for test mode
            }
            return readPrgRom(addr);

        } 
        else {

            System.out.println("Ignoring mem access at " + Integer.toHexString(addr));
            return 0;
        }
    }

    // =========================
    // Memory Write
    // =========================
    @Override
    public void memWrite(int addr, int data) {

        addr &= 0xFFFF;
        data &= 0xFF;

        if (addr >= RAM && addr <= RAM_MIRRORS_END) {

            int mirrorDownAddr = addr & 0x07FF;
            cpuVram[mirrorDownAddr] = (byte)data;

        } else if (addr >= PPU_REGISTERS && addr <= PPU_REGISTERS_MIRRORS_END) {

            //int mirrorDownAddr = addr & 0x2007;
            throw new UnsupportedOperationException("PPU not supported yet");

        } else if (addr >= 0x8000 && addr <= 0xFFFF) {
             if (rom == null) {
                return; // ignore writes in test mode
            }
            throw new UnsupportedOperationException(
                "Attempt to write to Cartridge ROM space"
            );
        } else {
                System.out.println("Ignoring mem write-access at " + Integer.toHexString(addr));
            }
    }

    private int readPrgRom(int addr) {

        addr -= 0x8000; // Map CPU space to ROM space

        int prgLength = rom.prgRom.length;

        // If 16KB ROM, mirror it
        if (prgLength == 0x4000 && addr >= 0x4000) {
            addr = addr % 0x4000;
        }

        return rom.prgRom[addr] & 0xFF;
    }

    @Override
    public int memReadU16(int pos) {
        int lo = memRead(pos) & 0xFF;  // Treat as unsigned byte
        int hi = memRead(pos + 1) & 0xFF;  // Treat as unsigned byte
        return (hi << 8) | lo;
    }

    @Override
    public void memWriteU16(int pos, int data) {
        byte hi = (byte)((data >> 8) & 0xFF);
        byte lo = (byte)(data & 0xFF);
        memWrite(pos, lo);
        memWrite(pos + 1, hi);
    }

}
