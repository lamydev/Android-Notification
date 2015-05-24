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
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import zemin.notification.NotificationView.ChildView;

/**
 * Callback for {@link NotificationView}. This is the default implementation.
 */
public class NotificationViewCallback {

    private static final String TAG = "zemin.NotificationViewCallback";
    public static boolean DBG;

    /**
     * Called only once after this callback is set.
     *
     * @param view
     */
    public void onViewSetup(NotificationView view) {
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

        if (layoutId == R.layout.notification_simple ||
            layoutId == R.layout.notification_large_icon ||
            layoutId == R.layout.notification_full) {

            view.setChildViewSwitcher(ChildView.ICON, R.id.switcher_icon);
            view.setChildViewSwitcher(ChildView.TITLE, R.id.switcher_title);
            view.setChildViewSwitcher(ChildView.TEXT, R.id.switcher_text);
            view.setChildViewSwitcher(ChildView.WHEN, R.id.switcher_when);

        } else if (layoutId == R.layout.notification_simple_2) {

            view.setChildView(ChildView.ICON, R.id.icon);
            view.setChildView(ChildView.TITLE, R.id.title);
            view.setChildView(ChildView.TEXT, R.id.text);
            view.setChildView(ChildView.WHEN, R.id.when);
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

        final Drawable icon = entry.iconDrawable;
        final CharSequence title = entry.title;
        final CharSequence text = entry.text;
        final CharSequence when = entry.showWhen ? entry.whenFormatted : null;

        boolean titleChanged = true;
        boolean contentChanged = view.isContentLayoutChanged();
        NotificationEntry lastEntry = view.getLastNotification();

        if (layoutId == R.layout.notification_simple_2) {
            view.getContentViewSwitcher().start();
        }

        if (!contentChanged && title != null &&
            lastEntry != null && title.equals(lastEntry.title)) {
            titleChanged = false;
        }

        if (icon != null) {
            view.showChildView(ChildView.ICON);
            if (titleChanged) {
                view.setChildViewImageDrawable(ChildView.ICON, icon);
            }
        } else {
            view.hideChildView(ChildView.ICON);
        }

        if (title != null) {
            view.showChildView(ChildView.TITLE);
            if (titleChanged) {
                view.setChildViewText(ChildView.TITLE, title);
            }
        } else {
            view.hideChildView(ChildView.TITLE);
        }

        if (text != null) {
            view.showChildView(ChildView.TEXT);
            view.setChildViewText(ChildView.TEXT, text);
        } else {
            view.hideChildView(ChildView.TEXT);
        }

        if (when != null) {
            view.showChildView(ChildView.WHEN);
            view.setChildViewText(ChildView.WHEN, when);
        } else {
            view.hideChildView(ChildView.WHEN);
        }
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
    }
}
