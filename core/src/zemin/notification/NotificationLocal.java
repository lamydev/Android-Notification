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
 * Manage in-layout notification view.
 */
public class NotificationLocal extends NotificationHandler {

    public static boolean DBG;

    private NotificationView mView;

    /* package */ NotificationLocal(Context context, Looper looper) {
        super(context, NotificationEntry.TARGET_LOCAL, looper);
    }

    public void setView(NotificationView view) {
        mView = view;
        mView.setNotificationHandler(this);
    }

    public NotificationView getView() {
        return mView;
    }

    public void dismissView() {
        mView.dismiss();
    }

    public View findViewById(int resId) {
        return mView.findViewById(resId);
    }

    public boolean hasViewCallback() {
        return mView.hasCallback();
    }

    public void setViewCallback(NotificationView.Callback cb) {
        mView.setCallback(cb);
    }

    @Override
    protected void onCancel(NotificationEntry entry) {
        mView.onCancel(entry);
    }

    @Override
    protected void onCancelAll() {
        mView.onCancelAll();
    }

    @Override
    protected void onArrival(NotificationEntry entry) {
        if (mView == null) {
            Log.w(TAG, "NotificationView not found.");
            onSendIgnored(entry);
            return;
        }

        if (mView.getParent() == null) {
            throw new IllegalStateException("NotificationView should have a parent.");
        }

        if (!mView.hasCallback()) {
            mView.setCallback(DEFAULT_VIEWCALLBACK);
        }

        mView.onArrival(entry);
    }

    private final NotificationView.Callback DEFAULT_VIEWCALLBACK = new NotificationViewCallback();
}
