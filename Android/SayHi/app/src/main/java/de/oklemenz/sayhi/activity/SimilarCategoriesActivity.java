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
import de.oklemenz.sayhi.model.Category;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.DataService;
import de.oklemenz.sayhi.service.Utilities;
import de.oklemenz.sayhi.view.SimilarCategoriesAdapter;

/**
 * Created by Oliver Klemenz on 15.02.17.
 */

public class SimilarCategoriesActivity extends BaseActivity {

    @Bind(R.id.activityIndicator)
    ProgressBar activityIndicator;

    @Bind(R.id.titleLabel)
    TextView titleLabel;

    @Bind(R.id.categoryList)
    ListView categoryList;

    private String contentLangCode = UserData.getInstance().getLangCode();

    private List<Category> categories = new ArrayList<>();

    public final static String DELEGATE = "delegate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.similar_categories);

        Intent intent = getIntent();
        categories = (List<Category>) intent.getSerializableExtra("categories");
        if (intent.getStringExtra("contentLangCode") != null) {
            contentLangCode = intent.getStringExtra("contentLangCode");
        }

        updateTitle();
        updateContent();
    }

    private void updateTitle() {
        String titleHTML = "<b><font color='" + AppDelegate.AccentColor + "'>" + this.getString(R.string.SimilarCategories) + "</font></b>";
        titleHTML += "<br/><font color='" + Color.BLACK + "'><small>" + new Locale(contentLangCode).getDisplayLanguage() + "</font></small>";
        titleLabel.setText(Utilities.toSpan(titleHTML));
    }

    private void updateContent() {
        categoryList.setAdapter(new SimilarCategoriesAdapter(this, categories));
        categoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SimilarCategoriesAdapter adapter = ((SimilarCategoriesAdapter) parent.getAdapter());
                Category category = adapter.getItem(position);
                if (category != null) {
                    ResultReceiver receiver = getIntent().getParcelableExtra(DELEGATE);
                    Bundle resultData = new Bundle();
                    resultData.putSerializable("category", category);
                    receiver.send(Activity.RESULT_OK, resultData);
                    finish();
                }
            }
        });
        activityIndicator.setVisibility(View.VISIBLE);
        DataService.getInstance().completeCategories(categories).then(new DoneCallback<List<Category>>() {
            @Override
            public void onDone(List<Category> result) {
                categoryList.setAdapter(new SimilarCategoriesAdapter(SimilarCategoriesActivity.this, categories));
                activityIndicator.setVisibility(View.GONE);
            }
        });
    }

    public void onHelpPressed(View view) {
        showHelp(view, R.layout.similar_categories_help);
    }

    public void onUseOwnPressed(View view) {
        ResultReceiver receiver = getIntent().getParcelableExtra(DELEGATE);
        Bundle resultData = new Bundle();
        resultData.putBoolean("useOwn", true);
        receiver.send(Activity.RESULT_OK, resultData);
        finish();
    }
}