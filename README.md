## NEXT: Section 2 - Flags and Arithmetic ⭐ (I am here!)

Time: 1-2 hours
Learn:

Carry flag (for addition/subtraction overflow)
ADC (Add with Carry)
SBC (Subtract with Carry)
CLC (Clear Carry)
SEC (Set Carry)

This builds directly on what you know (flags) and is essential for real programs.

## Section 3 - The Stack

Time: 1-2 hours
Learn:

What the stack is (a special memory area)
Stack Pointer (SP register)
PHA/PLA (Push/Pull Accumulator)
PHP/PLP (Push/Pull Processor Status)
How JSR/RTS use the stack for subroutines

The stack is like a stack of plates - you can only add/remove from the top!

## Section 4 - Overflow Flag and Signed Arithmetic

Time: 1 hour
Learn:

Overflow flag (for signed number overflow)
BVC/BVS (Branch on overflow)
The difference between Carry (unsigned) and Overflow (signed)

This completes your understanding of all the flags.

## Section 5 - Logical Operations

Time: 1 hour
Learn:

AND (combine bits)
ORA (OR bits together)
EOR (XOR - exclusive OR)
BIT (test bits)

These are used for bit manipulation - turning on/off individual bits.

## Section 6 - Shifts and Rotates

Time: 1 hour
Learn:

ASL (shift left - multiply by 2)
LSR (shift right - divide by 2)
ROL (rotate left)
ROR (rotate right)

## Section 7 - All Addressing Modes

Time: 2-3 hours
Learn the remaining addressing modes:

Absolute,X and Absolute,Y
Indirect,X and Indirect,Y
Zero Page,X and Zero Page,Y

This lets you access arrays and data structures.

## Section 8 - Memory Management

Time: 2 hours
Learn:

How NES memory is organized
Memory-mapped I/O
Mirroring
Building a proper Bus class

## Section 9 - The PPU (Graphics)

Time: Many hours!
This is the big one - learning how the NES displays graphics.

## Notes:

### Memory

NES has 65,536 memory addresses (0x0000 to 0xFFFF). But different parts of this address space mean different things.

Address Range Size What It's Used For
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
0x0000 - 0x00FF 256 bytes Zero Page (Fast RAM)
0x0100 - 0x01FF 256 bytes Stack
0x0200 - 0x07FF 1.5 KB General RAM
0x0800 - 0x1FFF (mirrors) Mirrors of 0x0000-0x07FF
0x2000 - 0x2007 8 bytes PPU Registers (Graphics)
0x2008 - 0x3FFF (mirrors) Mirrors of PPU registers
0x4000 - 0x4017 24 bytes APU & I/O Registers (Sound/Input)
0x4018 - 0x401F 8 bytes APU & I/O (rarely used)
0x4020 - 0xFFFF ~49 KB Cartridge space (ROM, mapper)

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

The high byte is 10101010
The low byte is 01010101
