package zemin.notification;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import static zemin.notification.NotificationView.SWITCHER_ICON;
import static zemin.notification.NotificationView.SWITCHER_TITLE;
import static zemin.notification.NotificationView.SWITCHER_TEXT;
import static zemin.notification.NotificationView.SWITCHER_WHEN;

//
// NotificationView callback
//
// @author Zemin Liu
//
public class NotificationViewCallback implements NotificationView.Callback {

    public static boolean DBG;
    private static final String TAG = "zemin.NotificationViewCallback";

    private ImageView mViewIcon;
    private TextView mViewTitle;
    private TextView mViewText;
    private TextView mViewWhen;

    @Override
    public int getDefaultContentResId() {
        return R.layout.zemin_notification_simple_2;
    }

    @Override
    public void onSetupView(NotificationView view) {
    }

    @Override
    public void onContentViewChanged(NotificationView view, View contentView, int contentResId) {

        if (contentResId == R.layout.zemin_notification_simple ||
            contentResId == R.layout.zemin_notification_large_icon ||
            contentResId == R.layout.zemin_notification_full) {

            view.setChildViewSwitcher(SWITCHER_ICON, R.id.switcher_icon);
            view.setChildViewSwitcher(SWITCHER_TITLE, R.id.switcher_title);
            view.setChildViewSwitcher(SWITCHER_TEXT, R.id.switcher_text);
            view.setChildViewSwitcher(SWITCHER_WHEN, R.id.switcher_when);

        } else if (contentResId == R.layout.zemin_notification_simple_2) {

            mViewIcon = (ImageView) contentView.findViewById(R.id.icon);
            mViewTitle = (TextView) contentView.findViewById(R.id.title);
            mViewText = (TextView) contentView.findViewById(R.id.text);
            mViewWhen = (TextView) contentView.findViewById(R.id.when);
        }
    }

    @Override
    public void onShowNotification(NotificationView view, NotificationEntry entry, int contentResId) {

        final Drawable icon = entry.iconDrawable;
        final CharSequence title = entry.title;
        final CharSequence text = entry.text;
        CharSequence when = null;

        if (entry.showWhen) {
            if (entry.whenFormatted == null) {
                entry.setWhen(null, entry.whenLong > 0L ? entry.whenLong : System.currentTimeMillis());
            }
            when = entry.whenFormatted;
        }

        if (contentResId == R.layout.zemin_notification_simple ||
            contentResId == R.layout.zemin_notification_large_icon ||
            contentResId == R.layout.zemin_notification_full) {

            ImageSwitcher iconSwitcher = view.getIconSwitcher();
            TextSwitcher titleSwitcher = view.getTitleSwitcher();
            TextSwitcher textSwitcher = view.getTextSwitcher();
            TextSwitcher whenSwitcher = view.getWhenSwitcher();
            NotificationEntry lastEntry = view.getLastEntry();
            boolean titleChanged = true;

            if (!view.isContentLayoutChanged() && title != null &&
                lastEntry != null && title.equals(lastEntry.title)) {
                titleChanged = false;
            }

            if (iconSwitcher != null) {
                if (icon != null) {
                    iconSwitcher.setVisibility(View.VISIBLE);
                    if (titleChanged) {
                        iconSwitcher.setImageDrawable(icon);
                    }
                } else {
                    iconSwitcher.setVisibility(View.INVISIBLE);
                }
            }

            if (titleSwitcher != null) {
                if (title != null && !title.equals("")) {
                    titleSwitcher.setVisibility(View.VISIBLE);
                    if (titleChanged) {
                        titleSwitcher.setText(title);
                    }
                } else {
                    titleSwitcher.setVisibility(View.INVISIBLE);
                }
            }

            if (textSwitcher != null) {
                if (text != null) {
                    textSwitcher.setVisibility(View.VISIBLE);
                    textSwitcher.setText(text);
                } else {
                    textSwitcher.setVisibility(View.INVISIBLE);
                }
            }

            if (whenSwitcher != null) {
                if (when != null) {
                    whenSwitcher.setVisibility(View.VISIBLE);
                    whenSwitcher.setText(when);
                } else {
                    whenSwitcher.setVisibility(View.INVISIBLE);
                }
            }

        } else if (contentResId == R.layout.zemin_notification_simple_2) {
            view.animateContentView();

            if (mViewIcon != null) {
                mViewIcon.setImageDrawable(icon);
            }

            if (mViewTitle != null) {
                mViewTitle.setText(title);
            }

            if (mViewText != null) {
                mViewText.setText(text);
            }

            if (mViewWhen != null) {
                if (when != null) {
                    mViewWhen.setVisibility(View.VISIBLE);
                    mViewWhen.setText(when);
                } else {
                    mViewWhen.setVisibility(View.INVISIBLE);
                }
            }
        }
    }
}
