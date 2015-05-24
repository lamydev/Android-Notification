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

    public static final String KEY_ENTRY_ID = "entry_id";
    public static final String KEY_CLASS = "class_name";
    public static final String KEY_EXTRA = "extra_bundle";
    public static final String KEY_AUTO_CANCEL = "auto_cancel";

    private static int sID = 0;

    private NotificationRemoteCallback mCallback;
    private NotificationManager mManager;
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
    public int genId() {
        return sID++;
    }

    /**
     * Create an PendingIntent to execute when the notification is explicitly dismissed by the user.
     *
     * @see android.app.Notification#setDeleteIntent(PendingIntent)
     *
     * @param entryId
     * @return PendingIntent
     */
    public PendingIntent getDeleteIntent(int entryId) {
        Intent intent = new Intent(ACTION_CANCEL);
        intent.putExtra(KEY_ENTRY_ID, entryId);
        return PendingIntent.getBroadcast(mContext, genId(), intent, 0);
    }

    /**
     * Create an PendingIntent to execute when the notification is clicked by the user.
     *
     * @see android.app.Notification#setContentIntent(PendingIntent)
     *
     * @param entryId
     * @param activityClass
     * @param extra
     * @param autoCancel
     * @return PedningIntent
     */
    public PendingIntent getContentIntent(
        int entryId, Class activityClass, Bundle extra, boolean autoCancel) {

        Intent intent = new Intent(ACTION_CONTENT);
        intent.putExtra(KEY_ENTRY_ID, entryId);
        intent.putExtra(KEY_AUTO_CANCEL, autoCancel);
        if (activityClass != null) {
            intent.putExtra(KEY_CLASS, activityClass.getName());
        }
        if (extra != null) {
            intent.putExtra(KEY_EXTRA, extra);
        }
        return PendingIntent.getBroadcast(mContext, genId(), intent, 0);
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

    private void onCanceledRemotely(NotificationEntry entry) {
        reportCanceled(entry);
    }

    private void startListening() {
        if (!mListening) {
            IntentFilter filter = getFilter();
            filter.addAction(ACTION_CANCEL);
            filter.addAction(ACTION_CONTENT);
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
            final NotificationEntry entry = getEntry(entryId);
            if (DBG) Log.d(TAG, "onReceive - action=" + action + ", entryId=" + entryId);

            if (ACTION_CANCEL.equals(action)) {
                onCanceledRemotely(entry);
            } else if (ACTION_CONTENT.equals(action)) {
                if (intent.getBooleanExtra(KEY_AUTO_CANCEL, true)) {
                    onCanceledRemotely(entry);
                }
            }

            if (mCallback.onReceive(NotificationRemote.this, intent, entry)) {
                return;
            }

            if (ACTION_CONTENT.equals(action)) {
                final String className = intent.getStringExtra(KEY_CLASS);
                final Bundle extra = intent.getBundleExtra(KEY_EXTRA);
                if (className != null) {
                    Intent activityIntent = new Intent();
                    activityIntent.setClassName(mContext.getPackageName(), className);
                    activityIntent.putExtras(extra);
                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(activityIntent);
                }
            }
        }
    }

    @Override public String toSimpleString() { return SIMPLE_NAME; }
    @Override public String toString() { return SIMPLE_NAME; }
}
