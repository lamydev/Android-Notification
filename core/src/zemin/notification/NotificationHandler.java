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

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.util.Log;

/**
 * Parent class of {@link NotificationLocal}, {@link NotificationGlobal} and {@link NotificationRemote}.
 */
public class NotificationHandler extends Handler {

    public static boolean DBG;
    protected String TAG;

    public final int ID;
    protected Context mContext;
    protected NotificationCenter mCenter;
    protected NotificationEffect mEffect;
    protected boolean mEffectEnabled;
    protected boolean mEnabled = true;

    /* package */ NotificationHandler(Context context, int id, Looper looper) {
        super(looper != null ? looper : Looper.myLooper());
        mContext = context;
        ID = id;
        TAG = "zemin." + getClass().getSimpleName() + "@" + id;
        if (DBG) Log.i(TAG, "init.");
    }

    final void setCenter(NotificationCenter center) {
        mCenter = center;
        mEffect = center.effect();
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void enable(boolean enable) {
        if (enable && !mEnabled) {
            if (DBG) Log.d(TAG, "Notification handler=" + ID + " is now enabled.");
            mEnabled = true;
        } else if (!enable && mEnabled) {
            if (DBG) Log.d(TAG, "Notification handler=" + ID + " is now disabled.");
            mEnabled = false;
            cancelAll();
        }
    }

    protected void onArrival(NotificationEntry entry) {
    }

    protected void onCancel(NotificationEntry entry) {
    }

    protected void onCancelAll() {
    }

    public NotificationEffect effect() {
        return mEffect;
    }

    public void enableEffect(boolean enable) {
        if (enable && !mEffectEnabled) {
            if (DBG) Log.d(TAG, "Notification effect is now enabled for handler=" + ID);
            mEffectEnabled = true;
        } else if (enable && !mEffectEnabled) {
            if (DBG) Log.d(TAG, "Notification effect is now disabled for handler=" + ID);
            mEffectEnabled = false;
            if (mEffect.isConsumer(ID)) {
                mEffect.cancel();
            }
        }
    }

    public void cancelEffect(NotificationEntry entry) {
        entry.mEffectConsumers &= ~ID;
    }

    public NotificationEntry getEntry(int entryId) {
        return mCenter.getEntry(entryId);
    }

    public int getNotificationCount() {
        return mCenter.getEntryCount(ID);
    }

    public void send(NotificationEntry entry) {
        if (entry.isSentToTarget(ID)) {
            final int targets = entry.mTargets;
            entry.mTargets = ID;
            updateEntryState(entry);
            entry.mTargets = targets;
        }
    }

    public void cancel(int entryId) {
        NotificationEntry entry = mCenter.getEntry(entryId);
        if (entry != null) {
            cancel(entry);
        }
    }

    public void cancel(NotificationEntry entry) {
        if (entry.isSentToTarget(ID)) {
            final int targets = entry.mTargets;
            entry.mTargets = ID;
            entry.cancel();
            updateEntryState(entry);
            entry.mTargets = targets;
        }
    }

    public void cancelAll() {
        if (DBG) Log.v(TAG, "prepare to cancel all");
        cancelSchedule(ARRIVE);
        schedule(CANCEL_ALL, 0, 0, null, 0);
    }

    void reportCanceled(NotificationEntry entry) {
        onCancelFinished(entry);
        mCenter.cancel(entry);
    }

    void onSendRequested(NotificationEntry entry) {
        if (entry.isSentToTarget(ID)) {
            if (mEnabled) {
                if (DBG) Log.v(TAG, "prepare to send - " + entry.ID);
                entry.mEffectConsumers |= ID;
                schedule(ARRIVE, 0, 0, entry, entry.delay);
            } else {
                onSendIgnored(entry);
            }
        }
    }

    void onCancelRequested(NotificationEntry entry) {
        if (entry.isSentToTarget(ID) && !entry.isCanceled(ID)) {
            if (DBG) Log.v(TAG, "prepare to cancel - " + entry.ID);
            schedule(CANCEL, 0, 0, entry, 0);
        }
    }

    void onSendFinished(NotificationEntry entry) {
        if (DBG) Log.v(TAG, "send - " + entry.ID);
        synchronized (mEffect.mLock) {
            if (mEffectEnabled) {
                if ((entry.mEffectConsumers & ID) != 0) {
                    entry.mEffectConsumers = ID;
                    mEffect.setConsumer(ID);
                    mEffect.play(entry);
                }
            } else {
                entry.mEffectConsumers &= ~ID;
            }
        }
        entry.mFlag |= NotificationEntry.FLAG_SEND_FINISHED;
        updateEntryState(entry);
    }

    void onSendIgnored(NotificationEntry entry) {
        if (DBG) Log.v(TAG, "ignore - " + entry.ID);
        entry.mEffectConsumers &= ~ID;
        entry.mFlag |= NotificationEntry.FLAG_SEND_IGNORED;
        entry.sendToTarget(false, ID);
        updateEntryState(entry);
    }

    void onCancelFinished(NotificationEntry entry) {
        if (DBG) Log.v(TAG, "cancel - " + entry.ID);
        entry.mFlag |= NotificationEntry.FLAG_CANCEL_FINISHED;
        entry.mCancels |= ID;
        updateEntryState(entry);
    }

    void onCancelAllFinished() {
        mCenter.clearEntry(ID);
    }

    private void updateEntryState(NotificationEntry entry) {
        mCenter.updateEntryState(entry);
    }

    protected static final int ARRIVE = 0;
    protected static final int CANCEL = 1;
    protected static final int CANCEL_ALL = 2;

    protected void cancelSchedule(int what) {
        removeMessages(what);
    }

    protected void schedule(int what, int arg1, int arg2, Object obj, int delay) {
        sendMessageDelayed(obtainMessage(what, 0, 0, obj), delay);
    }

    protected void dispatchOnArrival(NotificationEntry entry) {
        updateEntryState(entry);
        onArrival(entry);
        updateEntryState(entry);
    }

    protected void dispatchOnCancel(NotificationEntry entry) {
        updateEntryState(entry);
        onCancel(entry);
        updateEntryState(entry);
    }

    protected void dispatchOnCancelAll() {
        if (mEffect.isConsumer(ID)) {
            mEffect.cancel();
        }
        onCancelAll();
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
        case ARRIVE:
            dispatchOnArrival((NotificationEntry) msg.obj);
            break;
        case CANCEL:
            dispatchOnCancel((NotificationEntry) msg.obj);
            break;
        case CANCEL_ALL:
            dispatchOnCancelAll();
            break;
        }
    }
}
