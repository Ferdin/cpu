package test.java.com.ferdin.nes;

import main.java.com.ferdin.nes.apu.APU;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class APUTest {

    private APU apu;

    @BeforeEach
    void setUp() {
        apu = new APU(); // no AudioOutput — silent mode for testing
    }

    // =========================================================
    // APU Frame Counter Tests
    // =========================================================

    @Test
    void testFrameCounterDefaultMode() {
        // Default mode is 4-step (mode 0)
        // Writing 0x00 to $4017 sets 4-step mode
        apu.writeRegister(0x4017, 0x00);
        // Tick to step 1 (7457 cycles) — should trigger quarter frame
        for (int i = 0; i < 7457; i++) apu.tick();
        // No assertion needed — just confirm no crash
        // Quarter frame clocks envelopes and linear counter
    }

    @Test
    void testFrameCounterFiveStepMode() {
        // Writing 0x80 to $4017 sets 5-step mode
        apu.writeRegister(0x4017, 0x80);
        // Should not throw
        for (int i = 0; i < 37282; i++) apu.tick();
    }

    @Test
    void testFrameCounterIrqInhibit() {
        // Writing bit 6 of $4017 inhibits IRQ
        apu.writeRegister(0x4017, 0x40);
        // Tick through full 4-step sequence
        for (int i = 0; i < 29830; i++) apu.tick();
        assertFalse(apu.isFrameIrq(), "IRQ should be inhibited");
    }

    @Test
    void testFrameCounterIrqFires() {
        // 4-step mode without IRQ inhibit should set frameIrq at step 4
        apu.writeRegister(0x4017, 0x00); // 4-step, no inhibit
        for (int i = 0; i < 29829; i++) apu.tick();
        assertTrue(apu.isFrameIrq(), "Frame IRQ should fire at cycle 29829");
    }

    @Test
    void testReadStatusClearsIrq() {
        apu.writeRegister(0x4017, 0x00);
        for (int i = 0; i < 29829; i++) apu.tick();
        assertTrue(apu.isFrameIrq());
        apu.readStatus(); // reading status clears IRQ
        assertFalse(apu.isFrameIrq(), "Reading status should clear frame IRQ");
    }

    // =========================================================
    // APU Status Register Tests
    // =========================================================

    @Test
    void testStatusRegisterEnableChannels() {
        // $4015 = 0x1F enables all 5 channels
        apu.writeRegister(0x4015, 0x1F);
        // All channels enabled — length counters start at 0 until programmed
        // Status should show 0 since length counters are 0
        int status = apu.readStatus();
        assertEquals(0, status & 0x1F, "Length counters should be 0 before programming");
    }

    @Test
    void testStatusRegisterDisableChannels() {
        // Enable then disable pulse1
        apu.writeRegister(0x4015, 0x01); // enable pulse1
        apu.writeRegister(0x4000, 0x3F); // program pulse1
        apu.writeRegister(0x4003, 0x08); // set length counter
        apu.writeRegister(0x4015, 0x00); // disable all
        int status = apu.readStatus();
        assertEquals(0, status & 0x01, "Pulse1 length counter should be 0 after disable");
    }

    // =========================================================
    // Pulse Channel Tests
    // =========================================================

    @Test
    void testPulseChannelSilentWhenDisabled() {
        // Pulse channel should output 0 when disabled
        apu.writeRegister(0x4000, 0x3F); // max volume
        apu.writeRegister(0x4002, 0x00); // timer low
        apu.writeRegister(0x4003, 0x08); // timer high + length
        // Do NOT write $4015 to enable
        float sample = getPulse1Sample();
        assertEquals(0.0f, sample, "Disabled pulse should output 0");
    }

    @Test
    void testPulseChannelOutputsWhenEnabled() {
        // Enable pulse1 and program it
        apu.writeRegister(0x4015, 0x01); // enable pulse1
        apu.writeRegister(0x4000, 0b00111111); // 50% duty, constant vol 15
        apu.writeRegister(0x4002, 0x40); // timer low — audible frequency
        apu.writeRegister(0x4003, 0x08); // length counter load

        // Tick enough to advance sequencer past first step
        for (int i = 0; i < 200; i++) apu.tick();

        // At some point the duty step should be non-zero and output > 0
        boolean anyNonZero = false;
        for (int i = 0; i < 1000; i++) {
            apu.tick();
            if (getPulse1Sample() > 0) {
                anyNonZero = true;
                break;
            }
        }
        assertTrue(anyNonZero, "Pulse1 should output non-zero samples when enabled");
    }

    @Test
    void testPulseChannelSilentWhenLengthCounterExpires() {
        apu.writeRegister(0x4015, 0x01);
        apu.writeRegister(0x4000, 0b00011111); // no length halt, constant vol 15
        apu.writeRegister(0x4002, 0x40);
        apu.writeRegister(0x4003, 0x08); // length = 10 (from LENGTH_TABLE[1])

        // Tick enough half-frames to expire the length counter (10 half frames)
        // Each half frame = 14913 cycles
        for (int i = 0; i < 14913 * 11; i++) apu.tick();

        assertEquals(0.0f, getPulse1Sample(),
            "Pulse1 should be silent after length counter expires");
    }

    @Test
    void testPulseChannelLengthCounterHalt() {
        apu.writeRegister(0x4015, 0x01);
        apu.writeRegister(0x4000, 0b00111111); // length counter HALT set (bit 5)
        apu.writeRegister(0x4002, 0x40);
        apu.writeRegister(0x4003, 0x08); // length = 10

        // Tick many half frames — length counter should not expire
        for (int i = 0; i < 14913 * 20; i++) apu.tick();

        boolean anyNonZero = false;
        for (int i = 0; i < 1000; i++) {
            apu.tick();
            if (getPulse1Sample() > 0) {
                anyNonZero = true;
                break;
            }
        }
        assertTrue(anyNonZero, "Pulse1 with halted length counter should keep playing");
    }

    @Test
    void testPulseChannelDutyCycles() {
        apu.writeRegister(0x4015, 0x01);
        apu.writeRegister(0x4002, 0x08); // low period for fast cycling
        apu.writeRegister(0x4003, 0xF8); // max length

        // Test each duty mode produces output
        for (int duty = 0; duty < 4; duty++) {
            apu.writeRegister(0x4000, (duty << 6) | 0b00111111);
            boolean anyNonZero = false;
            for (int i = 0; i < 500; i++) {
                apu.tick();
                if (getPulse1Sample() > 0) {
                    anyNonZero = true;
                    break;
                }
            }
            assertTrue(anyNonZero, "Duty mode " + duty + " should produce output");
        }
    }

    @Test
    void testPulseChannelConstantVolume() {
        apu.writeRegister(0x4015, 0x01);
        apu.writeRegister(0x4000, 0b00110111); // constant volume = 7 (half max)
        apu.writeRegister(0x4002, 0x08);
        apu.writeRegister(0x4003, 0xF8);

        // Advance to a point where duty step is high
        for (int i = 0; i < 500; i++) apu.tick();

        float maxSample = 0;
        for (int i = 0; i < 1000; i++) {
            apu.tick();
            maxSample = Math.max(maxSample, getPulse1Sample());
        }
        assertEquals(7.0f / 15.0f, maxSample, 0.001f,
            "Constant volume 7 should output 7/15");
    }

    @Test
    void testPulse2IndependentFromPulse1() {
        // Enable only pulse2
        apu.writeRegister(0x4015, 0x02);
        apu.writeRegister(0x4004, 0b00111111);
        apu.writeRegister(0x4006, 0x40);
        apu.writeRegister(0x4007, 0x08);

        for (int i = 0; i < 500; i++) apu.tick();

        assertEquals(0.0f, getPulse1Sample(), "Pulse1 should be silent");
        boolean anyNonZero = false;
        for (int i = 0; i < 1000; i++) {
            apu.tick();
            if (getPulse2Sample() > 0) {
                anyNonZero = true;
                break;
            }
        }
        assertTrue(anyNonZero, "Pulse2 should output when enabled");
    }

    // =========================================================
    // Triangle Channel Tests
    // =========================================================

    @Test
    void testTriangleChannelSilentWhenDisabled() {
        apu.writeRegister(0x4008, 0xFF);
        apu.writeRegister(0x400A, 0x40);
        apu.writeRegister(0x400B, 0x08);
        // No $4015 enable
        for (int i = 0; i < 500; i++) apu.tick();
        assertEquals(0.0f, getTriangleSample(), "Disabled triangle should output 0");
    }

    @Test
    void testTriangleChannelOutputsWhenEnabled() {
        apu.writeRegister(0x4015, 0x04); // enable triangle
        apu.writeRegister(0x4008, 0xFF); // control flag + max linear counter
        apu.writeRegister(0x400A, 0x40); // timer low
        apu.writeRegister(0x400B, 0x08); // timer high + length

        for (int i = 0; i < 200; i++) apu.tick();

        boolean anyNonZero = false;
        for (int i = 0; i < 2000; i++) {
            apu.tick();
            if (getTriangleSample() > 0) {
                anyNonZero = true;
                break;
            }
        }
        assertTrue(anyNonZero, "Triangle should output non-zero when enabled");
    }

    @Test
    void testTriangleMutesOnUltrasonicFrequency() {
        apu.writeRegister(0x4015, 0x04);
        apu.writeRegister(0x4008, 0xFF);
        apu.writeRegister(0x400A, 0x01); // timerPeriod = 1 — ultrasonic, should mute
        apu.writeRegister(0x400B, 0x08);
        for (int i = 0; i < 500; i++) apu.tick();
        assertEquals(0.0f, getTriangleSample(),
            "Triangle should mute at ultrasonic frequencies (period < 2)");
    }

    @Test
    void testTriangleLinearCounter() {
        apu.writeRegister(0x4015, 0x04);
        apu.writeRegister(0x4008, 0x01); // linear counter period = 1, no control flag
        apu.writeRegister(0x400A, 0x40);
        apu.writeRegister(0x400B, 0x08);

        // After 2 quarter frames the linear counter should reach 0
        for (int i = 0; i < 7457 * 3; i++) apu.tick();

        assertEquals(0.0f, getTriangleSample(),
            "Triangle should silence when linear counter expires");
    }

    // =========================================================
    // Noise Channel Tests
    // =========================================================

    @Test
    void testNoiseChannelSilentWhenDisabled() {
        apu.writeRegister(0x400C, 0x3F);
        apu.writeRegister(0x400E, 0x00);
        apu.writeRegister(0x400F, 0x08);
        for (int i = 0; i < 500; i++) apu.tick();
        assertEquals(0.0f, getNoiseSample(), "Disabled noise should output 0");
    }

    @Test
    void testNoiseChannelOutputsWhenEnabled() {
        apu.writeRegister(0x4015, 0x08); // enable noise
        apu.writeRegister(0x400C, 0b00111111); // constant vol 15, length halt
        apu.writeRegister(0x400E, 0x00); // shortest period
        apu.writeRegister(0x400F, 0xF8); // max length

        boolean anyNonZero = false;
        for (int i = 0; i < 5000; i++) {
            apu.tick();
            if (getNoiseSample() > 0) {
                anyNonZero = true;
                break;
            }
        }
        assertTrue(anyNonZero, "Noise should output non-zero samples when enabled");
    }

    @Test
    void testNoiseShiftRegisterInitialValue() {
        // LFSR starts at 1, bit 0 = 1, so first output should be non-zero
        apu.writeRegister(0x4015, 0x08);
        apu.writeRegister(0x400C, 0b00111111);
        apu.writeRegister(0x400E, 0x00);
        apu.writeRegister(0x400F, 0xF8);
        // Before any ticks, shift register bit 0 = 1
        assertTrue(getNoiseSample() > 0,
            "Noise LFSR bit 0 starts at 1 so initial output should be non-zero");
    }

    @Test
    void testNoiseModeFlag() {
        // Mode 0 (normal) and mode 1 (metallic) should both produce output
        apu.writeRegister(0x4015, 0x08);
        apu.writeRegister(0x400C, 0b00111111);
        apu.writeRegister(0x400F, 0xF8);

        // Mode 0
        apu.writeRegister(0x400E, 0x00);
        boolean modeZeroNonZero = false;
        for (int i = 0; i < 5000; i++) {
            apu.tick();
            if (getNoiseSample() > 0) { modeZeroNonZero = true; break; }
        }

        // Mode 1
        apu.writeRegister(0x400E, 0x80);
        boolean modeOneNonZero = false;
        for (int i = 0; i < 5000; i++) {
            apu.tick();
            if (getNoiseSample() > 0) { modeOneNonZero = true; break; }
        }

        assertTrue(modeZeroNonZero, "Mode 0 noise should produce output");
        assertTrue(modeOneNonZero,  "Mode 1 noise should produce output");
    }

    // =========================================================
    // DMC Channel Tests
    // =========================================================

    @Test
    void testDMCDirectLoad() {
        // $4011 direct load sets output level immediately
        apu.writeRegister(0x4011, 0x40); // load 64 (half of 127)
        float expected = 64.0f / 127.0f;
        assertEquals(expected, getDMCSample(), 0.001f,
            "DMC direct load should set output level immediately");
    }

    @Test
    void testDMCDirectLoadMax() {
        apu.writeRegister(0x4011, 0x7F); // max value (7-bit = 127)
        assertEquals(1.0f, getDMCSample(), 0.001f,
            "DMC direct load 127 should output 1.0");
    }

    @Test
    void testDMCDirectLoadZero() {
        apu.writeRegister(0x4011, 0x7F); // set to max first
        apu.writeRegister(0x4011, 0x00); // then clear
        assertEquals(0.0f, getDMCSample(), 0.001f,
            "DMC direct load 0 should output 0.0");
    }

    @Test
    void testDMCSilentWhenDisabled() {
        apu.writeRegister(0x4010, 0x0F); // max rate
        apu.writeRegister(0x4013, 0xFF); // max length
        // No $4015 enable
        for (int i = 0; i < 500; i++) apu.tick();
        // bytesRemaining should be 0
        assertEquals(0, getDMCBytesRemaining(),
            "DMC bytes remaining should be 0 when disabled");
    }

    @Test
    void testDMCMemoryReader() {
        int[] fakeRom = new int[0x10000];
        for (int i = 0xC000; i < 0xD000; i++) {
            fakeRom[i] = (i % 2 == 0) ? 0xFF : 0x00;
        }
        apu.getDmc().setMemoryReader(addr -> fakeRom[addr & 0xFFFF]);

        // Write sample config BEFORE enabling
        apu.writeRegister(0x4010, 0x0F); // rate
        apu.writeRegister(0x4012, 0x00); // sample address = $C000
        apu.writeRegister(0x4013, 0x10); // sample length = 257 bytes
        apu.writeRegister(0x4015, 0x10); // enable LAST — now sampleLength is set

        for (int i = 0; i < 50000; i++) apu.tick();

        assertNotEquals(0.0f, getDMCSample(),
            "DMC should produce output when memory reader is set");
    }

    // =========================================================
    // Mixing Tests
    // =========================================================

    @Test
    void testMixerAllSilentOutputsZero() {
        // No channels enabled — output should be 0
        float sample = apu.getSample();
        assertEquals(0.0f, sample, 0.001f, "All silent channels should mix to 0");
    }

    @Test
    void testMixerPulseOnlyOutput() {
        apu.writeRegister(0x4015, 0x01);
        apu.writeRegister(0x4000, 0b00111111);
        apu.writeRegister(0x4002, 0x08);
        apu.writeRegister(0x4003, 0xF8);

        float maxSample = 0;
        for (int i = 0; i < 2000; i++) {
            apu.tick();
            maxSample = Math.max(maxSample, apu.getSample());
        }
        assertTrue(maxSample > 0, "Pulse only mix should be > 0");
        assertTrue(maxSample < 1.0f, "Mix output should be < 1.0 (normalized)");
    }

    @Test
    void testMixerOutputInValidRange() {
        // Enable all channels with max volume
        apu.writeRegister(0x4015, 0x1F);
        apu.writeRegister(0x4000, 0b00111111);
        apu.writeRegister(0x4002, 0x08);
        apu.writeRegister(0x4003, 0xF8);
        apu.writeRegister(0x4004, 0b00111111);
        apu.writeRegister(0x4006, 0x08);
        apu.writeRegister(0x4007, 0xF8);
        apu.writeRegister(0x4008, 0xFF);
        apu.writeRegister(0x400A, 0x08);
        apu.writeRegister(0x400B, 0xF8);
        apu.writeRegister(0x400C, 0b00111111);
        apu.writeRegister(0x400E, 0x00);
        apu.writeRegister(0x400F, 0xF8);
        apu.writeRegister(0x4011, 0x7F);

        for (int i = 0; i < 5000; i++) apu.tick();

        float sample = apu.getSample();
        assertTrue(sample >= 0.0f, "Mix output should be >= 0");
        assertTrue(sample <= 1.0f, "Mix output should be <= 1.0");
    }

    // =========================================================
    // Helper methods — access channel samples directly
    // =========================================================

    private float getPulse1Sample() {
        return apu.getPulse1().getSample();
    }

    private float getPulse2Sample() {
        return apu.getPulse2().getSample();
    }

    private float getTriangleSample() {
        return apu.getTriangle().getSample();
    }

    private float getNoiseSample() {
        return apu.getNoise().getSample();
    }

    private float getDMCSample() {
        return apu.getDmc().getSample();
    }

    private int getDMCBytesRemaining() {
        return apu.getDmc().bytesRemaining;
    }
}