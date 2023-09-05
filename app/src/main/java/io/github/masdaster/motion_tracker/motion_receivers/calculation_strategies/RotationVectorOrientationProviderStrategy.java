package io.github.masdaster.motion_tracker.motion_receivers.calculation_strategies;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;

/**
 * Created by Z-Byte on .
 */
public class RotationVectorOrientationProviderStrategy implements OrientationProviderStrategy {
    private final float[] rotationMatrix = new float[16], rotationVector = new float[3];

    @NonNull
    @Override
    public int[] getSensorTypes() {
        return new int[]{Sensor.TYPE_ROTATION_VECTOR};
    }

    @NonNull
    @Override
    public float[] calculate(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            System.arraycopy(event.values, 0, rotationVector, 0, 3);
        }
        float[] orientation = new float[3];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);
        SensorManager.getOrientation(rotationMatrix, orientation);
        return orientation;
    }

    @NonNull
    @Override
    public CalculatedDataType getDataType() {
        return CalculatedDataType.ORIENTATION;
    }
}
