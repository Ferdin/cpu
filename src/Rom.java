import java.util.Arrays;

public class Rom {

    public enum Mirroring {
        Vertical,
        Horizontal,
        FourScreen
    }

    public byte[] prgRom;
    public byte[] chrRom;
    public int mapper;
    public Mirroring screenMirroring;

    private static final int PRG_ROM_PAGE_SIZE = 16384; // 16 KB
    private static final int CHR_ROM_PAGE_SIZE = 8192;  // 8 KB
    private static final byte[] NES_TAG = {0x4E, 0x45, 0x53, 0x1A}; // "NES\x1A"

    public Rom(byte[] raw) throws Exception {
        if (raw.length < 16 || !Arrays.equals(Arrays.copyOfRange(raw, 0, 4), NES_TAG)) {
            throw new Exception("File is not in iNES file format");
        }

        // Mapper calculation: upper nibble of byte 7 | upper nibble of byte 6
        this.mapper = ((raw[7] & 0xF0) | ((raw[6] & 0xFF) >> 4)) & 0xFF;

        int inesVer = (raw[7] >> 2) & 0b11;
        if (inesVer != 0) {
            throw new Exception("NES2.0 format is not supported");
        }

        boolean fourScreen = (raw[6] & 0b1000) != 0;
        boolean verticalMirroring = (raw[6] & 0b1) != 0;

        if (fourScreen) {
            this.screenMirroring = Mirroring.FourScreen;
        } else if (verticalMirroring) {
            this.screenMirroring = Mirroring.Vertical;
        } else {
            this.screenMirroring = Mirroring.Horizontal;
        }

        int prgRomSize = (raw[4] & 0xFF) * PRG_ROM_PAGE_SIZE;
        int chrRomSize = (raw[5] & 0xFF) * CHR_ROM_PAGE_SIZE;

        boolean skipTrainer = (raw[6] & 0b100) != 0;
        int prgRomStart = 16 + (skipTrainer ? 512 : 0);
        int chrRomStart = prgRomStart + prgRomSize;

        this.prgRom = Arrays.copyOfRange(raw, prgRomStart, prgRomStart + prgRomSize);
        this.chrRom = Arrays.copyOfRange(raw, chrRomStart, chrRomStart + chrRomSize);
    }
}
