package edu.neu.testing;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Database access convenience class
 * 
 * @author eric
 * 
 */
public class DatabaseAdapter {
	public static final String TAG = "DatabaseAdapter";

	public static final String DB_NAME = "sleepdb";
	public static final int DB_VERSION = 1;

	public static final String DB_TABLE_NAME = "nightly_sleep_logs";
	public static final String COL_EPOCHLIST = "epoch_list";

	public static final String DB_CREATE = "CREATE TABLE " + DB_TABLE_NAME
			+ " (" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ COL_EPOCHLIST + " TEXT NOT NULL);";
	public static final String DB_DROP = "DROP TABLE IF EXISTS "
			+ DB_TABLE_NAME + ";";

	private static DatabaseAdapter dbHelper;
	private DbHelper mDbHelper;
	private SQLiteDatabase mDb;
	private Context mCtx;

	public static synchronized DatabaseAdapter getInstance(Context c) {
		if (dbHelper == null) {
			dbHelper = new DatabaseAdapter(c);
		}
		return dbHelper;
	}

	private DatabaseAdapter(Context c) {
		this.mCtx = c;
	}

	public DatabaseAdapter open() throws SQLException {
		mDbHelper = new DbHelper(mCtx, DB_NAME, null, DB_VERSION);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	public long addSleepLog(List<Epoch> epochList) {
		ContentValues values = new ContentValues();
		values.put(COL_EPOCHLIST, DowntimeTools.listEpochToJson(epochList));
		return mDb.insert(DB_TABLE_NAME, null, values);
	}

	public boolean deleteSleepLog(long rowID) {
		return mDb.delete(DB_TABLE_NAME, BaseColumns._ID + "=" + rowID, null) > 0;
	}


	public void clear() {
		mDbHelper.clear(mDb);
	}

	public Cursor fetchAllLogs() {
		return mDb.query(DB_TABLE_NAME, null, null, null, null, null, null);
	}

	public Cursor fetchLogById(Long rowID) {
		return mDb.query(DB_TABLE_NAME, null, BaseColumns._ID + "=?",
				new String[] { rowID.toString() }, null, null, null);
	}

	/**
	 * Internal db creation/upgrade tool
	 * 
	 */
	private static class DbHelper extends SQLiteOpenHelper {
		public DbHelper(Context context, String name, CursorFactory factory,
				int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DB_CREATE);
		}

		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all data.");
			db.execSQL(DB_DROP);
			onCreate(db);
		}

		public void clear(SQLiteDatabase db) {
			db.execSQL(DB_DROP);
			onCreate(db);
		}
	}
}
