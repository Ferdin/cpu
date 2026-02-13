package com.ferdin.nescpu;

public class DemoNES {
    // I created this class to learn NESCPU from bugzmanov/nes_ebook
//https://github.com/bugzmanov/nes_ebook/blob/master/src/chapter_3_2.md
        // CPU Registers (8-bit)
        public int registerA;
        public int registerX;
        public int registerY;
        public int status;

        // Program Counter (16-bit)
        public int programCounter;

        // 64KB Memory
        private byte[] memory;

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

        public void lda(int value){
            registerA = value;
            update_zero_and_negative_flags(registerA);
        }

        public void tax(){
            registerX = registerA;
            update_zero_and_negative_flags(registerX);
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

        public void run(){
            
            while(true){
                // Read opcode (convert signed byte to unsigned)
                int opcode = memRead(programCounter) & 0xFF;

                // Increment program counter
                programCounter++;

                switch(opcode){
                    case 0xA9:{
                        //LDA
                        // Read parameter (convert to unsigned)
                        int param = memRead(programCounter) & 0xFF;
                        programCounter++;

                        lda(param);
                        break;
                    }
                    case 0xAA: {
                        // TAX - Transfer A to X
                        //Copy A into X
                        tax();
                        break;
                    }
                    case 0xE8: {
                        // INX - Increment X
                        registerX = (registerX + 1) & 0xFF;
                        update_zero_and_negative_flags(registerX);
                        break;
                    }
                    case 0x00:
                        // BRK - Break (for this demo, we'll just stop execution)
                        return;    
                    default:
                        throw new UnsupportedOperationException("Opcode not implemented yet.");
                }
            }
            
            //
        }
    public static void main(String[] args){
        
    }
}
