package de.oklemenz.sayhi.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;

import butterknife.Bind;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.service.Utilities;

/**
 * Created by Oliver Klemenz on 15.02.17.
 */

public class DataPrivacyActivity extends BaseActivity {

    @Bind(R.id.webView)
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_privacy);
        updateContent();
    }

    public String backLabel() {
        return this.getString(R.string.SettingsShort);
    }

    private void updateContent() {
        int rawResourceId = Utilities.getLocalizedRawResource(this, "data_privacy", R.raw.data_privacy_en);
        webView.loadData(Utilities.readHML(this, rawResourceId), "text/html; charset=UTF-8", null);
        webView.setBackgroundColor(Color.TRANSPARENT);
    }
}