package io.github.masdaster.motion_tracker.motion_receivers.calculation_strategies;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;

import java.util.logging.Logger;

/**
 * Created by Z-Byte on .
 */
public class BasicOrientationProviderStrategy implements OrientationProviderStrategy {
    @NonNull
    private final Logger logger = Logger.getLogger("BasicMotionReceiver");
    @NonNull
    private final float[] accSensorValues = new float[3], magSensorValues = new float[3], R = new float[9], I = new float[9];

    @NonNull
    @Override
    public int[] getSensorTypes() {
        return new int[]{Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD};
    }

    @NonNull
    @Override
    public float[] calculate(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, accSensorValues, 0, 3);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, magSensorValues, 0, 3);
                break;
        }
        float[] orientation = new float[3];
        if (SensorManager.getRotationMatrix(R, I, accSensorValues, magSensorValues)) {
            SensorManager.getOrientation(R, orientation);
        } else {
            logger.warning("Cannot calculate orientation.");
        }
        return orientation;
    }

    @NonNull
    @Override
    public CalculatedDataType getDataType() {
        return CalculatedDataType.ORIENTATION;
    }
}
