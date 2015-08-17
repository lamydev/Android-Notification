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

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.lang.ref.WeakReference;

/**
 *
 */
public class ViewSwitcherWrapper extends ViewWrapper {

    private static final String TAG = "zemin.ViewSwitcherWrapper";
    public static boolean DBG;

    public static final int TRANSITION_TIME = 700;

    private Animation mInAnimation;
    private Animation mOutAnimation;
    private int mInDuration = TRANSITION_TIME;
    private int mOutDuration = TRANSITION_TIME;

    public ViewSwitcherWrapper(String name) {
        super(name);
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

    @Override
    public void reset() {
        super.reset();
        View view = getView();
        if (view == null) {
            return;
        }

        ViewSwitcher viewSwitcher = (ViewSwitcher) view;
        viewSwitcher.setAnimateFirstView(false);
        viewSwitcher.reset();
    }

    @Override
    public void setView(View view) {
        super.setView(view);
        updateAnimation();
    }

    @Override
    public void setImageDrawable(Drawable drawable, boolean animate) {
        View view = getView();
        if (view == null) {
            return;
        }

        ImageSwitcher imageSwitcher = (ImageSwitcher) view;
        if (animate) {
            imageSwitcher.setImageDrawable(drawable);
        } else {
            ImageView curr = (ImageView) imageSwitcher.getCurrentView();
            curr.setImageDrawable(drawable);
        }
    }

    @Override
    public void setText(CharSequence text, boolean animate) {
        View view = getView();
        if (view == null) {
            return;
        }

        TextSwitcher textSwitcher = (TextSwitcher) view;
        if (animate) {
            textSwitcher.setText(text);
        } else {
            TextView curr = (TextView) textSwitcher.getCurrentView();
            curr.setText(text);
        }

        //
        // waiting for the first layout of SWITCHER to be finished,
        // so that we can adjust its size according to its content.
        //
        // 100 ms
        //
        schedule(MSG_TEXT_VIEW_ADJUST_HEIGHT,  100);
    }

    @Override
    public void setTextSize(int size) {
        View view = getView();
        if (view == null) {
            return;
        }

        TextSwitcher textSwitcher = (TextSwitcher) view;
        for (int i = 0; i < 2; i++) {
            TextView titleView = (TextView) textSwitcher.getChildAt(i);
            titleView.setTextSize(size);
        }
    }

    @Override
    public void setTextColor(int color) {
        View view = getView();
        if (view == null) {
            return;
        }

        TextSwitcher textSwitcher = (TextSwitcher) view;
        for (int i = 0; i < 2; i++) {
            TextView titleView = (TextView) textSwitcher.getChildAt(i);
            titleView.setTextColor(color);
        }
    }

    private void updateAnimation() {
        View view = getView();
        if (view == null) {
            return;
        }

        ViewSwitcher viewSwitcher = (ViewSwitcher) view;
        if (viewSwitcher.getInAnimation() != mInAnimation || mInAnimation == null) {
            if (mInAnimation == null) {
                mInAnimation = AnimationFactory.pushDownIn();
            }
            if (viewSwitcher instanceof TextSwitcher) {
                mInAnimation.setAnimationListener(mTextViewInAnimationListener);
            }
            mInAnimation.setDuration(mInDuration);
            viewSwitcher.setInAnimation(mInAnimation);
        }
        if (viewSwitcher.getOutAnimation() != mOutAnimation || mOutAnimation == null) {
            if (mOutAnimation == null) {
                mOutAnimation = AnimationFactory.pushDownOut();
            }
            mOutAnimation.setDuration(mOutDuration);
            viewSwitcher.setOutAnimation(mOutAnimation);
        }
    }

    // TODO: a better way to wrap content of TextView.
    private void adjustTextViewHeight() {
        View view = getView();
        if (view == null) {
            return;
        }

        TextSwitcher textSwitcher = (TextSwitcher) view;
        TextView curr = (TextView) textSwitcher.getCurrentView();
        TextView next = (TextView) textSwitcher.getNextView();
        int currH = curr.getLineCount() * curr.getLineHeight();
        int nextH = next.getLineCount() * next.getLineHeight();
        if (currH != nextH) {
            curr.setHeight(currH);
            next.setHeight(currH);
        }
    }

    private final AnimationListener mTextViewInAnimationListener = new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                adjustTextViewHeight();
            }
        };

    // ------------------------------------------------
    // handler

    private static final int MSG_TEXT_VIEW_ADJUST_HEIGHT = 0;

    protected void handleMessage(Message msg) {
        switch (msg.what) {
        case MSG_TEXT_VIEW_ADJUST_HEIGHT:
            adjustTextViewHeight();
            break;
        }
    }

    private H mH;

    private void cancel(int what) {
        final H h = getHandler();
        if (what == -1)
            h.removeCallbacksAndMessages(null);
        else
            h.removeMessages(what);
    }

    private void schedule(int what, int delay) {
        final H h = getHandler();
        h.removeMessages(what);
        h.sendEmptyMessageDelayed(what, delay);
    }

    private void schedule(int what, int arg1, int arg2, Object obj, int delay) {
        final H h = getHandler();
        h.removeMessages(what);
        h.sendMessageDelayed(h.obtainMessage(what, arg1, arg2, obj), delay);
    }

    public H getHandler() { if (mH == null) mH = new H(this); return mH; }

    private static final class H extends Handler {
        private WeakReference<ViewSwitcherWrapper> mView;
        H(ViewSwitcherWrapper v) {
            super(Looper.getMainLooper());
            mView = new WeakReference<ViewSwitcherWrapper>(v);
        }
        @Override
        public void handleMessage(Message msg) {
            ViewSwitcherWrapper v = mView.get();
            if (v != null) v.handleMessage(msg);
        }
    }
}
