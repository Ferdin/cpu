package main.java.com.ferdin.nes.apu;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

public class AudioOutput {

    // -------------------------
    // Constants
    // -------------------------
    private static final int SAMPLE_RATE    = 44100;  // Hz
    private static final int BUFFER_COUNT   = 4;      // number of rotating buffers
    private static final int BUFFER_SIZE    = 1024;   // samples per buffer

    // -------------------------
    // OpenAL handles
    // -------------------------
    private long device;
    private long context;
    private int  source;
    private int[] buffers;

    // -------------------------
    // Sample accumulation
    // -------------------------
    private final short[] sampleBuffer = new short[BUFFER_SIZE];
    private int sampleIndex = 0;

    // -------------------------
    // Timing
    // -------------------------
    // NES APU outputs a sample every ~40.6 CPU cycles at 44100 Hz
    // CPU runs at 1,789,773 Hz / 44100 Hz = ~40.58 cycles per sample
    private static final double CPU_CYCLES_PER_SAMPLE = 1_789_773.0 / SAMPLE_RATE;
    private double cycleAccumulator = 0;

    public AudioOutput() {
        init();
    }

    private void init() {
        try {
            System.out.println("Opening AL device...");
            device = alcOpenDevice((CharSequence) null);
            System.out.println("Device: " + device);
            if (device == 0) throw new RuntimeException("Failed to open OpenAL device");

            System.out.println("Creating context...");
            int[] attribs = { 0 };
            context = alcCreateContext(device, attribs);
            System.out.println("Context: " + context);
            if (context == 0) throw new RuntimeException("Failed to create OpenAL context");

            System.out.println("Making context current...");
            alcMakeContextCurrent(context);

            System.out.println("Creating capabilities...");
            ALCCapabilities alcCaps = ALC.createCapabilities(device);
            AL.createCapabilities(alcCaps);

            System.out.println("Generating source and buffers...");
            source  = alGenSources();
            buffers = new int[BUFFER_COUNT];
            alGenBuffers(buffers);

            System.out.println("Setting source properties...");
            alSourcef(source,  AL_GAIN,    1.0f);
            alSourcef(source,  AL_PITCH,   1.0f);
            alSource3f(source, AL_POSITION, 0, 0, 0);
            alSource3f(source, AL_VELOCITY, 0, 0, 0);
            alSourcei(source,  AL_LOOPING,  AL_FALSE);

            System.out.println("Pre-filling buffers with silence...");
            ShortBuffer silence = ByteBuffer
                .allocateDirect(BUFFER_SIZE * 2)
                .order(java.nio.ByteOrder.nativeOrder())
                .asShortBuffer();
            for (int i = 0; i < BUFFER_COUNT; i++) {
                silence.clear();
                alBufferData(buffers[i], AL_FORMAT_MONO16, silence, SAMPLE_RATE);
                alSourceQueueBuffers(source, buffers[i]);
            }

            System.out.println("Starting playback...");
            alSourcePlay(source);
            System.out.println("OpenAL initialized successfully at " + SAMPLE_RATE + " Hz");

        } catch (Exception e) {
            System.out.println("OpenAL init failed at: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Called every APU tick (every 2 CPU cycles)
    // accumulates samples and pushes to OpenAL when buffer is full
    public void receiveSample(float sample, int cpuCycles) {
        cycleAccumulator += cpuCycles;

        while (cycleAccumulator >= CPU_CYCLES_PER_SAMPLE) {
            cycleAccumulator -= CPU_CYCLES_PER_SAMPLE;

            // Convert float [-1, 1] to short [-32768, 32767]
            // APU output is [0, ~1] so we center and scale it
            float centered = (sample * 2.0f) - 1.0f;
            sampleBuffer[sampleIndex++] = (short)(centered * 32767);

            if (sampleIndex >= BUFFER_SIZE) {
                pushBuffer();
                sampleIndex = 0;
            }
        }
    }

    // Pre-allocate a direct ShortBuffer as a class field — don't allocate per call
    private final ShortBuffer directBuffer = ByteBuffer
        .allocateDirect(BUFFER_SIZE * 2)
        .order(java.nio.ByteOrder.nativeOrder())
        .asShortBuffer();

    private void pushBuffer() {
        int processed = alGetSourcei(source, AL_BUFFERS_PROCESSED);
        if (processed == 0) return;

        int buffer = alSourceUnqueueBuffers(source);

        directBuffer.clear();
        directBuffer.put(sampleBuffer, 0, BUFFER_SIZE);
        directBuffer.flip();

        alBufferData(buffer, AL_FORMAT_MONO16, directBuffer, SAMPLE_RATE);
        alSourceQueueBuffers(source, buffer);

        if (alGetSourcei(source, AL_SOURCE_STATE) != AL_PLAYING) {
            alSourcePlay(source);
        }
    }

    public void cleanup() {
        alSourceStop(source);
        alDeleteSources(source);
        alDeleteBuffers(buffers);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    public int getSampleRate() {
        return SAMPLE_RATE;
    }
}