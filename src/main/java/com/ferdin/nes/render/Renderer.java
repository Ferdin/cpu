package main.java.com.ferdin.nes.render;

import main.java.com.ferdin.nes.ppu.NesPPU;

public class Renderer {

    private static int[] bgPalette(NesPPU ppu, int tileColumn, int tileRow) {
        int attrTableIdx = (tileRow / 4) * 8 + (tileColumn / 4);
        int attrByte = ppu.vram[0x3C0 + attrTableIdx] & 0xFF;

        int palletIdx;
        int col = (tileColumn % 4) / 2;
        int row = (tileRow % 4) / 2;

        if (col == 0 && row == 0)      palletIdx =  attrByte & 0b11;
        else if (col == 1 && row == 0) palletIdx = (attrByte >> 2) & 0b11;
        else if (col == 0 && row == 1) palletIdx = (attrByte >> 4) & 0b11;
        else                           palletIdx = (attrByte >> 6) & 0b11;

        int paletteStart = 1 + palletIdx * 4;
        return new int[] {
            ppu.paletteTable[0] & 0xFF,
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

    public static void render(NesPPU ppu, Frame frame) {

        // --- Background ---
        int bank = ppu.ctrl.bkndPatternAddr();

        for (int i = 0; i < 0x3C0; i++) {
            int tile = ppu.vram[i] & 0xFF;
            int tileColumn = i % 32;
            int tileRow    = i / 32;
            int tileStart  = bank + tile * 16;
            int[] palette  = bgPalette(ppu, tileColumn, tileRow);

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

                    int[] rgb = Palette.SYSTEM_PALETTE[paletteIndex];
                    frame.setPixel(tileColumn * 8 + x, tileRow * 8 + y, rgb[0], rgb[1], rgb[2]);
                }
            }
        }

        // --- Sprites ---
        int spriteBank = ppu.ctrl.sprtPatternAddr();

        for (int i = ppu.oamData.length - 4; i >= 0; i -= 4) {
            int tileIdx = ppu.oamData[i + 1] & 0xFF;
            int tileX   = ppu.oamData[i + 3] & 0xFF;
            int tileY   = ppu.oamData[i]     & 0xFF;

            boolean flipVertical   = ((ppu.oamData[i + 2] >> 7) & 1) == 1;
            boolean flipHorizontal = ((ppu.oamData[i + 2] >> 6) & 1) == 1;

            int paletteIdx     = ppu.oamData[i + 2] & 0b11;
            int[] spritePal    = spritePalette(ppu, paletteIdx);
            int sprTileStart   = spriteBank + tileIdx * 16;

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