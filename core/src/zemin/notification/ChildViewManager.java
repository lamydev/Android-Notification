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
import android.widget.ViewSwitcher;

import android.support.v4.util.ArrayMap;

import java.util.Collection;

/**
 *
 */
public class ChildViewManager {

    private static final String TAG = "zemin.ChildViewManager";
    public static boolean DBG;

    public void setView(String name, View view) {
        Holder holder = mHolders.get(name);
        if (holder == null && name != null) {
            holder = new Holder(name);
            mHolders.put(name, holder);
        }
        holder.set(view);
    }

    public ViewWrapper getViewWrapper(String name) {
        Holder holder = mHolders.get(name);
        return holder != null ? holder.curr() : null;
    }

    public void show(String name) {
        getViewWrapper(name).show();
    }

    public void hide(String name) {
        getViewWrapper(name).hide();
    }

    public void setImageDrawable(String name, Drawable drawable) {
        setImageDrawable(name, drawable, true);
    }

    public void setImageDrawable(String name, Drawable drawable, boolean animate) {
        ViewWrapper v = getViewWrapper(name);
        if (drawable != null) {
            v.show();
            v.setImageDrawable(drawable, animate);
        } else {
            v.hide();
        }
    }

    public void setText(String name, CharSequence text) {
        setText(name, text, true);
    }

    public void setText(String name, CharSequence text, boolean animate) {
        ViewWrapper v = getViewWrapper(name);
        if (text != null) {
            v.show();
            v.setText(text, animate);
        } else {
            v.hide();
        }
    }

    public void setTextSize(String name, int size) {
        getViewWrapper(name).setTextSize(size);
    }

    public void setTextColor(String name, int color) {
        getViewWrapper(name).setTextColor(color);
    }

    public void clear() {
        Collection<Holder> holders = mHolders.values();
        for (Holder h : holders) {
            h.clear();
        }
    }

    public void reset() {
        Collection<Holder> holders = mHolders.values();
        for (Holder h : holders) {
            h.reset();
        }
    }

    private final ArrayMap<String, Holder> mHolders =
        new ArrayMap<String, Holder>();

    private class Holder {
        final String name;
        ViewWrapper view;
        ViewSwitcherWrapper switcher;
        Holder(String name) { this.name = name; }

        ViewWrapper curr() {
            if (switcher != null && switcher.hasView()) {
                return switcher;
            } else if (view != null && view.hasView()) {
                return view;
            }
            return null;
        }

        void set(View v) {
            if (v instanceof ViewSwitcher) {
                if (switcher == null) {
                    switcher = new ViewSwitcherWrapper(name);
                }
                switcher.setView(v);
            } else {
                if (view == null) {
                    view = new ViewWrapper(name);
                }
                view.setView(v);
            }
        }

        void clear() {
            ViewWrapper curr = curr();
            if (curr != null) curr.clear();
        }

        void reset() {
            ViewWrapper curr = curr();
            if (curr != null) curr.reset();
        }
    }
}
