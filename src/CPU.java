public class CPU implements Mem{

    @FunctionalInterface
    public interface CpuCallback {
        void execute(CPU cpu);
    }

    public static final Instruction[] OPCODES = new Instruction[256];

    // CPU Registers (8-bit)
    private int registerA;
    private int registerX;
    private int registerY;
    private int stackPointer;
    private int status;

    // Program Counter (16-bit)
    private int programCounter;

    // private int stack = 0x0100;
    private int stack_reset = 0xFD;

    // Bus
    private Bus bus;

    // Cycle tracking
    private int cycles = 0;         // Total cycles elapsed

    // Helper flag for Carry
    private boolean carryFlag = false;

    // Flag bit masks
    private static final int CARRY             = 0b00000001;
    private static final int ZERO              = 0b00000010;
    private static final int INTERRUPT_DISABLE = 0b00000100;
    private static final int DECIMAL_MODE      = 0b00001000;
    private static final int BREAK             = 0b00010000;
    private static final int BREAK2            = 0b00100000;
    private static final int OVERFLOW          = 0b01000000;
    private static final int NEGATIVE          = 0b10000000;

    public CPU(Bus bus) {
        this.bus = bus;
        reset();
    }

    public void reset() {
        registerA = 0;
        registerX = 0;
        registerY = 0;
        stackPointer = stack_reset;
        status = 0x24;
        programCounter = memReadU16(0xFFFC); // Reset vector
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
    
    public int getRegisterY() {
        return registerY;
    }

    public int getStackPointer() {
        return stackPointer;
    }

    public int setRegisterA(int value) {
        registerA = value & 0xFF;
        return registerA;
    }

    public int setRegisterX(int value) {
        registerX = value & 0xFF;
        return registerX;
    }

    public int setRegisterY(int value) {
        registerY = value & 0xFF;
        return registerY;
    }

    public int setStackPointer(int value) {
        stackPointer = value & 0xFF;
        return stackPointer;
    }

    public int setProgramCounter(int value) {
        programCounter = value & 0xFFFF;
        return programCounter;
    }

    public int setStatus(int value) {
        status = value & 0xFF;
        return status;
    }

    public void runUntilBreak(java.util.function.Consumer<CPU> callback) {
        while (true) {
            if (callback != null) {
                callback.accept(this);
            }

            int opcode = memRead(programCounter) & 0xFF;

            step();  // always execute instruction

            if (opcode == 0x00) {  // BRK
                break;
            }
        }
    }

    public void runWithCallback(java.util.function.Consumer<CPU> callback) {
        runUntilBreak(callback);
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
        // boolean running = true;
        // while(running) {
        //     int opcode = memRead(programCounter) & 0xFF;
        //     if(opcode == 0x00) {  // BRK
        //         step();           // execute BRK
        //         running = false;
        //     } else {
        //         step();
        //     }
        // }
        runUntilBreak(null);
    }

    public void load(int[] program) {
        for (int i = 0; i < program.length; i++) {
            memWrite(0x0000 + i, program[i]);
        }
        memWriteU16(0xFFFC, 0x0000);
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

    public void  brk() {

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

        // return Cycles.CYCLES[0x00];  // BRK cycles
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
    
    public void sbc(AddressingMode mode) {

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

    public void txs(){
        stackPointer = registerX;
    }

    public void nop() {
        // No operation, just consume cycles
       // cycles += Cycles.CYCLES[0xEA]; // NOP opcode
    }

    static {
        // LDA Immediate
        OPCODES[0xA9] = new Instruction("LDA", AddressingMode.IMMEDIATE, (cpu, mode) -> cpu.lda(mode));
        // LDA Zero Page
        OPCODES[0xA5] = new Instruction("LDA", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.lda(mode));
        // LDA Absolute
        OPCODES[0xAD] = new Instruction("LDA", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.lda(mode));
        // LDA Zero Page,X
        OPCODES[0xB5] = new Instruction("LDA", AddressingMode.ZERO_PAGE_X, (cpu, mode) -> cpu.lda(mode));
        // LDA Absolute,X
        OPCODES[0xBD] = new Instruction("LDA", AddressingMode.ABSOLUTE_X, (cpu, mode) -> cpu.lda(mode));
        // LDA Absolute,Y
        OPCODES[0xB9] = new Instruction("LDA", AddressingMode.ABSOLUTE_Y, (cpu, mode) -> cpu.lda(mode));
        // LDA (Indirect,X)
        OPCODES[0xA1] = new Instruction("LDA", AddressingMode.INDIRECT_X, (cpu, mode) -> cpu.lda(mode));
        // LDA (Indirect),Y
        OPCODES[0xB1] = new Instruction("LDA", AddressingMode.INDIRECT_Y, (cpu, mode) -> cpu.lda(mode));
        // STA Zero Page
        OPCODES[0x85] = new Instruction("STA", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.sta(mode));
        // STA Absolute
        OPCODES[0x8D] = new Instruction("STA", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.sta(mode));
        // STA Zero Page,X
        OPCODES[0x95] = new Instruction("STA", AddressingMode.ZERO_PAGE_X, (cpu, mode) -> cpu.sta(mode));
        // STA Absolute,X
        OPCODES[0x9D] = new Instruction("STA", AddressingMode.ABSOLUTE_X, (cpu, mode) -> cpu.sta(mode));
        // STA Absolute,Y
        OPCODES[0x99] = new Instruction("STA", AddressingMode.ABSOLUTE_Y, (cpu, mode) -> cpu.sta(mode));
        // STA (Indirect,X)
        OPCODES[0x81] = new Instruction("STA", AddressingMode.INDIRECT_X, (cpu, mode) -> cpu.sta(mode));
        // STA (Indirect),Y
        OPCODES[0x91] = new Instruction("STA", AddressingMode.INDIRECT_Y, (cpu, mode) -> cpu.sta(mode));
        // STX Zero Page
        OPCODES[0x86] = new Instruction("STX", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.stx(mode));
        // STX Absolute
        OPCODES[0x8E] = new Instruction("STX", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.stx(mode));
        // STX Zero Page,Y
        OPCODES[0x96] = new Instruction("STX", AddressingMode.ZERO_PAGE_Y, (cpu, mode) -> cpu.stx(mode));
        // STY Zero Page
        OPCODES[0x84] = new Instruction("STY", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.sty(mode));
        // STY Absolute
        OPCODES[0x8C] = new Instruction("STY", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.sty(mode));
        // STY Zero Page,X
        OPCODES[0x94] = new Instruction("STY", AddressingMode.ZERO_PAGE_X, (cpu, mode) -> cpu.sty(mode));
        // TAX
        OPCODES[0xAA] = new Instruction("TAX", AddressingMode.IMPLIED, (cpu, mode) -> cpu.tax());
        // TAY
        OPCODES[0xA8] = new Instruction("TAY", AddressingMode.IMPLIED, (cpu, mode) -> cpu.tay());
        // INX
        OPCODES[0xE8] = new Instruction("INX", AddressingMode.IMPLIED, (cpu, mode) -> cpu.inx());
        // INY
        OPCODES[0xC8] = new Instruction("INY", AddressingMode.IMPLIED, (cpu, mode) -> cpu.iny());
        // INC - Zero Page
        OPCODES[0xE6] = new Instruction("INC", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.inc(mode));
        // INC - Absolute
        OPCODES[0xEE] = new Instruction("INC", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.inc(mode));
        // INC - Zero Page,X
        OPCODES[0xF6] = new Instruction("INC", AddressingMode.ZERO_PAGE_X, (cpu, mode) -> cpu.inc(mode));
        // INC - Absolute,X
        OPCODES[0xFE] = new Instruction("INC", AddressingMode.ABSOLUTE_X, (cpu, mode) -> cpu.inc(mode));
        // AND - Immediate
        OPCODES[0x29] = new Instruction("AND", AddressingMode.IMMEDIATE, (cpu, mode) -> cpu.and(mode));
        // AND - Zero Page
        OPCODES[0x25] = new Instruction("AND", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.and(mode));
        // AND - Absolute
        OPCODES[0x2D] = new Instruction("AND", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.and(mode));
        // AND - Zero Page,X
        OPCODES[0x35] = new Instruction("AND", AddressingMode.ZERO_PAGE_X, (cpu, mode) -> cpu.and(mode));
        // AND - Absolute,X
        OPCODES[0x3D] = new Instruction("AND", AddressingMode.ABSOLUTE_X, (cpu, mode) -> cpu.and(mode));
        // AND - Absolute,Y
        OPCODES[0x39] = new Instruction("AND", AddressingMode.ABSOLUTE_Y, (cpu, mode) -> cpu.and(mode));
        // AND - Indirect,X
        OPCODES[0x21] = new Instruction("AND", AddressingMode.INDIRECT_X, (cpu, mode) -> cpu.and(mode));
        // AND - Indirect,Y
        OPCODES[0x31] = new Instruction("AND", AddressingMode.INDIRECT_Y, (cpu, mode) -> cpu.and(mode));
        // TYA 
        OPCODES[0x98] = new Instruction("TYA", AddressingMode.IMPLIED, (cpu, mode) -> cpu.tya());
        // TXA
        OPCODES[0x8A] = new Instruction("TXA", AddressingMode.IMPLIED, (cpu, mode) -> cpu.txa());
        // TSX
        OPCODES[0xBA] = new Instruction("TSX", AddressingMode.IMPLIED, (cpu, mode) -> cpu.tsx());
        // PLA
        OPCODES[0x68] = new Instruction("PLA", AddressingMode.IMPLIED, (cpu, mode) -> cpu.pla());
        // ORA - Immediate
        OPCODES[0x09] = new Instruction("ORA", AddressingMode.IMMEDIATE, (cpu, mode) -> cpu.ora(mode));
        // ORA - Zero Page
        OPCODES[0x05] = new Instruction("ORA", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.ora(mode));
        // ORA - Absolute
        OPCODES[0x0D] = new Instruction("ORA", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.ora(mode));
        // ORA - Zero Page,X
        OPCODES[0x15] = new Instruction("ORA", AddressingMode.ZERO_PAGE_X, (cpu, mode) -> cpu.ora(mode));
        // ORA - Absolute,X
        OPCODES[0x1D] = new Instruction("ORA", AddressingMode.ABSOLUTE_X, (cpu, mode) -> cpu.ora(mode));
        // ORA - Absolute,Y
        OPCODES[0x19] = new Instruction("ORA", AddressingMode.ABSOLUTE_Y, (cpu, mode) -> cpu.ora(mode));
        // ORA - Indirect,X
        OPCODES[0x01] = new Instruction("ORA", AddressingMode.INDIRECT_X, (cpu, mode) -> cpu.ora(mode));
        // ORA - Indirect,Y
        OPCODES[0x11] = new Instruction("ORA", AddressingMode.INDIRECT_Y, (cpu, mode) -> cpu.ora(mode));
        // LDX - Immediate
        OPCODES[0xA2] = new Instruction("LDX", AddressingMode.IMMEDIATE, (cpu, mode) -> cpu.ldx(mode));
        // LDX - Zero Page
        OPCODES[0xA6] = new Instruction("LDX", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.ldx(mode));
        // LDX - Absolute
        OPCODES[0xAE] = new Instruction("LDX", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.ldx(mode));
        // LDX - Zero Page,Y
        OPCODES[0xB6] = new Instruction("LDX", AddressingMode.ZERO_PAGE_Y, (cpu, mode) -> cpu.ldx(mode));
        // LDX - Absolute,Y
        OPCODES[0xBE] = new Instruction("LDX", AddressingMode.ABSOLUTE_Y, (cpu, mode) -> cpu.ldx(mode));
        // LDY - Immediate
        OPCODES[0xA0] = new Instruction("LDY", AddressingMode.IMMEDIATE, (cpu, mode) -> cpu.ldy(mode));
        // LDY - Zero Page
        OPCODES[0xA4] = new Instruction("LDY", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.ldy(mode));
        // LDY - Absolute
        OPCODES[0xAC] = new Instruction("LDY", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.ldy(mode));
        // LDY - Zero Page,X
        OPCODES[0xB4] = new Instruction("LDY", AddressingMode.ZERO_PAGE_X, (cpu, mode) -> cpu.ldy(mode));
        // LDY - Absolute,X
        OPCODES[0xBC] = new Instruction("LDY", AddressingMode.ABSOLUTE_X, (cpu, mode) -> cpu.ldy(mode));
        // EOR - Immediate
        OPCODES[0x49] = new Instruction("EOR", AddressingMode.IMMEDIATE, (cpu, mode) -> cpu.eor(mode));
        // EOR - Zero Page
        OPCODES[0x45] = new Instruction("EOR", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.eor(mode));
        // EOR - Absolute
        OPCODES[0x4D] = new Instruction("EOR", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.eor(mode));
        // EOR - Zero Page,X
        OPCODES[0x55] = new Instruction("EOR", AddressingMode.ZERO_PAGE_X, (cpu, mode) -> cpu.eor(mode));
        // EOR - Absolute,X
        OPCODES[0x5D] = new Instruction("EOR", AddressingMode.ABSOLUTE_X, (cpu, mode) -> cpu.eor(mode));
        // EOR - Absolute,Y
        OPCODES[0x59] = new Instruction("EOR", AddressingMode.ABSOLUTE_Y, (cpu, mode) -> cpu.eor(mode));
        // EOR - Indirect,X
        OPCODES[0x41] = new Instruction("EOR", AddressingMode.INDIRECT_X, (cpu, mode) -> cpu.eor(mode));
        // EOR - Indirect,Y
        OPCODES[0x51] = new Instruction("EOR", AddressingMode.INDIRECT_Y, (cpu, mode) -> cpu.eor(mode));
        // DEX 
        OPCODES[0xCA] = new Instruction("DEX", AddressingMode.IMPLIED, (cpu, mode) -> cpu.dex());
        // DEY
        OPCODES[0x88] = new Instruction("DEY", AddressingMode.IMPLIED, (cpu, mode) -> cpu.dey());
        // TXS
        OPCODES[0x9A] = new Instruction("TXS", AddressingMode.IMPLIED, (cpu, mode) -> cpu.txs());
        // ADC - Immediate
        OPCODES[0x69] = new Instruction("ADC", AddressingMode.IMMEDIATE, (cpu, mode) -> cpu.adc(mode));
        // ADC - Zero Page
        OPCODES[0x65] = new Instruction("ADC", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.adc(mode));
        // ADC - Absolute
        OPCODES[0x6D] = new Instruction("ADC", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.adc(mode));
        // ADC - Zero Page,X
        OPCODES[0x75] = new Instruction("ADC", AddressingMode.ZERO_PAGE_X, (cpu, mode) -> cpu.adc(mode));
        // ADC - Absolute,X
        OPCODES[0x7D] = new Instruction("ADC", AddressingMode.ABSOLUTE_X, (cpu, mode) -> cpu.adc(mode));
        // ADC - Absolute,Y
        OPCODES[0x79] = new Instruction("ADC", AddressingMode.ABSOLUTE_Y, (cpu, mode) -> cpu.adc(mode));
        // ADC - Indirect,X
        OPCODES[0x61] = new Instruction("ADC", AddressingMode.INDIRECT_X, (cpu, mode) -> cpu.adc(mode));
        // ADC - Indirect,Y
        OPCODES[0x71] = new Instruction("ADC", AddressingMode.INDIRECT_Y, (cpu, mode) -> cpu.adc(mode));
        // ASL Accumulator
        OPCODES[0x0A] = new Instruction("ASL", AddressingMode.ACCUMULATOR, (cpu, mode) -> cpu.aslAccumulator());
        // ASL Zero Page
        OPCODES[0x06] = new Instruction("ASL", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.asl(mode));
        // ASL Absolute
        OPCODES[0x0E] = new Instruction("ASL", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.asl(mode));
        // ASL Zero Page,X
        OPCODES[0x16] = new Instruction("ASL", AddressingMode.ZERO_PAGE_X, (cpu, mode) -> cpu.asl(mode));
        // ASL Absolute,X
        OPCODES[0x1E] = new Instruction("ASL", AddressingMode.ABSOLUTE_X, (cpu, mode) -> cpu.asl(mode));
        // BCC 
        OPCODES[0x90] = new Instruction("BCC", AddressingMode.IMPLIED, (cpu, mode) -> cpu.bcc());
        // BCS
        OPCODES[0xB0] = new Instruction("BCS", AddressingMode.IMPLIED, (cpu, mode) -> cpu.bcs());
        // SEC
        OPCODES[0x38] = new Instruction("SEC", AddressingMode.IMPLIED, (cpu, mode) -> cpu.sec());
        // BEQ
        OPCODES[0xF0] = new Instruction("BEQ", AddressingMode.IMPLIED, (cpu, mode) -> cpu.beq());
        // BIT - Zero Page
        OPCODES[0x24] = new Instruction("BIT", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.bit(mode));
        // BIT - Absolute
        OPCODES[0x2C] = new Instruction("BIT", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.bit(mode));
        // BMI
        OPCODES[0x30] = new Instruction("BMI", AddressingMode.IMPLIED, (cpu, mode) -> cpu.bmi());
        // BNE
        OPCODES[0xD0] = new Instruction("BNE", AddressingMode.IMPLIED, (cpu, mode) -> cpu.bne());
        // BPL
        OPCODES[0x10] = new Instruction("BPL", AddressingMode.IMPLIED, (cpu, mode) -> cpu.bpl());
        // BVC
        OPCODES[0x50] = new Instruction("BVC", AddressingMode.IMPLIED, (cpu, mode) -> cpu.bvc());
        // BVS
        OPCODES[0x70] = new Instruction("BVS", AddressingMode.IMPLIED, (cpu, mode) -> cpu.bvs());
        // CLC
        OPCODES[0x18] = new Instruction("CLC", AddressingMode.IMPLIED, (cpu, mode) -> cpu.clc());
        // CLD
        OPCODES[0xD8] = new Instruction("CLD", AddressingMode.IMPLIED, (cpu, mode) -> cpu.cld());
        // CLI
        OPCODES[0x58] = new Instruction("CLI", AddressingMode.IMPLIED, (cpu, mode) -> cpu.cli());
        // CLV
        OPCODES[0xB8] = new Instruction("CLV", AddressingMode.IMPLIED, (cpu, mode) -> cpu.clv());
        // CMP - Immediate
        OPCODES[0xC9] = new Instruction("CMP", AddressingMode.IMMEDIATE, (cpu, mode) -> cpu.cmp(mode));
        // CMP - Zero Page
        OPCODES[0xC5] = new Instruction("CMP", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.cmp(mode));
        // CMP - Absolute
        OPCODES[0xCD] = new Instruction("CMP", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.cmp(mode));
        // CMP - Zero Page,X
        OPCODES[0xD5] = new Instruction("CMP", AddressingMode.ZERO_PAGE_X, (cpu, mode) -> cpu.cmp(mode));
        // CMP - Absolute,X
        OPCODES[0xDD] = new Instruction("CMP", AddressingMode.ABSOLUTE_X, (cpu, mode) -> cpu.cmp(mode));
        // CMP - Absolute,Y
        OPCODES[0xD9] = new Instruction("CMP", AddressingMode.ABSOLUTE_Y, (cpu, mode) -> cpu.cmp(mode));
        // CMP - Indirect,X
        OPCODES[0xC1] = new Instruction("CMP", AddressingMode.INDIRECT_X, (cpu, mode) -> cpu.cmp(mode));
        // CMP - Indirect,Y
        OPCODES[0xD1] = new Instruction("CMP", AddressingMode.INDIRECT_Y, (cpu, mode) -> cpu.cmp(mode));
        // CPX - Immediate
        OPCODES[0xE0] = new Instruction("CPX", AddressingMode.IMMEDIATE, (cpu, mode) -> cpu.cpx(mode));
        // CPX - Zero Page
        OPCODES[0xE4] = new Instruction("CPX", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.cpx(mode));
        // CPX - Absolute
        OPCODES[0xEC] = new Instruction("CPX", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.cpx(mode));
        // CPY - Immediate
        OPCODES[0xC0] = new Instruction("CPY", AddressingMode.IMMEDIATE, (cpu, mode) -> cpu.cpy(mode));
        // CPY - Zero Page
        OPCODES[0xC4] = new Instruction("CPY", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.cpy(mode));
        // CPY - Absolute
        OPCODES[0xCC] = new Instruction("CPY", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.cpy(mode));
        // DEC - Zero Page
        OPCODES[0xC6] = new Instruction("DEC", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.dec(mode));
        // DEC - Absolute
        OPCODES[0xCE] = new Instruction("DEC", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.dec(mode));
        // DEC - Zero Page,X
        OPCODES[0xD6] = new Instruction("DEC", AddressingMode.ZERO_PAGE_X, (cpu, mode) -> cpu.dec(mode));
        // DEC - Absolute,X
        OPCODES[0xDE] = new Instruction("DEC", AddressingMode.ABSOLUTE_X, (cpu, mode) -> cpu.dec(mode));
        // JMP Absolute
        OPCODES[0x4C] = new Instruction("JMP", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.jmpAbsolute());
        // JMP Indirect
        OPCODES[0x6C] = new Instruction("JMP", AddressingMode.INDIRECT, (cpu, mode) -> cpu.jmpIndirect());
        // JSR
        OPCODES[0x20] = new Instruction("JSR", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.jsr());
        // RTS
        OPCODES[0x60] = new Instruction("RTS", AddressingMode.IMPLIED, (cpu, mode) -> cpu.rts());
        // LSR Accumulator
        OPCODES[0x4A] = new Instruction("LSR", AddressingMode.ACCUMULATOR, (cpu, mode) -> cpu.lsrAccumulator());
        // LSR Zero Page
        OPCODES[0x46] = new Instruction("LSR", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.lsr(mode));
        // LSR Absolute
        OPCODES[0x4E] = new Instruction("LSR", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.lsr(mode));
        // LSR Zero Page,X
        OPCODES[0x56] = new Instruction("LSR", AddressingMode.ZERO_PAGE_X, (cpu, mode) -> cpu.lsr(mode));
        // LSR Absolute,X
        OPCODES[0x5E] = new Instruction("LSR", AddressingMode.ABSOLUTE_X, (cpu, mode) -> cpu.lsr(mode));
        // PHA 
        OPCODES[0x68] = new Instruction("PHA", AddressingMode.IMPLIED, (cpu, mode) -> cpu.pha());
        // PHP
        OPCODES[0x08] = new Instruction("PHP", AddressingMode.IMPLIED, (cpu, mode) -> cpu.php());
        // PLP
        OPCODES[0x28] = new Instruction("PLP", AddressingMode.IMPLIED, (cpu, mode) -> cpu.plp());
        // ROL Accumulator
        OPCODES[0x2A] = new Instruction("ROL", AddressingMode.ACCUMULATOR, (cpu, mode) -> cpu.rolAccumulator());
        // ROL Zero Page
        OPCODES[0x26] = new Instruction("ROL", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.rol(mode));
        // ROL Absolute
        OPCODES[0x2E] = new Instruction("ROL", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.rol(mode));
        // ROL Zero Page,X
        OPCODES[0x36] = new Instruction("ROL", AddressingMode.ZERO_PAGE_X, (cpu, mode) -> cpu.rol(mode));
        // ROL Absolute,X
        OPCODES[0x3E] = new Instruction("ROL", AddressingMode.ABSOLUTE_X, (cpu, mode) -> cpu.rol(mode));
        // ROR Accumulator
        OPCODES[0x6A] = new Instruction("ROR", AddressingMode.ACCUMULATOR, (cpu, mode) -> cpu.rorAccumulator());
        // ROR Zero Page
        OPCODES[0x66] = new Instruction("ROR", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.ror(mode));
        // ROR Absolute
        OPCODES[0x6E] = new Instruction("ROR", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.ror(mode));
        // ROR Zero Page,X
        OPCODES[0x76] = new Instruction("ROR", AddressingMode.ZERO_PAGE_X, (cpu, mode) -> cpu.ror(mode));
        // ROR Absolute,X
        OPCODES[0x7E] = new Instruction("ROR", AddressingMode.ABSOLUTE_X, (cpu, mode) -> cpu.ror(mode));
        // SBC - Immediate
        OPCODES[0xE9] = new Instruction("SBC", AddressingMode.IMMEDIATE, (cpu, mode) -> cpu.sbc(mode));
        // SBC - Zero Page
        OPCODES[0xE5] = new Instruction("SBC", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.sbc(mode));
        // SBC - Absolute
        OPCODES[0xED] = new Instruction("SBC", AddressingMode.ABSOLUTE, (cpu, mode) -> cpu.sbc(mode));
        // SBC - Zero Page,X
        OPCODES[0xF5] = new Instruction("SBC", AddressingMode.ZERO_PAGE_X, (cpu, mode) -> cpu.sbc(mode));
        // SBC - Absolute,X
        OPCODES[0xFD] = new Instruction("SBC", AddressingMode.ABSOLUTE_X, (cpu, mode) -> cpu.sbc(mode));
        // SBC - Absolute,Y
        OPCODES[0xF9] = new Instruction("SBC", AddressingMode.ABSOLUTE_Y, (cpu, mode) -> cpu.sbc(mode));
        // SBC - Indirect,X
        OPCODES[0xE1] = new Instruction("SBC", AddressingMode.INDIRECT_X, (cpu, mode) -> cpu.sbc(mode));
        // SBC - Indirect,Y
        OPCODES[0xF1] = new Instruction("SBC", AddressingMode.INDIRECT_Y, (cpu, mode) -> cpu.sbc(mode));
        // SED
        OPCODES[0xF8] = new Instruction("SED", AddressingMode.IMPLIED, (cpu, mode) -> cpu.sed());
        // SEI
        OPCODES[0x78] = new Instruction("SEI", AddressingMode.IMPLIED, (cpu, mode) -> cpu.sei());
        // NOP
        OPCODES[0xEA] = new Instruction("NOP", AddressingMode.IMPLIED, (cpu, mode) -> cpu.nop());
        // RTI
        OPCODES[0x40] = new Instruction("RTI", AddressingMode.IMPLIED, (cpu, mode) -> cpu.rti());
        // BRK
        OPCODES[0x00] = new Instruction("BRK", AddressingMode.IMPLIED, (cpu, mode) -> cpu.brk());
    }

    public int step() {
        // Read opcode (convert signed byte to unsigned)
        int opcode = memRead(programCounter++) & 0xFF;
        Instruction instr = OPCODES[opcode];
        int baseCycles = Cycles.CYCLES[opcode];
        int extraCyclesBefore = cycles;

        if (instr != null) {
            instr.execute.accept(this, instr.mode);
        } else {
            throw new UnsupportedOperationException(
                String.format("Opcode 0x%02X not implemented", opcode)
            );
        }
        int extraCycles = cycles - extraCyclesBefore;
        return baseCycles + extraCycles;  
    }
}
