package com.ferdin.nescpu;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DemoNESTest {
    @Test
    void test_0xA9_lda_immediate_load_data(){
        Bus bus = new Bus();
        DemoNES nes = new DemoNES(bus);
        nes.loadAndRun(new int[] { 0xA9, 0x05, 0x00 });

        assertEquals(0x05, nes.registerA);
        assertEquals(0, nes.status & 0b0000_0010); // Zero flag should be clear
        assertEquals(0, nes.status & 0b1000_0000); // Negative flag should be clear
    }
    @Test
    void test_lda_from_memory(){
        Bus bus = new Bus();
        DemoNES nes = new DemoNES(bus);
        nes.memWrite(0x10,(byte) 0x55);
        nes.loadAndRun(new int[] {
            0xa5, 0x10, 0x00
        });
        assertEquals(nes.registerA, 0x55);
    }
    @Test
    void test_0xA9_lda_zero_flag() {
        Bus bus = new Bus();
        DemoNES nes = new DemoNES(bus);

        nes.loadAndRun(new int[] {
                0xA9,
                0x00,
                0x00
        });

        assertEquals(0b10, nes.status & 0b0000_0010); // Zero flag should be set
    }
    @Test
    void test_0xaa_tax_move_a_to_x(){
        Bus bus = new Bus();
        DemoNES nes = new DemoNES(bus);
        nes.loadAndRun(new int[] {
            0xA9, 0x0A,  // LDA #$0A (load 10 into A)
            0xAA,        // TAX (transfer A to X)
            0x00         // BRK
        });
        assertEquals(10, nes.registerX);
    }
    @Test
    void test_5_ops_working_together(){
        Bus bus = new Bus();
        DemoNES nes = new DemoNES(bus);
        nes.loadAndRun(new int[]{
                0xA9, 0xC0, 
                0xAA,              
                0xE8, 0x00           
        });

        assertEquals(0xC1, nes.registerX & 0xFF);
    }
    @Test
    void test_inx_overflow(){
        Bus bus = new Bus();
        DemoNES nes = new DemoNES(bus);
        nes.registerX = 0xFF;
        nes.loadAndRun(new int[]{
            0xE8, 0xE8, 0x00
        });
        assertEquals(2, nes.registerX & 0xFF);
    }
    @Test
    void test_bcc_branch_taken(){
        Bus bus = new Bus();
        DemoNES nes = new DemoNES(bus);
        nes.loadAndRun(new int[]{
            0x90, 0x02, // BCC +2
            0xA9, 0x01, // LDA #$01 (should be skipped)
            0xA9, 0x02, // LDA #$02 (should be executed)
            0x00
        });
        assertEquals(0x02, nes.registerA);
    }
    @Test
    void test_bcs_branch_taken(){
        Bus bus = new Bus();
        DemoNES nes = new DemoNES(bus);
        nes.loadAndRun(new int[]{
            0x38, // SEC - Set Carry Flag
            0xB0, 0x02, // BCS +2
            0xA9, 0x01, // LDA #$01 (should be skipped)
            0xA9, 0x02, // LDA #$02 (should be executed)
            0x00
        });
        assertEquals(0x02, nes.registerA);
    }
    @Test
    void test_bcs_branch_not_taken(){
        Bus bus = new Bus();
        DemoNES nes = new DemoNES(bus);
        nes.loadAndRun(new int[]{
            0xB0, 0x02, // BCS +2 (not taken)
            0xA9, 0x01, // LDA #$01 (should be executed)
            0x00,              // BRK (stop here)
            0xA9, 0x02, // LDA #$02 (should be skipped)
            0x00
        });
        assertEquals(0x01, nes.registerA);
    }
    @Test
    void test_bit(){
        Bus bus = new Bus();
        DemoNES nes = new DemoNES(bus);
        nes.registerA = 0b00000001;
        nes.memWrite(0x10,(byte)0b11000000);
        nes.loadAndRun(new int[]{
           0x24,
           0x10,
           0x00
        });
        assertTrue((nes.status & 0b10000000) != 0); // Negative flag should be set
        assertTrue((nes.status & 0b01000000) != 0); // Overflow flag should be set
        assertTrue((nes.status & 0b00000010) != 0); // because A and memory is zero
    }

    @Test
    void test_cmp_equal() {
        Bus bus = new Bus();
        DemoNES cpu = new DemoNES(bus);

        cpu.load(new int[]{
           0xC9, 0x42,
           0x00
        });
        cpu.reset();
        cpu.registerA = 0x42;  // set AFTER reset
        cpu.step();

        assertTrue((cpu.status & DemoNES.CARRY) != 0);
        assertTrue((cpu.status & DemoNES.ZERO) != 0);
        assertTrue((cpu.status & DemoNES.NEGATIVE) == 0);
    }

    @Test
    void test_pha() {
        Bus bus = new Bus();
        DemoNES cpu = new DemoNES(bus);

        cpu.load(new int[]{
           0x48,
           0x00
        });
        cpu.reset();
        cpu.registerA = 0x42;  // set AFTER reset
        cpu.step();

        int valueOnStack = cpu.memRead(0x0100 + cpu.stackPointer + 1);
        assertEquals(0x42, valueOnStack);
    }

    @Test
    void test_php() {
        Bus bus = new Bus();
        DemoNES cpu = new DemoNES(bus);

        cpu.load(new int[]{
           0x08,  // PHP
           0x00   // BRK
        });
        cpu.reset();
        cpu.status = 0x42;  // set AFTER reset
        cpu.step();

        int valueOnStack = cpu.memRead(0x0100 + cpu.stackPointer + 1);
        assertEquals(0x42 | DemoNES.BREAK | DemoNES.BREAK2, valueOnStack);
    }

}
