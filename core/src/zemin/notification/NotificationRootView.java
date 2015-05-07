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
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

/**
 * root view
 */
public class NotificationRootView extends FrameLayout {

    private static final String TAG = "zemin.NotificationRootView";
    public static boolean DBG;

    public static final int DEFAULT_WIDTH = 1000;
    public static final int DEFAULT_HEIGHT = 65;

    public static final int PADDING_TOP = 50;
    public static final int PADDING_LEFT = 50;
    public static final int PADDING_RIGHT = 50;
    public static final int PADDING_BOTTOM = 50;

    private NotificationView.LifecycleListener mLifecycleListener;

    public NotificationRootView(Context context) {
        super(context);
    }

    public NotificationRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void addNotificationView(NotificationView view, LayoutParams lp) {
        if (view != null) {
            addView(view, lp);
            view.setLifecycleListener(LIFECYCLE_LISTENER);
        }
    }

    public void removeNotificationView(NotificationView view) {
        if (view != null) {
            removeView(view);
        }
    }

    public void setLifecycleListener(NotificationView.LifecycleListener l) {
        mLifecycleListener = l;
    }

    private final NotificationView.LifecycleListener LIFECYCLE_LISTENER =
        new NotificationView.LifecycleListener() {

            @Override
            public void onShow() {
                if (mLifecycleListener != null) {
                    mLifecycleListener.onShow();
                }
            }

            @Override
            public void onDismiss() {
                if (mLifecycleListener != null) {
                    mLifecycleListener.onDismiss();
                }
            }
        };
}
