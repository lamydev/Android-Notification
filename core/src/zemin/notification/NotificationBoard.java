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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.support.v4.view.GestureDetectorCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Random;

/**
 * A board showing a list of current notifications. You can provide your own implementation
 * of {@link NotificationBoardCallback} to customize the board appearance.
 *
 * @see NotificationBoard#setCallback.
 */
public class NotificationBoard extends FrameLayout
        implements NotificationListener,
                   GestureDetector.OnGestureListener,
                   GestureDetector.OnDoubleTapListener {

    private static final String TAG = "zemin.NotificationBoard";
    public static boolean DBG;

    public static final int OPEN_TRANSITION_TIME = 500;
    public static final int CLOSE_TRANSITION_TIME = 500;

    public static final int OPEN_TRIGGER_VELOCITY = 150;
    public static final int CLOSE_TRIGGER_VELOCITY = 150;

    public static final int HEADER_HEIGHT = 146;
    public static final int FOOTER_HEIGHT = 200;

    public static final int GESTURE_CONSUMER_DEFAULT = 0;
    public static final int GESTURE_CONSUMER_USER = 1;

    public static final float DIM_ALPHA = 1.0f;
    public static final int DIM_COLOR = 0xff757575;

    public static final int X = 0;
    public static final int Y = 1;

    private final int[] mRowMargin = { 0, 0, 0, 0 };

    private ArrayList<NotificationEntry> mPendingArrives = null;
    private ArrayList<NotificationEntry> mPendingCancels = null;
    private ArrayList<StateListener> mListeners = null;

    private final Object mLock = new Object();
    // private final Random mRandom = new Random();

    private Context mContext;
    private LayoutInflater mInflater;
    private GestureDetectorCompat mGestureDetector;
    private NotificationBoardCallback mCallback;
    private NotificationCenter mCenter;
    private GestureListener mGestureListener;
    private ContentView mContentView;
    private BodyView mBody;
    private HeaderView mHeader;
    private FooterView mFooter;
    private LinearLayout mContainer;
    private RowView mRemovingView;
    private Drawable mHeaderDivider;
    private Drawable mFooterDivider;
    private int mHeaderDividerHeight;
    private int mFooterDividerHeight;
    private View mClearView;
    private View mDimView;
    private float mDimAlpha = DIM_ALPHA;
    private int mDimColor = DIM_COLOR;
    private boolean mDimEnabled = true;
    private boolean mFirstLayout = true;
    private boolean mCallbackChanged = false;
    private boolean mEnabled = true;
    private boolean mPaused = false;
    private boolean mAnimating = false;
    private boolean mScrolling = false;
    private boolean mDismissed = false;
    private boolean mOpened = false;
    private boolean mInLayout = false;
    private boolean mShowing = false;
    private boolean mClosing = false;
    private boolean mPrepareX = false;
    private boolean mPrepareY = false;
    private boolean mCloseOnHomeKey = true;
    private boolean mCloseOnOutsideTouch = true;
    private boolean mCloseOnRemovingRowView = false;
    private int mRowViewToRemove;
    private float mInitialX;
    private float mInitialY;
    private int mDirection = -1;
    private int mGestureConsumer;
    private int mOpenTransitionTime;
    private int mCloseTransitionTime;
    private int mStatusBarHeight;
    private int mBodyHeight;

    /**
     * Monitor the state of this board.
     */
    public interface StateListener {

        /**
         * Called before this board is being displayed.
         *
         * @param board
         */
        void onBoardPrepare(NotificationBoard board);

        /**
         * Called when this board is moving in x direction.
         *
         * @param board
         * @param x
         */
        void onBoardTranslationX(NotificationBoard board, float x);

        /**
         * Called when this board is moving in y direction.
         *
         * @param board
         * @param y
         */
        void onBoardTranslationY(NotificationBoard board, float y);

        /**
         * Called when this board is rotated in x direction.
         *
         * @param board
         * @param x
         */
        void onBoardRotationX(NotificationBoard board, float x);

        /**
         * Called when this board is rotated in y direction.
         *
         * @param board
         * @param y
         */
        void onBoardRotationY(NotificationBoard board, float y);

        /**
         * Called when the x location of pivot point is changed.
         *
         * @param board
         * @param x
         */
        void onBoardPivotX(NotificationBoard board, float x);

        /**
         * Called when the y location of pivot point is changed.
         *
         * @param board
         * @param y
         */
        void onBoardPivotY(NotificationBoard board, float y);

        /**
         * Called when the alpha value is changed.
         *
         * @param board
         * @param alpha
         */
        void onBoardAlpha(NotificationBoard board, float alpha);

        /**
         * Called when the open animation is started.
         *
         * @param board
         */
        void onBoardStartOpen(NotificationBoard board);

        /**
         * Called when the open animation is done.
         *
         * @param board
         */
        void onBoardEndOpen(NotificationBoard board);

        /**
         * Called when the open animation is canceled.
         *
         * @param board
         */
        void onBoardCancelOpen(NotificationBoard board);

        /**
         * Called when the close animation is started.
         *
         * @param board
         */
        void onBoardStartClose(NotificationBoard board);

        /**
         * Called when the close animation is done.
         *
         * @param board
         */
        void onBoardEndClose(NotificationBoard board);

        /**
         * Called when the close animation is canceled.
         *
         * @param board
         */
        void onBoardCancelClose(NotificationBoard board);
    }

    /**
     * A convenience class to extend when you only want to listen for a subset
     * of all states. This implements all methods in the {@link StateListener}.
     */
    public static class SimpleStateListener implements StateListener {

        public void onBoardPrepare(NotificationBoard board) {}
        public void onBoardTranslationX(NotificationBoard board, float x) {}
        public void onBoardTranslationY(NotificationBoard board, float y) {}
        public void onBoardRotationX(NotificationBoard board, float x) {}
        public void onBoardRotationY(NotificationBoard board, float y) {}
        public void onBoardPivotX(NotificationBoard board, float x) {}
        public void onBoardPivotY(NotificationBoard board, float y) {}
        public void onBoardAlpha(NotificationBoard board, float alpha) {}
        public void onBoardStartOpen(NotificationBoard board) {}
        public void onBoardEndOpen(NotificationBoard board) {}
        public void onBoardCancelOpen(NotificationBoard board) {}
        public void onBoardStartClose(NotificationBoard board) {}
        public void onBoardEndClose(NotificationBoard board) {}
        public void onBoardCancelClose(NotificationBoard board) {}
    }

    public NotificationBoard(Context context) {
        super(context);
        initialize();
    }

    public NotificationBoard(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    /**
     * Get {@link LayoutInflater}.
     *
     * @return LayoutInflater
     */
    public LayoutInflater getInflater() {
        return mInflater;
    }

    /**
     * Whether the callback {@link NotificationBoardCallback} has been set.
     *
     * @return boolean
     */
    public boolean hasCallback() {
        return mCallback != null;
    }

    /**
     * Set the callback. You can have your board layout customized by
     * extending {@link NotificationBoardCallback}.
     *
     * @see NotificationBoardCallback.
     *
     * @param cb
     */
    public void setCallback(NotificationBoardCallback cb) {
        if (mCallback != cb) {
            mCallback = cb;
            mCallbackChanged = true;
        }
    }

    /**
     * Whether this board is enabled.
     *
     * @return boolean
     */
    public boolean isBoardEnabled() {
        return mEnabled;
    }

    /**
     * Enable/disable this board.
     *
     * @param enable
     */
    public void setBoardEnabled(boolean enable) {
        if (mEnabled != enable) {
            if (DBG) Log.v(TAG, "enable - " + enable);
            mEnabled = enable;
        }
    }

    /**
     * Whether this board is showing.
     *
     * @return boolean
     */
    public boolean isShowing() {
        return mShowing;
    }

    /**
     * Whether this board is completely opened.
     *
     * @return boolean
     */
    public boolean isOpened() {
        return mOpened;
    }

    /**
     * Whether this board is being scrolled by the user.
     *
     * @return boolean
     */
    public boolean isScrolling() {
        return mScrolling;
    }

    /**
     * Whether this board is animating.
     *
     * @return boolean
     */
    public boolean isAnimating() {
        return mAnimating;
    }

    /**
     * Open the board by performing a pull-down animation.
     *
     * @return boolean false, if the board is disabled.
     */
    public boolean open() {
        return open(true);
    }

    /**
     * Open the board.
     *
     * @param anim pull-down animation.
     * @return boolean false, if the board is disabled.
     */
    public boolean open(boolean anim) {
        if (!mEnabled) {
            return false;
        }

        if (mOpened) {
            return true;
        }

        if (!mShowing) {
            show();
        }

        if (anim) {
            animateOpen();
        } else {
            onEndOpen();
        }
        return true;
    }

    /**
     * Close the board by perform a push-up animation.
     *
     * @return boolean false, if the board is disabled.
     */
    public boolean close() {
        return close(true);
    }

    /**
     * Close the board.
     *
     * @param anim push-up animation.
     * @reutrn boolean false, if the board is disable.
     */
    public boolean close(boolean anim) {
        if (!mEnabled || !mShowing) {
            return false;
        }

        if (anim) {
            animateClose();
        } else {
            onEndClose();
        }
        return true;
    }

    /**
     * add {@link NotificationBoard#StateListener}.
     *
     * @param l
     */
    public void addStateListener(StateListener l) {
        if (mListeners == null) {
            mListeners = new ArrayList<StateListener>();
        }
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    /**
     * remove {@link NotificationBoard#StateListener}.
     *
     * @param l
     */
    public void removeStateListener(StateListener l) {
        if (mListeners != null && mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    /**
     * @see GestureListener.
     *
     * @param l
     */
    public void setGestureListener(GestureListener l) {
        mGestureListener = l;
    }

    /**
     * Set the duration of the open animation.
     *
     * @param ms
     */
    public void setOpenTransitionTime(int ms) {
        mOpenTransitionTime = ms;
    }

    /**
     * Get the duration of the open animation.
     *
     * @param ms
     */
    public int getOpenTransitionTime() {
        return mOpenTransitionTime;
    }

    /**
     * Set the duration of the close animation.
     *
     * @param ms
     */
    public void setCloseTransitionTime(int ms) {
        mCloseTransitionTime = ms;
    }

    /**
     * Get the duration of the close animation.
     *
     * @param ms
     */
    public int getCloseTransitionTime() {
        return mCloseTransitionTime;
    }

    /**
     * Whether this board should be closed when home key is pressed.
     *
     * @param close
     */
    public void setCloseOnHomeKey(boolean close) {
        mCloseOnHomeKey = close;
    }

    /**
     * @return boolean
     */
    public boolean getCloseOnHomeKey() {
        return mCloseOnHomeKey;
    }

    /**
     * Whether this board should be closed when user touches outside of the board.
     *
     * @param close
     */
    public void setCloseOnOutsideTouch(boolean close) {
        mCloseOnOutsideTouch = close;
    }

    /**
     * @return boolean
     */
    public boolean getCloseOnOutsideTouch() {
        return mCloseOnOutsideTouch;
    }

    /**
     * Set the touch area where the user can touch to pull the board down.
     *
     * @param l
     * @param t
     * @param r
     * @param b
     */
    public void setInitialTouchArea(int l, int t, int r, int b) {
        mContentView.setTouchToOpen(l, t, r, b);
    }

    /**
     * Set the margin of the header.
     *
     * @param l
     * @param t
     * @param r
     * @param b
     */
    public void setHeaderMargin(int l, int t, int r, int b) {
        mHeader.setMargin(l, t, r, b);
    }

    /**
     * Set the margin of the footer.
     *
     * @param l
     * @param t
     * @param r
     * @param b
     */
    public void setFooterMargin(int l, int t, int r, int b) {
        mFooter.setMargin(l, t, r, b);
    }

    /**
     * Set the margin of the body.
     *
     * @param l
     * @param t
     * @param r
     * @param b
     */
    public void setBodyMargin(int l, int t, int r, int b) {
        mBody.setMargin(l, t, r, b);
    }

    /**
     * Set the margin of each row.
     *
     * @param l
     * @param t
     * @param r
     * @param b
     */
    public void setRowMargin(int l, int t, int r, int b) {
        mRowMargin[0] = l; mRowMargin[1] = t; mRowMargin[2] = r; mRowMargin[3] = b;
    }

    /**
     * Set the height of the header.
     *
     * @param height
     */
    public void setHeaderHeight(int height) {
        mHeader.setHeight(height);
    }

    /**
     * Get the height of the header.
     *
     * @return int
     */
    public int getHeaderHeight() {
        return mHeader.getSuggestedHeight();
    }

    /**
     * Set the height of the footer.
     *
     * @param height
     */
    public void setFooterHeight(int height) {
        mFooter.setHeight(height);
    }

    /**
     * Get the height of the footer.
     *
     * @return int
     */
    public int getFooterHeight() {
        return mFooter.getSuggestedHeight();
    }

    /**
     * Set the height of the body.
     *
     * @param height
     */
    public void setBodyHeight(int height) {
        mBodyHeight = height;
    }

    /**
     * Get the height of the body.
     *
     * @return int
     */
    public int getBodyHeight() {
        return mBodyHeight;
    }

    /**
     * Add header view.
     *
     * @param view
     */
    public void addHeaderView(View view) {
        mHeader.addView(view);
    }

    /**
     * Add header view.
     *
     * @param view
     * @param index
     * @param lp
     */
    public void addHeaderView(View view, int index, ViewGroup.LayoutParams lp) {
        mHeader.addView(view, index, lp);
    }

    /**
     * Remove header view.
     *
     * @param view
     */
    public void removeHeaderView(View view) {
        mHeader.removeView(view);
    }

    /**
     * Get header view at a certain index.
     *
     * @param index
     * @return View
     */
    public View getHeaderView(int index) {
        return mHeader.getChildAt(index);
    }

    /**
     * Get header view count.
     *
     * @return int
     */
    public int getHeaderViewCount() {
        return mHeader.getChildCount();
    }

    /**
     * Add footer view.
     *
     * @param view
     */
    public void addFooterView(View view) {
        mFooter.addView(view);
    }

    /**
     * Add footer view.
     *
     * @param view
     * @param index
     * @param lp
     */
    public void addFooterView(View view, int index, ViewGroup.LayoutParams lp) {
        mFooter.addView(view, index, lp);
    }

    /**
     * Remove footer view.
     *
     * @param view
     */
    public void removeFooterView(View view) {
        mFooter.removeView(view);
    }

    /**
     * Get footer view at a certain index.
     *
     * @param index
     * @return View
     */
    public View getFooterView(int index) {
        return mFooter.getChildAt(index);
    }

    /**
     * Get footer view count.
     *
     * @return int
     */
    public int getFooterViewCount() {
        return mFooter.getChildCount();
    }

    /**
     * Add body view.
     *
     * @param view
     */
    public void addBodyView(View view) {
        mBody.addView(view);
    }

    /**
     * Add body view.
     *
     * @param view
     * @param index
     * @param lp
     */
    public void addBodyView(View view, int index, ViewGroup.LayoutParams lp) {
        mBody.addView(view, index, lp);
    }

    /**
     * Remove body view.
     *
     * @param view
     */
    public void removeBodyView(View view) {
        mBody.removeView(view);
    }

    /**
     * Get body view at a certain index.
     *
     * @param index
     * @return View
     */
    public View getBodyView(int index) {
        return mBody.getChildAt(index);
    }

    /**
     * Get body view count.
     *
     * @return int
     */
    public int getBodyViewCount() {
        return mBody.getChildCount();
    }

    /**
     * Set header divider.
     *
     * @param resId
     */
    public void setHeaderDivider(int resId) {
        setHeaderDivider(getResources().getDrawable(resId));
    }

    /**
     * Set header divider.
     *
     * @param drawable
     */
    public void setHeaderDivider(Drawable drawable) {
        mHeaderDivider = drawable;
        if (drawable != null) {
            mHeaderDividerHeight = drawable.getIntrinsicHeight();
        } else {
            mHeaderDividerHeight = 0;
        }
        mContentView.setWillNotDraw(drawable == null);
        mContentView.invalidate();
    }

    /**
     * Get header divider.
     *
     * @return Drawable
     */
    public Drawable getHeaderDivider() {
        return mHeaderDivider;
    }

    /**
     * Set the height of header divider.
     *
     * @param height
     */
    public void setHeaderDividerHeight(int height) {
        mHeaderDividerHeight = height;
    }

    /**
     * Get the height of header divider.
     *
     * @return int
     */
    public int getHeaderDividerHeight() {
        return mHeaderDividerHeight;
    }

    /**
     * Set footer divider.
     *
     * @param resId
     */
    public void setFooterDivider(int resId) {
        setFooterDivider(getResources().getDrawable(resId));
    }

    /**
     * Set footer divider.
     *
     * @param drawable
     */
    public void setFooterDivider(Drawable drawable) {
        mFooterDivider = drawable;
        if (drawable != null) {
            mFooterDividerHeight = drawable.getIntrinsicHeight();
        } else {
            mFooterDividerHeight = 0;
        }
        mContentView.setWillNotDraw(drawable == null);
        mContentView.invalidate();
    }

    /**
     * Get footer divider.
     *
     * @return Drawable
     */
    public Drawable getFooterDivider() {
        return mFooterDivider;
    }

    /**
     * Set the height of footer divider.
     *
     * @param height
     */
    public void setFooterDividerHeight(int height) {
        mFooterDividerHeight = height;
    }

    /**
     * Get the height of footer divider.
     *
     * @param int
     */
    public int getFooterDividerHeight() {
        return mFooterDividerHeight;
    }

    /**
     * Set body row divider.
     *
     * @param resId
     */
    public void setRowDivider(int resId) {
        mContainer.setDividerDrawable(getResources().getDrawable(resId));
    }

    /**
     * Set body row divider.
     *
     * @param drawable
     */
    public void setRowDivider(Drawable drawable) {
        mContainer.setDividerDrawable(drawable);
    }

    /**
     * Get body row divider.
     *
     * @return Drawable
     */
    public Drawable getRowDivider() {
        return mContainer.getDividerDrawable();
    }

    /**
     * Set body overscroll mode.
     *
     * @see View#OVER_SCROLL_ALWAYS
     * @see View#OVER_SCROLL_IF_CONTENT_SCROLLS
     * @see View#OVER_SCROLL_NONE
     *
     * @param mode
     */
    public void setBodyOverScrollMode(int mode) {
        mBody.scroller.setOverScrollMode(mode);
    }

    /**
     * Get body overscroll mode.
     *
     * @see View#OVER_SCROLL_ALWAYS
     * @see View#OVER_SCROLL_IF_CONTENT_SCROLLS
     * @see View#OVER_SCROLL_NONE
     *
     * @return int
     */
    public int getBodyOverScrollMode() {
        return mBody.scroller.getOverScrollMode();
    }

    /**
     * Get {@link NotificationBoard#RowView} by the id of {@link NotificationEntry}.
     *
     * @param notification
     * @return RowView
     */
    public RowView getRowView(int notification) {
        for (int i = 0, count = mContainer.getChildCount(); i < count; i++) {
            RowView rowView = (RowView) mContainer.getChildAt(i);
            if (rowView.notification == notification) {
                return rowView;
            }
        }
        return null;
    }

    /**
     * Get {@link NotificationBoard#RowView} by its children.
     *
     * @param child
     * @return RowView
     */
    public RowView getRowView(View child) {
        ViewParent parent = child.getParent();
        if (parent instanceof RowView) {
            return (RowView) parent;
        }
        return null;
    }

    /**
     * Get {@link NotificationBoard#RowView} by touch event.
     *
     * @param event
     */
    public RowView getRowView(MotionEvent event) {
        View view = mBody.findViewByTouch(event);
        return view != null ? (RowView) view : null;
    }

    /**
     * Get {@link NotificationEntry} by its id.
     *
     * @param notification
     * @return NotificationEntry
     */
    public NotificationEntry getNotification(int notification) {
        RowView rowView = getRowView(notification);
        return rowView != null ? rowView.getNotification() : null;
    }

    /**
     * Get notification count.
     *
     * @return int
     */
    public int getNotificationCount() {
        return mContainer.getChildCount();
    }

    /**
     * Cancel all notifications.
     */
    public void cancelAllNotifications() {
        removeAllRowViews();
    }

    /**
     * Set clear view. If clicked, all notifications will be canceled.
     *
     * @param view
     */
    public void setClearView(View view) {
        mClearView = view;
        if (view != null) {
            view.setVisibility(mOpened ? VISIBLE : INVISIBLE);
            view.setOnClickListener(mOnClickListenerClearView);
        }
    }

    /**
     * Get clear view.
     *
     * @return View
     */
    public View getClearView() {
        return mClearView;
    }

    /**
     * Show/hide the clear view.
     *
     * @param show
     */
    public void showClearView(boolean show) {
        if (mClearView.isShown() != show) {
            mClearView.setVisibility(show ? VISIBLE : INVISIBLE);
        }
    }

    /**
     * Get the width of this board.
     *
     * @return float
     */
    public float getBoardWidth() {
        return mContentView.getMeasuredWidth();
    }

    /**
     * Get the height of this board.
     *
     * @return float
     */
    public float getBoardHeight() {
        return mContentView.getMeasuredHeight();
    }

    /**
     * Set the dimension of the entire board.
     *
     * @param width
     * @param height
     */
    public void setBoardDimension(int width, int height) {
        mContentView.setDimension(width, height);
    }

    /**
     * Set the margin of the entire board.
     *
     * @param l
     * @param t
     * @param r
     * @param b
     */
    public void setBoardPadding(int l, int t, int r, int b) {
        mContentView.setPadding(l, t, r, b);
    }

    /**
     * Get the x point of initial down {@link android.view.MotionEvent}.
     *
     * @return float
     */
    public float getInitialTouchX() {
        return mInitialX - mContentView.getPaddingLeft();
    }

    /**
     * Get the y point of initial down {@link android.view.MotionEvent}.
     *
     * @return float
     */
    public float getInitialTouchY() {
        return mInitialY - mContentView.getPaddingTop();
    }

    /**
     * Get get gesture direction.
     *
     * @see #X
     * @see #Y
     *
     * @return int
     */
    public int getGestureDirection() {
        return mDirection;
    }

    /**
     * Set the x translation of this board.
     *
     * @param x
     */
    public void setBoardTranslationX(float x) {
        if (mListeners != null) {
            for (StateListener l : mListeners) {
                l.onBoardTranslationX(this, x);
            }
        }
        mContentView.setTranslationX(x);
    }

    /**
     * Get the x translation of this board.
     *
     * @return float
     */
    public float getBoardTranslationX() {
        return mContentView.getTranslationX();
    }

    /**
     * Set the y translation of this board.
     *
     * @param y
     */
    public void setBoardTranslationY(float y) {
        if (mListeners != null) {
            for (StateListener l : mListeners) {
                l.onBoardTranslationY(this, y);
            }
        }
        mContentView.setTranslationY(y);
    }

    /**
     * Get the y translation of this board.
     *
     * @return y
     */
    public float getBoardTranslationY() {
        return mContentView.getTranslationY();
    }

    /**
     * Set the x location of pivot point around which this board is rotated.
     *
     * @param x
     */
    public void setBoardPivotX(float x) {
        if (mListeners != null) {
            for (StateListener l : mListeners) {
                l.onBoardPivotX(this, x);
            }
        }
        mContentView.setPivotX(x);
    }

    /**
     * Get the x location of pivot point around which this board is rotated.
     *
     * @return float
     */
    public float getBoardPivotX() {
        return mContentView.getPivotX();
    }

    /**
     * Set the y location of pivot point around which this board is rotated.
     *
     * @param y
     */
    public void setBoardPivotY(float y) {
        if (mListeners != null) {
            for (StateListener l : mListeners) {
                l.onBoardPivotY(this, y);
            }
        }
        mContentView.setPivotY(y);
    }

    /**
     * Get the y location of pivot point around which this board is rotated.
     *
     * @return float
     */
    public float getBoardPivotY() {
        return mContentView.getPivotY();
    }

    /**
     * Set the x degree that this board is rotated.
     *
     * @param x
     */
    public void setBoardRotationX(float x) {
        if (mListeners != null) {
            for (StateListener l : mListeners) {
                l.onBoardRotationX(this, x);
            }
        }
        mContentView.setRotationX(x);
    }

    /**
     * Get the x degree that this board is rotated.
     *
     * @return float
     */
    public float getBoardRotationX() {
        return mContentView.getRotationX();
    }

    /**
     * Set the y degree that this board is rotated.
     *
     * @param y
     */
    public void setBoardRotationY(float y) {
        if (mListeners != null) {
            for (StateListener l : mListeners) {
                l.onBoardRotationY(this, y);
            }
        }
        mContentView.setRotationY(y);
    }

    /**
     * Get the y degree that this board is rotated.
     *
     * @return float
     */
    public float getBoardRotationY() {
        return mContentView.getRotationY();
    }

    /**
     * Set the opacity of this board.
     *
     * @param alpha
     */
    public void setBoardAlpha(float alpha) {
        if (mListeners != null) {
            for (StateListener l : mListeners) {
                l.onBoardAlpha(this, alpha);
            }
        }
        mContentView.setAlpha(alpha);
    }

    /**
     * Get the opacity of this board.
     *
     * @return float
     */
    public float getBoardAlpha() {
        return mContentView.getAlpha();
    }

    /**
     * Enable/disable the dim-behind layer.
     *
     * @param enable
     */
    public void setDimEnabled(boolean enable) {
        mDimEnabled = enable;
    }

    /**
     * @return boolean
     */
    public boolean getDimEnabled() {
        return mDimEnabled;
    }

    /**
     * Set the color of the dim-behind layer.
     *
     * @param color
     */
    public void setDimColor(int color) {
        mDimColor = color;
    }

    /**
     * Get the color of the dim-behind layer.
     *
     * @return int
     */
    public int getDimColor() {
        return mDimColor;
    }

    /**
     * Set the opacity of the dim-behind layer.
     *
     * @param alpha
     */
    public void setDimAlpha(float alpha) {
        mDimAlpha = alpha;
    }

    /**
     * Get the opacity of the dim-behind layer.
     *
     * @return float
     */
    public float getDimAlpha() {
        return mDimAlpha;
    }

    /**
     * Set the dim-behind layer a specific opacity.
     *
     * @param alpha
     */
    public void dimAt(float alpha) {
        if (!mDimEnabled) {
            return;
        }
        if (mDimView == null) {
            mDimView = makeDimView();
        }
        if (!mDimView.isShown()) {
            mDimView.setVisibility(VISIBLE);
            mDimView.setBackgroundColor(mDimColor);
        }
        mDimView.setAlpha(alpha);
    }

    /**
     * Start the dim animation.
     *
     * @param duration
     */
    public void dim(int duration) {
        if (!mDimEnabled) {
            return;
        }
        if (mDimView == null) {
            mDimView = makeDimView();
        }
        if (!mDimView.isShown()) {
            mDimView.setVisibility(VISIBLE);
            mDimView.setBackgroundColor(mDimColor);
        }
        mDimView.animate().cancel();
        mDimView.animate().alpha(mDimAlpha)
            .setListener(null)
            .setDuration(duration)
            .start();
    }

    /**
     * Start the undim animation.
     *
     * @param duration
     */
    public void undim(int duration) {
        if (mDimView != null && mDimView.isShown() && mDimView.getAlpha() != 0) {
            mDimView.animate().cancel();
            mDimView.animate().alpha(0.0f)
                .setListener(mDimAnimatorListener)
                .setDuration(duration)
                .start();
        }
    }

    private View makeDimView() {
        View dimView = new View(mContext);
        dimView.setAlpha(0.0f);
        dimView.setVisibility(GONE);
        addView(dimView, 0, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        return dimView;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        if (!mEnabled || mPaused) {
            return false;
        }

        mPrepareY = !mBody.isInsideTouch(event);
        mPrepareX = !mPrepareY && mOpened && !mAnimating && !mInLayout;

        if (mPrepareX) {
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mInitialX = event.getX();
                mInitialY = event.getY();
                mGestureDetector.onTouchEvent(event);
                break;

            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getX() - mInitialX;
                float deltaY = event.getY() - mInitialY;
                mDirection = Math.abs(deltaX) > Math.abs(deltaY) ? X : Y;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mDirection == Y) {
                    mGestureDetector.onTouchEvent(event);
                }
                break;
            }
        }

        return !mOpened || (mPrepareX && mDirection == X);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!mEnabled || mPaused) {
            return false;
        }

        final int action = event.getAction();
        boolean handled = false;
        if (!mOpened || mPrepareY || action != MotionEvent.ACTION_DOWN) {
            handled = mGestureDetector.onTouchEvent(event);
        }

        switch (action) {
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            onUpOrCancel(event, handled);
            break;
        }

        return handled ? handled : super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event) {
        if (DBG) Log.v(TAG, "onDown");
        mDismissed = false;
        mDirection = -1;
        mRemovingView = null;
        mGestureConsumer = GESTURE_CONSUMER_DEFAULT;
        mInitialX = event.getX();
        mInitialY = event.getY();
        if (mGestureListener != null) {
            mGestureListener.onDown(event);
        }
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        if (DBG) Log.v(TAG, "onShowPress");
        if (mGestureListener != null) {
            mGestureListener.onShowPress(event);
        }
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        if (DBG) Log.v(TAG, "onSingleTapUp");
        boolean handled = false;
        if (mGestureListener != null && mGestureListener.onSingleTapUp(event)) {
            handled = true;
        }
        return handled;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        if (DBG) Log.v(TAG, "onSingleTapConfirmed");
        boolean handled = false;
        if (mCloseOnOutsideTouch && !mContentView.isInsideTouch(event)) {
            animateClose();
            handled = true;
        } else if (mGestureListener != null) {
            handled = mGestureListener.onSingleTapConfirmed(event);
        }
        return handled;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        if (DBG) Log.v(TAG, "onDoubleTap");
        boolean handled = false;
        if (mGestureListener != null) {
            handled = mGestureListener.onDoubleTap(event);
        }
        return handled;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        if (DBG) Log.v(TAG, "onDoubleTapEvent");
        boolean handled = false;
        if (mGestureListener != null) {
            handled = mGestureListener.onDoubleTapEvent(event);
        }
        return handled;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        if (DBG) Log.v(TAG, "onLongPress");
        if (mGestureListener != null) {
            mGestureListener.onLongPress(event);
        }
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        // if (DBG) Log.v(TAG, "onScroll");

        if (mGestureConsumer == GESTURE_CONSUMER_DEFAULT) {
            if (mDirection == -1 && mGestureListener != null &&
                mGestureListener.onScroll(event1, event2, distanceX, distanceY)) {

                mGestureConsumer = GESTURE_CONSUMER_USER;
                return true;
            }
        } else if (mGestureConsumer == GESTURE_CONSUMER_USER) {
            return mGestureListener != null ?
                mGestureListener.onScroll(event1, event2, distanceX, distanceY) : false;
        }

        final int direction = Math.abs(distanceX) > Math.abs(distanceY) ? X : Y;
        if (mDirection != -1 && mDirection != direction) {
            // if (DBG) Log.v(TAG, "wrong direction(curr=" + direction +
            //                ", prev=" + mDirection + "): skip scroll.");
            return false;
        }

        if (mDismissed || mAnimating) {
            return false;
        }

        if (direction == Y) {
            if (!mShowing) {
                if (distanceY > 0.0f || !mContentView.isInsideTouchToOpen(event1)) {
                    return false;
                }

                show();
            }

            if (mDirection == -1) {
                mDirection = direction;
            }

            final float y = mContentView.getTranslationY() - distanceY;
            if (y <= 0.0f) {
                if (y + mContentView.getMeasuredHeight() > 0.0f) {
                    mScrolling = true;
                    mOpened = y == 0.0f;
                    setBoardTranslationY(y);
                    dimAt(Utils.getAlphaForOffset(mDimAlpha, 0.0f, 0.0f, -mContentView.getMeasuredHeight(), y));
                    return true;
                }
            }
        } else {
            if (!mShowing || !mOpened) {
                return false;
            }

            if (mRemovingView == null) {
                if (!mBody.isInsideTouch(event1)) {
                    return false;
                }

                mRemovingView = getRowView(event1);
                mDirection = direction;
            }

            if (mRemovingView != null) {
                mScrolling = true;
                mRemovingView.onScrollX(mRemovingView.getTranslationX() - distanceX);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        if (DBG) Log.v(TAG, "onFling");

        mScrolling = false;
        if (mGestureConsumer == GESTURE_CONSUMER_DEFAULT) {
            if (mDirection == -1 && mGestureListener != null &&
                mGestureListener.onFling(event1, event2, velocityX, velocityY)) {
                return true;
            }
        } else if (mGestureConsumer == GESTURE_CONSUMER_USER) {
            return mGestureListener != null ?
                mGestureListener.onFling(event1, event2, velocityX, velocityY) : false;
        }

        final int direction = Math.abs(velocityX) > Math.abs(velocityY) ? X : Y;
        if (mDirection != -1 && mDirection != direction) {
            if (DBG) Log.v(TAG, "wrong direction(curr=" + direction +
                           ", prev=" + mDirection + "): skip fling.");
            return false;
        }

        if (mDismissed || mAnimating) {
            return false;
        }

        if (direction == Y) {
            int ret = 0;
            if (velocityY > 0 && Math.abs(velocityY) > OPEN_TRIGGER_VELOCITY) {
                ret = 1;
            } else if (velocityY < 0 && Math.abs(velocityY) > CLOSE_TRIGGER_VELOCITY) {
                ret = 2;
            }

            if (ret != 0) {
                if (!mShowing) {
                    if (velocityY < 0 || !mContentView.isInsideTouchToOpen(event1)) {
                        return false;
                    }

                    show();
                }

                if (ret == 1) {
                    animateOpen();
                } else {
                    animateClose();
                }
                return true;
            }
        } else {
            if (!mShowing || !mOpened) {
                return false;
            }

            if (Math.abs(velocityX) > RowView.DISMISS_TRIGGER_VELOCITY) {
                if (mRemovingView == null) {
                    if (!mBody.isInsideTouch(event1)) {
                        return false;
                    }

                    mRemovingView = getRowView(event1);
                }

                if (mRemovingView != null) {
                    mRemovingView.onFlingX(velocityX);
                    mRemovingView = null;
                    return true;
                }
            }
        }
        return false;
    }

    public void onUpOrCancel(MotionEvent event, boolean handled) {
        if (DBG) Log.v(TAG, "onUpOrCancel: handled=" + handled + ", showing=" +
                       mShowing + ", animating=" + mAnimating + ", direction=" + mDirection);

        mScrolling = false;
        if (mGestureListener != null) {
            mGestureListener.onUpOrCancel(event, handled);
        }

        if (!handled && mShowing && !mAnimating) {
            switch (mDirection) {
            case Y:
                if (mContentView.getTranslationY() + mContentView.getMeasuredHeight() * 0.6 > 0) {
                    animateOpen();
                } else {
                    animateClose();
                }
                break;

            case X:
                if (mRemovingView != null) {
                    mRemovingView.onUpOrCancel();
                    mRemovingView = null;
                }
                break;
            }
        }
    }

    @Override
    public void onArrival(NotificationEntry entry) {
        synchronized (mLock) {
            if (mShowing && !mClosing) {
                if (mPaused || mAnimating || mScrolling) {
                    addPendingArrive(entry);
                } else {
                    addRowView(entry);
                }
            }
        }
    }

    @Override
    public void onCancel(NotificationEntry entry) {
        synchronized (mLock) {
            if (mShowing && !mClosing) {
                if (mAnimating || mScrolling) {
                    addPendingCancel(entry);
                } else {
                    removeRowView(entry);
                }
            }
        }
    }

    private void show() {
        if (mCallback == null) {
            if (DBG) Log.v(TAG, "set default NotificationBoardCallback");
            setCallback(new NotificationBoardCallback());
        }

        if (mShowing) {
            return;
        }

        if (DBG) Log.v(TAG, "show");

        if (mCallbackChanged) {
            mCallbackChanged = false;
            mCallback.onBoardSetup(this);
        }

        mShowing = true;
        mFirstLayout = true;

        mContentView.updateLayoutParams();
        mHeader.updateMargin();
        mHeader.updateDimension();
        mBody.updateMargin();
        mFooter.updateMargin();
        mFooter.updateDimension();

        updateRowViews();
        setVisibility(VISIBLE);
        onPrepare();
    }

    private void animateOpen() {
        if (mOpened) {
            return;
        }

        if (mOpenTransitionTime <= 0) {
            mOpenTransitionTime = OPEN_TRANSITION_TIME;
        }

        mContentView.animateOpen();
        dim(mOpenTransitionTime);
    }

    private void animateClose() {
        if (mClosing) {
            return;
        }

        if (mRowViewToRemove > 0) {
            mCloseOnRemovingRowView = true;
            return;
        }

        if (mCloseTransitionTime <= 0) {
            mCloseTransitionTime = CLOSE_TRANSITION_TIME;
        }

        mContentView.animateClose();
        undim(mCloseTransitionTime);
    }

    private final AnimatorListener mOpenAnimatorListener = new AnimatorListener() {

            private boolean mCanceled;

            @Override
            public void onAnimationStart(Animator animation) {
                if (DBG) Log.v(TAG, "open start");
                mCanceled = false;
                mAnimating = true;
                onStartOpen();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mCanceled) {
                    if (DBG) Log.v(TAG, "open end");
                    mContentView.animate().setListener(null);
                    mAnimating = false;
                    onEndOpen();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (DBG) Log.v(TAG, "open cancel");
                mCanceled = true;
                onCancelOpen();
            }
        };

    private final AnimatorListener mCloseAnimatorListener = new AnimatorListener() {

            private boolean mCanceled;

            @Override
            public void onAnimationStart(Animator animation) {
                if (DBG) Log.v(TAG, "close start");
                mCanceled = false;
                mAnimating = true;
                mClosing = true;
                onStartClose();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mCanceled) {
                    if (DBG) Log.v(TAG, "close end");
                    mContentView.animate().setListener(null);
                    mAnimating = false;
                    mClosing = false;
                    onEndClose();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (DBG) Log.v(TAG, "close cancel");
                mCanceled = true;
                onCancelClose();
            }
        };

    private final AnimatorListener mDimAnimatorListener = new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                if (DBG) Log.v(TAG, "dim start");
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (DBG) Log.v(TAG, "dim end");
                mDimView.animate().setListener(null);
                mDimView.setAlpha(0.0f);
                mDimView.setVisibility(GONE);
            }
        };

    public class RowView extends FrameLayout {

        public static final int DISMISS_TRANSITION_TIME = 500;
        public static final int DRAG_CANCEL_TRANSITION_TIME = 500;

        public static final float DISMISS_TRIGGER_VELOCITY = 150.0f;
        public static final float DISMISS_DRAG_DISTANCE_FACTOR = 0.7f;

        public final int notification;

        private NotificationEntry mEntry;
        private float mDismissOnDragDistanceFarEnough;
        private boolean mDismissOnClick = true;
        private boolean mCloseBoardOnClick = true;

        private final int[] mMargin = {
            /* l */ 0,
            /* t */ 5,
            /* r */ 0,
            /* b */ 5,
        };

        RowView(Context context, NotificationEntry entry) {
            super(context);
            mEntry = entry;
            this.notification = entry.ID;
            setOnClickListener(mOnClickListenerRowView);
        }

        public void setDismissOnClick(boolean dismiss) {
            mDismissOnClick = dismiss;
        }

        public void setCloseBoardOnClick(boolean close) {
            mCloseBoardOnClick = close;
        }

        public void setMargin(int l, int t, int r, int b) {
            mMargin[0] = l; mMargin[1] = t; mMargin[2] = r; mMargin[3] = b;
            updateLayoutParams();
        }

        public NotificationEntry getNotification() {
            return mEntry;
        }

        public boolean canBeDismissed() {
            return !mEntry.ongoing;
        }

        public void dismiss(boolean anim) {
            if (canBeDismissed()) {
                if (!mPaused) {
                    mPaused = true;
                    mRowViewToRemove = 1;
                }

                if (anim) {
                    animateDismissX();
                } else {
                    doDismiss();
                }
            }
        }

        private void doDismiss() {
            mCenter.cancel(notification);
        }

        public void onScrollX(float offset) {
            setTranslationX(offset);
            if (canBeDismissed()) {
                if (mDismissOnDragDistanceFarEnough == 0) {
                    mDismissOnDragDistanceFarEnough = getMeasuredWidth() * DISMISS_DRAG_DISTANCE_FACTOR;
                }
                float alpha = Utils.getAlphaForOffset(
                    1.0f, 0.0f, 0.0f, mDismissOnDragDistanceFarEnough, Math.abs(offset));
                if (alpha < 0.0f) {
                    alpha = 0.0f;
                }
                setAlpha(alpha);
            }
        }

        public void onFlingX(float velocityX) {
            if (canBeDismissed() && (getTranslationX() == 0 || getTranslationX() > 0 == velocityX > 0)) {
                animateDismissX();
            } else {
                animateDragCancelX();
            }
        }

        public void onUpOrCancel() {
            if (getTranslationX() == 0) {
                return;
            }
            if (mDismissOnDragDistanceFarEnough == 0) {
                mDismissOnDragDistanceFarEnough = getMeasuredWidth() * DISMISS_DRAG_DISTANCE_FACTOR;
            }
            if (canBeDismissed() && Math.abs(getTranslationX()) > mDismissOnDragDistanceFarEnough) {
                animateDismissX();
            } else {
                animateDragCancelX();
            }
        }

        public void animateDismissX() {
            final int w = getMeasuredWidth();
            final float t = getTranslationX();
            int x;
            // if (t == 0) {
            //     x = mRandom.nextInt(2) > 0 ? w : -w;
            // } else {
            //     x = t > 0 ? w : -w;
            // }
            x = t >= 0 ? w : -w;

            animate().cancel();
            animate().alpha(0.0f).translationX(x)
                .setListener(mDismissAnimatorListener)
                .setDuration(DISMISS_TRANSITION_TIME)
                .start();
        }

        public void animateDragCancelX() {
            animate().cancel();
            animate().alpha(1.0f).translationX(0.0f)
                .setListener(mDragCancelAnimatorListener)
                .setDuration(DRAG_CANCEL_TRANSITION_TIME)
                .start();
        }

        public LinearLayout.LayoutParams makeLayoutParams() {
            final int w = LinearLayout.LayoutParams.MATCH_PARENT;
            final int h = LinearLayout.LayoutParams.WRAP_CONTENT;
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
            lp.leftMargin = mRowMargin[0];
            lp.topMargin = mRowMargin[1];
            lp.rightMargin = mRowMargin[2];
            lp.bottomMargin = mRowMargin[3];
            return lp;
        }

        private void updateLayoutParams() {
            final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) getLayoutParams();
            if (lp.leftMargin != mMargin[0] ||
                lp.topMargin != mMargin[1] ||
                lp.rightMargin != mMargin[2] ||
                lp.bottomMargin != mMargin[3]) {

                lp.leftMargin = mMargin[0];
                lp.topMargin = mMargin[1];
                lp.rightMargin = mMargin[2];
                lp.bottomMargin = mMargin[3];

                setLayoutParams(lp);
            }
        }

        private final AnimatorListener mDismissAnimatorListener = new AnimatorListener() {

                private boolean mCanceled;

                @Override
                public void onAnimationStart(Animator animation) {
                    if (DBG) Log.v(TAG, "RowView dismiss start");
                    mCanceled = false;
                    mAnimating = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!mCanceled) {
                        if (DBG) Log.v(TAG, "RowView dismiss end");
                        mAnimating = false;
                        doDismiss();
                        updatePendings();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    if (DBG) Log.v(TAG, "RowView dismiss cancel");
                    mCanceled = true;
                }
            };

        private final AnimatorListener mDragCancelAnimatorListener = new AnimatorListener() {

                private boolean mCanceled;

                @Override
                public void onAnimationStart(Animator animation) {
                    if (DBG) Log.v(TAG, "RowView drag cancel start");
                    mCanceled = false;
                    mAnimating = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!mCanceled) {
                        if (DBG) Log.v(TAG, "RowView drag cancel end");
                        mAnimating = false;
                        updatePendings();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    if (DBG) Log.v(TAG, "RowView drag cancel cancel");
                    mCanceled = true;
                }
            };
    }

    private final OnClickListener mOnClickListenerClearView = new OnClickListener() {

            @Override
            public void onClick(View view) {
                if (DBG) Log.v(TAG, "onClick - clear.");

                onClickClearView(view);
                removeAllRowViews();
                animateClose();
            }
        };

    private final OnClickListener mOnClickListenerRowView = new OnClickListener() {

            @Override
            public void onClick(View view) {
                RowView rowView = (RowView) view;
                NotificationEntry entry = rowView.mEntry;
                if (DBG) Log.v(TAG, "onClickRowView - " + rowView.notification);

                if (rowView.mDismissOnClick && entry.autoCancel) {
                    rowView.dismiss(false);
                }

                if (rowView.mCloseBoardOnClick) {
                    animateClose();
                }

                onClickRowView(rowView);
                if (entry.onClickListener != null) {
                    entry.onClickListener.onClick(view);
                }
            }
        };

    private void onClickClearView(View view) {
        mCallback.onClickClearView(this, view);
    }

    private void onClickRowView(RowView rowView) {
        mCallback.onClickRowView(this, rowView, rowView.mEntry);
    }

    public void dismissRowView(RowView rowView, boolean anim) {
        rowView.dismiss(anim);
    }

    private RowView makeRowView(NotificationEntry entry) {
        if (entry.showWhen && entry.whenFormatted == null) {
            entry.setWhen(null, entry.whenLong > 0L ?
                          entry.whenLong : System.currentTimeMillis());
        }

        RowView rowView = new RowView(mContext, entry);
        View view = mCallback.makeRowView(this, entry, mInflater);
        rowView.addView(view);
        return rowView;
    }

    private void addRowView(NotificationEntry entry) {
        if (DBG) Log.v(TAG, "addRowView - " + entry.ID);
        mInLayout = true;
        RowView rowView = makeRowView(entry);
        mContainer.addView(rowView, 0, rowView.makeLayoutParams());
        mCallback.onRowViewAdded(this, rowView, entry);
        removePendingArrive(entry);
    }

    private void removeRowView(NotificationEntry entry) {
        for (int i = 0, count = mContainer.getChildCount(); i < count; i++) {
            RowView rowView = (RowView) mContainer.getChildAt(i);
            if (entry.ID == rowView.notification) {
                removeRowView(rowView);
                break;
            }
        }
    }

    private void removeRowView(RowView rowView) {
        if (DBG) Log.v(TAG, "removeRowView - " + rowView.notification);
        mInLayout = true;
        mContainer.removeView(rowView);
        mCallback.onRowViewRemoved(this, rowView, rowView.mEntry);
        removePendingCancel(rowView.mEntry);

        if (mRowViewToRemove > 0) {
            mRowViewToRemove--;
            if (mRowViewToRemove == 0) {
                mPaused = false;
                updatePendings();
                if (mCloseOnRemovingRowView) {
                    mCloseOnRemovingRowView = false;
                    schedule(MSG_CLOSE, 0);
                }
            }
        }
    }

    private void removeAllRowViews() {
        mRowViewToRemove = mContainer.getChildCount();
        mPaused = mRowViewToRemove > 0;
        for (int i = 0, count = mRowViewToRemove; i < count; i++) {
            RowView rowView = (RowView) mContainer.getChildAt(i);
            schedule(MSG_REMOVE_ROW_VIEW, 1 /* anim */, 0, rowView, 200 * (i + 1));
        }
    }

    private void updateRowViews() {
        synchronized (mLock) {
            final int count = mCenter.getEntryCount();
            final int childCount = mContainer.getChildCount();
            if (DBG) Log.v(TAG, "updateRowViews - old: " + childCount + ", new: " + count);
            if (count != childCount) {
                ArrayList<NotificationEntry> entries = mCenter.getEntries();
                ArrayList<RowView> toRemove = null;
                for (int i = 0; i < childCount; i++) {
                    RowView rowView = (RowView) mContainer.getChildAt(i);
                    boolean found = false;
                    ListIterator<NotificationEntry> iter = entries.listIterator();
                    while (iter.hasNext()) {
                        NotificationEntry entry = iter.next();
                        if (entry.ID == rowView.notification) {
                            removePendingCancel(entry);
                            iter.remove();
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        if (toRemove == null) {
                            toRemove = new ArrayList<RowView>();
                        }
                        toRemove.add(rowView);
                    }
                }

                if (toRemove != null) {
                    for (RowView r : toRemove) {
                        removeRowView(r);
                    }
                }

                for (NotificationEntry entry : entries) {
                    addRowView(entry);
                }
            }
        }
    }

    private void addPendingArrive(NotificationEntry entry) {
        if (mPendingArrives == null) {
            mPendingArrives = new ArrayList<NotificationEntry>();
        }
        mPendingArrives.add(entry);
    }

    private void addPendingCancel(NotificationEntry entry) {
        if (mPendingCancels == null) {
            mPendingCancels = new ArrayList<NotificationEntry>();
        }
        mPendingCancels.add(entry);
    }

    private void removePendingArrive(NotificationEntry entry) {
        if (mPendingArrives != null && mPendingArrives.contains(entry)) {
            mPendingArrives.remove(entry);
        }
    }

    private void removePendingCancel(NotificationEntry entry) {
        if (mPendingCancels != null && mPendingCancels.contains(entry)) {
            mPendingCancels.remove(entry);
        }
    }

    private void updatePendings() {
        if (mPendingCancels != null && !mPendingCancels.isEmpty()) {
            for (NotificationEntry entry : mPendingCancels) {
                removeRowView(entry);
            }
            mPendingCancels.clear();
        }
        if (mPendingArrives != null && !mPendingArrives.isEmpty()) {
            for (NotificationEntry entry : mPendingArrives) {
                addRowView(entry);
            }
            mPendingArrives.clear();
        }
    }

    private void clearPendings() {
        if (mPendingCancels != null) {
            mPendingCancels.clear();
        }
        if (mPendingArrives != null) {
            mPendingArrives.clear();
        }
    }

    private void onPrepare() {
        if (mListeners != null) {
            for (StateListener l : mListeners) {
                l.onBoardPrepare(this);
            }
        }
    }

    private void onStartOpen() {
        if (mListeners != null) {
            for (StateListener l : mListeners) {
                l.onBoardStartOpen(this);
            }
        }
    }

    private void onEndOpen() {
        mOpened = true;
        updateRowViews();
        updatePendings();
        mContentView.setTranslationY(0.0f);

        if (mListeners != null) {
            for (StateListener l : mListeners) {
                l.onBoardEndOpen(this);
            }
        }
    }

    private void onCancelOpen() {
        if (mListeners != null) {
            for (StateListener l : mListeners) {
                l.onBoardCancelOpen(this);
            }
        }
    }

    private void onStartClose() {
        if (mListeners != null) {
            for (StateListener l : mListeners) {
                l.onBoardStartClose(this);
            }
        }
    }

    private void onEndClose() {
        mOpened = false;
        mShowing = false;
        mDismissed = true;
        clearPendings();
        setVisibility(GONE);

        if (mListeners != null) {
            for (StateListener l : mListeners) {
                l.onBoardEndClose(this);
            }
        }
    }

    private void onCancelClose() {
        if (mListeners != null) {
            for (StateListener l : mListeners) {
                l.onBoardCancelClose(this);
            }
        }
    }

    public void onBackKey() {
        if (mShowing) {
            animateClose();
        }
    }

    public void onHomeKey() {
        if (mShowing && mCloseOnHomeKey) {
            animateClose();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mInLayout = false;
        if (mFirstLayout) {
            mFirstLayout = false;
            mContentView.setTranslationY(-mContentView.getMeasuredHeight());
        }
    }

    private void initialize() {
        mContext = getContext();
        mInflater = LayoutInflater.from(mContext);
        mCenter = NotificationDelegater.getInstance().center();
        mCenter.addListener(this);
        mGestureDetector = new GestureDetectorCompat(mContext, this);
        mH = new H(this);

        mContentView = new ContentView(mContext);
        addView(mContentView,
                new FrameLayout.LayoutParams(
                    mContentView.mWidth, mContentView.mHeight,
                    Gravity.CENTER | Gravity.TOP));
    }

    private class ContentView extends LinearLayout {

        private final int[] mTouchToOpen = {
            /* l */ 0,
            /* t */ 0,
            /* r */ 0,
            /* b */ 0,
        };

        private int mWidth = FrameLayout.LayoutParams.MATCH_PARENT;
        private int mHeight = FrameLayout.LayoutParams.WRAP_CONTENT;

        ContentView(Context context) {
            super(context);
            setOrientation(LinearLayout.VERTICAL);

            mHeader = new HeaderView(context);
            mBody = new BodyView(context);
            mFooter = new FooterView(context);

            addView(mHeader);
            addView(mBody);
            addView(mFooter);
        }

        boolean isInsideTouch(MotionEvent event) {
            final float x = event.getX();
            final float y = event.getY();
            return y > getTop() && y < getBottom() && x > getLeft() && x < getRight();
        }

        boolean isInsideTouchToOpen(MotionEvent event) {
            final float x = event.getX();
            final float y = event.getY();
            return y > mTouchToOpen[1] && y < mTouchToOpen[3] &&
                x > mTouchToOpen[0] && x < mTouchToOpen[2];
        }

        void setTouchToOpen(int l, int t, int r, int b) {
            mTouchToOpen[0] = l; mTouchToOpen[1] = t; mTouchToOpen[2] = r; mTouchToOpen[3] = b;
        }

        void setDimension(int width, int height) {
            mWidth = width; mHeight = height;
        }

        int getLayoutParamsWidth() {
            return ((FrameLayout.LayoutParams) getLayoutParams()).width;
        }

        int getLayoutParamsHeight() {
            return ((FrameLayout.LayoutParams) getLayoutParams()).height;
        }

        void updateLayoutParams() {
            final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
            if (lp.width != mWidth || lp.height != mHeight) {
                lp.width = mWidth;
                lp.height = mHeight;
                setLayoutParams(lp);
            }
        }

        void animateOpen() {
            animate().cancel();
            animate().translationY(0.0f)
                .setListener(mOpenAnimatorListener)
                .setDuration(mOpenTransitionTime)
                .start();
        }

        void animateClose() {
            animate().cancel();
            animate().translationY(-getMeasuredHeight())
                .setListener(mCloseAnimatorListener)
                .setDuration(mCloseTransitionTime)
                .start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (mHeaderDivider != null) {
                final int l = mHeader.getLeft();
                final int r = mHeader.getRight();
                final int t = mHeader.getBottom() + mHeader.getBottomMargin() + getPaddingTop();
                final int b = t + mHeaderDividerHeight;

                mHeaderDivider.setBounds(l, t, r, b);
                mHeaderDivider.draw(canvas);
            }

            if (mFooterDivider != null) {
                final int l = mFooter.getLeft();
                final int r = mFooter.getRight();
                final int b = mFooter.getTop() - mFooter.getTopMargin();
                final int t = b - mFooterDividerHeight;

                mFooterDivider.setBounds(l, t, r, b);
                mFooterDivider.draw(canvas);
            }
        }
    }

    private class SubContentView extends FrameLayout {

        private final int[] mMargin = {
            /* l */ 0,
            /* t */ 0,
            /* r */ 0,
            /* b */ 0,
        };

        protected int mWidth = ViewGroup.LayoutParams.MATCH_PARENT;
        protected int mHeight = ViewGroup.LayoutParams.WRAP_CONTENT;

        SubContentView(Context context) {
            super(context);
        }

        public boolean isInsideTouch(MotionEvent event) {
            final float x = event.getX();
            final float y = event.getY();
            return y > getTop() && y < getBottom() && x > getLeft() && x < getRight();
        }

        public void setWidth(int width) {
            mWidth = width;
        }

        public void setHeight(int height) {
            mHeight = height;
        }

        public void setMargin(int l, int t, int r, int b) {
            mMargin[0] = l; mMargin[1] = t; mMargin[2] = r; mMargin[3] = b;
        }

        public int getLeftMargin() {
            return ((LinearLayout.LayoutParams) getLayoutParams()).leftMargin;
        }

        public int getTopMargin() {
            return ((LinearLayout.LayoutParams) getLayoutParams()).topMargin;
        }

        public int getRightMargin() {
            return ((LinearLayout.LayoutParams) getLayoutParams()).rightMargin;
        }

        public int getBottomMargin() {
            return ((LinearLayout.LayoutParams) getLayoutParams()).bottomMargin;
        }

        public int getSuggestedWidth() {
            return getChildCount() > 0 && mWidth != 0 ? mWidth : 0;
        }

        public int getSuggestedHeight() {
            return getChildCount() > 0 && mHeight != 0 ? mHeight : 0;
        }

        public void updateMargin() {
            final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) getLayoutParams();
            if (lp.leftMargin != mMargin[0] ||
                lp.topMargin != mMargin[1] ||
                lp.rightMargin != mMargin[2] ||
                lp.bottomMargin != mMargin[3]) {

                lp.leftMargin = mMargin[0];
                lp.topMargin = mMargin[1];
                lp.rightMargin = mMargin[2];
                lp.bottomMargin = mMargin[3];

                setLayoutParams(lp);
            }
        }

        public void updateDimension() {
            if (getChildCount() > 0) {
                final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) getLayoutParams();
                if (mWidth != 0 && lp.width != mWidth) {
                    lp.width = mWidth;
                }
                if (mHeight != 0 && lp.height != mHeight) {
                    lp.height = mHeight;
                }
                setLayoutParams(lp);
            }
        }
    }

    public class HeaderView extends SubContentView {

        HeaderView(Context context) {
            super(context);
            setHeight(HEADER_HEIGHT);
        }
    }

    public class FooterView extends SubContentView {

        FooterView(Context context) {
            super(context);
            setHeight(FOOTER_HEIGHT);
        }
    }

    public class BodyView extends SubContentView {
        Scroller scroller;

        BodyView(Context context) {
            super(context);

            scroller = new Scroller(context);
            addView(scroller, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }

        public View findViewByTouch(MotionEvent event) {
            return scroller.findViewByTouch(event);
        }

        @Override
        public int getSuggestedHeight() {
            int bodyHeight = mBodyHeight;
            if (bodyHeight <= 0) {
                final int contentViewHeight = mContentView.getLayoutParamsHeight();
                if (contentViewHeight > 0) {
                    bodyHeight = contentViewHeight;
                } else {
                    final Resources res = getResources();
                    bodyHeight = res.getDisplayMetrics().heightPixels;

                    // status bar
                    if (mStatusBarHeight == 0) {
                        int resId = res.getIdentifier("status_bar_height", "dimen", "android");
                        if (resId > 0) {
                            mStatusBarHeight = res.getDimensionPixelSize(resId);
                        }
                    }
                    bodyHeight -= mStatusBarHeight;
                }

                // contentView
                bodyHeight -= mContentView.getPaddingTop() + mContentView.getPaddingBottom();

                // header
                bodyHeight -= mHeader.getSuggestedHeight() +
                    mHeader.getTopMargin() + mHeader.getBottomMargin();

                if (mHeaderDivider != null) {
                    bodyHeight -= mHeaderDividerHeight;
                }

                // body
                bodyHeight -= mBody.getTopMargin() + mBody.getBottomMargin();

                // footer
                bodyHeight -= mFooter.getSuggestedHeight() +
                    mFooter.getTopMargin() + mFooter.getBottomMargin();

                if (mFooterDivider != null) {
                    bodyHeight -= mFooterDividerHeight;
                }
            }
            return bodyHeight;
        }
    }

    private class Scroller extends ScrollView {
        LinearLayout container;

        Scroller(Context context) {
            super(context);

            setOverScrollMode(OVER_SCROLL_ALWAYS);

            container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
            addView(container);
            mContainer = container;
        }

        public View findViewByTouch(MotionEvent event) {
            final int pos = getScrollY() + (int) event.getY() -
                mHeader.getTopMargin() - mHeader.getBottomMargin() -
                mHeader.getSuggestedHeight() - mBody.getTopMargin();
            for (int i = 0, count = container.getChildCount(); i < count; i++) {
                View child = container.getChildAt(i);
                if (child.getBottom() > pos) {
                    return child;
                }
            }
            return null;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            final int height = mBody.getSuggestedHeight();
            if (height > 0) {
                if (mContentView.getLayoutParamsHeight() != FrameLayout.LayoutParams.WRAP_CONTENT ||
                    getMeasuredHeight() > height) {
                    setMeasuredDimension(getMeasuredWidth(), height);
                }
            }
        }
    }

    private static final int MSG_REMOVE_ROW_VIEW = 0;
    private static final int MSG_CLOSE = 1;

    private H mH;

    private void cancel(int what) {
        if (what == -1) {
            mH.removeCallbacksAndMessages(null);
        } else {
            mH.removeMessages(what);
        }
    }

    private void schedule(int what, int delay) {
        mH.sendEmptyMessageDelayed(what, delay);
    }

    private void schedule(int what, int arg1, int arg2, Object obj, int delay) {
        mH.sendMessageDelayed(mH.obtainMessage(what, arg1, arg2, obj), delay);
    }

    private static class H extends Handler {
        private WeakReference<NotificationBoard> mBoard;

        H(NotificationBoard board) {
            super();
            mBoard = new WeakReference<NotificationBoard>(board);
        }

        @Override
        public void handleMessage(Message msg) {
            NotificationBoard b = mBoard.get();
            if (b == null) return;

            switch (msg.what) {
            case MSG_REMOVE_ROW_VIEW:
                b.dismissRowView((RowView) msg.obj, msg.arg1 == 1);
                break;

            case MSG_CLOSE:
                b.animateClose();
                break;
            }
        }
    }
}
