package de.oklemenz.sayhi.view;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.shawnlin.numberpicker.NumberPicker;

import java.util.Calendar;

import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.UserData;

import static de.oklemenz.sayhi.model.UserData.BaseYear;

/**
 * Created by Oliver Klemenz on 09.03.17.
 */

public class SetupBirthYearFragment extends Fragment {

    private int year = Calendar.getInstance().get(Calendar.YEAR);

    public NumberPicker setupBirthYearPicker;

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle) {
        View view = paramLayoutInflater.inflate(R.layout.setup_birth_year, paramViewGroup, false);

        setupBirthYearPicker = (NumberPicker) view.findViewById(R.id.setupBirthYearPicker);
        String[] birthYearDisplayValues = new String[year - BaseYear + 1];
        for (int i = 0; i < birthYearDisplayValues.length; i++) {
            birthYearDisplayValues[i] = Integer.toString(year - i);
        }
        setupBirthYearPicker.setMinValue(BaseYear);
        setupBirthYearPicker.setMaxValue(year);
        setupBirthYearPicker.setDisplayedValues(birthYearDisplayValues);
        setupBirthYearPicker.setWrapSelectorWheel(false);
        if (UserData.getInstance().getBirthYear() >= BaseYear) {
            setupBirthYearPicker.setValue(BaseYear + (year - UserData.getInstance().getBirthYear()));
        } else {
            setupBirthYearPicker.setValue(BaseYear + 30);
        }
        setupBirthYearPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                UserData.getInstance().setBirthYear(year - (newVal - BaseYear));
            }
        });

        BaseActivity.applyColors(this.getContext(), view);

        return view;
    }
}