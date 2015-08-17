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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 *
 */
public class ViewWrapper {

    private static final String TAG = "zemin.ViewWrapper";
    public static boolean DBG;

    public final String name;
    public View view;

    public ViewWrapper(String name) {
        this.name = name;
    }

    public void clear() {
        view = null;
    }

    public void reset() {
    }

    public boolean hasView() {
        return view != null;
    }

    public void setView(View view) {
        this.view = view;
    }

    public View getView() {
        return view;
    }

    public void show() {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    public void hide() {
        if (view != null) {
            view.setVisibility(View.INVISIBLE);
        }
    }

    public void setImageDrawable(Drawable drawable) {
        setImageDrawable(drawable, true);
    }

    // throws ClassCastException
    public void setImageDrawable(Drawable drawable, boolean animate) {
        if (view != null) {
            ((ImageView) view).setImageDrawable(drawable);
        }
    }

    public void setText(CharSequence text) {
        setText(text, true);
    }

    // throws ClassCastException
    public void setText(CharSequence text, boolean animate) {
        if (view != null) {
            ((TextView) view).setText(text);
        }
    }

    public void setTextSize(int size) {
        if (view != null) {
            ((TextView) view).setTextSize(size);
        }
    }

    public void setTextColor(int color) {
        if (view != null) {
            ((TextView) view).setTextColor(color);
        }
    }
}
