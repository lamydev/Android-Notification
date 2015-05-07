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
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.GestureDetectorCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Notification view
 *
 * SDK Ver. >= {@link android.os.Build.VERSION_CODES.HONEYCOMB}.
 */
public class NotificationView extends FrameLayout
    implements NotificationListener {

    private static final String TAG = "zemin.NotificationView";
    public static boolean DBG;

    public static final int NOTIFICATION_DISPLAY_TIME = 3000;
    public static final int NOTIFICATION_DISPLAY_TIME_ON_GESTURE = 4000;

    public static final int BACKGROUND_TRANSITION_TIME = 1000;
    public static final int SHOW_TRANSITION_TIME = 500;
    public static final int HIDE_TRANSITION_TIME = 500;
    public static final int RESUME_TIME = 1000;

    public static final float DEFAULT_CORNER_RADIUS = 8.0f;
    public static final int DEFAULT_BACKGROUND_COLOR = 0xfffafafa;

    // child-view switcher
    public static final int SWITCHER_ICON       = 0;
    public static final int SWITCHER_TITLE      = 1;
    public static final int SWITCHER_TEXT       = 2;
    public static final int SWITCHER_WHEN       = 3;
    // add more...

    /**
     * notification entries
     */
    private final ArrayList<NotificationEntry> mEntries =
        new ArrayList<NotificationEntry>();

    /**
     * view switcher for animating child views
     */
    private final ArrayMap<Integer, ChildViewSwitcher> mSwitchers =
        new ArrayMap<Integer, ChildViewSwitcher>();

    private final Object mEntryLock = new Object();

    /**
     * [0] x [1] y [2] w [3] h
     */
    private final int[] mGeometry = new int[4];

    /**
     * [0] l [1] t [2] r [3] b
     */
    private final int[] mContentPadding = new int[4];

    private Context mContext;
    private Callback mCB;
    private NotificationHandler mNotificationHandler;
    private NotificationEntry mLastEntry;
    private NotificationEntry mPendingEntry;
    private GestureDetectorCompat mGestureDetector;
    private GestureListenerInner mGestureListenerInner;
    private GestureListener mGestureListener;
    private LifecycleListener mLifecycleListener;

    /**
     * content view
     */
    private View mContentView;
    private View mDefaultContentView;
    private int mDefaultContentResId;
    private int mCurrentContentResId;

    /**
     * view animation
     */
    private ObjectAnimator mBackgroundColorAnimator;
    private AnimationListener mShowAnimationListener;
    private AnimationListener mHideAnimationListener;
    private ContentViewSwitcher mContentViewSwitcher;
    private boolean mShowHideAnimEnabled = true;
    private Animation mDefaultShowAnimation;
    private Animation mDefaultHideAnimation;
    private Animation mShowAnimation;
    private Animation mHideAnimation;

    /**
     * background drawable
     */
    private Drawable mBackground;
    private GradientDrawable mContentBackground;
    private int mStrokeWidth;
    private int mStrokeColor;
    private float mCornerRadius;
    private int mDefaultBackgroundColor = DEFAULT_BACKGROUND_COLOR;

    /**
     * time
     */
    private int mBackgroundTransitionTime = BACKGROUND_TRANSITION_TIME;
    private int mShowTransitionTime = SHOW_TRANSITION_TIME;
    private int mHideTransitionTime = HIDE_TRANSITION_TIME;
    private int mNotiDisplayTime = NOTIFICATION_DISPLAY_TIME;
    private int mNotiDisplayTimeOnGesture = NOTIFICATION_DISPLAY_TIME_ON_GESTURE;
    private int mResumeTime = RESUME_TIME;

    /**
     * Monitor the view's lifecycle.
     *
     */
    public interface LifecycleListener {

        /**
         * called when shown.
         */
        void onShow();

        /**
         * called when dismissed.
         */
        void onDismiss();
    }

    /**
     * How the notification is to be presented to the user depends on the
     * implementation of {@link Callback}.
     *
     */
    public interface Callback {

        /**
         * default layout resource.
         */
        int getDefaultContentResId();

        /**
         * called only once, after view is created.
         */
        void onSetupView(NotificationView view);

        /**
         * called when contentView is changed.
         */
        void onContentViewChanged(NotificationView view, View contentView, int contentResId);

        /**
         * called to update notification ui.
         */
        void onShowNotification(NotificationView view, NotificationEntry entry, int contentResId);
    }

    private interface OnGestureListenerExt {

        /**
         * called when {@link MotionEvent#ACTION_UP} or {@link MotionEvent#ACTION_CANCEL} event occurs.
         */
        boolean onUpOrCancel(MotionEvent e);

        /**
         * called when the view is dismissed by a drag action.
         */
        void onViewDismissed();

        /**
         * called when the drag action is canceled and
         * the view is translated to its original position.
         */
        void onDragCanceled();
    }

    /**
     * User gesture detection.
     *
     * @see GestureDetector.OnGestureListener
     * @see GestureDetector.OnDoubleTapListener
     * @see OnGestureListenerExt
     */
    public static class GestureListener implements OnGestureListenerExt,
                                                   GestureDetector.OnGestureListener,
                                                   GestureDetector.OnDoubleTapListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public boolean onUpOrCancel(MotionEvent e) {
            return false;
        }

        @Override
        public void onViewDismissed() {
        }

        @Override
        public void onDragCanceled() {
        }
    }

    public NotificationView(Context context) {
        super(context);
        mContext = context;
    }

    public NotificationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public boolean hasCallback() {
        return mCB != null;
    }

    public void setCallback(Callback cb) {
        mCB = cb;
    }

    public void setLifecycleListener(LifecycleListener l) {
        mLifecycleListener = l;
    }

    public void setGestureListener(GestureListener l) {
        mGestureListener = l;
    }

    void setNotificationHandler(NotificationHandler handler) {
        mNotificationHandler = handler;
    }

    /**
     * @return boolean
     */
    public boolean isTicking() {
        return hasState(TICKING);
    }

    /**
     * notification display time
     *
     * @param ms
     */
    public void setDisplayTime(int ms) {
        mNotiDisplayTime = ms;
    }

    /**
     * notification display time when gesture detected
     *
     * @param ms
     */
    public void setDisplayTimeOnGesure(int ms) {
        mNotiDisplayTimeOnGesture = ms;
    }

    /**
     * time to wait when resumed
     *
     * @param ms
     */
    public void setResumeTime(int ms) {
        mResumeTime = ms;
    }

    /**
     * background transition time
     *
     * @param ms
     */
    public void setBackgroundTransitionTime(int ms) {
        mBackgroundTransitionTime = ms;
    }

    /**
     * default background color
     *
     * @param color
     */
    public void setDefaultBackgroundColor(int color) {
        mDefaultBackgroundColor = color;
    }

    /**
     * background
     *
     * @return Drawable
     */
    public Drawable getContentBackground() {
        return mBackground;
    }

    /**
     * background
     *
     * @param b
     */
    public void setContentBackground(Drawable b) {
        mBackground = b;
    }

    /**
     * background corner radius
     *
     * @param r
     */
    public void setCornerRadius(float r) {
        mCornerRadius = r;
    }

    /**
     * background padding
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    public void setContentPadding(int left, int top, int right, int bottom) {
        mContentPadding[0] = left;
        mContentPadding[1] = top;
        mContentPadding[2] = right;
        mContentPadding[3] = bottom;
    }

    /**
     * background stroke
     */
    public void setStroke(int width, int color) {
        mStrokeWidth = width;
        mStrokeColor = color;
    }

    /**
     * view geometry
     *
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public void setGeometry(int x, int y, int width, int height) {
        mGeometry[0] = x;
        mGeometry[1] = y;
        mGeometry[2] = width;
        mGeometry[3] = height;
        updateGeometry();
    }

    /**
     * view position
     *
     * @param x
     * @param y
     */
    public void setPosition(int x, int y) {
        mGeometry[0] = x;
        mGeometry[1] = y;
        updateGeometry();
    }

    /**
     * view dimension
     *
     * @param width
     * @param height
     */
    public void setDimension(int width, int height) {
        mGeometry[2] = width;
        mGeometry[3] = height;
        updateGeometry();
    }

    /**
     * enable show/hide animation
     *
     * @param enable
     */
    public void enableShowHideAnimation(boolean enable) {
        mShowHideAnimEnabled = enable;
    }

    /**
     * notification show transition time
     *
     * @param ms
     */
    public void setShowTransitionTime(int ms) {
        mShowTransitionTime = ms;
    }

    /**
     * notification hide transition time
     *
     * @param ms
     */
    public void setHideTransitionTime(int ms) {
        mHideTransitionTime = ms;
    }

    /**
     * show animation (AnimationListener will be overrided)
     *
     * @param anim
     */
    public void setShowAnimation(Animation anim) {
        mShowAnimation = anim;
    }

    /**
     * hide animation (AnimationListener will be overrided)
     *
     * @param anim
     */
    public void setHideAnimation(Animation anim) {
        mHideAnimation = anim;
    }

    /**
     * contentView switcher
     *
     * @return ContentViewSwitcher
     */
    public ContentViewSwitcher getContentViewSwitcher() {
        return mContentViewSwitcher;
    }

    /**
     * child-view switcher
     *
     * @see NotificationView#SWITCHER_ICON
     * @see NotificationView#SWITCHER_TITLE
     * @see NotificationView#SWITCHER_TEXT
     * @see NotificationView#SWITCHER_WHEN
     *
     * @param contentType
     * @param resId
     */
    public void setChildViewSwitcher(Integer contentType, int resId) {
        ChildViewSwitcher switcher = mSwitchers.get(contentType);
        if (switcher == null) {
            switcher = new ChildViewSwitcher(contentType);
            mSwitchers.put(contentType, switcher);
        }
        switcher.updateSwitcher(resId);
    }

    /**
     * remove child-view switcher
     *
     * @see NotificationView#SWITCHER_ICON
     * @see NotificationView#SWITCHER_TITLE
     * @see NotificationView#SWITCHER_TEXT
     * @see NotificationView#SWITCHER_WHEN
     *
     * @param contentType
     */
    public void removeChildViewSwitcher(Integer contentType) {
        if (mSwitchers.containsKey(contentType))
            mSwitchers.remove(contentType);
    }

    /**
     * @param contentType
     * @return ChildViewSwitcher
     */
    public ChildViewSwitcher getChildViewSwitcher(int contentType) {
        return mSwitchers.get(contentType);
    }

    /**
     * @param contentType
     * @return ViewSwitcher
     */
    public ViewSwitcher getViewSwitcher(int contentType) {
        ChildViewSwitcher s = mSwitchers.get(SWITCHER_ICON);
        return s != null ? s.mSwitcher : null;
    }

    /**
     * a animator for switching icons (ImageView)
     *
     * @return ImageSwitcher
     */
    public ImageSwitcher getIconSwitcher() {
        ChildViewSwitcher s = mSwitchers.get(SWITCHER_ICON);
        return s != null ? (ImageSwitcher) s.mSwitcher : null;
    }

    /**
     * a animator for switching titles (TextView)
     *
     * @return TextSwitcher
     */
    public TextSwitcher getTitleSwitcher() {
        ChildViewSwitcher s = mSwitchers.get(SWITCHER_TITLE);
        return s != null ? (TextSwitcher) s.mSwitcher : null;
    }

    /**
     * a animator for switching texts (TextView)
     *
     * @return TextSwitcher
     */
    public TextSwitcher getTextSwitcher() {
        ChildViewSwitcher s = mSwitchers.get(SWITCHER_TEXT);
        return s != null ? (TextSwitcher) s.mSwitcher : null;
    }

    /**
     * a animator for switching when (TextView)
     *
     * @return TextSwitcher
     */
    public TextSwitcher getWhenSwitcher() {
        ChildViewSwitcher s = mSwitchers.get(SWITCHER_WHEN);
        return s != null ? (TextSwitcher) s.mSwitcher : null;
    }

    /**
     * @return boolean
     */
    public boolean isContentLayoutChanged() {
        return hasState(CONTENT_CHANGED);
    }

    /**
     * @return int
     */
    public int getCurrentContentResId() {
        return mCurrentContentResId;
    }

    /**
     * @return NotificationEntry
     */
    public NotificationEntry getLastEntry() {
        return mLastEntry;
    }

    public boolean hasContentView() {
        return mContentView != null;
    }

    /**
     * set contentView
     *
     * @param resId
     */
    public void setContentView(int resId) {
        if (mCurrentContentResId != resId) {
            View view = inflate(mContext, resId, null);
            if (mDefaultContentView == null &&
                resId == mCB.getDefaultContentResId()) {
                mDefaultContentView = view;
                mDefaultContentResId = resId;
            }
            mCurrentContentResId = resId;
            setContentView(view);
        }
    }

    /**
     * set contentView
     *
     * @param view
     */
    void setContentView(View view) {
        if (mContentView == view || view == null) return;
        if (mContentView != null) {
            mContentViewSwitcher.start(view);
        } else {
            mContentBackground = new GradientDrawable();
            mBackground = mContentBackground;

            ColorProperty colorProperty = new ColorProperty();
            mBackgroundColorAnimator = ObjectAnimator.ofObject(
                mContentBackground, colorProperty, new ArgbEvaluator(), 0, 0);

            mShowAnimationListener = DEFAULT_SHOW_ANIMATION_LISTENER;
            mHideAnimationListener = DEFAULT_HIDE_ANIMATION_LISTENER;
            mDefaultShowAnimation = AnimationFactory.pushDownIn();
            mDefaultHideAnimation = AnimationFactory.pushUpOut();

            mContentViewSwitcher = new ContentViewSwitcher();
            mGestureListenerInner = new GestureListenerInner();
            mGestureDetector = new GestureDetectorCompat(mContext, mGestureListenerInner);

            mCB.onSetupView(this);
            setContentViewInner(view);
        }
    }

    private void setContentViewInner(View view) {
        removeAllViews();
        mLastEntry = null;
        mContentView = view;
        addState(CONTENT_CHANGED);
        mGestureListenerInner.setView(view);
        clearChildViewSwitchers();
        mCB.onContentViewChanged(
            this, view, mCurrentContentResId);
        view.setBackground(mBackground);
        mContentBackground.setCornerRadius(mCornerRadius);
        mContentBackground.setStroke(mStrokeWidth, mStrokeColor);
        mBackgroundColorAnimator.setDuration(mBackgroundTransitionTime);
    }

    /**
     *
     */
    public void animateContentView() {
        mContentViewSwitcher.start();
    }

    /**
     * resume
     */
    public void resume() {
        synchronized (mEntryLock) {
            if (hasState(PAUSED)) {
                if (DBG) Log.v(TAG, "resume.");
                clearState(PAUSED);
                schedule(MSG_START, mResumeTime);
            }
        }
    }

    /**
     * pause
     */
    public void pause() {
        synchronized (mEntryLock) {
            if (hasState(TICKING) && !hasState(PAUSED) && !mEntries.isEmpty()) {
                if (DBG) Log.v(TAG, "pause. " + mEntries.size());
                mHideAnimationListener = DEFAULT_HIDE_ANIMATION_LISTENER;
                addState(PAUSED);
                cancel(-1);
                mLastEntry = null;
                hide();
            }
        }
    }

    /**
     * dismiss, but pending notifications will still be sent to {@link NotificationListener},
     * if {@link NotificationEntry#isSentToListener()} returns true.
     */
    public void dismiss() {
        schedule(MSG_DISMISS, 0 /* cancelAll */, 0, null, 0);
    }

    /**
     * dismiss current notification and show next notification immediately.
     */
    public void next() {
        schedule(MSG_SHOW);
    }

    @Override
    public void onArrival(NotificationEntry entry) {
        synchronized (mEntryLock) {
            Iterator<NotificationEntry> iter = mEntries.iterator();
            int index = mEntries.size();
            while (iter.hasNext()) {
                NotificationEntry ne = iter.next();
                if (entry.priority.higher(ne.priority)) {
                    index = mEntries.indexOf(ne); break;
                }
            }
            mEntries.add(index, entry);
            if (!hasState(TICKING)) {
                addState(TICKING);
                schedule(MSG_START);
            }
        }
    }

    @Override
    public void onCancel(NotificationEntry entry) {
        synchronized (mEntryLock) {
            if (mEntries.contains(entry)) {
                mEntries.remove(entry);
            }
        }
        mNotificationHandler.onCancelFinished(entry);
    }

    /**
     * cancel all, including the pendings.
     */
    public void onCancelAll() {
        schedule(MSG_DISMISS, 1 /* cancelAll */, 0, null, 0);
    }

    /**
     * show notification view
     *
     * @param entry
     */
    protected void onShowNotification(NotificationEntry entry) {
        if (DBG) Log.v(TAG, "onShowNotification: " + entry.ID);

        mCB.onShowNotification(this, entry, mCurrentContentResId);

        if (entry.backgroundColor == 0) {
            entry.backgroundColor = mDefaultBackgroundColor;
        }
        final int lastColor = mLastEntry != null ?
            mLastEntry.backgroundColor : Color.WHITE;
        final int currColor = entry.backgroundColor;
        if (lastColor != currColor) {
            mBackgroundColorAnimator.setIntValues(lastColor, currColor);
            mBackgroundColorAnimator.start();
        }

        // TODO !!!
        // ensure that, the "nextView" of ViewSwitcher is visible.
        Collection<ChildViewSwitcher> switchers = mSwitchers.values();
        for (ChildViewSwitcher switcher : switchers) {
            ViewSwitcher s = switcher.mSwitcher;
            if (s != null) s.getNextView().setVisibility(VISIBLE);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        } else {
            switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mGestureListenerInner.onUpOrCancel(event);
                break;
            }
        }
        return super.onTouchEvent(event);
    }

    private void show() {
        if (mContentView.getParent() == null) {
            addView(mContentView);
        }

        // reset
        mContentView.setTranslationX(0.0f);
        mContentView.setAlpha(1.0f);

        mContentView.setPadding(
            mContentPadding[0], mContentPadding[1],
            mContentPadding[2], mContentPadding[3]);

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

        mContentView.setVisibility(VISIBLE);
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
        }
    }

    private void resetChildViewSwitchers() {
        Collection<ChildViewSwitcher> switchers = mSwitchers.values();
        for (ChildViewSwitcher switcher : switchers) {
            switcher.reset();
        }
    }

    private void clearChildViewSwitchers() {
        Collection<ChildViewSwitcher> switchers = mSwitchers.values();
        for (ChildViewSwitcher switcher : switchers) {
            switcher.clear();
        }
    }

    private void onContentViewVisibilityChanged(boolean shown) {
        if (hasState(TICKING) && shown == hasState(PAUSED)) {
            if (shown)
                resume();
            else
                pause();
        }
    }

    private void onMsgStart() {
        if (hasState(PAUSED)) return;
        if (DBG) Log.v(TAG, "msg_start");

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
            dismiss();
            return;
        }

        if (mContentView == null) {
            setContentView(mCB.getDefaultContentResId());
        }

        // check whether a differenct contentView is being requested,
        // if so, change the contentView now.
        View newContentView = null;
        if (entry.contentResId > 0) {
            if (entry.contentResId != mCurrentContentResId) {
                if (entry.contentResId == mDefaultContentResId) {
                    mCurrentContentResId = mDefaultContentResId;
                    newContentView = mDefaultContentView;
                } else {
                    mCurrentContentResId = entry.contentResId;
                    newContentView = inflate(mContext, entry.contentResId, null);
                }
            }
        } else if (mContentView != mDefaultContentView) {
            mCurrentContentResId = mDefaultContentResId;
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
            resetChildViewSwitchers();
            if (hasState(DISMISSED)) {
                clearState(DISMISSED);
                if (mLifecycleListener != null) {
                    mLifecycleListener.onShow();
                }
            }
            show();
        }
    }

    private void onMsgShow() {
        if (hasState(PAUSED)) return;
        if (DBG) Log.v(TAG, "msg_show");

        NotificationEntry entry = mPendingEntry;
        mPendingEntry = null;
        synchronized (mEntryLock) {
            if (entry == null && !mEntries.isEmpty()) {
                entry = mEntries.remove(0);
            }
        }

        if (entry == null) {
            dismiss();
            return;
        }

        if (!hasState(TICKING)) {
            if (DBG) Log.v(TAG, "not ticking? quit.");
            return;
        }

        // check whether a different contentView is requested,
        // if so, switch to the new contentView and perform animation.
        if (entry.contentResId > 0) {
            if (entry.contentResId != mCurrentContentResId) {
                mPendingEntry = entry;
                if (entry.contentResId == mDefaultContentResId) {
                    mCurrentContentResId = mDefaultContentResId;
                    mPendingEntry = entry;
                    setContentView(mDefaultContentView);
                } else {
                    setContentView(entry.contentResId);
                }
                return;
            }
        } else if (mContentView != mDefaultContentView) {
            if (mDefaultContentView == null) {
                final int resId = mCB.getDefaultContentResId();
                mDefaultContentView = inflate(mContext, resId, null);
                mDefaultContentResId = resId;
            }
            mCurrentContentResId = mDefaultContentResId;
            mPendingEntry = entry;
            setContentView(mDefaultContentView);
            return;
        }

        mGestureListenerInner.setDismissable(!entry.ongoing);
        onShowNotification(entry);
        mLastEntry = entry;
        clearState(CONTENT_CHANGED);

        if (hasState(STARTING)) {
            //
            // waiting for the first layout of SWITCHER to be finished,
            // so that we can adjust its size according to its content.
            //
            // 100 ms
            //
            schedule(MSG_SWITCHER_ADJUST_HEIGHT,  100);
            clearState(STARTING);
        }

        mNotificationHandler.onSendFinished(entry);
        if (!isScheduled(MSG_SWITCH_TO_SELF)) {
            schedule(MSG_SHOW, mNotiDisplayTime);
        }
    }

    private void onMsgDismiss(boolean cancelAll) {
        synchronized (mEntryLock) {
            if (DBG) Log.v(TAG, "msg_dismiss");
            if (hasState(TICKING)) {
                if (DBG) Log.v(TAG, "dismiss. " + mEntries.size());
                mHideAnimationListener = DEFAULT_HIDE_ANIMATION_LISTENER;
                clearState(TICKING);
                addState(DISMISSING);
                mLastEntry = null;
                cancel(-1);
                hide();
            }

            if (cancelAll) {
                mEntries.clear();
                mNotificationHandler.onCancelAllFinished();
            } else {
                for (NotificationEntry entry : mEntries) {
                    mNotificationHandler.onSendIgnored(entry);
                }
                mEntries.clear();
            }
        }
    }

    private void onMsgClearAnimation() {
        if (mContentView.getAnimation() != null) {
            if (DBG) Log.v(TAG, "msg_clearAnimation");
            mContentView.clearAnimation();
        }
    }

    private void onMsgSwitchToSelf() {
        if (hasState(PAUSED)) return;
        synchronized (mEntries) {
            if (!mEntries.isEmpty()) {
                if (DBG) Log.v(TAG, "msg_switchToSelf");
                mHideAnimationListener = DEFAULT_SELF_SWITCH_ANIMATION_LISTENER;
                hide();
            } else {
                dismiss();
            }
        }
    }

    private void onMsgSwitchToTarget(View target) {
        if (hasState(PAUSED)) return;
        if (DBG) Log.v(TAG, "msg_switchToTarget");
        mHideAnimationListener = DEFAULT_SWITCH_ANIMATION_LISTENER;
        mHideAnimationListener.setTargetView(target);
        hide();
    }

    private void onMsgSwitcherAdjustHeight() {
        if (DBG) Log.v(TAG, "msg_switcherAdjustHeight");
        Collection<ChildViewSwitcher> switchers = mSwitchers.values();
        for (ChildViewSwitcher switcher : switchers) {
            switcher.adjustHeight();
        }
    }

    private void updateGeometry() {
        final ViewGroup.LayoutParams lp = getLayoutParams();
        if (mGeometry[0] != getX()) {
            setX(mGeometry[0]);
        }
        if (mGeometry[1] != getY()) {
            setY(mGeometry[1]);
        }
        if (lp.width != mGeometry[3] || lp.height != mGeometry[4]) {
            lp.width = mGeometry[3];
            lp.height = mGeometry[4];
            setLayoutParams(lp);
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
        // TODO
    }

    public class ContentViewSwitcher {

        /**
         * switch to self
         */
        public void start() {
            start(mNotiDisplayTime);
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

    public class ChildViewSwitcher {

        public static final int TRANSITION_TIME = 700;

        final int mContentType;
        int mAttrsResId;
        ViewSwitcher mSwitcher;
        Animation mInAnimation;
        Animation mOutAnimation;
        int mInDuration = TRANSITION_TIME;
        int mOutDuration = TRANSITION_TIME;

        private ChildViewSwitcher(int contentType) {
            mContentType = contentType;
        }

        public ViewSwitcher switcher() {
            return mSwitcher;
        }

        public Animation getInAnimation() {
            return mInAnimation;
        }

        public Animation getOutAnimation() {
            return mOutAnimation;
        }

        public int getInDuration() {
            return mInDuration;
        }

        public int getOutDuration() {
            return mOutDuration;
        }

        public void setInAnimation(Animation animation, int duration) {
            mInAnimation = animation;
            mInDuration = duration;
            updateAnimation();
        }

        public void setOutAnimation(Animation animation, int duration) {
            mOutAnimation = animation;
            mOutDuration = duration;
            updateAnimation();
        }

        private void reset() {
            if (mSwitcher != null) {
                mSwitcher.setAnimateFirstView(false);
                mSwitcher.reset();
            }
        }

        private void clear() {
            mSwitcher = null;
        }

        private void updateSwitcher(int resId) {
            if (DBG) Log.v(TAG, "switcher[" + mContentType + "] update.");
            mSwitcher = (ViewSwitcher) mContentView.findViewById(resId);
            if (mSwitcher == null) {
                throw new IllegalStateException("child-view switcher not found.");
            }
            updateAnimation();
        }

        private void updateAnimation() {
            if (mSwitcher == null) return;
            if (mSwitcher.getInAnimation() != mInAnimation || mInAnimation == null) {
                if (mInAnimation == null) {
                    mInAnimation = AnimationFactory.pushDownIn();
                }
                mInAnimation.setAnimationListener(mInAnimationListener);
                mInAnimation.setDuration(mInDuration);
                mSwitcher.setInAnimation(mInAnimation);
            }
            if (mSwitcher.getOutAnimation() != mOutAnimation || mOutAnimation == null) {
                if (mOutAnimation == null) {
                    mOutAnimation = AnimationFactory.pushDownOut();
                }
                mOutAnimation.setAnimationListener(mOutAnimationListener);
                mOutAnimation.setDuration(mOutDuration);
                mSwitcher.setOutAnimation(mOutAnimation);
            }
        }

        // TODO: a better way to wrap content of mContentView, especially TextView.
        private void adjustHeight() {
            if (mSwitcher == null) return;
            if (mContentType == SWITCHER_TEXT) {
                TextView curr = (TextView) mSwitcher.getCurrentView();
                TextView next = (TextView) mSwitcher.getNextView();
                int currH = curr.getLineCount() * curr.getLineHeight();
                int nextH = next.getLineCount() * next.getLineHeight();
                if (currH != nextH) {
                    curr.setHeight(currH);
                    next.setHeight(currH);
                }
            }
        }

        private final AnimationListener mInAnimationListener = new AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                    adjustHeight();
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                }
            };

        private final AnimationListener mOutAnimationListener = new AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                }
            };
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

    // show
    private final AnimationListener DEFAULT_SHOW_ANIMATION_LISTENER = new AnimationListener() {

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

    // hide
    private final AnimationListener DEFAULT_HIDE_ANIMATION_LISTENER = new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                if (DBG) Log.v(TAG, "hide start");
                schedule(MSG_CLEAR_ANIMATION, (int) animation.getDuration());
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (DBG) Log.v(TAG, "hide end");
                animation.setAnimationListener(null);
                removeView(mContentView);
                clearState(DISMISSING);
                addState(DISMISSED);
                if (mLifecycleListener != null) {
                    mLifecycleListener.onDismiss();
                }
            }
        };

    // switch to target
    private final AnimationListener DEFAULT_SWITCH_ANIMATION_LISTENER = new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                if (DBG) Log.v(TAG, "switch start");
                schedule(MSG_CLEAR_ANIMATION, (int) animation.getDuration());
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (DBG) Log.v(TAG, "switch end");
                animation.setAnimationListener(null);
                mContentView.setBackground(null);
                setContentViewInner(mTargetView);
                schedule(MSG_START);
            }
        };

    // self switch
    private final AnimationListener DEFAULT_SELF_SWITCH_ANIMATION_LISTENER = new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                if (DBG) Log.v(TAG, "self switch start");
                schedule(MSG_CLEAR_ANIMATION, (int) animation.getDuration());
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (DBG) Log.v(TAG, "self switch end");
                animation.setAnimationListener(null);
                schedule(MSG_START);
            }
        };

    public static class AnimationListener implements Animation.AnimationListener {
        protected View mTargetView;
        public void setTargetView(View view) { mTargetView = view; }

        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }

    private final AnimatorListener FLING_ANIMATOR_LISTENER_DISMISS = new AnimatorListener() {

            private boolean mCanceled;

            @Override
            public void onAnimationStart(Animator animation) {
                if (DBG) Log.v(TAG, "fling dismiss start");
                mCanceled = false;
                addState(DISMISSING);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mCanceled) {
                    return;
                }

                if (DBG) Log.v(TAG, "fling dismiss end");
                mContentView.animate().setListener(null);
                mNotificationHandler.reportCanceled(mLastEntry);
                clearState(DISMISSING);
                cancel(MSG_START);

                synchronized (mEntries) {
                    if (!mEntries.isEmpty()) {
                        mContentView.setVisibility(INVISIBLE);
                        addState(CONTENT_CHANGED);
                        schedule(MSG_START, 200);
                    } else {
                        clearState(TICKING);
                        addState(DISMISSED);
                        mLastEntry = null;
                        cancel(-1);
                        removeView(mContentView);
                        if (mLifecycleListener != null) {
                            mLifecycleListener.onDismiss();
                        }
                    }
                }

                if (mGestureListener != null) {
                    mGestureListener.onViewDismissed();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (DBG) Log.v(TAG, "fling dismiss cancel");
                mCanceled = true;
                clearState(DISMISSING);
            }
        };

    private final AnimatorListener FLING_ANIMATOR_LISTENER_DRAG_CANCEL = new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                if (DBG) Log.v(TAG, "fling drag cancel start");
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (DBG) Log.v(TAG, "fling drag cancel end");
                mContentView.animate().setListener(null);
                schedule(MSG_SHOW, mNotiDisplayTimeOnGesture);
                if (mGestureListener != null) {
                    mGestureListener.onDragCanceled();
                }
            }
        };

    public static class AnimatorListener implements Animator.AnimatorListener {
        protected View mTargetView;
        public void setTargetView(View view) { mTargetView = view; }

        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationCancel(Animator animator) {
        }
    }

    // factory
    public static class AnimationFactory {

        public static Animation pushDownIn() {
            AnimationSet animationSet = new AnimationSet(true);
            animationSet.setFillAfter(true);
            animationSet.addAnimation(new TranslateAnimation(0, 0, -100, 0));
            animationSet.addAnimation(new AlphaAnimation(0.0f, 1.0f));
            return animationSet;
        }

        public static Animation pushDownOut() {
            AnimationSet animationSet = new AnimationSet(true);
            animationSet.setFillAfter(true);
            animationSet.addAnimation(new TranslateAnimation(0, 0, 0, 100));
            animationSet.addAnimation(new AlphaAnimation(1.0f, 0.0f));
            return animationSet;
        }

        public static Animation pushUpOut() {
            AnimationSet animationSet = new AnimationSet(true);
            animationSet.setFillAfter(true);
            animationSet.addAnimation(new TranslateAnimation(0, 0, 0, -100));
            animationSet.addAnimation(new AlphaAnimation(1.0f, 0.0f));
            return animationSet;
        }
    }

    private final class GestureListenerInner
        implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

        private static final int FLING_TRANSITION_TIME_DISMISS = 500;
        private static final int FLING_TRANSITION_TIME_DRAG_CANCEL = 500;
        private static final float FLING_VELOCITY_DISMISS = 120.0f;
        private static final float FLING_DISTANCE_FACTOR_DISMISS = 0.6f;

        private View mView;
        private float mDismissOnSwipeDistanceFarEnough;
        private boolean mDismissable = true;

        @Override
        public boolean onDown(MotionEvent e) {
            if (DBG) Log.v(TAG, "onDown");
            cancel(MSG_SHOW);
            mContentViewSwitcher.cancelPendings();
            if (mGestureListener != null) {
                mGestureListener.onDown(e);
            }
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            if (DBG) Log.v(TAG, "onShowPress");
            if (mGestureListener != null) {
                mGestureListener.onShowPress(e);
            }
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (DBG) Log.v(TAG, "onSingleTapUp");
            boolean handled = false;
            if (mGestureListener != null && mGestureListener.onSingleTapUp(e)) {
                handled = true;
            }
            return handled;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (DBG) Log.v(TAG, "onSingleTapConfirmed");
            boolean handled = false;
            if (mGestureListener != null && mGestureListener.onSingleTapConfirmed(e)) {
                handled = true;
            }
            schedule(MSG_SHOW, mNotiDisplayTimeOnGesture);
            return handled;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (DBG) Log.v(TAG, "onDoubleTap");
            boolean handled = false;
            if (mGestureListener != null && mGestureListener.onDoubleTap(e)) {
                handled = true;
            }
            schedule(MSG_SHOW, mNotiDisplayTimeOnGesture);
            return handled;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            if (DBG) Log.v(TAG, "onDoubleTapEvent");
            boolean handled = false;
            if (mGestureListener != null && mGestureListener.onDoubleTapEvent(e)) {
                handled = true;
            }
            return handled;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (DBG) Log.v(TAG, "onLongPress");
            if (mGestureListener != null) {
                mGestureListener.onLongPress(e);
            }
            schedule(MSG_SHOW, mNotiDisplayTimeOnGesture);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (mGestureListener != null && mGestureListener.onScroll(e1, e2, distanceX, distanceY)) {
                return true;
            }

            if (Math.abs(distanceX) > Math.abs(distanceY)) {
                mView.setTranslationX(mView.getTranslationX() - distanceX);
                return true;
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (DBG) Log.v(TAG, "onFling");
            if (mGestureListener != null && mGestureListener.onFling(e1, e2, velocityX, velocityY)) {
                return true;
            }

            final float absVelocityX = Math.abs(velocityX);
            final float absVelocityY = Math.abs(velocityY);
            if (absVelocityX > absVelocityY) {
                final boolean dismiss =
                    mDismissable && absVelocityX > FLING_VELOCITY_DISMISS;
                if (dismiss) {
                    onDismiss();
                } else {
                    onDragCancel();
                }
                return true;
            } else {
                if (velocityY < 0 && absVelocityY > FLING_VELOCITY_DISMISS) {
                    mContentViewSwitcher.start(0);
                    return true;
                }
            }
            return false;
        }

        public void onUpOrCancel(MotionEvent e) {
            if (DBG) Log.v(TAG, "onUpOrCancel");
            if (mGestureListener != null && mGestureListener.onUpOrCancel(e)) {
                return;
            }

            final float x = mView.getTranslationX();
            if (x == 0) {
                schedule(MSG_SHOW, mNotiDisplayTimeOnGesture);
                return;
            }

            if (mDismissOnSwipeDistanceFarEnough == 0) {
                mDismissOnSwipeDistanceFarEnough =
                    FLING_DISTANCE_FACTOR_DISMISS * mView.getMeasuredWidth();
            }

            final boolean dismiss =
                mDismissable && Math.abs(x) > mDismissOnSwipeDistanceFarEnough;
            if (dismiss) {
                onDismiss();
            } else {
                onDragCancel();
            }
        }

        private void onDismiss() {
            if (hasState(DISMISSED)) {
                return;
            }

            final int width = mView.getMeasuredWidth();
            final int x = mView.getTranslationX() > 0 ? width : -width;
            mView.animate().cancel();
            mView.animate().alpha(0.0f).translationX(x)
                .setListener(FLING_ANIMATOR_LISTENER_DISMISS)
                .setDuration(FLING_TRANSITION_TIME_DISMISS)
                .start();
        }

        private void onDragCancel() {
            mView.animate().cancel();
            mView.animate().alpha(1.0f).translationX(0.0f)
                .setListener(FLING_ANIMATOR_LISTENER_DRAG_CANCEL)
                .setDuration(FLING_TRANSITION_TIME_DRAG_CANCEL)
                .start();
        }

        public void setDismissable(boolean dismiss) {
            mDismissable = dismiss;
        }

        public void setView(View view) { mView = view; reset(); }
        public void reset() { mDismissOnSwipeDistanceFarEnough = 0; }
    }

    private static final int STARTING               = 0x00000001;
    private static final int TICKING                = 0x00000002;
    private static final int DISMISSING             = 0x00000004;
    private static final int DISMISSED              = 0x00000008;
    private static final int PAUSED                 = 0x00000010;
    private static final int CONTENT_CHANGED        = 0x00000020;

    private int mState = DISMISSED;

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
    private static final int MSG_SWITCHER_ADJUST_HEIGHT        = 6;

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
                v.onMsgDismiss(msg.arg1 == 1);
                break;

            case MSG_CLEAR_ANIMATION:
                v.onMsgClearAnimation();
                break;

            case MSG_SWITCHER_ADJUST_HEIGHT:
                v.onMsgSwitcherAdjustHeight();
                break;
            }
        }
    }
}
