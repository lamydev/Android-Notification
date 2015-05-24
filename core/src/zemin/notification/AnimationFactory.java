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

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;

/**
 * Animation Factory
 */
public class AnimationFactory {

    /**
     * Create push down animation for entering.
     *
     * @return Animation
     */
    public static Animation pushDownIn() {
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setFillAfter(true);
        animationSet.addAnimation(new TranslateAnimation(0, 0, -100, 0));
        animationSet.addAnimation(new AlphaAnimation(0.0f, 1.0f));
        return animationSet;
    }

    /**
     * Create push down animation for leaving.
     *
     * @return Animation
     */
    public static Animation pushDownOut() {
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setFillAfter(true);
        animationSet.addAnimation(new TranslateAnimation(0, 0, 0, 100));
        animationSet.addAnimation(new AlphaAnimation(1.0f, 0.0f));
        return animationSet;
    }

    /**
     * Create push up animation for entering.
     *
     * @return Animation
     */
    public static Animation pushUpIn() {
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setFillAfter(true);
        animationSet.addAnimation(new TranslateAnimation(0, 0, 100, 0));
        animationSet.addAnimation(new AlphaAnimation(0.0f, 1.0f));
        return animationSet;
    }

    /**
     * Create push up animation for leaving.
     *
     * @return Animation
     */
    public static Animation pushUpOut() {
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setFillAfter(true);
        animationSet.addAnimation(new TranslateAnimation(0, 0, 0, -100));
        animationSet.addAnimation(new AlphaAnimation(1.0f, 0.0f));
        return animationSet;
    }
}
