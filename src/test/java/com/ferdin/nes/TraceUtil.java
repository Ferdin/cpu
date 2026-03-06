package test.java.com.ferdin.nes;

import main.java.com.ferdin.nes.cpu.*;

public class TraceUtil {

    public static String trace(CPU cpu) {

        int pc = cpu.getProgramCounter();
        int opcode = cpu.memRead(pc) & 0xFF;

        Instruction inst = CPU.OPCODES[opcode];

        if (inst == null) {
            throw new IllegalStateException(
                String.format("Undefined opcode: 0x%02X at PC: 0x%04X", opcode, pc)
            );
        }

        int a = cpu.getRegisterA();
        int x = cpu.getRegisterX();
        int y = cpu.getRegisterY();
        int sp = cpu.getStackPointer();
        int p = cpu.getStatus();

        StringBuilder bytes = new StringBuilder();
        String operandText = "";

        String mnemonicStr = inst.illegal ? "*" + inst.mnemonic : inst.mnemonic;

        switch (inst.mode) {

            case IMMEDIATE -> {
                int value = cpu.memRead(pc + 1) & 0xFF;
                bytes.append(String.format("%02X %02X", opcode, value));
                operandText = String.format("%s #$%02X", mnemonicStr, value);
            }

            case IMPLIED -> {
                bytes.append(String.format("%02X", opcode));
                operandText = mnemonicStr;
            }

            case ZERO_PAGE -> {
                int addr = cpu.memRead(pc + 1) & 0xFF;
                int value = cpu.memRead(addr) & 0xFF;          // add this
                bytes.append(String.format("%02X %02X", opcode, addr));
                operandText = String.format("%s $%02X = %02X", mnemonicStr, addr, value);  // add = %02X
            }

            case ZERO_PAGE_X -> {
                int base = cpu.memRead(pc + 1) & 0xFF;
                int addr = (base + x) & 0xFF;
                int value = cpu.memRead(addr) & 0xFF;

                bytes.append(String.format("%02X %02X", opcode, base));
                operandText = String.format("%s $%02X,X @ %02X = %02X",
                        mnemonicStr, base, addr, value);
            }
            case ZERO_PAGE_Y -> {
                int base = cpu.memRead(pc + 1) & 0xFF;
                int addr = (base + y) & 0xFF;
                int value = cpu.memRead(addr) & 0xFF;
                bytes.append(String.format("%02X %02X", opcode, base));
                operandText = String.format("%s $%02X,Y @ %02X = %02X",
                        mnemonicStr, base, addr, value);
            }
            case ABSOLUTE -> {
                int lo = cpu.memRead(pc + 1) & 0xFF;
                int hi = cpu.memRead(pc + 2) & 0xFF;
                int addr = (hi << 8) | lo;

                bytes.append(String.format("%02X %02X %02X", opcode, lo, hi));
                operandText = mnemonicStr + " $" + String.format("%04X", addr);

                // Only show value for load/store/etc instructions
                if (inst.showsMemoryValue()) {
                    int value = cpu.memRead(addr) & 0xFF;
                    operandText += " = " + String.format("%02X", value);
                }
            }

            case ABSOLUTE_X -> {
                int lo = cpu.memRead(pc + 1) & 0xFF;
                int hi = cpu.memRead(pc + 2) & 0xFF;

                int base = (hi << 8) | lo;
                int addr = (base + x) & 0xFFFF;  // mask to 16 bits here
                int value = cpu.memRead(addr) & 0xFF;

                bytes.append(String.format("%02X %02X %02X", opcode, lo, hi));
                operandText = String.format("%s $%04X,X @ %04X = %02X",
                        mnemonicStr, base, addr, value);
            }

            case ABSOLUTE_Y -> {
                int lo = cpu.memRead(pc + 1) & 0xFF;
                int hi = cpu.memRead(pc + 2) & 0xFF;

                int base = (hi << 8) | lo;
                int addr = (base + y) & 0xFFFF;  // mask to 16 bits here
                int value = cpu.memRead(addr) & 0xFF;

                bytes.append(String.format("%02X %02X %02X", opcode, lo, hi));
                operandText = String.format("%s $%04X,Y @ %04X = %02X",
                        mnemonicStr, base, addr, value);
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
                        mnemonicStr, zp, ptr, addr, value);
            }

            case INDIRECT_Y -> {
                int zp = cpu.memRead(pc + 1) & 0xFF;
                int lo = cpu.memRead(zp) & 0xFF;
                int hi = cpu.memRead((zp + 1) & 0xFF) & 0xFF;
                int base = (hi << 8) | lo;
                int addr = (base + y) & 0xFFFF;  // mask to 16 bits here
                int value = cpu.memRead(addr) & 0xFF;
                bytes.append(String.format("%02X %02X", opcode, zp));
                operandText = String.format("%s ($%02X),Y = %04X @ %04X = %02X",
                        mnemonicStr, zp, base, addr, value);
            }

            case RELATIVE -> {
                int offset = cpu.memRead(pc + 1) & 0xFF;
                bytes.append(String.format("%02X %02X", opcode, offset));
                // Convert to signed and calculate target address
                int signedOffset = (offset < 0x80) ? offset : offset - 0x100;
                int target = (pc + 2 + signedOffset) & 0xFFFF;
                operandText = String.format("%s $%04X", mnemonicStr, target);
            }

            case ACCUMULATOR -> {
                bytes.append(String.format("%02X", opcode));
                operandText = mnemonicStr + " A";
            }

            case INDIRECT -> {
                int lo = cpu.memRead(pc + 1) & 0xFF;
                int hi = cpu.memRead(pc + 2) & 0xFF;
                int ptr = (hi << 8) | lo;
                // Replicate the 6502 page boundary bug
                int targetLo = cpu.memRead(ptr) & 0xFF;
                int targetHi = cpu.memRead((ptr & 0xFF00) | ((ptr + 1) & 0xFF)) & 0xFF;
                int addr = (targetHi << 8) | targetLo;
                bytes.append(String.format("%02X %02X %02X", opcode, lo, hi));
                operandText = String.format("%s ($%04X) = %04X", mnemonicStr, ptr, addr);
            }

            default -> {
                bytes.append(String.format("%02X", opcode));
                operandText = mnemonicStr;
            }
        }

        return String.format(
                inst.illegal ? "%04X  %-8s %-32s A:%02X X:%02X Y:%02X P:%02X SP:%02X" : "%04X  %-9s %-31s A:%02X X:%02X Y:%02X P:%02X SP:%02X",
                pc & 0xFFFF,
                bytes.toString(),
                operandText,
                a, x, y, p, sp
        );
    }
}