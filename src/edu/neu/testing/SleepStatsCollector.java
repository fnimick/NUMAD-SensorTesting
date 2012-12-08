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
	public static final float ACTIVITY_LEVEL_DIVISION_RATIO = 5.0f;
	/**
	 * Interval length in milliseconds
	 */
	public static final int INTERVAL_LENGTH_SECS = 60;
	public static final int INTERVAL_LENGTH = INTERVAL_LENGTH_SECS * 1000;
	
	public static final String SAVE_DATA_BOOL = "saveData";

	private final IBinder mBinder = new SleepBinder();
	private SensorManager mSensorManager;
	private Sensor mAccel;
	private long x;
	private float[] y;
	private double[][] oldepoch;
	private double[][] epoch = new double[INTERVAL_LENGTH_SECS*10][3];
	private List<Epoch> epochList = new ArrayList<Epoch>();
	private int i = 0;
	private Long start;
	private Long end;
	private boolean isListenerRegistered = false;

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
		startForeground(42, DowntimeNotificationManager.createSetNotification(this));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		registerListener();
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		startForeground(42, DowntimeNotificationManager.createSetNotification(this));
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
		return millToHour(x);
	}

	/**
	 * Fetch y value
	 */
	double getData() {
		return Math.abs(y[0]) + Math.abs(y[1]) + Math.abs(y[2]) - 9.81;
	}

	/**
	 * Process the data provided by the sensor
	 */
	void processData(final long timestamp, float[] values) {
		float x, y, z;
		x = values[0];
		y = values[1];
		z = values[2];

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
			epoch = new double[INTERVAL_LENGTH_SECS*10][3];
			start = null; //TODO: add epoch for current values
			i = 0;
		} else {
			if (i < INTERVAL_LENGTH_SECS*10-1) {
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

	public void onSensorChanged(SensorEvent event) {
		// Process sensor event
		x = System.currentTimeMillis();
		y = event.values;
		processData(x, event.values);
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
