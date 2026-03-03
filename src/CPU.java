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

    private int stack = 0x0100;
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

    public int brk() {

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

    static {
        // LDA Immediate
        OPCODES[0xA9] = new Instruction("LDA", AddressingMode.IMMEDIATE, (cpu, mode) -> cpu.lda(mode));
        // LDA Zero Page
        OPCODES[0xA5] = new Instruction("LDA", AddressingMode.ZERO_PAGE, (cpu, mode) -> cpu.lda(mode));
        // LDX Immediate
        OPCODES[0xA2] = new Instruction("LDX", AddressingMode.IMMEDIATE, (cpu, mode) -> cpu.ldx(mode));
        // DEX
        OPCODES[0xCA] = new Instruction("DEX", AddressingMode.IMPLIED, (cpu, mode) -> cpu.dex());
        // DEY
        OPCODES[0x88] = new Instruction("DEY", AddressingMode.IMPLIED, (cpu, mode) -> cpu.dey());
        // BRK
        OPCODES[0x00] = new Instruction("BRK", AddressingMode.IMPLIED, (cpu, mode) -> cpu.brk());
        // ... add all other opcodes similarly
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
