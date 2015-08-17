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

import java.util.List;

/**
 * Delegater
 */
public class NotificationDelegater {

    private static final String TAG = "zemin.NotificationDelegater";
    public static boolean DBG;

    /**
     * Support local (in-layout) notifications.
     */
    public static final int LOCAL  = 0x00000001;

    /**
     * Support global (floating) notifications.
     */
    public static final int GLOBAL = 0x00000002;

    /**
     * Support remote (status-bar) notifications.
     */
    public static final int REMOTE = 0x00000004;

    public static final int MASK   = 0x0000000F;


    /**
     * Get the singleton instance.
     *
     * @return NotificationDelegater
     */
    public static NotificationDelegater getInstance() {
        synchronized (NotificationDelegater.class) {
            if (sSelf == null)
                sSelf = new NotificationDelegater();
            return sSelf;
        }
    }

    /**
     * Initialization.
     *
     * @see NotificationDelegater#LOCAL
     * @see NotificationDelegater#GLOBAL
     * @see NotificationDelegater#REMOTE
     *
     * @param context
     * @param components
     */
    public static void initialize(Context context, int components) {
        NotificationDelegater delegater = getInstance();
        if (delegater.center() != null)
            throw new IllegalStateException("NotificationDelegater already init.");

        delegater.setContext(context);
        delegater.initComponents(components);
        Log.i(TAG, "Notification delegater initialize");
    }

    /**
     * Whether notification is disabled globally.
     *
     * @return boolean
     */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Enable/disable notification globally.
     *
     * @param enable
     */
    public void setEnabled(boolean enable) {
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
     * Send notification. If a notification with the same id has already been posted,
     * it will be replaced by the updated information.
     *
     * @param entry
     */
    public void send(NotificationEntry entry) {
        if (mEnabled) CENTER.send(entry);
    }

    /**
     * Cancel notification.
     *
     * @param entryId
     */
    public void cancel(int entryId) {
        if (mEnabled) CENTER.cancel(entryId);
    }

    /**
     * Cancel notification. Notifications having the same tag will all be canceled.
     *
     * @param tag
     */
    public void cancel(String tag) {
        if (mEnabled) CENTER.cancel(tag);
    }

    /**
     * Cancel notification.
     *
     * @param entry
     */
    public void cancel(NotificationEntry entry) {
        if (mEnabled) CENTER.cancel(entry);
    }

    /**
     * Cancel all notifications.
     */
    public void cancelAll() {
        if (mEnabled) CENTER.cancelAll();
    }

    /**
     * Whether any notifications exist.
     *
     * @return boolean
     */
    public boolean hasNotifications() {
        return CENTER.hasEntries();
    }

    /**
     * Whether the notification with id exists.
     *
     * @param entryId
     * @return boolean
     */
    public boolean hasNotification(int entryId) {
        return CENTER.hasEntry(entryId);
    }

    /**
     * Whether the notifications with tag exists.
     *
     * @param tag
     * @return boolean
     */
    public boolean hasNotifications(String tag) {
        return CENTER.hasEntries(tag);
    }

    /**
     * Get notification by its id.
     *
     * @param entryId
     * @return NotificationEntry
     */
    public NotificationEntry getNotification(int entryId) {
        return CENTER.getEntry(entryId);
    }

    /**
     * Get notification by its tag.
     *
     * @param tag
     * @return List
     */
    public List<NotificationEntry> getNotifications(String tag) {
        return CENTER.getEntries(tag);
    }

    /**
     * Get notifications.
     *
     * @return List
     */
    public List<NotificationEntry> getNotifications() {
        return CENTER.mActives.getEntries();
    }

    /**
     * Get current notification count.
     *
     * @param tag
     * @return int
     */
    public int getNotificationCount(String tag) {
        return CENTER.mActives.getEntryCount(tag);
    }

    /**
     * Get current notification count.
     *
     * @return int
     */
    public int getNotificationCount() {
        return CENTER.mActives.getEntryCount();
    }

    /**
     * Add listener whick will be invoked when a notification arrives or get canceled.
     *
     * @param listener
     */
    public void addListener(NotificationListener listener) {
        CENTER.addListener(listener);
    }

    /**
     * Remove listener.
     *
     * @param listener
     */
    public void removeListener(NotificationListener listener) {
        CENTER.removeListener(listener);
    }

    /**
     * Enable/disable notification effect globally.
     *
     * @param enable
     */
    public void enableEffect(boolean enable) {
        CENTER.effect().setEnabled(enable);
    }

    /**
     * Get the singleton object of NotificationEffect.
     *
     * @return NotificationEffect
     */
    public NotificationEffect effect() {
        return CENTER.effect();
    }

    /**
     * Get the singleton object of NotificationRemote.
     *
     * @return NotificationRemote
     */
    public NotificationRemote remote() {
        return (NotificationRemote) CENTER.get(REMOTE);
    }

    /**
     * Get the singleton object of NotificationLocal.
     *
     * @return NotificationLocal
     */
    public NotificationLocal local() {
        return (NotificationLocal) CENTER.get(LOCAL);
    }

    /**
     * Get the singleton object of NotificationGlobal.
     *
     * @return NotificationGlobal
     */
    public NotificationGlobal global() {
        return (NotificationGlobal) CENTER.get(GLOBAL);
    }

    /**
     * Enable/disable debug log.
     *
     * @param debug
     */
    public static void debug(boolean debug) {
        NotificationCenter.DBG =
            NotificationEffect.DBG =
            NotificationBuilder.DBG =
            NotificationEntry.DBG =
            NotificationHandler.DBG =
            NotificationRemote.DBG =
            NotificationRemoteCallback.DBG =
            NotificationLocal.DBG =
            NotificationGlobal.DBG =
            NotificationView.DBG =
            NotificationViewCallback.DBG =
            NotificationRootView.DBG =
            NotificationBoard.DBG =
            NotificationBoardCallback.DBG =
            ViewWrapper.DBG =
            ViewSwitcherWrapper.DBG =
            ChildViewManager.DBG =
            DBG = debug;
    }

    private Context mContext;
    private boolean mEnabled = true;
    private HandlerThread mHT;
    private NotificationCenter CENTER;

    /* package */ NotificationCenter center() { return CENTER; }

    private void setContext(Context context) {
        mContext = context;
    }

    private void initComponents(int components) {
        CENTER = new NotificationCenter(mContext);

        // effect
        NotificationEffect effect = CENTER.effect();
        effect.setRingtoneResource(R.raw.fallbackring);
        effect.setVibrateTime(300L);

        // remote
        if ((components & REMOTE) != 0) {
            CENTER.register(new NotificationRemote(mContext, getMyLooper()));
        }

        // local
        if ((components & LOCAL) != 0) {
            CENTER.register(new NotificationLocal(mContext, getMyLooper()));
        }

        // global
        if ((components & GLOBAL) != 0) {
            CENTER.register(new NotificationGlobal(mContext, getMyLooper()));
        }

        debug((mContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
    }

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
