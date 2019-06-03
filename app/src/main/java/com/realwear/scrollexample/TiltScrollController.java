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
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import java.util.Objects;

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

    private SensorManager mSensorManager;
    private Sensor mRotationSensor;
    private ScrollListener mListener;


    private boolean mInitialized = false;

    private int mLastAccuracy;
    private WindowManager mWindowManager;
    private float mOldZ;
    private float mOldX;
    private final float[] mRotationMatrix = new float[9];
    private final float[] mAdjustedRotationMatrix = new float[9];
    private final float[] mOrientation = new float[3];
    private boolean mFirstRun = true;


    public TiltScrollController(Context ctx, ScrollListener scrollListener) {
        mSensorManager = ctx.getSystemService(SensorManager.class);

        mListener = scrollListener;

        // Can be null if the sensor hardware is not available
        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mWindowManager = ctx.getSystemService(WindowManager.class);
        mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY_MICROS);
        mFirstRun = true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mLastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }
        if (event.sensor == mRotationSensor) {
            updateOrientation(event.values.clone());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (mLastAccuracy != accuracy) {
            mLastAccuracy = accuracy;
        }
    }

    /**
     * Calculate mOrientation
     */
    private void updateOrientation(float[] rotationVector) {

        // Get rotation's based on vector locations
        getRotationMatrixFromVector(mRotationMatrix, rotationVector);

        final int worldAxisForDeviceAxisX;
        final int worldAxisForDeviceAxisY;

        // Remap the axes as if the device screen was the instrument panel,
        // and adjust the rotation matrix for the device orientation.
        switch (mWindowManager.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
            default:
                worldAxisForDeviceAxisX = SensorManager.AXIS_X;
                worldAxisForDeviceAxisY = SensorManager.AXIS_Z;
                break;
            case Surface.ROTATION_90:
                worldAxisForDeviceAxisX = SensorManager.AXIS_Z;
                //noinspection SuspiciousNameCombination
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X;
                break;
            case Surface.ROTATION_180:
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X;
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Z;
                break;
            case Surface.ROTATION_270:
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Z;
                //noinspection SuspiciousNameCombination
                worldAxisForDeviceAxisY = SensorManager.AXIS_X;
                break;
        }

        remapCoordinateSystem(mRotationMatrix, worldAxisForDeviceAxisX,
                worldAxisForDeviceAxisY, mAdjustedRotationMatrix);

        // Transform rotation matrix into azimuth/pitch/roll
        getOrientation(mAdjustedRotationMatrix, mOrientation);

        // Convert radians to degrees and flat
        float newX = (float) Math.toDegrees(mOrientation[1]);
        float newZ = (float) Math.toDegrees(mOrientation[0]);

        // How many degrees has the users head rotated since last time.
        float deltaX = applyThreshold(angularRounding(newX - mOldX));
        float deltaZ = applyThreshold(angularRounding(newZ - mOldZ));

        //Ignore first head position in order to find base line
        if (mFirstRun) {
            deltaX = 0;
            deltaZ = 0;
            mFirstRun = false;
        }

        mOldX = newX;
        mOldZ = newZ;

        Log.d("K",deltaX + "/" + deltaZ);

        mListener.onTilt((int) deltaZ * 60, (int) deltaX * 60);

    }

    private float applyThreshold(float input) {
        // Apply a minimum value to the input. If input is below the threshold, return zero to remove noise.
        return abs(input) > THRESHOLD_MOTION ? input : 0;
    }

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