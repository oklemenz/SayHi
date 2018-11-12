package de.oklemenz.sayhi.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.jdeferred.DoneCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.Tag;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.DataService;
import de.oklemenz.sayhi.service.Utilities;
import de.oklemenz.sayhi.view.SimilarTagsAdapter;

/**
 * Created by Oliver Klemenz on 15.02.17.
 */

public class SimilarTagsActivity extends BaseActivity {

    @Bind(R.id.activityIndicator)
    ProgressBar activityIndicator;

    @Bind(R.id.titleLabel)
    TextView titleLabel;

    @Bind(R.id.tagList)
    ListView tagList;

    private String contentLangCode = UserData.getInstance().getLangCode();

    private List<Tag> tags = new ArrayList<>();

    public final static String DELEGATE = "delegate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.similar_tags);

        Intent intent = getIntent();
        tags = (List<Tag>) intent.getSerializableExtra("tags");
        if (intent.getStringExtra("contentLangCode") != null) {
            contentLangCode = intent.getStringExtra("contentLangCode");
        }

        updateTitle();
        updateContent();
    }

    private void updateTitle() {
        String titleHTML = "<b><font color='" + AppDelegate.AccentColor + "'>" + this.getString(R.string.SimilarTags) + "</font></b>";
        titleHTML += "<br/><font color='" + Color.BLACK + "'><small>" + new Locale(contentLangCode).getDisplayLanguage() + "</font></small>";
        titleLabel.setText(Utilities.toSpan(titleHTML));
    }

    private void updateContent() {
        tagList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SimilarTagsAdapter adapter = ((SimilarTagsAdapter) parent.getAdapter());
                Tag tag = adapter.getItem(position);
                if (tag != null) {
                    ResultReceiver receiver = getIntent().getParcelableExtra(DELEGATE);
                    Bundle resultData = new Bundle();
                    resultData.putSerializable("tag", tag);
                    receiver.send(Activity.RESULT_OK, resultData);
                    finish();
                }
            }
        });
        activityIndicator.setVisibility(View.VISIBLE);
        DataService.getInstance().completeTags(tags).then(new DoneCallback<List<Tag>>() {
            @Override
            public void onDone(List<Tag> result) {
                tagList.setAdapter(new SimilarTagsAdapter(SimilarTagsActivity.this, tags));
                activityIndicator.setVisibility(View.GONE);
            }
        });
    }

    public void onHelpPressed(View view) {
        showHelp(view, R.layout.similar_tags_help);
    }

    public void onUseOwnPressed(View view) {
        ResultReceiver receiver = getIntent().getParcelableExtra(DELEGATE);
        Bundle resultData = new Bundle();
        resultData.putBoolean("useOwn", true);
        receiver.send(Activity.RESULT_OK, resultData);
        finish();
    }
}