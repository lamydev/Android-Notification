/*
 * Copyright (C) 2015 Zemin Liu
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

package zemin.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.support.v4.app.NotificationCompat;

/**
 * Callback for {@link NotificationRemote}. This is the default implementation.
 */
public class NotificationRemoteCallback {

    private static final String TAG = "zemin.NotificationRemoteCallback";
    public static boolean DBG;

    /**
     * Called only once after this callback is set.
     *
     * @param remote
     */
    public void onRemoteSetup(NotificationRemote remote) {
    }

    /**
     * Called when receiving a broadcast.
     *
     * @param remote
     * @param intent
     * @param entry
     * @return boolean true, if handled.
     */
    public boolean onReceive(NotificationRemote remote, Intent intent, NotificationEntry entry) {
        return false;
    }

    /**
     * Create a new {@link android.app.Notification} object.
     *
     * @param remote
     * @param entry
     * @param layoutId
     * @return Notification
     */
    public Notification makeStatusBarNotification(NotificationRemote remote, NotificationEntry entry, int layoutId) {

        final int entryId = entry.ID;
        final CharSequence title = entry.title;
        final CharSequence text = entry.text;
        CharSequence tickerText = entry.tickerText;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(remote.getContext());
        if (entry.smallIconRes > 0) {
            builder.setSmallIcon(entry.smallIconRes);
        } else {
            Log.w(TAG, "***************** small icon not set.");
        }

        if (tickerText == null) {
            Log.w(TAG, "***************** tickerText not set.");
            tickerText = title + ": " + text;
        }

        if (entry.largeIconBitmap != null) {
            builder.setLargeIcon(entry.largeIconBitmap);
        }

        builder.setTicker(tickerText);
        builder.setContentTitle(title);
        builder.setContentText(text);
        builder.setShowWhen(entry.showWhen);

        if (entry.showWhen && entry.whenLong > 0) {
            builder.setWhen(entry.whenLong);
        }

        PendingIntent deleteIntent = remote.getDeleteIntent(entryId);
        builder.setDeleteIntent(deleteIntent);

        PendingIntent contentIntent = remote.getContentIntent(
            entryId, entry.activityClass, entry.extra, entry.autoCancel);
        builder.setContentIntent(contentIntent);
        builder.setAutoCancel(entry.autoCancel);

        builder.setOngoing(entry.ongoing);

        if (entry.useSystemEffect) {
            int defaults = 0;
            if (entry.playRingtone) {
                if (entry.ringtoneUri != null) {
                    builder.setSound(entry.ringtoneUri);
                } else {
                    defaults |= Notification.DEFAULT_SOUND;
                }
            }

            if (entry.useVibration) {
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
