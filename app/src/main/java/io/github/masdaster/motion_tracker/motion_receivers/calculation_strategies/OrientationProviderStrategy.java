package io.github.masdaster.motion_tracker.motion_receivers.calculation_strategies;

import android.hardware.SensorEvent;

import androidx.annotation.NonNull;

/**
 * Created by Z-Byte on .
 */
public interface OrientationProviderStrategy {
    @NonNull
    int[] getSensorTypes();

    @NonNull
    float[] calculate(SensorEvent event);

    @NonNull
    CalculatedDataType getDataType();

    enum CalculatedDataType {
        RAW, ORIENTATION
    }
}
