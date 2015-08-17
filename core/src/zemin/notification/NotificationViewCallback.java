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

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;

/**
 * Callback for {@link NotificationView}. This is the default implementation.
 */
public class NotificationViewCallback {

    private static final String TAG = "zemin.NotificationViewCallback";
    public static boolean DBG;

    public static final String ICON       = "icon";
    public static final String TITLE      = "title";
    public static final String TEXT       = "text";
    public static final String WHEN       = "when";
    // public static final int PROGRESS   = 4;
    // add more..

    /**
     * Called only once after this callback is set.
     *
     * @param view
     */
    public void onViewSetup(NotificationView view) {
        if (DBG) Log.v(TAG, "onViewSetup");

        view.setCornerRadius(8.0f);
        view.setContentMargin(50, 50, 50, 50);
        view.setShadowEnabled(true);
    }

    /**
     * Called to get the default layoutId.
     *
     * @param view
     * @return int
     */
    public int getContentViewDefaultLayoutId(NotificationView view) {
        return R.layout.notification_full;
    }

    /**
     * Called when content view is changed. All child-views were cleared due the
     * change of content view. You need to re-setup the associated child-views.
     *
     * @param view
     * @param contentView
     * @param layoutId
     */
    public void onContentViewChanged(NotificationView view, View contentView, int layoutId) {
        if (DBG) Log.v(TAG, "onContentViewChanged");

        ChildViewManager mgr = view.getChildViewManager();

        if (layoutId == R.layout.notification_simple ||
            layoutId == R.layout.notification_large_icon ||
            layoutId == R.layout.notification_full) {

            view.setNotificationTransitionEnabled(false);

            mgr.setView(ICON, contentView.findViewById(R.id.switcher_icon));
            mgr.setView(TITLE, contentView.findViewById(R.id.switcher_title));
            mgr.setView(TEXT, contentView.findViewById(R.id.switcher_text));
            mgr.setView(WHEN, contentView.findViewById(R.id.switcher_when));

        } else if (layoutId == R.layout.notification_simple_2) {

            view.setNotificationTransitionEnabled(true);

            mgr.setView(ICON, contentView.findViewById(R.id.icon));
            mgr.setView(TITLE, contentView.findViewById(R.id.title));
            mgr.setView(TEXT, contentView.findViewById(R.id.text));
            mgr.setView(WHEN, contentView.findViewById(R.id.when));
        }
    }

    /**
     * Called when a notification is being displayed. This is the place to update
     * the user interface of child-views for the new notification.
     *
     * @param view
     * @param contentView
     * @param entry
     * @param layoutId
     */
    public void onShowNotification(NotificationView view, View contentView, NotificationEntry entry, int layoutId) {
        if (DBG) Log.v(TAG, "onShowNotification - " + entry.ID);

        final Drawable icon = entry.iconDrawable;
        final CharSequence title = entry.title;
        final CharSequence text = entry.text;
        final CharSequence when = entry.showWhen ? entry.whenFormatted : null;

        ChildViewManager mgr = view.getChildViewManager();

        if (layoutId == R.layout.notification_simple ||
            layoutId == R.layout.notification_large_icon ||
            layoutId == R.layout.notification_full) {

            boolean titleChanged = true;
            boolean contentChanged = view.isContentLayoutChanged();
            NotificationEntry lastEntry = view.getLastNotification();

            if (!contentChanged && title != null &&
                lastEntry != null && title.equals(lastEntry.title)) {
                titleChanged = false;
            }

            mgr.setImageDrawable(ICON, icon, titleChanged);
            mgr.setText(TITLE, title, titleChanged);
            mgr.setText(TEXT, text);
            mgr.setText(WHEN, when);

        } else if (layoutId == R.layout.notification_simple_2) {

            mgr.setImageDrawable(ICON, icon);
            mgr.setText(TITLE, title);
            mgr.setText(TEXT, text);
            mgr.setText(WHEN, when);
        }
    }

    /**
     * Called when a notification is being updated.
     *
     * @param view
     * @param contentView
     * @param entry
     * @param layoutId
     */
    public void onUpdateNotification(NotificationView view, View contentView, NotificationEntry entry, int layoutId) {
        if (DBG) Log.v(TAG, "onUpdateNotification - " + entry.ID);

        final Drawable icon = entry.iconDrawable;
        final CharSequence title = entry.title;
        final CharSequence text = entry.text;
        final CharSequence when = entry.showWhen ? entry.whenFormatted : null;

        ChildViewManager mgr = view.getChildViewManager();

        mgr.setImageDrawable(ICON, icon, false);
        mgr.setText(TITLE, title, false);
        mgr.setText(TEXT, text, false);
        mgr.setText(WHEN, when, false);
    }

    /**
     * Called when the view has been clicked.
     *
     * @param view
     * @param contentView
     * @param entry
     * @return boolean true, if handled.
     */
    public void onClickContentView(NotificationView view, View contentView, NotificationEntry entry) {
        if (DBG) Log.v(TAG, "onClickContentView - " + entry.ID);
    }
}
