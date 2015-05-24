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
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.support.v4.util.ArrayMap;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Notification center.
 */
public class NotificationCenter {

    private static final String TAG = "zemin.NotificationCenter";
    public static boolean DBG;

    private final ArrayMap<Integer, NotificationEntry> mEntries =
        new ArrayMap<Integer, NotificationEntry>();

    private final ArrayList<NotificationListener> mListeners =
        new ArrayList<NotificationListener>();

    private final ArrayList<NotificationHandler> mHandlers =
        new ArrayList<NotificationHandler>();

    private Context mContext;
    private NotificationEffect mEffect;

    /* package */ NotificationCenter(Context context) {
        mContext = context;
        mH = new H(this);
        mEffect = new NotificationEffect(context);
    }

    Context getContext() { return mContext; }
    NotificationEffect effect() { return mEffect; }

    void register(NotificationHandler h) {
        if (!mHandlers.contains(h)) { h.setCenter(this); mHandlers.add(h); }
    }

    NotificationHandler get(int target) {
        for (NotificationHandler h : mHandlers)
            if (h.ID == target) return h;
        return null;
    }

    void addListener(NotificationListener listener) {
        if (!mListeners.contains(listener)) mListeners.add(listener);
    }

    void removeListener(NotificationListener listener) {
        if (mListeners.contains(listener)) mListeners.remove(listener);
    }

    void send(NotificationEntry entry) {
        updateEntryState(entry);
    }

    void cancel(int entryId) {
        NotificationEntry entry = getEntry(entryId);
        if (entry != null) {
            cancel(entry);
        } else {
            Log.e(TAG, "failed to get NotificationEntry for id=" + entryId);
        }
    }

    void cancel(NotificationEntry entry) {
        entry.cancel();
        updateEntryState(entry);
    }

    void cancelAll() {
        for (NotificationHandler h : mHandlers)
            h.cancelAll();
        clearEntry(0);
    }

    boolean hasEntries() {
        return !mEntries.isEmpty();
    }

    boolean hasEntry(Integer id) {
        return mEntries.containsKey(id);
    }

    ArrayList<NotificationEntry> getEntries() {
        return new ArrayList<NotificationEntry>(mEntries.values());
    }

    NotificationEntry getEntry(Integer id) {
        return mEntries.get(id);
    }

    int getEntryCount() {
        return mEntries.size();
    }

    int getEntryCount(int target) {
        synchronized (mEntries) {
            int count = 0;
            Collection<NotificationEntry> entries = mEntries.values();
            for (NotificationEntry entry : entries)
                if (entry.isSentToTarget(target))
                    count++;
            return count;
        }
    }

    private void onSendRequested(NotificationEntry entry) {
        for (NotificationHandler h : mHandlers)
            h.onSendRequested(entry);
    }

    private void onCancelRequested(NotificationEntry entry) {
        for (NotificationHandler h : mHandlers)
            h.onCancelRequested(entry);
    }

    private void onSendAsDefault(NotificationEntry entry) {
        // playEffect(entry);
        addEntry(entry.ID, entry);
    }

    private void playEffect(NotificationEntry entry) {
        synchronized (mEffect.mLock) {
            mEffect.play(entry);
        }
    }

    private void addEntry(Integer id, NotificationEntry entry) {
        synchronized (mEntries) {
            if (!mEntries.containsKey(id)) {
                if (DBG) Log.v(TAG, "[entry:" + id + "] in - " + entry);
                mEntries.put(id, entry);
                if (entry.mSendToListener) {
                    schedule(MSG_ARRIVAL, 0, 0, entry, 0);
                }
            }
        }
    }

    private void removeEntry(Integer id) {
        synchronized (mEntries) {
            if (mEntries.containsKey(id)) {
                NotificationEntry entry = mEntries.remove(id);
                if (DBG) Log.v(TAG, "[entry:" + id + "] out - " + entry);
                if (entry.mSendToListener) {
                    schedule(MSG_CANCEL, 0, 0, entry, 0);
                }
            }
        }
    }

    /* package */ void clearEntry(int target) {
        synchronized (mEntries) {
            Collection<NotificationEntry> entries = mEntries.values();
            Iterator<NotificationEntry> iter = entries.iterator();
            while (iter.hasNext()) {
                NotificationEntry entry = iter.next();
                if (entry.mTargets == target) {
                    if (DBG) Log.v(TAG, "[entry:" + entry.ID + "] out - " + entry);
                    iter.remove();
                    if (entry.mSendToListener) {
                        schedule(MSG_CANCEL, 0, 0, entry, 0);
                    }
                } else {
                    entry.mTargets &= ~target;
                }
            }
        }
    }

    /* package */ void updateEntryState(NotificationEntry entry) {
        synchronized (entry.mLock) {
            final int id = entry.ID;
            if (entry.priority == null) {
                entry.priority = NotificationEntry.DEFAULT_PRIORITY;
            }
            if (entry.mFlag != entry.mPrevFlag) {
                final int diff = entry.mPrevFlag ^ entry.mFlag & entry.mFlag;

                if (DBG) {
                    Log.d(TAG, "updateEntryState: entryId=" + entry.ID +
                          ", flag=" + entry.mFlag + ", prev=" + entry.mPrevFlag +
                          ", diff=" + diff);
                }

                if ((diff & NotificationEntry.FLAG_REQUEST_SEND) != 0) {
                    if (entry.mTargets == 0) {
                        onSendAsDefault(entry);
                    } else {
                        onSendRequested(entry);
                    }
                } else if ((diff & NotificationEntry.FLAG_REQUEST_CANCEL) != 0) {
                    if (entry.mTargets == entry.mCancels) {
                        removeEntry(entry.ID);
                    } else {
                        onCancelRequested(entry);
                    }
                } else if ((diff & NotificationEntry.FLAG_SEND_FINISHED) != 0) {
                    entry.mFlag &= ~NotificationEntry.FLAG_SEND_FINISHED;
                    addEntry(entry.ID, entry);
                } else if ((diff & NotificationEntry.FLAG_SEND_IGNORED) != 0) {
                    entry.mFlag &= ~NotificationEntry.FLAG_SEND_IGNORED;
                    if (entry.mTargets == 0) {
                        onSendAsDefault(entry);
                    }
                } else if ((diff & NotificationEntry.FLAG_CANCEL_FINISHED) != 0) {
                    entry.mFlag &= ~NotificationEntry.FLAG_CANCEL_FINISHED;
                    if (entry.mTargets == entry.mCancels) {
                        removeEntry(entry.ID);
                    }
                }
                entry.mPrevFlag = entry.mFlag;
            }
        }
    }

    private static final int MSG_ARRIVAL = 0;
    private static final int MSG_CANCEL  = 1;
    private final H mH;

    private void schedule(int what, int delay) {
        mH.sendEmptyMessageDelayed(what, delay);
    }

    private void schedule(int what, int arg1, int arg2, Object obj, int delay) {
        mH.sendMessageDelayed(mH.obtainMessage(what, arg1, arg2, obj), delay);
    }

    // main looper
    private static final class H extends Handler {
        private WeakReference<NotificationCenter> mCenter;
        H(NotificationCenter c) {
            super(Looper.getMainLooper());
            mCenter = new WeakReference<NotificationCenter>(c);
        }

        @Override
        public void handleMessage(Message msg) {
            NotificationCenter c = mCenter.get();
            if (c == null) return;

            switch (msg.what) {
            case MSG_ARRIVAL:
                for (NotificationListener l : c.mListeners) {
                    l.onArrival((NotificationEntry) msg.obj);
                }
                break;

            case MSG_CANCEL:
                for (NotificationListener l : c.mListeners) {
                    l.onCancel((NotificationEntry) msg.obj);
                }
                break;
            }
        }
    }
}
