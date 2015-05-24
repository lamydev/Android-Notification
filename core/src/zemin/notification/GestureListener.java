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

import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * A convenience class to extend when you only want to listen for a subset
 * of all gesture states. This implements all methods in the
 * {@link android.view.GestureDetector#OnGestureListener} and
 * {@link android.view.GestureDetector#OnDoubleTapListener}.
 *
 * Moreover, new APIs are also introduced:
 *
 * 1) void onUpOrCancel(MotionEvent event, boolean handled);
 */
public class GestureListener implements GestureDetector.OnGestureListener,
                                        GestureDetector.OnDoubleTapListener {

    /**
     * Called when {@link MotionEvent#ACTION_UP} or {@link MotionEvent#ACTION_CANCEL} event occurs.
     *
     * @param event
     * @param handled
     */
    public void onUpOrCancel(MotionEvent event, boolean handled) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onDown(MotionEvent event) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onShowPress(MotionEvent event) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onDoubleTap(MotionEvent event) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLongPress(MotionEvent event) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        return false;
    }
}
