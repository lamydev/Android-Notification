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

import java.util.ArrayList;

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
        if (DBG) Log.v(TAG, "onRemoteSetup");
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
        if (DBG) Log.v(TAG, "makeStatusBarNotification - " + entry.ID);

        final int entryId = entry.ID;
        final CharSequence title = entry.title;
        final CharSequence text = entry.text;
        CharSequence tickerText = entry.tickerText;

        NotificationCompat.Builder builder = remote.getStatusBarNotificationBuilder();
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

        if (entry.progressMax != 0 || entry.progressIndeterminate) {
            builder.setProgress(entry.progressMax, entry.progress, entry.progressIndeterminate);
        }

        builder.setAutoCancel(entry.autoCancel);
        builder.setOngoing(entry.ongoing);
        builder.setDeleteIntent(remote.getDeleteIntent(entry));
        builder.setContentIntent(remote.getContentIntent(entry));

        if (entry.hasActions()) {
            ArrayList<NotificationEntry.Action> actions = entry.getActions();
            for (NotificationEntry.Action act : actions) {
                builder.addAction(act.icon, act.title, remote.getActionIntent(entry, act));
            }
        }

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

    /**
     * Called when notification is clicked.
     *
     * @param remote
     * @param entry
     */
    public void onClickRemote(NotificationRemote remote, NotificationEntry entry) {
        if (DBG) Log.v(TAG, "onClickRemote - " + entry.ID);
    }

    /**
     * Called when notification is canceled.
     *
     * @param remote
     * @param entry
     */
    public void onCancelRemote(NotificationRemote remote, NotificationEntry entry) {
        if (DBG) Log.v(TAG, "onCancelRemote - " + entry.ID);
    }

    /**
     * Called when notification action view is clicked.
     *
     * @param remote
     * @param entry
     * @param act
     */
    public void onClickRemoteAction(NotificationRemote remote, NotificationEntry entry,
                                    NotificationEntry.Action act) {
        if (DBG) Log.v(TAG, "onClickRemoteAction - " + entry.ID + ", " + act);
    }

    /**
     * Called when receiving a broadcast.
     *
     * @param remote
     * @param entry
     * @param intent
     * @param intentAction
     */
    public void onReceive(NotificationRemote remote, NotificationEntry entry,
                          Intent intent, String intentAction) {
        if (DBG) Log.d(TAG, "onReceive - " + entry.ID + ", " + intentAction);
    }
}
