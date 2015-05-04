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

import java.io.File;

//
// @author Zemin Liu
//
public class NotificationEntry {

    private static final String TAG = "zemin.NotificationEntry";
    public static boolean DBG;

    /**
     * default date format
     */
    public final static String DEFAULT_DATE_FORMAT = "aHH:mm";

    /**
     * default priority
     */
    public final static Priority DEFAULT_PRIORITY = Priority.LOW;

    public final int ID;
    public String tag;
    public Priority priority;
    public int delay;
    public int contentResId;
    public int backgroundColor;
    public int smallIconRes;
    public Bitmap largeIconBitmap;
    public Drawable iconDrawable;
    public CharSequence title;
    public CharSequence text;
    public boolean showWhen = true;
    public long whenLong;
    public CharSequence whenFormatted;
    public boolean useSystemEffect = true;
    public boolean useRingtone = true;
    public Uri ringtoneUri;
    public boolean useVibrate = true;
    public long[] vibratePattern;
    public int vibrateRepeat;
    public long vibrateTime;
    public Bundle extra;
    public Object obj;
    public Class activityClass;
    public boolean autoCancel = true;

    /**
     * factory
     *
     * @return NotificationEntry
     */
    public static NotificationEntry create() {
        return new NotificationEntry(genId());
    }

    /**
     * send this notification to StatusBar.
     *
     * default is false.
     *
     * @param send
     */
    public void sendToRemote(boolean send) {
        sendToTarget(send, TARGET_REMOTE);
    }

    /**
     * send this notification to NotifiationView.
     *
     * default is false.
     *
     * @param send
     */
    public void sendToLocalView(boolean send) {
        sendToTarget(send, TARGET_LOCAL);
    }

    /**
     * send this notification to NotificationView.
     *
     * default is false.
     *
     * @param send
     */
    public void sendToGlobalView(boolean send) {
        sendToTarget(send, TARGET_GLOBAL);
    }

    /**
     * send this notification to NotificationListener.
     *
     * default is true.
     *
     * @param send
     */
    public void sendToListener(boolean send) {
        mSendToListener = send;
    }

    /**
     * could be null.
     *
     * @param tag
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * a Notification with higher priority will display first.
     *
     * default is low.
     *
     * @param priority
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    /**
     * delay this notification.
     *
     * @param ms
     */
    public void setDelay(int ms) {
        this.delay = ms;
    }

    /**
     * each Notification can have its own layout different from others.
     *
     * if 0, default layout will be used.
     *
     * @param resId
     */
    public void setContentResId(int resId) {
        this.contentResId = resId;
    }

    /**
     * each Notification can have its own background color different from others.
     *
     * if 0, default background color will be used.
     *
     * @param color
     */
    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }

    /**
     * @param show
     */
    public void setShowWhen(boolean show) {
        this.showWhen = show;
    }

    /**
     * @param when
     */
    public void setWhen(long when) {
        this.whenLong = when;
    }

    /**
     * @param when
     */
    public void setWhen(CharSequence when) {
        this.whenFormatted = when;
    }

    /**
     * @param format
     * @param when
     */
    public void setWhen(CharSequence format, long when) {
        if (format == null) format = DEFAULT_DATE_FORMAT;
        this.whenFormatted = DateFormat.format(format, when);
    }

    /**
     * @param icon
     */
    public void setSmallIconResource(int icon) {
        this.smallIconRes = icon;
    }

    /**
     * @param icon
     */
    public void setLargeIconBitmap(Bitmap icon) {
        this.largeIconBitmap = icon;
    }

    /**
     * @param icon
     */
    public void setIconDrawable(Drawable icon) {
        this.iconDrawable = icon;
    }

    /**
     * @param title
     */
    public void setTitle(CharSequence title) {
        this.title = title;
    }

    /**
     * @param text
     */
    public void setText(CharSequence text) {
        this.text = text;
    }

    /**
     * only for {@link NotificationRemote}.
     *
     * if true, effects will be controlled by the system.
     * if false, effects will be handled by {@link NotificationEffect}.
     *
     * default is true.
     *
     * @param use
     */
    public void setUseSystemEffect(boolean use) {
        this.useSystemEffect = use;
    }

    /**
     * enable/disable the ringtone
     *
     * @param use
     */
    public void setUseRingtone(boolean use) {
        this.useRingtone = use;
    }

    /**
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
     * @param uri
     */
    public void setRingtone(Uri uri) {
        if (uri != null) {
            this.ringtoneUri = uri;
        }
    }

    /**
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
     * enable/disable vibration
     *
     * @param use
     */
    public void setUseVibrate(boolean use) {
        this.useVibrate = use;
    }

    /**
     * @param pattern
     * @param repeat
     */
    public void setVibrate(long[] pattern, int repeat) {
        if (pattern != null && repeat >= 0) {
            this.vibratePattern = pattern;
            this.vibrateRepeat = repeat;
        }
    }

    /**
     * @param ms
     */
    public void setVibrate(long ms) {
        if (ms > 0) {
            this.vibrateTime = ms;
        }
    }

    /**
     * @param extra
     */
    public void setExtra(Bundle extra) {
        this.extra = extra;
    }

    /**
     * @param obj
     */
    public void setObject(Object obj) {
        this.obj = obj;
    }

    /**
     * @param activityClass
     */
    public void setActivityClass(Class activityClass) {
        this.activityClass = activityClass;
    }

    /**
     * @param autoCancel
     */
    public void setAutoCancel(boolean autoCancel) {
        this.autoCancel = autoCancel;
    }

    // ==========================================

    public boolean isSentToRemote() {
        return isSentToTarget(TARGET_REMOTE);
    }

    public boolean isSentToLocalView() {
        return isSentToTarget(TARGET_LOCAL);
    }

    public boolean isSentToGlobalView() {
        return isSentToTarget(TARGET_GLOBAL);
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

    // targets
    static final int TARGET_REMOTE           = 0x00000001;
    static final int TARGET_LOCAL            = 0x00000002;
    static final int TARGET_GLOBAL           = 0x00000004;

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" Notification@").append(hashCode()).append(":").append(priority);
        sb.append(" [ targets=").append(mTargets);
        sb.append(", cancels=").append(mCancels);
        sb.append(" ] { tag=").append(tag);
        sb.append(", id=").append(ID);
        sb.append(", whenLong=").append(whenLong);
        sb.append(", whenFormatted=").append(whenFormatted);
        sb.append(", title=").append(title);
        sb.append(", text=").append(text);
        sb.append(", backgroundColor=").append(backgroundColor);
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
