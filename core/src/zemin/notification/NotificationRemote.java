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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import android.support.v4.app.NotificationCompat;

/**
 * Status-bar notification.
 */
public class NotificationRemote extends NotificationHandler {

    public static final String SIMPLE_NAME = "Remote";
    public static boolean DBG;

    public static final String ACTION_CANCEL =
        "notification.intent.action.notificationremote.cancel";

    public static final String ACTION_CONTENT =
        "notification.intent.action.notificationremote.content";

    public static final String ACTION_ACTION =
        "notification.intent.action.notificationremote.action";

    public static final String KEY_ENTRY_ID = "key_entry_id";
    public static final String KEY_ACTION_ID = "key_action_id";

    private static int sID = 0;

    private NotificationRemoteCallback mCallback;
    private NotificationManager mManager;
    private NotificationCompat.Builder mBuilder;
    private Receiver mReceiver;
    private IntentFilter mFilter;
    private boolean mListening;

    /* package */ NotificationRemote(Context context, Looper looper) {
        super(context, NotificationDelegater.REMOTE, looper);

        mManager = (NotificationManager)
            context.getSystemService(Context.NOTIFICATION_SERVICE);

        mReceiver = new Receiver();
    }

    /**
     * Set callback.
     *
     * @param cb
     */
    public void setCallback(NotificationRemoteCallback cb) {
        if (mCallback != cb) {
            stopListening();
            mCallback = cb;
        }
    }

    /**
     * Add a new Intent action for BroadcastReceiver {@link #Receiver}.
     *
     * @param action
     */
    public void addAction(String action) {
        getFilter().addAction(action);
    }

    /**
     * Whether the given action is included in the filter.
     *
     * @param action
     */
    public boolean hasAction(String action) {
        return getFilter().hasAction(action);
    }

    /**
     * Generate Id for {@link android.app.PendingIntent}.
     *
     * @return int
     */
    public int genIdForPendingIntent() {
        return sID++;
    }

    /**
     * Get builder for {@link android.app.Notification}.
     *
     * @return NotificationCompat#Builder
     */
    public NotificationCompat.Builder getStatusBarNotificationBuilder() {
        if (mBuilder == null) {
            mBuilder = new NotificationCompat.Builder(mContext);
        }
        return mBuilder;
    }

    /**
     * Create an PendingIntent to execute when the notification is explicitly dismissed by the user.
     *
     * @see android.app.Notification#setDeleteIntent(PendingIntent)
     *
     * @param entry
     * @return PendingIntent
     */
    public PendingIntent getDeleteIntent(NotificationEntry entry) {
        Intent intent = new Intent(ACTION_CANCEL);
        intent.putExtra(KEY_ENTRY_ID, entry.ID);
        return PendingIntent.getBroadcast(mContext, genIdForPendingIntent(), intent, 0);
    }

    /**
     * Create an PendingIntent to execute when the notification is clicked by the user.
     *
     * @see android.app.Notification#setContentIntent(PendingIntent)
     *
     * @param entry
     * @return PedningIntent
     */
    public PendingIntent getContentIntent(NotificationEntry entry) {
        Intent intent = new Intent(ACTION_CONTENT);
        intent.putExtra(KEY_ENTRY_ID, entry.ID);
        return PendingIntent.getBroadcast(mContext, genIdForPendingIntent(), intent, 0);
    }

    /**
     * Create an PendingIntent to be fired when the notification action is invoked.
     *
     * @see android.app.Notification#addAction(int, CharSequence, PendingIntent)
     *
     * @param entry
     * @param act
     * @return PendingIntent
     */
    public PendingIntent getActionIntent(NotificationEntry entry, NotificationEntry.Action act) {
        Intent intent = new Intent(ACTION_ACTION);
        intent.putExtra(KEY_ENTRY_ID, entry.ID);
        intent.putExtra(KEY_ACTION_ID, entry.mActions.indexOf(act));
        return PendingIntent.getBroadcast(mContext, genIdForPendingIntent(), intent, 0);
    }

    @Override
    protected void onCancel(NotificationEntry entry) {
        mManager.cancel(entry.ID);
        onCancelFinished(entry);
    }

    @Override
    protected void onCancelAll() {
        mManager.cancelAll();
        onCancelAllFinished();
    }

    @Override
    protected void onArrival(NotificationEntry entry) {

        if (mCallback == null) {
            if (DBG) Log.v(TAG, "set default NotificationRemoteCallback");
            mCallback = new NotificationRemoteCallback();
        }

        if (!mListening) {
            mCallback.onRemoteSetup(this);
            startListening();
        }

        Notification n = mCallback.makeStatusBarNotification(this, entry, entry.layoutId);
        if (n == null) {
            Log.e(TAG, "failed to send remote notification. {null Notification}");
            onSendIgnored(entry);
            return;
        }

        mManager.notify(entry.tag, entry.ID, n);

        if (entry.useSystemEffect) {
            cancelEffect(entry);
        }
        onSendFinished(entry);
    }

    @Override
    protected void onUpdate(NotificationEntry entry) {
        onArrival(entry);
    }

    private void onCanceledRemotely(NotificationEntry entry) {
        reportCanceled(entry);
    }

    private void startListening() {
        if (!mListening) {
            IntentFilter filter = getFilter();
            filter.addAction(ACTION_CANCEL);
            filter.addAction(ACTION_CONTENT);
            filter.addAction(ACTION_ACTION);
            mContext.registerReceiver(mReceiver, filter, null, this);
            mListening = true;
        }
    }

    private void stopListening() {
        if (mListening) {
            mListening = false;
            mFilter = null;
            mContext.unregisterReceiver(mReceiver);
        }
    }

    private IntentFilter getFilter() {
        if (mFilter == null) {
            mFilter = new IntentFilter();
        }
        return mFilter;
    }

    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            final int entryId = intent.getIntExtra(KEY_ENTRY_ID, -1);
            final NotificationEntry entry = getNotification(entryId);
            if (DBG) Log.d(TAG, "onReceive - " + entryId + ", " + action);

            if (ACTION_CANCEL.equals(action)) {
                onCanceledRemotely(entry);
                mCallback.onCancelRemote(NotificationRemote.this, entry);
            } else if (ACTION_CONTENT.equals(action)) {
                entry.executeContentAction(mContext);
                mCallback.onClickRemote(NotificationRemote.this, entry);
                if (entry.autoCancel) {
                    onCanceledRemotely(entry);
                }
            } else if (ACTION_ACTION.equals(action)) {
                final int actionId = intent.getIntExtra(KEY_ACTION_ID, -1);
                final NotificationEntry.Action act = entry.mActions.get(actionId);
                mCallback.onClickRemoteAction(NotificationRemote.this, entry, act);
                act.execute(mContext);
            } else {
                mCallback.onReceive(NotificationRemote.this, entry, intent, action);
            }
        }
    }

    @Override public String toSimpleString() { return SIMPLE_NAME; }
    @Override public String toString() { return SIMPLE_NAME; }
}
