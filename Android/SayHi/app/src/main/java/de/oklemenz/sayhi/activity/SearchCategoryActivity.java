package de.oklemenz.sayhi.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
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
import de.oklemenz.sayhi.model.CategoryQuery;
import de.oklemenz.sayhi.model.NewCategory;
import de.oklemenz.sayhi.model.Settings;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.DataService;
import de.oklemenz.sayhi.service.Utilities;
import de.oklemenz.sayhi.view.SearchCategoryAdapter;

/**
 * Created by Oliver Klemenz on 15.02.17.
 */

public class SearchCategoryActivity extends BaseActivity implements SearchView.OnQueryTextListener {

    @Bind(R.id.helpButton)
    ImageButton helpButton;

    @Bind(R.id.addButton)
    ImageButton addButton;

    @Bind(R.id.titleLabel)
    TextView titleLabel;

    @Bind(R.id.categorySearch)
    SearchView categorySearch;

    @Bind(R.id.activityIndicator)
    ProgressBar activityIndicator;

    @Bind(R.id.categoryList)
    ListView categoryList;

    private String contentLangCode = UserData.getInstance().getLangCode();

    private CategoryQuery query = new CategoryQuery();
    private List<Category> categories = new ArrayList<>();

    private String selectedCategoryKey;
    private Category primaryLangCategory;

    private boolean readOnly = false;

    public final static String DELEGATE = "delegate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_category);

        Intent intent = getIntent();
        if (intent.getStringExtra("contentLangCode") != null) {
            contentLangCode = intent.getStringExtra("contentLangCode");
        }
        query.langCode = contentLangCode;

        if (intent.getStringExtra("selectedCategoryKey") != null) {
            selectedCategoryKey = intent.getStringExtra("selectedCategoryKey");
        }
        primaryLangCategory = (Category) intent.getSerializableExtra("primaryLangCategory");

        readOnly = intent.getBooleanExtra("readOnly", false);
        if (readOnly) {
            makeReadOnly();
        }

        updateTitle();
        updateContent();
        refresh();
    }

    public String backLabel() {
        Intent intent = getIntent();
        return intent.getStringExtra("backLabel");
    }

    private void updateContent() {
        query.search = true;
        query.searchText = "";

        categorySearch.setOnQueryTextListener(this);

        categoryList.setAdapter(new SearchCategoryAdapter(this, categories, selectedCategoryKey));
        categoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SearchCategoryAdapter adapter = ((SearchCategoryAdapter) parent.getAdapter());
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

        if (Settings.getInstance().getDisableNewCategories()) {
            addButton.setVisibility(View.INVISIBLE);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) addButton.getLayoutParams();
            layoutParams.width = 0;
            addButton.setLayoutParams(layoutParams);
        }
    }

    public void onHelpPressed(View view) {
        showHelp(view, R.layout.search_category_help);
    }

    protected Dialog showHelp(View view, View helpView) {
        if (Settings.getInstance().getDisableNewCategories()) {
            helpView.findViewById(R.id.addNewCategoryLabel).setVisibility(View.GONE);
            helpView.findViewById(R.id.selectCategoryArrow).setVisibility(View.GONE);
        }
        return super.showHelp(view, helpView);
    }


    private void updateTitle() {
        String titleHTML = "<b><font color='" + AppDelegate.AccentColor + "'>" + this.getString(R.string.SearchCategory) + "</font></b>";
        titleHTML += "<br/><font color='" + Color.BLACK + "'><small>" + new Locale(contentLangCode).getDisplayLanguage() + "</font></small>";
        titleLabel.setText(Utilities.toSpan(titleHTML));
    }

    public void onAddCategoryPressed(View view) {
        Intent newCategoryIntent = new Intent(this, NewCategoryActivity.class);
        newCategoryIntent.putExtra("contentLangCode", contentLangCode);
        if (primaryLangCategory != null) {
            newCategoryIntent.putExtra("primaryLangCategory", primaryLangCategory);
        }
        newCategoryIntent.putExtra(NewCategoryActivity.DELEGATE, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {
                    NewCategory newCategory = (NewCategory) resultData.getSerializable("newCategory");
                    if (newCategory != null) {
                        selectedCategoryKey = newCategory.key;
                        ((SearchCategoryAdapter) categoryList.getAdapter()).notifyDataSetChanged();
                        ResultReceiver receiver = getIntent().getParcelableExtra(DELEGATE);
                        resultData = new Bundle();
                        resultData.putSerializable("category", newCategory.getCategory());
                        resultData.putSerializable("newCategory", newCategory);
                        receiver.send(Activity.RESULT_OK, resultData);
                        finish();
                    }
                }
            }
        });
        startActivity(newCategoryIntent, TransitionState.MODAL);
    }

    public void makeReadOnly() {
        helpButton.setVisibility(View.GONE);
        addButton.setVisibility(View.GONE);
        readOnly = true;
    }

    public void refresh() {
        fetchCategories();
    }

    private void clearCategories() {
        categories.clear();
        ((SearchCategoryAdapter) categoryList.getAdapter()).notifyDataSetChanged();
    }

    private void fetchCategories() {
        clearCategories();
        activityIndicator.setVisibility(View.VISIBLE);
        DataService.getInstance().fetchCategories(query).then(new DoneCallback<List<Category>>() {
            @Override
            public void onDone(List<Category> categories) {
                SearchCategoryActivity.this.categories = categories;
                categoryList.setAdapter(new SearchCategoryAdapter(SearchCategoryActivity.this, categories, selectedCategoryKey));
                activityIndicator.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onQueryTextSubmit(String searchText) {
        query.searchText = searchText;
        refresh();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String searchText) {
        query.searchText = searchText;
        refresh();
        return true;
    }
}
