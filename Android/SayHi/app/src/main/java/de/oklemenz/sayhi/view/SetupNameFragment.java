package de.oklemenz.sayhi.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.TextView;

import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.activity.WelcomeActivity;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.UserData;

/**
 * Created by Oliver Klemenz on 09.03.17.
 */

public class SetupNameFragment extends Fragment {

    public ImageButton setupTestButton;
    public de.oklemenz.sayhi.view.EditText nameEditText;

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle) {
        View view = paramLayoutInflater.inflate(R.layout.setup_name, paramViewGroup, false);

        setupTestButton = (ImageButton) view.findViewById(R.id.setupTestButton);
        nameEditText = (de.oklemenz.sayhi.view.EditText) view.findViewById(R.id.setupNameEditText);
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                UserData.getInstance().setFirstName(s.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        nameEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                ((WelcomeActivity) getActivity()).moveNameUp();
                return false;
            }
        });
        nameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    ((WelcomeActivity) getActivity()).hideKeyboard();
                    return true;
                }
                return false;
            }
        });
        nameEditText.setKeyImeChangeListener(new de.oklemenz.sayhi.view.EditText.KeyImeChange() {
            @Override
            public void onKeyIme(int keyCode, KeyEvent event) {
                if (KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
                    ((WelcomeActivity) getActivity()).moveNameDown();
                }
            }
        });

        nameEditText.setText(UserData.getInstance().getFirstName());
        nameEditText.requestFocus();

        BaseActivity.applyColors(this.getContext(), view);

        return view;
    }
}
