package edu.neu.testing;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

public class DowntimeNotificationManager {
	public static final String TAG = "DowntimeNotificationManager";

	public static final int ALARM_SET_NOTIFICATION = 10;
	public static final int ALARM_RINGING_NOTIFICATION = 11;

	public static Notification createSetNotification(Context ctx) {
		Intent i = new Intent(ctx, MainActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent notifyIntent = PendingIntent.getActivity(ctx,
				ALARM_SET_NOTIFICATION, i, PendingIntent.FLAG_CANCEL_CURRENT);
		Notification noti = new NotificationCompat.Builder(ctx)
				.setContentTitle("SensorTesting collecting")
				.setContentText("Click to go to Downtime")
				.setSmallIcon(R.drawable.a_justice)
				.setContentIntent(notifyIntent).build();
		noti.flags |= Notification.FLAG_ONGOING_EVENT;
		return noti;
	}
}
