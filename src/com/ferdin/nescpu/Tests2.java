package com.ferdin.nescpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class Tests2 {
    private DemoNES nes;

    @BeforeEach
    void setUp() {
        nes = new DemoNES();
    }

    private void loadAndRun(int[] program) {
        nes.load(program);
        nes.reset();
        runUntilBrk();
    }

    private void runUntilBrk() {
        for (int i = 0; i < 10000; i++) {
            int op = nes.memRead(nes.getProgramCounter()) & 0xFF;
            if (op == 0x00) break;
            nes.step();
        }
    }

    // ===== LDA =====
    @Test
    void lda_immediate_loadsValue() {
        loadAndRun(new int[]{0xA9, 0x42, 0x00});
        assertEquals(0x42, nes.getRegisterA());
    }

    @Test
    void lda_setsZeroFlag() {
        loadAndRun(new int[]{0xA9, 0x00, 0x00});
        assertTrue((nes.getStatus() & 0x02) != 0, "Zero flag should be set");
    }

    @Test
    void lda_setsNegativeFlag() {
        loadAndRun(new int[]{0xA9, 0x80, 0x00});
        assertTrue((nes.getStatus() & 0x80) != 0, "Negative flag should be set");
    }

    @Test
    void lda_clearsZeroFlag() {
        loadAndRun(new int[]{0xA9, 0x01, 0x00});
        assertFalse((nes.getStatus() & 0x02) != 0, "Zero flag should be clear");
    }

    @Test
    void lda_zeroPage() {
        loadAndRun(new int[]{
            0xA9, 0x42,   // LDA #$42
            0x85, 0x10,   // STA $10
            0xA9, 0x00,   // LDA #$00
            0xA5, 0x10,   // LDA $10
            0x00
        });
        assertEquals(0x42, nes.getRegisterA());
    }

    // ===== STA =====
    @Test
    void sta_zeroPage_writesMemory() {
        loadAndRun(new int[]{0xA9, 0x42, 0x85, 0x10, 0x00});
        assertEquals(0x42, nes.memRead(0x10));
    }

    // ===== TAX =====
    @Test
    void tax_transfersAToX() {
        loadAndRun(new int[]{0xA9, 0x42, 0xAA, 0x00});
        assertEquals(0x42, nes.getRegisterX());
    }

    @Test
    void tax_setsZeroFlag() {
        loadAndRun(new int[]{0xA9, 0x00, 0xAA, 0x00});
        assertTrue((nes.getStatus() & 0x02) != 0, "Zero flag should be set");
    }

    @Test
    void tax_setsNegativeFlag() {
        loadAndRun(new int[]{0xA9, 0x80, 0xAA, 0x00});
        assertTrue((nes.getStatus() & 0x80) != 0, "Negative flag should be set");
    }

    // ===== INX =====
    @Test
    void inx_incrementsX() {
        loadAndRun(new int[]{0xA9, 0x41, 0xAA, 0xE8, 0x00});
        assertEquals(0x42, nes.getRegisterX());
    }

    @Test
    void inx_overflow_wrapsToZero() {
        loadAndRun(new int[]{0xA9, 0xFF, 0xAA, 0xE8, 0x00});
        assertEquals(0x00, nes.getRegisterX());
    }

    @Test
    void inx_overflow_setsZeroFlag() {
        loadAndRun(new int[]{0xA9, 0xFF, 0xAA, 0xE8, 0x00});
        assertTrue((nes.getStatus() & 0x02) != 0, "Zero flag should be set on overflow");
    }

    // ===== CMP =====
    @Test
    void cmp_equal_setsZeroFlag() {
        loadAndRun(new int[]{0xA9, 0x42, 0xC9, 0x42, 0x00});
        assertTrue((nes.getStatus() & 0x02) != 0, "Zero flag should be set when equal");
    }

    @Test
    void cmp_equal_setsCarryFlag() {
        loadAndRun(new int[]{0xA9, 0x42, 0xC9, 0x42, 0x00});
        assertTrue((nes.getStatus() & 0x01) != 0, "Carry flag should be set when A >= value");
    }

    @Test
    void cmp_greater_setsCarryFlag() {
        loadAndRun(new int[]{0xA9, 0x42, 0xC9, 0x10, 0x00});
        assertTrue((nes.getStatus() & 0x01) != 0, "Carry flag should be set when A > value");
    }

    @Test
    void cmp_less_clearsCarryFlag() {
        loadAndRun(new int[]{0xA9, 0x10, 0xC9, 0x42, 0x00});
        assertFalse((nes.getStatus() & 0x01) != 0, "Carry flag should be clear when A < value");
    }

    // ===== BEQ =====
    @Test
    void beq_taken_whenZeroFlagSet() {
        loadAndRun(new int[]{
            0xA9, 0x42,   // LDA #$42
            0xC9, 0x42,   // CMP #$42  (sets zero flag)
            0xF0, 0x02,   // BEQ +2   (should branch)
            0xA9, 0x00,   // LDA #$00  (skipped)
            0xA9, 0x01,   // LDA #$01
            0x00
        });
        assertEquals(0x01, nes.getRegisterA());
    }

    @Test
    void beq_notTaken_whenZeroFlagClear() {
        loadAndRun(new int[]{
            0xA9, 0x42,   // LDA #$42
            0xC9, 0x11,   // CMP #$11  (clears zero flag)
            0xF0, 0x02,   // BEQ +2   (should NOT branch)
            0xA9, 0x07,   // LDA #$07  (should run)
            0x00
        });
        assertEquals(0x07, nes.getRegisterA());
    }

    // ===== BNE =====
    @Test
    void bne_taken_whenZeroFlagClear() {
        loadAndRun(new int[]{
            0xA9, 0x42,   // LDA #$42
            0xC9, 0x11,   // CMP #$11  (clears zero flag)
            0xD0, 0x02,   // BNE +2   (should branch)
            0xA9, 0x00,   // LDA #$00  (skipped)
            0xA9, 0x01,   // LDA #$01
            0x00
        });
        assertEquals(0x01, nes.getRegisterA());
    }

    @Test
    void bne_notTaken_whenZeroFlagSet() {
        loadAndRun(new int[]{
            0xA9, 0x42,   // LDA #$42
            0xC9, 0x42,   // CMP #$42  (sets zero flag)
            0xD0, 0x02,   // BNE +2   (should NOT branch)
            0xA9, 0x07,   // LDA #$07  (should run)
            0x00
        });
        assertEquals(0x07, nes.getRegisterA());
    }

    // ===== BIT =====
    @Test
    void bit_clearsZeroFlag_whenAndIsNonZero() {
        loadAndRun(new int[]{
            0xA9, 0x04,   // LDA #$04
            0x85, 0x10,   // STA $10
            0xA9, 0x04,   // LDA #$04
            0x24, 0x10,   // BIT $10  (0x04 & 0x04 = 0x04, non-zero)
            0x00
        });
        assertFalse((nes.getStatus() & 0x02) != 0, "Zero flag should be clear");
    }

    @Test
    void bit_setsZeroFlag_whenAndIsZero() {
        loadAndRun(new int[]{
            0xA9, 0x04,   // LDA #$04
            0x85, 0x10,   // STA $10
            0xA9, 0x08,   // LDA #$08
            0x24, 0x10,   // BIT $10  (0x08 & 0x04 = 0x00)
            0x00
        });
        assertTrue((nes.getStatus() & 0x02) != 0, "Zero flag should be set");
    }

    @Test
    void bit_setsNegativeFlag_fromBit7ofMemory() {
        loadAndRun(new int[]{
            0xA9, 0x80,   // LDA #$80
            0x85, 0x10,   // STA $10
            0xA9, 0xFF,   // LDA #$FF
            0x24, 0x10,   // BIT $10
            0x00
        });
        assertTrue((nes.getStatus() & 0x80) != 0, "Negative flag should be set from bit 7");
    }

    @Test
    void bit_setsOverflowFlag_fromBit6ofMemory() {
        loadAndRun(new int[]{
            0xA9, 0x40,   // LDA #$40
            0x85, 0x10,   // STA $10
            0xA9, 0xFF,   // LDA #$FF
            0x24, 0x10,   // BIT $10
            0x00
        });
        assertTrue((nes.getStatus() & 0x40) != 0, "Overflow flag should be set from bit 6");
    }

    // ===== JSR / RTS =====
    @Test
    void jsr_rts_returnsCorrectly() {
        loadAndRun(new int[]{
            0x20, 0x06, 0x06, // JSR $0606
            0xA9, 0x42,       // LDA #$42  (runs after RTS)
            0x00,             // BRK
            0x60              // RTS
        });
        assertEquals(0x42, nes.getRegisterA());
    }

    @Test
    void jsr_rts_restoresStackPointer() {
        loadAndRun(new int[]{
            0x20, 0x06, 0x06, // JSR $0606
            0x00,             // BRK
            0x00, 0x00,       // padding
            0x60              // RTS
        });
        assertEquals(0xFD, nes.getStackPointer());
    }

    // ===== AND =====
    @Test
    void and_immediate() {
        loadAndRun(new int[]{0xA9, 0xFF, 0x29, 0x03, 0x00});
        assertEquals(0x03, nes.getRegisterA());
    }

    @Test
    void and_setsZeroFlag() {
        loadAndRun(new int[]{0xA9, 0xF0, 0x29, 0x0F, 0x00});
        assertTrue((nes.getStatus() & 0x02) != 0, "Zero flag should be set");
    }

    // ===== ADC =====
    @Test
    void adc_immediate() {
        loadAndRun(new int[]{0xA9, 0x10, 0x69, 0x10, 0x00});
        assertEquals(0x20, nes.getRegisterA());
    }

    @Test
    void adc_setsCarryOnOverflow() {
        loadAndRun(new int[]{0xA9, 0xFF, 0x69, 0x01, 0x00});
        assertTrue((nes.getStatus() & 0x01) != 0, "Carry flag should be set");
        assertEquals(0x00, nes.getRegisterA());
    }

    // ===== LSR (used in snake game for direction) =====
    @Test
    void lsr_accumulator_shiftsRight() {
        loadAndRun(new int[]{0xA9, 0x04, 0x4A, 0x00}); // LDA #$04, LSR A
        assertEquals(0x02, nes.getRegisterA());
    }

    @Test
    void lsr_setsCarryFromBit0() {
        loadAndRun(new int[]{0xA9, 0x01, 0x4A, 0x00}); // LDA #$01, LSR A
        assertTrue((nes.getStatus() & 0x01) != 0, "Carry should be set from bit 0");
        assertEquals(0x00, nes.getRegisterA());
    }
}
