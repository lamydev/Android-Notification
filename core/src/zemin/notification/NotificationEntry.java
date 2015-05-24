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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;

import java.io.File;

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

    public final int ID;
    public String tag;
    public Priority priority;
    public boolean ongoing;
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
    public boolean useSystemEffect = true;
    public boolean playRingtone = true;
    public Uri ringtoneUri;
    public boolean useVibration = true;
    public long[] vibratePattern;
    public int vibrateRepeat;
    public long vibrateTime;
    public Bundle extra;
    public Object obj;
    public Class activityClass;
    public boolean autoCancel = true;
    public View.OnClickListener onClickListener;

    /**
     * Creator.
     */
    public static NotificationEntry create() {
        return new NotificationEntry(genId());
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
     * Set activity class. Activity will be launched when the user touches it.
     *
     * Only used for:
     * @see NotificationRemote
     *
     * @param activityClass
     */
    public void setActivityClass(Class<?> activityClass) {
        this.activityClass = activityClass;
    }

    /**
     * Set whether to cancel the notification automatically when the user touches it.
     *
     * @param autoCancel
     */
    public void setAutoCancel(boolean autoCancel) {
        this.autoCancel = autoCancel;
    }

    /**
     * Set an object of {@link View#OnClickListener} which will be invoked when the user clicks on it.
     *
     * Only used for:
     * @see NotificationLocal
     * @see NotificationGlobal
     *
     * @param l
     */
    public void setOnClickListener(View.OnClickListener l) {
        this.onClickListener = l;
    }

    // ==========================================

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

    // cancel
    public void cancel() {
        synchronized (mLock) {
            if ((mFlag & FLAG_REQUEST_CANCEL) == 0) {
                mPrevFlag = mFlag;
                mFlag |= FLAG_REQUEST_CANCEL;
            }
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

    int mFlag;
    int mPrevFlag;
    int mTargets;
    int mCancels;
    int mEffectConsumers;
    boolean mSendToListener;
    final Object mLock = new Object();

    private NotificationEntry(int id) {
        ID = id;
        mFlag = FLAG_REQUEST_SEND;
        mPrevFlag = 0;
        mTargets = 0;
        mEffectConsumers = 0;
        mSendToListener = true;
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
        sb.append(", activityClass=").append(activityClass);
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
