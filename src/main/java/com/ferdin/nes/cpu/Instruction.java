package main.java.com.ferdin.nes.cpu;
import java.util.function.BiConsumer;

public class Instruction {
    // Single data structure to hold all the information about an instruction.
    public final String mnemonic;
    public final AddressingMode mode;
    public final BiConsumer<CPU, AddressingMode> execute;
    public final int cycles;
    public final boolean illegal;

    // For official opcodes, the execute function will be non-null and illegal will be false.
    public Instruction(String mnemonic, int cycles, AddressingMode mode, BiConsumer<CPU, AddressingMode> execute) {
        this.mnemonic = mnemonic;
        this.cycles = cycles;
        this.mode = mode;
        this.execute = execute;
        this.illegal = false;
    }
    // For illegal opcodes, the execute function will be null and illegal will be true.
    public Instruction(String mnemonic, int cycles, AddressingMode mode, BiConsumer<CPU, AddressingMode> execute, boolean illegal) {
        this.mnemonic = mnemonic;
        this.cycles = cycles;
        this.mode = mode;
        this.execute = execute;
        this.illegal = illegal;
    }

    public boolean showsMemoryValue() {
        // switch(this.mnemonic) {
        //     case "LDA": case "LDX": case "LDY":
        //     case "STA": case "STX": case "STY":
        //     case "ADC": case "SBC": case "AND":
        //     case "ORA": case "EOR": case "CMP":
        //     case "CPX": case "CPY":
        //     case "BIT": return true;
        //     default: return false;
        // }
        switch(this.mnemonic) {
            case "LDA": case "LDX": case "LDY":
            case "STA": case "STX": case "STY":
            case "ADC": case "SBC": case "AND":
            case "ORA": case "EOR": case "CMP":
            case "CPX": case "CPY": case "LSR":
            case "ASL": case "ROL": case "ROR":
            case "DEC": case "INC": case "NOP":
            case "LAX": case "SAX": case "DCP":
            case "ISB": case "SLO": case "RLA":          
            case "BIT": case "SRE": case "RRA":
            return true;
            default: return false;
        }
    }
}
