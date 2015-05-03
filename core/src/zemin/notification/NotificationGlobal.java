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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Looper;
import android.util.Log;
import android.view.View;

//
// NotificationView handler
//
// @author Zemin Liu
//
public class NotificationGlobal extends NotificationHandler {

    public static boolean DBG;

    private NotificationView mView;

    /* package */ NotificationGlobal(Context context, Looper looper) {
        super(context, NotificationEntry.TARGET_GLOBAL, looper);

        mView = new NotificationView(context);
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
        if (!mView.hasCallback()) {
            mView.setCallback(DEFAULT_VIEWCALLBACK);
        }
        mView.onArrival(entry);
    }

    private final NotificationView.Callback DEFAULT_VIEWCALLBACK = new ViewCallback();

    public class ViewCallback extends NotificationViewCallback {

        @Override
        public int getDefaultContentResId() {
            return R.layout.notification_full;
        }

        @Override
        public void onSetupView(NotificationView view) {
            final float cornerRadius = NotificationView.DEFAULT_CORNER_RADIUS;
            final Drawable contentBackground = view.getContentBackground();
            final int numLayer = 5;
            final int inset_lr_step = 1;
            final int inset_b_step = 2;
            final int inset_t = 5;
            final Drawable[] layers = new Drawable[numLayer];
            for (int i = numLayer - 2; i >= 0; i--) {
                GradientDrawable blur = new GradientDrawable();
                blur.setColor((1<<(i + 28)) | 0x00cccccc);
                blur.setCornerRadius(cornerRadius);
                layers[i] = blur;
            }
            layers[numLayer - 1] = contentBackground;

            LayerDrawable layerBackground = new LayerDrawable(layers);
            for (int i = 0, last = numLayer - 1; i <= last; i++) {
                final int inset_lr = i * inset_lr_step;
                final int inset_b = i * inset_b_step;
                if (i == 0) {
                    layerBackground.setLayerInset(i, 0, inset_t, 0, 0);
                } else if (i == last) {
                    layerBackground.setLayerInset(i, inset_lr, 0, inset_lr, inset_b);
                } else {
                    layerBackground.setLayerInset(i, inset_lr, inset_t, inset_lr, inset_b);
                }
            }
            view.setContentBackground(layerBackground);

            final int padding = (numLayer - 1) * inset_b_step;
            view.setContentPadding(padding, 0, padding, padding);
            view.setCornerRadius(cornerRadius);
        }

        @Override
        public void onContentViewChanged(NotificationView view, View contentView, int contentResId) {
            super.onContentViewChanged(view, contentView, contentResId);
        }

        @Override
        public void onShowNotification(NotificationView view, NotificationEntry entry, int contentResId) {
            super.onShowNotification(view, entry, contentResId);
        }
    }
}
