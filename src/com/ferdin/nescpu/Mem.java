package com.ferdin.nescpu;

public interface Mem {
    int memRead(int addr);
    void memWrite(int addr, int data);
    int memReadU16(int pos);
    void memWriteU16(int pos, int data);
}
