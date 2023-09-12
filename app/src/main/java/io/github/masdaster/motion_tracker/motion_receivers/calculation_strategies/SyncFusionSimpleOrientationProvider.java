package io.github.masdaster.motion_tracker.motion_receivers.calculation_strategies;

import android.hardware.Sensor;

/**
 * Created by Z-Byte on .
 * <p>
 * The orientation provider that delivers the absolute orientation from the {@link Sensor#TYPE_GYROSCOPE
 * Gyroscope} and {@link Sensor#TYPE_ROTATION_VECTOR Android Rotation Vector sensor}.
 * <p>
 * It mainly relies on the gyroscope, but corrects with the Android Rotation Vector which also provides an absolute
 * estimation of current orientation. The correction is a static weight.
 *
 * @author Alexander Pacha
 */
public class SyncFusionSimpleOrientationProvider extends SyncFusionBaseOrientationProvider {

    /**
     * This weight determines directly how much the rotation sensor will be used to correct (in
     * Sensor-fusion-scenario 1 - SensorSelection.GyroscopeAndRotationVector). Must be a value between 0 and 1.
     * 0 means that the system entirely relies on the gyroscope, whereas 1 means that the system relies entirely on
     * the rotationVector.
     */
    private static final float DIRECT_INTERPOLATION_WEIGHT = 0.005f;

    @Override
    protected void slerpQuaternionGyroscope() {
        quaternionGyroscope.slerp(quaternionRotationVector, interpolatedQuaternion, DIRECT_INTERPOLATION_WEIGHT);
    }

    @Override
    protected float getOutlierPanicThreshold() {
        return 0.65f;
    }
}
