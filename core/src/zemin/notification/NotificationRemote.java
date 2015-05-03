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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

// handle remote notifications (StatusBar Notification)
//
// @author Zemin Liu
//
public class NotificationRemote extends NotificationHandler {

    public static boolean DBG;

    private NotificationRemoteFactory mFactory;
    private NotificationManager mManager;
    private Receiver mReceiver;
    private IntentFilter mFilter;
    private boolean mListening;

    /* package */ NotificationRemote(Context context, Looper looper) {
        super(context, NotificationEntry.TARGET_REMOTE, looper);

        mManager = (NotificationManager)
            context.getSystemService(Context.NOTIFICATION_SERVICE);

        mReceiver = new Receiver();
        mFilter = new IntentFilter();
    }

    public void setFactory(NotificationRemoteFactory factory) {
        if (mFactory != factory) {
            if (factory != null) {
                factory.init(mContext, this);
            }
            mFactory = factory;
        }
    }

    public void addAction(String action) {
        mFilter.addAction(action);
    }

    public boolean hasAction(String action) {
        return mFilter.hasAction(action);
    }

    private void startListening() {
        mListening = true;
        mFilter.addAction(NotificationRemoteFactory.ACTION_CANCEL);
        mFilter.addAction(NotificationRemoteFactory.ACTION_CONTENT);
        mContext.registerReceiver(mReceiver, mFilter, null, this);
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

        if (mFactory == null) {
            mFactory = new NotificationRemoteFactory();
            mFactory.init(mContext, this);
            startListening();
        }

        if (!mListening) {
            startListening();
        }

        Notification n = mFactory.getStatusBarNotification(entry, entry.contentResId);
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

    private void onCanceledRemotely(int entryId) {
        NotificationEntry entry = getEntry(entryId);
        if (entry != null) {
            onCancelAlready(entry);
        } else {
            Log.e(TAG, "failed to cancel. notification entry not found for id=" + entryId);
        }
    }

    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mFactory.onReceive(intent)) return;

            final String action = intent.getAction();
            final int entryId = intent.getIntExtra(NotificationRemoteFactory.KEY_ENTRY_ID, -1);
            if (DBG) Log.d(TAG, "onReceive - action=" + action + ", entryId=" + entryId);

            if (NotificationRemoteFactory.ACTION_CANCEL.equals(action)) {
                onCanceledRemotely(entryId);
            } else if (NotificationRemoteFactory.ACTION_CONTENT.equals(action)) {
                if (intent.getBooleanExtra(NotificationRemoteFactory.KEY_AUTO_CANCEL, true)) {
                    onCanceledRemotely(entryId);
                }

                final String className = intent.getStringExtra(NotificationRemoteFactory.KEY_CLASS);
                final Bundle bundle = intent.getBundleExtra(NotificationRemoteFactory.KEY_BUNDLE);
                if (className != null) {
                    Intent activityIntent = new Intent();
                    activityIntent.setClassName(mContext.getPackageName(), className);
                    activityIntent.putExtras(bundle);
                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(activityIntent);
                }
            }
        }
    }
}
