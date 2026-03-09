package test.java.com.ferdin.nes;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import main.java.com.ferdin.nes.render.Frame;
import main.java.com.ferdin.nes.render.Palette;
import main.java.com.ferdin.nes.rom.Rom;

public class TileViewer {
    private static final int SCALE = 3;
    private static final int WIN_W = Frame.WIDTH  * SCALE;
    private static final int WIN_H = Frame.HEIGHT * SCALE;

    // ── Helper ───────────────────────────────────────────────────────────

    private static int[] paletteColor(int value) {
        return switch (value) {
            case 0  -> Palette.SYSTEM_PALETTE[0x01];
            case 1  -> Palette.SYSTEM_PALETTE[0x23];
            case 2  -> Palette.SYSTEM_PALETTE[0x27];
            case 3  -> Palette.SYSTEM_PALETTE[0x30];
            default -> throw new IllegalStateException("can't be");
        };
    }
    
    // ── show_tile ────────────────────────────────────────────────────────
    public static Frame showTile(byte[] chrRom, int bank, int tileN) {
        if (bank > 1) throw new IllegalArgumentException("bank must be 0 or 1");
        Frame frame = new Frame();
        int bankOffset = bank * 0x1000;

        byte[] tile = new byte[16];
        System.arraycopy(chrRom, bankOffset + tileN * 16, tile, 0, 16);

        for (int y = 0; y <= 7; y++) {
            int upper = Byte.toUnsignedInt(tile[y]);
            int lower = Byte.toUnsignedInt(tile[y + 8]);
            for (int x = 7; x >= 0; x--) {
                int value = ((1 & upper) << 1) | (1 & lower);
                upper >>= 1;
                lower >>= 1;
                int[] color = paletteColor(value);
                frame.setPixel(x, y, color[0], color[1], color[2]);
            }
        }
        return frame;
    }
        // ── show_tile_bank ───────────────────────────────────────────────────
    public static Frame showTileBank(byte[] chrRom, int bank) {
        if (bank > 1) throw new IllegalArgumentException("bank must be 0 or 1");
        Frame frame = new Frame();
        int tileY = 0;
        int tileX = 0;
        int bankOffset = bank * 0x1000;

        for (int tileN = 0; tileN < 255; tileN++) {
            if (tileN != 0 && tileN % 20 == 0) {
                tileY += 10;
                tileX = 0;
            }

            byte[] tile = new byte[16];
            System.arraycopy(chrRom, bankOffset + tileN * 16, tile, 0, 16);

            for (int y = 0; y <= 7; y++) {
                int upper = Byte.toUnsignedInt(tile[y]);
                int lower = Byte.toUnsignedInt(tile[y + 8]);
                for (int x = 7; x >= 0; x--) {
                    int value = ((1 & upper) << 1) | (1 & lower);
                    upper >>= 1;
                    lower >>= 1;
                    int[] color = paletteColor(value);
                    frame.setPixel(tileX + x, tileY + y, color[0], color[1], color[2]);
                }
            }
            tileX += 10;
        }
        return frame;
    }

    // ── main ─────────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        // ── Init GLFW ────────────────────────────────────────────────────
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE,   GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        long window = glfwCreateWindow(WIN_W, WIN_H, "Tile viewer", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Failed to create GLFW window");

        // Escape or window-close → exit  (mirrors SDL2 event loop)
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS)
                glfwSetWindowShouldClose(win, true);
        });

        // Centre on primary monitor
        try (MemoryStack stack = stackPush()) {
            IntBuffer pW = stack.mallocInt(1);
            IntBuffer pH = stack.mallocInt(1);
            glfwGetWindowSize(window, pW, pH);
            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidMode != null) {
                glfwSetWindowPos(window,
                    (vidMode.width()  - pW.get(0)) / 2,
                    (vidMode.height() - pH.get(0)) / 2);
            }
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);   // vsync  (mirrors present_vsync)
        glfwShowWindow(window);

        // ── Init OpenGL ──────────────────────────────────────────────────
        GL.createCapabilities();

        // 2-D orthographic projection: pixel (0,0) = top-left
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, WIN_W, WIN_H, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glViewport(0, 0, WIN_W, WIN_H);

        // ── Load ROM & build frame ───────────────────────────────────────
        byte[] bytes = Files.readAllBytes(Paths.get("pacman.nes"));
        Rom rom  = new Rom(bytes);
        Frame rightBank = showTileBank(rom.chrRom, 1);

        // Upload pixel data to an OpenGL texture (GL_NEAREST = pixel-perfect)
        int texId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        ByteBuffer pixelBuf = memAlloc(rightBank.data.length);
        pixelBuf.put(rightBank.data).flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB,
                     Frame.WIDTH, Frame.HEIGHT, 0,
                     GL_RGB, GL_UNSIGNED_BYTE, pixelBuf);
        memFree(pixelBuf);

        // ── Render / event loop ──────────────────────────────────────────
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT);

            // Full-window textured quad – GPU handles the 3× scale
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, texId);
            glBegin(GL_QUADS);
                glTexCoord2f(0, 0); glVertex2f(0,     0);
                glTexCoord2f(1, 0); glVertex2f(WIN_W, 0);
                glTexCoord2f(1, 1); glVertex2f(WIN_W, WIN_H);
                glTexCoord2f(0, 1); glVertex2f(0,     WIN_H);
            glEnd();
            glDisable(GL_TEXTURE_2D);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }

        // ── Cleanup ──────────────────────────────────────────────────────
        glDeleteTextures(texId);
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}
