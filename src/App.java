public class App {

    int A = 0;
    int X = 0;
    int Y = 0;

    int[] memory = new int[256];

    int programCounter = 0;

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
                System.out.println("Loaded " + number + " into A.");
            }
            else if(instruction == 2) {
                A = A + X;
                System.out.println("Added X to A. A is now " + A);
            }
            else if(instruction == 3) {
                int number = memory[programCounter];
                programCounter++;
                X = number;
                System.out.println("Loaded " + number + " into A.");
            }
        }
        System.out.println("Final result in A: " + A);
    }
    public static void main(String[] args) throws Exception {
        App cpu = new App();
        cpu.memory[0] = 1;  // Instruction: Load into A (One is an instruction Code)
        cpu.memory[1] = 10; // The number to load
        cpu.memory[2] = 3;  // Instruction: Load into X (Three is an instruction Code)
        cpu.memory[3] = 5;  // The number to load
        cpu.memory[4] = 2;  // Instruction: Add X to A (Two is an instruction Code)
        cpu.memory[5] = 0;  // Instruction: Stop (Zero is an instruction Code)
        cpu.run();
    }
}
