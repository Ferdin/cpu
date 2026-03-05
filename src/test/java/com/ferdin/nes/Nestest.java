package test.java.com.ferdin.nes;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import main.java.com.ferdin.nes.cpu.CPU;
import main.java.com.ferdin.nes.bus.Bus;
import main.java.com.ferdin.nes.rom.Rom;;


public class Nestest {
    @Test
    void test_nestest_rom() throws Exception {

        byte[] rawRom = Files.readAllBytes(Paths.get("C:\\Users\\fnorbert\\Documents\\Java\\CPU\\src\\main\\java\\resources\\roms\\nestest.nes"));
        Rom rom = new Rom(rawRom);

        Bus bus = new Bus(rom);
        CPU cpu = new CPU(bus);

        cpu.reset();
        cpu.setProgramCounter(0xC000);

        List<String> reference = Files.readAllLines(Paths.get("C:\\Users\\fnorbert\\Documents\\Java\\CPU\\src\\main\\java\\resources\\roms\\nestest.log"));

        for (int i = 0; i < reference.size(); i++) {

            String expected = reference.get(i);
            String actual = TraceUtil.trace(cpu);

            assertEquals(expected.substring(0, 73), actual.substring(0, 73),
                    "Mismatch at line " + i);

            cpu.step();
        }
    }
}
