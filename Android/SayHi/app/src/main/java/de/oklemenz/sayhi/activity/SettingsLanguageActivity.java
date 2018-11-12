package de.oklemenz.sayhi.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import butterknife.Bind;
import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.Settings;
import de.oklemenz.sayhi.service.Utilities;
import de.oklemenz.sayhi.view.LanguageAdapter;
import de.oklemenz.sayhi.view.LanguageSectionAdapter;

/**
 * Created by Oliver Klemenz on 15.02.17.
 */

public class SettingsLanguageActivity extends BaseActivity implements View.OnClickListener {

    @Bind(R.id.backButton)
    Button backButton;

    @Bind(R.id.favorite)
    ToggleButton favoriteButton;

    @Bind(R.id.preferred)
    ToggleButton preferredButton;

    @Bind(R.id.all)
    ToggleButton allButton;

    @Bind(R.id.languageList)
    ListView languageList;

    @Bind(R.id.indexLayout)
    LinearLayout indexLayout;

    List<Map<String, String>> favoriteLanguages = new ArrayList<>();
    List<Map<String, String>> preferredLanguages = new ArrayList<>();
    List<Map<String, String>> allLanguages = new ArrayList<>();

    List<String> index = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z");
    Map<String, Integer> sections = new HashMap<>();

    public boolean isPresentedModal = false;
    public String selectedCode;
    public final static String DELEGATE = "delegate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_language);
        updateContent();
    }

    public String backLabel() {
        return this.getString(R.string.SettingsShort);
    }

    private void updateContent() {
        Intent intent = getIntent();
        this.selectedCode = intent.getStringExtra("selectedCode");
        this.isPresentedModal = intent.getBooleanExtra("isPresentedModal", false);
        if (this.isPresentedModal) {
            backButton.setText(R.string.Done);
        }

        for (String favoriteLanguage : Settings.getInstance().getFavoriteLanguages()) {
            Map<String, String> entry = new HashMap<>();
            entry.put("code", favoriteLanguage);
            entry.put("name", new Locale(favoriteLanguage).getDisplayLanguage());
            favoriteLanguages.add(entry);
        }
        Collections.sort(favoriteLanguages, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> lang1, Map<String, String> lang2) {
                return lang1.get("name").compareTo(lang2.get("name"));
            }
        });

        for (String preferredLanguage : Arrays.asList("en", "fr", "es", "zh", "ar", "pt", "ru", "de", "nl", "af", "hi", "bn", "ms", "id", "sw", "fa", "tr", "it", "ta", "jpn")) {
            Map<String, String> entry = new HashMap<>();
            entry.put("code", preferredLanguage);
            entry.put("name", new Locale(preferredLanguage).getDisplayLanguage());
            preferredLanguages.add(entry);
        }
        Collections.sort(preferredLanguages, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> lang1, Map<String, String> lang2) {
                return lang1.get("name").compareTo(lang2.get("name"));
            }
        });

        for (String allLanguage : Locale.getISOLanguages()) {
            Map<String, String> entry = new HashMap<>();
            entry.put("code", allLanguage);
            entry.put("name", new Locale(allLanguage).getDisplayLanguage());
            allLanguages.add(entry);
        }
        Collections.sort(allLanguages, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> lang1, Map<String, String> lang2) {
                return lang1.get("name").compareTo(lang2.get("name"));
            }
        });

        languageList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LanguageAdapter adapter = ((LanguageAdapter) parent.getAdapter());
                Map<String, String> value = adapter.getValue(position);
                if (value != null && value.get("section") == null) {
                    selectedCode = value.get("code");
                    adapter.selectedCode = selectedCode;
                    adapter.notifyDataSetChanged();

                    ResultReceiver receiver = getIntent().getParcelableExtra(DELEGATE);
                    Bundle resultData = new Bundle();
                    resultData.putString("code", selectedCode);
                    receiver.send(Activity.RESULT_OK, resultData);
                    finish();
                }
            }
        });

        showIndex();
        refresh();
    }

    public void onHelpPressed(View view) {
        showHelp(view, R.layout.settings_language_help);
    }

    public void refresh() {
        LanguageAdapter adapter = null;
        List<Map<String, String>> languages = new ArrayList<>();
        if (favoriteButton.isChecked()) {
            for (Map<String, String> entry : favoriteLanguages) {
                languages.add(entry);
            }
            adapter = new LanguageAdapter(this, languages, selectedCode);
        } else if (preferredButton.isChecked()) {
            for (Map<String, String> entry : preferredLanguages) {
                languages.add(entry);
            }
            adapter = new LanguageAdapter(this, languages, selectedCode);
        } else if (allButton.isChecked()) {
            for (Map<String, String> entry : allLanguages) {
                languages.add(entry);
            }
            adapter = new LanguageSectionAdapter(this, languages, selectedCode);
        }
        languageList.setAdapter(adapter);
    }

    private void showIndex() {
        for (String character : index) {
            TextView textView = (TextView) getLayoutInflater().inflate(R.layout.index_item, null);
            textView.setTextColor(AppDelegate.AccentColor);
            textView.setText(character);
            textView.setOnClickListener(this);
            indexLayout.addView(textView);
        }
    }

    private void buildIndex() {
        ListIterator it = ((LanguageSectionAdapter) languageList.getAdapter()).getValues().listIterator();
        while (it.hasNext()) {
            int position = it.nextIndex();
            Map<String, String> value = (Map<String, String>) it.next();
            String name = value.get("name");
            String firstChar = name.substring(0, 1).toUpperCase();
            if (!sections.containsKey(firstChar)) {
                sections.put(firstChar, position);
            }
        }
    }

    public void onClick(View view) {
        TextView selectedIndex = (TextView) view;
        languageList.setSelection(sections.get(selectedIndex.getText()).intValue());
    }

    public void onFavoritePressed(View view) {
        favoriteButton.setChecked(true);
        preferredButton.setChecked(false);
        allButton.setChecked(false);
        indexLayout.setVisibility(View.GONE);
        languageList.setDivider(Utilities.getDrawable(this, R.drawable.standard_list_divider, null));
        languageList.setDividerHeight(Utilities.convertDpToPx(this, 1));
        refresh();
    }

    public void onPreferredPressed(View view) {
        favoriteButton.setChecked(false);
        preferredButton.setChecked(true);
        allButton.setChecked(false);
        indexLayout.setVisibility(View.GONE);
        languageList.setDivider(Utilities.getDrawable(this, R.drawable.standard_list_divider, null));
        languageList.setDividerHeight(Utilities.convertDpToPx(this, 1));
        refresh();
    }

    public void onAllPressed(View view) {
        favoriteButton.setChecked(false);
        preferredButton.setChecked(false);
        allButton.setChecked(true);
        indexLayout.setVisibility(View.VISIBLE);
        languageList.setDivider(Utilities.getDrawable(this, R.drawable.language_list_divider, null));
        languageList.setDividerHeight(Utilities.convertDpToPx(this, 1));
        refresh();
        buildIndex();
    }
}
