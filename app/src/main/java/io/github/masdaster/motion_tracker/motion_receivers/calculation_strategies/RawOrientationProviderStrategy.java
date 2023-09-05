package io.github.masdaster.motion_tracker.motion_receivers.calculation_strategies;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import androidx.annotation.NonNull;

/**
 * Created by Z-Byte on .
 */
public class RawOrientationProviderStrategy implements OrientationProviderStrategy {
    @NonNull
    private final float[] rawValues = new float[9];

    @NonNull
    @Override
    public int[] getSensorTypes() {
        return new int[]{Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_MAGNETIC_FIELD};
    }

    @NonNull
    @Override
    public float[] calculate(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, rawValues, 0, 3);
                break;
            case Sensor.TYPE_GYROSCOPE:
                System.arraycopy(event.values, 0, rawValues, 3, 3);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, rawValues, 6, 3);
                break;
        }
        return rawValues;
    }

    @NonNull
    @Override
    public CalculatedDataType getDataType() {
        return CalculatedDataType.RAW;
    }
}
