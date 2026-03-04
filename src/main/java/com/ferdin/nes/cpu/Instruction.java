package main.java.com.ferdin.nes.cpu;
import java.util.function.BiConsumer;

public class Instruction {
    // Single data structure to hold all the information about an instruction.
    public final String mnemonic;
    public final AddressingMode mode;
    public final BiConsumer<CPU, AddressingMode> execute;

    public Instruction(String mnemonic, AddressingMode mode, BiConsumer<CPU, AddressingMode> execute) {
        this.mnemonic = mnemonic;
        this.mode = mode;
        this.execute = execute;
    }
}
