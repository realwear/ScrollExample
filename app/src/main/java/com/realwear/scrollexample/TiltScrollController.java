/**
 * RealWear Development Software, Source Code and Object Code
 * (c) RealWear, Inc. All rights reserved.
 * <p>
 * Contact info@realwear.com for further information about the use of this code.
 */

package com.realwear.scrollexample;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Controller for utilizing the accelerometer to add scroll functionality to elements
 */
public class TiltScrollController implements SensorEventListener {
    private final float MinMovementThreshold = 0.02f;
    private final float XScaleFactor = -500f;
    private final float YScaleFactor = 300f;

    private SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;

    private ScrollListener mListener;

    private boolean mInitialized = false;

    private float mLastXValue = 0;
    private float mLastYValue = 0;

    /**
     * Constructor
     *
     * @param sensorManager SensorManager to use to access accelerometer
     * @param listener      Listener to be informed of scroll events
     */
    public TiltScrollController(SensorManager sensorManager, ScrollListener listener) {
        mSensorManager = sensorManager;
        mListener = listener;

        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        final float x = event.values[0];
        final float y = event.values[2];

        if (!mInitialized) {
            mLastXValue = x;
            mLastYValue = y;

            mInitialized = true;
            return;
        }

        float newXOffset = 0;
        float newYOffset = 0;

        final float xDiff = x - mLastXValue;
        final float yDiff = y - mLastYValue;

        if (xDiff < -MinMovementThreshold || xDiff > MinMovementThreshold) {
            newXOffset = xDiff * XScaleFactor;
            mLastXValue = x;
        }

        if (yDiff < -MinMovementThreshold || yDiff > MinMovementThreshold) {
            newYOffset = yDiff * YScaleFactor;
            mLastYValue = y;
        }

        if (Math.abs(newXOffset) > Math.abs(newYOffset)) {
            newYOffset = 0;
        } else {
            newXOffset = 0;
        }

        if (newXOffset != 0 || newYOffset != 0) {
            mListener.onTilt((int) newXOffset, (int) newYOffset);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /**
     * Request access to sensors
     */
    public void requestAllSensors() {
        mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Release the sensors when they are no longer used
     */
    public void releaseAllSensors() {
        mSensorManager.unregisterListener(this, mAccelerometerSensor);
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