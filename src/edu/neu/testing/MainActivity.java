package edu.neu.testing;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
	
	public void start(View v) {
		Intent intent = new Intent(this, SleepStatsCollector.class);
		startService(intent);
	}
	
	public void cancel(View v) {
		Intent intent = new Intent(this, SleepStatsCollector.class);
		stopService(intent);
		finish();
	}
	
	public void saveData(View v) {
		Intent intent = new Intent(this, SleepStatsCollector.class);
		intent.putExtra(SleepStatsCollector.SAVE_DATA_BOOL, true);
		startService(intent);
	}
	
	public void outputDbToLog(View v) {
		DatabaseAdapter db = DatabaseAdapter.getInstance(this);
		db.open();
		Cursor c = db.fetchAllLogs();
		c.moveToFirst();
		while (!c.isAfterLast()) {
			Log.d("MainActivity", DowntimeTools.jsonToListEpoch(c.getString(1)).toString());
			c.moveToNext();
		}
	}
	
	public void displayGraph(View v) {
		return;
	}
	
	public void clearData(View v) {
		DatabaseAdapter db = DatabaseAdapter.getInstance(this);
		db.open();
		db.clear();
		db.close();
	}

}
