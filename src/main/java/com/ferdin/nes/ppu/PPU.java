package main.java.com.ferdin.nes.ppu;

public interface PPU {
    void writeToCtrl(int value);
    void writeToMask(int value);
    int readStatus();
    void writeToOamAddr(int value);
    void writeToOamData(int value);
    int readOamData();
    void writeToScroll(int value);
    void writeToPpuAddr(int value);
    void writeToData(int value);
    int readData();
    void writeOamDma(int[] data);
}