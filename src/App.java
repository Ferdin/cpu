public class App {

    int A = 0;
    int X = 0;
    int Y = 0;

    int[] memory = new int[256];

    int programCounter = 0;

    boolean zeroFlag = false;
    boolean negativeFlag = false;
    boolean carryFlag = false;

    public void run(){
        while(true){
            int instruction = memory[programCounter];
            programCounter++;

            switch(instruction){
                case 0:
                    System.out.println("Program finished!");
                    return;
                case 1: {
                    int number = memory[programCounter];
                    programCounter++;
                    A = number;
                    updateZeroAndNegativeFlags(A);
                    System.out.println("LDA: Loaded " + number + " into A.");
                    break;
                }
                case 2: {
                    A = A + X;
                    if(A > 255) A = A - 256;
                    updateZeroAndNegativeFlags(A);
                    System.out.println("ADC: Added X to A. A is now " + A);
                    break;
                }
                // Other cases handled below for clarity
                case 3: {
                    int number = memory[programCounter];
                    programCounter++;
                    X = number;
                    updateZeroAndNegativeFlags(X);
                    System.out.println("LDX: Loaded " + number + " into X.");
                    break;
                }
                case 4: {
                    int number = memory[programCounter];
                    programCounter++;
                    Y = number;
                    updateZeroAndNegativeFlags(Y);
                    System.out.println("LDY: Loaded " + number + " into Y.");
                    break;
                }
                case 5: {
                    int address = memory[programCounter];
                    programCounter++;
                    memory[address] = A;
                    System.out.println("STA: Stored A (" + A + ") to memory[" + address + "]");
                    break;
                }
                case 6: {
                    int address = memory[programCounter];
                    programCounter++;
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
                    int number = memory[programCounter];
                    programCounter++; // Move past the data
                    int result = A - number;
                    carryFlag = (A >= number);
                    updateZeroAndNegativeFlags(result);
                    System.out.println("CMP: Compared A (" + A + ") with " + number);
                    System.out.println("     Zero flag: " + zeroFlag + ", Negative flag: " + negativeFlag);
                    break;
                }
                case 13: {
                    // 13 = Branch if Zero flag is set
                    int offset = memory[programCounter];
                    programCounter++; // Move past the offset
                    if (zeroFlag) {
                        programCounter = offset;
                        System.out.println("BEQ: Zero flag is set! Jumping to position " + offset);
                    } else {
                        System.out.println("BEQ: Zero flag is not set. Continue normally.");
                    }
                    break;
                }
                case 14: {
                    // 14 = Branch if Zero flag is NOT set
                    int offset = memory[programCounter];
                    programCounter++; // Move past the offset
                    if (!zeroFlag) {
                        programCounter = offset;
                        System.out.println("BNE: Zero flag is not set! Jumping to position " + offset);
                    } else {
                        System.out.println("BNE: Zero flag is set. Continue normally.");
                    }
                    break;
                }
                case 15: {
                    // 15 = CPX - Compare X with a number
                    int number = memory[programCounter];
                    programCounter++;
                    int result = X - number;
                    updateZeroAndNegativeFlags(result);
                    System.out.println("CPX: Compared X(" + X + ") with " + number);
                    System.out.println("Zero flag: " + zeroFlag + ", Negative flag: " + negativeFlag);
                    break;
                }
                case 16:{
                    // 16 = CPY - Compare Y with a number
                    int number = memory[programCounter];
                    programCounter++;
                    int result = Y - number;
                    updateZeroAndNegativeFlags(result);
                    System.out.println("CPY: Compared Y (" + Y + ") with " + number);
                    System.out.println("     Zero flag: " + zeroFlag + ", Negative flag: " + negativeFlag);
                    break;
                }
                case 17: {
                    // 17 = ADC - Add with Carry
                    int number = memory[programCounter];
                    programCounter++;
                    
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
                    int number = memory[programCounter];
                    programCounter++;
                    
                    int result = A - number - (carryFlag ? 0 : 1); // Subtract with old carry (if carry is clear, we subtract an extra 1)
                    carryFlag = (result >= 0);

                    if (result < 0) {
                        result = result + 256; // Wrap around for negative results
                    }

                    A = result & 0xFF;

                    updateZeroAndNegativeFlags(A);
                    System.out.println("SBC: Subtracted " + number + " from A. A is now " + A);
                    System.out.println("     Carry flag: " + carryFlag);
                    break;}
                default:
                    System.out.println("Unknown instruction: " + instruction);
                    return;
            }
            System.out.println("     A=" + A + " X=" + X + " Y=" + Y + " PC=" + programCounter);
            System.out.println();
        }
    }

    private void updateZeroAndNegativeFlags(int value) {
        value = value & 255;
        zeroFlag = (value == 0);
        negativeFlag = (value >= 128) ;
    }
    public static void main(String[] args) throws Exception {
        exampleSBC2();
    }

    // Example: Basic operations
    public static void example1() {
        System.out.println("=== EXAMPLE 1: Basic Operations ===\n");
        App cpu = new App();
        
        // Load 10 into A
        // Load 5 into X
        // Add X to A (should be 15)
        // Transfer A to Y
        // Stop
        
        cpu.memory[0] = 1;   // LDA
        cpu.memory[1] = 10;  // Load 10
        cpu.memory[2] = 3;   // LDX
        cpu.memory[3] = 5;   // Load 5
        cpu.memory[4] = 2;   // Add X to A
        cpu.memory[5] = 7;   // TAX (Transfer A to X)
        cpu.memory[6] = 0;   // Stop
        
        cpu.run();
    }
      // Example 2: Memory operations
    public static void example2() {
        System.out.println("=== EXAMPLE 2: Memory Operations ===\n");
        App cpu = new App();
        
        // Load 42 into A
        // Store A to memory location 100
        // Load 0 into A (clear it)
        // Load from memory location 100 back into A
        // Stop
        
        cpu.memory[0] = 1;    // LDA
        cpu.memory[1] = 42;   // Load 42
        cpu.memory[2] = 5;    // STA
        cpu.memory[3] = 100;  // Store to address 100
        cpu.memory[4] = 1;    // LDA
        cpu.memory[5] = 0;    // Load 0
        cpu.memory[6] = 6;    // LDA from memory
        cpu.memory[7] = 100;  // Load from address 100
        cpu.memory[8] = 0;    // Stop
        
        cpu.run();
    }

    // Example 3: Loops with branching!
    public static void example3() {
        System.out.println("=== EXAMPLE 3: A Simple Loop ===\n");
        App cpu = new App();
        
        // This program counts from 0 to 5 in X
        // Load 0 into X
        // [LOOP] Increment X
        // Compare X with 5
        // If not equal, go back to LOOP
        // Stop
        
        // cpu.memory[0] = 3;    // LDX
        // cpu.memory[1] = 0;    // Load 0
        // cpu.memory[2] = 9;    // INX (increment X) ← LOOP position
        // cpu.memory[3] = 12;   // CMP (we'll compare A with X, so first transfer X to A)
        // cpu.memory[4] = 5;    // Actually, let's compare X manually...
        
        // Let me rewrite this more clearly:
        cpu.memory[0] = 3;    // LDX
        cpu.memory[1] = 0;    // Load 0 into X
        // Position 2 = LOOP START
        cpu.memory[2] = 9;    // INX (increment X)
        cpu.memory[3] = 8;    // TXA (transfer X to A so we can compare)
        cpu.memory[4] = 12;   // CMP (compare A with next number)
        cpu.memory[5] = 5;    // Compare with 5
        cpu.memory[6] = 14;   // BNE (branch if not equal)
        cpu.memory[7] = 2;    // Jump back to position 2 (LOOP)
        cpu.memory[8] = 0;    // Stop
        
        cpu.run();
    }
    
    public static void example3Better(){
        System.out.println("=== EXAMPLE 3 (Improved with CPX) ===\n");
        App cpu = new App();

        cpu.memory[0] = 3;  // LDX
        cpu.memory[1] = 0;  // Loading 0 into X
        //Postion 2 = LOOP START
        cpu.memory[2] = 9;     // INX (increment X)
        cpu.memory[3] = 15;    // CPX (compare X directly - no TXA needed!)
        cpu.memory[4] = 5;     // Compare with 5
        cpu.memory[5] = 14;    // BNE (branch if not equal)
        cpu.memory[6] = 2;     // Jump back to position 2
        cpu.memory[7] = 0;     // Stop

        cpu.run();
    }

    // Example 4: Using all registers
    public static void example4() {
        System.out.println("=== EXAMPLE 4: Using All Registers ===\n");
        App cpu = new App();
        
        // Load different values into A, X, Y
        // Then increment them
        
        cpu.memory[0] = 1;    // LDA
        cpu.memory[1] = 10;   // Load 10
        cpu.memory[2] = 3;    // LDX
        cpu.memory[3] = 20;   // Load 20
        cpu.memory[4] = 4;    // LDY
        cpu.memory[5] = 30;   // Load 30
        cpu.memory[6] = 9;    // INX
        cpu.memory[7] = 10;   // INY
        cpu.memory[8] = 0;    // Stop
        
        cpu.run();
    }

    public static void example5() {
        System.out.println("=== EXAMPLE 5: ADC and Carry Flag ===\n");
        App cpu = new App();
        
        // Load 200 into A
        // Add 100 to A (should wrap around to 44 and set carry flag)
        
        cpu.memory[0] = 1;    // LDA
        cpu.memory[1] = 200;  // Load 200
        cpu.memory[2] = 17;   // ADC
        cpu.memory[3] = 100;  // Add 100
        cpu.memory[4] = 0;    // Stop
        
        cpu.run();
    }

    public static void example6() {
        System.out.println("=== EXAMPLE 5: ADC with no carry===\n");
        App cpu = new App();
        
        // Load 200 into A
        // Add 100 to A (should wrap around to 44 and set carry flag)
        
        cpu.memory[0] = 1;    // LDA
        cpu.memory[1] = 200;  // Load 200
        cpu.memory[2] = 17;   // ADC
        cpu.memory[3] = 4;  // Add 4
        cpu.memory[4] = 0;    // Stop
        
        cpu.run();
    }

    public static void example7() {
        App cpu = new App();

        cpu.memory[0] = 19; // SEC
        cpu.memory[1] = 1;  // LDA
        cpu.memory[2] = 50; // Load 50
        cpu.memory[3] = 20; // SBC
        cpu.memory[4] = 20; // Subtract 20
        cpu.memory[5] = 0;  // Stop

        cpu.run();
    }

    public static void exampleSBC2() {
        App cpu = new App();
        
        cpu.memory[0] = 19;   // SEC
        cpu.memory[1] = 1;    // LDA
        cpu.memory[2] = 10;   // Load 10
        cpu.memory[3] = 20;   // SBC
        cpu.memory[4] = 50;   // Subtract 50 (bigger than 10!)
        cpu.memory[5] = 0;    // Stop
        
        cpu.run();
    }
}
