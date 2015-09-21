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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimerTask;

/**
 * This service samples heart beat and accelerations,
 * writing them periodically to a local log file.
 * Il also keeps track of total number of writings into
 * the log and effective writing frequency for the
 * current run, and provides encapsulation methods to
 * access them
 */

public class SensorSamplingService extends Service {
	private static final String TAG = SensorSamplingService.class.getName();
	private static int SAMPLING_INTERVAL = -1;                          // sampling interval [ms]
	private static int SAMPLING_FREQ;                                   // sampling freq [Hz]
	private static int SAMPLES_PER_HOUR;	                            // samples per hour [Hz * 60 * 60]
	private static final int SENSOR_TYPE_HEARTRATE_GEAR_LIVE = 65562;  	// Samsung Gear Live custom HB sensor

	private volatile float lastHB = 0.0f;                               // last sampled Heart Beat [BPM]
	private volatile float[] lastAcc = new float[] {0.0f, 0.0f, 0.0f};  // last sampled accelerations [m/s^2]

	private static long samples = 0;                                    // number of log writings
	private static float effectiveSamplingFrequency = 0.0f;             // effective log writing frequency

	private SensorManager mSensorManager;
	private Sensor mHeartRateSensor;
	private Sensor mAccelerationSensor;
	private SensorEventListener selHB = null;
	private SensorEventListener selAcc = null;
	private PowerManager powerManager;
	private PowerManager.WakeLock wakeLock;
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
	private static final String LOG_PATH = Environment.getExternalStorageDirectory()
			+ File.separator + "logs";

	private static volatile boolean isRunning = false;
	private static volatile boolean isStarted = false;

    /**
	 * Task to write a single entry of log file, to be used
	 * in conjunction with Timer. Also updates {@code SensorSamplingService.samples}
	 * and {@code SensorSamplingService.effectiveSamplingFrequency}, since they're
	 * related to log writing
	 */

	private class LogWriter extends TimerTask {
		private Writer printer = null;
		private long firstTimestamp = 0;

		private void closeLog() throws IOException {
			printer.close();
			Log.d(TAG, "Log file closed");
		}

		/**
		 * Constructs a new LogWriter, creating logFile and parent
		 * directories if necessary, and writes log header into logFile
		 * @param logFile The file log entries will be written to
		 * @throws IOException If something wrong occur when log file
		 * is created or open, or when writing header line
		 */

		public LogWriter(File logFile) throws IOException {
			logFile.getParentFile().mkdirs();
			logFile.createNewFile();

			printer = new FileWriter(logFile);
			printer.write(LOG_HEADER);

			firstTimestamp = System.currentTimeMillis();
            samples = 0;
		}

		/**
		 * Convenience constructor that uses a string for log file
		 * path rather than a {@code File} object
		 * @param filePath The path of the file log entries will
		 * be written into
		 * @throws IOException If something wrong occur when log
		 * file is created or open, or when writing header line
		 */

		public LogWriter(String filePath) throws IOException {
			this( new File(filePath) );
		}

		/**
		 * {@inheritDoc}. Also, closes log file.
		 * @return {@inheritDoc}
		 */

		@Override
		public boolean cancel() {
			try {

				closeLog();

			} catch (IOException e) {
				e.printStackTrace();
			}
			return super.cancel();
		}

		/**
		 * {@inheritDoc}. Also closes log file
		 * @throws {@inheritDoc}
		 */

		@Override
		public void finalize() throws Throwable {
			closeLog();
			super.finalize();
		}

		/**
		 * Writes a log entry. First writes a line for round number of
		 * hours of sleep when they occur. Then writes the log
		 * entry, using {@code LOG_FORMAT} and current time stamp.
		 * Finally, updates {@code SensorSamplingService.samples}
		 * and {@code SensorSamplingService.effectiveSamplingFrequency}
		 */

		@Override
		public void run() {
			try {

				if ( samples % SAMPLES_PER_HOUR == 0 )
					printer.write( ( samples / SAMPLES_PER_HOUR + 1 ) + " hour of sleep\r\n" );

				long timestamp = System.currentTimeMillis();
				printer.write(String.format(Locale.ENGLISH, LOG_FORMAT,
						timestamp, lastHB,
						lastAcc[0], lastAcc[1], lastAcc[2]));
				effectiveSamplingFrequency = 1000.0f /
						( (float) (timestamp - firstTimestamp) / (float) ++samples );

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private LogWriter logWriter = null;

	public SensorSamplingService() {}

	public static boolean isStarted () { return isStarted; }

	public static long getSamples () { return samples; }

	public static float getRealSamplingFrequency () { return effectiveSamplingFrequency; }

    public static int getSamplingInterval() { return SAMPLING_INTERVAL; }

    public static void setSamplingInterval(int samplingInterval) {
        if (samplingInterval != SAMPLING_INTERVAL) {
            SAMPLING_INTERVAL = samplingInterval;
            SAMPLING_FREQ = 1000 / SAMPLING_INTERVAL;
            SAMPLES_PER_HOUR = SAMPLING_FREQ * 3600;
        }
    }

    @Override
	public IBinder onBind(Intent intent) { return null; } // service is unbounded so no need to bind

	@Override
	public void onCreate ()
	{
		super.onCreate();
		Log.i(TAG, "onCreate()");

		mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

		/*for (Sensor s : mSensorManager.getSensorList(Sensor.TYPE_ALL)) // print all the available sensors
		{
			Log.d(TAG, "Sensor: " + s.getName() + ", type = " + s.getType());
		}*/

		// Use SENSOR_TYPE_HEARTRATE_GEAR_LIVE below to have a better accuracy (only for Samsung Gear Live)
		mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
		selHB = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) { lastHB = sensorEvent.values[0]; }

			@Override
			public void onAccuracyChanged(Sensor sensor, int i) {}
		};
		mSensorManager.registerListener(selHB, this.mHeartRateSensor, 500000); // Forced to 0.5s

		mAccelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		selAcc = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) { lastAcc = sensorEvent.values; }

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

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorSamplingServiceWakeLock");
	}

	@Override
	public int onStartCommand (Intent intent, int flags, int startId)
	{
		Log.i(TAG, "onStartCommand()");

		isStarted = true;
		wakeLock.acquire();

        File logFile = new File(LOG_PATH, "hb_log-" + sdf.format(new Date()) + ".txt");
        Log.d(TAG, "Log saved to : " + logFile.getAbsolutePath());

        try {

            logWriter = new LogWriter(logFile);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // MainActivity should have set SAMPLING_INTERVAL at this point, but just in case...
        if (SAMPLING_INTERVAL == -1)
            setSamplingInterval(getResources().getInteger(R.integer.default_sampling_interval));

        Scheduler.timer.scheduleAtFixedRate(logWriter, SAMPLING_INTERVAL, SAMPLING_INTERVAL);

		Notification notification = new NotificationCompat.Builder(this)
				.setContentTitle("Logger")
				.setContentText("Logger").build();
		startForeground(101, notification);

		isRunning = true;

		return START_STICKY;
	}

	@Override
	public void onDestroy ()
	{
		Log.i(TAG, "onDestroy()");

		isRunning = false;

		logWriter.cancel();
        logWriter = null;

		stopForeground(true);

		if (wakeLock != null)
			if (wakeLock.isHeld())
				wakeLock.release();

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
