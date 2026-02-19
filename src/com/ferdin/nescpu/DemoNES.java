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
            registerY = 0;
            stackPointer = stack_reset;
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

        public void memWriteU16(int pos, int data) {
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

        public void clc() {
            status &= ~CARRY;  // clear the carry bit
            carryFlag = false; // if you are also tracking it separately
        }

        public void beq() {

            int offset = memRead(programCounter);
            programCounter++;

            if ((status & ZERO) != 0) {  // Zero flag set?
                int signedOffset = (byte) offset; // convert to signed
                programCounter += signedOffset;
            }
        }

        public void bit(AddressingMode mode) {

            int addr = getOperandAddress(mode);
            int value = memRead(addr);

            // 1️⃣ Zero flag: set if (A & value) == 0
            if ((registerA & value) == 0) {
                status |= ZERO;
            } else {
                status &= ~ZERO;
            }

            // 2️⃣ Negative flag = bit 7 of memory
            if ((value & 0x80) != 0) {
                status |= NEGATIVE;
            } else {
                status &= ~NEGATIVE;
            }

            // 3️⃣ Overflow flag = bit 6 of memory
            if ((value & 0x40) != 0) {
                status |= OVERFLOW;
            } else {
                status &= ~OVERFLOW;
            }
        }

        public void bmi() {

            int offset = memRead(programCounter);
            programCounter++;

            if ((status & NEGATIVE) != 0) {  // Negative flag set?
                programCounter += (byte) offset;
            }
        }

        public void bne() {
            int offset = memRead(programCounter);
            programCounter++;

            if ((status & ZERO) == 0) {  // Zero flag clear?
                programCounter += (byte) offset;
            }
        }

        public void bpl() {
            int offset = memRead(programCounter);
            programCounter++;

            // Negative flag clear?
            if ((status & NEGATIVE) == 0) {
                programCounter += (byte) offset;  // signed addition
            }
        }

        public void bvc() {
            int offset = memRead(programCounter);
            programCounter++;

            // Overflow flag clear?
            if ((status & OVERFLOW) == 0) {
                programCounter += (byte) offset;  // signed addition
            }
        }

        public void bvs() {
            int offset = memRead(programCounter);
            programCounter++;

            // Overflow flag set?
            if ((status & OVERFLOW) != 0) {
                programCounter += (byte) offset;  // signed addition
            }
        }

        public void cld(){
            status &= ~DECIMAL_MODE;  // clear the Decimal Mode flag
        }

        public void cli() {
            status &= ~INTERRUPT_DISABLE;  // clear the I flag
        }

        public void clv() {
            status &= ~OVERFLOW;  // clear the V flag
        }

        public void cpx(AddressingMode mode) {
            int addr = getOperandAddress(mode);
            int value = memRead(addr);
            int result = registerX - value;

            // Carry flag
            if (registerX >= value) {
                status |= CARRY;
            } else {
                status &= ~CARRY;
            }

            // Zero & Negative flags
            update_zero_and_negative_flags(result & 0xFF);
        }

        public void cpy(AddressingMode mode) {
            int addr = getOperandAddress(mode);
            int value = memRead(addr);
            int result = registerY - value;

            // Carry flag
            if (registerY >= value) {
                status |= CARRY;
            } else {
                status &= ~CARRY;
            }

            // Zero & Negative flags
            update_zero_and_negative_flags(result & 0xFF);
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

        public void cmp(AddressingMode mode) {
            int addr = getOperandAddress(mode);
            int value = memRead(addr);         // fetch operand
            int result = registerA - value;    // subtract

            // Update Carry flag: set if A >= value
            if (registerA >= value) {
                status |= CARRY;
            } else {
                status &= ~CARRY;
            }

            // Update Zero and Negative flags
            update_zero_and_negative_flags(result & 0xFF);  // result is treated as 8-bit
        }

        public void dec(AddressingMode mode) {
            int addr = getOperandAddress(mode);
            int value = memRead(addr);

            value = (value - 1) & 0xFF;   // wrap around 0x00 -> 0xFF

            memWrite(addr, (byte) value);

            update_zero_and_negative_flags(value);
        }

        public void jmpAbsolute() {
            int addr = memReadU16(programCounter);
            programCounter = addr;
        }

        public void jmpIndirect() {
            int ptr = memReadU16(programCounter);

            int lo = memRead(ptr);

            int hi;
            if ((ptr & 0x00FF) == 0x00FF) {
                // Simulate 6502 page boundary bug
                hi = memRead(ptr & 0xFF00);
            } else {
                hi = memRead(ptr + 1);
            }

            programCounter = (hi << 8) | lo;
        }

        private void stackPush(int value) {
            memWrite(0x0100 + stackPointer, (byte)(value & 0xFF));
            stackPointer--;
        }

        private void stackPushU16(int value) {
            int hi = (value >> 8) & 0xFF;
            int lo = value & 0xFF;

            stackPush(hi);
            stackPush(lo);
        }

        private int stackPop() {
            stackPointer++;
            return memRead(0x0100 + stackPointer);
        }

        private int stackPopU16() {
            int lo = stackPop();
            int hi = stackPop();
            return (hi << 8) | lo;
        }


        public void jsr() {
            int targetAddr = memReadU16(programCounter);

            int returnAddr = programCounter + 1;

            stackPushU16(returnAddr);

            programCounter = targetAddr;
        }

        public void rts() {
            int returnAddr = stackPopU16();
            programCounter = returnAddr + 1;
        }

        private int lsrValue(int value) {
            // Set carry from bit 0
            if ((value & 0x01) != 0) {
                status |= CARRY;
            } else {
                status &= ~CARRY;
            }

            int result = (value >> 1) & 0xFF;

            // Update zero flag
            if (result == 0) {
                status |= ZERO;
            } else {
                status &= ~ZERO;
            }

            // Negative flag always cleared (bit 7 is 0)
            status &= ~NEGATIVE;

            return result;
        }

        public void lsrAccumulator() {
            registerA = lsrValue(registerA);
        }

        public void lsr(AddressingMode mode) {
            int addr = getOperandAddress(mode);
            int value = memRead(addr);

            int result = lsrValue(value);

            memWrite(addr, (byte) (result & 0xFF));
        }

        public void pha() {
            stackPush(registerA);
        }

        public void php() {
            // Copy status and set BREAK and BREAK2 bits
            int flagsToPush = status | BREAK | BREAK2;
            stackPush(flagsToPush);
        }

        public void plp() {
            int value = stackPop();

            // Normally, BREAK (bit 4) is ignored in CPU status
            status = value & ~(BREAK | BREAK2);
        }

        private int rolValue(int value) {
            int result = ((value << 1) & 0xFF); // shift left
            if (carryFlag) {
                result |= 0x01; // insert previous carry into bit 0
            }

            // Update carry from old bit 7
            carryFlag = (value & 0x80) != 0;
            if (carryFlag) status |= CARRY;
            else status &= ~CARRY;

            // Update zero and negative flags
            update_zero_and_negative_flags(result);

            return result & 0xFF;
        }

        public void rolAccumulator() {
            registerA = rolValue(registerA);
        }

        public void rol(AddressingMode mode) {
            int addr = getOperandAddress(mode);
            int value = memRead(addr);

            int result = rolValue(value);

            memWrite(addr, (byte) (result & 0xFF));
        }

        private int rorValue(int value) {
            // Capture old bit 0 for carry
            boolean oldCarry = (value & 0x01) != 0;

            int result = (value >> 1) & 0xFF;

            // Insert previous carry into bit 7
            if (carryFlag) {
                result |= 0x80;
            }

            // Update carry flag from old bit 0
            carryFlag = oldCarry;
            if (carryFlag) status |= CARRY;
            else status &= ~CARRY;

            // Update zero and negative flags
            update_zero_and_negative_flags(result);

            return result & 0xFF;
        }

        public void rorAccumulator() {
            registerA = rorValue(registerA);
        }

        public void ror(AddressingMode mode) {
            int addr = getOperandAddress(mode);
            int value = memRead(addr);

            int result = rorValue(value);

            memWrite(addr, (byte) (result & 0xFF));
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
                    case 0x24: {
                        // BIT - Test Bits in Memory with Accumulator
                        bit(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0x2C: {
                        // BIT - Absolute
                        bit(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0x30: {
                        // BMI - Branch if Minus (Negative flag set)
                        bmi();
                        break;
                    }
                    case 0xD0: {
                        // BNE - Branch if Not Equal (Zero flag clear)
                        bne();
                        break;
                    }
                    case 0x10: {
                        // BPL - Branch if Positive (Negative flag clear)
                        bpl();
                        break;
                    }
                    case 0x50: {
                        // BVC - Branch if Overflow Clear
                        bvc();
                        break;
                    }
                    case 0x70: {
                        // BVS - Branch if Overflow Set
                        bvs();
                        break;
                    }
                    case 0x18: {
                        // CLC - Clear Carry Flag
                        clc();
                        break;
                    }
                    case 0xD8: {
                        // CLD - Clear Decimal Mode
                        cld();
                        break;
                    }
                    case 0x58: {
                        // CLI - Clear Interrupt Disable
                        cli();
                        break;
                    }
                    case 0xB8: {
                        // CLV - Clear Overflow Flag
                        clv();
                        break;
                    }
                    case 0xc9: {
                        // CMP - Compare
                        cmp(AddressingMode.IMMEDIATE);
                        programCounter++;
                        break;
                    }
                    case 0xC5: {
                        // CMP - Zero Page
                        cmp(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0xD5: {
                        // CMP - Zero Page,X
                        cmp(AddressingMode.ZERO_PAGE_X);
                        programCounter++;
                        break;
                    }
                    case 0xCD: {
                        // CMP - Absolute
                        cmp(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0xDD: {
                        // CMP - Absolute,X
                        cmp(AddressingMode.ABSOLUTE_X);
                        programCounter += 2;
                        break;
                    }
                    case 0xD9: {
                        // CMP - Absolute,Y
                        cmp(AddressingMode.ABSOLUTE_Y);
                        programCounter += 2;
                        break;
                    }
                    case 0xC1: {
                        // CMP - Indirect,X
                        cmp(AddressingMode.INDIRECT_X);
                        programCounter++;
                        break;
                    }
                    case 0xD1: {
                        // CMP - Indirect,Y
                        cmp(AddressingMode.INDIRECT_Y);
                        programCounter++;
                        break;
                    }
                    case 0xE0: {
                        // CPX - Compare X Register
                        cpx(AddressingMode.IMMEDIATE);
                        programCounter++;
                        break;
                    }
                    case 0xE4: {
                        // CPX - Zero Page
                        cpx(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }   
                    case 0xEC: {
                        // CPX - Absolute
                        cpx(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0xC0: {
                        // CPY - Compare Y Register
                        cpy(AddressingMode.IMMEDIATE);
                        programCounter++;
                        break;
                    }
                    case 0xC4: {
                        // CPY - Zero Page
                        cpy(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0xCC: {
                        // CPY - Absolute
                        cpy(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0xC6: {
                        // DEC - Zero Page
                        dec(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0xD6: {
                        // DEC - Zero Page,X
                        dec(AddressingMode.ZERO_PAGE_X);
                        programCounter++;
                        break;
                    }
                    case 0xCE: {
                        // DEC - Absolute
                        dec(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0xDE: {
                        // DEC - Absolute,X
                        dec(AddressingMode.ABSOLUTE_X);
                        programCounter += 2;
                        break;
                    }
                    case 0x6C: {
                        // JMP - Indirect
                        jmpIndirect();
                        break;
                    }
                    case 0x4C: {
                        // JMP - Absolute
                        jmpAbsolute();
                        break;
                    }
                    case 0x20:
                        jsr();
                        break;
                    case 0x60:
                        rts();
                        break;   
                    case 0x4A: {
                        // LSR - Accumulator
                        lsrAccumulator();
                        break;
                    }
                    case 0x46: {
                        // LSR - Zero Page
                        lsr(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0x56: {
                        // LSR - Zero Page,X
                        lsr(AddressingMode.ZERO_PAGE_X);
                        programCounter++;
                        break;
                    }
                    case 0x4E: {
                        // LSR - Absolute
                        lsr(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0x5E: {
                        // LSR - Absolute,X
                        lsr(AddressingMode.ABSOLUTE_X);
                        programCounter += 2;
                        break;
                    }
                    case 0x48: {
                        // PHA - Push Accumulator on Stack
                        pha();
                        break;
                    }
                    case 0x08: {
                        // PHP - Push Processor Status on Stack
                        php();
                        break;
                    }
                    case 0x28: {
                        // PLP - Pull Processor Status from Stack
                        plp();
                        break;
                    }   
                    case 0x2A: {
                        // ROL - Accumulator
                        rolAccumulator();
                        break;
                    }
                    case 0x26: {
                        // ROL - Zero Page
                        rol(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0x36: {
                        // ROL - Zero Page,X
                        rol(AddressingMode.ZERO_PAGE_X);
                        programCounter++;
                        break;
                    }
                    case 0x2E: {
                        // ROL - Absolute
                        rol(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0x3E: {
                        // ROL - Absolute,X
                        rol(AddressingMode.ABSOLUTE_X);
                        programCounter += 2;
                        break;
                    }
                    case 0x6A: {
                        // ROR - Accumulator
                        rorAccumulator();
                        break;
                    }
                    case 0x66: {
                        // ROR - Zero Page
                        ror(AddressingMode.ZERO_PAGE);
                        programCounter++;
                        break;
                    }
                    case 0x76: {
                        // ROR - Zero Page,X
                        ror(AddressingMode.ZERO_PAGE_X);
                        programCounter++;
                        break;
                    }
                    case 0x6E: {
                        // ROR - Absolute
                        ror(AddressingMode.ABSOLUTE);
                        programCounter += 2;
                        break;
                    }
                    case 0x7E: {
                        // ROR - Absolute,X
                        ror(AddressingMode.ABSOLUTE_X);
                        programCounter += 2;
                        break;
                    }
                    case 0xEA:{
                        // NOP - No Operation
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
