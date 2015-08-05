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

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SensorSamplingService extends Service
{
	private static final String TAG = "SensorSamplingService";
	private static final int SAMPLING_INTERVAL = 100;                  // sampling interval [ms]
	private static final int SAMPLING_FREQ = 1000 / SAMPLING_INTERVAL; // sampling freq [Hz]
	private static final int SENSOR_TYPE_HEARTRATE_GEAR_LIVE = 65562;  // Samsung Gear Live custom HB sensor

	private volatile float lastHB = 0;
	private volatile float[] lastAcc = new float[] {0, 0, 0};

	private static int samples = 0;
	private static float realSamplingFrequency = 0;
	private long tsInit;
	private long lastUptime;
	private SensorManager mSensorManager;
	private Sensor mHeartRateSensor;
	private Sensor mAccelerationSensor;
	private SensorEventListener selHB = null;
	private SensorEventListener selAcc = null;
	private PowerManager powerManager;
	private PowerManager.WakeLock wakeLock;
	private BufferedWriter bw = null;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS", Locale.ENGLISH);

	// ADDITIONAL SENSORS, ENABLE IF NEEDED (!) modify the header and format strings accordingly
/*	private Sensor mGyroscopeSensor;
	private Sensor mOrientationSensor;
	private Sensor mStepSensor;
	private volatile float[] lastGyro = new float[] {0, 0, 0};
	private volatile float[] lastOrientation = new float[] {0, 0, 0};
	private volatile float lastStepCount = 0;
	private SensorEventListener selGyroscope = null;
	private SensorEventListener selOrientation = null;
	private SensorEventListener selStep = null;
*/
	private static final String LOG_HEADER = "TS,  BPM, AccX, AccY, AccZ\r\n";
	private static final String LOG_FORMAT = "%d,%3.1f,%3.3f,%3.3f,%3.3f\r\n";

	private static volatile boolean isRunning = false;
	private static volatile boolean isStarted = false;

	private Handler handler;
	private HandlerThread t = new HandlerThread("MyThread");
	private Runnable writeTask = new Runnable()
	{
		@Override
		public void run()
		{
			if (isRunning)
			{
				lastUptime += SAMPLING_INTERVAL;
				handler.postAtTime(writeTask, lastUptime);
			}

			long ts = System.currentTimeMillis();

			if (bw != null)
			{
				try {
					bw.write(String.format(Locale.ENGLISH, LOG_FORMAT,
							ts,
							lastHB,
							lastAcc[0], lastAcc[1], lastAcc[2]));

					samples++;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			realSamplingFrequency = 1000.0f / ((float)(ts - tsInit) / (float)samples);
		}
	};

	public SensorSamplingService()
	{
	}

	public static boolean isStarted ()
	{
		return isStarted;
	}

	public static int getSamples ()
	{
		return samples;
	}

	public static float getRealSamplingFrequency ()
	{
		return realSamplingFrequency;
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null; // service is unbounded so no need to bind
	}

	@Override
	public void onCreate ()
	{
		super.onCreate();
		Log.i(TAG, "onCreate()");

		mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));

		/*for (Sensor s : mSensorManager.getSensorList(Sensor.TYPE_ALL)) // print all the available sensors
		{
			Log.d(TAG, "Sensor: " + s.getName() + ", type = " + s.getType());
		}*/

		// Use SENSOR_TYPE_HEARTRATE_GEAR_LIVE below to have a better accuracy (only for Samsung Gear Live)
		mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
		selHB = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) {
				lastHB = sensorEvent.values[0];
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int i) {}
		};
		mSensorManager.registerListener(selHB, this.mHeartRateSensor, 500000); // Forced to 0.5s

		mAccelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		selAcc = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) {
				lastAcc = sensorEvent.values;
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int i) {}
		};
		mSensorManager.registerListener(selAcc, this.mAccelerationSensor, SAMPLING_INTERVAL * 750); // sampling interval * 0.75

		// ENABLE IF REQUIRED
/*		mGyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		selGyroscope = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) { lastGyro = sensorEvent.values; }

			@Override
			public void onAccuracyChanged(Sensor sensor, int i) {}
		};
		mSensorManager.registerListener(selGyroscope, this.mGyroscopeSensor, SAMPLING_INTERVAL * 1000);

		mOrientationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		selOrientation = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) { lastOrientation = sensorEvent.values; }

			@Override
			public void onAccuracyChanged(Sensor sensor, int i) {}
		};
		mSensorManager.registerListener(selOrientation, this.mOrientationSensor, SAMPLING_INTERVAL * 1000);

		mStepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
		selStep = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) { lastStepCount = sensorEvent.values[0]; }

			@Override
			public void onAccuracyChanged(Sensor sensor, int i) {}
		};
		mSensorManager.registerListener(selStep, this.mStepSensor, 1000000); // Forced to 1s
*/
		File logFile = new File(Environment.getExternalStorageDirectory() + File.separator + "logs" + File.separator + "hb_log-" + sdf.format(new Date()) + ".txt");
		try {
			logFile.getParentFile().mkdirs();
			logFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		try {
			Log.d (TAG, "Log saved to : " + logFile.getAbsolutePath());
			bw = new BufferedWriter(new FileWriter(logFile));
			bw.write(String.format(Locale.ENGLISH, LOG_HEADER));
		} catch (Exception e) {
			e.printStackTrace();
		}

		powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorSamplingServiceWakeLock");
	}

	@Override
	public int onStartCommand (Intent intent, int flags, int startId)
	{
		Log.i(TAG, "onStartCommand()");

		isStarted = true;
		wakeLock.acquire();

		Notification notification = new NotificationCompat.Builder(this)
				.setContentTitle("Logger")
				.setContentText("Logger").build();
		startForeground(101, notification);

		samples = 0;
		isRunning = true;
		t.start();

		handler = new Handler(t.getLooper());
		tsInit = System.currentTimeMillis();
		lastUptime = SystemClock.uptimeMillis() + SAMPLING_INTERVAL;
		handler.postAtTime(writeTask, lastUptime);

		return START_STICKY;
	}

	@Override
	public void onDestroy ()
	{
		Log.i(TAG, "onDestroy()");

		isRunning = false;
		t.quit();

		stopForeground(true);

		if (wakeLock != null)
			if (wakeLock.isHeld())
				wakeLock.release();

		if (bw != null) {
			try {
				bw.close();
				Log.d (TAG, "File closed");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (selHB != null)
			mSensorManager.unregisterListener(selHB);
		if (selAcc != null)
			mSensorManager.unregisterListener(selAcc);
/*		if (selGyroscope != null)
			mSensorManager.unregisterListener(selGyroscope);
		if (selOrientation != null)
			mSensorManager.unregisterListener(selOrientation);
		if (selStep != null)
			mSensorManager.unregisterListener(selStep);
*/
		isStarted = false;

		super.onDestroy();
	}
}
