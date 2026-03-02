package com.ferdin.nescpu;

public class DemoNES implements Mem {

    @FunctionalInterface
    public interface CpuCallback {
        void execute(DemoNES cpu);
    }
    // I created this class to learn NESCPU from bugzmanov/nes_ebook
    //https://github.com/bugzmanov/nes_ebook/blob/master/src/chapter_3_2.md
        // CPU Registers (8-bit)
        public int registerA;
        public int registerX;
        public int registerY;
        public int stackPointer;
        public int status;

        // Cycle tracking
        private int cycles = 0;         // Total cycles elapsed
        private int stallCycles;    // Cycles to stall (DMA, etc.)

        // Program Counter (16-bit)
        public int programCounter;

        // Stack
        public int stack = 0x0100;
        public int stack_reset = 0xFD;

        // Bus
        private final Bus bus;

        // 64KB Memory
        //private byte[] memory;

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

        public int getStatus() {
            return status;
        }
        public int getCycles(){
            return cycles;
        }

        public int getProgramCounter() {
            return programCounter;
        }

        public int getRegisterA() {
            return registerA;
        }

        public int getRegisterX() {
            return registerX;
        }   

        public int getStackPointer() {
            return stackPointer;
        }

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
        // Called once per CPU cycle from the main loop
        public void tick() {
            if (stallCycles > 0) {
                stallCycles--;
                cycles++;
                return;
            }
            // Only execute a new instruction when cyclesRemaining hits 0
            // (handled below via step())
        }

        private boolean pageCrossed(int addr1, int addr2) {
            return (addr1 & 0xFF00) != (addr2 & 0xFF00);
        }
        
        private int getOperandAddress(AddressingMode mode) {
            switch (mode) {

                case IMMEDIATE: {
                    int addr = programCounter;
                    programCounter++;
                    return addr;
                }

                case ZERO_PAGE: {
                    int addr = memRead(programCounter) & 0xFF;
                    programCounter++;
                    return addr;
                }

                case ABSOLUTE: {
                    int addr = memReadU16(programCounter);
                    programCounter += 2;
                    return addr;
                }

                case ZERO_PAGE_X: {
                    int pos = memRead(programCounter) & 0xFF;
                    programCounter++;
                    return (pos + registerX) & 0xFF;
                }

                case ZERO_PAGE_Y: {
                    int pos = memRead(programCounter) & 0xFF;
                    programCounter++;
                    return (pos + registerY) & 0xFF;
                }

                case ABSOLUTE_X: {
                    int base = memReadU16(programCounter);
                    programCounter += 2;

                    int addr = (base + registerX) & 0xFFFF;

                    if (pageCrossed(base, addr)) {
                        cycles++;
                    }

                    return addr;
                }

                case ABSOLUTE_Y: {
                    int base = memReadU16(programCounter);
                    programCounter += 2;

                    int addr = (base + registerY) & 0xFFFF;

                    if (pageCrossed(base, addr)) {
                        cycles++;
                    }

                    return addr;
                }

                case INDIRECT_X: {
                    int base = memRead(programCounter) & 0xFF;
                    programCounter++;

                    int ptr = (base + registerX) & 0xFF;
                    int lo = memRead(ptr) & 0xFF;
                    int hi = memRead((ptr + 1) & 0xFF) & 0xFF;

                    return (hi << 8) | lo;
                }

                case INDIRECT_Y: {
                    int base = memRead(programCounter) & 0xFF;
                    programCounter++;

                    int lo = memRead(base) & 0xFF;
                    int hi = memRead((base + 1) & 0xFF) & 0xFF;

                    int derefBase = (hi << 8) | lo;
                    int addr = (derefBase + registerY) & 0xFFFF;

                    if (pageCrossed(derefBase, addr)) {
                        cycles++;
                    }

                    return addr;
                }

                default:
                    throw new UnsupportedOperationException("Unsupported addressing mode");
            }
        }

        public DemoNES(Bus bus) {
            this.bus = bus;
            //this.memory = new byte[0x10000];
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

        @Override
        public int memRead(int addr) {
            // return memory[addr & 0xFFFF] & 0xFF;
            return bus.memRead(addr);
        }

        @Override
        public void memWrite(int addr, int data) {
            bus.memWrite(addr, data);
        }

        @Override
        public int memReadU16(int pos) {
            return bus.memReadU16(pos);
        }

        @Override
        public void memWriteU16(int pos, int data) {
            bus.memWriteU16(pos, data);
        }

        // Loads program into memory at a default start address
        public void loadAndRun(int[] program) {
            load(program);
            reset();
            // Keep stepping until BRK (opcode 0x00)
            boolean running = true;
            while(running) {
                int opcode = memRead(programCounter) & 0xFF;
                if(opcode == 0x00) {  // BRK
                    step();           // execute BRK
                    running = false;
                } else {
                    step();
                }
            }
        }

        public void load(int[] program) {
            for (int i = 0; i < program.length; i++) {
                memWrite(0x0000 + i, program[i]);
            }
            memWriteU16(0xFFFC, 0x0000);
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
            registerX = stackPointer;
            update_zero_and_negative_flags(stackPointer);
        }

        public void pla(){
            // Pull from stack
            stackPointer = (stackPointer + 1) & 0xFF;  // increment first
            registerA = memRead(0x0100 + stackPointer) & 0xFF;
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

        private int brk() {

            // BRK acts like a 2-byte instruction
            programCounter++;

            // Push PC to stack
            stackPushU16(programCounter);

            // Push status with B flag set and unused bit set
            int statusToPush = status | BREAK | BREAK2;
            stackPush((byte)(statusToPush & 0xFF));

            // Set Interrupt Disable flag
            status |= INTERRUPT_DISABLE;

            // Load IRQ/BRK vector at $FFFE
            int low = memRead(0xFFFE) & 0xFF;
            int high = memRead(0xFFFF) & 0xFF;
            programCounter = (high << 8) | low;

            return 7;
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

                int result = oldA + value + ((status & CARRY) != 0 ? 1 : 0);

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

        public void sec() {
            status |= CARRY;
        }

        public void clc() {
            status &= ~CARRY;  // clear the carry bit
            carryFlag = false; // if you are also tracking it separately
        }

        private void branchIf(boolean condition) {
            int offset = memRead(programCounter) & 0xFF; // fetch offset byte
            programCounter++; // move past the branch operand

            // Convert to signed byte
            if (offset > 127) offset -= 256; 

            if (condition) {
                cycles++; // +1 cycle for branch taken
                int oldPC = programCounter;
                programCounter = (programCounter + offset) & 0xFFFF;

                if (pageCrossed(oldPC, programCounter)) {
                    cycles++; // +1 extra cycle if branch crosses page
                }
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

        public void beq() { branchIf((status & ZERO) != 0); }
        
        public void bmi() { branchIf((status & NEGATIVE) != 0); }

        public void bne() { branchIf((status & ZERO) == 0); }

        public void bpl() { branchIf((status & NEGATIVE) == 0); }

        public void bvc() { branchIf((status & OVERFLOW) == 0); }

        public void bvs() { branchIf((status & OVERFLOW) != 0); }

        public void bcc() { branchIf((status & CARRY) == 0); }

        public void bcs() { branchIf((status & CARRY) != 0); }

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
            stackPointer = (stackPointer - 1) & 0xFF;
        }

        private void stackPushU16(int value) {
            int hi = (value >> 8) & 0xFF;
            int lo = value & 0xFF;

            stackPush(hi);
            stackPush(lo);
        }

        private int stackPop() {
            stackPointer = (stackPointer + 1) & 0xFF;
            return memRead(0x0100 + stackPointer) & 0xFF;
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

        public void rti() {
            // 1. Restore status
            int value = stackPop();

            // Break flag is not actually stored in CPU
            status = value & ~(BREAK | BREAK2);

            // 2. Restore PC (low then high)
            int lo = stackPop();
            int hi = stackPop();

            programCounter = (hi << 8) | lo;
        }
        
        private void sbc(AddressingMode mode) {

            int addr = getOperandAddress(mode);
            int value = memRead(addr);

            int carryIn = (status & CARRY) != 0 ? 1 : 0;

            int result = registerA + (value ^ 0xFF) + carryIn;

            // Carry flag (set if no borrow)
            if (result > 0xFF) {
                status |= CARRY;
            } else {
                status &= ~CARRY;
            }

            int finalResult = result & 0xFF;

            // Overflow detection (clean subtraction form)
            if (((registerA ^ finalResult) & (registerA ^ value) & 0x80) != 0) {
                status |= OVERFLOW;
            } else {
                status &= ~OVERFLOW;
            }

            registerA = finalResult;

            update_zero_and_negative_flags(registerA);
        }

        public void sed() {
            status |= DECIMAL_MODE;
        }

        public void sei() {
            status |= INTERRUPT_DISABLE;
        }

        // public void run() {
        //     runWithCallback(cpu -> {});
        // }

        public int step(){
            
            // while(true){
                // Call callback before each instruction
                // callback.execute(this);

                // Read opcode (convert signed byte to unsigned)
                int opcode = memRead(programCounter++) & 0xFF;

                int baseCycles = CYCLES[opcode];
                int extraCyclesBefore = cycles;

                switch(opcode){
                    case 0xA9: {
                        //LDA - Immediate mode
                        lda(AddressingMode.IMMEDIATE);
                        break;
                    }
                    case 0xA5: {
                        // LDA - Zero Page
                        lda(AddressingMode.ZERO_PAGE);
                        break;
                    }
                    case 0xAD: {
                        // LDA - Absolute
                        lda(AddressingMode.ABSOLUTE);
                        break;
                    }
                    case 0xB5: {
                        // LDA - Zero Page,X
                        lda(AddressingMode.ZERO_PAGE_X);
                        break;
                    }
                    case 0xBD: {
                        // LDA - Absolute,X
                        lda(AddressingMode.ABSOLUTE_X);
                        break;
                    }
                    case 0xB9: {
                        // LDA - Absolute,Y
                        lda(AddressingMode.ABSOLUTE_Y);
                        break;
                    }
                    case 0xA1: {
                        // LDA - Indirect,X
                        lda(AddressingMode.INDIRECT_X);
                        break;
                    }
                    case 0xB1: {
                        // LDA - Indirect,Y
                        lda(AddressingMode.INDIRECT_Y);
                        break;
                    }
                    case 0x85: {
                        // STA - Zero Page
                        sta(AddressingMode.ZERO_PAGE);
                        break;
                    }
                    case 0x95: {
                        // STA - Zero Page,X
                        sta(AddressingMode.ZERO_PAGE_X);
                        break;
                    }
                    case 0x8D: {
                        // STA - Absolute
                        sta(AddressingMode.ABSOLUTE);
                        break;
                    }
                    case 0x9D: {
                        // STA - Absolute,X
                        sta(AddressingMode.ABSOLUTE_X);
                        break;
                    }
                    case 0x99: {
                        // STA - Absolute,Y
                        sta(AddressingMode.ABSOLUTE_Y);
                        break;
                    }
                    case 0x81: {
                        // STA - Indirect,X
                        sta(AddressingMode.INDIRECT_X);
                        break;
                    }
                    case 0x91: {
                        // STA - Indirect,Y
                        sta(AddressingMode.INDIRECT_Y);
                        break;
                    }
                    case 0x86: {
                        // STX - Zero Page
                        stx(AddressingMode.ZERO_PAGE);
                        break;
                    }
                    case 0x96: {
                        // STX - Zero Page,Y
                        stx(AddressingMode.ZERO_PAGE_Y);
                        break;
                    }
                    case 0x8E: {
                        // STX - Absolute
                        stx(AddressingMode.ABSOLUTE);
                        break;
                    }
                    case 0x84: {
                        // STY - Zero Page
                        sty(AddressingMode.ZERO_PAGE);
                        break;
                    }
                    case 0x94: {
                        // STY - Zero Page,X
                        sty(AddressingMode.ZERO_PAGE_X);
                        
                        break;
                    }
                    case 0x8C: {
                        // STY - Absolute
                        sty(AddressingMode.ABSOLUTE);
                        
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
                        
                        break;
                    }
                    case 0xF6: {
                        // INC - Zero Page,X
                        inc(AddressingMode.ZERO_PAGE_X);
                        
                        break;
                    }
                    case 0xEE: {
                        // INC - Absolute
                        inc(AddressingMode.ABSOLUTE);
                        
                        break;
                    }
                    case 0xFE: {
                        // INC - Absolute,X
                        inc(AddressingMode.ABSOLUTE_X);
                        
                        break;
                    }
                    case 0x29: {
                        // AND - Immediate
                        and(AddressingMode.IMMEDIATE);
                        
                        break;
                    }
                    case 0x25: {
                        // AND - Zero Page
                        and(AddressingMode.ZERO_PAGE);
                        
                        break;
                    }
                    case 0x35: {
                        // AND - Zero Page,X
                        and(AddressingMode.ZERO_PAGE_X);
                        
                        break;
                    }
                    case 0x2D: {
                        // AND - Absolute
                        and(AddressingMode.ABSOLUTE);
                        
                        break;
                    }
                    case 0x3D: {
                        // AND - Absolute,X
                        and(AddressingMode.ABSOLUTE_X);
                        
                        break;
                    }
                    case 0x39: {
                        // AND - Absolute,Y
                        and(AddressingMode.ABSOLUTE_Y);
                        
                        break;
                    }
                    case 0x21: {
                        // AND - Indirect,X
                        and(AddressingMode.INDIRECT_X);
                        
                        break;
                    }
                    case 0x31: {
                        // AND - Indirect,Y
                        and(AddressingMode.INDIRECT_Y);
                        
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
                        
                        break;
                    }
                    case 0x05: {
                        // ORA - Zero Page
                        ora(AddressingMode.ZERO_PAGE);
                        
                        break;
                    }   
                    case 0x15: {
                        // ORA - Zero Page,X
                        ora(AddressingMode.ZERO_PAGE_X);
                        
                        break;
                    }
                    case 0x0D: {
                        // ORA - Absolute
                        ora(AddressingMode.ABSOLUTE);
                        
                        break;
                    }
                    case 0x1D: {
                        // ORA - Absolute,X
                        ora(AddressingMode.ABSOLUTE_X);
                        
                        break;
                    }   
                    case 0x19: {
                        // ORA - Absolute,Y
                        ora(AddressingMode.ABSOLUTE_Y);
                        
                        break;
                    }
                    case 0x01: {
                        // ORA - Indirect,X
                        ora(AddressingMode.INDIRECT_X);
                        
                        break;
                    }
                    case 0x11: {
                        // ORA - Indirect,Y
                        ora(AddressingMode.INDIRECT_Y);
                        
                        break;
                    }
                    case 0xA2: {
                        // LDX - Immediate
                        ldx(AddressingMode.IMMEDIATE);
                        
                        break;
                    }
                    case 0xA6: {
                        // LDX - Zero Page
                        ldx(AddressingMode.ZERO_PAGE);
                        
                        break;
                    }   
                    case 0xB6: {
                        // LDX - Zero Page,Y
                        ldx(AddressingMode.ZERO_PAGE_Y);
                        
                        break;
                    }
                    case 0xAE: {
                        // LDX - Absolute
                        ldx(AddressingMode.ABSOLUTE);
                        
                        break;
                    }
                    case 0xBE: {
                        // LDX - Absolute,Y
                        ldx(AddressingMode.ABSOLUTE_Y);
                        
                        break;
                    }
                    case 0xA0: {
                        // LDY - Immediate
                        ldy(AddressingMode.IMMEDIATE);
                        
                        break;
                    }
                    case 0xA4: {
                        // LDY - Zero Page
                        ldy(AddressingMode.ZERO_PAGE);
                        
                        break;
                    }
                    case 0xB4: {
                        // LDY - Zero Page,X
                        ldy(AddressingMode.ZERO_PAGE_X);
                        
                        break;
                    }
                    case 0xAC: {
                        // LDY - Absolute
                        ldy(AddressingMode.ABSOLUTE);
                        
                        break;
                    }
                    case 0xBC: {
                        // LDY - Absolute,X
                        ldy(AddressingMode.ABSOLUTE_X);
                        
                        break;
                    }
                    case 0x49: {
                        // EOR - Immediate
                        eor(AddressingMode.IMMEDIATE);
                        
                        break;
                    }
                    case 0x45: {
                        // EOR - Zero Page
                        eor(AddressingMode.ZERO_PAGE);
                        
                        break;
                    }
                    case 0x55: {
                        // EOR - Zero Page,X
                        eor(AddressingMode.ZERO_PAGE_X);
                        
                        break;
                    }
                    case 0x4D: {
                        // EOR - Absolute
                        eor(AddressingMode.ABSOLUTE);
                        
                        break;
                    }
                    case 0x5D: {
                        // EOR - Absolute,X
                        eor(AddressingMode.ABSOLUTE_X);
                        
                        break;
                    }
                    case 0x59: {
                        // EOR - Absolute,Y
                        eor(AddressingMode.ABSOLUTE_Y);
                        
                        break;
                    }
                    case 0x41: {
                        // EOR - Indirect,X
                        eor(AddressingMode.INDIRECT_X);
                        
                        break;
                    }
                    case 0x51: {
                        // EOR - Indirect,Y
                        eor(AddressingMode.INDIRECT_Y);
                        
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
                        
                        break;
                    }
                    case 0x65: {
                        // ADC - Zero Page
                        adc(AddressingMode.ZERO_PAGE);
                        
                        break;
                    }
                    case 0x75: {
                        // ADC - Zero Page,X
                        adc(AddressingMode.ZERO_PAGE_X);
                        
                        break;
                    }
                    case 0x6D: {
                        // ADC - Absolute
                        adc(AddressingMode.ABSOLUTE);
                        
                        break;
                    }
                    case 0x7D: {
                        // ADC - Absolute,X
                        adc(AddressingMode.ABSOLUTE_X);
                        
                        break;
                    }
                    case 0x79: {
                        // ADC - Absolute,Y
                        adc(AddressingMode.ABSOLUTE_Y);
                        
                        break;
                    }
                    case 0x61: {
                        // ADC - Indirect,X
                        adc(AddressingMode.INDIRECT_X);
                        
                        break;
                    }
                    case 0x71: {
                        // ADC - Indirect,Y
                        adc(AddressingMode.INDIRECT_Y);
                        
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
                        
                        break;
                    }
                    case 0x16: {
                        // ASL - Zero Page,X
                        asl(AddressingMode.ZERO_PAGE_X);
                        
                        break;
                    }
                    case 0x0E: {
                        // ASL - Absolute
                        asl(AddressingMode.ABSOLUTE);
                        
                        break;
                    }
                    case 0x1E: {
                        // ASL - Absolute,X
                        asl(AddressingMode.ABSOLUTE_X);
                        
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
                        
                        break;
                    }
                    case 0x2C: {
                        // BIT - Absolute
                        bit(AddressingMode.ABSOLUTE);
                        
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
                        
                        break;
                    }
                    case 0xC5: {
                        // CMP - Zero Page
                        cmp(AddressingMode.ZERO_PAGE);
                        
                        break;
                    }
                    case 0xD5: {
                        // CMP - Zero Page,X
                        cmp(AddressingMode.ZERO_PAGE_X);
                        
                        break;
                    }
                    case 0xCD: {
                        // CMP - Absolute
                        cmp(AddressingMode.ABSOLUTE);
                        
                        break;
                    }
                    case 0xDD: {
                        // CMP - Absolute,X
                        cmp(AddressingMode.ABSOLUTE_X);
                        
                        break;
                    }
                    case 0xD9: {
                        // CMP - Absolute,Y
                        cmp(AddressingMode.ABSOLUTE_Y);
                        
                        break;
                    }
                    case 0xC1: {
                        // CMP - Indirect,X
                        cmp(AddressingMode.INDIRECT_X);
                        
                        break;
                    }
                    case 0xD1: {
                        // CMP - Indirect,Y
                        cmp(AddressingMode.INDIRECT_Y);
                        
                        break;
                    }
                    case 0xE0: {
                        // CPX - Compare X Register
                        cpx(AddressingMode.IMMEDIATE);
                        
                        break;
                    }
                    case 0xE4: {
                        // CPX - Zero Page
                        cpx(AddressingMode.ZERO_PAGE);
                        
                        break;
                    }   
                    case 0xEC: {
                        // CPX - Absolute
                        cpx(AddressingMode.ABSOLUTE);
                        
                        break;
                    }
                    case 0xC0: {
                        // CPY - Compare Y Register
                        cpy(AddressingMode.IMMEDIATE);
                        
                        break;
                    }
                    case 0xC4: {
                        // CPY - Zero Page
                        cpy(AddressingMode.ZERO_PAGE);
                        
                        break;
                    }
                    case 0xCC: {
                        // CPY - Absolute
                        cpy(AddressingMode.ABSOLUTE);
                        
                        break;
                    }
                    case 0xC6: {
                        // DEC - Zero Page
                        dec(AddressingMode.ZERO_PAGE);
                        
                        break;
                    }
                    case 0xD6: {
                        // DEC - Zero Page,X
                        dec(AddressingMode.ZERO_PAGE_X);
                        
                        break;
                    }
                    case 0xCE: {
                        // DEC - Absolute
                        dec(AddressingMode.ABSOLUTE);
                        
                        break;
                    }
                    case 0xDE: {
                        // DEC - Absolute,X
                        dec(AddressingMode.ABSOLUTE_X);
                        
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
                        
                        break;
                    }
                    case 0x56: {
                        // LSR - Zero Page,X
                        lsr(AddressingMode.ZERO_PAGE_X);
                        
                        break;
                    }
                    case 0x4E: {
                        // LSR - Absolute
                        lsr(AddressingMode.ABSOLUTE);
                        
                        break;
                    }
                    case 0x5E: {
                        // LSR - Absolute,X
                        lsr(AddressingMode.ABSOLUTE_X);
                        
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
                        
                        break;
                    }
                    case 0x36: {
                        // ROL - Zero Page,X
                        rol(AddressingMode.ZERO_PAGE_X);
                        
                        break;
                    }
                    case 0x2E: {
                        // ROL - Absolute
                        rol(AddressingMode.ABSOLUTE);
                        
                        break;
                    }
                    case 0x3E: {
                        // ROL - Absolute,X
                        rol(AddressingMode.ABSOLUTE_X);
                        
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
                        
                        break;
                    }
                    case 0x76: {
                        // ROR - Zero Page,X
                        ror(AddressingMode.ZERO_PAGE_X);
                        
                        break;
                    }
                    case 0x6E: {
                        // ROR - Absolute
                        ror(AddressingMode.ABSOLUTE);
                        
                        break;
                    }
                    case 0x7E: {
                        // ROR - Absolute,X
                        ror(AddressingMode.ABSOLUTE_X);
                        
                        break;
                    }
                    case 0xE9: {
                        // SBC - Immediate
                        sbc(AddressingMode.IMMEDIATE);
                        
                        break;
                    }
                    case 0xE5: {
                        // SBC - Zero Page
                        sbc(AddressingMode.ZERO_PAGE);
                        
                        break;
                    }
                    case 0xF5: {
                        // SBC - Zero Page,X
                        sbc(AddressingMode.ZERO_PAGE_X);
                        
                        break;
                    }
                    case 0xED: {
                        // SBC - Absolute
                        sbc(AddressingMode.ABSOLUTE);
                        
                        break;
                    }
                    case 0xFD: {
                        // SBC - Absolute,X
                        sbc(AddressingMode.ABSOLUTE_X);
                        
                        break;
                    }
                    case 0xF9: {
                        // SBC - Absolute,Y
                        sbc(AddressingMode.ABSOLUTE_Y);
                        
                        break;
                    }
                    case 0xE1: {
                        // SBC - Indirect,X
                        sbc(AddressingMode.INDIRECT_X);
                        
                        break;
                    }
                    case 0xF1: {
                        // SBC - Indirect,Y
                        sbc(AddressingMode.INDIRECT_Y);
                        
                        break;
                    }
                    case 0xF8: {
                        // SED - Set Decimal Flag
                        sed();
                        break;
                    }
                    case 0x78: {
                        // SEI - Set Interrupt Disable
                        sei();
                        break;
                    }
                    case 0xEA:{
                        // NOP - No Operation
                        break;
                    }
                    case 0x40:
                        // RTI - Return from Interrupt
                        rti();
                        break;
                    case 0x00:
                        // BRK - Break (for this demo, we'll just stop execution)
                        // programCounter = memReadU16(0xFFFE); // IRQ/BRK vector
                        brk();  
                        break;  
                    default:
                        throw new UnsupportedOperationException("Opcode " + opcode + " not implemented yet.");
                }
            int extraCycles = cycles - extraCyclesBefore;
            return baseCycles + extraCycles;    
            //}
        }
}
