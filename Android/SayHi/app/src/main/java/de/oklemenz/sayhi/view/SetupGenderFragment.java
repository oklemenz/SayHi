package de.oklemenz.sayhi.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.activity.WelcomeActivity;
import de.oklemenz.sayhi.base.BaseActivity;

/**
 * Created by Oliver Klemenz on 09.03.17.
 */

public class SetupGenderFragment extends Fragment {

    public ImageButton setupTestButton;
    public ToggleButton setupGenderMale;
    public ToggleButton setupGenderFemale;

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle) {
        View view = paramLayoutInflater.inflate(R.layout.setup_gender, paramViewGroup, false);

        setupTestButton = (ImageButton) view.findViewById(R.id.setupTestButton);
        setupGenderMale = (ToggleButton) view.findViewById(R.id.setupGenderMale);
        setupGenderFemale = (ToggleButton) view.findViewById(R.id.setupGenderFemale);

        ((WelcomeActivity) getActivity()).changeGender();

        BaseActivity.applyColors(this.getContext(), view);

        return view;
    }
}
