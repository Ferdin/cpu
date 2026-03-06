package test.java.com.ferdin.nes;

import main.java.com.ferdin.nes.rom.Rom.Mirroring;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import main.java.com.ferdin.nes.ppu.NesPPU;

public class NesPPUTest {
    @Test
    void testPpuVramWrites() {
        NesPPU ppu = NesPPU.newEmptyRom();
        ppu.writeToPpuAddr(0x23);
        ppu.writeToPpuAddr(0x05);
        ppu.writeToData(0x66);

        assertEquals(0x66, ppu.vram[0x0305]);
    }

    @Test
    void testPpuVramReads() {
        NesPPU ppu = NesPPU.newEmptyRom();
        ppu.writeToCtrl(0);
        ppu.vram[0x0305] = 0x66;

        ppu.writeToPpuAddr(0x23);
        ppu.writeToPpuAddr(0x05);

        ppu.readData(); // load into buffer
        assertEquals(0x2306, ppu.addr.get());
        assertEquals(0x66, ppu.readData());
    }

    @Test
    void testPpuVramReadsCrossPage() {
        NesPPU ppu = NesPPU.newEmptyRom();
        ppu.writeToCtrl(0);
        ppu.vram[0x01ff] = 0x66;
        ppu.vram[0x0200] = 0x77;

        ppu.writeToPpuAddr(0x21);
        ppu.writeToPpuAddr(0xff);

        ppu.readData(); // load into buffer
        assertEquals(0x66, ppu.readData());
        assertEquals(0x77, ppu.readData());
    }

    @Test
    void testPpuVramReadsStep32() {
        NesPPU ppu = NesPPU.newEmptyRom();
        ppu.writeToCtrl(0b100);
        ppu.vram[0x01ff]      = 0x66;
        ppu.vram[0x01ff + 32] = 0x77;
        ppu.vram[0x01ff + 64] = 0x88;

        ppu.writeToPpuAddr(0x21);
        ppu.writeToPpuAddr(0xff);

        ppu.readData(); // load into buffer
        assertEquals(0x66, ppu.readData());
        assertEquals(0x77, ppu.readData());
        assertEquals(0x88, ppu.readData());
    }

    @Test
    void testVramHorizontalMirror() {
        NesPPU ppu = NesPPU.newEmptyRom();

        ppu.writeToPpuAddr(0x24);
        ppu.writeToPpuAddr(0x05);
        ppu.writeToData(0x66); // write to a

        ppu.writeToPpuAddr(0x28);
        ppu.writeToPpuAddr(0x05);
        ppu.writeToData(0x77); // write to B

        ppu.writeToPpuAddr(0x20);
        ppu.writeToPpuAddr(0x05);
        ppu.readData(); // load into buffer
        assertEquals(0x66, ppu.readData()); // read from A

        ppu.writeToPpuAddr(0x2C);
        ppu.writeToPpuAddr(0x05);
        ppu.readData(); // load into buffer
        assertEquals(0x77, ppu.readData()); // read from b
    }

    @Test
    void testVramVerticalMirror() {
        NesPPU ppu = new NesPPU(new int[2048], Mirroring.Vertical);

        ppu.writeToPpuAddr(0x20);
        ppu.writeToPpuAddr(0x05);
        ppu.writeToData(0x66); // write to A

        ppu.writeToPpuAddr(0x2C);
        ppu.writeToPpuAddr(0x05);
        ppu.writeToData(0x77); // write to b

        ppu.writeToPpuAddr(0x28);
        ppu.writeToPpuAddr(0x05);
        ppu.readData(); // load into buffer
        assertEquals(0x66, ppu.readData()); // read from a

        ppu.writeToPpuAddr(0x24);
        ppu.writeToPpuAddr(0x05);
        ppu.readData(); // load into buffer
        assertEquals(0x77, ppu.readData()); // read from B
    }

    @Test
    void testReadStatusResetsLatch() {
        NesPPU ppu = NesPPU.newEmptyRom();
        ppu.vram[0x0305] = 0x66;

        ppu.writeToPpuAddr(0x21);
        ppu.writeToPpuAddr(0x23);
        ppu.writeToPpuAddr(0x05);

        ppu.readData(); // load into buffer
        assertNotEquals(0x66, ppu.readData());

        ppu.readStatus();

        ppu.writeToPpuAddr(0x23);
        ppu.writeToPpuAddr(0x05);
        ppu.readData(); // load into buffer
        assertEquals(0x66, ppu.readData());
    }

    @Test
    void testPpuVramMirroring() {
        NesPPU ppu = NesPPU.newEmptyRom();
        ppu.writeToCtrl(0);
        ppu.vram[0x0305] = 0x66;

        ppu.writeToPpuAddr(0x63); // 0x6305 -> 0x2305
        ppu.writeToPpuAddr(0x05);
        ppu.readData(); // load into buffer
        assertEquals(0x66, ppu.readData());
    }

    @Test
    void testReadStatusResetsVblank() {
        NesPPU ppu = NesPPU.newEmptyRom();
        ppu.status.setVblankStatus(true);

        int status = ppu.readStatus();
        assertEquals(1, status >> 7);
        assertEquals(0, ppu.status.snapshot() >> 7);
    }

    @Test
    void testOamReadWrite() {
        NesPPU ppu = NesPPU.newEmptyRom();
        ppu.writeToOamAddr(0x10);
        ppu.writeToOamData(0x66);
        ppu.writeToOamData(0x77);

        ppu.writeToOamAddr(0x10);
        assertEquals(0x66, ppu.readOamData());

        ppu.writeToOamAddr(0x11);
        assertEquals(0x77, ppu.readOamData());
    }

    @Test
    void testOamDma() {
        NesPPU ppu = NesPPU.newEmptyRom();

        int[] data = new int[256];
        java.util.Arrays.fill(data, 0x66);
        data[0]   = 0x77;
        data[255] = 0x88;

        ppu.writeToOamAddr(0x10);
        ppu.writeOamDma(data);

        ppu.writeToOamAddr(0x0f); // wrap around
        assertEquals(0x88, ppu.readOamData());

        ppu.writeToOamAddr(0x10);
        assertEquals(0x77, ppu.readOamData());

        ppu.writeToOamAddr(0x11);
        assertEquals(0x66, ppu.readOamData());
    }
}
