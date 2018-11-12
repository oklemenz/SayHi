package de.oklemenz.sayhi.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.Bind;
import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.Match;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.Emoji;
import de.oklemenz.sayhi.service.SwipeGestureRecognizer;
import de.oklemenz.sayhi.service.Utilities;
import de.oklemenz.sayhi.view.HistorySectionAdapter;

/**
 * Created by Oliver Klemenz on 15.02.17.
 */

public class HistoryActivity extends BaseActivity {

    @Bind(R.id.titleLabel)
    TextView titleLabel;

    @Bind(R.id.deleteAllButton)
    ImageButton deleteAllButton;

    @Bind(R.id.editButton)
    Button editButton;

    @Bind(R.id.historyList)
    ListView historyList;

    private boolean editing = false;
    private int editingPos = -1;

    protected GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history);

        historyList.setAdapter(new HistorySectionAdapter(this));
        historyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!editing) {
                    int matchIndex = getHistoryAdapter().getMatchIndex(position);
                    if (matchIndex > -1) {
                        displayMatch(matchIndex);
                    }
                } else {
                    setEditing(false);
                }
            }
        });

        gestureDetector = new GestureDetector(this, new SwipeGestureRecognizer(historyList, new SwipeGestureRecognizer.Callback() {
            @Override
            public void swipeLeft(int position) {
                if (editingPos == -1) {
                    if (!editing) {
                        setEditingPos(position);
                        refresh();
                    }
                } else {
                    setEditingPos(-1);
                    refresh();
                }
            }

            @Override
            public void swipeRight(int position) {
                setEditingPos(-1);
                refresh();
            }
        }));
        historyList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    MotionEvent cancelEvent = MotionEvent.obtain(event);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                    v.onTouchEvent(cancelEvent);
                    return true;
                }
                return false;
            }
        });


        updateContent();
        updateLabels();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    public void onHelpPressed(View view) {
        showHelp(view, R.layout.history_help);
    }

    protected Dialog showHelp(View view, View helpView) {
        TextView profileEditMore = (TextView) helpView.findViewById(R.id.matchInformation);
        profileEditMore.setText(Utilities.toSpan(this.getTermString("MatchInformation",
                Emoji.getLike(Emoji.Size.Medium),
                Emoji.getDislike(Emoji.Size.Medium),
                Emoji.getRelationType(Emoji.Size.Medium),
                Emoji.getMatchingMode(Emoji.Size.Medium))));
        if (TextUtils.isEmpty(profileEditMore.getText())) {
            helpView.findViewById(R.id.matchInformationArrow).setVisibility(View.INVISIBLE);
        }
        return super.showHelp(view, helpView);
    }

    public void onEditPressed(View view) {
        setEditing(!editing);
    }

    public void onMatchDetailPressed(View view) {
        displayMatch(getHistoryAdapter().getMatchIndex((int) view.getTag()));
    }

    public void onItemDeletePressed(View view) {
        final Match match = getHistoryAdapter().getMatch((int) view.getTag());
        AlertDialog.Builder builder = new AlertDialog.Builder(HistoryActivity.this);
        builder.setTitle(R.string.MatchDeletion)
                .setMessage(R.string.DeleteMatch)
                .setCancelable(false)
                .setPositiveButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setEditing(false);
                    }
                })
                .setNeutralButton(R.string.Delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setEditing(false);
                        UserData.getInstance().removeMatch(match);
                        refresh();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }

    private void updateContent() {
        setEditing(false);
    }

    private void updateLabels() {
        String matches = String.format(this.getString(UserData.getInstance().scoreMatchCount == 1 ? R.string.MatchNum : R.string.MatchesNum), UserData.getInstance().scoreMatchCount);
        String matchScore = String.format(this.getString(UserData.getInstance().matchScore() == 1 ? R.string.MatchingPointNum : R.string.MatchingPointsNum), UserData.getInstance().matchScore());
        String titleHTML = "<b><font color='" + AppDelegate.AccentColor + "'>" + matches + "</font></b><br/><small>" + matchScore + "</small>";
        titleLabel.setTextColor(Color.BLACK);
        titleLabel.setText(Utilities.toSpan(titleHTML));
    }

    private HistorySectionAdapter getHistoryAdapter() {
        return ((HistorySectionAdapter) historyList.getAdapter());
    }

    private void refresh() {
        getHistoryAdapter().buildSections();
        getHistoryAdapter().notifyDataSetChanged();
    }

    private void displayMatch(int matchIndex) {
        Intent matchIntent = new Intent(this, MatchActivity.class);
        matchIntent.putExtra("backLabel", this.getString(R.string.Matches));
        matchIntent.putExtra("match", matchIndex);
        this.startActivity(matchIntent, TransitionState.SHOW);
    }

    private void setEditing(boolean editing) {
        this.editing = editing;
        this.editingPos = -1;
        if (this.editing) {
            editButton.setText(R.string.Done);
        } else {
            editButton.setText(R.string.Edit);
        }
        updateButtons();
        getHistoryAdapter().setEditing(editing);
        refresh();
    }

    private void setEditingPos(int position) {
        this.editing = position >= 0;
        this.editingPos = position;
        if (this.editing) {
            editButton.setText(R.string.Done);
        } else {
            editButton.setText(R.string.Edit);
        }
        updateButtons();
        getHistoryAdapter().setEditing(false);
        getHistoryAdapter().editingPos = position;
        refresh();
    }

    public void updateButtons() {
        if (editing && UserData.getInstance().history.size() > 0) {
            deleteAllButton.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) deleteAllButton.getLayoutParams();
            layoutParams.width = Utilities.convertDpToPx(this, 34);
            deleteAllButton.setLayoutParams(layoutParams);
        } else {
            deleteAllButton.setVisibility(View.INVISIBLE);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) deleteAllButton.getLayoutParams();
            layoutParams.width = 0;
            deleteAllButton.setLayoutParams(layoutParams);
        }
    }

    public void onDeleteAllPressed(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(HistoryActivity.this);
        builder.setTitle(R.string.DeleteMatchingHistory)
                .setMessage(R.string.DeleteAllMatches)
                .setCancelable(false)
                .setPositiveButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNegativeButton(R.string.OK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        UserData.getInstance().clearHistory();
                        setEditing(false);
                        refresh();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }
}