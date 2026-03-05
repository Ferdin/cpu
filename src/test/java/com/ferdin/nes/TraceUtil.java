package test.java.com.ferdin.nes;

import main.java.com.ferdin.nes.cpu.*;

public class TraceUtil {

    public static String trace(CPU cpu) {

        int pc = cpu.getProgramCounter();
        int opcode = cpu.memRead(pc) & 0xFF;

        Instruction inst = CPU.OPCODES[opcode];

        int a = cpu.getRegisterA();
        int x = cpu.getRegisterX();
        int y = cpu.getRegisterY();
        int sp = cpu.getStackPointer();
        int p = cpu.getStatus();

        StringBuilder bytes = new StringBuilder();
        String operandText = "";

        switch (inst.mode) {

            case IMMEDIATE -> {
                int value = cpu.memRead(pc + 1) & 0xFF;
                bytes.append(String.format("%02X %02X", opcode, value));
                operandText = String.format("%s #$%02X", inst.mnemonic, value);
            }

            case IMPLIED -> {
                bytes.append(String.format("%02X", opcode));
                operandText = inst.mnemonic;
            }

            case ZERO_PAGE -> {
                int addr = cpu.memRead(pc + 1) & 0xFF;
                bytes.append(String.format("%02X %02X", opcode, addr));
                operandText = String.format("%s $%02X", inst.mnemonic, addr);
            }

            case ZERO_PAGE_X -> {
                int base = cpu.memRead(pc + 1) & 0xFF;
                int addr = (base + x) & 0xFF;
                int value = cpu.memRead(addr) & 0xFF;

                bytes.append(String.format("%02X %02X", opcode, base));
                operandText = String.format("%s $%02X,X @ %02X = %02X",
                        inst.mnemonic, base, addr, value);
            }

            case ABSOLUTE -> {
                int lo = cpu.memRead(pc + 1) & 0xFF;
                int hi = cpu.memRead(pc + 2) & 0xFF;
                int addr = (hi << 8) | lo;
                int value = cpu.memRead(addr) & 0xFF;

                bytes.append(String.format("%02X %02X %02X", opcode, lo, hi));
                operandText = String.format("%s $%04X = %02X",
                        inst.mnemonic, addr, value);
            }

            case ABSOLUTE_X -> {
                int lo = cpu.memRead(pc + 1) & 0xFF;
                int hi = cpu.memRead(pc + 2) & 0xFF;

                int base = (hi << 8) | lo;
                int addr = base + x;
                int value = cpu.memRead(addr) & 0xFF;

                bytes.append(String.format("%02X %02X %02X", opcode, lo, hi));
                operandText = String.format("%s $%04X,X @ %04X = %02X",
                        inst.mnemonic, base, addr, value);
            }

            case ABSOLUTE_Y -> {
                int lo = cpu.memRead(pc + 1) & 0xFF;
                int hi = cpu.memRead(pc + 2) & 0xFF;

                int base = (hi << 8) | lo;
                int addr = base + y;
                int value = cpu.memRead(addr) & 0xFF;

                bytes.append(String.format("%02X %02X %02X", opcode, lo, hi));
                operandText = String.format("%s $%04X,Y @ %04X = %02X",
                        inst.mnemonic, base, addr, value);
            }

            case INDIRECT_X -> {
                int zp = cpu.memRead(pc + 1) & 0xFF;
                int ptr = (zp + x) & 0xFF;

                int lo = cpu.memRead(ptr) & 0xFF;
                int hi = cpu.memRead((ptr + 1) & 0xFF) & 0xFF;

                int addr = (hi << 8) | lo;
                int value = cpu.memRead(addr) & 0xFF;

                bytes.append(String.format("%02X %02X", opcode, zp));
                operandText = String.format("%s ($%02X,X) @ %02X = %04X = %02X",
                        inst.mnemonic, zp, ptr, addr, value);
            }

            case INDIRECT_Y -> {
                int zp = cpu.memRead(pc + 1) & 0xFF;

                int lo = cpu.memRead(zp) & 0xFF;
                int hi = cpu.memRead((zp + 1) & 0xFF) & 0xFF;

                int base = (hi << 8) | lo;
                int addr = base + y;
                int value = cpu.memRead(addr) & 0xFF;

                bytes.append(String.format("%02X %02X", opcode, zp));
                operandText = String.format("%s ($%02X),Y = %04X @ %04X = %02X",
                        inst.mnemonic, zp, base, addr, value);
            }

            default -> {
                bytes.append(String.format("%02X", opcode));
                operandText = inst.mnemonic;
            }
        }

        return String.format(
                "%04X  %-9s %-31s A:%02X X:%02X Y:%02X P:%02X SP:%02X",
                pc,
                bytes.toString(),
                operandText,
                a, x, y, p, sp
        );
    }
}