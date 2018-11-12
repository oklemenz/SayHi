package de.oklemenz.sayhi.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.shawnlin.numberpicker.NumberPicker;

import org.jdeferred.DoneCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.Bind;
import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.Enum;
import de.oklemenz.sayhi.model.Settings;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.Analytics;
import de.oklemenz.sayhi.service.Crypto;
import de.oklemenz.sayhi.service.DataService;
import de.oklemenz.sayhi.service.Emoji;
import de.oklemenz.sayhi.service.Mail;
import de.oklemenz.sayhi.service.NotificationCenter;
import de.oklemenz.sayhi.service.SecureStore;
import de.oklemenz.sayhi.service.Utilities;
import de.oklemenz.sayhi.service.Voice;

import static de.oklemenz.sayhi.AppDelegate.UserDataLangChangedNotification;
import static de.oklemenz.sayhi.model.UserData.BaseYear;
import static de.oklemenz.sayhi.service.SecureStore.StandardSpace;

/**
 * Created by Oliver Klemenz on 03.11.16.
 */

public class SettingsActivity extends BaseActivity {

    private static int PickerCellHeight = 120;
    private static long PickerCellAnimationDuration = 250;

    @Bind(R.id.nameEditText)
    EditText nameEditText;

    @Bind(R.id.birthYearPickerCell)
    TableRow birthYearPickerCell;
    @Bind(R.id.birthYearPickerContainer)
    LinearLayout birthYearPickerContainer;
    @Bind(R.id.birthYearPicker)
    NumberPicker birthYearPicker;
    @Bind(R.id.birthYearDetailLabel)
    TextView birthYearDetailLabel;

    @Bind(R.id.genderNA)
    ToggleButton genderNA;
    @Bind(R.id.setupGenderMale)
    ToggleButton genderMale;
    @Bind(R.id.setupGenderFemale)
    ToggleButton genderFemale;

    @Bind(R.id.languageValueLabel)
    TextView contentLanguageLabel;
    @Bind(R.id.languageDetailButton)
    ImageButton contentLanguageDetailButton;

    @Bind(R.id.statusEditText)
    EditText statusEditText;

    @Bind(R.id.matchVibrateSwitch)
    Switch matchVibrateSwitch;

    @Bind(R.id.matchGreetingPlaySwitch)
    Switch matchGreetingPlaySwitch;

    @Bind(R.id.matchRecordGreetingValueLabel)
    TextView matchRecordGreetingValueLabel;

    @Bind(R.id.matchHandshakeSwitch)
    Switch matchHandshakeSwitch;

    @Bind(R.id.matchModePickerCell)
    TableRow matchModePickerCell;
    @Bind(R.id.matchModePickerContainer)
    LinearLayout matchModePickerContainer;
    @Bind(R.id.matchModePicker)
    NumberPicker matchModePicker;
    @Bind(R.id.matchModeLabel)
    TextView matchModeLabel;
    @Bind(R.id.matchModeDetailLabel)
    TextView matchModeDetailLabel;

    @Bind(R.id.modeHint)
    TextView matchModeHint;

    @Bind(R.id.spaceValueLabel)
    TextView spaceValueLabel;

    @Bind(R.id.fingerprintSwitch)
    Switch fingerprintSwitch;

    @Bind(R.id.requestAuthPickerCell)
    TableRow requestAuthPickerCell;
    @Bind(R.id.requestAuthPickerContainer)
    LinearLayout requestAuthPickerContainer;
    @Bind(R.id.requestAuthPicker)
    NumberPicker requestAuthPicker;
    @Bind(R.id.requestAuthDetailLabel)
    TextView requestAuthDetailLabel;

    private String[] matchingDescriptions = {};

    private int year = Calendar.getInstance().get(Calendar.YEAR);

    private boolean birthYearPickerViewCellVisible = false;
    private boolean matchModePickerViewCellVisible = false;
    private boolean requestAuthPickerViewCellVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        updateContent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Voice.getInstance().clearSpeak();
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideKeyboard();
        UserData.getInstance().touch(null);
    }

    private void updateContent() {
        matchingDescriptions = new String[]{
                this.getTermString("MatchBasic", Emoji.getLike(Emoji.Size.None)),
                this.getTermString("MatchExact", Emoji.getLike(Emoji.Size.None), Emoji.getDislike(Emoji.Size.None)),
                this.getTermString("MatchAdapt", Emoji.getLike(Emoji.Size.None), Emoji.getDislike(Emoji.Size.None)),
                this.getTermString("MatchTry", Emoji.getDislike(Emoji.Size.None), Emoji.getLike(Emoji.Size.None)),
                this.getTermString("MatchOpen", Emoji.getLike(Emoji.Size.None), Emoji.getDislike(Emoji.Size.None))
        };

        nameEditText.setText(UserData.getInstance().getFirstName());
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

        updateBirthYear();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            birthYearPickerCell.setBackground(null);
        }

        String[] birthYearDisplayValues = new String[year - BaseYear + 1];
        for (int i = 0; i < birthYearDisplayValues.length; i++) {
            birthYearDisplayValues[i] = Integer.toString(year - i);
        }
        birthYearPicker.setMinValue(BaseYear);
        birthYearPicker.setMaxValue(year);
        birthYearPicker.setDisplayedValues(birthYearDisplayValues);
        birthYearPicker.setWrapSelectorWheel(false);
        if (UserData.getInstance().getBirthYear() >= BaseYear) {
            birthYearPicker.setValue(BaseYear + (year - UserData.getInstance().getBirthYear()));
        } else {
            birthYearPicker.setValue(BaseYear + 30);
        }
        birthYearPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                hideKeyboard();
                UserData.getInstance().setBirthYear(year - (newVal - BaseYear));
                updateBirthYear();
            }
        });

        updateGenderSegmentedControl();

        contentLanguageLabel.setText(new Locale(UserData.getInstance().getLangCode()).getDisplayLanguage());
        if (Settings.getInstance().getDisableSettingsLanguage()) {
            contentLanguageDetailButton.getLayoutParams().width = Utilities.convertDpToPx(this, 10);
            contentLanguageDetailButton.setImageBitmap(null);
        }

        statusEditText.setText(UserData.getInstance().getStatus());
        statusEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                UserData.getInstance().setStatus(s.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        updateMatchMode();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            matchModePickerCell.setBackground(null);
        }

        matchModeLabel.setText(Utilities.toSpan(String.format(this.getString(R.string.ModeIcon), Emoji.getMatchingMode(Emoji.Size.Large))));

        String[] matchModeDisplayValues = new String[Enum.MatchMode.values().length];
        for (int i = 0; i < matchModeDisplayValues.length; i++) {
            Enum.MatchMode matchMode = Enum.MatchMode.values()[i];
            matchModeDisplayValues[i] = this.getString(Utilities.getStringResourceId(this, matchMode.toNumberedString()));
        }
        matchModePicker.setMinValue(1);
        matchModePicker.setMaxValue(Enum.MatchMode.values().length);
        matchModePicker.setDisplayedValues(matchModeDisplayValues);
        matchModePicker.setWrapSelectorWheel(false);
        matchModePicker.setValue(UserData.getInstance().getMatchMode().code);
        matchModePicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                hideKeyboard();
                UserData.getInstance().setMatchMode(Enum.MatchMode.fromCode(newVal));
                updateMatchMode();
            }
        });

        if (Utilities.vibrateEnabled(this)) {
            matchVibrateSwitch.setChecked(UserData.getInstance().matchVibrate);
        } else {
            matchVibrateSwitch.setChecked(false);
            matchVibrateSwitch.setEnabled(false);
        }
        matchVibrateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                hideKeyboard();
                UserData.getInstance().matchVibrate = isChecked;
            }
        });

        matchGreetingPlaySwitch.setChecked(UserData.getInstance().matchPlayGreeting);
        matchGreetingPlaySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                hideKeyboard();
                UserData.getInstance().matchPlayGreeting = isChecked;
            }
        });

        matchHandshakeSwitch.setChecked(UserData.getInstance().getMatchHandshake());
        matchHandshakeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                hideKeyboard();
                UserData.getInstance().setMatchHandshake(isChecked);
            }
        });
        if (Settings.getInstance().getDisableSettingsHandshake() != null) {
            if (Settings.getInstance().getDisableSettingsHandshake()) {
                matchHandshakeSwitch.setEnabled(false);
            }
        }

        updateRecordMatchGreeting();

        if (SecureStore.getSpace().equals(StandardSpace)) {
            spaceValueLabel.setText(SecureStore.getStandardSpaceName(this));
        } else {
            spaceValueLabel.setText(SecureStore.getSpace());
        }

        fingerprintSwitch.setChecked(UserData.getInstance().getFingerprint());
        if (!Utilities.authEnabled(this)) {
            fingerprintSwitch.setChecked(false);
            fingerprintSwitch.setEnabled(false);
        }
        fingerprintSwitch.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                view.setTag(true);
                return false;
            }
        });
        fingerprintSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.getTag() instanceof Boolean) {
                    buttonView.setTag(null);
                    hideKeyboard();
                    UserData.getInstance().setFingerprint(isChecked);
                }
            }
        });

        updateRequestAuth();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            requestAuthPickerCell.setBackground(null);
        }

        String[] requestAuthDisplayValues = new String[Enum.PasscodeTimeout.values().length];
        for (int i = 0; i < requestAuthDisplayValues.length; i++) {
            requestAuthDisplayValues[i] = this.getString(Utilities.getStringResourceId(this, Enum.PasscodeTimeout.values()[i].toString()));
        }
        requestAuthPicker.setMinValue(0);
        requestAuthPicker.setMaxValue(Enum.PasscodeTimeout.values().length - 1);
        requestAuthPicker.setDisplayedValues(requestAuthDisplayValues);
        requestAuthPicker.setWrapSelectorWheel(false);
        switch (UserData.getInstance().passcodeTimeout) {
            case Min0:
                requestAuthPicker.setValue(0);
                break;
            case Min1:
                requestAuthPicker.setValue(1);
                break;
            case Min5:
                requestAuthPicker.setValue(2);
                break;
            case Min10:
                requestAuthPicker.setValue(3);
                break;
        }
        requestAuthPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                hideKeyboard();
                switch (newVal) {
                    case 0:
                        UserData.getInstance().passcodeTimeout = Enum.PasscodeTimeout.Min0;
                        break;
                    case 1:
                        UserData.getInstance().passcodeTimeout = Enum.PasscodeTimeout.Min1;
                        break;
                    case 2:
                    default:
                        UserData.getInstance().passcodeTimeout = Enum.PasscodeTimeout.Min5;
                        break;
                    case 3:
                        UserData.getInstance().passcodeTimeout = Enum.PasscodeTimeout.Min10;
                        break;
                }
                updateRequestAuth();
            }
        });
    }

    public void updateBirthYear() {
        if (UserData.getInstance().getBirthYear() >= BaseYear) {
            if (UserData.getInstance().getAge() == 1) {
                birthYearDetailLabel.setText(String.format(this.getString(R.string.Year), UserData.getInstance().getBirthYear(), UserData.getInstance().getAge()));
            } else {
                birthYearDetailLabel.setText(String.format(this.getString(R.string.Years), UserData.getInstance().getBirthYear(), UserData.getInstance().getAge()));
            }
        } else {
            birthYearDetailLabel.setText(this.getString(R.string.NA));
        }
    }

    public void updateGenderSegmentedControl() {
        switch (UserData.getInstance().getGender()) {
            case None:
                genderNA.setChecked(true);
                genderMale.setChecked(false);
                genderFemale.setChecked(false);
                break;
            case Male:
                genderNA.setChecked(false);
                genderMale.setChecked(true);
                genderFemale.setChecked(false);
                break;
            case Female:
                genderNA.setChecked(false);
                genderMale.setChecked(false);
                genderFemale.setChecked(true);
                break;
        }
    }

    public void updateMatchMode() {
        matchModeDetailLabel.setText(Utilities.getStringResourceId(this, UserData.getInstance().getMatchMode().toString()));
        String matchModeHint = "";
        switch (UserData.getInstance().getMatchMode()) {
            case Basic:
                matchModeHint = matchingDescriptions[0];
                break;
            case Exact:
                matchModeHint = matchingDescriptions[1];
                break;
            case Adapt:
                matchModeHint = matchingDescriptions[2];
                break;
            case Tries:
                matchModeHint = matchingDescriptions[3];
                break;
            case Open:
                matchModeHint = matchingDescriptions[4];
                break;
        }
        if (!TextUtils.isEmpty(matchModeHint)) {
            matchModeHint += "\n\n";
            this.matchModeHint.getLayoutParams().height = Utilities.convertDpToPx(this, 100);
            this.matchModeHint.setLayoutParams(this.matchModeHint.getLayoutParams());
        } else {
            this.matchModeHint.getLayoutParams().height = Utilities.convertDpToPx(this, 50);
            this.matchModeHint.setLayoutParams(this.matchModeHint.getLayoutParams());
        }
        if (Settings.getInstance().getDisableSettingsMatchMode()) {
            matchModeHint += this.getString(R.string.MatchForce);
        } else {
            matchModeHint += this.getString(R.string.MatchExplanation);
        }
        this.matchModeHint.setText(matchModeHint);
    }

    public void updateRecordMatchGreeting() {
        matchRecordGreetingValueLabel.setText(UserData.getInstance().greetingVoice != null && UserData.getInstance().greetingVoice.length > 0
                ? this.getString(R.string.Recorded) : this.getString(R.string.Synthesized));
    }

    public void updateRequestAuth() {
        requestAuthDetailLabel.setText(Utilities.getStringResourceId(this, UserData.getInstance().passcodeTimeout.toString()));
    }

    public void onLicensesPressed(View view) {
        Intent licensesIntent = new Intent(this, LicenseActivity.class);
        this.startActivity(licensesIntent, TransitionState.SHOW);
    }

    public void onFeedbackPressed(View view) {
        Mail.getInstance().sendSupportMail(this);
    }

    public void onDataPrivacyPressed(View view) {
        Intent dataPrivacyIntent = new Intent(this, DataPrivacyActivity.class);
        this.startActivity(dataPrivacyIntent, TransitionState.SHOW);
    }

    public void onRateAppPressed(View view) {
        hideKeyboard();
        AppDelegate.openAppInStore(this);
        Analytics.getInstance().logRateApp();
    }

    public void onSendMessagePressed(View view) {
        hideKeyboard();
        Mail.getInstance().sendInvitationMessage(this, null);
    }

    public void onWriteMailPressed(View view) {
        hideKeyboard();
        Mail.getInstance().sendInvitationMail(this, null);
    }

    public void onHelpPressed(View view) {
        hideKeyboard();
        showHelp(view, R.layout.settings_help);
    }

    public void onContentLanguageCellPressed(View view) {
        if (Settings.getInstance().getDisableSettingsLanguage()) {
            return;
        }
        hideKeyboard();
        Intent languageIntent = new Intent(this, SettingsLanguageActivity.class);
        languageIntent.putExtra("selectedCode", UserData.getInstance().getLangCode());
        languageIntent.putExtra(SettingsLanguageActivity.DELEGATE, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {
                    String code = resultData.getString("code");
                    UserData.getInstance().setLangCode(code);
                    contentLanguageLabel.setText(new Locale(code).getDisplayLanguage());
                    NotificationCenter.getInstance().post(UserDataLangChangedNotification);
                    AppDelegate.NoTagAlertShown = false;
                }
            }
        });
        this.startActivity(languageIntent, TransitionState.SHOW);
    }

    public void onBirthYearCellPressed(View view) {
        hideKeyboard();
        birthYearPickerViewCellVisible = !birthYearPickerViewCellVisible;
        if (birthYearPickerViewCellVisible) {
            ValueAnimator valueAnimator = ValueAnimator.ofInt(birthYearPickerContainer.getHeight(), Utilities.convertDpToPx(this, PickerCellHeight));
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    birthYearPickerContainer.getLayoutParams().height = (int) animation.getAnimatedValue();
                    birthYearPickerContainer.requestLayout();
                }
            });
            valueAnimator.setDuration(PickerCellAnimationDuration);
            valueAnimator.start();
        } else {
            ValueAnimator valueAnimator = ValueAnimator.ofInt(birthYearPickerContainer.getHeight(), Utilities.convertDpToPx(this, 0));
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    birthYearPickerContainer.getLayoutParams().height = (int) animation.getAnimatedValue();
                    birthYearPickerContainer.requestLayout();
                }
            });
            valueAnimator.setDuration(PickerCellAnimationDuration);
            valueAnimator.start();
        }
    }

    public void onVoiceCellPressed(View view) {
        hideKeyboard();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        TextView titleView = new TextView(this);
        String titleHTML = this.getString(R.string.TagMatching) + "<br><small>" + this.getString(R.string.MatchingVoice) + "</small>";
        titleView.setText(Utilities.toSpan(titleHTML));
        titleView.setGravity(Gravity.START);
        titleView.setTextSize(20);
        titleView.setTextColor(Color.BLACK);
        titleView.setPadding(40, 20, 10, 10);
        titleView.invalidate();

        List<CharSequence> actions = new ArrayList<>();
        actions.add(this.getString(R.string.RecordNew));
        if (UserData.getInstance().greetingVoice != null && UserData.getInstance().greetingVoice.length > 0) {
            actions.add(this.getString(R.string.PlayRecordedVoice));
            actions.add(this.getString(R.string.ClearRecordedVoice));
        } else {
            actions.add(this.getString(R.string.PlaySynthesizedVoice));
        }
        builder.setCustomTitle(titleView)
                .setCancelable(false)
                .setNegativeButton(this.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setItems(actions.toArray(new CharSequence[]{}), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int code) {
                        if (code == 0) {
                            Voice.getInstance().record(SettingsActivity.this,
                                    String.format(SettingsActivity.this.getString(R.string.HiIm), UserData.getInstance().getFirstName()), new Voice.Callback() {
                                        @Override
                                        public void completion(byte[] voiceData) {
                                            if (voiceData != null) {
                                                boolean rerecord = UserData.getInstance().greetingVoice != null && UserData.getInstance().greetingVoice.length > 0;
                                                UserData.getInstance().greetingVoice = voiceData;
                                                updateRecordMatchGreeting();
                                                if (!rerecord) {
                                                    Analytics.getInstance().logRecordVoice();
                                                } else {
                                                    Analytics.getInstance().logRerecordVoice();
                                                }
                                            }
                                        }
                                    });
                        }
                        if (UserData.getInstance().greetingVoice != null && UserData.getInstance().greetingVoice.length > 0) {
                            if (code == 1) {
                                Voice.getInstance().replay(SettingsActivity.this, UserData.getInstance().greetingVoice);
                            } else if (code == 2) {
                                UserData.getInstance().greetingVoice = null;
                                updateRecordMatchGreeting();
                                Analytics.getInstance().logRemoveRecordedVoice();
                            }
                        } else {
                            if (code == 1) {
                                Voice.getInstance().sayHi(AppDelegate.getInstance().getApplicationContext(),
                                        UserData.getInstance().getFirstName(), UserData.getInstance().getGender(), true);
                            }
                        }
                    }
                });

        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }

    public void onRequestAuthCellPressed(View view) {
        hideKeyboard();
        final ScrollView scrollView = (ScrollView) this.findViewById(R.id.scrollView);
        requestAuthPickerViewCellVisible = !requestAuthPickerViewCellVisible;
        if (requestAuthPickerViewCellVisible) {
            ValueAnimator valueAnimator = ValueAnimator.ofInt(requestAuthPickerContainer.getHeight(), Utilities.convertDpToPx(this, PickerCellHeight));
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    requestAuthPickerContainer.getLayoutParams().height = (int) animation.getAnimatedValue();
                    requestAuthPickerContainer.requestLayout();
                }
            });
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            final Rect rect = new Rect(0, 0, requestAuthPickerCell.getWidth(), requestAuthPickerCell.getHeight());
                            requestAuthPickerCell.requestRectangleOnScreen(rect, false);
                        }
                    });
                }
            });
            valueAnimator.setDuration(PickerCellAnimationDuration);
            valueAnimator.start();
        } else {
            ValueAnimator valueAnimator = ValueAnimator.ofInt(requestAuthPickerContainer.getHeight(), Utilities.convertDpToPx(this, 0));
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    requestAuthPickerContainer.getLayoutParams().height = (int) animation.getAnimatedValue();
                    requestAuthPickerContainer.requestLayout();
                }
            });
            valueAnimator.setDuration(PickerCellAnimationDuration);
            valueAnimator.start();
        }
    }

    public void onGenderPressed(View view) {
        hideKeyboard();
        if (view == genderNA) {
            UserData.getInstance().setGender(Enum.Gender.None);
        } else if (view == genderMale) {
            UserData.getInstance().setGender(Enum.Gender.Male);
        } else if (view == genderFemale) {
            UserData.getInstance().setGender(Enum.Gender.Female);
        }
        updateGenderSegmentedControl();
    }

    public void onMatchModeCellPressed(View view) {
        if (Settings.getInstance().getDisableSettingsMatchMode()) {
            return;
        }
        hideKeyboard();
        final ScrollView scrollView = (ScrollView) this.findViewById(R.id.scrollView);
        matchModePickerViewCellVisible = !matchModePickerViewCellVisible;
        if (matchModePickerViewCellVisible) {
            ValueAnimator valueAnimator = ValueAnimator.ofInt(matchModePickerContainer.getHeight(), Utilities.convertDpToPx(this, PickerCellHeight));
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    matchModePickerContainer.getLayoutParams().height = (int) animation.getAnimatedValue();
                    matchModePickerContainer.requestLayout();
                }
            });
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            final Rect rect = new Rect(0, 0, matchModePickerCell.getWidth(), matchModePickerCell.getHeight());
                            matchModePickerCell.requestRectangleOnScreen(rect, false);
                        }
                    });
                }
            });
            valueAnimator.setDuration(PickerCellAnimationDuration);
            valueAnimator.start();
        } else {
            ValueAnimator valueAnimator = ValueAnimator.ofInt(matchModePickerContainer.getHeight(), Utilities.convertDpToPx(this, 0));
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    matchModePickerContainer.getLayoutParams().height = (int) animation.getAnimatedValue();
                    matchModePickerContainer.requestLayout();
                }
            });
            valueAnimator.setDuration(PickerCellAnimationDuration);
            valueAnimator.start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AppDelegate.getInstance().preventBackgroundProtect = false;
        Analytics.getInstance().logSupportMail();
        Mail.getInstance().stopSendObserver();
    }

    public void onSpaceCellPressed(View view) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setHint(R.string.Name);
        input.post(new Runnable() {
            @Override
            public void run() {
                showKeyboard(input);
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.SpaceSwitch)
                .setMessage(R.string.EnterSpaceName)
                .setCancelable(false)
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final String space = input.getText().toString().trim();
                        final String spaceRefName = space.trim().toLowerCase();
                        if (spaceRefName.equals(SecureStore.getSpaceRefName())) {
                            return;
                        } else if (spaceRefName.equals(StandardSpace.toLowerCase())) {
                            performSpaceSwitch(null);
                        } else {
                            DataService.getInstance().fetchSpaceMeta(spaceRefName).then(new DoneCallback<Map<String, Object>>() {
                                @Override
                                public void onDone(Map<String, Object> meta) {
                                    if (meta != null) {
                                        if ((Boolean) meta.get("protected")) {
                                            final EditText input = new EditText(SettingsActivity.this);
                                            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                                            input.setHint(R.string.AccessCode);
                                            input.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    showKeyboard(input);
                                                }
                                            });
                                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                                    LinearLayout.LayoutParams.MATCH_PARENT);
                                            AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                                            builder.setTitle(R.string.SpaceSwitch)
                                                    .setMessage(R.string.EnterAccessCode)
                                                    .setCancelable(false)
                                                    .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            final String accessCode = input.getText().toString().trim();
                                                            try {
                                                                final String accessCodeHash = Crypto.hash(accessCode);
                                                                DataService.getInstance().verifySpaceProtection(spaceRefName, accessCodeHash).then(new DoneCallback<Boolean>() {
                                                                    @Override
                                                                    public void onDone(Boolean verified) {
                                                                        if (verified) {
                                                                            performSpaceSwitch(space);
                                                                        } else {
                                                                            AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                                                                            builder.setTitle(R.string.SpaceSwitch)
                                                                                    .setMessage(R.string.AccessCodeNotCorrect)
                                                                                    .setCancelable(false)
                                                                                    .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                                                                        public void onClick(DialogInterface dialog, int id) {

                                                                                        }
                                                                                    });
                                                                            final AlertDialog alert = builder.create();
                                                                            alert.setOnShowListener(new DialogInterface.OnShowListener() {
                                                                                @Override
                                                                                public void onShow(DialogInterface dialogInterface) {
                                                                                    alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                                                                                }
                                                                            });
                                                                            alert.show();
                                                                        }
                                                                    }
                                                                });
                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    })
                                                    .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                        }
                                                    });
                                            final AlertDialog alert = builder.create();
                                            alert.setOnShowListener(new DialogInterface.OnShowListener() {
                                                @Override
                                                public void onShow(DialogInterface dialogInterface) {
                                                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                                }
                                            });
                                            input.addTextChangedListener(new TextWatcher() {
                                                @Override
                                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                                }

                                                @Override
                                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!TextUtils.isEmpty(s));
                                                }

                                                @Override
                                                public void afterTextChanged(Editable editable) {
                                                }
                                            });
                                            input.setLayoutParams(layoutParams);
                                            int marginPx = Utilities.convertDpToPx(SettingsActivity.this, 20);
                                            alert.setView(input, marginPx, 0, marginPx, 0);
                                            alert.setOnShowListener(new DialogInterface.OnShowListener() {
                                                @Override
                                                public void onShow(DialogInterface dialogInterface) {
                                                    alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                                                    alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(AppDelegate.AccentColor);
                                                }
                                            });
                                            alert.show();
                                        } else {
                                            performSpaceSwitch(space);
                                        }
                                    }
                                }
                            });
                        }
                    }
                })
                .setNeutralButton(SecureStore.getStandardSpaceName(this), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (SecureStore.getSpaceRefName().equals(StandardSpace.toLowerCase())) {
                            return;
                        }
                        performSpaceSwitch(null);
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!TextUtils.isEmpty(s));
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        input.setLayoutParams(layoutParams);
        int marginPx = Utilities.convertDpToPx(this, 20);
        alert.setView(input, marginPx, 0, marginPx, 0);
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }

    public void performSpaceSwitch(final String space) {
        UserData.getInstance().touch(new UserData.Callback() {
            @Override
            public void error() {

            }

            @Override
            public void cancel() {

            }

            @Override
            public void completion() {
                if (space != null) {
                    SecureStore.switchSpace(space, false);
                    spaceValueLabel.setText(space);
                } else {
                    SecureStore.switchToStandardSpace();
                    spaceValueLabel.setText(SecureStore.getStandardSpaceName(SettingsActivity.this));
                }
                finish();
            }
        });
    }
}