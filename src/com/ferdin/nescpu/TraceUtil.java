package com.ferdin.nescpu;

public class TraceUtil {

    // Generate a string trace of the current CPU state
    public static String trace(DemoNES cpu) {
        StringBuilder sb = new StringBuilder();

        int pc = cpu.getProgramCounter();
        int a = cpu.getRegisterA();
        int x = cpu.getRegisterX();
        int y = cpu.getRegisterY();
        int sp = cpu.getStackPointer();
        int p = cpu.getStatus();

        // You can include opcode bytes too if you want
        int opcode = cpu.memRead(pc) & 0xFF;
        int operand = cpu.memRead(pc + 1) & 0xFF;

        // Format like Rust trace example
        sb.append(String.format("%04X  %02X %02X     ", pc, opcode, operand));

        // Add instruction mnemonic (you need a helper method for this)
        //sb.append(Disassembler.disassemble(cpu, pc));

        // Add registers
        sb.append(String.format("  A:%02X X:%02X Y:%02X P:%02X SP:%02X",
                a, x, y, p, sp));

        return sb.toString();
    }
}