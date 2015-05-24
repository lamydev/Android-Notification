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
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;

/**
 * Effects:
 * 1) ringtone
 * 2) vibration (require manifest permission {@link android.Manifest.permission#VIBRATE})
 */
public class NotificationEffect {

    private static final String TAG = "zemin.NotificationEffect";
    public static boolean DBG;

    final Object mLock = new Object();
    protected Context mContext;
    protected boolean mEnabled = true;
    private int mConsumer;

    // ringtone
    protected AudioManager mAudioManager;
    protected Ringtone mRingtone;
    protected Uri mRingtoneUri;
    private boolean mRingtoneAuto = true;
    private boolean mRingtoneEnabled = true;
    private int mRingtoneRes;

    // vibrate
    protected Vibrator mVibrator;
    protected boolean mHasVibrator = true;
    protected boolean mIsVibrating;
    private boolean mVibratorAuto = true;
    private boolean mVibratorEnabled = true;
    private long mVibrateTime;

    public NotificationEffect(Context context) {
        mContext = context;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean enable) {
        if (enable && !mEnabled) {
            if (DBG) Log.d(TAG, "Notification effect is now enabled.");
            mEnabled = true;
        } else if (!enable && mEnabled) {
            if (DBG) Log.d(TAG, "Notification effect is now disabled.");
            mEnabled = false;
            cancel();
        }
    }

    public void setConsumer(int consumer) {
        if (DBG) Log.d(TAG, "set consumer=" + consumer);
        mConsumer = consumer;
    }

    public int getConsumer() {
        return mConsumer;
    }

    public boolean isConsumer(int consumer) {
        return mConsumer == consumer;
    }

    public boolean isRingtoneEnabled() {
        return mRingtoneEnabled;
    }

    public void enableRingtone(boolean enable) {
        if (enable && !mRingtoneEnabled) {
            if (DBG) Log.d(TAG, "ringtone is now enabled.");
            mRingtoneEnabled = true;
        } else if (!enable && mRingtoneEnabled) {
            if (DBG) Log.d(TAG, "ringtone is now disabled.");
            mRingtoneEnabled = false;
        }
    }

    public void setRingtoneResource(int resId) {
        mRingtoneRes = resId;
    }

    public int getRingtoneResource() {
        return mRingtoneRes;
    }

    /**
     * if both "mRingtoneEnabled" and "mRingtoneAuto" are true,
     * ringtone will still be played even though {@link NotificationEntry} does not set its ringtone.
     */
    public void setRingtoneAuto(boolean auto) {
        mRingtoneAuto = auto;
    }

    public boolean isRingtoneAuto() {
        return mRingtoneAuto;
    }

    public boolean isVibratorEnabled() {
        return mVibratorEnabled;
    }

    public void enableVibrator(boolean enable) {
        if (enable && !mVibratorEnabled) {
            if (DBG) Log.d(TAG, "vibrator is now enabled.");
            mVibratorEnabled = true;
        } else if (!enable && mVibratorEnabled) {
            if (DBG) Log.d(TAG, "vibrator is now disabled.");
            mVibratorEnabled = false;
        }
    }

    public void setVibrateTime(long time) {
        if (time > 0L) mVibrateTime = time;
    }

    /**
     * if both "mVibratorEnabled" and "mVibratorAuto" are true,
     * vibrator will sill vibrate the device even though {@link NotificationEntry} does not set its vibrate time.
     */
    public void setVibratorAuto(boolean auto) {
        mVibratorAuto = auto;
    }

    public boolean isVibratorAuto() {
        return mVibratorAuto;
    }

    public void play(NotificationEntry entry) {
        ringtone(entry);
        vibrate(entry);
    }

    // ringtone
    public void ringtone(NotificationEntry entry) {
        if (!mEnabled) {
            Log.w(TAG, "failed to play ringtone. effect disabled.");
            return;
        }
        if (mRingtoneEnabled && mRingtoneAuto &&
            entry.playRingtone && entry.ringtoneUri == null) {
            // default ringtone
            if (DBG) Log.d(TAG, "[default] ringtone");
            entry.setRingtone(mContext, mRingtoneRes);
        }
        if (entry.playRingtone) {
            Uri ringtone = entry.ringtoneUri;
            if (ringtone == null) {
                Log.e(TAG, "ringtone uri not found.");
                return;
            }

            Ringtone r = null;
            if (mRingtone != null && mRingtoneUri != null && mRingtoneUri.equals(ringtone)) {
                r = mRingtone;
            } else {
                r = RingtoneManager.getRingtone(mContext, ringtone);
                mRingtone = r;
                mRingtoneUri = ringtone;
            }

            if (r == null) {
                Log.e(TAG, "ringtone not found.");
                return;
            }

            if (mAudioManager == null) {
                mAudioManager = (AudioManager)
                    mContext.getSystemService(Context.AUDIO_SERVICE);
            }

            if (mAudioManager != null &&
                mAudioManager.getStreamVolume(r.getStreamType()) == 0) {
                Log.i(TAG, "volume muted. won't play any ringtone.");
                return;
            }

            if (DBG) Log.d(TAG, "ringtone - " + entry.ringtoneUri);

            r.play();
        }
    }

    // vibrate
    public void vibrate(NotificationEntry entry) {
        if (!mEnabled) {
            Log.w(TAG, "failed to vibrate. effect disabled.");
            return;
        }
        if (mHasVibrator) {
            if (mVibrator == null) {
                mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= 11) {
                    mHasVibrator = mVibrator != null ? mVibrator.hasVibrator() : false;
                }
            }

            if (!mHasVibrator) {
                Log.w(TAG, "don't have vibrator on this device.");
                return;
            }

            if (mVibratorEnabled && mVibratorAuto &&
                entry.useVibration && entry.vibrateTime <= 0 &&
                entry.vibratePattern == null) {
                // default vibration
                if (DBG) Log.d(TAG, "[default] vibrate - " + mVibrateTime + " ms");
                entry.setVibrate(mVibrateTime);
            }

            cancelVibration();
            if (entry.useVibration) {
                if (entry.vibratePattern != null) {
                    if (DBG) Log.d(TAG, "vibrate - " + entry.vibratePattern);
                    mVibrator.vibrate(entry.vibratePattern, entry.vibrateRepeat);
                    mIsVibrating = true;
                } else if (entry.vibrateTime > 0) {
                    if (DBG) Log.d(TAG, "vibrate - " + entry.vibrateTime);
                    mVibrator.vibrate(entry.vibrateTime);
                    mIsVibrating = true;
                } else {
                    Log.e(TAG, "no vibrate.");
                }
            }
        }
    }

    public void cancel() {
        if (DBG) Log.d(TAG, "cancel");
        cancelRingtone();
        cancelVibration();
        mConsumer = 0;
    }

    public void cancelRingtone() {
        if (mRingtone != null && mRingtone.isPlaying()) {
            if (DBG) Log.d(TAG, "cancel ringtone");
            mRingtone.stop();
        }
    }

    public void cancelVibration() {
        if (mVibrator != null && mIsVibrating) {
            if (DBG) Log.d(TAG, "cancel vibration");
            mVibrator.cancel();
            mIsVibrating = false;
        }
    }
}
