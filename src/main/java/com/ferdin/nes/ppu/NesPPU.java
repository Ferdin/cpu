package main.java.com.ferdin.nes.ppu;

import main.java.com.ferdin.nes.rom.Rom.Mirroring;
import main.java.com.ferdin.nes.ppu.registers.ControlRegister;
import main.java.com.ferdin.nes.ppu.registers.MaskRegister;
import main.java.com.ferdin.nes.ppu.registers.StatusRegister;
import main.java.com.ferdin.nes.ppu.registers.ScrollRegister;
import main.java.com.ferdin.nes.ppu.registers.AddrRegister;

public class NesPPU implements PPU {

    public int[]           chrRom;
    public Mirroring       mirroring;
    public ControlRegister ctrl;
    public MaskRegister    mask;
    public StatusRegister  status;
    public ScrollRegister  scroll;
    public AddrRegister    addr;
    public int[]           vram;
    public int             oamAddr;
    public int[]           oamData;
    public int[]           paletteTable;
    public int             cycles;
    public int             scanline;
    public Byte            nmiInterrupt; // null if no NMI, 1 if NMI should be triggered

    private int internalDataBuf;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    public static NesPPU newEmptyRom() {
        return new NesPPU(new int[2048], Mirroring.Horizontal);
    }

    public NesPPU(int[] chrRom, Mirroring mirroring) {
        this.chrRom        = chrRom;
        this.mirroring     = mirroring;
        this.ctrl          = new ControlRegister();
        this.mask          = new MaskRegister();
        this.status        = new StatusRegister();
        this.scroll        = new ScrollRegister();
        this.addr          = new AddrRegister();
        this.vram          = new int[2048];
        this.oamAddr       = 0;
        this.oamData       = new int[256];
        this.paletteTable  = new int[32];
        this.internalDataBuf = 0;
    }

    // -----------------------------------------------------------------------
    // VRAM helpers
    // -----------------------------------------------------------------------

    // Horizontal:
    //   [ A ] [ a ]
    //   [ B ] [ b ]
    // Vertical:
    //   [ A ] [ B ]
    //   [ a ] [ b ]
    public int mirrorVramAddr(int addr) {
        int mirrored  = addr & 0b10111111111111; // mirror 0x3000-0x3eff → 0x2000-0x2eff
        int vramIndex = mirrored - 0x2000;
        int nameTable = vramIndex / 0x400;

        switch (mirroring) {
            case Vertical:
                if (nameTable == 2 || nameTable == 3) return vramIndex - 0x800;
                break;
            case Horizontal:
                if (nameTable == 1) return vramIndex - 0x400;
                if (nameTable == 2) return vramIndex - 0x400;
                if (nameTable == 3) return vramIndex - 0x800;
                break;
            case FourScreen:
                // no mirroring, all name tables are unique
                break;
             default:
                 throw new IllegalStateException("Unexpected mirroring type: " + mirroring);    
        }
        return vramIndex;
    }

    private void incrementVramAddr() {
        addr.increment(ctrl.vramAddrIncrement());
    }

    // -----------------------------------------------------------------------
    // PPU interface implementation
    // -----------------------------------------------------------------------

    @Override
    public void writeToCtrl(int value) {
        boolean beforeNmiStatus = ctrl.generateVblankNmi();
        ctrl.update(value);
        if(!beforeNmiStatus && ctrl.generateVblankNmi() && status.isInVblank()) {
            nmiInterrupt = 1;
        }
    }

    @Override
    public void writeToMask(int value) {
        mask.update(value);
    }

    @Override
    public int readStatus() {
        int data = status.snapshot();
        status.resetVblankStatus();
        addr.resetLatch();
        scroll.resetLatch();
        return data;
    }

    @Override
    public void writeToOamAddr(int value) {
        this.oamAddr = value & 0xFF;
    }

    @Override
    public void writeToOamData(int value) {
        oamData[oamAddr] = value & 0xFF;
        oamAddr = (oamAddr + 1) & 0xFF; // wrapping add
    }

    @Override
    public int readOamData() {
        return oamData[oamAddr];
    }

    @Override
    public void writeToScroll(int value) {
        scroll.write(value);
    }

    @Override
    public void writeToPpuAddr(int value) {
        addr.update(value);
    }

    @Override
    public void writeToData(int value) {
        int address = addr.get();

        if (address <= 0x1fff) {
            System.out.println("attempt to write to chr rom space " + address);

        } else if (address <= 0x2fff) {
            vram[mirrorVramAddr(address)] = value & 0xFF;

        } else if (address <= 0x3eff) {
            throw new UnsupportedOperationException(
                "addr " + address + " shouldn't be used in reality");

        } else if (address == 0x3f10 || address == 0x3f14
                || address == 0x3f18 || address == 0x3f1c) {
            int mirrorAddr = address - 0x10;
            paletteTable[mirrorAddr - 0x3f00] = value & 0xFF;

        } else if (address <= 0x3fff) {
            paletteTable[address - 0x3f00] = value & 0xFF;

        } else {
            throw new IllegalStateException(
                "unexpected access to mirrored space " + address);
        }

        incrementVramAddr();
    }

    @Override
    public int readData() {
        int address = addr.get();
        incrementVramAddr();

        if (address <= 0x1fff) {
            int result = internalDataBuf;
            internalDataBuf = chrRom[address] & 0xFF;
            return result;

        } else if (address <= 0x2fff) {
            int result = internalDataBuf;
            internalDataBuf = vram[mirrorVramAddr(address)] & 0xFF;
            return result;

        } else if (address <= 0x3eff) {
            throw new UnsupportedOperationException(
                "addr " + address + " shouldn't be used in reality");

        } else if (address == 0x3f10 || address == 0x3f14
                || address == 0x3f18 || address == 0x3f1c) {
            int mirrorAddr = address - 0x10;
            return paletteTable[mirrorAddr - 0x3f00] & 0xFF;

        } else if (address <= 0x3fff) {
            return paletteTable[address - 0x3f00] & 0xFF;

        } else {
            throw new IllegalStateException(
                "unexpected access to mirrored space " + address);
        }
    }

    @Override
    public void writeOamDma(int[] data) {
        for (int b : data) {
            oamData[oamAddr] = b & 0xFF;
            oamAddr = (oamAddr + 1) & 0xFF; // wrapping add
        }
    }

    public boolean tick(int cycles) {
        this.cycles += cycles;
        if (this.cycles >= 341) {
            if (isSprite0Hit(this.cycles)) {
                status.setSpriteZeroHit(true);
            }

            this.cycles -= 341;
            scanline++;

            if (scanline == 241) {
                status.setVblankStatus(true);
                status.setSpriteZeroHit(false);
                if (ctrl.generateVblankNmi()) {
                    nmiInterrupt = 1;
                }
            }

            if (scanline >= 262) {
                scanline = 0;
                nmiInterrupt = null;
                status.setSpriteZeroHit(false);
                status.resetVblankStatus();
                return true;
            }
        }
        return false;
    }

    private boolean isSprite0Hit(int cycle) {
        int y = oamData[0] & 0xFF;
        int x = oamData[3] & 0xFF;
        return (y == scanline) && (x <= cycle) && mask.showSprites();
    }

    public Byte pollNmiInterrupt() {

        Byte result = nmiInterrupt;
        nmiInterrupt = null;

        return result;
    }
}