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
import java.util.List;

/**
 * Notification center.
 */
public class NotificationCenter {

    private static final String TAG = "zemin.NotificationCenter";
    public static boolean DBG;

    private final ArrayList<NotificationListener> mListeners =
        new ArrayList<NotificationListener>();

    private final ArrayList<NotificationHandler> mHandlers =
        new ArrayList<NotificationHandler>();

    private Context mContext;
    private NotificationEffect mEffect;
    NotificationCenterInner mActives;
    NotificationCenterInner mPendings;

    /* package */ NotificationCenter(Context context) {
        mContext = context;
        mH = new H(this);
        mEffect = new NotificationEffect(context);
        mActives = new NotificationCenterInner();
        mPendings = new NotificationCenterInner();
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
        entry.requestSend();
        updateEntryState(entry);
    }

    void cancel(int entryId) {
        if (mActives.cancel(entryId) || mPendings.cancel(entryId)) {
            return;
        }
        Log.e(TAG, "failed to get NotificationEntry for id=" + entryId);
    }

    void cancel(String tag) {
        mActives.cancel(tag);
        mPendings.cancel(tag);
    }

    void cancel(NotificationEntry entry) {
        if (mActives.cancel(entry)) {
            return;
        }
        mPendings.cancel(entry);
    }

    void cancelAll() {
        for (NotificationHandler h : mHandlers)
            h.cancelAll();
        clearEntry(0);
    }

    boolean hasEntry(int id) {
        return mActives.hasEntry(id) || mPendings.hasEntry(id);
    }

    boolean hasEntry(int target, int id) {
        return mActives.hasEntry(target, id) || mPendings.hasEntry(target, id);
    }

    boolean hasEntries() {
        return mActives.hasEntries() || mPendings.hasEntries();
    }

    boolean hasEntries(int target) {
        return hasEntries(target, null);
    }

    boolean hasEntries(String tag) {
        return hasEntries(NotificationDelegater.MASK, tag);
    }

    boolean hasEntries(int target, String tag) {
        return mActives.hasEntries(target, tag) || mPendings.hasEntries(target, tag);
    }

    NotificationEntry getEntry(int id) {
        NotificationEntry entry = mActives.getEntry(id);
        return entry != null ? entry : mPendings.getEntry(id);
    }

    NotificationEntry getEntry(int target, int id) {
        NotificationEntry entry = mActives.getEntry(target, id);
        return entry != null ? entry : mPendings.getEntry(target, id);
    }

    ArrayList<NotificationEntry> getEntries() {
        ArrayList<NotificationEntry> entries = new ArrayList<NotificationEntry>();
        entries.addAll(mActives.getRawEntries());
        entries.addAll(mPendings.getRawEntries());
        return entries;
    }

    ArrayList<NotificationEntry> getEntries(int target) {
        return getEntries(target, null);
    }

    ArrayList<NotificationEntry> getEntries(String tag) {
        return getEntries(NotificationDelegater.MASK, tag);
    }

    ArrayList<NotificationEntry> getEntries(int target, String tag) {
        ArrayList<NotificationEntry> entries = getEntries();
        Iterator<NotificationEntry> iter = entries.iterator();
        while (iter.hasNext()) {
            NotificationEntry entry = iter.next();
            if ((tag != null && !tag.equals(entry.tag)) || !entry.isSentToTarget(target)) {
                iter.remove();
            }
        }
        return entries;
    }

    int getEntryCount() {
        return mActives.getEntryCount() + mPendings.getEntryCount();
    }

    int getEntryCount(int target) {
        return getEntryCount(target, null);
    }

    int getEntryCount(String tag) {
        return getEntryCount(NotificationDelegater.MASK, tag);
    }

    int getEntryCount(int target, String tag) {
        return mActives.getEntryCount(target, tag) + mPendings.getEntryCount(target, tag);
    }

    class NotificationCenterInner {

        private final ArrayMap<Integer, NotificationEntry> mEntries =
            new ArrayMap<Integer, NotificationEntry>();

        private boolean cancel(int entryId) {
            NotificationEntry entry = getEntry(entryId);
            if (entry == null) {
                return false;
            }

            entry.requestCancel();
            updateEntryState(entry);
            return true;
        }

        private void cancel(String tag) {
            List<NotificationEntry> entries = getEntries(tag);
            if (entries == null || entries.isEmpty()) {
                Log.w(TAG, "no NotificationEntry found for tag=" + tag);
                return;
            }

            for (NotificationEntry entry : entries) {
                cancel(entry);
            }
        }

        private boolean cancel(NotificationEntry entry) {
            if (mEntries.containsKey(Integer.valueOf(entry.ID))) {
                entry.requestCancel();
                updateEntryState(entry);
                return true;
            }
            return false;
        }

        void addEntry(int id, NotificationEntry entry) {
            mEntries.put(Integer.valueOf(id), entry);
        }

        NotificationEntry removeEntry(int id) {
            return mEntries.remove(Integer.valueOf(id));
        }

        void clearEntry() {
            mEntries.clear();
        }

        boolean hasEntry(int id) {
            return mEntries.containsKey(Integer.valueOf(id));
        }

        boolean hasEntry(int target, int id) {
            NotificationEntry entry = mEntries.get(Integer.valueOf(id));
            return entry != null && entry.isSentToTarget(target);
        }

        boolean hasEntries() {
            return !mEntries.isEmpty();
        }

        boolean hasEntries(int target) {
            return hasEntries(target, null);
        }

        boolean hasEntries(String tag) {
            return hasEntries(NotificationDelegater.MASK, tag);
        }

        boolean hasEntries(int target, String tag) {
            synchronized (mEntries) {
                Collection<NotificationEntry> entries = mEntries.values();
                for (NotificationEntry entry : entries) {
                    if ((tag == null || tag.equals(entry.tag)) && entry.isSentToTarget(target)) {
                        return true;
                    }
                }
                return false;
            }
        }

        NotificationEntry getEntry(int id) {
            return mEntries.get(Integer.valueOf(id));
        }

        NotificationEntry getEntry(int target, int id) {
            NotificationEntry entry = mEntries.get(Integer.valueOf(id));
            return entry != null && entry.isSentToTarget(target) ? entry : null;
        }

        ArrayList<NotificationEntry> getEntries() {
            return new ArrayList<NotificationEntry>(mEntries.values());
        }

        ArrayList<NotificationEntry> getEntries(int target) {
            return getEntries(target, null);
        }

        ArrayList<NotificationEntry> getEntries(String tag) {
            return getEntries(NotificationDelegater.MASK, tag);
        }

        ArrayList<NotificationEntry> getEntries(int target, String tag) {
            synchronized (mEntries) {
                Collection<NotificationEntry> entries = mEntries.values();
                ArrayList<NotificationEntry> ret = new ArrayList<NotificationEntry>();
                for (NotificationEntry entry : entries) {
                    if ((tag == null || tag.equals(entry.tag)) && entry.isSentToTarget(target)) {
                        ret.add(entry);
                    }
                }
                return ret;
            }
        }

        Collection<NotificationEntry> getRawEntries() {
            return mEntries.values();
        }

        int getEntryCount() {
            return mEntries.size();
        }

        int getEntryCount(int target) {
            return getEntryCount(target, null);
        }

        int getEntryCount(String tag) {
            return getEntryCount(NotificationDelegater.MASK, tag);
        }

        int getEntryCount(int target, String tag) {
            synchronized (mEntries) {
                int count = 0;
                Collection<NotificationEntry> entries = mEntries.values();
                for (NotificationEntry entry : entries)
                    if ((tag == null || tag.equals(entry.tag)) && entry.isSentToTarget(target))
                        count++;
                return count;
            }
        }
    }

    private void onSendRequested(NotificationEntry entry) {
        for (NotificationHandler h : mHandlers)
            h.onSendRequested(entry);
    }

    private void onUpdateRequested(NotificationEntry entry) {
        for (NotificationHandler h : mHandlers)
            h.onUpdateRequested(entry);
    }

    private void onCancelRequested(NotificationEntry entry) {
        for (NotificationHandler h : mHandlers)
            h.onCancelRequested(entry);
    }

    private void onSendAsDefault(NotificationEntry entry) {
        // playEffect(entry);
        addEntry(entry.ID, entry);
    }

    private void onUpdateAsDefault(NotificationEntry entry) {
        updateEntry(entry);
    }

    private void playEffect(NotificationEntry entry) {
        synchronized (mEffect.mLock) {
            mEffect.play(entry);
        }
    }

    private void addEntry(int id, NotificationEntry entry) {
        synchronized (mActives) {
            if (!mActives.hasEntry(id)) {
                if (DBG) Log.v(TAG, "[entry:" + id + "] in - " + entry);
                mActives.addEntry(id, entry);
                mPendings.removeEntry(id);
                entry.mSent = true;
                if (entry.mSendToListener) {
                    schedule(MSG_ARRIVAL, 0, 0, entry, 0);
                }
            }
        }
    }

    private void removeEntry(int id) {
        synchronized (mActives) {
            if (mActives.hasEntry(id)) {
                NotificationEntry entry = mActives.removeEntry(id);
                if (DBG) Log.v(TAG, "[entry:" + id + "] out - " + entry);
                if (entry.mSendToListener) {
                    schedule(MSG_CANCEL, 0, 0, entry, 0);
                }
            }
        }
    }

    private void updateEntry(NotificationEntry entry) {
        if (entry.mSendToListener) {
            schedule(MSG_UPDATE, 0, 0, entry, 0);
        }
    }

    /* package */ void clearEntry(int target) {
        synchronized (mActives) {
            ArrayList<NotificationEntry> entries = getEntries();
            Iterator<NotificationEntry> iter = entries.iterator();
            while (iter.hasNext()) {
                NotificationEntry entry = iter.next();
                if (entry.mTargets == target) {
                    if (DBG) Log.v(TAG, "[entry:" + entry.ID + "] out - " + entry);
                    iter.remove();
                    mActives.removeEntry(entry.ID);
                    mPendings.removeEntry(entry.ID);
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
                    mPendings.addEntry(entry.ID, entry);
                    if (entry.mTargets == 0) {
                        onSendAsDefault(entry);
                    } else {
                        onSendRequested(entry);
                    }
                } if ((diff & NotificationEntry.FLAG_REQUEST_UPDATE) != 0) {
                    entry.mFlag &= ~NotificationEntry.FLAG_REQUEST_UPDATE;
                    if (entry.mTargets == 0) {
                        onUpdateAsDefault(entry);
                    } else {
                        onUpdateRequested(entry);
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
                    if (entry.mTargets == entry.mIgnores) {
                        onSendAsDefault(entry);
                    }
                } else if ((diff & NotificationEntry.FLAG_UPDATE_FINISHED) != 0) {
                    entry.mFlag &= ~NotificationEntry.FLAG_UPDATE_FINISHED;
                    if (entry.mTargets == entry.mUpdates) {
                        updateEntry(entry);
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
    private static final int MSG_UPDATE  = 2;
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

            NotificationEntry entry = (NotificationEntry) msg.obj;
            switch (msg.what) {
            case MSG_ARRIVAL:
                for (NotificationListener l : c.mListeners) {
                    l.onArrival(entry);
                }
                break;

            case MSG_CANCEL:
                for (NotificationListener l : c.mListeners) {
                    l.onCancel(entry);
                }
                if (!entry.contentExecuted || !entry.autoCancel) {
                    entry.executeCancelAction(c.mContext);
                }
                break;

            case MSG_UPDATE:
                for (NotificationListener l : c.mListeners) {
                    l.onUpdate(entry);
                }
            }
        }
    }
}
