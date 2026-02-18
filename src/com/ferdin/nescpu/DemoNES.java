package com.ferdin.nescpu;

public class DemoNES {
    // I created this class to learn NESCPU from bugzmanov/nes_ebook
//https://github.com/bugzmanov/nes_ebook/blob/master/src/chapter_3_2.md
        // CPU Registers (8-bit)
        public int registerA;
        public int registerX;
        public int registerY;
        public int stackPointer;
        public int status;

        // Program Counter (16-bit)
        public int programCounter;

        // Stack
        public int stack = 0x0100;
        public int stack_reset = 0xFD;

        // 64KB Memory
        private byte[] memory;

        // Flags
        private boolean carryFlag = false;

        // Flag bit masks
        public static final int CARRY             = 0b00000001;
        public static final int ZERO              = 0b00000010;
        public static final int INTERRUPT_DISABLE = 0b00000100;
        public static final int DECIMAL_MODE      = 0b00001000;
        public static final int BREAK             = 0b00010000;
        public static final int BREAK2            = 0b00100000;
        public static final int OVERFLOW          = 0b01000000;
        public static final int NEGATIVE          = 0b10000000;

        public enum AddressingMode {
            IMMEDIATE,
            ZERO_PAGE,
            ZERO_PAGE_X,
            ZERO_PAGE_Y,
            ABSOLUTE,
            ABSOLUTE_X,
            ABSOLUTE_Y,
            INDIRECT_X,
            INDIRECT_Y,
            NONE_ADDRESSING
        }

        // In your DemoNES class, add:

        private int getOperandAddress(AddressingMode mode) {
            switch (mode) {
                case IMMEDIATE:
                    return programCounter;
                    
                case ZERO_PAGE:
                    return memRead(programCounter) & 0xFF;
                    
                case ABSOLUTE:
                    return memReadU16(programCounter);
                    
                case ZERO_PAGE_X: {
                    int pos = memRead(programCounter) & 0xFF;
                    int addr = (pos + registerX) & 0xFF;  // Wrapping add, keep in zero page
                    return addr;
                }
                
                case ZERO_PAGE_Y: {
                    int pos = memRead(programCounter) & 0xFF;
                    int addr = (pos + registerY) & 0xFF;  // Wrapping add, keep in zero page
                    return addr;
                }
                
                case ABSOLUTE_X: {
                    int base = memReadU16(programCounter);
                    int addr = (base + registerX) & 0xFFFF;  // Wrapping add
                    return addr;
                }
                
                case ABSOLUTE_Y: {
                    int base = memReadU16(programCounter);
                    int addr = (base + registerY) & 0xFFFF;  // Wrapping add
                    return addr;
                }
                
                case INDIRECT_X: {
                    int base = memRead(programCounter) & 0xFF;
                    int ptr = (base + registerX) & 0xFF;  // Wrapping add
                    int lo = memRead(ptr) & 0xFF;
                    int hi = memRead((ptr + 1) & 0xFF) & 0xFF;  // Wrapping add
                    return (hi << 8) | lo;
                }
                
                case INDIRECT_Y: {
                    int base = memRead(programCounter) & 0xFF;
                    int lo = memRead(base) & 0xFF;
                    int hi = memRead((base + 1) & 0xFF) & 0xFF;  // Wrapping add
                    int derefBase = (hi << 8) | lo;
                    int deref = (derefBase + registerY) & 0xFFFF;  // Wrapping add
                    return deref;
                }
                
                case NONE_ADDRESSING:
                    throw new UnsupportedOperationException("AddressingMode " + mode + " is not supported");
                    
                default:
                    throw new UnsupportedOperationException("Unknown addressing mode: " + mode);
            }
        }
        public DemoNES() {
            this.memory = new byte[0xFFFF];
            reset();
        }

        public void reset() {
            registerA = 0;
            registerX = 0;
            status = 0;
            programCounter = memReadU16(0xFFFC); // Reset vector
        }

         // Equivalent to Rust’s mem_read
        public int memRead(int addr) {
            return memory[addr & 0xFFFF] & 0xFF;
        }

        // Equivalent to Rust’s mem_write
        public void memWrite(int addr, byte data) {
            memory[addr & 0xFFFF] = data;
        }

        // Loads program into memory at a default start address
        public void loadAndRun(byte[] program) {
            load(program);
            reset();
            run();
        }

        public void load(byte[] program) {
            // Copy program into memory starting at 0x8000
            System.arraycopy(program, 0, memory, 0x8000, program.length);
            memWriteU16(0xFFFC, 0x8000);
        }

        private int memReadU16(int pos) {
            int lo = memRead(pos) & 0xFF;  // Treat as unsigned byte
            int hi = memRead(pos + 1) & 0xFF;  // Treat as unsigned byte
            return (hi << 8) | lo;
        }

        private void memWriteU16(int pos, int data) {
            byte hi = (byte)((data >> 8) & 0xFF);
            byte lo = (byte)(data & 0xFF);
            memWrite(pos, lo);
            memWrite(pos + 1, hi);
        }

        public void lda(AddressingMode mode){
            int addr = getOperandAddress(mode);
            int value = memRead(addr);

            registerA = value & 0xFF;
            update_zero_and_negative_flags(registerA);
        }

        public void sta(AddressingMode mode){
            int addr = getOperandAddress(mode);
            memWrite(addr, (byte)(registerA & 0xFF));
        }

        public void stx(AddressingMode mode){
            int addr = getOperandAddress(mode);
            memWrite(addr, (byte)(registerX & 0xFF));
        }

        public void sty(AddressingMode mode){
            int addr = getOperandAddress(mode);
            memWrite(addr, (byte)(registerY & 0xFF));
        }

        public void tax(){
            registerX = registerA;
            update_zero_and_negative_flags(registerX);
        }

        public void tay(){
            registerY = registerA;
            update_zero_and_negative_flags(registerY);
        }

        public void and(AddressingMode mode){
            int addr = getOperandAddress(mode);
            int value = memRead(addr);

            registerA = (registerA & value) & 0xFF;
            update_zero_and_negative_flags(registerA);
        }

        public void inx(){
            registerX = (registerX + 1) & 0xFF;
            update_zero_and_negative_flags(registerX);
        }

        public void iny(){
            registerY = (registerY + 1) & 0xFF;
            update_zero_and_negative_flags(registerY);
        }

        public void tya(){
            registerA = registerY;
            update_zero_and_negative_flags(registerA);
        }

        public void txa(){
            registerA = registerX;
            update_zero_and_negative_flags(registerA);
        }

        public void tsx(){
            stackPointer = registerX;
            update_zero_and_negative_flags(stackPointer);
        }

        public void pla(){
            // Pull from stack
            int addr = 0x0100 + (stackPointer & 0xFF);
            registerA = memRead(addr) & 0xFF;
            stackPointer = (stackPointer + 1) & 0xFF; // Increment stack pointer
            update_zero_and_negative_flags(registerA);
        }

        public void ora(AddressingMode mode){
            int addr = getOperandAddress(mode);
            int value = memRead(addr);
            registerA = (registerA | value) & 0xFF;
            update_zero_and_negative_flags(registerA);
        }

        public void inc(AddressingMode mode){
            int addr = getOperandAddress(mode);
            int value = memRead(addr);
            value = (value + 1) & 0xFF;
            memWrite(addr, (byte)value);
            update_zero_and_negative_flags(value);
        }

        public void ldx(AddressingMode mode){
            int addr = getOperandAddress(mode);
            int value = memRead(addr);
            registerX = value & 0xFF;
            update_zero_and_negative_flags(registerX);
        }

        public void ldy(AddressingMode mode){
            int addr = getOperandAddress(mode);
            int value = memRead(addr);
            registerY = value & 0xFF;
            update_zero_and_negative_flags(registerY);
        }

        public void eor(AddressingMode mode){
            int addr = getOperandAddress(mode);
            int value = memRead(addr);
            registerA = (registerA ^ value) & 0xFF;
            update_zero_and_negative_flags(registerA);
        }

        public void dex(){
            registerX = (registerX - 1) & 0xFF;
            update_zero_and_negative_flags(registerX);
        }

        public void dey(){
            registerY = (registerY - 1) & 0xFF;
            update_zero_and_negative_flags(registerY);
        }

        public void adc(AddressingMode mode){
                int addr = getOperandAddress(mode);
                int value = memRead(addr);

                int oldA = registerA;   // Save original A

                int result = oldA + value + (carryFlag ? 1 : 0);

                updateCarryFlag(result);

                int newA = result & 0xFF;

                updateOverflowFlag(oldA, value, newA);

                registerA = newA;

                update_zero_and_negative_flags(registerA);
        }

        private int aslValue(int value) {

            // Set Carry from bit 7 before shift
            if ((value & 0x80) != 0) {
                status |= CARRY;
            } else {
                status &= ~CARRY;
            }

            int result = (value << 1) & 0xFF;

            update_zero_and_negative_flags(result);

            return result;
        }

        public void aslAccumulator() {
            registerA = aslValue(registerA);
        }

        public void asl(AddressingMode mode) {
            int addr = getOperandAddress(mode);
            int value = memRead(addr);

            int result = aslValue(value);

            memWrite(addr, (byte) (result & 0xFF));
        }

        public void bcc() {
            int offset = memRead(programCounter);
            programCounter++;

            if ((status & CARRY) == 0) {   // Carry clear?
                int signedOffset = (byte) offset;
                programCounter += signedOffset;
            }
        }

        public void bcs(){
            int offset = memRead(programCounter);
            programCounter++;

            if ((status & CARRY) != 0) {   // Carry set?
                int signedOffset = (byte) offset;
                programCounter += signedOffset;
            }
        }

        public void sec() {
            status |= CARRY;
        }

        public void beq() {

            int offset = memRead(programCounter);
            programCounter++;

            if ((status & ZERO) != 0) {  // Zero flag set?
                int signedOffset = (byte) offset; // convert to signed
                programCounter += signedOffset;
            }
        }


        public void update_zero_and_negative_flags(int result){
            // ---- Zero Flag (bit 1) ----
            if (result == 0) {
                status = status | 0b0000_0010;      // Set zero flag
            } else {
                status = status & 0b1111_1101;      // Clear zero flag
            }

            // ---- Negative Flag (bit 7) ----
            if ((result & 0b1000_0000) != 0) {
                status = status | 0b1000_0000;      // Set negative flag
            } else {
                status = status & 0b0111_1111;      // Clear negative flag
            }
        }

        public void updateCarryFlag(int result) {
            if (result > 0xFF) {
                status |= 0b0000_0001;   // Set carry
            } else {
                status &= 0b1111_1110;   // Clear carry
            }
        }

        private void updateOverflowFlag(int oldA, int value, int result) {
            if (((oldA ^ result) & (value ^ result) & 0x80) != 0) {
                status |= OVERFLOW;      // set V flag
            } else {
                status &= ~OVERFLOW;     // clear V flag
            }
        }


        public void run(){
            
            while(true){
                // Read opcode (convert signed byte to unsigned)
                int opcode = memRead(programCounter) & 0xFF;

                // Increment program counter
                programCounter++;

                switch(opcode){
                    case 0xA9: {
                        //LDA - Immediate mode
                        lda(AddressingMode.IMMEDIATE);
                        programCounter++;
                        break;
                    }
                    case 0xA5: {
                        // LDA - Zero Page
                        lda(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0xAD: {
                        // LDA - Absolute
                        lda(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0xB5: {
                        // LDA - Zero Page,X
                        lda(AddressingMode.ZERO_PAGE_X);
                        programCounter++;
                        break;
                    }
                    case 0xBD: {
                        // LDA - Absolute,X
                        lda(AddressingMode.ABSOLUTE_X);
                        programCounter += 2;
                        break;
                    }
                    case 0xB9: {
                        // LDA - Absolute,Y
                        lda(AddressingMode.ABSOLUTE_Y);
                        programCounter += 2;
                        break;
                    }
                    case 0xA1: {
                        // LDA - Indirect,X
                        lda(AddressingMode.INDIRECT_X);
                        programCounter++;
                        break;
                    }
                    case 0xB1: {
                        // LDA - Indirect,Y
                        lda(AddressingMode.INDIRECT_Y);
                        programCounter++;
                        break;
                    }
                    case 0x85: {
                        // STA - Zero Page
                        sta(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0x95: {
                        // STA - Zero Page,X
                        sta(AddressingMode.ZERO_PAGE_X);
                        programCounter++;
                        break;
                    }
                    case 0x8D: {
                        // STA - Absolute
                        sta(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0x9D: {
                        // STA - Absolute,X
                        sta(AddressingMode.ABSOLUTE_X);
                        programCounter += 2;
                        break;
                    }
                    case 0x99: {
                        // STA - Absolute,Y
                        sta(AddressingMode.ABSOLUTE_Y);
                        programCounter += 2;
                        break;
                    }
                    case 0x81: {
                        // STA - Indirect,X
                        sta(AddressingMode.INDIRECT_X);
                        programCounter++;
                        break;
                    }
                    case 0x91: {
                        // STA - Indirect,Y
                        sta(AddressingMode.INDIRECT_Y);
                        programCounter++;
                        break;
                    }
                    case 0x86: {
                        // STX - Zero Page
                        stx(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0x96: {
                        // STX - Zero Page,Y
                        stx(AddressingMode.ZERO_PAGE_X);
                        programCounter++;
                        break;
                    }
                    case 0x8E: {
                        // STX - Absolute
                        stx(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0x84: {
                        // STY - Zero Page
                        sty(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0x94: {
                        // STY - Zero Page,X
                        sty(AddressingMode.ZERO_PAGE_X);
                        programCounter++;
                        break;
                    }
                    case 0x8C: {
                        // STY - Absolute
                        sty(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0xAA: {
                        // TAX - Transfer A to X
                        //Copy A into X
                        tax();
                        break;
                    }
                    case 0xA8: {
                        // TAY - Transfer A to Y
                        tay();
                        break;
                    }
                    case 0xE8: {
                        // INX - Increment X
                        inx();
                        break;
                    }
                    case 0xC8: {
                        // INY - Increment Y
                        iny();
                        break;
                    }
                    case 0xE6: {
                        // INC - Zero Page
                        inc(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0xF6: {
                        // INC - Zero Page,X
                        inc(AddressingMode.ZERO_PAGE_X);
                        programCounter++;
                        break;
                    }
                    case 0xEE: {
                        // INC - Absolute
                        inc(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0xFE: {
                        // INC - Absolute,X
                        inc(AddressingMode.ABSOLUTE_X);
                        programCounter += 2;
                        break;
                    }
                    case 0x29: {
                        // AND - Immediate
                        and(AddressingMode.IMMEDIATE);
                        programCounter++;
                        break;
                    }
                    case 0x25: {
                        // AND - Zero Page
                        and(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0x35: {
                        // AND - Zero Page,X
                        and(AddressingMode.ZERO_PAGE_X);
                        programCounter++;
                        break;
                    }
                    case 0x2D: {
                        // AND - Absolute
                        and(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0x3D: {
                        // AND - Absolute,X
                        and(AddressingMode.ABSOLUTE_X);
                        programCounter += 2;
                        break;
                    }
                    case 0x39: {
                        // AND - Absolute,Y
                        and(AddressingMode.ABSOLUTE_Y);
                        programCounter += 2;
                        break;
                    }
                    case 0x21: {
                        // AND - Indirect,X
                        and(AddressingMode.INDIRECT_X);
                        programCounter++;
                        break;
                    }
                    case 0x31: {
                        // AND - Indirect,Y
                        and(AddressingMode.INDIRECT_Y);
                        programCounter++;
                        break;
                    }
                    case 0x98: {
                        // TYA - Transfer Y to A
                        tya();
                        break;
                    }
                    case 0x8A: {
                        // TXA - Transfer X to A
                        txa();
                        break;
                    }
                    case 0xBA: {
                        // TSX - Transfer X to Stack Pointer
                        tsx();
                        break;
                    }
                    case 0x68: {
                        // PLA - Pull Accumulator from Stack
                        pla();
                        break;
                    }
                    case 0x09: {
                        // ORA - Immediate
                        ora(AddressingMode.IMMEDIATE);
                        programCounter++;
                        break;
                    }
                    case 0x05: {
                        // ORA - Zero Page
                        ora(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }   
                    case 0x15: {
                        // ORA - Zero Page,X
                        ora(AddressingMode.ZERO_PAGE_X);
                        programCounter++;
                        break;
                    }
                    case 0x0D: {
                        // ORA - Absolute
                        ora(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0x1D: {
                        // ORA - Absolute,X
                        ora(AddressingMode.ABSOLUTE_X);
                        programCounter += 2;
                        break;
                    }   
                    case 0x19: {
                        // ORA - Absolute,Y
                        ora(AddressingMode.ABSOLUTE_Y);
                        programCounter += 2;
                        break;
                    }
                    case 0x01: {
                        // ORA - Indirect,X
                        ora(AddressingMode.INDIRECT_X);
                        programCounter++;
                        break;
                    }
                    case 0x11: {
                        // ORA - Indirect,Y
                        ora(AddressingMode.INDIRECT_Y);
                        programCounter++;
                        break;
                    }
                    case 0xA2: {
                        // LDX - Immediate
                        ldx(AddressingMode.IMMEDIATE);
                        programCounter++;
                        break;
                    }
                    case 0xA6: {
                        // LDX - Zero Page
                        ldx(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }   
                    case 0xB6: {
                        // LDX - Zero Page,Y
                        ldx(AddressingMode.ZERO_PAGE_Y);
                        programCounter++;
                        break;
                    }
                    case 0xAE: {
                        // LDX - Absolute
                        ldx(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0xBE: {
                        // LDX - Absolute,Y
                        ldx(AddressingMode.ABSOLUTE_Y);
                        programCounter += 2;
                        break;
                    }
                    case 0xA0: {
                        // LDY - Immediate
                        ldy(AddressingMode.IMMEDIATE);
                        programCounter++;
                        break;
                    }
                    case 0xA4: {
                        // LDY - Zero Page
                        ldy(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0xB4: {
                        // LDY - Zero Page,X
                        ldy(AddressingMode.ZERO_PAGE_X);
                        programCounter++;
                        break;
                    }
                    case 0xAC: {
                        // LDY - Absolute
                        ldy(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0xBC: {
                        // LDY - Absolute,X
                        ldy(AddressingMode.ABSOLUTE_X);
                        programCounter += 2;
                        break;
                    }
                    case 0x49: {
                        // EOR - Immediate
                        eor(AddressingMode.IMMEDIATE);
                        programCounter++;
                        break;
                    }
                    case 0x45: {
                        // EOR - Zero Page
                        eor(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0x55: {
                        // EOR - Zero Page,X
                        eor(AddressingMode.ZERO_PAGE_X);
                        programCounter++;
                        break;
                    }
                    case 0x4D: {
                        // EOR - Absolute
                        eor(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0x5D: {
                        // EOR - Absolute,X
                        eor(AddressingMode.ABSOLUTE_X);
                        programCounter += 2;
                        break;
                    }
                    case 0x59: {
                        // EOR - Absolute,Y
                        eor(AddressingMode.ABSOLUTE_Y);
                        programCounter += 2;
                        break;
                    }
                    case 0x41: {
                        // EOR - Indirect,X
                        eor(AddressingMode.INDIRECT_X);
                        programCounter++;
                        break;
                    }
                    case 0x51: {
                        // EOR - Indirect,Y
                        eor(AddressingMode.INDIRECT_Y);
                        programCounter++;
                        break;
                    }
                    case 0xCA: {
                        // DEX - Decrement X
                        dex();
                        break;
                    }
                    case 0x88: {
                        // DEY - Decrement Y
                        dey();
                        break;
                    }
                    case 0x9A: {
                        // TXS - Transfer X to Stack Pointer
                        stackPointer = registerX;
                        break;
                    }
                    case 0x69: {
                        // ADC - Immediate
                        adc(AddressingMode.IMMEDIATE);
                        programCounter++;
                        break;
                    }
                    case 0x65: {
                        // ADC - Zero Page
                        adc(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0x75: {
                        // ADC - Zero Page,X
                        adc(AddressingMode.ZERO_PAGE_X);
                        programCounter++;
                        break;
                    }
                    case 0x6D: {
                        // ADC - Absolute
                        adc(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0x7D: {
                        // ADC - Absolute,X
                        adc(AddressingMode.ABSOLUTE_X);
                        programCounter += 2;
                        break;
                    }
                    case 0x79: {
                        // ADC - Absolute,Y
                        adc(AddressingMode.ABSOLUTE_Y);
                        programCounter += 2;
                        break;
                    }
                    case 0x61: {
                        // ADC - Indirect,X
                        adc(AddressingMode.INDIRECT_X);
                        programCounter++;
                        break;
                    }
                    case 0x71: {
                        // ADC - Indirect,Y
                        adc(AddressingMode.INDIRECT_Y);
                        programCounter++;
                        break;
                    }
                    case 0x0A: {
                        // ASL - Accumulator
                        aslAccumulator();
                        break;
                    }
                    case 0x06: {
                        // ASL - Zero Page
                        asl(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0x16: {
                        // ASL - Zero Page,X
                        asl(AddressingMode.ZERO_PAGE_X);
                        programCounter++;
                        break;
                    }
                    case 0x0E: {
                        // ASL - Absolute
                        asl(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0x1E: {
                        // ASL - Absolute,X
                        asl(AddressingMode.ABSOLUTE_X);
                        programCounter += 2;
                        break;
                    }
                    case 0x90: {
                        // BCC - Branch if Carry Clear
                        bcc();
                        break;
                    }
                    case 0xB0: {
                        // BCS - Branch if Carry Set
                        bcs();
                        break;
                    }
                    case 0x38: {
                        // SEC - Set Carry Flag
                        sec();
                        break;
                    }
                    case 0xf0: {
                        // BEQ - Branch if Equal (Zero flag set)
                        beq();
                        break;
                    }
                    case 0x00:
                        // BRK - Break (for this demo, we'll just stop execution)
                        return;    
                    default:
                        throw new UnsupportedOperationException("Opcode not implemented yet.");
                }
            }
        }
    public static void main(String[] args){
        
    }
}
