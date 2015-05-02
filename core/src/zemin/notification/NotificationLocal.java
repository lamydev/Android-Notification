package zemin.notification;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

//
// NotificationView handler
//
// @author Zemin Liu
//
public class NotificationLocal extends NotificationHandler {

    public static boolean DBG;

    private NotificationView mView;

    /* package */ NotificationLocal(Context context, Looper looper) {
        super(context, NotificationEntry.TARGET_LOCAL, looper);
    }

    public void setView(NotificationView view) {
        mView = view;
        mView.setNotificationHandler(this);
    }

    public NotificationView getView() {
        return mView;
    }

    public void dismissView() {
        mView.dismiss();
    }

    public View findViewById(int resId) {
        return mView.findViewById(resId);
    }

    public boolean hasViewCallback() {
        return mView.hasCallback();
    }

    public void setViewCallback(NotificationView.Callback cb) {
        mView.setCallback(cb);
    }

    @Override
    protected void onCancel(NotificationEntry entry) {
        mView.onCancel(entry);
    }

    @Override
    protected void onCancelAll() {
        mView.onCancelAll();
    }

    @Override
    protected void onArrival(NotificationEntry entry) {
        if (mView == null) {
            Log.w(TAG, "NotificationView not found.");
            onSendIgnored(entry);
            return;
        }

        if (mView.getParent() == null) {
            throw new IllegalStateException("NotificationView should have a parent.");
        }

        if (!mView.hasCallback()) {
            mView.setCallback(DEFAULT_VIEWCALLBACK);
        }

        mView.onArrival(entry);
    }

    private final NotificationView.Callback DEFAULT_VIEWCALLBACK = new NotificationViewCallback();
}
