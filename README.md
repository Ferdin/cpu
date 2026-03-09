## Memory Management

Learn:

✅ How NES memory is organized
✅ Memory-mapped I/O
✅ Mirroring
✅ Building a proper Bus class

## Test CPU with ROMS

✅ Refactor the working CPU to follow DRY principle.

✅ Run Emulation tests with the roms available in the link below:
https://www.nesdev.org/wiki/Emulator_tests (will do more as I progress)

## The PPU (Graphics) ⭐ (I am here!)

Time: Many hours!
This is the big one - learning how the NES displays graphics.
Test the PacMan rom

## Notes:

### Memory

NES has 65,536 memory addresses (0x0000 to 0xFFFF). But different parts of this address space mean different things.

Address Range Size What It's Used For

```
 0x0000 - 0x00FF 256 bytes Zero Page (Fast RAM)
 0x0100 - 0x01FF 256 bytes Stack
 0x0200 - 0x07FF 1.5 KB General RAM
 0x0800 - 0x1FFF (mirrors) Mirrors of 0x0000-0x07FF
 0x2000 - 0x2007 8 bytes PPU Registers (Graphics)
 0x2008 - 0x3FFF (mirrors) Mirrors of PPU registers
 0x4000 - 0x4017 24 bytes APU & I/O Registers (Sound/Input)
 0x4018 - 0x401F 8 bytes APU & I/O (rarely used)
 0x4020 - 0xFFFF ~49 KB Cartridge space (ROM, mapper)
```

More generalized layout:

0xFFFF
Interrupt Vectors
0xFFFA

0x8000
ROM (Program Code typically)
0x0200

0x0100
Stack Page
0x01FF

0x0000
Zero Page
0x00FF

### Stack

In the 6502, the stack:

- Lives in memory from 0x0100 to 0x01FF (256 bytes)
- Uses the SP (Stack Pointer) register to track the top
- Grows downward (high address → low address)

### Stack Pointer (SP)

The SP register points to the next free space on the stack.

When you PUSH a value:

- Store the value at 0x0100 + SP
- Decrement SP (move down)

When you POP a value:

- Increment SP (move up)
- Read the value from 0x0100 + SP

### Understanding the `|=` Operator

The `|=` is a **bitwise OR assignment**. this is how it works:

### What is `|` (OR)?

```
a | b   means:  Set bit to 1 if EITHER a OR b has that bit set

Example:
  00000001  (0x01)
| 10000000  (0x80)
----------
  10000001  (0x81)  ← Result has both bits set!
```

### Understanding the `&` Operator

The `&` is a **bitwise AND**. It checks if a specific bit is set:

```
a & b   means:  Set bit to 1 ONLY if BOTH a AND b have that bit set

Example:
  10000001  (0x81)
& 00000001  (0x01 - checking bit 0)
----------
  00000001  (bit 0 was set!)

  10000001  (0x81)
& 00000010  (0x02 - checking bit 1)
----------
  00000000  (bit 1 was NOT set)
```

### High and Low Bytes

Eg: 10101010 01010101

The _high byte_ is _10101010_

The _low byte_ is _01010101_

Writing 0x1234 at address 0x8000:<br/>
memWriteU16(0x8000, 0x1234);

Results in:
memory[0x8000] = 0x34 (low byte)
memory[0x8001] = 0x12 (high byte)

Reading it back:
int value = memReadU16(0x8000); // Returns 0x1234

reset() - Resets the CPU state:

Clears all registers to 0
Reads the program counter from address 0xFFFC (the reset vector in 6502 architecture)

load() - Loads a program into memory:

Copies the program to memory starting at address 0x8000
Writes the start address (0x8000) to the reset vector at 0xFFFC

loadAndRun() - Convenience method that:

Loads the program
Resets the CPU (which sets the program counter to 0x8000)
Starts execution

Cycles - All done accordingly

Addressing modes - All done.

PPU - Started.

### What is the PPU?

The PPU (Picture Processing Unit) is a separate chip from the CPU. It runs concurrently with the CPU and its sole job is to produce the video signal — 256×240 pixels at ~60fps.
The CPU and PPU communicate through memory-mapped registers (0x2000–0x2007 in CPU address space).

The NES screen is built from 3 layers stacked on top of each other:
[Background tiles] ← drawn from nametables using pattern tables
[Sprites (OAM)] ← up to 64 sprites, drawn from pattern tables  
[Palette] ← both layers look up colors here

```
0x0000–0x0FFF  Pattern Table 0 ─┐ CHR ROM
0x1000–0x1FFF  Pattern Table 1 ─┘ (from cartridge)

0x2000–0x23FF  Nametable 0 ─┐
0x2400–0x27FF  Nametable 1 │ VRAM (2KB on NES)
0x2800–0x2BFF  Nametable 2 │ mirrored depending
0x2C00–0x2FFF  Nametable 3 ─┘ on cartridge

0x3F00–0x3F1F  Palette RAM (32 bytes)
```

_The 5 Core Concepts_

1. Pattern Tables (CHR ROM)
   These store tile pixel data. Each tile is 8×8 pixels. Every tile takes 16 bytes — 2 bitplanes combined to give each pixel a 2-bit value (0–3).
   Pixel color index = bit from plane 1 | bit from plane 0
   → gives you 0, 1, 2, or 3
   → that index goes into the palette to get the actual color
2. Nametables (VRAM)
   A nametable is a 32×30 grid of tile indices (960 bytes). Each byte says "draw tile #X from the pattern table here." The last 64 bytes of each nametable are the attribute table — they assign palette numbers to 2×2 groups of tiles.
3. Mirroring
   The NES only has 2KB of VRAM but the address space has room for 4 nametables. So two of them are always mirrors of the real ones. How they mirror depends on the cartridge:

**Horizontal mirroring** — top and bottom share, good for vertical scrolling games
**Vertical mirroring** — left and right share, good for horizontal scrolling games

This is exactly what mirrorVramAddr() in your code handles. 4. OAM (Object Attribute Memory)
256 bytes of sprite data — holds 64 sprites × 4 bytes each:
Byte 0: Y position
Byte 1: Tile index
Byte 2: Attributes (palette, flip horizontal/vertical, priority)
Byte 3: X position
The CPU writes to OAM either byte-by-byte via 0x2003/0x2004, or all at once via OAM DMA (writes 256 bytes instantly via 0x4014). 5. Palette Table
32 bytes total — 16 for background, 16 for sprites. Each entry is an index into the NES's 64-color master palette. Color index 0 in any palette is always transparent (shows background).

_The PPU Registers (0x2000–0x2007)_
AddressNameR/WWhat it does0x2000PPUCTRLWConfig: NMI enable, sprite size, pattern table select, VRAM increment0x2001PPUMASKWEnable/disable rendering, grayscale, color emphasis0x2002PPUSTATUSRVBlank flag, sprite 0 hit, sprite overflow0x2003OAMADDRWSet OAM write position0x2004OAMDATAR/WRead/write one byte of OAM0x2005PPUSCROLLWSet scroll position (written twice: X then Y)0x2006PPUADDRWSet VRAM address (written twice: high byte then low)0x2007PPUDATAR/WRead/write VRAM at current address, then auto-increment

_The Internal Data Buffer — Why it Exists_
When the CPU reads from PPUDATA (0x2007), it doesn't get the data immediately. The PPU returns the previously buffered value and loads the new value into the buffer. This is a one-read delay caused by the PPU's internal bus timing.
Exception: Palette reads (0x3F00+) are returned immediately — no buffering.
This is exactly what internalDataBuf in your code is doing.

_The PPU Timing Loop_
The PPU runs on its own cycle counter. For every 1 CPU cycle, the PPU runs 3 PPU cycles. A full frame looks like:
Scanlines 0–239: Visible scanlines (drawing pixels)
Scanline 240: Post-render (idle)
Scanline 241: VBlank START → set VBlank flag → trigger NMI to CPU
Scanlines 242–260: VBlank period (CPU does its work here)
Scanline 261: Pre-render (prepare for next frame)

Each scanline = 341 PPU cycles
Total per frame = 262 × 341 = ~89,342 cycles
VBlank is the window where the CPU is safe to update PPU memory without causing visual glitches. The NMI interrupt fires at the start of VBlank to tell the CPU "now is your chance."

**1. Scanline**
Think of it like an old CRT TV. The electron gun draws the screen one horizontal line at a time, left to right, top to bottom.
Screen (256 × 240)

```
Scanline 0 →  ████████████████████████████████ ← PPU draws this first
Scanline 1 →  ████████████████████████████████
Scanline 2 →  ████████████████████████████████
...
Scanline 239→ ████████████████████████████████ ← last visible line
──────────────────────────────────────────────
Scanline 240→ (idle, nothing drawn)
Scanline 241→ VBLANK STARTS here ← CPU gets NMI interrupt
...
Scanline 261→ (pre-render, reset for next frame)
```

The PPU doesn't draw the whole frame at once. It draws one pixel at a time, left to right, and when it finishes a row it moves to the next scanline. Each scanline takes 341 PPU clock cycles — 256 for the visible pixels, the rest for housekeeping.
Why does this matter? Some NES games do tricks by changing PPU registers mid-scanline (called "raster effects") to create split-screen scrolling or status bars.

**2. DMA (Direct Memory Access)**
Normally, if the CPU wants to fill OAM (256 bytes of sprite data), it would have to do 256 individual write operations — very slow.
DMA is a hardware shortcut that says:

"Copy 256 bytes from CPU RAM starting at address X, directly into OAM — do it in one shot."

Normal way (slow):
CPU → write byte 0 → PPU OAM
CPU → write byte 1 → PPU OAM
CPU → write byte 2 → PPU OAM
... × 256 times

DMA way (fast):
CPU writes one value to 0x4014
Hardware copies all 256 bytes at once → PPU OAM
The CPU writes a page number (e.g. 0x02) to address 0x4014. The hardware then copies the entire page 0x0200–0x02FF from RAM straight into OAM.
The catch: The CPU is frozen/suspended for 513–514 cycles while DMA happens. It literally cannot do anything else. This is why in your code writeOamDma() just blindly copies all 256 bytes in a loop — the timing complexity is handled at the bus level.

**3. Bitplane**
This is how the NES stores tile pixel data efficiently.
Each tile is 8×8 pixels. Each pixel needs a 2-bit color index (values 0–3). Naively you'd store 2 bits × 64 pixels = 128 bits = 16 bytes per tile.
The NES does this with two separate 1-bit layers called bitplanes:
Tile "A" in memory — 16 bytes total:

Plane 0 (first 8 bytes): Plane 1 (next 8 bytes):
Row 0: 0 1 1 1 1 0 0 0 Row 0: 0 1 1 1 1 0 0 0
Row 1: 0 1 0 0 0 1 0 0 Row 1: 0 1 1 0 0 1 0 0
... ...
To get the actual color index of any pixel, you combine the two planes:
Pixel color = (Plane1 bit) << 1 | (Plane0 bit)

Plane1=0, Plane0=0 → color 0 (transparent/background)
Plane1=0, Plane0=1 → color 1
Plane1=1, Plane0=0 → color 2
Plane1=1, Plane0=1 → color 3
Visually:
Plane 0 Plane 1 Combined (color index per pixel)
. 1 1 1 . 1 1 1 . 3 3 3
. 1 . . + . 1 1 . = . 3 2 .
. . . . . 1 1 . . 2 2 .
That color index (0–3) is then fed into the palette table to get the final NES color. So the full pipeline per pixel is:
Bitplane → 2-bit index → Palette lookup → NES color (0–63) → RGB on screen

How They Connect
Each SCANLINE, the PPU:

1. Fetches tile data from CHR ROM (reads BITPLANES)
2. Combines bitplanes → color indices
3. Looks up palette → final colors
4. Pushes pixels to screen

Meanwhile, during VBLANK, the CPU:

1. Gets NMI interrupt
2. Uses DMA to quickly refresh all sprite positions in OAM
3. Updates scroll, palette, nametables for next frame
