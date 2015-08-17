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
import android.os.Looper;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

/**
 * In-layout notification view.
 */
public class NotificationLocal extends NotificationHandler {

    public static final String SIMPLE_NAME = "Local";
    public static boolean DBG;

    /**
     * Implementaiton of {@link NotificationViewCallback} for in-layout {@link NotificationView}.
     */
    public static class ViewCallback extends NotificationViewCallback {

        @Override
        public int getContentViewDefaultLayoutId(NotificationView view) {
            return R.layout.notification_simple_2;
        }

        @Override
        public void onViewSetup(NotificationView view) {
            // nothing.
        }
    }

    private NotificationView mView;

    /* package */ NotificationLocal(Context context, Looper looper) {
        super(context, NotificationDelegater.LOCAL, looper);
    }

    /**
     * Set notification view.
     *
     * @param view
     */
    public void setView(NotificationView view) {
        view.initialize(this);
        mView = view;
    }

    /**
     * Set callback for notification view.
     *
     * @param cb
     */
    public void setViewCallback(NotificationViewCallback cb) {
        if (mView != null) {
            mView.setCallback(cb);
        }
    }

    /**
     * Enable/disable notification view.
     *
     * @param enable
     */
    public void setViewEnabled(boolean enable) {
        if (mView != null) {
            mView.setViewEnabled(enable);
        }
    }

    /**
     * Get notification view.
     *
     * @param NotificationView
     */
    public NotificationView getView() {
        return mView;
    }

    /**
     * Dismiss notification view.
     */
    public void dismissView() {
        if (mView != null) {
            mView.dismiss();
        }
    }

    @Override
    protected void onCancel(NotificationEntry entry) {
        if (mView != null) {
            mView.onCancel(entry);
        } else {
            onCancelFinished(entry);
        }
    }

    @Override
    protected void onCancelAll() {
        if (mView != null) {
            mView.onCancelAll();
        } else {
            onCancelAllFinished();
        }
    }

    @Override
    protected void onArrival(NotificationEntry entry) {
        if (mView == null) {
            Log.w(TAG, "NotificationView not found.");
            onSendIgnored(entry);
            return;
        }

        if (!mView.hasCallback()) {
            if (DBG) Log.v(TAG, "set default NotificationViewCallback.");
            mView.setCallback(new ViewCallback());
        }

        if (!mView.isViewEnabled()) {
            if (DBG) Log.v(TAG, "NotificationView is currently disabled.");
            onSendIgnored(entry);
            return;
        }

        mView.onArrival(entry);
    }


    @Override
    protected void onUpdate(NotificationEntry entry) {
        if (mView == null) {
            Log.w(TAG, "NotificationView not found.");
            onUpdateIgnored(entry);
            return;
        }

        if (!mView.isViewEnabled()) {
            if (DBG) Log.v(TAG, "NotificationView is currently disabled.");
            onUpdateIgnored(entry);
            return;
        }

        mView.onUpdate(entry);
    }

    @Override public String toSimpleString() { return SIMPLE_NAME; }
    @Override public String toString() { return SIMPLE_NAME; }
}
