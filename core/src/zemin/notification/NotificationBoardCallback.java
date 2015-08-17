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

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import zemin.notification.NotificationBoard.RowView;
import zemin.notification.NotificationEntry.Action;

/**
 * Callback for {@link NotificationBoard}. This is the default implementation.
 */
public class NotificationBoardCallback {

    private static final String TAG = "zemin.NotificationBoardCallback";
    public static boolean DBG;

    /**
     * Called only once after this callback is set.
     *
     * @param board
     */
    public void onBoardSetup(NotificationBoard board) {
        if (DBG) Log.v(TAG, "onBoardSetup");

        LayoutInflater inflater = board.getInflater();
        View footer = inflater.inflate(R.layout.notification_board_footer, null, false);
        board.addFooterView(footer);
        board.setFooterHeight(200);

        // board.setHeaderHeight(150);
        board.setHeaderMargin(0, 150, 0, 0);

        board.setHeaderDivider(R.drawable.divider_horizontal_dark);
        board.setFooterDivider(R.drawable.divider_horizontal_dark);
        board.setRowDivider(R.drawable.divider_horizontal_dark);

        board.setBoardPadding(80, 0, 80, 0);
        board.setRowMargin(20, 0, 20, 0);

        board.setClearView(footer.findViewById(R.id.clear));

        board.addStateListener(new NotificationBoard.SimpleStateListener() {

                @Override
                public void onBoardTranslationY(NotificationBoard board, float y) {
                    final float t = board.getBoardTranslationY();
                    if (t == 0.0f) {
                        board.showClearView(false);
                    } else if (t == -board.getBoardHeight()) {
                        board.showClearView(false);
                    } else if (y == 0.0f) {
                        board.showClearView(board.getNotificationCount() > 0);
                    }
                }

                @Override
                public void onBoardEndOpen(NotificationBoard board) {
                    board.showClearView(board.getNotificationCount() > 0);
                }

                @Override
                public void onBoardStartClose(NotificationBoard board) {
                    board.showClearView(false);
                }
            });
    }

    /**
     * Called to instantiate a view being placed in the row view,
     * which is the user interface for the incoming notification.
     *
     * @param board
     * @param entry
     * @param inflater
     * @return View
     */
    public View makeRowView(NotificationBoard board, NotificationEntry entry, LayoutInflater inflater) {

        return inflater.inflate(R.layout.notification_board_row, null, false);
    }

    /**
     * Called when a row view is added to the board.
     *
     * @param board
     * @param rowView
     * @param entry
     */
    public void onRowViewAdded(NotificationBoard board, RowView rowView, NotificationEntry entry) {
        if (DBG) Log.v(TAG, "onRowViewAdded - " + entry.ID);

        if (entry.hasActions()) {
            ArrayList<Action> actions = entry.getActions();
            ViewGroup vg = (ViewGroup) rowView.findViewById(R.id.actions);
            vg.setVisibility(View.VISIBLE);
            vg = (ViewGroup) vg.getChildAt(0);

            final View.OnClickListener onClickListener = new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        Action act = (Action) view.getTag();
                        onClickActionView(act.entry, act, view);
                        act.execute(view.getContext());
                    }
                };

            for (int i = 0, count = actions.size(); i < count; i++) {
                Action act = actions.get(i);
                Button actionBtn = (Button) vg.getChildAt(i);
                actionBtn.setVisibility(View.VISIBLE);
                actionBtn.setTag(act);
                actionBtn.setText(act.title);
                actionBtn.setOnClickListener(onClickListener);
                actionBtn.setCompoundDrawablesWithIntrinsicBounds(act.icon, 0, 0, 0);
            }
        }
    }

    /**
     * Called when a row view is removed from the board.
     *
     * @param board
     * @param rowView
     * @param entry
     */
    public void onRowViewRemoved(NotificationBoard board, RowView rowView, NotificationEntry entry) {
        if (DBG) Log.v(TAG, "onRowViewRemoved - " + entry.ID);
    }

    /**
     * Called when a row view is being updated.
     *
     * @param board
     * @param rowView
     * @param entry
     */
    public void onRowViewUpdate(NotificationBoard board, RowView rowView, NotificationEntry entry) {
        if (DBG) Log.v(TAG, "onRowViewUpdate - " + entry.ID);

        ImageView iconView = (ImageView) rowView.findViewById(R.id.icon);
        TextView titleView = (TextView) rowView.findViewById(R.id.title);
        TextView textView = (TextView) rowView.findViewById(R.id.text);
        TextView whenView = (TextView) rowView.findViewById(R.id.when);
        ProgressBar bar = (ProgressBar) rowView.findViewById(R.id.progress);

        if (entry.iconDrawable != null) {
            iconView.setImageDrawable(entry.iconDrawable);
        } else if (entry.smallIconRes != 0) {
            iconView.setImageResource(entry.smallIconRes);
        } else if (entry.largeIconBitmap != null) {
            iconView.setImageBitmap(entry.largeIconBitmap);
        }

        titleView.setText(entry.title);
        textView.setText(entry.text);

        if (entry.showWhen) {
            whenView.setText(entry.whenFormatted);
        }

        if (entry.progressMax != 0 || entry.progressIndeterminate) {
            bar.setVisibility(View.VISIBLE);
            bar.setIndeterminate(entry.progressIndeterminate);
            if (!entry.progressIndeterminate) {
                bar.setMax(entry.progressMax);
                bar.setProgress(entry.progress);
            }
        } else {
            bar.setVisibility(View.GONE);
        }

    }

    /**
     * Called when a row view has been clicked.
     *
     * @param board
     * @param rowView
     * @param entry
     */
    public void onClickRowView(NotificationBoard board, RowView rowView, NotificationEntry entry) {
        if (DBG) Log.v(TAG, "onClickRowView - " + entry.ID);
    }

    /**
     * Called when the clear view has been clicked.
     *
     * @param board
     * @param clearView
     */
    public void onClickClearView(NotificationBoard board, View clearView) {
        if (DBG) Log.v(TAG, "onClickClearView");
    }

    public void onClickActionView(NotificationEntry entry, Action act, View actionView) {
        if (DBG) Log.v(TAG, "onClickActionView - " + entry.ID + ", " + act);
    }
}
