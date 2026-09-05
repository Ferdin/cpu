package test.java.com.ferdin.nes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import main.java.com.ferdin.nes.testing.BlargPpuRunner;

public class BlargPpuTest {
    @TestFactory
    Stream<DynamicTest> runBlargPpuSuite() throws Exception {
        String configuredDirectory = System.getProperty("blarg.ppu.dir");
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            configuredDirectory = System.getenv("BLARG_PPU_DIR");
        }
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            Assumptions.assumeTrue(false,
                "Set -Dblarg.ppu.dir=<directory> or BLARG_PPU_DIR before running this test");
        }

        Path directory = Path.of(configuredDirectory);
        if (!Files.isDirectory(directory)) {
            Assumptions.assumeTrue(false, "Directory does not exist: " + directory);
        }

        return BlargPpuRunner.findRoms(directory).stream()
                .map(rom -> DynamicTest.dynamicTest(rom.getFileName().toString(), () -> {
                    BlargPpuRunner.Result result = BlargPpuRunner.run(rom);
                    assertTrue(result.passed,
                            result.message + " (status=" + result.status + ", cycles=" + result.cycles + ")");
                }));
    }
}