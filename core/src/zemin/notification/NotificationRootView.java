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

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.support.v4.view.GestureDetectorCompat;

/**
 * Root view handling both {@link NotificationView} and {@link NotificationBoard}.
 */
public class NotificationRootView extends FrameLayout
        implements GestureDetector.OnGestureListener,
                   GestureDetector.OnDoubleTapListener {

    private static final String TAG = "zemin.NotificationRootView";
    public static boolean DBG;

    private NotificationView mView;
    private NotificationBoard mBoard;
    private GestureDetectorCompat mGestureDetector;
    private GestureDetector.OnGestureListener mGestureConsumer;

    public NotificationRootView(Context context) {
        super(context);
    }

    public NotificationRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
     * Get notification board.
     *
     * @param NotificationBoard
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
        if (enable && mView == null) {
            mView = makeView();
        }
        if (mView != null) {
            mView.setViewEnabled(enable);
        }
    }

    /**
     * Enable/disable notification board.
     *
     * @param enable
     */
    public void setBoardEnabled(boolean enable) {
        if (enable && mBoard == null) {
            mBoard = makeBoard();
        }
        if (mBoard != null) {
            mBoard.setBoardEnabled(enable);
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

    private NotificationView makeView() {
        NotificationView view = new NotificationView(getContext());
        view.setClipChildren(false);
        view.setClipToPadding(false);
        // view.setDismissOnHomeKey(true);
        int index = getChildCount() > 1 ? 1 : 0;
        addView(view, index, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return view;
    }

    private NotificationBoard makeBoard() {
        NotificationBoard board = new NotificationBoard(getContext());
        BoardListener listener = new BoardListener();
        board.addStateListener(listener);
        board.setVisibility(GONE);
        addView(board, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        return board;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean intercept = false;
        if (mBoard != null && !mBoard.isShowing()) {
            intercept = true;
        }
        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mBoard != null) {
            if (mBoard.isOpened()) {
                return mBoard.onTouchEvent(event);
            }
        } else if (mView != null) {
            return mView.onTouchEvent(event);
        }

        if (mGestureDetector == null) {
            mGestureDetector = new GestureDetectorCompat(getContext(), this);
        }

        boolean handled = mGestureDetector.onTouchEvent(event);
        switch (event.getAction()) {
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            onUpOrCancel(event, handled);
            break;
        }

        return handled ? handled : super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event) {
        mGestureConsumer = null;
        if (mBoard != null && mBoard.isBoardEnabled()) {
            if (mView != null && mView.isViewEnabled() && mView.isTicking()) {
                mBoard.setInitialTouchArea(
                    mView.getLeft(), mView.getTop(), mView.getRight(), mView.getBottom());
            } else {
                mBoard.setInitialTouchArea(0, 0, 0, 0);
            }
            mBoard.onDown(event);
        }
        if (mView != null && mView.isViewEnabled()) {
            mView.onDown(event);
        }
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        if (mView != null && mView.isViewEnabled()) {
            mView.onShowPress(event);
        }
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        return mView != null && mView.isViewEnabled() ?
            mView.onSingleTapUp(event) : false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        return mView != null && mView.isViewEnabled() ?
            mView.onSingleTapConfirmed(event) : false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        return mView != null && mView.isViewEnabled() ?
            mView.onDoubleTap(event) : false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        return mView != null && mView.isViewEnabled() ?
            mView.onDoubleTapEvent(event) : false;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        if (mView != null && mView.isViewEnabled()) {
            mView.onLongPress(event);
        }
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        if (mGestureConsumer != null) {
            return mGestureConsumer.onScroll(event1, event2, distanceX, distanceY);
        }
        if (mBoard != null && mBoard.isBoardEnabled() && mBoard.onScroll(event1, event2, distanceX, distanceY)) {
            mGestureConsumer = mBoard;
            return true;
        }
        if (mView != null && mView.isViewEnabled() && mView.onScroll(event1, event2, distanceX, distanceY)) {
            mGestureConsumer = mView;
            return true;
        }
        return false;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        if (mGestureConsumer != null) {
            return mGestureConsumer.onFling(event1, event2, velocityX, velocityY);
        }
        if (mBoard != null && mBoard.isBoardEnabled() && mBoard.onFling(event1, event2, velocityX, velocityY)) {
            return true;
        }
        if (mView != null && mView.isViewEnabled() && mView.onFling(event1, event2, velocityX, velocityY)) {
            return true;
        }
        return false;
    }

    private void onUpOrCancel(MotionEvent event, boolean handled) {
        mGestureConsumer = null;
        if (mBoard != null && mBoard.isBoardEnabled()) {
            mBoard.onUpOrCancel(event, handled);
        }
        if (mView != null && mView.isViewEnabled()) {
            mView.onUpOrCancel(event, handled);
        }
    }

    public void onBackKey() {
        if (mBoard != null) {
            mBoard.onBackKey();
        }
        if (mView != null) {
            mView.onBackKey();
        }
    }

    public void onHomeKey() {
        if (mBoard != null) {
            mBoard.onHomeKey();
        }
        if (mView != null) {
            mView.onHomeKey();
        }
    }

    private class BoardListener extends NotificationBoard.SimpleStateListener {

        @Override
        public void onBoardPrepare(NotificationBoard board) {
            if (mView != null) {
                mView.setViewEnabled(false);
            }
        }

        @Override
        public void onBoardStartOpen(NotificationBoard board) {
            if (mView != null && mView.isTicking()) {
                mView.pause();
                mView.animateContentViewRotationX(
                    -90.0f, 0.0f, mViewDismissAnimatorListener, board.getOpenTransitionTime());
            }
        }

        @Override
        public void onBoardStartClose(NotificationBoard board) {
            if (mView != null) {
                mView.setViewEnabled(true);
                if (mView.isTicking()) {
                    if (mView.getContentViewRotationX() < -90.0f) {
                        mView.setContentViewRotationX(-90.0f);
                    }
                    mView.setContentViewVisibility(VISIBLE);
                    mView.animateContentViewRotationX(
                        0.0f, 1.0f, mViewShowAnimatorListener, board.getCloseTransitionTime());
                }
            }
        }

        @Override
        public void onBoardEndClose(NotificationBoard board) {
            if (mView != null) {
                mView.resume();
            }
        }

        @Override
        public void onBoardTranslationY(NotificationBoard board, float y) {
            if (mView != null && mView.isTicking()) {
                final float destR = -90.0f - y * 90.0f / board.getBoardHeight();
                if (destR > 0.0f) {
                    return;
                }

                if (mView.getContentViewRotationX() == 0.0f) {
                    mView.pause();
                    mView.setContentViewPivotY(0.0f);
                }

                if (destR < -90.0f) {
                    if (mView.isPaused() && mView.isContentViewShown()) {
                        mView.setContentViewVisibility(INVISIBLE);
                    }
                } else {
                    if (!mView.isContentViewShown()) {
                        mView.setContentViewVisibility(VISIBLE);
                    }
                }

                float alpha = Utils.getAlphaForOffset(1.0f, 0.0f, 0.0f, -90.0f, destR);

                mView.setContentViewRotationX(destR);
                mView.setContentViewAlpha(alpha);
            }
        }
    }

    private final AnimatorListener mViewDismissAnimatorListener = new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                if (DBG) Log.v(TAG, "view dismiss start");
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (DBG) Log.v(TAG, "view dismiss end");
                mView.sendPendings();
                mView.dismiss();
            }
        };

    private final AnimatorListener mViewShowAnimatorListener = new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                if (DBG) Log.v(TAG, "view show start");
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (DBG) Log.v(TAG, "view show end");
                mView.resume();
            }
        };
}
