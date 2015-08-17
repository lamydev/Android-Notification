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
import android.animation.ArgbEvaluator;
import android.animation.IntEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.animation.Animation;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import android.support.v4.view.GestureDetectorCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Notification view.
 *
 * Callback {@link NotificationViewCallback} must be set before this view is displayed,
 * otherwise exception {@link CallbackNotFoundException} will be thrown.
 *
 * @see NotificationView#setCallback.
 *
 * SDK Ver. >= {@link android.os.Build.VERSION_CODES.HONEYCOMB}.
 */
public class NotificationView extends FrameLayout
        implements GestureDetector.OnGestureListener,
                   GestureDetector.OnDoubleTapListener {

    private String TAG = "zemin.NotificationView";
    public static boolean DBG;

    public static final int NOTIFICATION_DISPLAY_TIME = 3000;

    public static final int BACKGROUND_TRANSITION_TIME = 1000;
    public static final int SHOW_TRANSITION_TIME = 500;
    public static final int HIDE_TRANSITION_TIME = 500;

    public static final float DEFAULT_CORNER_RADIUS = 8.0f;
    public static final int DEFAULT_BACKGROUND_COLOR = 0xfffafafa;

    public static final int DISMISS_FREEZE_TIME = 1200;
    public static final float DISMISS_GESTURE_VELOCITY = 120.0f;
    public static final float DISMISS_DRAG_DISTANCE_FACTOR = 0.7f;
    public static final int DRAG_OUT_TRANSITION_TIME = 500;
    public static final int DRAG_CANCEL_TRANSITION_TIME = 500;

    public static final int GESTURE_CONSUMER_DEFAULT = 0;
    public static final int GESTURE_CONSUMER_USER = 1;

    public static final int X = 0;
    public static final int Y = 1;

    public static final int DEFAULT_GRAVITY = Gravity.CENTER | Gravity.TOP;

    private final ArrayList<NotificationEntry> mEntries =
        new ArrayList<NotificationEntry>();

    private ArrayList<StateListener> mListeners = null;

    private final Object mEntryLock = new Object();

    /** [0] w [1] h */
    private final int[] mDimension = new int[2];

    /** [0] l [1] t [2] r [3] b */
    private final int[] mContentPadding = new int[4];

    /** [0] l [1] t [2] r [3] b */
    private final int[] mContentMargin = new int[4];

    private Context mContext;
    private NotificationViewCallback mCallback;
    private NotificationHandler mNotificationHandler;
    private NotificationEntry mLastEntry;
    private NotificationEntry mPendingEntry;
    private GestureListener mGestureListener;
    private GestureDetectorCompat mGestureDetector;

    private View mContentView;
    private View mTargetContentView;
    private View mDefaultContentView;
    private int mDefaultLayoutId;
    private int mCurrentLayoutId;
    private int mGravity;
    private int mShowTransitionTime = SHOW_TRANSITION_TIME;
    private int mHideTransitionTime = HIDE_TRANSITION_TIME;
    private int mNotiDisplayTime = NOTIFICATION_DISPLAY_TIME;

    private ChildViewManager mChildViewManager;

    private ObjectAnimator mBackgroundColorAnimator;
    private ObjectAnimator mBackgroundAlphaAnimator;
    private AnimationListener mHideAnimationListener;
    private ContentViewSwitcher mContentViewSwitcher;
    private boolean mTransitionEnabled = true;
    private boolean mShowHideAnimEnabled = true;
    private Animation mDefaultShowAnimation;
    private Animation mDefaultHideAnimation;
    private Animation mShowAnimation;
    private Animation mHideAnimation;

    private Drawable mBackground;
    private Drawable mContentBackground;
    private int mStrokeWidth;
    private int mStrokeColor;
    private float mCornerRadius;
    private int mDefaultBackgroundColor = DEFAULT_BACKGROUND_COLOR;
    private int mDefaultBackgroundAlpha = 0xff;
    private boolean mShadowEnabled;
    private int mBackgroundTransitionTime = BACKGROUND_TRANSITION_TIME;

    private boolean mDismissOnHomeKey = false;
    private float mDismissOnDragDistanceFarEnough;
    private boolean mDismissableOnGesture = true;
    private boolean mDismissOnGestureEnabled;
    private boolean mDismissOnClick = true;
    private int mDirection = -1;
    private int mGestureConsumer;

    /**
     * Monitor the state of this view.
     */
    public interface StateListener {

        /**
         * Called when this view starts ticking.
         *
         * @param view
         */
        void onViewTicking(NotificationView view);

        /**
         * Called when this view is dismissed.
         *
         * @param view
         */
        void onViewDismiss(NotificationView view);
    }

    /**
     * A convenience class to extend when you only want to listen for a subset
     * of all states. This implements all methods in the {@link StateListener}.
     */
    public static class SimpleStateListener implements StateListener {

        public void onViewTicking(NotificationView view) {}
        public void onViewDismiss(NotificationView view) {}
    }

    public NotificationView(Context context) {
        super(context);
    }

    public NotificationView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void initialize(NotificationHandler handler) {
        TAG += "@" + handler.toSimpleString();
        mContext = getContext();
        mNotificationHandler = handler;
        mContentViewSwitcher = new ContentViewSwitcher();
        mChildViewManager = new ChildViewManager();
        mGestureDetector = new GestureDetectorCompat(mContext, this);
        mContentBackground = new GradientDrawable();
        mDimension[0] = LayoutParams.MATCH_PARENT;
        mDimension[1] = LayoutParams.WRAP_CONTENT;
        mHideAnimationListener = mDismissAnimationListener;
        mDefaultShowAnimation = AnimationFactory.pushDownIn();
        mDefaultHideAnimation = AnimationFactory.pushUpOut();
    }

    /**
     * Whether the callback {@link NotificationViewCallback} has been set.
     *
     * @return boolean
     */
    public boolean hasCallback() {
        return mCallback != null;
    }

    /**
     * Set the callback. If not set, exception {@link CallbackNotFoundException}
     * will be thrown when starts ticking.
     *
     * @param cb
     */
    public void setCallback(NotificationViewCallback cb) {
        if (mCallback != cb) {
            mCallback = cb;
            addState(CALLBACK_CHANGED);
        }
    }

    /**
     * Whether this view is enabled.
     *
     * @return boolean
     */
    public boolean isViewEnabled() {
        return hasState(ENABLED);
    }

    /**
     * Enable/disable this view. If disabled, any notification delivered here will be ignored.
     *
     * @param enable
     */
    public void setViewEnabled(boolean enable) {
        if (hasState(ENABLED) != enable) {
            if (DBG) Log.v(TAG, "enable - " + enable);
            if (enable) {
                addState(ENABLED);
            } else {
                clearState(ENABLED);
            }
        }
    }

    /**
     * Whether this view is ticking.
     *
     * @return boolean
     */
    public boolean isTicking() {
        return hasState(TICKING) && !hasState(DISMISSED);
    }

    /**
     * Enable/disable transition between notifications. If disabled,
     *
     * @param enable
     */
    public void setNotificationTransitionEnabled(boolean enable) {
        if (mTransitionEnabled != enable) {
            if (DBG) Log.v(TAG, "transition enable - " + enable);
            mTransitionEnabled = enable;
        }
    }

    /**
     *
     */
    public boolean isNotificationTransitionEnabled() {
        return mTransitionEnabled;
    }

    /**
     * Add state listener.
     *
     * @see NotificationView#StateListener
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
     * Remove state listener.
     *
     * @see NotificationView#StateListener
     *
     * @param l
     */
    public void removeStateListener(StateListener l) {
        if (mListeners != null && mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    /**
     * Set gesture listener.
     *
     * @see GestureListener
     *
     * @param l
     */
    public void setGestureListener(GestureListener l) {
        mGestureListener = l;
    }

    /**
     * Whether it is paused.
     *
     * @return boolean
     */
    public boolean isPaused() {
        return hasState(PAUSED);
    }

    /**
     * Pause. Any notification delivered here will be suspended.
     */
    public void pause() {
        if (hasState(TICKING) && !hasState(PAUSED)) {
            if (DBG) Log.v(TAG, "pause. " + mEntries.size());
            mContentView.animate().cancel();
            addState(PAUSED);
            cancel(-1);
        }
    }

    /**
     * Resume.
     */
    public void resume() {
        if (hasState(PAUSED)) {
            if (DBG) Log.v(TAG, "resume.");
            clearState(PAUSED);

            if (mContentView.isShown()) {
                schedule(MSG_SHOW, mNotiDisplayTime);
                return;
            }

            if (!mEntries.isEmpty()) {
                addState(TICKING);
                schedule(MSG_START, mNotiDisplayTime);
            }
        }
    }

    /**
     * Whether to dismiss when this view get clicked.
     *
     * @param dismiss
     */
    public void setDismissOnClick(boolean dismiss) {
        mDismissOnClick = dismiss;
    }

    /**
     * Whether to dismiss when home key is pressed.
     *
     * @param dismiss
     */
    public void setDismissOnHomeKey(boolean dismiss) {
        mDismissOnHomeKey = dismiss;
    }

    /**
     * Set notification display time.
     *
     * @param ms
     */
    public void setDisplayTime(int ms) {
        mNotiDisplayTime = ms;
    }

    /**
     * Set background transition time.
     *
     * @param ms
     */
    public void setBackgroundTransitionTime(int ms) {
        mBackgroundTransitionTime = ms;
        addState(CONTENT_BACKGROUND_CHANGED_MINOR);
    }

    /**
     * Set default background color.
     *
     * @param color
     */
    public void setDefaultBackgroundColor(int color) {
        mDefaultBackgroundColor = color;
    }

    /**
     * Set default background alpha.
     *
     * @param alpha
     */
    public void setDefaultBackgroundAlpha(int alpha) {
        mDefaultBackgroundAlpha = alpha;
    }

    /**
     * Get background.
     *
     * @return Drawable
     */
    public Drawable getContentBackground() {
        return mContentBackground;
    }

    /**
     * Set background.
     *
     * @param b
     */
    public void setContentBackground(Drawable b) {
        mContentBackground = b;
        addState(CONTENT_BACKGROUND_CHANGED);
    }

    /**
     * Enable/disable shadow background.
     *
     * @param enable
     */
    public void setShadowEnabled(boolean enable) {
        mShadowEnabled = enable;
        addState(CONTENT_BACKGROUND_CHANGED);
    }

    /**
     * Set background corner radius.
     *
     * @param r
     */
    public void setCornerRadius(float r) {
        mCornerRadius = r;
        addState(CONTENT_BACKGROUND_CHANGED);
    }

    /**
     * Set background stroke.
     */
    public void setStroke(int width, int color) {
        mStrokeWidth = width;
        mStrokeColor = color;
        addState(CONTENT_BACKGROUND_CHANGED_MINOR);
    }

    /**
     * Set padding.
     *
     * @param l
     * @param t
     * @param r
     * @param b
     */
    public void setContentPadding(int l, int t, int r, int b) {
        mContentPadding[0] = l;
        mContentPadding[1] = t;
        mContentPadding[2] = r;
        mContentPadding[3] = b;
    }

    /**
     * Set margin.
     *
     * @param l
     * @param t
     * @param r
     * @param b
     */
    public void setContentMargin(int l, int t, int r, int b) {
        mContentMargin[0] = l;
        mContentMargin[1] = t;
        mContentMargin[2] = r;
        mContentMargin[3] = b;
    }

    /**
     * Set gravity.
     *
     * @param gravity
     */
    public void setGravity(int gravity) {
        mGravity = gravity;
    }

    /**
     * Set dimension.
     *
     * @param width
     * @param height
     */
    public void setDimension(int width, int height) {
        mDimension[0] = width;
        mDimension[1] = height;
    }

    /**
     * Enable/disable show/hide animation
     *
     * @param enable
     */
    public void setShowHideAnimationEnabled(boolean enable) {
        mShowHideAnimEnabled = enable;
    }

    /**
     * Set transition time of show animation.
     *
     * @param ms
     */
    public void setShowTransitionTime(int ms) {
        mShowTransitionTime = ms;
    }

    /**
     * Set transition time of hide animation.
     *
     * @param ms
     */
    public void setHideTransitionTime(int ms) {
        mHideTransitionTime = ms;
    }

    /**
     * Set show animation (AnimationListener and duration will be replaced).
     *
     * @param anim
     */
    public void setShowAnimation(Animation anim) {
        mShowAnimation = anim;
    }

    /**
     * Set hide animation (AnimationListener and duration will be replaced).
     *
     * @param anim
     */
    public void setHideAnimation(Animation anim) {
        mHideAnimation = anim;
    }

    /**
     * @return ContentViewSwitcher
     */
    public ContentViewSwitcher getContentViewSwitcher() {
        return mContentViewSwitcher;
    }

    /**
     * @return ChildViewManager
     */
    public ChildViewManager getChildViewManager() {
        return mChildViewManager;
    }

    /**
     * Whether the contentView has been changed.
     *
     * @return boolean
     */
    public boolean isContentLayoutChanged() {
        return hasState(CONTENT_CHANGED);
    }

    /**
     * Get layout resource ID of the current contentView.
     *
     * @return int
     */
    public int getCurrentLayoutId() {
        return mCurrentLayoutId;
    }

    /**
     * Get last notification {@link NotificationEntry}.
     *
     * @return NotificationEntry
     */
    public NotificationEntry getLastNotification() {
        return mLastEntry;
    }

    /**
     * Whether the contentView has been created.
     *
     * @return boolean
     */
    public boolean hasContentView() {
        return mContentView != null;
    }

    /**
     * Whether the contentView is currently visible.
     *
     * @return boolean
     */
    public boolean isContentViewShown() {
        return mContentView.isShown();
    }

    /**
     * Set visibility of the contentView.
     *
     * @param vis
     */
    public void setContentViewVisibility(int vis) {
        if (DBG) Log.v(TAG, "setContentVisibility - vis=" + vis);
        mContentView.setVisibility(vis);
    }

    /**
     * Set contentView.
     *
     * @param resId
     */
    public void setContentView(int resId) {
        if (mCurrentLayoutId != resId) {
            View view = inflate(mContext, resId, null);
            if (mDefaultContentView == null &&
                resId == mCallback.getContentViewDefaultLayoutId(this)) {
                mDefaultContentView = view;
                mDefaultLayoutId = resId;
            }
            mCurrentLayoutId = resId;
            setContentView(view);
        }
    }

    void setContentView(View view) {
        if (mContentView == view || view == null) return;
        if (mContentView != null) {
            mContentViewSwitcher.start(view);
        } else {
            addState(CONTENT_BACKGROUND_CHANGED);
            setContentViewInner(view);
        }
    }

    private void setContentViewInner(View view) {
        addState(CONTENT_CHANGED);
        removeAllViews();
        clearLastEntry();
        mDismissOnDragDistanceFarEnough = 0;
        view.setBackground(null);
        mChildViewManager.clear();
        mContentView = view;
        mCallback.onContentViewChanged(this, view, mCurrentLayoutId);
        updateContentBackground();
    }

    private void updateContentBackground() {
        Drawable background = mBackground;
        if (hasState(CONTENT_BACKGROUND_CHANGED)) {
            if (mContentBackground != null) {
                if (mShadowEnabled) {
                    final Drawable[] layers = new Drawable[] {
                        getResources().getDrawable(android.R.drawable.dialog_holo_light_frame),
                        mContentBackground,
                    };
                    background = new LayerDrawable(layers);

                    int l, t, r, b;
                    l = t = r = b = 0;

                    for (int i = 0, size = layers.length; i < size; i++) {
                        Rect rect = new Rect();
                        layers[i].getPadding(rect);

                        l += rect.left;
                        t += rect.top;
                        r += rect.right;
                        b += rect.bottom;
                    }
                    setContentPadding(l, t, r, b);

                } else {

                    background = mContentBackground;

                    Rect rect = new Rect();
                    background.getPadding(rect);
                    setContentPadding(rect.left, rect.top, rect.right, rect.bottom);
                }

                if (mContentBackground instanceof GradientDrawable) {
                    GradientDrawable b = (GradientDrawable) mContentBackground;
                    b.setCornerRadius(mCornerRadius);
                    b.setStroke(mStrokeWidth, mStrokeColor);

                    if (mBackgroundColorAnimator != null) {
                        mBackgroundColorAnimator.cancel();
                        mBackgroundColorAnimator = null;
                    }

                    ColorProperty colorProperty = new ColorProperty();
                    mBackgroundColorAnimator = ObjectAnimator.ofObject(
                        b, colorProperty, new ArgbEvaluator(), 0, 0);
                    mBackgroundColorAnimator.setDuration(mBackgroundTransitionTime);

                    if (mBackgroundAlphaAnimator != null) {
                        mBackgroundAlphaAnimator.cancel();
                        mBackgroundAlphaAnimator = null;
                    }

                    AlphaProperty alphaProperty = new AlphaProperty();
                    mBackgroundAlphaAnimator = ObjectAnimator.ofObject(
                        b, alphaProperty, new IntEvaluator(), 0, 0);
                    mBackgroundAlphaAnimator.setDuration(mBackgroundTransitionTime);
                }
            }

            clearState(CONTENT_BACKGROUND_CHANGED);
            clearState(CONTENT_BACKGROUND_CHANGED_MINOR);

        } else if (hasState(CONTENT_BACKGROUND_CHANGED_MINOR)) {
            if (mContentBackground instanceof GradientDrawable) {
                GradientDrawable b = (GradientDrawable) mContentBackground;
                b.setStroke(mStrokeWidth, mStrokeColor);
                mBackgroundColorAnimator.setDuration(mBackgroundTransitionTime);
                mBackgroundAlphaAnimator.setDuration(mBackgroundTransitionTime);
            }

            clearState(CONTENT_BACKGROUND_CHANGED_MINOR);
        }

        mBackground = background;
        mContentView.setBackground(background);
    }

    private void updateContentBackgroundColor(NotificationEntry entry) {
        if (entry.backgroundColor == 0) {
            entry.backgroundColor = mDefaultBackgroundColor;
        }
        final int lastColor = mLastEntry != null ?
            mLastEntry.backgroundColor : Color.WHITE;
        final int currColor = entry.backgroundColor;
        if (lastColor != currColor) {
            mBackgroundColorAnimator.cancel();
            mBackgroundColorAnimator.setIntValues(lastColor, currColor);
            mBackgroundColorAnimator.start();
        }
    }

    private void updateContentBackgroundAlpha(NotificationEntry entry) {
        if (entry.backgroundAlpha == NotificationEntry.INVALID) {
            entry.backgroundAlpha = mDefaultBackgroundAlpha;
        }
        final int lastAlpha = mLastEntry != null ?
            mLastEntry.backgroundAlpha : 0xff;
        final int currAlpha = entry.backgroundAlpha;
        if (lastAlpha != currAlpha) {
            mBackgroundAlphaAnimator.cancel();
            mBackgroundAlphaAnimator.setIntValues(lastAlpha, currAlpha);
            mBackgroundAlphaAnimator.start();
        }
    }

    private void showNotification(NotificationEntry entry) {
        mCallback.onShowNotification(this, mContentView, entry, mCurrentLayoutId);
        updateContentBackground();
        updateContentBackgroundColor(entry);
        updateContentBackgroundAlpha(entry);
    }

    private void refreshContentView() {
        refreshContentView(mContentView);
    }

    private void refreshContentView(View target) {
        mContentView.setBackground(null);
        setContentViewInner(target);
        schedule(MSG_START);
    }

    /**
     * Set the x translation of contentView.
     *
     * @param x
     */
    public void setContentViewTranslationX(float x) {
        // if (DBG) Log.v(TAG, "setContentViewTranslationX - x=" + x);
        mContentView.setTranslationX(x);
        mDirection = X;
    }

    /**
     * Get the x translation of contentView.
     *
     * @return float
     */
    public float getContentViewTranslationX() {
        return mContentView.getTranslationX();
    }

    /**
     * Set the y translation of contentView.
     *
     * @param y
     */
    public void setContentViewTranslationY(float y) {
        // if (DBG) Log.v(TAG, "setContentViewTranslationY - y=" + y);
        mContentView.setTranslationY(y);
        mDirection = Y;
    }

    /**
     * Get the y translation of contentView.
     *
     * @return float
     */
    public float getContentViewTranslationY() {
        return mContentView.getTranslationY();
    }

    /**
     * Set the opacity of contentView.
     *
     * @param alpha
     */
    public void setContentViewAlpha(float alpha) {
        // if (DBG) Log.v(TAG, "setContentViewAlpha - alpha=" + alpha);
        mContentView.setAlpha(alpha);
    }

    /**
     * Get the opacity of contentView.
     *
     * @return float
     */
    public float getContentViewAlpha() {
        return mContentView.getAlpha();
    }

    /**
     * Set the x degree that contentView is rotated.
     *
     * @param x
     */
    public void setContentViewRotationX(float x) {
        // if (DBG) Log.v(TAG, "setContentViewRotationX - degree=" + x);
        mContentView.setRotationX(x);
    }

    /**
     * Get the x degree that contentView is rotated.
     *
     * @return float
     */
    public float getContentViewRotationX() {
        return mContentView.getRotationX();
    }

    /**
     * Set the y degree that contentView is rotated.
     *
     * @param y
     */
    public void setContentViewRotationY(float y) {
        // if (DBG) Log.v(TAG, "setContentViewRotationY - degree=" + y);
        mContentView.setRotationY(y);
    }

    /**
     * Get the y degree that contentView is rotated.
     *
     * @return float
     */
    public float getContentViewRotationY() {
        return mContentView.getRotationY();
    }

    /**
     * Set the x location of pivot point around which the contentView is rotated.
     *
     * @param x
     */
    public void setContentViewPivotX(float x) {
        if (DBG) Log.v(TAG, "setContentViewPivotX - x=" + x);
        mContentView.setPivotY(x);
    }

    /**
     * Get the x location of pivot point around which the contentView is rotated.
     *
     * @return float
     */
    public float getContentViewPivotX() {
        return mContentView.getPivotX();
    }

    /**
     * Set the y location of pivot point around which the contentView is rotated.
     *
     * @param y
     */
    public void setContentViewPivotY(float y) {
        if (DBG) Log.v(TAG, "setContentViewPivotY - y=" + y);
        mContentView.setPivotY(y);
    }

    /**
     * Get the y location of pivot point around which the contentView is rotated.
     *
     * @return float
     */
    public float getContentViewPivotY() {
        return mContentView.getPivotY();
    }

    /**
     * Rotate the contentView to x degree with animation.
     *
     * @param degree
     * @param alpha
     * @param listener
     * @param duration
     */
    public void animateContentViewRotationX(float degree, float alpha,
                                            Animator.AnimatorListener listener,
                                            int duration) {

        if (DBG) Log.v(TAG, "animateContentViewRotationX - " +
                       "degree=" + degree + ", alpha=" + alpha);

        mContentView.animate().cancel();
        mContentView.animate()
            .alpha(alpha)
            .rotationX(degree)
            .setListener(listener)
            .setDuration(duration)
            .start();
    }

    /**
     * Rotate the contentView to y degree with animation.
     *
     * @param degree
     * @param alpha
     * @param listener
     * @param duration
     */
    public void animateContentViewRotationY(float degree, float alpha,
                                            Animator.AnimatorListener listener,
                                            int duration) {

        if (DBG) Log.v(TAG, "animateContentViewRotationY - " +
                       "degree=" + degree + ", alpha=" + alpha);

        mContentView.animate().cancel();
        mContentView.animate()
            .alpha(alpha)
            .rotationY(degree)
            .setListener(listener)
            .setDuration(duration)
            .start();
    }

    /**
     * Move the contentView to x position with animation.
     *
     * @param x
     * @param alpha
     * @param listener
     * @param duration
     */
    public void animateContentViewTranslationX(float x, float alpha,
                                               Animator.AnimatorListener listener,
                                               int duration) {

        if (DBG) Log.v(TAG, "animateContentViewTranslationX - " +
                       "x=" + x + ", alpha=" + alpha);

        mContentView.animate().cancel();
        mContentView.animate()
            .alpha(alpha)
            .translationX(x)
            .setListener(listener)
            .setDuration(duration)
            .start();
    }

    /**
     * Move the contentView to y position with animation.
     *
     * @param y
     * @param alpha
     * @param listener
     * @param duration
     */
    public void animateContentViewTranslationY(float y, float alpha,
                                               Animator.AnimatorListener listener,
                                               int duration) {

        if (DBG) Log.v(TAG, "animateContentViewTranslationY - " +
                       "y=" + y + ", alpha=" + alpha);

        mContentView.animate().cancel();
        mContentView.animate()
            .alpha(alpha)
            .translationY(y)
            .setListener(listener)
            .setDuration(duration)
            .start();
    }

    /**
     * Call this to perform a self-switch animation.
     */
    public void startInOutTransition() {
        mContentViewSwitcher.start();
    }

    /**
     * Dismiss this view.
     */
    public void dismiss() {
        mContentViewSwitcher.start();
    }

    void onArrival(NotificationEntry entry) {
        synchronized (mEntryLock) {
            if (hasState(PAUSED) || entry.silentMode) {
                mNotificationHandler.onSendFinished(entry);
                return;
            }

            ListIterator<NotificationEntry> iter = mEntries.listIterator();
            int index = mEntries.size();
            while (iter.hasNext()) {
                if (entry.priority.higher(iter.next().priority)) {
                    index = iter.nextIndex() - 1; break;
                }
            }
            mEntries.add(index, entry);
            if (!hasState(TICKING)) {
                addState(TICKING);
                schedule(MSG_START);
            }
        }
    }

    void onUpdate(NotificationEntry entry) {
        synchronized (mEntryLock) {
            if (mLastEntry == entry) {
                schedule(MSG_UPDATE_NOTIFICATION, 0, 0, entry, 0);
                mNotificationHandler.onUpdateFinished(entry);
            } else if (!mEntries.contains(entry)) {
                onArrival(entry);
            }
        }
    }

    void onCancel(NotificationEntry entry) {
        synchronized (mEntryLock) {
            if (mEntries.contains(entry)) {
                mEntries.remove(entry);
            } else if (mLastEntry == entry) {
                mContentViewSwitcher.start();
            }
        }
        mNotificationHandler.onCancelFinished(entry);
    }

    void onCancelAll() {
        mContentViewSwitcher.start();
        mNotificationHandler.onCancelAllFinished();
    }

    private void clearLastEntry() {
        if (mLastEntry != null) {
            if (mLastEntry.nohistory) {
                mNotificationHandler.reportCanceled(mLastEntry);
            }
            if (mLastEntry.autoSilentMode) {
                mLastEntry.silentMode = true;
            }
        }
        mLastEntry = null;
    }

    public void sendPendings() {
        synchronized (mEntryLock) {
            for (NotificationEntry entry : mEntries) {
                mNotificationHandler.onSendFinished(entry);
            }
            mEntries.clear();
        }
    }

    private void show() {
        if (mContentView.getParent() == null) {
            addView(mContentView);
        }

        // reset
        mContentView.setTranslationX(0.0f);
        mContentView.setRotationX(0.0f);
        mContentView.setAlpha(1.0f);

        mContentView.setPadding(
            mContentPadding[0], mContentPadding[1],
            mContentPadding[2], mContentPadding[3]);

        if (mGravity == 0) {
            mGravity = DEFAULT_GRAVITY;
        }

        final LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
        if (lp.leftMargin != mContentMargin[0] ||
            lp.topMargin != mContentMargin[1] ||
            lp.rightMargin != mContentMargin[2] ||
            lp.bottomMargin != mContentMargin[3] ||
            lp.width != mDimension[0] ||
            lp.height != mDimension[1] ||
            lp.gravity != mGravity) {

            lp.leftMargin = mContentMargin[0];
            lp.topMargin = mContentMargin[1];
            lp.rightMargin = mContentMargin[2];
            lp.bottomMargin = mContentMargin[3];
            lp.width = mDimension[0];
            lp.height = mDimension[1];
            lp.gravity = mGravity;

            mContentView.setLayoutParams(lp);
        }

        mContentView.setVisibility(VISIBLE);
        if (mShowHideAnimEnabled) {
            if (mShowAnimation == null) {
                mShowAnimation = mDefaultShowAnimation;
            }
            if (mShowTransitionTime == 0) {
                mShowTransitionTime = SHOW_TRANSITION_TIME;
            }
            mShowAnimation.setAnimationListener(mShowAnimationListener);
            mShowAnimation.setDuration(mShowTransitionTime);
            mContentView.startAnimation(mShowAnimation);
        }
    }

    private void hide() {
        if (mShowHideAnimEnabled) {
            if (mHideAnimation == null) {
                mHideAnimation = mDefaultHideAnimation;
            }
            if (mHideTransitionTime == 0) {
                mHideTransitionTime = HIDE_TRANSITION_TIME;
            }
            mHideAnimation.setAnimationListener(mHideAnimationListener);
            mHideAnimation.setDuration(mHideTransitionTime);
            mContentView.startAnimation(mHideAnimation);
        } else {
            mContentView.setVisibility(GONE);
            if (hasState(DISMISSING)) {
                onDismiss();
            }
        }
    }

    private void onDismiss() {
        if (DBG) Log.v(TAG, "dismiss.");
        cancel(-1);
        clearState(TICKING);
        clearState(DISMISSING);
        addState(DISMISSED);
        clearLastEntry();
        removeView(mContentView);
        onViewDismiss();
    }

    private void onContentViewVisibilityChanged(boolean shown) {
        if (DBG) Log.v(TAG, "onContentViewVisibilityChanged - " + shown);
        if (shown == hasState(PAUSED)) {
            if (shown) {
                resume();
            } else {
                pause();
                onDismiss();
            }
        }
    }

    private void onMsgStart() {
        if (hasState(PAUSED)) return;
        if (DBG) Log.v(TAG, "start");

        if (getParent() == null) {
            throw new IllegalStateException("NotificationView should have a parent.");
        }

        if (mCallback == null) {
            throw new CallbackNotFoundException("NotificationView.setCallback() not called.");
        }

        addState(STARTING);
        if (hasState(DISMISSING)) {
            if (DBG) Log.v(TAG, "dismissing now. schedule next start.");
            schedule(MSG_START, (int) mHideAnimation.getDuration());
            return;
        }

        NotificationEntry entry = mPendingEntry;
        synchronized (mEntryLock) {
            if (entry == null && !mEntries.isEmpty()) {
                entry = mEntries.get(0);
            }
        }

        if (entry == null) {
            Log.w(TAG, "no notification? quit.");
            schedule(MSG_DISMISS);
            return;
        }

        if (hasState(CALLBACK_CHANGED)) {
            clearState(CALLBACK_CHANGED);
            mCallback.onViewSetup(this);

            final int layoutId = mCallback.getContentViewDefaultLayoutId(this);
            if (mCurrentLayoutId != layoutId) {
                mDefaultContentView = null;
                setContentView(layoutId);
            } else {
                setContentViewInner(mContentView);
            }
        }

        // check whether a differenct contentView is being requested,
        // if so, change the contentView now.
        View newContentView = null;
        if (entry.layoutId > 0) {
            if (entry.layoutId != mCurrentLayoutId) {
                if (entry.layoutId == mDefaultLayoutId) {
                    mCurrentLayoutId = mDefaultLayoutId;
                    newContentView = mDefaultContentView;
                } else {
                    mCurrentLayoutId = entry.layoutId;
                    newContentView = inflate(mContext, entry.layoutId, null);
                }
            }
        } else if (mContentView != mDefaultContentView) {
            mCurrentLayoutId = mDefaultLayoutId;
            newContentView = mDefaultContentView;
        }

        if (!hasState(TICKING)) {
            if (DBG) Log.v(TAG, "not ticking? quit.");
            return;
        }

        if (newContentView != null) {
            setContentViewInner(newContentView);
            schedule(MSG_START);
        } else {
            mChildViewManager.reset();
            if (hasState(DISMISSED)) {
                clearState(DISMISSED);
                onViewTicking();
            }
            show();
        }
    }

    private void onMsgShow() {
        if (hasState(PAUSED) || !hasState(ENABLED)) return;
        if (DBG) Log.v(TAG, "show");

        NotificationEntry entry = mPendingEntry;
        mPendingEntry = null;
        synchronized (mEntryLock) {
            if (entry == null && !mEntries.isEmpty()) {
                entry = mEntries.remove(0);
            }
        }

        if (entry == null) {
            schedule(MSG_DISMISS);
            return;
        }

        if (!hasState(TICKING)) {
            if (DBG) Log.v(TAG, "not ticking? quit.");
            return;
        }

        // temporarily, view cannot be dismissed by user gesture.
        // this will be re-enabled after some delay.
        mDismissOnGestureEnabled = false;

        // view cannot be dismissed by user gesture.
        // it will always be placed on {@link NotificationBoard},
        // unless explicitly call {@link NotificationDelegater#cancel()}.
        mDismissableOnGesture = !entry.ongoing;

        // check whether a different contentView is requested,
        // if so, switch to the new contentView and perform animation.
        if (entry.layoutId > 0) {
            if (entry.layoutId != mCurrentLayoutId) {
                mPendingEntry = entry;
                if (entry.layoutId == mDefaultLayoutId) {
                    mCurrentLayoutId = mDefaultLayoutId;
                    mPendingEntry = entry;
                    setContentView(mDefaultContentView);
                } else {
                    setContentView(entry.layoutId);
                }
                return;
            }
        } else if (mContentView != mDefaultContentView) {
            if (mDefaultContentView == null) {
                final int resId = mCallback.getContentViewDefaultLayoutId(this);
                mDefaultContentView = inflate(mContext, resId, null);
                mDefaultLayoutId = resId;
            }
            mCurrentLayoutId = mDefaultLayoutId;
            mPendingEntry = entry;
            setContentView(mDefaultContentView);
            return;
        }

        if (entry.showWhen && entry.whenFormatted == null) {
            entry.setWhen(null, entry.whenLong > 0L ?
                          entry.whenLong : System.currentTimeMillis());
        }

        showNotification(entry);
        clearState(CONTENT_CHANGED);
        clearState(STARTING);
        clearLastEntry();
        mLastEntry = entry;
        mNotificationHandler.onSendFinished(entry);

        if (mTransitionEnabled) {
            mContentViewSwitcher.start(mNotiDisplayTime);
        } else {
            schedule(MSG_SHOW, mNotiDisplayTime);
        }

        schedule(MSG_ENABLE_DISMISS_ON_GESTURE, DISMISS_FREEZE_TIME);
    }

    private void onMsgUpdateNotification(NotificationEntry entry) {
        if (mLastEntry == entry) {
            mCallback.onUpdateNotification(this, mContentView, entry, mCurrentLayoutId);
            updateContentBackground();
            updateContentBackgroundColor(entry);
            updateContentBackgroundAlpha(entry);
        }
    }

    private void onMsgDismiss() {
        if (hasState(TICKING) && !hasState(DISMISSING)) {
            if (mContentView.isShown()) {
                addState(DISMISSING);
                mHideAnimationListener = mDismissAnimationListener;
                hide();
            } else {
                onDismiss();
            }
        }
    }

    private void onMsgClearAnimation() {
        if (mContentView.getAnimation() != null) {
            if (DBG) Log.v(TAG, "clearAnimation");
            mContentView.clearAnimation();
        }
    }

    private void onMsgSwitchToSelf() {
        if (!mEntries.isEmpty()) {
            if (DBG) Log.v(TAG, "switchToSelf");
            mHideAnimationListener = mSwitchSelfAnimationListener;
            hide();
        } else {
            schedule(MSG_DISMISS);
        }
    }

    private void onMsgSwitchToTarget(View target) {
        if (DBG) Log.v(TAG, "switchToTarget");
        mTargetContentView = target;
        mHideAnimationListener = mSwitchContentAnimationListener;
        hide();
    }

    private void onMsgEnableDismissOnGesture() {
        if (DBG) Log.v(TAG, "enableDismissOnGesture");
        mDismissOnGestureEnabled = true;
    }

    public void onBackKey() {
        if (isTicking()) {
            schedule(MSG_DISMISS);
        }
    }

    public void onHomeKey() {
        if (isTicking() && mDismissOnHomeKey) {
            schedule(MSG_DISMISS);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (DBG) Log.v(TAG, "onWindowVisibilityChanged: " + visibility);
        if (!hasState(STARTING)) {
            onContentViewVisibilityChanged(visibility == VISIBLE);
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (DBG) Log.v(TAG, "onVisibilityChanged: " + visibility + ", " + changedView);
        if (changedView == this) {
            onContentViewVisibilityChanged(visibility == VISIBLE);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (DBG) Log.v(TAG, "onConfigurationChanged: " + newConfig);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!hasState(ENABLED)) {
            return false;
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
        if (DBG) Log.v(TAG, "onDown");
        cancel(MSG_SHOW);
        mContentViewSwitcher.cancelPendings();
        mDirection = -1;
        mGestureConsumer = GESTURE_CONSUMER_DEFAULT;
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
        if (mGestureListener != null) {
            handled = mGestureListener.onSingleTapUp(event);
        }
        return handled;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        if (DBG) Log.v(TAG, "onSingleTapConfirmed");
        boolean handled = false;
        if (mGestureListener != null) {
            handled = mGestureListener.onSingleTapConfirmed(event);
        }

        mLastEntry.executeContentAction(mContext);
        mCallback.onClickContentView(this, mContentView, mLastEntry);
        if (mLastEntry.autoCancel) {
            mLastEntry.cancel();
        } else {
            schedule(MSG_SHOW, mNotiDisplayTime);
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
        schedule(MSG_SHOW, mNotiDisplayTime);

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
        schedule(MSG_SHOW, mNotiDisplayTime);
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

        if (direction == X) {
            if (mDismissOnDragDistanceFarEnough == 0) {
                mDismissOnDragDistanceFarEnough =
                    DISMISS_DRAG_DISTANCE_FACTOR * mContentView.getMeasuredWidth();
            }

            final float x = mContentView.getTranslationX() - distanceX;
            float alpha = Utils.getAlphaForOffset(
                1.0f, 0.0f, 0.0f, mDismissOnDragDistanceFarEnough, Math.abs(x));
            if (alpha < 0.0f) {
                alpha = 0.0f;
            }

            setContentViewTranslationX(x);
            setContentViewAlpha(alpha);
            return true;
        }
        return false;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        if (DBG) Log.v(TAG, "onFling");

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

        if (direction == X) {
            final boolean dismiss =
                mDismissOnGestureEnabled &&
                mDismissableOnGesture &&
                Math.abs(velocityX) > DISMISS_GESTURE_VELOCITY &&
                (mContentView.getTranslationX() == 0 || mContentView.getTranslationX() > 0 == velocityX > 0);

            if (dismiss) {
                onDragOut();
            } else {
                onDragCancel();
            }
            return true;

        } else {
            if (velocityY < 0 && Math.abs(velocityY) > DISMISS_GESTURE_VELOCITY) {
                if (mDismissOnGestureEnabled) {
                    mContentViewSwitcher.start();
                    return true;
                }
            }
        }
        return false;
    }

    public void onUpOrCancel(MotionEvent event, boolean handled) {
        if (DBG) Log.v(TAG, "onUpOrCancel: " + handled);
        if (mGestureListener != null) {
            mGestureListener.onUpOrCancel(event, handled);
        }

        if (handled) {
            return;
        }

        final float x = mContentView.getTranslationX();
        if (x == 0) {
            schedule(MSG_SHOW, mNotiDisplayTime);
            return;
        }

        if (mDismissOnDragDistanceFarEnough == 0) {
            mDismissOnDragDistanceFarEnough =
                DISMISS_DRAG_DISTANCE_FACTOR * mContentView.getMeasuredWidth();
        }

        final boolean dismiss =
            mDismissOnGestureEnabled &&
            mDismissableOnGesture &&
            Math.abs(x) > mDismissOnDragDistanceFarEnough;

        if (dismiss) {
            onDragOut();
        } else {
            onDragCancel();
        }
    }

    private void onDragOut() {
        if (hasState(DISMISSED)) {
            onDragCancel();
            return;
        }

        if (DBG) Log.v(TAG, "onDragOut");
        final int width = mContentView.getMeasuredWidth();
        final int x = mContentView.getTranslationX() >= 0 ? width : -width;
        animateContentViewTranslationX(
            x, 0.0f, mDragOutAnimatorListener, DRAG_OUT_TRANSITION_TIME);
    }

    private void onDragCancel() {
        if (DBG) Log.v(TAG, "onDragCancel");
        animateContentViewTranslationX(
            0.0f, 1.0f, mDragCancelAnimatorListener, DRAG_CANCEL_TRANSITION_TIME);
    }

    public class ContentViewSwitcher {

        /**
         * switch to self
         */
        public void start() {
            start(0);
        }

        public void start(int delay) {
            addState(CONTENT_CHANGED);
            schedule(MSG_SWITCH_TO_SELF, delay);
        }

        /**
         * switch to target
         */
        private void start(View target) {
            addState(CONTENT_CHANGED);
            schedule(MSG_SWITCH_TO_TARGET, 0, 0, target, 0);
        }

        /**
         * cancel
         */
        public void cancelPendings() {
            cancel(MSG_SWITCH_TO_SELF);
        }
    }

    private class ColorProperty extends Property<GradientDrawable, Integer> {
        ColorProperty() { super(Integer.class, "color"); }

        @Override
        public void set(GradientDrawable gd, Integer value) {
            gd.setColor(value.intValue());
        }

        @Override
        public Integer get(GradientDrawable gd) {
            return 0;
        }
    }

    private class AlphaProperty extends Property<GradientDrawable, Integer> {
        AlphaProperty() { super(Integer.class, "alpha"); }

        @Override
        public void set(GradientDrawable gd, Integer value) {
            gd.setAlpha(value.intValue());
        }

        @Override
        public Integer get(GradientDrawable gd) {
            return 0;
        }
    }

    // show
    private final AnimationListener mShowAnimationListener = new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                if (DBG) Log.v(TAG, "show start");
                schedule(MSG_CLEAR_ANIMATION, (int) animation.getDuration());
                schedule(MSG_SHOW);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (DBG) Log.v(TAG, "show end");
                animation.setAnimationListener(null);
            }
        };

    // dismiss
    private final AnimationListener mDismissAnimationListener = new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                if (DBG) Log.v(TAG, "hide start");
                schedule(MSG_CLEAR_ANIMATION, (int) animation.getDuration());
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (DBG) Log.v(TAG, "hide end");
                animation.setAnimationListener(null);
                onDismiss();
            }
        };

    // switch to target
    private final AnimationListener mSwitchContentAnimationListener = new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                if (DBG) Log.v(TAG, "switch content start");
                schedule(MSG_CLEAR_ANIMATION, (int) animation.getDuration());
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (DBG) Log.v(TAG, "switch content end");
                animation.setAnimationListener(null);
                refreshContentView(mTargetContentView);
                mTargetContentView = null;
            }
        };

    // self switch
    private final AnimationListener mSwitchSelfAnimationListener = new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                if (DBG) Log.v(TAG, "switch self start");
                schedule(MSG_CLEAR_ANIMATION, (int) animation.getDuration());
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (DBG) Log.v(TAG, "switch self end");
                animation.setAnimationListener(null);
                refreshContentView();
            }
        };

    // drag left/right
    private final AnimatorListener mDragOutAnimatorListener = new AnimatorListener() {

            private boolean mCanceled;

            @Override
            public void onAnimationStart(Animator animation) {
                if (DBG) Log.v(TAG, "drag out start");
                mCanceled = false;
                addState(DISMISSING);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mCanceled) {
                    if (DBG) Log.v(TAG, "drag out end");
                    mContentView.animate().setListener(null);
                    clearLastEntry();
                    clearState(DISMISSING);
                    cancel(MSG_START);
                    refreshContentView();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (DBG) Log.v(TAG, "drag out cancel");
                mCanceled = true;
                clearState(DISMISSING);
            }
        };

    // drag cancel
    private final AnimatorListener mDragCancelAnimatorListener = new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                if (DBG) Log.v(TAG, "drag cancel start");
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (DBG) Log.v(TAG, "drag cancel end");
                mContentView.animate().setListener(null);
                schedule(MSG_SHOW, mNotiDisplayTime);
            }
        };

    private void onViewTicking() {
        if (mListeners != null) {
            for (StateListener l : mListeners) {
                l.onViewTicking(this);
            }
        }
    }

    private void onViewDismiss() {
        if (mListeners != null) {
            for (StateListener l : mListeners) {
                l.onViewDismiss(this);
            }
        }
    }

    private static final int ENABLED                             = 0x00000001;
    private static final int PAUSED                              = 0x00000002;
    private static final int TICKING                             = 0x00000004;
    private static final int STARTING                            = 0x00000008;
    private static final int DISMISSING                          = 0x00000010;
    private static final int DISMISSED                           = 0x00000020;
    private static final int CONTENT_CHANGED                     = 0x00000100;
    private static final int CONTENT_BACKGROUND_CHANGED          = 0x00000200;
    private static final int CONTENT_BACKGROUND_CHANGED_MINOR    = 0x00000400;
    private static final int CALLBACK_CHANGED                    = 0x00000800;

    private int mState = ENABLED | DISMISSED;

    private void addState(int state) {
        mState |= state;
    }

    private void clearState(int state) {
        mState &= ~state;
    }

    private boolean hasState(int state) {
        return (mState & state) != 0;
    }

    private static final int MSG_START                         = 0;
    private static final int MSG_SHOW                          = 1;
    private static final int MSG_SWITCH_TO_SELF                = 2;
    private static final int MSG_SWITCH_TO_TARGET              = 3;
    private static final int MSG_DISMISS                       = 4;
    private static final int MSG_CLEAR_ANIMATION               = 5;
    private static final int MSG_ENABLE_DISMISS_ON_GESTURE     = 6;
    private static final int MSG_UPDATE_NOTIFICATION           = 7;

    private H mH;
    private H getH() { if (mH == null) mH = new H(this); return mH; }

    private boolean isScheduled(int what) {
        final H h = getH();
        return h.hasMessages(what);
    }

    private void cancel(int what) {
        final H h = getH();
        if (what == -1)
            h.removeCallbacksAndMessages(null);
        else
            h.removeMessages(what);
    }

    private void schedule(int what) {
        final H h = getH();
        h.removeMessages(what);
        h.sendEmptyMessage(what);
    }

    private void schedule(int what, int delay) {
        final H h = getH();
        h.removeMessages(what);
        h.sendEmptyMessageDelayed(what, delay);
    }

    private void schedule(int what, int arg1, int arg2, Object obj, int delay) {
        final H h = getH();
        h.removeMessages(what);
        h.sendMessageDelayed(h.obtainMessage(what, arg1, arg2, obj), delay);
    }

    // main looper
    private static final class H extends Handler {
        private WeakReference<NotificationView> mView;
        H(NotificationView v) {
            super(Looper.getMainLooper());
            mView = new WeakReference<NotificationView>(v);
        }

        @Override
        public void handleMessage(Message msg) {
            NotificationView v = mView.get();
            if (v == null) return;

            switch (msg.what) {
            case MSG_START:
                v.onMsgStart();
                break;

            case MSG_SHOW:
                v.onMsgShow();
                break;

            case MSG_SWITCH_TO_SELF:
                v.onMsgSwitchToSelf();
                break;

            case MSG_SWITCH_TO_TARGET:
                v.onMsgSwitchToTarget((View) msg.obj);
                break;

            case MSG_DISMISS:
                v.onMsgDismiss();
                break;

            case MSG_CLEAR_ANIMATION:
                v.onMsgClearAnimation();
                break;

            case MSG_ENABLE_DISMISS_ON_GESTURE:
                v.onMsgEnableDismissOnGesture();
                break;

            case MSG_UPDATE_NOTIFICATION:
                v.onMsgUpdateNotification((NotificationEntry) msg.obj);
                break;
            }
        }
    }
}
