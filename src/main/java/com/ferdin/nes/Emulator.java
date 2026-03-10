package main.java.com.ferdin.nes;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

// import java.nio.file.Files;
// import java.nio.file.Paths;
import main.java.com.ferdin.nes.bus.Bus;
import main.java.com.ferdin.nes.cpu.CPU;
import main.java.com.ferdin.nes.joypad.Joypad;
import main.java.com.ferdin.nes.render.Frame;
import main.java.com.ferdin.nes.rom.Rom;
import main.java.com.ferdin.nes.render.Renderer;


public class Emulator {

    private static final int WIDTH  = 256;
    private static final int HEIGHT = 240;

    private static byte[] loadRomFile(String resourcePath) throws IOException {
        try (InputStream is = Emulator.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new FileNotFoundException("Resource not found: " + resourcePath);
            return is.readAllBytes();
        }
    }

    public static void main(String[] args) throws Exception {

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        long window = GLFW.glfwCreateWindow(WIDTH * 3, HEIGHT * 3, "NES Emulator", 0, 0);
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GL.createCapabilities();
        glEnable(GL_TEXTURE_2D);

        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, WIDTH, HEIGHT, 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        byte[] pixelData = new byte[WIDTH * HEIGHT * 3];
        ByteBuffer glBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT * 3);
        AtomicBoolean frameReady = new AtomicBoolean(false);

        byte[] bytes = loadRomFile("/main/java/resources/roms/pacmanv2.nes");
        Rom rom = new Rom(bytes);
        Frame frame = new Frame();

        // Key map
        Map<Integer, Integer> keyMap = new HashMap<>();
        keyMap.put(GLFW.GLFW_KEY_DOWN,  Joypad.DOWN);
        keyMap.put(GLFW.GLFW_KEY_UP,    Joypad.UP);
        keyMap.put(GLFW.GLFW_KEY_RIGHT, Joypad.RIGHT);
        keyMap.put(GLFW.GLFW_KEY_LEFT,  Joypad.LEFT);
        keyMap.put(GLFW.GLFW_KEY_SPACE, Joypad.SELECT);
        keyMap.put(GLFW.GLFW_KEY_ENTER, Joypad.START);
        keyMap.put(GLFW.GLFW_KEY_A,     Joypad.BUTTON_A);
        keyMap.put(GLFW.GLFW_KEY_S,     Joypad.BUTTON_B);

        keyMap.put(GLFW.GLFW_KEY_W,      Joypad.UP);
        keyMap.put(GLFW.GLFW_KEY_D,      Joypad.RIGHT);
        keyMap.put(GLFW.GLFW_KEY_X,      Joypad.DOWN);
        keyMap.put(GLFW.GLFW_KEY_Z,      Joypad.LEFT);

        // Bus — callback only writes pixels
        Bus bus = new Bus(rom, (ppu, joypad) -> {
            Renderer.render(ppu, frame);
            System.arraycopy(frame.data, 0, pixelData, 0, frame.data.length);
            frameReady.set(true);
        });

        // Key callback — directly updates joypad on the main thread
        GLFW.glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            System.out.println("Key event: key=" + key + " action=" + action);
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                GLFW.glfwSetWindowShouldClose(win, true);
                return;
            }
            Integer button = keyMap.get(key);
            System.out.println("Mapped button: " + button);
            if (button == null) return;
            if (action == GLFW.GLFW_PRESS) {
                bus.getJoypad().setButtonPressed(button, true);
                //System.out.println("Button pressed, status=" + Integer.toBinaryString(bus.getJoypad().getButtonStatus()));
            } else if (action == GLFW.GLFW_RELEASE) {
                bus.getJoypad().setButtonPressed(button, false);
            }
        });

        // CPU on background thread
        CPU cpu = new CPU(bus);
        cpu.reset();

        Thread cpuThread = new Thread(() -> {
            cpu.runWithCallback(emulatorCpu -> {});
        });
        cpuThread.setDaemon(true);
        cpuThread.start();

        // Main thread — GLFW event loop + GL rendering
        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents(); // ← this triggers the key callback

            if (frameReady.get()) {
                frameReady.set(false);

                glBuffer.clear();
                glBuffer.put(pixelData);
                glBuffer.rewind();
                glBindTexture(GL_TEXTURE_2D, texture);
                glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, WIDTH, HEIGHT,
                        GL_RGB, GL_UNSIGNED_BYTE, glBuffer);

                glClear(GL_COLOR_BUFFER_BIT);
                glBegin(GL_QUADS);
                glTexCoord2f(0, 0); glVertex2f(-1,  1);
                glTexCoord2f(1, 0); glVertex2f( 1,  1);
                glTexCoord2f(1, 1); glVertex2f( 1, -1);
                glTexCoord2f(0, 1); glVertex2f(-1, -1);
                glEnd();

                GLFW.glfwSwapBuffers(window);
            }

            //try { Thread.sleep(1); } catch (InterruptedException ignored) {}
        }

        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
        System.exit(0);
    }
}
