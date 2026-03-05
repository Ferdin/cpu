package test.java.com.ferdin.nes;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import main.java.com.ferdin.nes.cpu.*;
import main.java.com.ferdin.nes.bus.Bus;

class TraceTest {

    @Test
    void test_format_trace() {

        Bus bus = new Bus(); // test-mode bus (no ROM)

        bus.memWrite(100, 0xA2);
        bus.memWrite(101, 0x01);
        bus.memWrite(102, 0xCA);
        bus.memWrite(103, 0x88);
        bus.memWrite(104, 0x00);

        CPU cpu = new CPU(bus);

        cpu.setProgramCounter(0x64);
        cpu.setRegisterA(1);
        cpu.setRegisterX(2);
        cpu.setRegisterY(3);
        // cpu.setStatus(0x24); 

        List<String> result = new ArrayList<>();

        cpu.runWithCallback(c -> {
            result.add(TraceUtil.trace(c));
        });

        assertEquals(
            "0064  A2 01     LDX #$01                        A:01 X:02 Y:03 P:24 SP:FD",
            result.get(0)
        );

        assertEquals(
            "0066  CA        DEX                             A:01 X:01 Y:03 P:24 SP:FD",
            result.get(1)
        );

        assertEquals(
            "0067  88        DEY                             A:01 X:00 Y:03 P:26 SP:FD",
            result.get(2)
        );
    }

    @Test
    void test_format_mem_access() {

        Bus bus = new Bus();

        // ORA ($33), Y
        bus.memWrite(100, 0x11);
        bus.memWrite(101, 0x33);

        // Pointer data
        bus.memWrite(0x33, 0x00);
        bus.memWrite(0x34, 0x04);

        // Target address 0x0400
        bus.memWrite(0x400, 0xAA);

        CPU cpu = new CPU(bus);

        cpu.setProgramCounter(0x64);
        cpu.setRegisterY(0);

        List<String> result = new ArrayList<>();

        cpu.runWithCallback(c -> {
            result.add(TraceUtil.trace(c));
        });

        assertEquals(
            "0064  11 33     ORA ($33),Y = 0400 @ 0400 = AA  A:00 X:00 Y:00 P:24 SP:FD",
            result.get(0)
        );
    }
}