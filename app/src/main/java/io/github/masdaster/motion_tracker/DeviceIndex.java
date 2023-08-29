package io.github.masdaster.motion_tracker;

import androidx.annotation.NonNull;

import java.util.Locale;

public class DeviceIndex {
    private final byte index;

    public DeviceIndex(byte index) {
        this.index = index;
    }

    public byte getIndex() {
        return index;
    }

    @NonNull
    public String toString() {
        return String.format(Locale.getDefault(), "Device index %d", index);
    }
}
