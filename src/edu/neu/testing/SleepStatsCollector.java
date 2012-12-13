package edu.neu.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class SleepStatsCollector extends Service implements SensorEventListener {
	public static final float ACTIVITY_LEVEL_DIVISION_RATIO = 1.0f;
	/**
	 * Interval length in milliseconds
	 */
	public static final int INTERVAL_LENGTH_SECS = 60;
	public static final int INTERVAL_LENGTH = INTERVAL_LENGTH_SECS * 1000;

	public static final String SAVE_DATA_BOOL = "saveData";

	private final IBinder mBinder = new SleepBinder();
	private SensorManager mSensorManager;
	private Sensor mAccel;
	private long currentTime;
	private float[] currentAccel;
	private double[][] oldepoch;
	private double[][] epoch = new double[INTERVAL_LENGTH_SECS * 10][3];
	private List<Epoch> epochList = new ArrayList<Epoch>();
	private int i = 0;
	private Long start;
	private Long end;
	private boolean isListenerRegistered = false;

	// Adaptive filter
	public static final float FILTER_CONSTANT = 0.9174f;
	public static final float K_ACCELEROMETER_MIN_STEP = 0.033f;
	public static final float K_ACCELEROMETER_NOISE_ATTENUATION = 3f;
	float alpha = FILTER_CONSTANT;
	float tempFloat, accelX, accelY, accelZ;
	float lastAccel[] = new float[3];
	float accelFilter[] = new float[3];

	@Override
	public void onCreate() {
		super.onCreate();
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}

	// This is the old onStart method that will be called on the pre-2.0
	// platform. On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
		registerListener();
		startForeground(42,
				DowntimeNotificationManager.createSetNotification(this));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		registerListener();
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		startForeground(42,
				DowntimeNotificationManager.createSetNotification(this));
		if (intent.getBooleanExtra(SAVE_DATA_BOOL, false)) {
			DatabaseAdapter db = DatabaseAdapter.getInstance(this);
			db.open();
			db.addSleepLog(epochList);
			db.close();
			stopSelf();
		}
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		registerListener();
		return mBinder;
	}

	private void registerListener() {
		if (!isListenerRegistered) {
			mSensorManager.registerListener(this, mAccel,
					SensorManager.SENSOR_DELAY_NORMAL);
			isListenerRegistered = true;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mSensorManager.unregisterListener(this);
	}

	public class SleepBinder extends Binder {
		SleepStatsCollector getService() {
			return SleepStatsCollector.this;
		}
	}

	// Client methods
	/**
	 * Fetch x value
	 */
	double getHour() {
		return millToHour(currentTime);
	}

	/**
	 * Fetch y value
	 */
	double getData() {
		return Math.abs(currentAccel[0]) + Math.abs(currentAccel[1])
				+ Math.abs(currentAccel[2]) - 9.81;
	}

	/**
	 * Process the data provided by the sensor
	 */
	void processData(final long timestamp, float x, float y, float z) {
		if (start == null) {
			start = timestamp;
			end = timestamp + INTERVAL_LENGTH;
		} else if (timestamp >= end) {
			oldepoch = epoch;
			// Add epoch to log
			Handler h = new Handler();
			h.post(new Runnable() {
				public void run() {
					double activityLevel = calculateActivityLevel(oldepoch);
					epochList.add(new Epoch(timestamp, activityLevel));
					Log.d("SleepStatsCollector", epochList.toString());
				}
			});
			// Clear the array
			epoch = new double[INTERVAL_LENGTH_SECS * 10][3];
			start = null; // TODO: add epoch for current values
			i = 0;
		} else {
			if (i < INTERVAL_LENGTH_SECS * 10 - 1) {
				epoch[i++] = new double[] { x, y, z };
			}
		}
	}

	protected double calculateActivityLevel(double[][] epoch2) {
		RealMatrix matrix = MatrixUtils.createRealMatrix(epoch2);
		double[] xcol = matrix.getColumn(0);
		double[] ycol = matrix.getColumn(1);
		double[] zcol = matrix.getColumn(2);

		DescriptiveStatistics xstat = new DescriptiveStatistics(xcol);
		DescriptiveStatistics ystat = new DescriptiveStatistics(ycol);
		DescriptiveStatistics zstat = new DescriptiveStatistics(zcol);

		double xstdev = xstat.getStandardDeviation();
		double ystdev = ystat.getStandardDeviation();
		double zstdev = zstat.getStandardDeviation();

		double totalstdev = Math.sqrt(Math.pow(xstdev, 2) + Math.pow(ystdev, 2)
				+ Math.pow(zstdev, 2));

		return totalstdev / ACTIVITY_LEVEL_DIVISION_RATIO;
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Don't care if accuracy changes
	}

	public static float norm(float x, float y, float z) {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}

	public static float clamp(float v, float min, float max) {
		if (v > max)
			return max;
		if (v < min)
			return min;
		return v;
	}
	
	// Implements a high-pass filter
	public void onSensorChanged(SensorEvent event) {
		accelX = event.values[0];
		accelY = event.values[1];
		accelZ = event.values[2];

		tempFloat = clamp(
				Math.abs(norm(accelFilter[0], accelFilter[1], accelFilter[2])
						- norm(accelX, accelY, accelZ))
						/ K_ACCELEROMETER_MIN_STEP - 1.0f, 0.0f, 1.0f);
		alpha = tempFloat * FILTER_CONSTANT / K_ACCELEROMETER_NOISE_ATTENUATION
				+ (1.0f - tempFloat) * FILTER_CONSTANT;
		
		accelFilter[0] = (float) (alpha * (accelFilter[0] + accelX - lastAccel[0]));
	    accelFilter[1] = (float) (alpha * (accelFilter[1] + accelY - lastAccel[1]));
	    accelFilter[2] = (float) (alpha * (accelFilter[2] + accelZ - lastAccel[2]));
	    
	    lastAccel[0] = accelX;
	    lastAccel[1] = accelY;
	    lastAccel[2] = accelZ;
	    onFilteredAccelerometerChanged(accelFilter[0], accelFilter[1], accelFilter[2]);
	}

	public void onFilteredAccelerometerChanged(float x, float y, float z) {
		currentTime = System.currentTimeMillis();
		currentAccel = new float[] { x, y, z };
		processData(currentTime, x, y, z);
	}

	public double millToHour(long time) {
		int offset = TimeZone.getDefault().getOffset(time);
		return ((time + offset) / (1000.0 * 60.0 * 60.0)) % 12;
	}

	static public boolean isServiceRunning(Context c) {
		ActivityManager manager = (ActivityManager) c
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (SleepStatsCollector.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
