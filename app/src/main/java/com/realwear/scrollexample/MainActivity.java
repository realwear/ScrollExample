/**
 * RealWear Development Software, Source Code and Object Code
 * (c) RealWear, Inc. All rights reserved.
 * <p>
 * Contact info@realwear.com for further information about the use of this code.
 */

package com.realwear.scrollexample;

import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ScrollView;

/**
 * Main activity for the application
 * Shows how to scroll elements using the HMT-1 accelerometer
 */
public class MainActivity extends AppCompatActivity implements TiltScrollController.ScrollListener {
    private ScrollView mScrollView;
    private TiltScrollController mTiltScrollController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScrollView = findViewById(R.id.scrollView);


        mTiltScrollController = new TiltScrollController(this,this);
    }

    @Override
    public void onTilt(int x, int y) {
        mScrollView.smoothScrollBy(x, y);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTiltScrollController.releaseAllSensors();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTiltScrollController.requestAllSensors();
    }
}
