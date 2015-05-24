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

package zemin.notification.samples;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.support.v7.app.ActionBarActivity;

import zemin.notification.NotificationBoard;
import zemin.notification.NotificationBoardCallback;
import zemin.notification.NotificationBuilder;
import zemin.notification.NotificationDelegater;
import zemin.notification.NotificationEntry;
import zemin.notification.NotificationGlobal;
import zemin.notification.NotificationListener;
import zemin.notification.NotificationLocal;
import zemin.notification.NotificationRemote;
import zemin.notification.NotificationView;
import zemin.notification.NotificationViewCallback;

//
public class MainActivity extends ActionBarActivity {

    private static final String TAG = "zemin.Notification.samples";
    private static final boolean DBG = true;

    private NotificationDelegater mDelegater;
    private NotificationRemote mRemote;
    private NotificationLocal mLocal;
    private NotificationGlobal mGlobal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();

        mDelegater = NotificationDelegater.getInstance();
        mRemote = mDelegater.remote();
        mLocal = mDelegater.local();
        mGlobal = mDelegater.global();

        // enable global view && board
        mGlobal.setViewEnabled(true);
        mGlobal.setBoardEnabled(true);

        // attach local NotificationView
        mLocal.setView((NotificationView) findViewById(R.id.nv));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // add listener
        mDelegater.addListener(mNotificationListener);

        // update notification count
        mTvTotalCount.setText(String.valueOf(mDelegater.getNotificationCount()));
    }

    @Override
    protected void onPause() {
        super.onPause();

        // remove listener
        mDelegater.removeListener(mNotificationListener);
    }

    private final NotificationListener mNotificationListener = new NotificationListener() {
            @Override
            public void onArrival(NotificationEntry entry) {
                updateNotificationCount(entry);
            }

            @Override
            public void onCancel(NotificationEntry entry) {
                updateNotificationCount(entry);
            }
        };

    private void updateNotificationCount(NotificationEntry entry) {
        if (entry.isSentToRemote()) {
            mTvRemoteCount.setText(String.valueOf(mRemote.getNotificationCount()));
        }
        if (entry.isSentToLocalView()) {
            mTvLocalCount.setText(String.valueOf(mLocal.getNotificationCount()));
        }
        if (entry.isSentToGlobalView()) {
            mTvGlobalCount.setText(String.valueOf(mGlobal.getNotificationCount()));
        }
        mTvTotalCount.setText(String.valueOf(mDelegater.getNotificationCount()));
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            public void onClick(View view) {

                switch (view.getId()) {
                case R.id.btn_remote_send: {
                    // send to remote
                    String title = getNextTitle();
                    String text = getNextText();

                    NotificationBuilder.V2 builder = NotificationBuilder.remote()
                        .setSmallIconResource(R.drawable.ic_launcher)
                        .setTicker(title + ": " + text)
                        .setTitle(title)
                        .setText(text);

                    mDelegater.send(builder.getNotification());

                } break;

                case R.id.btn_remote_cancel: {
                    // cancel all remote
                    mRemote.cancelAll();
                } break;

                case R.id.btn_local_send: {
                    // send to local view

                    NotificationBuilder.V1 builder = NotificationBuilder.local()
                        .setBackgroundColor(getNextColor())
                        .setLayoutId(getNextLayout())
                        .setIconDrawable(getResources().getDrawable(R.drawable.ic_launcher))
                        .setTitle(getNextTitle())
                        .setText(getNextText());

                    mDelegater.send(builder.getNotification());

                } break;

                case R.id.btn_local_cancel: {
                    // cancel all local
                    mLocal.cancelAll();
                } break;

                case R.id.btn_local_dismiss: {
                    // dismiss view
                    mLocal.dismissView();
                } break;

                case R.id.btn_global_send: {
                    // send to global view

                    NotificationBuilder.V1 builder = NotificationBuilder.global()
                        .setIconDrawable(getResources().getDrawable(R.drawable.ic_launcher))
                        .setTitle(getNextTitle())
                        .setText(getNextText());

                    mDelegater.send(builder.getNotification());

                } break;

                case R.id.btn_global_cancel: {
                    // cancel all global
                    mGlobal.cancelAll();
                } break;

                case R.id.btn_global_dismiss: {
                    // dismiss view, but not cancel the rest
                    mGlobal.dismissView();
                } break;

                case R.id.btn_total_cancel: {
                    // cancel all
                    mDelegater.cancelAll();
                } break;

                case R.id.btn_board: {
                    // open notification board
                    mGlobal.openBoard();
                    break;
                }
                }
            }
        };


    private Button mBtnRemoteSend;
    private Button mBtnRemoteCancel;
    private TextView mTvRemoteCount;
    private Button mBtnLocalSend;
    private Button mBtnLocalCancel;
    private Button mBtnLocalDismiss;
    private TextView mTvLocalCount;
    private Button mBtnGlobalSend;
    private Button mBtnGlobalCancel;
    private Button mBtnGlobalDismiss;
    private TextView mTvGlobalCount;
    private Button mBtnTotalCancel;
    private TextView mTvTotalCount;
    private int mTitleIdx;
    private int mTextIdx;
    private int mColorIdx;
    private int mLayoutIdx;
    private Button mBtnBoard;

    private static final String[] mTitleSet = new String[] {
        "Guess Who",
        "Meet Android",
        "I'm a Notification",
    };

    private static final String[] mTextSet = new String[] {
        "hello world",
        "welcome to the android world",
        "welcome to code samples for zemin-notification. " +
        "Here you can browse sample code and learn how to send, show and cancel a notification.",
        "zemin-notification library is available on GitHub under the Apache License v2.0. " +
        "You are free to make use of it.",
    };

    // if color is set to 0, the default will be used.
    private static final int[] mColorSet = new int[] {
        0,
        0xffc0ca33,
        0xff8bc34a,
        0xff00938d,
        0xff607d8b,
    };

    // if layout is set to 0, the default will be used.
    private static final int[] mLayoutSet = new int[] {
        0,
        zemin.notification.R.layout.notification_full,
        zemin.notification.R.layout.notification_simple,
        zemin.notification.R.layout.notification_large_icon,
        zemin.notification.R.layout.notification_simple_2,
    };

    private String getNextTitle() {
        if (mTitleIdx == mTitleSet.length) {
            mTitleIdx = 0;
        }
        return mTitleSet[mTitleIdx++];
    }

    private String getNextText() {
        if (mTextIdx == mTextSet.length) {
            mTextIdx = 0;
        }
        return mTextSet[mTextIdx++];
    }

    private int getNextColor() {
        if (mColorIdx == mColorSet.length) {
            mColorIdx = 0;
        }
        return mColorSet[mColorIdx++];
    }

    private int getNextLayout() {
        if (mLayoutIdx == mLayoutSet.length) {
            mLayoutIdx = 0;
        }
        return mLayoutSet[mLayoutIdx++];
    }

    private void initView() {
        setContentView(R.layout.main);

        mBtnRemoteSend = (Button) findViewById(R.id.btn_remote_send);
        mBtnRemoteSend.setOnClickListener(mOnClickListener);
        mBtnRemoteCancel = (Button) findViewById(R.id.btn_remote_cancel);
        mBtnRemoteCancel.setOnClickListener(mOnClickListener);
        mTvRemoteCount = (TextView) findViewById(R.id.tv_remote_count);
        mBtnLocalSend = (Button) findViewById(R.id.btn_local_send);
        mBtnLocalSend.setOnClickListener(mOnClickListener);
        mBtnLocalCancel = (Button) findViewById(R.id.btn_local_cancel);
        mBtnLocalCancel.setOnClickListener(mOnClickListener);
        mBtnLocalDismiss = (Button) findViewById(R.id.btn_local_dismiss);
        mBtnLocalDismiss.setOnClickListener(mOnClickListener);
        mTvLocalCount = (TextView) findViewById(R.id.tv_local_count);
        mBtnGlobalSend = (Button) findViewById(R.id.btn_global_send);
        mBtnGlobalSend.setOnClickListener(mOnClickListener);
        mBtnGlobalCancel = (Button) findViewById(R.id.btn_global_cancel);
        mBtnGlobalCancel.setOnClickListener(mOnClickListener);
        mBtnGlobalDismiss = (Button) findViewById(R.id.btn_global_dismiss);
        mBtnGlobalDismiss.setOnClickListener(mOnClickListener);
        mTvGlobalCount = (TextView) findViewById(R.id.tv_global_count);
        mBtnTotalCancel = (Button) findViewById(R.id.btn_total_cancel);
        mBtnTotalCancel.setOnClickListener(mOnClickListener);
        mTvTotalCount = (TextView) findViewById(R.id.tv_total_count);
        mBtnBoard = (Button) findViewById(R.id.btn_board);
        mBtnBoard.setOnClickListener(mOnClickListener);
    }
}
