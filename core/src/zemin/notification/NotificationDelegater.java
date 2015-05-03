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
import android.content.pm.ApplicationInfo;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

//
// Delegater
//
// @author Zemin Liu
//
public class NotificationDelegater {

    private static final String TAG = "zemin.NotificationDelegater";
    public static boolean DBG;

    /**
     * support sending remote notifications (such as StatusBar Notification).
     */
    public static final int FLAG_REMOTE_NOTIFICATION  = 0x00000001;

    /**
     * support sending local notifications (such as NotificationView in a Activity's layout).
     */
    public static final int FLAG_LOCAL_NOTIFICATION   = 0x00000002;

    /**
     * support sending global notifications (such as floating NotificationView).
     */
    public static final int FLAG_GLOBAL_NOTIFICATION  = 0x00000004;


    /**
     * @return the singleton instance of NotificationDelegater.
     */
    public static NotificationDelegater getInstance() {
        synchronized (NotificationDelegater.class) {
            if (sSelf == null)
                sSelf = new NotificationDelegater();
            return sSelf;
        }
    }

    /**
     * initialize
     *
     * @see NotificationDelegater#FLAG_REMOTE_NOTIFICATION
     * @see NotificationDelegater#FLAG_LOCAL_NOTIFICATION
     * @see NotificationDelegater#FLAG_GLOBAL_NOTIFICATION
     *
     * @param context
     * @param flags
     */
    public void init(Context context, int flags) {
        if (CENTER != null)
            throw new IllegalStateException("NotificationDelegater already init.");

        Log.i(TAG, "Notification delegater init");

        // debug
        DBG = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        NotificationCenter.DBG =
            NotificationEffect.DBG =
            NotificationEntry.DBG =
            NotificationHandler.DBG =
            NotificationRemote.DBG =
            NotificationRemoteFactory.DBG =
            NotificationLocal.DBG =
            NotificationGlobal.DBG =
            NotificationView.DBG =
            NotificationViewCallback.DBG =
            DBG;

        mContext = context;
        CENTER = new NotificationCenter(context);

        // effect
        NotificationEffect effect = CENTER.effect();
        effect.setRingtoneResource(R.raw.fallbackring);
        effect.setVibrateTime(300L);

        // remote
        if ((flags & FLAG_REMOTE_NOTIFICATION) != 0) {
            CENTER.register(new NotificationRemote(mContext, getMyLooper()));
        }

        // local
        if ((flags & FLAG_LOCAL_NOTIFICATION) != 0) {
            CENTER.register(new NotificationLocal(mContext, getMyLooper()));
        }

        // global
        if ((flags & FLAG_GLOBAL_NOTIFICATION) != 0) {
            CENTER.register(new NotificationGlobal(mContext, getMyLooper()));
        }
    }

    /**
     * @return boolean
     */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * enable/disable notification globally
     */
    public void enable(boolean enable) {
        if (enable && !mEnabled) {
            if (DBG) Log.d(TAG, "Notification is now enabled.");
            mEnabled = true;
        } else if (enable && !mEnabled) {
            if (DBG) Log.d(TAG, "Notification is now disabled.");
            mEnabled = false;
            cancelAll();
        }
    }

    /**
     * send notification
     *
     * @param entry
     */
    public void send(NotificationEntry entry) {
        if (mEnabled) CENTER.send(entry);
    }

    /**
     * cancel notification by entryId
     *
     * @param entryId
     */
    public void cancel(int entryId) {
        CENTER.cancel(entryId);
    }

    /**
     * cancel notification
     *
     * @param entry
     */
    public void cancel(NotificationEntry entry) {
        CENTER.cancel(entry);
    }

    /**
     * cancel all notifications
     */
    public void cancelAll() {
        CENTER.cancelAll();
    }

    /**
     * retrieve notification by entryId
     *
     * @param entryId
     * @return NotificationEntry
     */
    public NotificationEntry getNotification(int entryId) {
        return CENTER.getEntry(entryId);
    }

    /**
     * retrieve current notification count
     *
     * @return int
     */
    public int getNotificationCount() {
        return CENTER.getEntryCount();
    }

    /**
     * @param listener
     */
    public void addListener(NotificationListener listener) {
        CENTER.addListener(listener);
    }

    /**
     * @param listener
     */
    public void removeListener(NotificationListener listener) {
        CENTER.removeListener(listener);
    }

    /**
     * enable/disable notification effect globally
     *
     */
    public void enableEffect(boolean enable) {
        CENTER.effect().enable(enable);
    }

    /**
     * to configure NotificationEffect
     *
     * @return NotificationEffect
     */
    public NotificationEffect effect() {
        return CENTER.effect();
    }

    /**
     * to configure remote view
     *
     * @return NotificationRemote
     */
    public NotificationRemote remote() {
        return (NotificationRemote) CENTER.get(NotificationEntry.TARGET_REMOTE);
    }

    /**
     * to configure local view
     *
     * @return NotificationLocal
     */
    public NotificationLocal local() {
        return (NotificationLocal) CENTER.get(NotificationEntry.TARGET_LOCAL);
    }

    /**
     * to configure global view
     *
     * @return NotificationGlobal
     */
    public NotificationGlobal global() {
        return (NotificationGlobal) CENTER.get(NotificationEntry.TARGET_GLOBAL);
    }

    private NotificationCenter CENTER;
    /* package */ NotificationCenter center() { return CENTER; }

    private Context mContext;
    private boolean mEnabled = true;
    private HandlerThread mHT;

    private static NotificationDelegater sSelf;
    private NotificationDelegater() {}

    private Looper getMyLooper() {
        if (mHT == null) {
            mHT = new HandlerThread(TAG);
            mHT.start(); // non-stop
        }
        return mHT.getLooper();
    }
}
