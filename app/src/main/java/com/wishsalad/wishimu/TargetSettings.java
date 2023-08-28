package com.wishsalad.wishimu;

import android.hardware.SensorManager;

public class TargetSettings {
    private final String toIp;
    private final int port;
    private final byte deviceIndex;
    private final boolean sendOrientation;
    private final boolean sendRaw;
    private final int sampleRate;
    private SensorManager sensorManager;

    public TargetSettings(String toIp, int port, byte deviceIndex, boolean sendOrientation, boolean sendRaw, int sampleRate) {
        this.toIp = toIp;
        this.port = port;
        this.deviceIndex = deviceIndex;
        this.sensorManager = sensorManager;
        this.sendOrientation = sendOrientation;
        this.sendRaw = sendRaw;
        this.sampleRate = sampleRate;
    }

    public String getToIp() {
        return toIp;
    }

    public int getPort() {
        return port;
    }

    public byte getDeviceIndex() {
        return deviceIndex;
    }

    public boolean getSendOrientation() {
        return sendOrientation;
    }

    public boolean getSendRaw() {
        return sendRaw;
    }

    public int getSampleRate() {
        return sampleRate;
    }
}