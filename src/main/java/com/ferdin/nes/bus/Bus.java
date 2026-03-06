package main.java.com.ferdin.nes.bus;
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

import main.java.com.ferdin.nes.rom.Rom;
import main.java.com.ferdin.nes.ppu.NesPPU;

public class Bus implements Mem {

    // =========================
    // NES Memory Map Constants
    // =========================

    private static final int RAM = 0x0000;
    private static final int RAM_MIRRORS_END = 0x1FFF;
    //private static final int PPU_REGISTERS = 0x2000;
    private static final int PPU_REGISTERS_MIRRORS_END = 0x3FFF;

    // 2KB internal RAM
    private Rom rom;
    private NesPPU ppu;
    private byte[] cpuVram;
    private int[]  prgRom;

    public Bus() {
        this.rom = null;
        this.cpuVram = new byte[2048];
    }

    public Bus(Rom rom) {
        // Convert byte[] to int[] to match NesPPU's expected type
        int[] chrRom = new int[rom.chrRom.length];
        for (int i = 0; i < rom.chrRom.length; i++) {
            chrRom[i] = rom.chrRom[i] & 0xFF;
        }
        this.rom = rom;
        this.cpuVram = new byte[2048];
        this.ppu    = new NesPPU(chrRom, rom.screenMirroring);
    }

    // Optional getters if needed
    public byte[] getCpuVram() {
        return cpuVram;
    }

    public NesPPU getPpu(){ 
        return ppu; 
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
            int mirrorDownAddr = addr & 0b00000111_11111111;
            return cpuVram[mirrorDownAddr] & 0xFF;

        } else if (addr == 0x2000 || addr == 0x2001 || addr == 0x2003
                || addr == 0x2005 || addr == 0x2006 || addr == 0x4014) {
            throw new UnsupportedOperationException(
                "Attempt to read from write-only PPU address: " + Integer.toHexString(addr));

        } else if (addr == 0x2002) {
            return ppu.readStatus();

        } else if (addr == 0x2004) {
            return ppu.readOamData();

        } else if (addr == 0x2007) {
            return ppu.readData();

        } else if (addr >= 0x2008 && addr <= PPU_REGISTERS_MIRRORS_END) {
            int mirrorDownAddr = addr & 0b00100000_00000111;
            return memRead(mirrorDownAddr); // recursive mirror resolution

        } else if (addr >= 0x8000 && addr <= 0xFFFF) {
            return readPrgRom(addr);

        } else {
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
            int mirrorDownAddr = addr & 0b00000111_11111111;
            cpuVram[mirrorDownAddr] = (byte) data;

        } else if (addr == 0x2000) {
            ppu.writeToCtrl(data);

        } else if (addr == 0x2001) {
            ppu.writeToMask(data);

        } else if (addr == 0x2002) {
            throw new UnsupportedOperationException(
                "Attempt to write to read-only PPU status register");

        } else if (addr == 0x2003) {
            ppu.writeToOamAddr(data);

        } else if (addr == 0x2004) {
            ppu.writeToOamData(data);

        } else if (addr == 0x2005) {
            ppu.writeToScroll(data);

        } else if (addr == 0x2006) {
            ppu.writeToPpuAddr(data);

        } else if (addr == 0x2007) {
            ppu.writeToData(data);

        } else if (addr == 0x4014) {
            // OAM DMA - copy 256 bytes from CPU page into OAM
            int[] buffer = new int[256];
            int page = (data << 8) & 0xFFFF;
            for (int i = 0; i < 256; i++) {
                buffer[i] = memRead(page + i);
            }
            ppu.writeOamDma(buffer);

        } else if (addr >= 0x2008 && addr <= PPU_REGISTERS_MIRRORS_END) {
            int mirrorDownAddr = addr & 0b00100000_00000111;
            memWrite(mirrorDownAddr, data); // recursive mirror resolution

        } else if (addr >= 0x8000 && addr <= 0xFFFF) {
            throw new UnsupportedOperationException(
                "Attempt to write to Cartridge ROM space: " + Integer.toHexString(addr));

        } else {
            System.out.println("Ignoring mem write-access at " + Integer.toHexString(addr));
        }
    }

    // =========================
    // PRG ROM Read
    // =========================
    private int readPrgRom(int addr) {
        addr -= 0x8000;
        int prgLength = prgRom.length;
        // If 16KB ROM, mirror it
        if (prgLength == 0x4000 && addr >= 0x4000) {
            addr = addr % 0x4000;
        }
        return prgRom[addr] & 0xFF;
    }

    // =========================
    // 16-bit Read/Write
    // =========================
    @Override
    public int memReadU16(int pos) {
        int lo = memRead(pos)     & 0xFF;
        int hi = memRead(pos + 1) & 0xFF;
        return (hi << 8) | lo;
    }

    @Override
    public void memWriteU16(int pos, int data) {
        memWrite(pos,     data & 0xFF);
        memWrite(pos + 1, (data >> 8) & 0xFF);
    }

}
