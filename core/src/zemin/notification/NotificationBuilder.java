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
import android.util.Log;
import android.view.View;

/**
 * A builder class for {@link NotificationEntry} objects.
 */
public class NotificationBuilder {

    private static final String TAG = "zemin.NotificationBuilder";
    public static boolean DBG;

    /**
     * Create an empty {@link NotificationEntry}.
     *
     * @return NotificationEntry
     */
    public static NotificationEntry emptyNotification() {
        return NotificationEntry.create();
    }

    /**
     * Get builder v1 for constructing local notifications.
     *
     * @see NotificationLocal
     *
     * @return V1
     */
    public static V1 local() { V1 v = new V1(); v.N.sendToLocalView(true); return v; }

    /**
     * Get builder v1 for constructing global notifications.
     *
     * @see NotificationGlobal
     *
     * @return V1
     */
    public static V1 global() { V1 v = new V1(); v.N.sendToGlobalView(true); return v; }

    /**
     * Get builder v2 for constructing remote notifications.
     *
     * @see NotificationRemote
     *
     * @return V2
     */
    public static V2 remote() { V2 v = new V2(); v.N.sendToRemote(true); return v; }

    /**
     * Build {@link NotificationEntry} objects to display on {@link NotificationView}.
     *
     * @see NotificationView
     * @see NotificationLocal
     * @see NotificationGlobal
     */
    public static class V1 {
        protected NotificationEntry N;
        public V1() { N = NotificationEntry.create(); }
        public NotificationEntry getNotification() { return N; }

        /**
         * Set tag.
         *
         * @param tag
         */
        public V1 setTag(String tag) {
            N.setTag(tag);
            return this;
        }

        /**
         * Set priority.
         *
         * @param p
         */
        public V1 setPriority(NotificationEntry.Priority p) {
            N.setPriority(p);
            return this;
        }

        /**
         * Set whether this is an "ongoing" notification.
         *
         * @param ongoing
         */
        public V1 setOngoing(boolean ongoing) {
            N.setOngoing(ongoing);
            return this;
        }

        /**
         * Set delay before this notification get delivered.
         *
         * @param ms
         */
        public V1 setDelay(int ms) {
            N.setDelay(ms);
            return this;
        }

        /**
         * Set layout resource for customizing the user interface.
         *
         * @param resId
         */
        public V1 setLayoutId(int resId) {
            N.setLayoutId(resId);
            return this;
        }

        /**
         * Set background color.
         *
         * @param color
         */
        public V1 setBackgroundColor(int color) {
            N.setBackgroundColor(color);
            return this;
        }

        /**
         * Set opacity of the background.
         *
         * @param alpha
         */
        public V1 setBackgroundAlpha(int alpha) {
            N.setBackgroundAlpha(alpha);
            return this;
        }

        /**
         * Set whether the timestamp set with {@link #setWhen} is shown.
         *
         * @param show
         */
        public V1 setShowWhen(boolean show) {
            N.setShowWhen(show);
            return this;
        }

        /**
         * Set a timestamp pertaining to this notification.
         *
         * @param when
         */
        public V1 setWhen(long when) {
            N.setWhen(when);
            return this;
        }

        /**
         * Set a timestamp pertaining to this notification.
         *
         * @param when
         */
        public V1 setWhen(CharSequence when) {
            N.setWhen(when);
            return this;
        }

        /**
         * Set a timestamp pertaining to this notification.
         *
         * @param format
         * @param when
         */
        public V1 setWhen(CharSequence format, long when) {
            N.setWhen(format, when);
            return this;
        }

        /**
         * Set small icon resource.
         *
         * @param resId
         */
        public V1 setSmallIconResource(int resId) {
            N.setSmallIconResource(resId);
            return this;
        }

        /**
         * Set icon drawable.
         *
         * @param drawable
         */
        public V1 setIconDrawable(Drawable drawable) {
            N.setIconDrawable(drawable);
            return this;
        }

        /**
         * Set notification title.
         *
         * @param title
         */
        public V1 setTitle(CharSequence title) {
            N.setTitle(title);
            return this;
        }

        /**
         * Set notification text.
         *
         * @param text
         */
        public V1 setText(CharSequence text) {
            N.setText(text);
            return this;
        }

        /**
         * Set whether to play ringtone.
         *
         * @param play
         */
        public V1 setPlayRingtone(boolean play) {
            N.setPlayRingtone(play);
            return this;
        }

        /**
         * Set ringtone resource.
         *
         * @param context
         * @param resId
         */
        public V1 setRingtone(Context context, int resId) {
            N.setRingtone(context, resId);
            return this;
        }

        /**
         * Set ringtone uri.
         *
         * @param uri
         */
        public V1 setRingtone(Uri uri) {
            N.setRingtone(uri);
            return this;
        }

        /**
         * Set ringtone file path.
         *
         * @param filepath
         */
        public V1 setRingtone(String filepath) {
            N.setRingtone(filepath);
            return this;
        }

        /**
         * Set whether to use vibration.
         *
         * @param use
         */
        public V1 setUseVibration(boolean use) {
            N.setUseVibration(use);
            return this;
        }

        /**
         * Set vibration pattern.
         *
         * @param pattern
         * @param repeat
         */
        public V1 setVibrate(long[] pattern, int repeat) {
            N.setVibrate(pattern, repeat);
            return this;
        }

        /**
         * Set vibration period.
         *
         * @param ms
         */
        public V1 setVibrate(long ms) {
            N.setVibrate(ms);
            return this;
        }

        /**
         * Set metadata.
         *
         * @param extra
         */
        public V1 setExtra(Bundle extra) {
            N.setExtra(extra);
            return this;
        }

        /**
         * Set extra object.
         *
         * @param obj
         */
        public V1 setObject(Object obj) {
            N.setObject(obj);
            return this;
        }

        /**
         * Set whether to cancel the notification automatically when the user touches it.
         *
         * @param autoCancel
         */
        public V1 setAutoCancel(boolean autoCancel) {
            N.setAutoCancel(autoCancel);
            return this;
        }

        /**
         * Set an object of {@link View#OnClickListener} which will be invoked when the user clicks on it.
         *
         * @param l
         */
        public V1 setOnClickListener(View.OnClickListener l) {
            N.setOnClickListener(l);
            return this;
        }
    }

    /**
     * Build {@link NotificationEntry} objects to be sent to status-bar.
     *
     * @see NotificationRemote
     */
    public static class V2 {
        protected NotificationEntry N;
        public V2() { N = NotificationEntry.create(); }
        public NotificationEntry getNotification() { return N; }

        /**
         * Set tag.
         *
         * @param tag
         */
        public V2 setTag(String tag) {
            N.setTag(tag);
            return this;
        }

        /**
         * Set whether this is an "ongoing" notification.
         *
         * @param ongoing
         */
        public V2 setOngoing(boolean ongoing) {
            N.setOngoing(ongoing);
            return this;
        }

        /**
         * Set delay before this notification get delivered.
         *
         * @param ms
         */
        public V2 setDelay(int ms) {
            N.setDelay(ms);
            return this;
        }

        /**
         * Set layout resource for customizing the user interface.
         *
         * @param resId
         */
        public V2 setLayoutId(int resId) {
            N.setLayoutId(resId);
            return this;
        }

        /**
         * Set whether the timestamp set with {@link #setWhen} is shown.
         *
         * @param show
         */
        public V2 setShowWhen(boolean show) {
            N.setShowWhen(show);
            return this;
        }

        /**
         * Set a timestamp pertaining to this notification.
         *
         * @param when
         */
        public V2 setWhen(long when) {
            N.setWhen(when);
            return this;
        }

        /**
         * Set small icon resource.
         *
         * @param resId
         */
        public V2 setSmallIconResource(int resId) {
            N.setSmallIconResource(resId);
            return this;
        }

        /**
         * Set large icon bitmap.
         *
         * @param bitmap
         */
        public V2 setLargeIconBitmap(Bitmap bitmap) {
            N.setLargeIconBitmap(bitmap);
            return this;
        }

        /**
         * Set the text that is displayed in the status-bar when the notification first arrives.
         *
         * @param tickerText
         */
        public V2 setTicker(CharSequence tickerText) {
            N.setTicker(tickerText);
            return this;
        }

        /**
         * Set notification title.
         *
         * @param title
         */
        public V2 setTitle(CharSequence title) {
            N.setTitle(title);
            return this;
        }

        /**
         * Set notification text.
         *
         * @param text
         */
        public V2 setText(CharSequence text) {
            N.setText(text);
            return this;
        }

        /**
         * Set whether to use system effect.
         *
         * @param use
         */
        public V2 setUseSystemEffect(boolean use) {
            N.setUseSystemEffect(use);
            return this;
        }

        /**
         * Set whether to play ringtone.
         *
         * @param play
         */
        public V2 setPlayRingtone(boolean play) {
            N.setPlayRingtone(play);
            return this;
        }

        /**
         * Set ringtone resource.
         *
         * @param context
         * @param resId
         */
        public V2 setRingtone(Context context, int resId) {
            N.setRingtone(context, resId);
            return this;
        }

        /**
         * Set ringtone uri.
         *
         * @param uri
         */
        public V2 setRingtone(Uri uri) {
            N.setRingtone(uri);
            return this;
        }

        /**
         * Set ringtone file path.
         *
         * @param filepath
         */
        public V2 setRingtone(String filepath) {
            N.setRingtone(filepath);
            return this;
        }

        /**
         * Set whether to use vibration.
         *
         * @param use
         */
        public V2 setUseVibration(boolean use) {
            N.setUseVibration(use);
            return this;
        }

        /**
         * Set vibration pattern.
         *
         * @param pattern
         * @param repeat
         */
        public V2 setVibrate(long[] pattern) {
            N.setVibrate(pattern, 0);
            return this;
        }

        /**
         * Set metadata.
         *
         * @param extra
         */
        public V2 setExtra(Bundle extra) {
            N.setExtra(extra);
            return this;
        }

        /**
         * Set activity class. Activity will be launched when the user touches it.
         *
         * @param activityClass
         */
        public V2 setActivityClass(Class activityClass) {
            N.setActivityClass(activityClass);
            return this;
        }

        /**
         * Set whether to cancel the notification automatically when the user touches it.
         *
         * @param autoCancel
         */
        public V2 setAutoCancel(boolean autoCancel) {
            N.setAutoCancel(autoCancel);
            return this;
        }
    }
}
