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
public class SyncFusionEnhancedOrientationProvider extends SyncFusionBaseOrientationProvider {

    /**
     * This weight determines indirectly how much the rotation sensor will be used to correct. This weight will be
     * multiplied by the velocity to obtain the actual weight. (in sensor-fusion-scenario 2 -
     * SensorSelection.GyroscopeAndRotationVector2).
     * Must be a value between 0 and approx. 0.04 (because, if multiplied with a velocity of up to 25, should be still
     * less than 1, otherwise the SLERP will not correctly interpolate). Should be close to zero.
     */
    private static final float INDIRECT_INTERPOLATION_WEIGHT = 0.01f;

    @Override
    protected void slerpQuaternionGyroscope() {
        quaternionGyroscope.slerp(quaternionRotationVector, interpolatedQuaternion, (float) (INDIRECT_INTERPOLATION_WEIGHT * gyroscopeRotationVelocity));
    }

    @Override
    protected float getOutlierPanicThreshold() {
        return 0.75f;
    }
}
