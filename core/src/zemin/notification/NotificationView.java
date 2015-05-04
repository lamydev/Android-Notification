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

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Property;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import android.support.v4.util.ArrayMap;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

// a view for displaying notifications
//
// sdk >= 11
//
// @author Zemin Liu
//
public class NotificationView extends FrameLayout
    implements NotificationListener {

    private static final String TAG = "zemin.NotificationView";
    public static boolean DBG;

    // child-view switcher
    public static final int SWITCHER_ICON       = 0;
    public static final int SWITCHER_TITLE      = 1;
    public static final int SWITCHER_TEXT       = 2;
    public static final int SWITCHER_WHEN       = 3;
    // add more...

    /**
     * notification display time
     */
    public static final int NOTIFICATION_DISPLAY_TIME = 3000;

    /**
     * notification display time when active
     */
    public static final int NOTIFICATION_DISPLAY_TIME_ON_ACTIVE = 5000;

    /**
     * transition time from one notification to another
     */
    public static final int NOTIFICATION_TRANSITION_TIME = 700;

    /**
     * transition time from one background to another
     */
    public static final int BACKGROUND_TRANSITION_TIME = 1000;

    /**
     * transition time for show-animation of first notification
     */
    public static final int SHOW_TRANSITION_TIME = 500;

    /**
     * transition time for hide-animation of last notification
     */
    public static final int HIDE_TRANSITION_TIME = 500;

    /**
     * resume time
     */
    public static final int RESUME_TIME = 1000;

    /**
     * default width
     */
    public static final int DEFAULT_WIDTH = 1000;

    /**
     * default height
     */
    public static final int DEFAULT_HEIGHT = 65;

    /**
     * default margin top
     */
    public static final int DEFAULT_MARGIN_TOP = 50;

    /**
     * default margin left
     */
    public static final int DEFAULT_MARGIN_LEFT = 50;

    /**
     * default margin right
     */
    public static final int DEFAULT_MARGIN_RIGHT = 50;

    /**
     * default margin bottom
     */
    public static final int DEFAULT_MARGIN_BOTTOM = 50;

    /**
     * background corner radius
     */
    public static final float DEFAULT_CORNER_RADIUS = 8.0f;

    /**
     * background color
     */
    public static final int DEFAULT_BACKGROUND_COLOR = 0xfffafafa;

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
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private DisplayMetrics mDisplayMetrics;
    private NotificationHandler mNotificationHandler;
    private NotificationEntry mLastEntry;
    private NotificationEntry mPendingEntry;
    private Callback mCB;

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
    private int mNotiDisplayTimeOnActive = NOTIFICATION_DISPLAY_TIME_ON_ACTIVE;
    private int mResumeTime = RESUME_TIME;

    public NotificationView(Context context) {
        super(context);
        mContext = context;
    }

    public NotificationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
    }

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

    public boolean hasCallback() {
        return mCB != null;
    }

    public void setCallback(Callback cb) {
        mCB = cb;
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
     * notification display time
     *
     * @param ms
     */
    public void setDisplayTimeOnActive(int ms) {
        mNotiDisplayTimeOnActive = ms;
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
     * window/view geometry
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
     * window position
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
     * window/view dimension
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
     * child-view container
     *
     * @see NotificationView#SWITCHER_ICON
     * @see NotificationView#SWITCHER_TITLE
     * @see NotificationView#SWITCHER_TEXT
     * @see NotificationView#SWITCHER_WHEN
     *
     * @param contentType
     * @param resId
     * @param attrs
     */
    // public void setChildViewContainer(Integer contentType, int resId, int attrs) {
    //     ChildViewSwitcher switcher = mSwitchers.get(contentType);
    //     if (switcher == null) {
    //         switcher = new ChildViewSwitcher(contentType);
    //         mSwitchers.put(contentType, switcher);
    //     }
    //     switcher.updateContainer(resId, attrs);
    // }

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

            mContentViewSwitcher = new ContentViewSwitcher();
            mDisplayMetrics = getResources().getDisplayMetrics();

            final int width = mDisplayMetrics.widthPixels -
                DEFAULT_MARGIN_LEFT - DEFAULT_MARGIN_RIGHT;

            mGeometry[0] = 0;
            mGeometry[1] = DEFAULT_MARGIN_TOP;
            mGeometry[2] = Math.min(width, DEFAULT_WIDTH);
            mGeometry[3] = WindowManager.LayoutParams.WRAP_CONTENT;

            mCB.onSetupView(this);
            setContentViewInner(view);
        }
    }

    private void setContentViewInner(View view) {
        removeAllViews();
        mLastEntry = null;
        mContentView = view;
        addState(CONTENT_CHANGED);
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
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            onActive();
            break;
        }

        return super.dispatchTouchEvent(event);
    }

    private void onActive() {
        schedule(MSG_SHOW, mNotiDisplayTimeOnActive);

        // TODO : checkMark()
    }

    private void show() {
        if (mContentView.getParent() == null) {
            addView(mContentView);
        }

        if (getParent() == null) {
            if (mWindowManager == null) {
                mWindowManager = (WindowManager)
                    mContext.getSystemService(Context.WINDOW_SERVICE);
            }

            mWindowManager.addView(this, getWindowLayoutParams());
        }

        mContentView.setVisibility(VISIBLE);
        mContentView.setPadding(
            mContentPadding[0], mContentPadding[1],
            mContentPadding[2], mContentPadding[3]);

        if (mShowHideAnimEnabled) {
            if (mShowAnimation == null) {
                mShowAnimation = AnimationFactory.pushDownIn();
            }
            mShowAnimation.setAnimationListener(mShowAnimationListener);
            mShowAnimation.setDuration(mShowTransitionTime);
            mContentView.startAnimation(mShowAnimation);
        } else {
            mContentView.setVisibility(VISIBLE);
        }
    }

    private void hide() {
        if (mShowHideAnimEnabled) {
            if (mHideAnimation == null) {
                mHideAnimation = AnimationFactory.pushUpOut();
            }
            mHideAnimation.setAnimationListener(mHideAnimationListener);
            mHideAnimation.setDuration(mHideTransitionTime);
            mContentView.startAnimation(mHideAnimation);
        } else {
            mContentView.setVisibility(GONE);
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
        if (hasState(LEAVING)) {
            if (DBG) Log.v(TAG, "leaving now. schedule next start.");
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
            Collection<ChildViewSwitcher> switchers = mSwitchers.values();
            for (ChildViewSwitcher switcher : switchers) {
                switcher.mSwitcher.setAnimateFirstView(false);
                switcher.mSwitcher.reset();
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
                addState(LEAVING);
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
        if (mLayoutParams != null) {
            mLayoutParams.x = mGeometry[0];
            mLayoutParams.y = mGeometry[1];
            mLayoutParams.width = mGeometry[2];
            mLayoutParams.height = mGeometry[3];
            mWindowManager.updateViewLayout(this, mLayoutParams);
        } else if (getParent() != null) {
            ViewGroup.LayoutParams lp = getLayoutParams();
            lp.width = mGeometry[2];
            lp.height = mGeometry[3];
            setLayoutParams(lp);
        }
    }

    private WindowManager.LayoutParams getWindowLayoutParams() {
        if (mLayoutParams == null) {
            mLayoutParams = new WindowManager.LayoutParams();
            mLayoutParams.gravity = Gravity.TOP;
            mLayoutParams.setTitle(getClass().getSimpleName());
            mLayoutParams.packageName = mContext.getPackageName();
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_TOAST;
            mLayoutParams.format = PixelFormat.TRANSLUCENT;
            mLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        mLayoutParams.x = mGeometry[0];
        mLayoutParams.y = mGeometry[1];
        mLayoutParams.width = mGeometry[2];
        mLayoutParams.height = mGeometry[3];
        return mLayoutParams;
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
            schedule(MSG_SWITCH_TO_SELF, mNotiDisplayTime);
        }

        /**
         * switch to target
         */
        private void start(View target) {
            schedule(MSG_SWITCH_TO_TARGET, 0, 0, target, 0);
        }
    }

    public class ChildViewSwitcher {
        final int mContentType;
        int mContainerResId;
        int mAttrsResId;
        int mSwitcherResId;
        ViewGroup mContainer;
        ViewSwitcher mSwitcher;
        Animation mInAnimation;
        Animation mOutAnimation;
        int mInDuration = NOTIFICATION_TRANSITION_TIME;
        int mOutDuration = NOTIFICATION_TRANSITION_TIME;

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

        private void updateContainer(int resId, int attrs) {
            mContainerResId = resId;
            mAttrsResId = attrs;
            mSwitcherResId = 0;
            reinit();
        }

        private void updateSwitcher(int resId) {
            mContainerResId = 0;
            mAttrsResId = 0;
            mSwitcherResId = resId;
            reinit();
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
                mOutAnimation.setDuration(mOutDuration);
                mSwitcher.setOutAnimation(mOutAnimation);
            }
        }

        // TODO: a better way to wrap content of mContentView, especially TextView.
        private void adjustHeight() {
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
            };

        private void reinit() {
            if (mContainer != null) {
                mContainer.removeAllViews();
            }
            mContainer = null;
            mSwitcher = null;
            init();
        }

        private void init() {
            if (mContainer != null || mSwitcher != null || mContentView == null) return;
            if (DBG) Log.v(TAG, "switcher[" + mContentType + "] init.");
            if (mContainerResId != 0) {
                mContainer = (ViewGroup) mContentView.findViewById(mContainerResId);
                ViewGroup.LayoutParams lp = mContainer.getLayoutParams();
                if (SWITCHER_ICON == mContentType) {
                    mSwitcher = new ImageSwitcher(mContext);
                    for (int i = 0; i < 2; i++) {
                        ImageView iv = new ImageView(mContext);
                        iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        mSwitcher.addView(iv, i, lp);
                    }
                } else if (SWITCHER_TITLE == mContentType ||
                           SWITCHER_TEXT == mContentType ||
                           SWITCHER_WHEN == mContentType) {
                    mSwitcher = new TextSwitcher(mContext);
                    for (int i = 0; i < 2; i++) {
                        TextView tv = new TextView(mContext);
                        if (mAttrsResId != 0) {
                            tv.setTextAppearance(mContext, mAttrsResId);
                        }
                        tv.setSingleLine();
                        tv.setMaxWidth(200);
                        mSwitcher.addView(tv, i, lp);
                    }
                }
                mContainer.addView(mSwitcher);
            } else if (mSwitcherResId != 0) {
                mSwitcher = (ViewSwitcher) mContentView.findViewById(mSwitcherResId);
            }
            if (mSwitcher == null) {
                throw new IllegalStateException("child-view switcher cannot be null.");
            }
            updateAnimation();
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
                if (mWindowManager != null) {
                    if (NotificationView.this.getParent() != null) {
                        mWindowManager.removeView(NotificationView.this);
                    }
                } else {
                    if (mContentView.getParent() != null) {
                        removeView(mContentView);
                    }
                }
                clearState(LEAVING);
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

    private static final int STARTING               = 0x00000001;
    private static final int TICKING                = 0x00000002;
    private static final int LEAVING                = 0x00000004;
    private static final int PAUSED                 = 0x00000008;
    private static final int CONTENT_CHANGED        = 0x00000010;

    private int mState;

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
