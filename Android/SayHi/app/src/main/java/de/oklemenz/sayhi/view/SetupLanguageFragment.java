package de.oklemenz.sayhi.view;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ToggleButton;

import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.activity.WelcomeActivity;
import de.oklemenz.sayhi.base.BaseActivity;

/**
 * Created by Oliver Klemenz on 09.03.17.
 */

public class SetupLanguageFragment extends Fragment {

    public Button setupLanguageOther;
    public ToggleButton setupLanguageEnglish;
    public ToggleButton setupLanguageGerman;

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle) {
        View view = paramLayoutInflater.inflate(R.layout.setup_language, paramViewGroup, false);

        setupLanguageOther = (Button) view.findViewById(R.id.setupLanguageOther);
        setupLanguageEnglish = (ToggleButton) view.findViewById(R.id.setupLanguageEnglish);
        setupLanguageGerman = (ToggleButton) view.findViewById(R.id.setupLanguageGerman);

        ((WelcomeActivity) getActivity()).changeLanguage();

        BaseActivity.applyColors(this.getContext(), view);

        return view;
    }
}
