package com.ferdin.nescpu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


public class TestGame extends JPanel implements KeyListener {

    private static DemoNES nes;
    private static Random rng = new Random();
    private static TestGame panel;

    private static final int SCREEN_WIDTH = 32;
    private static final int SCREEN_HEIGHT = 32;
    private static final int SCALE = 10; // each NES pixel = 10x10 screen pixels
    // private static final int CPU_CLOCK = 1_790_000;
    // private static final int FPS = 60;
    // private static final int CYCLES_PER_FRAME = CPU_CLOCK / FPS;

    private BufferedImage screen = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB);
    private volatile byte currentInput = 0;

    public TestGame() {
        setPreferredSize(new Dimension(SCREEN_WIDTH * SCALE, SCREEN_HEIGHT * SCALE));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(screen, 0, 0, SCREEN_WIDTH * SCALE, SCREEN_HEIGHT * SCALE, null);
    }

    public boolean updateScreen() {
        boolean changed = false;
        for (int addr = 0x0200; addr < 0x0600; addr++) {
            int colorIdx = nes.memRead(addr) & 0xFF;
            int rgb = color(colorIdx);
            int pixel = addr - 0x0200;
            int x = pixel % SCREEN_WIDTH;
            int y = pixel / SCREEN_WIDTH;
            if (screen.getRGB(x, y) != rgb) {
                screen.setRGB(x, y, rgb);
                changed = true;
            }
        }
        return changed;
    }

    private int color(int value) {
        switch (value) {
            case 0:  return 0x000000;
            case 1:  return 0xFFFFFF;
            case 2: case 9:  return 0x808080;
            case 3: case 10: return 0xFF0000;
            case 4: case 11: return 0x00FF00;
            case 5: case 12: return 0x0000FF;
            case 6: case 13: return 0xFF00FF;
            case 7: case 14: return 0xFFFF00;
            default: return 0x00FFFF;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: currentInput = (byte)0x77; break;
            case KeyEvent.VK_S: currentInput = (byte)0x73; break;
            case KeyEvent.VK_A: currentInput = (byte)0x61; break;
            case KeyEvent.VK_D: currentInput = (byte)0x64; break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // currentInput = 0; // clear input on release
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private static byte[] loadRomFile(String resourcePath) throws IOException {
        try (InputStream is = TestGame.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }
            return is.readAllBytes();
        }
    }
    public static void main(String[] args) throws Exception {

        byte[] rawRom = loadRomFile("/com/ferdin/nescpu/roms/snake.nes");
        Rom rom = new Rom(rawRom);
        Bus bus = new Bus(rom);
        nes = new DemoNES(bus);
        // int[] game_code = new int[]{
        //     0x20, 0x06, 0x06, 0x20, 0x38, 0x06, 0x20, 0x0d, 0x06, 0x20, 0x2a, 0x06, 0x60, 0xa9, 0x02, 0x85,
        //     0x02, 0xa9, 0x04, 0x85, 0x03, 0xa9, 0x11, 0x85, 0x10, 0xa9, 0x10, 0x85, 0x12, 0xa9, 0x0f, 0x85,
        //     0x14, 0xa9, 0x04, 0x85, 0x11, 0x85, 0x13, 0x85, 0x15, 0x60, 0xa5, 0xfe, 0x85, 0x00, 0xa5, 0xfe,
        //     0x29, 0x03, 0x18, 0x69, 0x02, 0x85, 0x01, 0x60, 0x20, 0x4d, 0x06, 0x20, 0x8d, 0x06, 0x20, 0xc3,
        //     0x06, 0x20, 0x19, 0x07, 0x20, 0x20, 0x07, 0x20, 0x2d, 0x07, 0x4c, 0x38, 0x06, 0xa5, 0xff, 0xc9,
        //     0x77, 0xf0, 0x0d, 0xc9, 0x64, 0xf0, 0x14, 0xc9, 0x73, 0xf0, 0x1b, 0xc9, 0x61, 0xf0, 0x22, 0x60,
        //     0xa9, 0x04, 0x24, 0x02, 0xd0, 0x26, 0xa9, 0x01, 0x85, 0x02, 0x60, 0xa9, 0x08, 0x24, 0x02, 0xd0,
        //     0x1b, 0xa9, 0x02, 0x85, 0x02, 0x60, 0xa9, 0x01, 0x24, 0x02, 0xd0, 0x10, 0xa9, 0x04, 0x85, 0x02,
        //     0x60, 0xa9, 0x02, 0x24, 0x02, 0xd0, 0x05, 0xa9, 0x08, 0x85, 0x02, 0x60, 0x60, 0x20, 0x94, 0x06,
        //     0x20, 0xa8, 0x06, 0x60, 0xa5, 0x00, 0xc5, 0x10, 0xd0, 0x0d, 0xa5, 0x01, 0xc5, 0x11, 0xd0, 0x07,
        //     0xe6, 0x03, 0xe6, 0x03, 0x20, 0x2a, 0x06, 0x60, 0xa2, 0x02, 0xb5, 0x10, 0xc5, 0x10, 0xd0, 0x06,
        //     0xb5, 0x11, 0xc5, 0x11, 0xf0, 0x09, 0xe8, 0xe8, 0xe4, 0x03, 0xf0, 0x06, 0x4c, 0xaa, 0x06, 0x4c,
        //     0x35, 0x07, 0x60, 0xa6, 0x03, 0xca, 0x8a, 0xb5, 0x10, 0x95, 0x12, 0xca, 0x10, 0xf9, 0xa5, 0x02,
        //     0x4a, 0xb0, 0x09, 0x4a, 0xb0, 0x19, 0x4a, 0xb0, 0x1f, 0x4a, 0xb0, 0x2f, 0xa5, 0x10, 0x38, 0xe9,
        //     0x20, 0x85, 0x10, 0x90, 0x01, 0x60, 0xc6, 0x11, 0xa9, 0x01, 0xc5, 0x11, 0xf0, 0x28, 0x60, 0xe6,
        //     0x10, 0xa9, 0x1f, 0x24, 0x10, 0xf0, 0x1f, 0x60, 0xa5, 0x10, 0x18, 0x69, 0x20, 0x85, 0x10, 0xb0,
        //     0x01, 0x60, 0xe6, 0x11, 0xa9, 0x06, 0xc5, 0x11, 0xf0, 0x0c, 0x60, 0xc6, 0x10, 0xa5, 0x10, 0x29,
        //     0x1f, 0xc9, 0x1f, 0xf0, 0x01, 0x60, 0x4c, 0x35, 0x07, 0xa0, 0x00, 0xa5, 0xfe, 0x91, 0x00, 0x60,
        //     0xa6, 0x03, 0xa9, 0x00, 0x81, 0x10, 0xa2, 0x00, 0xa9, 0x01, 0x81, 0x10, 0x60, 0xa2, 0x00, 0xea,
        //     0xea, 0xca, 0xd0, 0xfb, 0x60
        // };
        // nes.load(game_code);
        nes.reset();

        // nes.runWithCallback(c -> {
        //     System.out.println(TraceUtil.trace(c));
        // });

        // Setup Swing window
        JFrame frame = new JFrame("Snake Game");
        panel = new TestGame();
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        panel.requestFocusInWindow();

        // Game loop using Swing Timer
        new Thread(() -> {
            final long FRAME_NS = 1_000_000_000L / 60;

            while (true) {
                long frameStart = System.nanoTime();

                int cyclesThisFrame = 0;
                while (cyclesThisFrame < 500) { // small batch, not full CYCLES_PER_FRAME
                    int op = nes.memRead(nes.getProgramCounter()) & 0xFF;
                    if (op == 0x00) return;

                    nes.memWrite(0xFF, panel.currentInput);
                    nes.memWrite(0xFE, (byte)(rng.nextInt(15) + 1));

                    cyclesThisFrame += nes.step();
                }

                panel.updateScreen();
                SwingUtilities.invokeLater(panel::repaint);

                long elapsed = System.nanoTime() - frameStart;
                long remaining = FRAME_NS - elapsed;
                if (remaining > 0) {
                    try {
                        Thread.sleep(remaining / 1_000_000, (int)(remaining % 1_000_000));
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
            }
        }).start();
    }
}