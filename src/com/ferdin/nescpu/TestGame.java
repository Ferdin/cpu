package com.ferdin.nescpu;

import org.lwjgl.*;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public class TestGame {

    private static DemoNES nes;
    private static long window;
    private static int textureId;
    private static Random rng = new Random();

    public static void main(String[] args) {
        // 1️⃣ Initialize NES CPU
        nes = new DemoNES();
        int[] game_code = new int[]{
            0x20, 0x06, 0x06, 0x20, 0x38, 0x06, 0x20, 0x0d, 0x06, 0x20, 0x2a, 0x06, 0x60, 0xa9, 0x02, 0x85,
            0x02, 0xa9, 0x04, 0x85, 0x03, 0xa9, 0x11, 0x85, 0x10, 0xa9, 0x10, 0x85, 0x12, 0xa9, 0x0f, 0x85,
            0x14, 0xa9, 0x04, 0x85, 0x11, 0x85, 0x13, 0x85, 0x15, 0x60, 0xa5, 0xfe, 0x85, 0x00, 0xa5, 0xfe,
            0x29, 0x03, 0x18, 0x69, 0x02, 0x85, 0x01, 0x60, 0x20, 0x4d, 0x06, 0x20, 0x8d, 0x06, 0x20, 0xc3,
            0x06, 0x20, 0x19, 0x07, 0x20, 0x20, 0x07, 0x20, 0x2d, 0x07, 0x4c, 0x38, 0x06, 0xa5, 0xff, 0xc9,
            0x77, 0xf0, 0x0d, 0xc9, 0x64, 0xf0, 0x14, 0xc9, 0x73, 0xf0, 0x1b, 0xc9, 0x61, 0xf0, 0x22, 0x60,
            0xa9, 0x04, 0x24, 0x02, 0xd0, 0x26, 0xa9, 0x01, 0x85, 0x02, 0x60, 0xa9, 0x08, 0x24, 0x02, 0xd0,
            0x1b, 0xa9, 0x02, 0x85, 0x02, 0x60, 0xa9, 0x01, 0x24, 0x02, 0xd0, 0x10, 0xa9, 0x04, 0x85, 0x02,
            0x60, 0xa9, 0x02, 0x24, 0x02, 0xd0, 0x05, 0xa9, 0x08, 0x85, 0x02, 0x60, 0x60, 0x20, 0x94, 0x06,
            0x20, 0xa8, 0x06, 0x60, 0xa5, 0x00, 0xc5, 0x10, 0xd0, 0x0d, 0xa5, 0x01, 0xc5, 0x11, 0xd0, 0x07,
            0xe6, 0x03, 0xe6, 0x03, 0x20, 0x2a, 0x06, 0x60, 0xa2, 0x02, 0xb5, 0x10, 0xc5, 0x10, 0xd0, 0x06,
            0xb5, 0x11, 0xc5, 0x11, 0xf0, 0x09, 0xe8, 0xe8, 0xe4, 0x03, 0xf0, 0x06, 0x4c, 0xaa, 0x06, 0x4c,
            0x35, 0x07, 0x60, 0xa6, 0x03, 0xca, 0x8a, 0xb5, 0x10, 0x95, 0x12, 0xca, 0x10, 0xf9, 0xa5, 0x02,
            0x4a, 0xb0, 0x09, 0x4a, 0xb0, 0x19, 0x4a, 0xb0, 0x1f, 0x4a, 0xb0, 0x2f, 0xa5, 0x10, 0x38, 0xe9,
            0x20, 0x85, 0x10, 0x90, 0x01, 0x60, 0xc6, 0x11, 0xa9, 0x01, 0xc5, 0x11, 0xf0, 0x28, 0x60, 0xe6,
            0x10, 0xa9, 0x1f, 0x24, 0x10, 0xf0, 0x1f, 0x60, 0xa5, 0x10, 0x18, 0x69, 0x20, 0x85, 0x10, 0xb0,
            0x01, 0x60, 0xe6, 0x11, 0xa9, 0x06, 0xc5, 0x11, 0xf0, 0x0c, 0x60, 0xc6, 0x10, 0xa5, 0x10, 0x29,
            0x1f, 0xc9, 0x1f, 0xf0, 0x01, 0x60, 0x4c, 0x35, 0x07, 0xa0, 0x00, 0xa5, 0xfe, 0x91, 0x00, 0x60,
            0xa6, 0x03, 0xa9, 0x00, 0x81, 0x10, 0xa2, 0x00, 0xa9, 0x01, 0x81, 0x10, 0x60, 0xa2, 0x00, 0xea,
            0xea, 0xca, 0xd0, 0xfb, 0x60
        };
        nes.load(game_code);
        nes.reset();

        // 2️⃣ Initialize GLFW + OpenGL
        initWindow();

        // 3️⃣ Create texture
        createTexture();

        // 4️⃣ Frame buffer for change detection
        ByteBuffer screenState = BufferUtils.createByteBuffer(32 * 32 * 3);

        // 5️⃣ Main loop
        nes.runWithCallback(cpu -> {
            // Handle user input (WASD, Escape) and window events
            glfwPollEvents();
            handleUserInput();

            // Write random number to 0xFE
            nes.memWrite(0xFE, (byte)(rng.nextInt(15) + 1)); // 1..15

            // Update screen if changed
            if (readScreenState(nes, screenState)) {
                updateTexture(screenState);
            }

            // Render quad
            renderTexture();
            glfwSwapBuffers(window);
            // glfwPollEvents();

            // Small delay (70ms)
            try { Thread.sleep(70); } catch (InterruptedException e) { e.printStackTrace(); }
        });

        glfwTerminate();
    }
    // ------------------------
    // Window + OpenGL setup
    private static void initWindow() {
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        window = glfwCreateWindow(320, 320, "Snake Game", NULL, NULL);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // vsync
        GL.createCapabilities();

        glViewport(0, 0, 320, 320);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, 32, 32, 0, -1, 1); // 32x32 NES screen
        glMatrixMode(GL_MODELVIEW);
    }

    private static void createTexture() {
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, 32, 32, 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer)null);
    }

    // ------------------------
    // User input handling (WASD + Escape)
    private static void handleUserInput() {
        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
            glfwSetWindowShouldClose(window, true);
        }
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) nes.memWrite(0xFF, (byte)0x77);
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) nes.memWrite(0xFF, (byte)0x73);
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) nes.memWrite(0xFF, (byte)0x61);
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) nes.memWrite(0xFF, (byte)0x64);
    }
    // ------------------------
    // Read screen state and return true if anything changed
    private static boolean readScreenState(DemoNES cpu, ByteBuffer frame) {
        boolean update = false;
        int frameIdx = 0;

        for (int addr = 0x0200; addr < 0x600; addr++) {
            int colorIdx = cpu.memRead(addr);
            int[] rgb = color(colorIdx);

            byte r = (byte) rgb[0];
            byte g = (byte) rgb[1];
            byte b = (byte) rgb[2];

            if (frame.get(frameIdx) != r || frame.get(frameIdx + 1) != g || frame.get(frameIdx + 2) != b) {
                frame.put(frameIdx, r);
                frame.put(frameIdx + 1, g);
                frame.put(frameIdx + 2, b);
                update = true;
            }
            frameIdx += 3;
        }

        return update;
    }
    // ------------------------
    // Update GPU texture
    private static void updateTexture(ByteBuffer frame) {
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 32, 32, GL_RGB, GL_UNSIGNED_BYTE, frame);
    }

    // ------------------------
    // Render quad
    private static void renderTexture() {
        glClear(GL_COLOR_BUFFER_BIT);
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textureId);

        glBegin(GL_QUADS);
        glTexCoord2f(0, 0); glVertex2f(0, 0);
        glTexCoord2f(1, 0); glVertex2f(32, 0);
        glTexCoord2f(1, 1); glVertex2f(32, 32);
        glTexCoord2f(0, 1); glVertex2f(0, 32);
        glEnd();
    }

    // ------------------------
    // Palette lookup
    private static int[] color(int value) {
        switch (value) {
            case 0: return new int[]{0, 0, 0};
            case 1: return new int[]{255, 255, 255};
            case 2: case 9: return new int[]{128, 128, 128};
            case 3: case 10: return new int[]{255, 0, 0};
            case 4: case 11: return new int[]{0, 255, 0};
            case 5: case 12: return new int[]{0, 0, 255};
            case 6: case 13: return new int[]{255, 0, 255};
            case 7: case 14: return new int[]{255, 255, 0};
            default: return new int[]{0, 255, 255};
        }
    }

}