package main.java.com.ferdin.nes.render;

import main.java.com.ferdin.nes.ppu.NesPPU;
import main.java.com.ferdin.nes.rom.Rom.Mirroring;
import java.util.Arrays;

public class Renderer {

    private static int[] bgPalette(NesPPU ppu, int[] attributeTable, int tileColumn, int tileRow) {
        int attrTableIdx = (tileRow / 4) * 8 + (tileColumn / 4);
        int attrByte     = attributeTable[attrTableIdx] & 0xFF;

        int col = (tileColumn % 4) / 2;
        int row = (tileRow    % 4) / 2;

        int paletteIdx;
        if      (col == 0 && row == 0) paletteIdx =  attrByte & 0b11;
        else if (col == 1 && row == 0) paletteIdx = (attrByte >> 2) & 0b11;
        else if (col == 0 && row == 1) paletteIdx = (attrByte >> 4) & 0b11;
        else                           paletteIdx = (attrByte >> 6) & 0b11;

        int paletteStart = 1 + paletteIdx * 4;
        return new int[] {
            ppu.paletteTable[0]                & 0xFF,
            ppu.paletteTable[paletteStart]     & 0xFF,
            ppu.paletteTable[paletteStart + 1] & 0xFF,
            ppu.paletteTable[paletteStart + 2] & 0xFF
        };
    }

    private static int[] spritePalette(NesPPU ppu, int paletteIdx) {
        int start = 0x11 + paletteIdx * 4;
        return new int[] {
            0,
            ppu.paletteTable[start]     & 0xFF,
            ppu.paletteTable[start + 1] & 0xFF,
            ppu.paletteTable[start + 2] & 0xFF
        };
    }

    private static void renderNameTable(NesPPU ppu, Frame frame, int[] nameTable,
                                        Rect viewPort, int shiftX, int shiftY) {
        int bank = ppu.ctrl.bkndPatternAddr();
        int[] attributeTable = Arrays.copyOfRange(nameTable, 0x3C0, 0x400);

        for (int i = 0; i < 0x3C0; i++) {
            int tileColumn = i % 32;
            int tileRow    = i / 32;
            int tileIdx    = nameTable[i] & 0xFF;
            int tileStart  = bank + tileIdx * 16;
            int[] palette  = bgPalette(ppu, attributeTable, tileColumn, tileRow);

            for (int y = 0; y < 8; y++) {
                int upper = ppu.chrRom[tileStart + y]     & 0xFF;
                int lower = ppu.chrRom[tileStart + y + 8] & 0xFF;

                for (int x = 7; x >= 0; x--) {
                    int value = ((lower & 1) << 1) | (upper & 1);
                    upper >>= 1;
                    lower >>= 1;

                    int paletteIndex = switch (value) {
                        case 0 -> ppu.paletteTable[0] & 0xFF;
                        case 1 -> palette[1];
                        case 2 -> palette[2];
                        case 3 -> palette[3];
                        default -> throw new RuntimeException("invalid pixel value");
                    };

                    int[] rgb  = Palette.SYSTEM_PALETTE[paletteIndex];
                    int pixelX = tileColumn * 8 + x;
                    int pixelY = tileRow    * 8 + y;

                    if (pixelX >= viewPort.x1 && pixelX < viewPort.x2
                     && pixelY >= viewPort.y1 && pixelY < viewPort.y2) {
                        frame.setPixel(shiftX + pixelX, shiftY + pixelY,
                                       rgb[0], rgb[1], rgb[2]);
                    }
                }
            }
        }
    }

    public static void render(NesPPU ppu, Frame frame) {
        int scrollX = ppu.scroll.getScrollX() & 0xFF;
        int scrollY = ppu.scroll.getScrollY() & 0xFF;

        int nametableAddr = ppu.ctrl.nametableAddr();

        int[] mainNameTable;
        int[] secondNameTable;

        if ((ppu.mirroring == Mirroring.Vertical   && (nametableAddr == 0x2000 || nametableAddr == 0x2800))
         || (ppu.mirroring == Mirroring.Horizontal && (nametableAddr == 0x2000 || nametableAddr == 0x2400))) {
            mainNameTable   = Arrays.copyOfRange(ppu.vram, 0,     0x400);
            secondNameTable = Arrays.copyOfRange(ppu.vram, 0x400, 0x800);

        } else if ((ppu.mirroring == Mirroring.Vertical   && (nametableAddr == 0x2400 || nametableAddr == 0x2C00))
                || (ppu.mirroring == Mirroring.Horizontal && (nametableAddr == 0x2800 || nametableAddr == 0x2C00))) {
            mainNameTable   = Arrays.copyOfRange(ppu.vram, 0x400, 0x800);
            secondNameTable = Arrays.copyOfRange(ppu.vram, 0,     0x400);

        } else {
            throw new RuntimeException("Unsupported mirroring type: " + ppu.mirroring);
        }

        // Main nametable — visible portion starting at scroll offset
        renderNameTable(ppu, frame,
            mainNameTable,
            new Rect(scrollX, scrollY, 256, 240),
            -scrollX, -scrollY
        );

        // Second nametable — only rendered when scrolling
        if (scrollX > 0) {
            renderNameTable(ppu, frame,
                secondNameTable,
                new Rect(0, 0, scrollX, 240),
                256 - scrollX, 0
            );
        } else if (scrollY > 0) {
            renderNameTable(ppu, frame,
                secondNameTable,
                new Rect(0, 0, 256, scrollY),
                0, 240 - scrollY
            );
        }

        // --- Sprites ---
        int spriteBank = ppu.ctrl.sprtPatternAddr();

        for (int i = ppu.oamData.length - 4; i >= 0; i -= 4) {
            int tileIdx = ppu.oamData[i + 1] & 0xFF;
            int tileX   = ppu.oamData[i + 3] & 0xFF;
            int tileY   = ppu.oamData[i]     & 0xFF;

            boolean flipVertical   = ((ppu.oamData[i + 2] >> 7) & 1) == 1;
            boolean flipHorizontal = ((ppu.oamData[i + 2] >> 6) & 1) == 1;

            int paletteIdx   = ppu.oamData[i + 2] & 0b11;
            int[] spritePal  = spritePalette(ppu, paletteIdx);
            int sprTileStart = spriteBank + tileIdx * 16;

            for (int y = 0; y < 8; y++) {
                int upper = ppu.chrRom[sprTileStart + y]     & 0xFF;
                int lower = ppu.chrRom[sprTileStart + y + 8] & 0xFF;

                for (int x = 7; x >= 0; x--) {
                    int value = ((lower & 1) << 1) | (upper & 1);
                    upper >>= 1;
                    lower >>= 1;

                    if (value == 0) continue; // transparent pixel

                    int paletteIndex = switch (value) {
                        case 1 -> spritePal[1];
                        case 2 -> spritePal[2];
                        case 3 -> spritePal[3];
                        default -> throw new RuntimeException("invalid sprite pixel value");
                    };

                    int[] rgb = Palette.SYSTEM_PALETTE[paletteIndex];
                    int drawX = flipHorizontal ? tileX + 7 - x : tileX + x;
                    int drawY = flipVertical   ? tileY + 7 - y : tileY + y;

                    frame.setPixel(drawX, drawY, rgb[0], rgb[1], rgb[2]);
                }
            }
        }
    }
}