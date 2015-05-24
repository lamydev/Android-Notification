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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * A floating notification window holds a notification root view {@link NotificationRootView},
 * where both view {@link NotificationView} and board {@link NotificationBoard} are sitting on.
 */
public class NotificationGlobal extends NotificationHandler {

    public static final String SIMPLE_NAME = "Global";
    public static boolean DBG;

    /**
     * Implementaiton of {@link NotificationViewCallback} for floating {@link NotificationView}.
     */
    public static class ViewCallback extends NotificationViewCallback {

        @Override
        public int getContentViewDefaultLayoutId(NotificationView view) {
            return R.layout.notification_full;
        }

        @Override
        public void onViewSetup(NotificationView view) {
            view.setContentMargin(0, 0, 0, 0);
            view.setDefaultBackgroundColor(0xff000000);
            view.setDefaultBackgroundAlpha(0x7f);
        }

        @Override
        public void onContentViewChanged(NotificationView view, View contentView, int layoutId) {
            super.onContentViewChanged(view, contentView, layoutId);

            NotificationView.ChildView childView;

            childView = view.getChildView(NotificationView.ChildView.TITLE);
            if (childView != null) {
                if (childView.viewSwitcher != null) {
                    for (int i = 0; i < 2; i++) {
                        TextView titleView = (TextView) childView.viewSwitcher.getChildAt(i);
                        titleView.setTextColor(0xffffffff);
                    }
                } else if (childView.view != null) {
                    TextView titleView = (TextView) childView.view;
                    titleView.setTextColor(0xffffffff);
                }
            }

            childView = view.getChildView(NotificationView.ChildView.TEXT);
            if (childView != null) {
                if (childView.viewSwitcher != null) {
                    for (int i = 0; i < 2; i++) {
                        TextView textView = (TextView) childView.viewSwitcher.getChildAt(i);
                        textView.setTextColor(0xffbdbdbd);
                    }
                } else if (childView.view != null) {
                    TextView textView = (TextView) childView.view;
                    textView.setTextColor(0xffbdbdbd);
                }
            }

            childView = view.getChildView(NotificationView.ChildView.WHEN);
            if (childView != null) {
                if (childView.viewSwitcher != null) {
                    for (int i = 0; i < 2; i++) {
                        TextView whenView = (TextView) childView.viewSwitcher.getChildAt(i);
                        whenView.setTextColor(0xffbdbdbd);
                    }
                } else if (childView.view != null) {
                    TextView whenView = (TextView) childView.view;
                    whenView.setTextColor(0xffbdbdbd);
                }
            }
        }
    }

    private NotificationView mView;
    private NotificationBoard mBoard;
    private NotificationWindow mWindow;

    /* package */ NotificationGlobal(Context context, Looper looper) {
        super(context, NotificationDelegater.GLOBAL, looper);

        mWindow = new NotificationWindow(context);
    }

    /**
     * Get notification view.
     *
     * @return NotificationView
     */
    public NotificationView getView() {
        return mView;
    }

    /**
     * Get notification board.
     *
     * @return NotificationBoard
     */
    public NotificationBoard getBoard() {
        return mBoard;
    }

    /**
     * Enable/disable notification view.
     *
     * @param enable
     */
    public void setViewEnabled(boolean enable) {
        final NotificationRootView root = mWindow.mRoot;
        root.setViewEnabled(enable);
        if (enable && mView == null) {
            mView = root.getView();
            mView.initialize(this);
            mView.addStateListener(new ViewStateListener());
        }
    }

    /**
     * Enable/disable notification board
     *
     * @param enable
     */
    public void setBoardEnabled(boolean enable) {
        final NotificationRootView root = mWindow.mRoot;
        root.setBoardEnabled(enable);
        if (enable && mBoard == null) {
            mBoard = root.getBoard();
            mBoard.addStateListener(new BoardStateListener());
        }
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
     * Set callback for notification board.
     *
     * @param cb
     */
    public void setBoardCallback(NotificationBoardCallback cb) {
        if (mBoard != null) {
            mBoard.setCallback(cb);
        }
    }

    /**
     * Dismiss notification view.
     */
    public void dismissView() {
        if (mView != null) {
            mView.dismiss();
        }
    }

    /**
     * Open notification board.
     */
    public void openBoard() {
        if (mBoard != null) {
            mBoard.open(true);
        }
    }

    /**
     * Close notification board.
     */
    public void closeBoard() {
        if (mBoard != null) {
            mBoard.close(true);
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

    private final class ViewStateListener extends NotificationView.SimpleStateListener {

        @Override
        public void onViewTicking(NotificationView view) {
            if (DBG) Log.v(TAG, "onViewTicking");
            if (mBoard == null || !mBoard.isShowing()) {
                mWindow.attach();
            }
        }

        @Override
        public void onViewDismiss(NotificationView view) {
            if (DBG) Log.v(TAG, "onViewDismiss");
            if (mBoard == null || !mBoard.isShowing()) {
                mWindow.detach();
            }
        }
    }

    private final class BoardStateListener extends NotificationBoard.SimpleStateListener {

        @Override
        public void onBoardPrepare(NotificationBoard board) {
            if (DBG) Log.v(TAG, "onBoardPrepare");
            if (mView == null || !mView.isTicking()) {
                mWindow.attach();
            }
            mWindow.expand(true);
        }

        @Override
        public void onBoardEndOpen(NotificationBoard board) {
            if (DBG) Log.v(TAG, "onBoardEndOpen");
        }

        @Override
        public void onBoardEndClose(NotificationBoard board) {
            if (DBG) Log.v(TAG, "onBoardEndClose");
            if (mView == null || !mView.isTicking()) {
                mWindow.detach();
            }
            mWindow.expand(false);
        }
    }

    private final class NotificationWindow extends FrameLayout {

        private WindowManager mWindowManager;
        private WindowManager.LayoutParams mLayoutParams;
        private NotificationRootView mRoot;
        private NotificationBoard mBoard;
        private NotificationView mView;
        private boolean mAttached;

        NotificationWindow(Context context) {
            super(context);

            mWindowManager = (WindowManager)
                mContext.getSystemService(Context.WINDOW_SERVICE);

            mRoot = new NotificationRootView(mContext);
            addView(mRoot);

            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.registerReceiver(mReceiver, filter);
        }

        void expand(boolean expand) {
            WindowManager.LayoutParams lp = getWindowLayoutParams();
            lp.height = expand ? WindowManager.LayoutParams.MATCH_PARENT :
                WindowManager.LayoutParams.WRAP_CONTENT;
            mWindowManager.updateViewLayout(this, lp);
        }

        private void attach() {
            if (!mAttached) mWindowManager.addView(this, getWindowLayoutParams());
        }

        private void detach() {
            if (mAttached) mWindowManager.removeView(this);
        }

        private void onBackKey() {
            mRoot.onBackKey();
        }

        private void onHomeKey() {
            mRoot.onHomeKey();
        }

        private WindowManager.LayoutParams getWindowLayoutParams() {
            if (mLayoutParams == null) {
                mLayoutParams = new WindowManager.LayoutParams();
                mLayoutParams.gravity = Gravity.TOP;
                mLayoutParams.setTitle(getClass().getSimpleName());
                mLayoutParams.packageName = mContext.getPackageName();
                mLayoutParams.type = WindowManager.LayoutParams.TYPE_TOAST;
                mLayoutParams.format = PixelFormat.TRANSLUCENT;
                mLayoutParams.flags |=
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
                mLayoutParams.x = 0;
                mLayoutParams.y = 0;
                mLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            }
            return mLayoutParams;
        }

        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    if ("homekey".equals(intent.getStringExtra("reason"))) {
                        onHomeKey();
                    }
                }
            };

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK &&
                event.getAction() == KeyEvent.ACTION_UP) {
                onBackKey();
                return true;
            }
            return super.dispatchKeyEvent(event);
        }

        @Override
        public void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (DBG) Log.v(TAG, "attached.");
            mAttached = true;
        }

        @Override
        public void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (DBG) Log.v(TAG, "detached.");
            mAttached = false;
        }
    }

    @Override public String toSimpleString() { return SIMPLE_NAME; }
    @Override public String toString() { return SIMPLE_NAME; }
}
