package io.github.masdaster.motion_tracker.motion_receivers;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;

import java.util.logging.Logger;

import io.github.masdaster.motion_tracker.motion_receivers.calculation_strategies.BasicOrientationProviderStrategy;
import io.github.masdaster.motion_tracker.motion_receivers.calculation_strategies.OrientationProviderStrategy;
import io.github.masdaster.motion_tracker.motion_receivers.calculation_strategies.RawOrientationProviderStrategy;
import io.github.masdaster.motion_tracker.motion_receivers.calculation_strategies.RotationVectorOrientationProviderStrategy;

/**
 * Created by Z-Byte on .
 */
public final class OrientationProvider implements SensorEventListener {
    @NonNull
    private static final Logger logger = Logger.getLogger("OrientationProvider");
    @NonNull
    private final OrientationProviderStrategy calculationStrategy;
    @NonNull
    private float[] data = new float[0];

    public OrientationProvider(@NonNull OrientationProviderStrategy calculationStrategy) {
        this.calculationStrategy = calculationStrategy;
    }

    public static OrientationProvider Create(String orientationProviderStrategyTag) {
        OrientationProviderStrategy strategy;
        switch (orientationProviderStrategyTag) {
            case "raw":
                strategy = new RawOrientationProviderStrategy();
                break;
            case "basic":
                strategy = new BasicOrientationProviderStrategy();
                break;
            case "rotation_vector":
                strategy = new RotationVectorOrientationProviderStrategy();
                break;
            default:
                logger.warning("Tag '" + orientationProviderStrategyTag + "' for orientation provider strategy is unknown.");
                strategy = new RotationVectorOrientationProviderStrategy();
                break;
        }
        return new OrientationProvider(strategy);
    }

    @NonNull
    public float[] getData() {
        return data;
    }

    @NonNull
    public OrientationProviderStrategy.CalculatedDataType getDataType() {
        return calculationStrategy.getDataType();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        synchronized (this) {
            data = calculationStrategy.calculate(sensorEvent);
            notifyAll();
        }
    }

    public void register(@NonNull SensorManager sensorManager, int sampleRate) {
        for (int sensorType : calculationStrategy.getSensorTypes()) {
            Sensor sensor = sensorManager.getDefaultSensor(sensorType);
            if (sensor == null) {
                logger.warning("Cannot resolve sensor of type " + sensorType + ".");
            } else {
                sensorManager.registerListener(this, sensor, sampleRate);
            }
        }
    }

    public void unregister(@NonNull SensorManager sensorManager) {
        sensorManager.unregisterListener(this);
    }
}
