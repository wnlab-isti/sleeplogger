/*
 * Copyright (C) 2014-2015 ISTI - CNR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.cnr.isti.doremi.sleeplogger;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class MyActivity extends Activity /*implements SensorEventListener*/
{
    private static final String TAG = MyActivity.class.getName();
    private static final int SAMPLING_INTERVAL = 100;                 // sampling interval [ms]

    private TextView rate = null;
    private TextView accuracy = null;
    private TextView sensorInformation = null;
    private TextView acceleration = null;
    private static final int SENSOR_TYPE_HEARTRATE = 65562;
    private Sensor mHeartRateSensor;
    private Sensor mAccelerationSensor;
    private Sensor mGyroscopeSensor;
    private Sensor mOrientationSensor;
    private Sensor mStepSensor;
    private SensorManager mSensorManager;
    private CountDownLatch latch;
    private FileOutputStream fos = null;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS", Locale.ENGLISH);
    private PendingIntent pintent;
    private AlarmManager alarm;
    private static final int SAMPLING_FREQ = 1000 / SAMPLING_INTERVAL;   // sampling freq [Hz]
    private volatile float lastHB = 0;
    private volatile float lastStepCount = 0;
    private volatile float[] lastAcc = new float[] {0, 0, 0};
    private volatile float[] lastGyro = new float[] {0, 0, 0};
    private volatile float[] lastOrientation = new float[] {0, 0, 0};
    private PowerManager pm;	// Required to keep the service active
    private PowerManager.WakeLock wl;
    private int samples = 0;
    private Thread tLogger = null;
    private SensorEventListener selHB = null;
    private SensorEventListener selAcc = null;
	private SensorEventListener selOrientation = null;
    private SensorEventListener selStep = null;

	private static final String LOG_HEADER = "TS,  BPM, AccX, AccY, AccZ\r\n";
	private static final String LOG_FORMAT = "%d,%3.1f,%3.3f,%3.3f,%3.3f\r\n";

    private class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive (Context context, Intent intent)
        {
            if (fos != null)
            {
                try {
                    fos.write(String.format(Locale.ENGLISH, LOG_FORMAT,
                            System.currentTimeMillis(),
                            lastHB,
                            lastAcc[0], lastAcc[1], lastAcc[2]
                            /*lastOrientation[0], lastOrientation[1], lastOrientation[2]*/).getBytes());
                    //Log.d(TAG, "Sensor data written to file: " + lastOrientation[0] + ", " + lastOrientation[1] + ", " + lastOrientation[2]);

                    samples++;
                    sensorInformation.setText ("Samples # (" + SAMPLING_FREQ + " Hz): " + samples);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    MyBroadcastReceiver br = new MyBroadcastReceiver ();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d (TAG, "onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        latch = new CountDownLatch(1);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                rate = (TextView) stub.findViewById(R.id.rate);
                rate.setText("Reading...");

                //accuracy = (TextView) stub.findViewById(R.id.accuracy);
                sensorInformation = (TextView) stub.findViewById(R.id.sensor);
                //acceleration = (TextView) stub.findViewById(R.id.acceleration);

                latch.countDown();
            }
        });

        mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));

        for (Sensor s : mSensorManager.getSensorList(Sensor.TYPE_ALL))
        {
            Log.d(TAG, "Sensor: " + s.getName() + ", type = " + s.getType());
        }

        mHeartRateSensor = mSensorManager.getDefaultSensor(SENSOR_TYPE_HEARTRATE); // using Sensor Lib2 (Samsung Gear Live)
        //mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        mAccelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mOrientationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mStepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        /*try {
            latch.await();
        } catch (InterruptedException e)
        {
            System.exit (-1);
        }*/

        selHB = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                lastHB = sensorEvent.values[0];
                /*if (rate != null)
                    rate.setText (String.valueOf (lastHB));
                if (accuracy != null)
                    accuracy.setText("Accuracy: " + sensorEvent.accuracy);*/
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        mSensorManager.registerListener(selHB, this.mHeartRateSensor, 1000000);

        selAcc = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                lastAcc = sensorEvent.values;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        mSensorManager.registerListener(selAcc, this.mAccelerationSensor, SAMPLING_INTERVAL * 1000);

        /*mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                lastGyro = sensorEvent.values;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        }, this.mGyroscopeSensor, SAMPLING_INTERVAL * 1000);
		*/

		/*selOrientation = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) { lastOrientation = sensorEvent.values; }

			@Override
			public void onAccuracyChanged(Sensor sensor, int i) {

			}
		};
        mSensorManager.registerListener(selOrientation, this.mOrientationSensor, SAMPLING_INTERVAL * 1000);
*/
		/*
        selStep = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) { lastStepCount = sensorEvent.values[0]; }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        mSensorManager.registerListener(selStep, this.mStepSensor, 1000000);*/

        File logFile = new File(Environment.getExternalStorageDirectory() + File.separator + "logs" + File.separator + "hb_log-" + sdf.format(new Date()) + ".txt");
        try {
            logFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Log.d (TAG, "Log saved to : " + logFile.getAbsolutePath());
            fos = new FileOutputStream(logFile);

			fos.write(String.format(Locale.ENGLISH, LOG_HEADER).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        getApplicationContext ().registerReceiver (br, new IntentFilter("it.cnr.isti.giraff.android.wear.WAKEUP"));

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 1);

        Intent i = new Intent("it.cnr.isti.giraff.android.wear.WAKEUP");
        pintent = PendingIntent.getBroadcast(this.getApplicationContext(), 14, i, 0);

        alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), SAMPLING_INTERVAL, pintent);
        Log.d (TAG, "Periodic wakeup set");

        /*pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (wl == null)
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Logger-Lock");
        if (!wl.isHeld ())
            wl.acquire();*/
    }

    @Override
    protected void onStart() {

		Log.d(TAG, "onStart()");
		super.onStart();

	}

   @Override
    protected void onStop() {
        Log.d (TAG, "onStop()");

        super.onStop();

    }

    @Override
    protected void onDestroy() {
        Log.d (TAG, "onDestroy()");
        super.onDestroy();

        if (fos != null) {
            try {
                fos.close();
                Log.d (TAG, "File closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (selHB != null)
            mSensorManager.unregisterListener(selHB);
        if (selAcc != null)
            mSensorManager.unregisterListener(selAcc);
		if (selOrientation != null)
			mSensorManager.unregisterListener(selOrientation);
        if (selStep != null)
            mSensorManager.unregisterListener(selStep);

        alarm.cancel (pintent);
        getApplicationContext ().unregisterReceiver (br);

        /*if (wl != null)
        {
            wl.release();
            wl = null;
        }*/
    }

    @Override
    protected void onPause() {
        Log.d (TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d (TAG, "onResume()");
        super.onResume();
    }
}
