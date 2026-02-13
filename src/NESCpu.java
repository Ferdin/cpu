public class NESCpu {

    // Registers
    int A, X, Y = 0;
    int SP = 0xFF;  // Stack starts at 0x01FF (0x0100 + 0xFF)
    int PC = 0x8000;  // Programs typically start at 0x8000
    int cycles = 0; // For timing (not fully implemented here)
    // Status Flags
    boolean carryFlag = false;
    boolean zeroFlag = false;
    boolean negativeFlag = false;

    // FULL NES MEMORY
    int[] memory = new int[65536];  // 64 KB (0x0000 - 0xFFFF)
     
    int read(int address){
        return memory[address & 0xFFFF] & 0xFF; // Ensure we return a byte value
    }

    public void run() {
            while(true){
            int instruction = read(PC);
            PC++;

            switch(instruction){
                case 0:
                    System.out.println("Program finished!");
                    return;
                case 1: {
                    int number = memory[PC];
                    PC++;
                    A = number;
                    updateZeroAndNegativeFlags(A);
                    System.out.println("LDA: Loaded " + number + " into A.");
                    break;
                }
                case 2: {
                    A = A + X;
                    if(A > 255) A = A - 256;
                    updateZeroAndNegativeFlags(A);
                    System.out.println("ADD: Added X to A. A is now " + A);
                    break;
                }
                // Other cases handled below for clarity
                case 3: {
                    int number = memory[PC];
                    PC++;
                    X = number;
                    updateZeroAndNegativeFlags(X);
                    System.out.println("LDX: Loaded " + number + " into X.");
                    break;
                }
                case 4: {
                    int number = memory[PC];
                    PC++;
                    Y = number;
                    updateZeroAndNegativeFlags(Y);
                    System.out.println("LDY: Loaded " + number + " into Y.");
                    break;
                }
                case 5: {
                    int address = memory[PC];
                    PC++;
                    memory[address] = A;
                    System.out.println("STA: Stored A (" + A + ") to memory[" + address + "]");
                    break;
                }
                case 6: {
                    int address = memory[PC];
                    PC++;
                    A = memory[address];
                    updateZeroAndNegativeFlags(A);
                    System.out.println("LDA (from memory): Loaded " + A + " from memory[" + address + "]");
                    break;
                }
                case 7: {
                    X = A;
                    updateZeroAndNegativeFlags(X);
                    System.out.println("TAX: Transferred A to X. X is now " + X);
                    break;
                }
                case 8: {
                    A = X;
                    updateZeroAndNegativeFlags(A);
                    System.out.println("TXA: Transferred X to A. A is now " + A);
                    break;
                }
                case 9: {
                    X = X + 1;
                    if (X > 255) X = 0; // Wrap around
                    updateZeroAndNegativeFlags(X);
                    System.out.println("INX: Incremented X. X is now " + X);
                    break;
                }
                case 10: {
                    Y = Y + 1;
                    if (Y > 255) Y = 0; // Wrap around
                    updateZeroAndNegativeFlags(Y);
                    System.out.println("INY: Incremented Y. Y is now " + Y);
                    break;
                }
                case 11: {
                    X = X - 1;
                    if( X < 0) X = 255;
                    updateZeroAndNegativeFlags(X);
                    System.out.println("DEX: Decremented X. X is now " + X);
                    break;
                }   
                case 12:  {
                    int number = memory[PC];
                    PC++; // Move past the data
                    int result = A - number;
                    carryFlag = (A >= number);
                    updateZeroAndNegativeFlags(result);
                    System.out.println("CMP: Compared A (" + A + ") with " + number);
                    System.out.println("     Zero flag: " + zeroFlag + ", Negative flag: " + negativeFlag);
                    break;
                }
                case 13: {
                    // 13 = Branch if Zero flag is set
                    int offset = memory[PC];
                    PC++; // Move past the offset
                    if (zeroFlag) {
                        PC = offset;
                        System.out.println("BEQ: Zero flag is set! Jumping to position " + offset);
                    } else {
                        System.out.println("BEQ: Zero flag is not set. Continue normally.");
                    }
                    break;
                }
                case 14: {
                    // 14 = Branch if Zero flag is NOT set
                    int offset = memory[PC];
                    PC++; // Move past the offset
                    if (!zeroFlag) {
                        PC = offset;
                        System.out.println("BNE: Zero flag is not set! Jumping to position " + offset);
                    } else {
                        System.out.println("BNE: Zero flag is set. Continue normally.");
                    }
                    break;
                }
                case 15: {
                    // 15 = CPX - Compare X with a number
                    int number = memory[PC];
                    PC++;
                    int result = X - number;
                    updateZeroAndNegativeFlags(result);
                    System.out.println("CPX: Compared X(" + X + ") with " + number);
                    System.out.println("Zero flag: " + zeroFlag + ", Negative flag: " + negativeFlag);
                    break;
                }
                case 16:{
                    // 16 = CPY - Compare Y with a number
                    int number = memory[PC];
                    PC++;
                    int result = Y - number;
                    updateZeroAndNegativeFlags(result);
                    System.out.println("CPY: Compared Y (" + Y + ") with " + number);
                    System.out.println("     Zero flag: " + zeroFlag + ", Negative flag: " + negativeFlag);
                    break;
                }
                case 17: {
                    // 17 = ADC - Add with Carry
                    int number = memory[PC];
                    PC++;
                    
                    // Add with old carry
                    int result = A + number + (carryFlag ? 1 : 0);
                    
                    // Set NEW carry if overflow
                    carryFlag = (result > 255);  // ← Check BEFORE masking!
                    
                    // Store result
                    A = result & 0xFF; // Bitwise AND operation (1 + 0 = 0, 1 + 1 = 1, 0 + 0 = 0, 1 + 0 = 0)
                    
                    // Update Z and N
                    updateZeroAndNegativeFlags(A);
                    
                    System.out.println("ADC: Added " + number + " to A. A is now " + A);
                    System.out.println("     Carry flag: " + carryFlag);
                    break;
                }
                case 18:
                    {// 18 = CLC - Clear Carry Flag
                    carryFlag = false;
                    System.out.println("CLC: Cleared carry flag");
                    break;}
                case 19:
                    {// 19 = SEC - Set Carry Flag
                    carryFlag = true;
                    System.out.println("SEC: Set carry flag");  
                    break; } 
                case 20:
                    {// 20 = SBC - Subtract with Carry
                    int number = memory[PC];
                    PC++;
                    
                    int result = A - number - (carryFlag ? 0 : 1); // Subtract with old carry (if carry is clear, we subtract an extra 1)
                    carryFlag = (result >= 0);

                    if (result < 0) {
                        result = result + 256; // Wrap around for negative results
                    }

                    A = result & 0xFF;

                    updateZeroAndNegativeFlags(A);
                    System.out.println("SBC: Subtracted " + number + " from A. A is now " + A);
                    System.out.println("     Carry flag: " + carryFlag);
                    break;
                }
                case 21: {
                    // 21 = PHA - Push A onto stack
                    memory[0x0100 + SP] = A; // Stack is at 0x0100 - 0x01FF
                    SP = (SP - 1) & 0xFF; // Decrement stack pointer and wrap at 0 
                    System.out.println("PHA: Pushed A (" + A + ") onto stack. SP is now " + SP);
                    break;
                }
                case 22: {
                    // 22 = PLA - Pull A from stack
                    SP = (SP + 1) & 0xFF; // Move SP up
                    A = memory[0x0100 + SP];
                    updateZeroAndNegativeFlags(A);
                    System.out.println("PLA: Pulled A (" + A + ") from stack. SP is now " + SP);
                    break;
                }
                case 23: {
                    // 23 = PHP - Push Processor Status
                    // Combine all flags into one byte
                    int status = 0;  // 00000000
                    if (carryFlag) status |= 0x01; // Set bit 0
                    // status = 00000000 | 00000001 = 00000001
                    if (zeroFlag) status |= 0x02; // Set bit 1
                    // status = 00000001 | 00000010 = 00000011
                    if (negativeFlag) status |= 0x80; // Set bit 7
                    // status = 00000011 | 10000000 = 10000011
                    // ... other flags ...
                    
                    memory[0x0100 + SP] = status;
                    SP = (SP - 1) & 0xFF;
                    System.out.println("PHP: Pushed status to stack");
                    break;
                }
                case 24: {
                    // 24 = PLP - Pull Processor Status
                    SP = (SP + 1) & 0xFF;
                    int status = memory[0x0100 + SP];
                    
                    // Extract flags
                    carryFlag = (status & 0x01) != 0;
                    zeroFlag = (status & 0x02) != 0;
                    negativeFlag = (status & 0x80) != 0;
                    // ... other flags ...
                    
                    System.out.println("PLP: Pulled status from stack");
                    break;
                }
                default:
                    System.out.println("Unknown instruction: " + instruction);
                    return;
            }
            System.out.println("     A=" + A + " X=" + X + " Y=" + Y + " PC=" + PC);
            System.out.println();
        }
    
    }

    public void reset() {
        A = 0;
        X = 0;
        Y = 0;
        SP = 0xFF;  // Stack starts at 0x01FF (0x0100 + 0xFF)
        PC = 0x8000;  // Programs typically start at 0x8000
        
        // Clear flags
        carryFlag = false;
        zeroFlag = false;
        negativeFlag = false;
        
        // Initialize memory to 0
        for (int i = 0; i < memory.length; i++) {
            memory[i] = 0;
        }
    }

    private void updateZeroAndNegativeFlags(int value) {
        value = value & 255;
        zeroFlag = (value == 0);
        negativeFlag = (value >= 128) ;
    }
    public static void main(String[] args){
        exampleStack();
    }

    // Example program to test the CPU
    public static void exampleStack() {
        NESCpu cpu = new NESCpu();
        
        int addr = 0x8000;
        // Save A to stack, change A, then restore it
        cpu.memory[addr++] = 1;    // LDA
        cpu.memory[addr++] = 42;   // Load 42
        cpu.memory[addr++] = 21;   // PHA (save 42 to stack)
        
        cpu.memory[addr++] = 1;    // LDA
        cpu.memory[addr++] = 99;   // Load 99 (change A)
        
        cpu.memory[addr++] = 22;   // PLA (restore 42 from stack)
        cpu.memory[addr++] = 0;    // Stop
        
        cpu.run();
    }
}


