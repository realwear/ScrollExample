/**
 * RealWear Development Software, Source Code and Object Code
 * (c) RealWear, Inc. All rights reserved.
 * <p>
 * Contact info@realwear.com for further information about the use of this code.
 */

package com.realwear.scrollexample;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;
import android.view.WindowManager;

import static android.hardware.SensorManager.getOrientation;
import static android.hardware.SensorManager.getRotationMatrixFromVector;
import static android.hardware.SensorManager.remapCoordinateSystem;
import static java.lang.Math.abs;

/**
 * Controller for utilizing the accelerometer to add scroll functionality to elements
 */
public class TiltScrollController implements SensorEventListener {
    private static final float THRESHOLD_MOTION = 0.001f;
    private static final int SENSOR_DELAY_MICROS = 32 * 1000; // 32ms

    private final ScrollListener mListener;

    private final WindowManager mWindowManager;
    private final SensorManager mSensorManager;
    private final Sensor mRotationSensor;

    private final float[] mRotationMatrix = new float[9];
    private final float[] mAdjustedRotationMatrix = new float[9];
    private final float[] mOrientation = new float[3];

    private boolean mInitialized = false;

    private int mLastAccuracy;
    private float mOldZ;
    private float mOldX;

    /**
     * Constructor.
     *
     * @param ctx            The context that the scroll view is running in.
     * @param scrollListener The listener for scroll events.
     */
    public TiltScrollController(Context ctx, ScrollListener scrollListener) {
        mListener = scrollListener;

        mWindowManager = ctx.getSystemService(WindowManager.class);
        mSensorManager = ctx.getSystemService(SensorManager.class);

        // Can be null if the sensor hardware is not available
        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY_MICROS);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mLastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }

        if (event.sensor == mRotationSensor) {
            updateRotation(event.values.clone());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (mLastAccuracy != accuracy) {
            mLastAccuracy = accuracy;
        }
    }

    /**
     * Update the rotation based on changes to the device's sensors.
     *
     * @param rotationVector The new rotation vectors.
     */
    private void updateRotation(float[] rotationVector) {
        // Get rotation's based on vector locations
        getRotationMatrixFromVector(mRotationMatrix, rotationVector);

        final int worldAxisForDeviceAxisX;
        final int worldAxisForDeviceAxisY;

        // Remap the axes as if the device screen was the instrument panel, and adjust the rotation
        // matrix for the device orientation.
        switch (mWindowManager.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
            default:
                worldAxisForDeviceAxisX = SensorManager.AXIS_X;
                worldAxisForDeviceAxisY = SensorManager.AXIS_Z;
                break;
            case Surface.ROTATION_90:
                worldAxisForDeviceAxisX = SensorManager.AXIS_Z;
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X;
                break;
            case Surface.ROTATION_180:
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X;
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Z;
                break;
            case Surface.ROTATION_270:
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Z;
                worldAxisForDeviceAxisY = SensorManager.AXIS_X;
                break;
        }

        remapCoordinateSystem(
                mRotationMatrix,
                worldAxisForDeviceAxisX,
                worldAxisForDeviceAxisY,
                mAdjustedRotationMatrix);

        // Transform rotation matrix into azimuth/pitch/roll
        getOrientation(mAdjustedRotationMatrix, mOrientation);

        // Convert radians to degrees and flat
        float newX = (float) Math.toDegrees(mOrientation[1]);
        float newZ = (float) Math.toDegrees(mOrientation[0]);

        // How many degrees has the users head rotated since last time.
        float deltaX = applyThreshold(angularRounding(newX - mOldX));
        float deltaZ = applyThreshold(angularRounding(newZ - mOldZ));

        // Ignore first head position in order to find base line
        if (!mInitialized) {
            mInitialized = true;
            deltaX = 0;
            deltaZ = 0;
        }

        mOldX = newX;
        mOldZ = newZ;

        mListener.onTilt((int) deltaZ * 60, (int) deltaX * 60);
    }

    /**
     * Apply a minimum value to the input.
     * If input is below the threshold, return zero to remove noise.
     *
     * @param input The value to inspect.
     * @return The value of input if within the threshold, or 0 if it is outside.
     */
    private float applyThreshold(float input) {
        return abs(input) > THRESHOLD_MOTION ? input : 0;
    }

    /**
     * Adjust the angle of rotation to take into account on the device orientation.
     *
     * @param input The rotation.
     * @return The rotation taking into account the device orientation.
     */
    private float angularRounding(float input) {
        if (input >= 180.0f) {
            return input - 360.0f;
        } else if (input <= -180.0f) {
            return 360 + input;
        } else {
            return input;
        }
    }

    /**
     * Request access to sensors
     */
    public void requestAllSensors() {
        mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY_MICROS);
    }

    /**
     * Release the sensors when they are no longer used
     */
    public void releaseAllSensors() {
        mSensorManager.unregisterListener(this, mRotationSensor);
    }

    /**
     * Interface for scroll events
     */
    public interface ScrollListener {
        /**
         * Called when the element should scroll
         *
         * @param x The distance to scroll on the X axis
         * @param y The distance to scroll on the Y axis
         */
        void onTilt(int x, int y);
    }
}