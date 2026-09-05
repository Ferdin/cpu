package main.java.com.ferdin.nes.testing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import main.java.com.ferdin.nes.bus.Bus;
import main.java.com.ferdin.nes.cpu.CPU;
import main.java.com.ferdin.nes.rom.Rom;

public final class BlargPpuRunner {
    private static final int RESULT = 0x6000;
    private static final int LEGACY_RESULT = 0x00F0;
    private static final int RUNNING = 0x80;
    private static final int MESSAGE = 0x6004;
    private static final long DEFAULT_MAX_CYCLES = 100_000_000L;

    private BlargPpuRunner() {
    }

    public static List<Path> findRoms(Path directory) throws IOException {
        try (var paths = Files.list(directory)) {
            return paths
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".nes"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    public static Result run(Path romPath) throws Exception {
        return run(romPath, DEFAULT_MAX_CYCLES);
    }

    public static Result run(Path romPath, long maxCycles) throws Exception {
        Rom rom = new Rom(Files.readAllBytes(romPath));
        Bus bus = new Bus(rom, (ppu, joypad) -> { });
        CPU cpu = new CPU(bus);
        boolean started = false;
        int legacyStatus = 0;
        long cycles = 0;

        while (cycles < maxCycles) {
            if (bus.pollNmiStatus() != null) {
                cpu.interruptNMI();
            }

            int cyclesUsed = cpu.step();
            bus.tick(cyclesUsed);
            cycles += cyclesUsed;

            int status = bus.memRead(RESULT);
            if (status == RUNNING) {
                started = true;
            } else if (started) {
                return new Result(romPath, status == 0 || status == 1, status, cycles, readMessage(bus));
            } else if (status != 0) {
                return new Result(romPath, status == 1, status, cycles, readMessage(bus));
            }

            int currentLegacyStatus = bus.memRead(LEGACY_RESULT);
            if (!started && currentLegacyStatus != 0) {
                legacyStatus = currentLegacyStatus;
                if (legacyStatus == 1) {
                    return new Result(romPath, true, legacyStatus, cycles,
                            "legacy PPU result at $00F0");
                }
            }
        }

        int status = started ? bus.memRead(RESULT) : legacyStatus;
        return new Result(romPath, false, status, cycles,
            "timed out after " + maxCycles + " CPU cycles"
                + " (pc=$" + String.format("%04X", cpu.getProgramCounter())
                + ", legacy=$" + String.format("%02X", legacyStatus)
                + ", scanline=" + bus.getPpu().scanline
                + ", ppuStatus=$" + String.format("%02X", bus.getPpu().status.snapshot()) + ")");
    }

    private static String readMessage(Bus bus) {
        byte[] message = new byte[256];
        int length = 0;
        for (; length < message.length; length++) {
            int value = bus.memRead(MESSAGE + length);
            if (value == 0) {
                break;
            }
            message[length] = (byte) value;
        }
        return new String(message, 0, length, StandardCharsets.US_ASCII).trim();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: BlargPpuRunner <directory>");
        }

        List<Result> results = new ArrayList<>();
        for (Path rom : findRoms(Path.of(args[0]))) {
            Result result = run(rom);
            results.add(result);
            System.out.printf("%s: %s%s%n", rom.getFileName(),
                    result.passed ? "PASS" : "FAIL", result.message.isEmpty() ? "" : " - " + result.message);
        }

        if (results.isEmpty() || results.stream().anyMatch(result -> !result.passed)) {
            throw new AssertionError("One or more Blargg PPU ROMs failed");
        }
    }

    public static final class Result {
        public final Path romPath;
        public final boolean passed;
        public final int status;
        public final long cycles;
        public final String message;

        private Result(Path romPath, boolean passed, int status, long cycles, String message) {
            this.romPath = romPath;
            this.passed = passed;
            this.status = status;
            this.cycles = cycles;
            this.message = message;
        }
    }
}