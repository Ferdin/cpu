public class App {

    int A = 0;
    int X = 0;
    int Y = 0;

    int[] memory = new int[256];

    int programCounter = 0;

    boolean zeroFlag = false;
    boolean negativeFlag = false;

    public void run(){
        while(true){
            int instruction = memory[programCounter];
            programCounter++;

            if(instruction == 0) {
                System.out.println("Program finished!");
                break;
            }
            else if(instruction == 1){
                int number = memory[programCounter];
                programCounter++;
                A = number;
                updateFlags(A);
                System.out.println("LDA: Loaded " + number + " into A.");
            }
            else if(instruction == 2) {
                A = A + X;
                if(A > 255) A = A - 256;
                updateFlags(A);
                System.out.println("ADC: Added X to A. A is now " + A);
            }
            else if(instruction == 3) {
                int number = memory[programCounter];
                programCounter++;
                X = number;
                updateFlags(X);
                System.out.println("LDX: Loaded " + number + " into X.");
            }
            else if(instruction == 4) {
                int number = memory[programCounter];
                programCounter++;
                Y = number;
                updateFlags(Y);
                System.out.println("LDY: Loaded " + number + " into Y.");
            }
            else if(instruction == 5) {
                int address = memory[programCounter];
                programCounter++;
                memory[address] = A;
                System.out.println("STA: Stored A (" + A + ") to memory[" + address + "]");
               
            }
            else if(instruction == 6) {
                int address = memory[programCounter];
                programCounter++;
                A = memory[address];
                updateFlags(A);
                System.out.println("LDA (from memory): Loaded \" + A + \" from memory[\" + address + \"]");
            }
            else if(instruction == 7) {
                X = A;
                updateFlags(X);
                System.out.println("TAX: Transferred A to X. X is now " + X);
            }
            else if(instruction == 8) {
                A = X;
                updateFlags(A);
                System.out.println("TXA: Transferred X to A. A is now " + A);
            }
            else if(instruction == 9) {
                X = X + 1;
                if (X > 255) X = 0; // Wrap around
                updateFlags(X);
                System.out.println("INX: Incremented X. X is now " + X);
            }
            else if(instruction == 10) {
                Y = Y + 1;
                if (Y > 255) Y = 0; // Wrap around
                updateFlags(Y);
                System.out.println("INY: Incremented Y. Y is now " + X);
            }
            else if(instruction == 11) {
                X = X - 1;
                if( X < 0) X = 255;
                updateFlags(X);
                System.out.println("DEX: Decremented X. X is now" + Y);
            }
            else if(instruction == 12)  {
                int number = memory[programCounter];
                programCounter++; // Move past the data
                int result = A - number;
                updateFlags(result);
                System.out.println("CMP: Compared A (" + A + ") with " + number);
                System.out.println("     Zero flag: " + zeroFlag + ", Negative flag: " + negativeFlag);
            }
            else if (instruction == 13) {
                // 13 = Branch if Zero flag is set
                int offset = memory[programCounter];
                programCounter++; // Move past the offset
                if (zeroFlag) {
                    programCounter = offset;
                    System.out.println("BEQ: Zero flag is set! Jumping to position " + offset);
                } else {
                    System.out.println("BEQ: Zero flag is not set. Continue normally.");
                }
                
            } 
            else if (instruction == 14) {
                // 14 = Branch if Zero flag is NOT set
                int offset = memory[programCounter];
                programCounter++; // Move past the offset
                if (!zeroFlag) {
                    programCounter = offset;
                    System.out.println("BNE: Zero flag is not set! Jumping to position " + offset);
                } else {
                    System.out.println("BNE: Zero flag is set. Continue normally.");
                }
                
            }
            else if (instruction == 15) {
                // 15 = CPX - Compare X with a number
                int number = memory[programCounter];
                programCounter++;
                int result = X - number;
                updateFlags(result);
                System.out.println("CPX: Compared X(" + X + ") with " + number);
                System.out.println("Zero flag: " + zeroFlag + ", Negative flag: " + negativeFlag);
            } 
            else if (instruction == 16){
                // 16 = CPY - Compare Y with a number
                int number = memory[programCounter];
                programCounter++;
                int result = Y - number;
                updateFlags(result);
                System.out.println("CPY: Compared Y (" + Y + ") with " + number);
                System.out.println("     Zero flag: " + zeroFlag + ", Negative flag: " + negativeFlag);

            }
            else {
                System.out.println("Unknown instruction: " + instruction);
                break;
            }
            System.out.println("     A=" + A + " X=" + X + " Y=" + Y + " PC=" + programCounter);
            System.out.println();
        }
        System.out.println("=== FINAL STATE ===");
        System.out.println("A: " + A);
        System.out.println("X: " + X);
        System.out.println("Y: " + Y);
    }

    private void updateFlags(int value) {
        value = value & 255;
        zeroFlag = (value == 0);
        negativeFlag = (value >= 128);
    }
    public static void main(String[] args) throws Exception {
        example3Better();
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
}
