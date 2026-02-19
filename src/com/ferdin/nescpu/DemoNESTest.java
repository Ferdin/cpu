package com.ferdin.nescpu;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DemoNESTest {
    @Test
    void test_0xA9_lda_immediate_load_data(){
        DemoNES nes = new DemoNES();
        nes.loadAndRun(new byte[] {(byte) 0xA9, (byte) 0x05, (byte) 0x00});

        assertEquals(0x05, nes.registerA);
        assertEquals(0, nes.status & 0b0000_0010); // Zero flag should be clear
        assertEquals(0, nes.status & 0b1000_0000); // Negative flag should be clear
    }
    @Test
    void test_lda_from_memory(){
        DemoNES nes = new DemoNES();
        nes.memWrite(0x10,(byte) 0x55);
        nes.loadAndRun(new byte[] {
            (byte) 0xa5, (byte) 0x10, (byte) 0x00
        });
        assertEquals(nes.registerA, 0x55);
    }
    @Test
    void test_0xA9_lda_zero_flag() {
        DemoNES nes = new DemoNES();

        nes.loadAndRun(new byte[] {
                (byte) 0xA9,
                (byte) 0x00,
                (byte) 0x00
        });

        assertEquals(0b10, nes.status & 0b0000_0010); // Zero flag should be set
    }
    @Test
    void test_0xaa_tax_move_a_to_x(){
        DemoNES nes = new DemoNES();
        nes.loadAndRun(new byte[] {
            (byte) 0xA9, (byte) 0x0A,  // LDA #$0A (load 10 into A)
            (byte) 0xAA,                // TAX (transfer A to X)
            (byte) 0x00                 // BRK
        });
        assertEquals(10, nes.registerX);
    }
    @Test
    void test_5_ops_working_together(){
        DemoNES nes = new DemoNES();
        nes.loadAndRun(new byte[]{
                (byte) 0xA9, (byte) 0xC0, 
                (byte) 0xAA,              
                (byte) 0xE8, (byte) 0x00           
        });

        assertEquals(0xC1, nes.registerX & 0xFF);
    }
    @Test
    void test_inx_overflow(){
        DemoNES nes = new DemoNES();
        nes.registerX = 0xFF;
        nes.loadAndRun(new byte[]{
            (byte) 0xE8, (byte) 0xE8, (byte) 0x00
        });
        assertEquals(2, nes.registerX & 0xFF);
    }
    @Test
    void test_bcc_branch_taken(){
        DemoNES nes = new DemoNES();
        nes.loadAndRun(new byte[]{
            (byte) 0x90, (byte) 0x02, // BCC +2
            (byte) 0xA9, (byte) 0x01, // LDA #$01 (should be skipped)
            (byte) 0xA9, (byte) 0x02, // LDA #$02 (should be executed)
            (byte) 0x00
        });
        assertEquals(0x02, nes.registerA);
    }
    @Test
    void test_bcs_branch_taken(){
        DemoNES nes = new DemoNES();
        nes.loadAndRun(new byte[]{
            (byte) 0x38, // SEC - Set Carry Flag
            (byte) 0xB0, (byte) 0x02, // BCC +2
            (byte) 0xA9, (byte) 0x01, // LDA #$01 (should be skipped)
            (byte) 0xA9, (byte) 0x02, // LDA #$02 (should be executed)
            (byte) 0x00
        });
        assertEquals(0x02, nes.registerA);
    }
    @Test
    void test_bcs_branch_not_taken(){
        DemoNES nes = new DemoNES();
        nes.loadAndRun(new byte[]{
            (byte) 0xB0, (byte) 0x02, // BCS +2 (not taken)
            (byte) 0xA9, (byte) 0x01, // LDA #$01 (should be executed)
            (byte) 0x00,              // BRK (stop here)
            (byte) 0xA9, (byte) 0x02, // LDA #$02 (should be skipped)
            (byte) 0x00
        });
        assertEquals(0x01, nes.registerA);
    }
    @Test
    void test_bit(){
        DemoNES nes = new DemoNES();
        nes.registerA = 0b00000001;
        nes.memWrite(0x10, (byte)0b11000000);
        nes.loadAndRun(new byte[]{
            (byte)0x24,
            (byte)0x10,
            (byte)0x00
        });
        assertTrue((nes.status & 0b10000000) != 0); // Negative flag should be set
        assertTrue((nes.status & 0b01000000) != 0); // Overflow flag should be set
        assertTrue((nes.status & 0b00000010) != 0); // because A and memory is zero
    }
    @Test
    void test_bmi_branch_taken(){
        DemoNES cpu = new DemoNES();
        cpu.status |= DemoNES.NEGATIVE;  // set negative

        cpu.loadAndRun(new byte[]{
            (byte)0x30,  // BMI
            (byte)0x01,  // +1
            (byte)0x00,  // skipped
            (byte)0x00   // executed
        });
        
        assertEquals(0x8003, cpu.programCounter);
    }
    @Test
    void test_bne_branch_taken() {
        DemoNES cpu = new DemoNES();
        cpu.status &= ~DemoNES.ZERO;  // clear zero (so branch happens)

        cpu.loadAndRun(new byte[]{
            (byte)0xD0,  // BNE
            (byte)0x01,  // +1
            (byte)0x00,  // skipped
            (byte)0x00   // executed
        });

        assertEquals(0x8004, cpu.programCounter);
    }
    @Test
    void test_bne_not_taken() {
        DemoNES cpu = new DemoNES();
        cpu.load(new byte[]{(byte)0xD0, (byte)0x01, (byte)0x00});
        cpu.reset();
        cpu.status |= DemoNES.ZERO;  // set AFTER reset
        cpu.run();
        assertEquals(0x8003, cpu.programCounter);
    }
    @Test
    void test_bpl_branch_taken() {
        DemoNES cpu = new DemoNES();
        cpu.load(new byte[]{
            (byte)0x10,  // BPL
            (byte)0x01,  // +1
            (byte)0x00,  // skipped
            (byte)0x00   // executed
        });
        cpu.reset();
        cpu.status &= ~DemoNES.NEGATIVE;  // clear negative (so branch happens)
        cpu.run();
        assertEquals(0x8004, cpu.programCounter);
    }
    @Test
    void test_bpl_no_branch_taken() {
        DemoNES cpu = new DemoNES();
        cpu.load(new byte[]{
            (byte)0x10,  // BPL
            (byte)0x01,  // +1
            (byte)0x00,  // skipped
            (byte)0x00   // executed
        });
        cpu.reset();
        cpu.status |= DemoNES.NEGATIVE;  // set negative (so branch not taken)
        cpu.run();
        assertEquals(0x8003, cpu.programCounter);
    }
    @Test
    void test_cmp_equal() {
        DemoNES cpu = new DemoNES();

        cpu.load(new byte[]{
            (byte)0xC9, 0x42,
            (byte)0x00
        });
        cpu.reset();
        cpu.registerA = 0x42;  // set AFTER reset
        cpu.run();

        assertTrue((cpu.status & DemoNES.CARRY) != 0);
        assertTrue((cpu.status & DemoNES.ZERO) != 0);
        assertTrue((cpu.status & DemoNES.NEGATIVE) == 0);
    }
}
