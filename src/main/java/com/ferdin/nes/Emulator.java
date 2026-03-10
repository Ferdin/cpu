package main.java.com.ferdin.nes;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
// import java.nio.file.Files;
// import java.nio.file.Paths;
import main.java.com.ferdin.nes.bus.Bus;
import main.java.com.ferdin.nes.cpu.CPU;
import main.java.com.ferdin.nes.render.Frame;
import main.java.com.ferdin.nes.rom.Rom;
import main.java.com.ferdin.nes.render.Renderer;

public class Emulator {

    private static final int WIDTH = 256;
    private static final int HEIGHT = 240;

    private static byte[] loadRomFile(String resourcePath) throws IOException {
        try (InputStream is = Emulator.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }
            return is.readAllBytes();
        }
    }

    public static void main(String[] args) throws Exception {

        // -----------------------------
        // Initialize GLFW
        // -----------------------------
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        long window = GLFW.glfwCreateWindow(WIDTH * 3, HEIGHT * 3, "NES Emulator", 0, 0);
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GL.createCapabilities();
        glEnable(GL_TEXTURE_2D); // ← critical, was missing before

        // -----------------------------
        // Create texture
        // -----------------------------
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, WIDTH, HEIGHT, 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        // -----------------------------
        // Shared frame buffer between CPU thread and main thread
        // -----------------------------
        ByteBuffer sharedPixels = ByteBuffer.allocateDirect(WIDTH * HEIGHT * 3);
        final boolean[] frameReady = { false }; // array trick to use in lambda

        // -----------------------------
        // Load ROM
        // -----------------------------
        byte[] bytes = loadRomFile("/main/java/resources/roms/pacmanv2.nes");
        Rom rom = new Rom(bytes);
        Frame frame = new Frame();

        // -----------------------------
        // Bus callback — CPU thread only writes pixel data, no GL calls
        // -----------------------------
        Bus bus = new Bus(rom, (ppu, joypad) -> {
            Renderer.render(ppu, frame);

            sharedPixels.clear();
            sharedPixels.put(frame.data);  // frame.data is byte[]
            sharedPixels.flip();

            frameReady[0] = true;
        });

        // -----------------------------
        // CPU runs on background thread
        // -----------------------------
        CPU cpu = new CPU(bus);
        cpu.reset();

        Thread cpuThread = new Thread(() -> {
            cpu.runWithCallback(emulatorCpu -> {
                // empty — rendering is driven by NMI via bus.tick()
            });
        });
        cpuThread.setDaemon(true);
        cpuThread.start();

        // -----------------------------
        // Main thread: GLFW event loop + all GL calls
        // -----------------------------
        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents();

            if (frameReady[0]) {
                frameReady[0] = false;

                sharedPixels.rewind(); // reset position before GL reads it

                glBindTexture(GL_TEXTURE_2D, texture);
                glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, WIDTH, HEIGHT,
                                GL_RGB, GL_UNSIGNED_BYTE, sharedPixels);

                glClear(GL_COLOR_BUFFER_BIT);
                glBegin(GL_QUADS);
                glTexCoord2f(0, 0); glVertex2f(-1,  1);
                glTexCoord2f(1, 0); glVertex2f( 1,  1);
                glTexCoord2f(1, 1); glVertex2f( 1, -1);
                glTexCoord2f(0, 1); glVertex2f(-1, -1);
                glEnd();

                GLFW.glfwSwapBuffers(window);
            }
            try { Thread.sleep(1); } catch (InterruptedException ignored) {}
        }

        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
        System.exit(0);
    }
}
