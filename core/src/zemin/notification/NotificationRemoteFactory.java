package zemin.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.support.v4.app.NotificationCompat;

// NotificationRemote factory
//
// @author Zemin Liu
//
public class NotificationRemoteFactory {

    public static boolean DBG;
    private static final String TAG = "zemin.NotificationRemoteFactory";

    public static final String ACTION_CANCEL =
        "notification.intent.action.notificationremote.cancel";

    public static final String ACTION_CONTENT =
        "notification.intent.action.notificationremote.content";

    public static final String KEY_ENTRY_ID = "entry_id";
    public static final String KEY_CLASS = "class_name";
    public static final String KEY_BUNDLE = "extra_bundle";
    public static final String KEY_AUTO_CANCEL = "auto_cancel";

    protected Context mContext;
    protected NotificationRemote mRemote;

    private static int sID = 0;

    public int genId() { return sID++; }

    final void init(Context context, NotificationRemote remote) {
        mContext = context; mRemote = remote;
    }

    public NotificationRemoteFactory() {}

    public void addAction(String action) {
        mRemote.addAction(action);
    }

    public boolean onReceive(Intent intent) {
        return false;
    }

    public PendingIntent getDeleteIntent(int entryId) {
        Intent intent = new Intent(ACTION_CANCEL);
        intent.putExtra(KEY_ENTRY_ID, entryId);
        return PendingIntent.getBroadcast(mContext, genId(), intent, 0);
    }

    public PendingIntent getContentIntent(
        int entryId, Class activityClass, Bundle bundle, boolean autoCancel) {

        Intent intent = new Intent(ACTION_CONTENT);
        intent.putExtra(KEY_ENTRY_ID, entryId);
        intent.putExtra(KEY_AUTO_CANCEL, autoCancel);
        if (activityClass != null) {
            intent.putExtra(KEY_CLASS, activityClass.getName());
        }
        if (bundle != null) {
            intent.putExtra(KEY_BUNDLE, bundle);
        }
        return PendingIntent.getBroadcast(mContext, genId(), intent, 0);
    }

    /**
     * default implementation (subclass can override this function to support customized layout)
     */
    public Notification getStatusBarNotification(NotificationEntry entry, int contentResId) {

        final int entryId = entry.ID;
        final CharSequence title = entry.title;
        final CharSequence text = entry.text;
        final CharSequence ticker = title + ": " + text;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        if (entry.smallIconRes > 0) {
            builder.setSmallIcon(entry.smallIconRes);
        } else {
            Log.w(TAG, "small icon not set.");
        }

        if (entry.largeIconBitmap != null) {
            builder.setLargeIcon(entry.largeIconBitmap);
        }

        builder.setTicker(ticker);
        builder.setContentTitle(title);
        builder.setContentText(text);
        builder.setShowWhen(entry.showWhen);

        if (entry.showWhen && entry.whenLong > 0) {
            builder.setWhen(entry.whenLong);
        }

        PendingIntent deleteIntent = getDeleteIntent(entryId);
        builder.setDeleteIntent(deleteIntent);

        PendingIntent contentIntent = getContentIntent(
            entryId, entry.activityClass, entry.extra, entry.autoCancel);
        builder.setContentIntent(contentIntent);
        builder.setAutoCancel(entry.autoCancel);

        if (entry.useSystemEffect) {
            int defaults = 0;
            if (entry.useRingtone) {
                if (entry.ringtoneUri != null) {
                    builder.setSound(entry.ringtoneUri);
                } else {
                    defaults |= Notification.DEFAULT_SOUND;
                }
            }

            if (entry.useVibrate) {
                if (entry.vibratePattern != null) {
                    builder.setVibrate(entry.vibratePattern);
                } else {
                    defaults |= Notification.DEFAULT_VIBRATE;
                }
            }

            if (defaults != 0) {
                builder.setDefaults(defaults);
            }
        }

        return builder.build();
    }
}
