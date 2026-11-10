package net.sourceforge.autotalent;

public class Autotalent {
    private static Autotalent sInstance = null;
    private static int sSampleRate = -1;

    public static synchronized Autotalent getInstance(int sampleRate) {
        if (sInstance == null || sSampleRate != sampleRate) {
            sInstance = new Autotalent(sampleRate);
            sSampleRate = sampleRate;
        }
        return sInstance;
    }

    private Autotalent(int sampleRate) {
    }

    public void setConcertA(float concertA) {
    }

    public void setKey(char key) {
    }

    public void setFixedPitch(float pitch) {
    }

    public void setFixedPull(float pull) {
    }

    public void setStrength(float strength) {
    }

    public void setSmoothness(float smoothness) {
    }

    public void setPitchShift(float shift) {
    }

    public void setScaleRotate(int rotate) {
    }

    public void setLfoDepth(float depth) {
    }

    public void setLfoRate(float rate) {
    }

    public void setLfoShape(float shape) {
    }

    public void setLfoSymmetric(float symmetric) {
    }

    public void setLfoQuantization(int quantization) {
    }

    public void enableFormantCorrection(boolean enabled) {
    }

    public void setFormantWarp(float warp) {
    }

    public void setMix(float mix) {
    }

    public void process(short[] samples, int numSamples) {
        // Pass-through processing stub
    }

    public void process(short[] samples, short[] instrumental, int numSamples) {
        // Pass-through processing stub
    }

    public void close() {
        sInstance = null;
    }
}