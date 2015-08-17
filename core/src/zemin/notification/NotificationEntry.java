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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.util.ArrayList;

/**
 * Notification. You can also use {@link NotificationBuilder} to create {@link NotificationEntry} objects.
 */
public class NotificationEntry {

    private static final String TAG = "zemin.NotificationEntry";
    public static boolean DBG;

    public static final int INVALID = -1;

    /**
     * default date format
     */
    public static final String DEFAULT_DATE_FORMAT = "ahh:mm";

    /**
     * default priority
     */
    public static final Priority DEFAULT_PRIORITY = Priority.LOW;

    /**
     * intent extra
     */
    public static final String KEY_EXTRA = "key_extra_bundle";


    public final int ID;
    public String tag;
    public Priority priority;
    public boolean ongoing;
    public boolean nohistory;
    public boolean silentMode;
    public boolean autoSilentMode = true;
    public int delay;
    public int layoutId;
    public int backgroundColor;
    public int backgroundAlpha = INVALID;
    public int smallIconRes;
    public Bitmap largeIconBitmap;
    public Drawable iconDrawable;
    public CharSequence tickerText;
    public CharSequence title;
    public CharSequence text;
    public boolean showWhen = true;
    public long whenLong;
    public CharSequence whenFormatted;
    public int progress;
    public int progressMax;
    public boolean progressIndeterminate;
    public boolean useSystemEffect = true;
    public boolean playRingtone = true;
    public Uri ringtoneUri;
    public boolean useVibration = true;
    public long[] vibratePattern;
    public int vibrateRepeat;
    public long vibrateTime;
    public Bundle extra;
    public Object obj;
    public boolean autoCancel = true;
    public Action contentAction;
    public Action cancelAction;
    ArrayList<Action> mActions;

    /**
     * Creator.
     */
    public static NotificationEntry create() {
        return new NotificationEntry(genId());
    }

    /**
     * Send this notifications.
     */
    public void send() {
        NotificationDelegater.getInstance().send(this);
    }

    /**
     * Cancel this notification.
     */
    public void cancel() {
        NotificationDelegater.getInstance().cancel(this);
    }

    /**
     * Send this notification to StatusBar.
     *
     * @param send
     */
    public void sendToRemote(boolean send) {
        sendToTarget(send, NotificationDelegater.REMOTE);
    }

    /**
     * Send this notification to {@link NotifiationView} managed by {@link NotificationLocal}.
     *
     * @param send
     */
    public void sendToLocalView(boolean send) {
        sendToTarget(send, NotificationDelegater.LOCAL);
    }

    /**
     * Send this notification to {@link NotificationView} managed by {@link NotificationGlobal}.
     *
     * @param send
     */
    public void sendToGlobalView(boolean send) {
        sendToTarget(send, NotificationDelegater.GLOBAL);
    }

    /**
     * Send this notification to {@link NotificationListener}. The default is true.
     *
     * @param send
     */
    public void sendToListener(boolean send) {
        mSendToListener = send;
    }

    /**
     * Set tag.
     *
     * @param tag
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * A Notification with higher priority will be displayed first.
     *
     * only used for:
     * @see NotificationLocal
     * @see NotificationGlobal
     *
     * @param priority
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    /**
     * A on-going notification cannot be canceled by the user.
     * (e.g. user gesture cannot cause NotificationView to be dismissed)
     *
     * @param ongoing
     */
    public void setOngoing(boolean ongoing) {
        this.ongoing = ongoing;
    }

    /**
     * A no-history notification is canceled immediately when the
     * {@link NotificationView} presenting it is dismissed.
     *
     * only used for:
     * @see NotificationLocal
     * @see NotificationGlobal
     *
     * @param nohistory
     */
    public void setNohistory(boolean nohistory) {
        this.nohistory = nohistory;
    }

    /**
     * In silent mode, any update of the notification won't be presented to the user.
     * In other word, {@link NotificationView} won't displayed for this notification.
     * To check the update, you can use {@link NotificationBoard} on which all the current
     * notifications reside.
     *
     * only used for:
     * @see NotificationLocal
     * @see NotificationGlobal
     *
     * @param silent
     */
    public void setSilentMode(boolean silent) {
        this.silentMode = silent;
    }

    /**
     * Automatically set silent mode, when {@link NotificationView} get dismissed.
     *
     * @param auto
     */
    public void setAutoSilentMode(boolean auto) {
        this.autoSilentMode = auto;
    }

    /**
     * Set delay before this notification get delivered.
     *
     * @param ms
     */
    public void setDelay(int ms) {
        this.delay = ms;
    }

    /**
     * Set layout resource for customizing the user interface. If 0, default layout will be used.
     *
     * @param resId
     */
    public void setLayoutId(int resId) {
        this.layoutId = resId;
    }

    /**
     * Set background color. If 0, default background color will be used.
     *
     * Only used for:
     * @see NotificationLocal
     * @see NotificationGlobal
     *
     * @param color
     */
    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }

    /**
     * Set opacity of the background. The default is 0xff.
     *
     * Only used for:
     * @see NotificationLocal
     * @see NotificationGlobal
     *
     * @param alpha [0 - 255]
     */
    public void setBackgroundAlpha(int alpha) {
        this.backgroundAlpha = alpha;
    }

    /**
     * Set whether the timestamp set with {@link #setWhen} is shown.
     *
     * @param show
     */
    public void setShowWhen(boolean show) {
        this.showWhen = show;
    }

    /**
     * Set a timestamp pertaining to this notification.
     *
     * @param when
     */
    public void setWhen(long when) {
        this.whenLong = when;
    }

    /**
     * Set a timestamp pertaining to this notification.
     *
     * Only used for:
     * @see NotificationLocal
     * @see NotificationGlobal
     *
     * @param when
     */
    public void setWhen(CharSequence when) {
        this.whenFormatted = when;
    }

    /**
     * Set a timestamp pertaining to this notification.
     *
     * Only used for:
     * @see NotificationLocal
     * @see NotificationGlobal
     *
     * @param format
     * @param when
     */
    public void setWhen(CharSequence format, long when) {
        if (format == null) format = DEFAULT_DATE_FORMAT;
        this.whenFormatted = DateFormat.format(format, when);
    }

    /**
     * Set small icon resource.
     *
     * @param resId
     */
    public void setSmallIconResource(int resId) {
        this.smallIconRes = resId;
    }

    /**
     * Set large icon bitmap.
     *
     * Only used for:
     * @see NotificationRemote
     *
     * @param bitmap
     */
    public void setLargeIconBitmap(Bitmap bitmap) {
        this.largeIconBitmap = bitmap;
    }

    /**
     * Set icon drawable.
     *
     * Only used for:
     * @see NotificationLocal
     * @see NotificationGlobal
     *
     * @param drawable
     */
    public void setIconDrawable(Drawable drawable) {
        this.iconDrawable = drawable;
    }

    /**
     * Set the text that is displayed in the status-bar when the notification first arrives.
     *
     * Only used for:
     * @see NotificationRemote
     *
     * @param tickerText
     */
    public void setTicker(CharSequence tickerText) {
        this.tickerText = tickerText;
    }

    /**
     * Set notification title.
     *
     * @param title
     */
    public void setTitle(CharSequence title) {
        this.title = title;
    }

    /**
     * Set notification text.
     *
     * @param text
     */
    public void setText(CharSequence text) {
        this.text = text;
    }

    /**
     * Set the progress this notification represents.
     *
     * @see android.app.Notification#setProgress(int, int, boolean)
     *
     * @param max
     * @param progress
     * @param indeterminate
     */
    public void setProgress(int max, int progress, boolean indeterminate) {
        this.progressMax = max;
        this.progress = progress;
        this.progressIndeterminate = indeterminate;
    }

    /**
     * Set a action to be fired when the notification content gets clicked.
     *
     * @param listener
     */
    public void setContentAction(Action.OnActionListener listener) {
        setContentAction(listener, null, null, null, null);
    }

    /**
     * Set a action to be fired when the notification content gets clicked.
     *
     * @param listener
     * @param extra
     */
    public void setContentAction(Action.OnActionListener listener, Bundle extra) {
        setContentAction(listener, null, null, null, extra);
    }

    /**
     * Set a action to be fired when the notification content gets clicked.
     *
     * @param listener
     * @param activity The activity to be started.
     * @param extra Intent extra.
     */
    public void setContentAction(Action.OnActionListener listener, ComponentName activity, Bundle extra) {
        setContentAction(listener, activity, null, null, extra);
    }

    /**
     * Set a action to be fired when the notification content gets clicked.
     *
     * @param listener
     * @param activity The activity to be started.
     * @param service The service to be started.
     * @param broadcast The broadcast to be sent.
     * @param extra Intent extra.
     */
    public void setContentAction(Action.OnActionListener listener, ComponentName activity,
                                 ComponentName service, String broadcast, Bundle extra) {
        setContentAction(new Action(listener, activity, service, broadcast, extra));
    }

    /**
     * Set a action to be fired when the notification content gets clicked.
     *
     * @param act
     */
    public void setContentAction(Action act) {
        if (act.entry != null && act.entry != this) {
            Log.e(TAG, "setContentAction failed. Already applied to another notification - " +
                  act.entry.ID + ". Current notification is " + ID);
            return;
        }
        this.contentAction = act;
        this.contentAction.entry = this;
        this.contentAction.title = "ContentAction";
    }

    /**
     * Set a action to be fired when this notification gets canceled.
     *
     * @param listener
     */
    public void setCancelAction(Action.OnActionListener listener) {
        setCancelAction(listener, null, null, null, null);
    }

    /**
     * Set a action to be fired when this notification gets canceled.
     *
     * @param listener
     * @param extra
     */
    public void setCancelAction(Action.OnActionListener listener, Bundle extra) {
        setCancelAction(listener, null, null, null, extra);
    }

    /**
     * Set a action to be fired when this notification gets canceled.
     *
     * @param listener
     * @param activity The activity to be started.
     * @param extra Intent extra.
     */
    public void setCancelAction(Action.OnActionListener listener, ComponentName activity,
                                Bundle extra) {
        setCancelAction(listener, activity, null, null, extra);
    }

    /**
     * Set a action to be fired when this notification gets canceled.
     *
     * @param listener
     * @param activity The activity to be started.
     * @param service The service to be started.
     * @param broadcast The broadcast to be sent.
     * @param extra Intent extra.
     */
    public void setCancelAction(Action.OnActionListener listener, ComponentName activity,
                                ComponentName service, String broadcast, Bundle extra) {
        setCancelAction(new Action(listener, activity, service, broadcast, extra));
    }

    /**
     * Set a action to be fired when this notification gets canceled.
     *
     * @param act
     */
    public void setCancelAction(Action act) {
        if (act.entry != null && act.entry != this) {
            Log.e(TAG, "setCancelAction failed. Already applied to another notification - " +
                  act.entry.ID + ". Current notification is " + ID);
            return;
        }
        this.cancelAction = act;
        this.cancelAction.entry = this;
        this.cancelAction.title = "CancelAction";
    }

    /**
     * Add a action to this notification. Actions are typically displayed as a
     * button adjacent to the notification content.
     *
     * @see android.app.Notification#addAction
     *
     * @param icon
     * @param title
     * @param listener
     */
    public void addAction(int icon, CharSequence title, Action.OnActionListener listener) {
        addAction(icon, title, listener, null, null, null, null);
    }

    /**
     * Add a action to this notification. Actions are typically displayed as a
     * button adjacent to the notification content.
     *
     * @see android.app.Notification#addAction
     *
     * @param icon
     * @param title
     * @param listener
     * @param extra
     */
    public void addAction(int icon, CharSequence title, Action.OnActionListener listener, Bundle extra) {
        addAction(icon, title, listener, null, null, null, extra);
    }

    /**
     * Add a action to this notification. Actions are typically displayed as a
     * button adjacent to the notification content.
     *
     * @see android.app.Notification#addAction
     *
     * @param icon
     * @param title
     * @param listener
     * @param activity The activity to be started.
     * @param service The service to be started.
     * @param broadcast The broadcast to be sent.
     * @param extra
     */
    public void addAction(int icon, CharSequence title, Action.OnActionListener listener,
                          ComponentName activity, ComponentName service, String broadcast,
                          Bundle extra) {
        addAction(new Action(icon, title, listener, activity, service, broadcast, extra));
    }

    /**
     * Add a action to this notification. Actions are typically displayed as a
     * button adjacent to the notification content.
     *
     * @see android.app.Notification#addAction
     *
     * @param act
     */
    public void addAction(Action act) {
        if (act.entry != null && act.entry != this) {
            Log.e(TAG, "addAction failed. Already applied to another notification - " +
                  act.entry.ID + ". Current notification is " + ID);
            return;
        }
        if (mActions == null) {
            mActions = new ArrayList<Action>();
        }
        if (mActions.size() == 3) {
            Log.w(TAG, "only suppport up to 3 actions.");
            return;
        }
        act.entry = this;
        mActions.add(act);
    }

    /**
     * Set whether to use system effect. Only for {@link NotificationRemote}. The default is true.
     *
     * If true, effects will be controlled by the system.
     * If false, effects will be handled by {@link NotificationEffect}.
     *
     * Only used for:
     * @see NotificationRemote
     *
     * @param use
     */
    public void setUseSystemEffect(boolean use) {
        this.useSystemEffect = use;
    }

    /**
     * Set whether to play ringtone.
     *
     * @param play
     */
    public void setPlayRingtone(boolean play) {
        this.playRingtone = play;
    }

    /**
     * Set ringtone resource.
     *
     * @param context
     * @param resId
     */
    public void setRingtone(Context context, int resId) {
        if (resId > 0) {
            this.ringtoneUri = Uri.parse("android.resource://" +
                context.getPackageName() + "/" + resId);
        }
    }

    /**
     * Set ringtone uri.
     *
     * @param uri
     */
    public void setRingtone(Uri uri) {
        if (uri != null) {
            this.ringtoneUri = uri;
        }
    }

    /**
     * Set ringtone file path.
     *
     * @param filepath
     */
    public void setRingtone(String filepath) {
        if (filepath != null) {
            File file = new File(filepath);
            if (file.exists()) {
                this.ringtoneUri = Uri.fromFile(file);
            } else {
                Log.e(TAG, "ringtone file not found.");
            }
        }
    }

    /**
     * Set whether to use vibration.
     *
     * @param use
     */
    public void setUseVibration(boolean use) {
        this.useVibration = use;
    }

    /**
     * Set vibration pattern.
     *
     * @param pattern
     * @param repeat
     */
    public void setVibrate(long[] pattern, int repeat) {
        if (pattern != null) {
            this.vibratePattern = pattern;
            this.vibrateRepeat = repeat;
        }
    }

    /**
     * Set vibration period.
     *
     * Only used for:
     * @see NotificationLocal
     * @see NotificationGlobal
     *
     * @param ms
     */
    public void setVibrate(long ms) {
        if (ms > 0) {
            this.vibrateTime = ms;
        }
    }

    /**
     * Set metadata.
     *
     * @param extra
     */
    public void setExtra(Bundle extra) {
        this.extra = extra;
    }

    /**
     * Set extra object.
     *
     * Only used for:
     * @see NotificationLocal
     * @see NotificationGlobal
     *
     * @param obj
     */
    public void setObject(Object obj) {
        this.obj = obj;
    }

    /**
     * Set whether to cancel the notification automatically when the user touches it.
     *
     * @param autoCancel
     */
    public void setAutoCancel(boolean autoCancel) {
        this.autoCancel = autoCancel;
    }

    // ==========================================

    public boolean hasActions() {
        return mActions != null && !mActions.isEmpty();
    }

    public ArrayList<Action> getActions() {
        return mActions != null ? new ArrayList<Action>(mActions) : null;
    }

    public Action getAction(int idx) {
        return mActions != null && idx >= 0 && idx < mActions.size() ? mActions.get(idx) : null;
    }

    public int getActionCount() {
        return mActions != null ? mActions.size() : 0;
    }

    public Action getContentAction() {
        return contentAction;
    }

    public Action getCancelAction() {
        return cancelAction;
    }

    public boolean isSentToRemote() {
        return isSentToTarget(NotificationDelegater.REMOTE);
    }

    public boolean isSentToLocalView() {
        return isSentToTarget(NotificationDelegater.LOCAL);
    }

    public boolean isSentToGlobalView() {
        return isSentToTarget(NotificationDelegater.GLOBAL);
    }

    public boolean isSentToListener() {
        return mSendToListener;
    }

    public void sendToTarget(boolean send, int target) {
        if (send) mTargets |= target;
        else mTargets &= ~target;
    }

    public boolean isSentToTarget(int target) {
        return (mTargets & target) != 0;
    }

    public boolean isCanceled(int target) {
        return (mCancels & target) != 0;
    }

    void executeContentAction(Context context) {
        contentExecuted = true;
        if (contentAction != null) {
            contentAction.execute(context);
        }
    }

    void executeCancelAction(Context context) {
        if (cancelAction != null) {
            cancelAction.execute(context);
        }
    }

    /**
     * @see android.app.Notification#addAction
     */
    public static class Action {

        public interface OnActionListener {
            boolean onAction(NotificationEntry entry, Action action);
        }

        public int icon;
        public CharSequence title;

        NotificationEntry entry;
        OnActionListener listener;
        Bundle extra;

        ComponentName activity;
        ComponentName service;
        String broadcast;

        public Action(OnActionListener listener, ComponentName activity,
                      ComponentName service, String broadcast, Bundle extra) {
            this.listener = listener;
            this.activity = activity;
            this.service = service;
            this.broadcast = broadcast;
            this.extra = extra;
        }

        public Action(int icon, CharSequence title, OnActionListener listener,
                      ComponentName activity, ComponentName service, String broadcast,
                      Bundle extra) {
            this.icon = icon;
            this.title = title;
            this.listener = listener;
            this.activity = activity;
            this.service = service;
            this.broadcast = broadcast;
            this.extra = extra;
        }

        public Bundle extra() {
            if (extra == null) {
                extra = new Bundle();
            }
            return extra;
        }

        public void execute(Context context) {
            if (DBG) Log.v(TAG, "execute action - " + toString());
            if (listener != null && listener.onAction(entry, this)) {
                return;
            }
            if (broadcast != null) {
                Intent intent = new Intent(broadcast);
                if (extra != null) {
                    intent.putExtra(KEY_EXTRA, extra);
                }
                context.sendBroadcast(intent);
            }
            if (activity != null) {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setComponent(activity);
                if (extra != null) {
                    intent.putExtra(KEY_EXTRA, extra);
                }
                context.startActivity(intent);
            }
            if (service != null) {
                Intent intent = new Intent();
                intent.setComponent(service);
                if (extra != null) {
                    intent.putExtra(KEY_EXTRA, extra);
                }
                context.startService(intent);
            }
        }

        @Override
        public String toString() {
            return (String) title;
        }
    }

    // priority
    public enum Priority {
        HIGH(2), MEDIAN(1), LOW(0);

        public static Priority get(int v) {
            switch (v) {
            case 2:  return HIGH;
            case 1:  return MEDIAN;
            case 0:  return LOW;
            default: return null;
            }
        }

        public boolean higher(Priority priority) {
            return V > priority.V;
        }

        private final int V;
        private Priority(int v) { V = v; }
    };

    // flags
    static final int FLAG_REQUEST_SEND       = 0x00000001;
    static final int FLAG_SEND_FINISHED      = 0x00000004;
    static final int FLAG_SEND_IGNORED       = 0x00000008;
    static final int FLAG_REQUEST_CANCEL     = 0x00000010;
    static final int FLAG_CANCEL_FINISHED    = 0x00000020;
    static final int FLAG_REQUEST_UPDATE     = 0x00000100;
    static final int FLAG_UPDATE_FINISHED    = 0x00000200;

    final Object mLock = new Object();

    int mFlag;
    int mPrevFlag;
    int mTargets;
    int mCancels;
    int mUpdates;
    int mIgnores;
    int mEffectConsumers;
    boolean mSendToListener;
    boolean mUpdate;
    boolean mSent;
    boolean contentExecuted;

    private NotificationEntry(int id) {
        ID = id;
        mPrevFlag = 0;
        mTargets = 0;
        mEffectConsumers = 0;
        mSendToListener = true;
    }

    void requestSend() {
        if (hasFlag(FLAG_REQUEST_SEND)) {
            addFlag(FLAG_REQUEST_UPDATE);
            mUpdate = true;
            mUpdates = 0;
        } else {
            addFlag(FLAG_REQUEST_SEND);
        }
    }

    void requestCancel() {
        addFlag(FLAG_REQUEST_CANCEL);
    }

    boolean hasFlag(int flag) {
        synchronized (mLock) {
            return (mFlag & flag) != 0;
        }
    }

    void addFlag(int flag) {
        synchronized (mLock) {
            if ((mFlag & flag) == 0) {
                mPrevFlag = mFlag;
                mFlag |= flag;
            }
        }
    }

    private static int sID = 0;
    private static int genId() { return sID++; }

    public static void appendComponentName(StringBuilder sb, int components) {
        boolean comma = false;
        if ((components & NotificationDelegater.LOCAL) != 0) {
            comma = true;
            sb.append(NotificationLocal.SIMPLE_NAME);
        }
        if ((components & NotificationDelegater.GLOBAL) != 0) {
            if (comma) {
                sb.append(",");
            }
            comma = true;
            sb.append(NotificationGlobal.SIMPLE_NAME);
        }
        if ((components & NotificationDelegater.REMOTE) != 0) {
            if (comma) {
                sb.append(",");
            }
            sb.append(NotificationRemote.SIMPLE_NAME);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" Notification@").append(hashCode()).append(":").append(priority);
        sb.append(" [ targets="); appendComponentName(sb, mTargets);
        sb.append(", cancels="); appendComponentName(sb, mCancels);
        sb.append(", listener=").append(mSendToListener);
        sb.append(" ] { tag=").append(tag);
        sb.append(", id=").append(ID);
        sb.append(", ongoing=").append(ongoing);
        sb.append(", whenLong=").append(whenLong);
        sb.append(", whenFormatted=").append(whenFormatted);
        sb.append(", title=").append(title);
        sb.append(", text=").append(text);
        sb.append(", backgroundColor=").append(backgroundColor);
        sb.append(", backgroundAlpha=").append(backgroundAlpha);
        sb.append(", hasActions=").append(hasActions());
        sb.append(", extra=").append(extra);
        sb.append(", obj=").append(obj);
        sb.append(", systemEffect=").append(useSystemEffect);
        sb.append(", ringtone=").append(ringtoneUri);
        sb.append(", vibrationPattern=").append(vibratePattern);
        sb.append(", vibrationTime=").append(vibrateTime);
        sb.append(" }");
        return sb.toString();
    }
}
