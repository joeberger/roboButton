package com.ndipatri.roboButton.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.activities.MainActivity;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.services.MonitoringService;

public class NotificationHelper {

    public static final String TAG = NotificationHelper.class.getCanonicalName();

    private Context context;

    public NotificationHelper(Context context) {
        this.context = context;
    }

    public void clearAllNotifications() {
        NotificationManager notificationManager = (NotificationManager) RBApplication.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    public void clearNotification(String buttonId) {
        NotificationManager notificationManager = (NotificationManager) RBApplication.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(buttonId.hashCode());
    }

    public void sendNotification(String buttonId, String buttonName, ButtonState buttonState) {

        Log.d(TAG, "Sending notification for state '" + buttonState + "'.");

        StringBuilder sbuf = new StringBuilder("Tap here to toggle '");
        sbuf.append(buttonName).append("'.");

        int notifId = buttonId.hashCode();

        NotificationManager notificationManager = (NotificationManager) RBApplication.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);

        // construct the Notification object.
        Notification.Builder builder = new Notification.Builder(context);
        builder.setWhen(System.currentTimeMillis());
        //builder.setContentText(tickerText);
        builder.setSmallIcon(buttonState.smallDrawableResourceId); // this is what shows up in notification bar before you pull it down
        builder.setNumber(1234);
        builder.setAutoCancel(true);
        builder.setOnlyAlertOnce(true);
        ///builder.setContentIntent(pendingIntent);
        builder.setVibrate(new long[]{0,     // start immediately
                200,   // on
                1000,  // off
                200,   // on
                -1});  // no repeat

        // This provides sub-information in the notification. not using right now.
        //String notifyTitle = "Test app for ORMLite";
        //builder.addAction(R.drawable.green_button_small, notifyTitle, pendingIntent);

        Notification notification = builder.build();

        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification_layout);
        contentView.setImageViewResource(R.id.buttonImageView, buttonState.smallDrawableResourceId);
        contentView.setTextViewText(R.id.detailTextView, sbuf.toString());
        notification.contentView = contentView;

        Intent serviceIntent = new Intent(context, MonitoringService.class);
        serviceIntent.putExtra(MonitoringService.SHOULD_TOGGLE_FLAG, true);
        serviceIntent.putExtra(MonitoringService.BUTTON_ID, buttonId);
        PendingIntent togglePendingIntent = PendingIntent.getService(context, 0, serviceIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent activityIntent = new Intent(context, MainActivity.class);
        PendingIntent launchPendingIntent = PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        contentView.setOnClickPendingIntent(R.id.detailTextView, togglePendingIntent);
        contentView.setOnClickPendingIntent(R.id.buttonImageView, launchPendingIntent);

        notificationManager.notify(notifId, notification);
    }
}
