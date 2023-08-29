package io.github.masdaster.motion_tracker;

import androidx.annotation.NonNull;

public class SampleRate {
    private final int id;
    private final String name;

    public SampleRate(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    @NonNull
    public String toString() {
        return name;
    }
}
