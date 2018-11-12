package de.oklemenz.sayhi.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.jdeferred.DoneCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.service.DataService;
import de.oklemenz.sayhi.service.SecureStore;
import de.oklemenz.sayhi.service.Utilities;
import de.oklemenz.sayhi.view.HighscoreAdapter;

/**
 * Created by Oliver Klemenz on 15.02.17.
 */

public class HighscoreActivity extends BaseActivity {

    @Bind(R.id.activityIndicator)
    ProgressBar activityIndicator;

    @Bind(R.id.titleLabel)
    TextView titleLabel;

    @Bind(R.id.highscoreList)
    ListView highscoreList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.highscore);

        updateTitle();
        updateContent();
    }

    private void updateTitle() {
        String titleHTML = "<b><font color='" + AppDelegate.AccentColor + "'>" + this.getString(R.string.Highscore) + "</font></b>";
        titleHTML += "<br/><font color='" + Color.BLACK + "'><small>" + SecureStore.getSpace() + "</font></small>";
        titleLabel.setText(Utilities.toSpan(titleHTML));
    }

    private void updateContent() {
        activityIndicator.setVisibility(View.VISIBLE);
        highscoreList.setAdapter(new HighscoreAdapter(this, new ArrayList<Map<String, Object>>()));

        DataService.getInstance().fetchHighscore().then(new DoneCallback<List<Map<String, Object>>>() {
            @Override
            public void onDone(List<Map<String, Object>> values) {
                highscoreList.setAdapter(new HighscoreAdapter(HighscoreActivity.this, values));
                activityIndicator.setVisibility(View.GONE);
            }
        });
    }

    public void onRefreshPressed(View view) {
        updateContent();
    }
}