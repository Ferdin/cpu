package main.java.com.ferdin.nes.cpu;
public class Cycles {
    public static final int[] CYCLES = new int[256];  // Placeholder for cycle counts of each opcode

    static {
        // ADC
        CYCLES[0x69] = 2;
        CYCLES[0x65] = 3;
        CYCLES[0x75] = 4;
        CYCLES[0x6D] = 4;
        CYCLES[0x7D] = 4; // +1 if page crossed
        CYCLES[0x79] = 4; // +1 if page crossed
        CYCLES[0x61] = 6;
        CYCLES[0x71] = 5; // +1 if page crossed

        // AND
        CYCLES[0x29] = 2;
        CYCLES[0x25] = 3;
        CYCLES[0x35] = 4;
        CYCLES[0x2D] = 4;
        CYCLES[0x3D] = 4; // +1 if page crossed
        CYCLES[0x39] = 4; // +1 if page crossed
        CYCLES[0x21] = 6;
        CYCLES[0x31] = 5; // +1 if page crossed

        // ASL
        CYCLES[0x0A] = 2;
        CYCLES[0x06] = 5;
        CYCLES[0x16] = 6;
        CYCLES[0x0E] = 6;
        CYCLES[0x1E] = 7;

        // BCC
        CYCLES[0x90] = 2; // +1 if branch taken, +2 if to a new page

        // BCS
        CYCLES[0xB0] = 2; // +1 if branch taken, +2 if to a new page

        // BEQ
        CYCLES[0xF0] = 2; // +1 if branch taken, +2 if to a new page

        // BIT
        CYCLES[0x24] = 3;
        CYCLES[0x2C] = 4;

        // BMI
        CYCLES[0x30] = 2; // +1 if branch taken, +2 if to a new page

        // BNE
        CYCLES[0xD0] = 2; // +1 if branch taken, +2 if to a new page

        // BPL
        CYCLES[0x10] = 2; // +1 if branch taken, +2 if to a new page

        // BRK
        CYCLES[0x00] = 7;

        // BVC
        CYCLES[0x50] = 2; // +1 if branch taken, +2 if to a new page

        // BVS
        CYCLES[0x70] = 2; // +1 if branch taken, +2 if to a new page

        // CLC
        CYCLES[0x18] = 2;

        // CLD
        CYCLES[0xD8] = 2;

        // CLI
        CYCLES[0x58] = 2;

        // CLV
        CYCLES[0xB8] = 2;

        // CMP
        CYCLES[0xC9] = 2;
        CYCLES[0xC5] = 3;
        CYCLES[0xD5] = 4;
        CYCLES[0xCD] = 4;
        CYCLES[0xDD] = 4; // +1 if page crossed
        CYCLES[0xD9] = 4; // +1 if page crossed
        CYCLES[0xC1] = 6;
        CYCLES[0xD1] = 5; // +1 if page crossed

        // CPX
        CYCLES[0xE0] = 2;
        CYCLES[0xE4] = 3;
        CYCLES[0xEC] = 4;

        // CPY
        CYCLES[0xC0] = 2;
        CYCLES[0xC4] = 3;
        CYCLES[0xCC] = 4;

        // DEC
        CYCLES[0xC6] = 5;
        CYCLES[0xD6] = 6;
        CYCLES[0xCE] = 6;
        CYCLES[0xDE] = 7;

        // DEX
        CYCLES[0xCA] = 2;

        // DEY
        CYCLES[0x88] = 2;

        // EOR
        CYCLES[0x49] = 2;
        CYCLES[0x45] = 3;
        CYCLES[0x55] = 4;
        CYCLES[0x4D] = 4;
        CYCLES[0x5D] = 4; // +1 if page crossed
        CYCLES[0x59] = 4; // +1 if page crossed
        CYCLES[0x41] = 6;
        CYCLES[0x51] = 5; // +1 if page crossed

        // INC
        CYCLES[0xE6] = 5;
        CYCLES[0xF6] = 6;
        CYCLES[0xEE] = 6;
        CYCLES[0xFE] = 7;

        // INX
        CYCLES[0xE8] = 2;

        // INY
        CYCLES[0xC8] = 2;

        // JMP
        CYCLES[0x4C] = 3;
        CYCLES[0x6C] = 5;

        // JSR
        CYCLES[0x20] = 6;

        // LDA 
        CYCLES[0xA9] = 2;
        CYCLES[0xA5] = 3;
        CYCLES[0xB5] = 4;
        CYCLES[0xAD] = 4;
        CYCLES[0xBD] = 4; // +1 if page crossed
        CYCLES[0xB9] = 4; // +1 if page crossed
        CYCLES[0xA1] = 6;
        CYCLES[0xB1] = 5; // +1 if page crossed

        // LDX
        CYCLES[0xA2] = 2;
        CYCLES[0xA6] = 3;
        CYCLES[0xB6] = 4;
        CYCLES[0xAE] = 4;
        CYCLES[0xBE] = 4; // +1 if page crossed

        // LDY
        CYCLES[0xA0] = 2;
        CYCLES[0xA4] = 3;
        CYCLES[0xB4] = 4;
        CYCLES[0xAC] = 4;
        CYCLES[0xBC] = 4; // +1 if page crossed

        // LSR
        CYCLES[0x4A] = 2;
        CYCLES[0x46] = 5;
        CYCLES[0x56] = 6;
        CYCLES[0x4E] = 6;
        CYCLES[0x5E] = 7;

        // NOP
        CYCLES[0xEA] = 2;

        // ORA
        CYCLES[0x09] = 2;
        CYCLES[0x05] = 3;
        CYCLES[0x15] = 4;
        CYCLES[0x0D] = 4;
        CYCLES[0x1D] = 4; // +1 if page crossed
        CYCLES[0x19] = 4; // +1 if page crossed
        CYCLES[0x01] = 6;
        CYCLES[0x11] = 5; // +1 if page crossed

        // PHA
        CYCLES[0x48] = 3;

        // PHP
        CYCLES[0x08] = 3;

        // PLA
        CYCLES[0x68] = 4;

        // PLP
        CYCLES[0x28] = 4;

        // ROL
        CYCLES[0x2A] = 2;
        CYCLES[0x26] = 5;
        CYCLES[0x36] = 6;
        CYCLES[0x2E] = 6;
        CYCLES[0x3E] = 7;

        // ROR
        CYCLES[0x6A] = 2;
        CYCLES[0x66] = 5;
        CYCLES[0x76] = 6;
        CYCLES[0x6E] = 6;
        CYCLES[0x7E] = 7;
        
        // RTI
        CYCLES[0x40] = 6;

        // RTS
        CYCLES[0x60] = 6;

        // SBC
        CYCLES[0xE9] = 2;
        CYCLES[0xE5] = 3;
        CYCLES[0xF5] = 4;
        CYCLES[0xED] = 4;
        CYCLES[0xFD] = 4; // +1 if page crossed
        CYCLES[0xF9] = 4; // +1 if page crossed
        CYCLES[0xE1] = 6;
        CYCLES[0xF1] = 5; // +1 if page crossed

        // SEC
        CYCLES[0x38] = 2;

        // SED
        CYCLES[0xF8] = 2;

        // SEI
        CYCLES[0x78] = 2;

        // STA
        CYCLES[0x85] = 3;
        CYCLES[0x95] = 4;
        CYCLES[0x8D] = 4;
        CYCLES[0x9D] = 5; 
        CYCLES[0x99] = 5;
        CYCLES[0x81] = 6;
        CYCLES[0x91] = 6;

        // STX
        CYCLES[0x86] = 3;
        CYCLES[0x96] = 4;
        CYCLES[0x8E] = 4;

        // STY
        CYCLES[0x84] = 3;
        CYCLES[0x94] = 4;
        CYCLES[0x8C] = 4;

        // TAX
        CYCLES[0xAA] = 2;

        // TAY
        CYCLES[0xA8] = 2;

        // TSX
        CYCLES[0xBA] = 2;

        // TXA
        CYCLES[0x8A] = 2;

        // TXS
        CYCLES[0x9A] = 2;

        // TYA
        CYCLES[0x98] = 2;
    }

    public int getCyclesForOpcode(int opcode) {
        return CYCLES[opcode];
    }
}
