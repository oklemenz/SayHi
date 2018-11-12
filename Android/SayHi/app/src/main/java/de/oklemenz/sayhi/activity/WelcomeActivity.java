package de.oklemenz.sayhi.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;

import java.util.Calendar;
import java.util.Locale;

import butterknife.Bind;
import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.Enum;
import de.oklemenz.sayhi.model.Message;
import de.oklemenz.sayhi.model.Profile;
import de.oklemenz.sayhi.model.Settings;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.Crypto;
import de.oklemenz.sayhi.service.DataService;
import de.oklemenz.sayhi.service.Emoji;
import de.oklemenz.sayhi.service.NotificationCenter;
import de.oklemenz.sayhi.service.QRCode;
import de.oklemenz.sayhi.service.SecureStore;
import de.oklemenz.sayhi.service.Utilities;
import de.oklemenz.sayhi.service.Voice;
import de.oklemenz.sayhi.view.SetupBirthYearFragment;
import de.oklemenz.sayhi.view.SetupGenderFragment;
import de.oklemenz.sayhi.view.SetupLanguageFragment;
import de.oklemenz.sayhi.view.SetupNameFragment;
import me.relex.circleindicator.CircleIndicator;

import static android.view.View.GONE;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;
import static de.oklemenz.sayhi.AppDelegate.SeparatorString;
import static de.oklemenz.sayhi.AppDelegate.SettingsFetchedNotification;
import static de.oklemenz.sayhi.AppDelegate.SetupEndNotification;
import static de.oklemenz.sayhi.AppDelegate.ShowContentNotification;
import static de.oklemenz.sayhi.AppDelegate.SpaceSwitchedNotification;
import static de.oklemenz.sayhi.AppDelegate.UserDataChangedNotification;
import static de.oklemenz.sayhi.AppDelegate.UserDataClearedNotification;
import static de.oklemenz.sayhi.AppDelegate.UserDataFetchedNotification;
import static de.oklemenz.sayhi.AppDelegate.UserDataLangChangedNotification;
import static de.oklemenz.sayhi.AppDelegate.UserDataMatchNotification;
import static de.oklemenz.sayhi.model.UserData.BaseYear;

/**
 * Created by Oliver Klemenz on 03.11.16.
 */

public class WelcomeActivity extends BaseActivity implements QRCode.Delegate {

    public interface Callback {
        void completion();
    }

    private static float TransformRatio = 0.28f;
    private static long StayDelay = 500;
    private static long MoveTransition = 750;
    private static long FadeTransition = 500;
    private static float QRTransparent = 0.25f;
    private static float IconSize = 70;

    private int year = Calendar.getInstance().get(Calendar.YEAR);

    @Bind(R.id.settingsButton)
    ImageButton settingsButton;

    @Bind(R.id.scoreLabel)
    TextView scoreLabel;

    @Bind(R.id.helpButton)
    ImageButton helpButton;

    @Bind(R.id.profilesButton)
    Button profilesButton;

    @Bind(R.id.nameButton)
    Button nameButton;

    @Bind(R.id.statusEditText)
    de.oklemenz.sayhi.view.EditText statusEditText;

    @Bind(R.id.logoTextView)
    TextView logoTextView;

    @Bind(R.id.currentQRPreview)
    ImageButton currentQRPreview;

    @Bind(R.id.currentQRPreviewIcon)
    TextView currentQRPreviewIcon;
    @Bind(R.id.currentQRPreviewLogo)
    ImageView currentQRPreviewLogo;

    @Bind(R.id.setupContainerView)
    RelativeLayout setupContainerView;
    @Bind(R.id.setupContainerPageView)
    ViewPager setupContainerPageView;
    @Bind(R.id.setupContainerPageIndicatorView)
    CircleIndicator setupContainerPageIndicatorView;

    @Bind(R.id.setupWelcomeLabel)
    TextView setupWelcomeLabel;
    @Bind(R.id.setupCancelButton)
    Button setupCancelButton;
    @Bind(R.id.setupNextButton)
    Button setupNextButton;

    @Bind(R.id.matchingHistoryButton)
    Button matchingHistoryButton;

    @Bind(R.id.currentProfileButton)
    Button currentProfileButton;

    private boolean introCompleted;

    private SetupNameFragment setupNameFragment;
    private SetupLanguageFragment setupLanguageFragment;
    private SetupGenderFragment setupGenderFragment;
    private SetupBirthYearFragment setupBirthYearFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        AppDelegate.getInstance().initialize();
        updateCurrentProfileContent();
        updateContent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Voice.getInstance().clearSpeak();
    }

    private void updateContent() {
        Intent intent = getIntent();

        statusEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                setStatusState(true);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        statusEditText.setText(UserData.getInstance().getStatus());
                        statusEditText.setSelection(statusEditText.getText().length());
                    }
                }, 250);
                return false;
            }
        });
        statusEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard();
                    storeStatusState();
                    return true;
                }
                return false;
            }
        });
        statusEditText.setKeyImeChangeListener(new de.oklemenz.sayhi.view.EditText.KeyImeChange() {
            @Override
            public void onKeyIme(int keyCode, KeyEvent event) {
                if (KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
                    setStatusText(false);
                    storeStatusState();
                } else if (KeyEvent.KEYCODE_ENTER == event.getKeyCode()) {
                    hideKeyboard();
                    storeStatusState();
                }
            }
        });
        statusEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (statusEditText.isFocusable()) {
                    UserData.getInstance().setStatus(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        if (rootView != null) {
            rootView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    hideKeyboard();
                    storeStatusState();
                    return false;
                }
            });
        }

        NotificationCenter.getInstance().addObserver(ShowContentNotification, this);
        NotificationCenter.getInstance().addObserver(UserDataFetchedNotification, this);
        NotificationCenter.getInstance().addObserver(UserDataChangedNotification, this);
        NotificationCenter.getInstance().addObserver(UserDataClearedNotification, this);
        NotificationCenter.getInstance().addObserver(UserDataMatchNotification, this);
        NotificationCenter.getInstance().addObserver(SpaceSwitchedNotification, this);
        NotificationCenter.getInstance().addObserver(SettingsFetchedNotification, this);
        NotificationCenter.getInstance().addObserver(UserDataLangChangedNotification, this);

        boolean skipIntro = intent.getBooleanExtra("skipIntro", false);
        if (!skipIntro) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    animateLogo();
                    animateText();
                }
            }, StayDelay);
        }

        setupNameFragment = new SetupNameFragment();
        setupLanguageFragment = new SetupLanguageFragment();
        setupGenderFragment = new SetupGenderFragment();
        setupBirthYearFragment = new SetupBirthYearFragment();

        setupContainerPageView.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                    default:
                        return setupNameFragment;
                    case 1:
                        return setupLanguageFragment;
                    case 2:
                        return setupGenderFragment;
                    case 3:
                        return setupBirthYearFragment;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        });
        setupContainerPageView.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                hideKeyboard();
                if (position == 3) {
                    setupNextButton.setText(R.string.Finish);
                } else {
                    setupNextButton.setText(R.string.Next);
                }
            }
        });
        setupContainerPageIndicatorView.setViewPager(setupContainerPageView);
        setupContainerPageIndicatorView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (event.getX() > setupContainerPageIndicatorView.getX() + setupContainerPageIndicatorView.getWidth() / 2.0) {
                        setupNext();
                    } else {
                        setupPrevious();
                    }
                }
                return true;
            }
        });

        settingsButton.setVisibility(GONE);
        helpButton.setVisibility(GONE);
        profilesButton.setVisibility(GONE);

        nameButton.setVisibility(View.INVISIBLE);
        nameButton.setAlpha(0.0f);
        scoreLabel.setVisibility(GONE);
        scoreLabel.setAlpha(0.0f);
        statusEditText.setVisibility(GONE);
        statusEditText.setAlpha(0.0f);
        currentQRPreview.setVisibility(GONE);
        currentQRPreview.setAlpha(0.0f);
        currentQRPreviewIcon.setVisibility(GONE);
        currentQRPreviewIcon.setAlpha(0.0f);
        currentQRPreviewLogo.setVisibility(GONE);
        currentQRPreviewLogo.setAlpha(0.0f);
        matchingHistoryButton.setVisibility(GONE);
        matchingHistoryButton.setAlpha(0.0f);
        setupContainerView.setVisibility(GONE);
        setupContainerView.setAlpha(0.0f);
        currentProfileButton.setVisibility(GONE);
        currentProfileButton.setAlpha(0.0f);

        if (!SecureStore.appInitialized()) {
            nameButton.setEnabled(false);
            getWindow().setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_HIDDEN | SOFT_INPUT_ADJUST_RESIZE);
        }

        final Uri data = intent.getData();
        AppDelegate.getInstance().setQueryUri(data);

        setLogoImage();

        if (skipIntro) {
            showLogo();
        }
    }

    private void storeStatusState() {
        setStatusState(false);
        UserData.getInstance().touch(null);
    }

    private void setStatusState(boolean state) {
        if (state) {
            statusEditText.setFocusable(true);
            statusEditText.setFocusableInTouchMode(true);
        } else {
            statusEditText.setFocusable(false);
        }
    }

    public void notify(String name, NotificationCenter.Notification notification) {
        super.notify(name, notification);
        if (name.equals(ShowContentNotification)) {
            showContentEvent();
        } else if (name.equals(SpaceSwitchedNotification)) {
            spaceSwitched();
        } else if (name.equals(SettingsFetchedNotification)) {
            settingsFetched();
        } else if (name.equals(UserDataLangChangedNotification)) {
            contentLanguageChanged();
        } else {
            handleUserDataEvent();
        }
    }

    private void animateLogo() {
        float nameButtonCenterX = nameButton.getX() + nameButton.getWidth() / 2.0f;
        float nameButtonCenterY = nameButton.getY() + nameButton.getHeight() / 2.0f;
        float logoTextViewCenterX = logoTextView.getX() + logoTextView.getWidth() / 2.0f;
        float logoTextViewCenterY = logoTextView.getY() + logoTextView.getHeight() / 2.0f;
        float logoTextViewWidthScaled = logoTextView.getWidth() * TransformRatio;
        logoTextView.animate().setStartDelay(2 * StayDelay)
                .scaleX(TransformRatio).scaleY(TransformRatio)
                .translationXBy(nameButtonCenterX - logoTextViewCenterX + (nameButton.getWidth() / 2.0f - logoTextViewWidthScaled) - 0.5f)
                .translationYBy(nameButtonCenterY - logoTextViewCenterY)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(MoveTransition);
    }

    private void showLogo() {
        nameButton.setVisibility(View.VISIBLE);
        nameButton.setAlpha(1.0f);
        logoTextView.setVisibility(View.GONE);
        introCompleted = true;
        showContent();
    }

    private void animateText() {
        nameButton.setVisibility(View.VISIBLE);
        nameButton.setAlpha(0.0f);
        nameButton.animate().setStartDelay(3 * StayDelay)
                .alpha(1.0f)
                .setDuration(FadeTransition)
                .setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    logoTextView.setVisibility(GONE);
                    introCompleted = true;
                    showContent();
                }
        });
    }

    private void spaceSwitched() {
        if (!this.isFinishing()) {
            checkSpaceChanged();
        }
    }

    private void settingsFetched() {
        setLogoImage();
        updateScore();
        updateCurrentProfileContent();
    }

    private void contentLanguageChanged() {
        changeLanguage();
    }

    private void updateScore() {
        scoreLabel.setEnabled(!(Settings.getInstance().getDisableHighscoreShare() && Settings.getInstance().getDisableHighscoreShow()));

        if (TextUtils.isEmpty(Settings.getInstance().getHighscoreLocal())) {
            UserData.getInstance().localScore = 0;
            UserData.getInstance().scoreLocalCount = 0;
        } else if (!UserData.getInstance().highscoreLocal.equals(Settings.getInstance().getHighscoreLocal())) {
            UserData.getInstance().localScore = UserData.getInstance().matchScore();
            UserData.getInstance().scoreLocalCount = UserData.getInstance().scoreMatchCount;
        }
        UserData.getInstance().highscoreLocal = Settings.getInstance().getHighscoreLocal();
    }

    private void setLogoImage() {
        RelativeLayout.LayoutParams logoLayoutParams = (RelativeLayout.LayoutParams) currentQRPreviewLogo.getLayoutParams();
        int size = Utilities.convertDpToPx(this, IconSize);
        if (Settings.getInstance().getLogoPlain()) {
            if (Settings.getInstance().getLogoZoom() != 1.0) {
                size = Utilities.convertDpToPx(this, (IconSize - 2 * 5) * Settings.getInstance().getLogoZoom());
            }
            currentQRPreviewIcon.setVisibility(View.GONE);
        } else {
            size = Utilities.convertDpToPx(this, IconSize);
            if (introCompleted && SecureStore.appInitialized()) {
                currentQRPreviewIcon.setVisibility(View.VISIBLE);
            }
        }
        logoLayoutParams.width = size;
        logoLayoutParams.height = size;
        currentQRPreviewLogo.setLayoutParams(logoLayoutParams);
        if (Settings.getInstance().getLogo() != null) {
            currentQRPreviewIcon.setText("");
            currentQRPreviewLogo.setImageBitmap(Settings.getInstance().getLogo());
            if (introCompleted && SecureStore.appInitialized()) {
                currentQRPreviewLogo.setVisibility(View.VISIBLE);
            }
        } else {
            currentQRPreviewLogo.setImageBitmap(null);
            currentQRPreviewLogo.setVisibility(View.INVISIBLE);
            currentQRPreviewIcon.setText(R.string.Hi);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setStatusState(false);
    }

    private void unlockAndStartActivity(final Intent intent) {
        unlockAndStartActivity(intent, TransitionState.SHOW);
    }

    private void unlockAndStartActivity(final Intent intent, final TransitionState transitionState) {
        UserData.getInstance().initialize(new UserData.Callback() {
            @Override
            public void error() {
                AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                builder.setTitle(R.string.ApplicationError)
                        .setMessage(R.string.NoActiveProfile)
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

            @Override
            public void cancel() {
            }

            @Override
            public void completion() {
                if (UserData.getInstance().currentProfile() != null) {
                    if (intent != null) {
                        intent.putExtra("profile", UserData.getInstance().currentProfile().id);
                        startActivity(intent, transitionState);
                    }
                }
            }
        });
    }

    public void onSettingsPressed(View view) {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        unlockAndStartActivity(settingsIntent, TransitionState.MODAL);
    }

    public void onHelpPressed(View view) {
        showHelp(view, R.layout.welcome_help);
    }

    protected Dialog showHelp(View view, View helpView) {
        TextView scoreLabelHelp = (TextView) helpView.findViewById(R.id.scoreLabel);
        scoreLabelHelp.setText(scoreLabel.isEnabled() ? this.getString(R.string.DisplaysMatchingScoreHighscore) : this.getString(R.string.DisplaysMatchingScore));
        return super.showHelp(view, helpView);
    }

    public void onProfilesPressed(View view) {
        Intent profilesIntent = new Intent(this, ProfileActivity.class);
        unlockAndStartActivity(profilesIntent, TransitionState.SHOW);
    }

    public void onMatchHistoryPressed(View view) {
        Intent historyIntent = new Intent(this, HistoryActivity.class);
        unlockAndStartActivity(historyIntent, TransitionState.SHOW);
    }

    public void onCurrentProfilePressed(View view) {
        Intent tagIntent = null;
        if (UserData.getInstance().isInitialized()) {
            tagIntent = new Intent(this, TagActivity.class);
        }
        unlockAndStartActivity(tagIntent, TransitionState.SHOW);
    }

    public void onScorePressed(View view) {
        if (UserData.getInstance().isInitialized()) {
            if ((!Settings.getInstance().getDisableHighscoreShare() && UserData.getInstance().shareScore() > 0) && !Settings.getInstance().getDisableHighscoreShow()) {
                handleShareOrShow();
            } else if (!Settings.getInstance().getDisableHighscoreShare() && UserData.getInstance().shareScore() > 0) {
                shareScore();
            } else if (!Settings.getInstance().getDisableHighscoreShow()) {
                showHighscore();
            }
        }
    }

    private void handleShareOrShow() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.Highscore)
                .setMessage(R.string.ShareOrShowHighscore)
                .setCancelable(false)
                .setPositiveButton(R.string.ShareScore, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        shareScore();
                    }
                })
                .setNegativeButton(R.string.ShowHighscore, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        showHighscore();
                    }
                })
                .setNeutralButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }

    private void shareScore() {
        String alias = UserData.getInstance().getFirstName();
        if (!TextUtils.isEmpty(UserData.getInstance().getStatus())) {
            alias += " (" + UserData.getInstance().getStatus() + ")";
        }
        final String finalAlias = alias;
        final long score = UserData.getInstance().shareScore();
        final long count = UserData.getInstance().scoreShareCount();
        String title = score == 1 ? String.format(this.getString(R.string.ShareMatchingPoint), "" + score) : String.format(this.getString(R.string.ShareMatchingPoints), "" + score);
        String titleSuffix = count == 1 ? String.format(this.getString(R.string.ShareMatchingMatch), "" + count) : String.format(this.getString(R.string.ShareMatchingMatches), "" + count);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setText(alias);
        input.setHint(R.string.Alias);
        input.post(new Runnable() {
            @Override
            public void run() {
                input.setSelection(input.getText().length());
                showKeyboard(input);
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title + " " + titleSuffix)
                .setMessage(R.string.ProvideAliasShare)
                .setCancelable(false)
                .setPositiveButton(R.string.Share, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String alias = input.getText().toString().trim();
                        DataService.getInstance().shareHighscore(alias, score, count).done(new DoneCallback<Void>() {
                            @Override
                            public void onDone(Void result) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                                builder.setTitle(R.string.Highscore)
                                        .setMessage(R.string.ScoreShared)
                                        .setCancelable(false)
                                        .setNegativeButton(R.string.OK, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                            }
                                        });
                                if (!Settings.getInstance().getDisableHighscoreShow()) {
                                    builder.setPositiveButton(R.string.ShowHighscore, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            showHighscore();
                                        }
                                    });
                                }
                                final AlertDialog alert = builder.create();
                                alert.setOnShowListener(new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(DialogInterface dialogInterface) {
                                        alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                                        alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(AppDelegate.AccentColor);
                                    }
                                });
                                alert.show();

                            }
                        }).fail(new FailCallback<Exception>() {
                            @Override
                            public void onFail(Exception result) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                                builder.setTitle(R.string.NetworkError)
                                        .setMessage(R.string.UnexpectedErrorOccurred)
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
                        });
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        input.setLayoutParams(layoutParams);
        int marginPx = Utilities.convertDpToPx(this, 20);
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!TextUtils.isEmpty(finalAlias));
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
        alert.setView(input, marginPx, 0, marginPx, 0);
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                input.getBackground().setColorFilter(AppDelegate.AccentColor, PorterDuff.Mode.SRC_ATOP);
                alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }

    private void showHighscore() {
        Intent highscoreIntent = new Intent(this, HighscoreActivity.class);
        unlockAndStartActivity(highscoreIntent, TransitionState.MODAL);
    }

    public void onQRPressed(View view) {
        Intent qrCodeIntent = new Intent(this, QRCodeActivity.class);
        unlockAndStartActivity(qrCodeIntent, TransitionState.MODAL);
    }

    private void updateCurrentProfileContent() {
        Profile profile = UserData.getInstance().currentProfile();
        if (SecureStore.appInitialized() && profile != null) {
            String scoreHTML = "<b><font color='" + AppDelegate.AccentColor + "'>" + UserData.getInstance().shareScore() + "</font></b><br/>";
            if (UserData.getInstance().shareScore() == 1) {
                scoreHTML += "<small>" + getString(R.string.MatchingPoint) + "</small>";
            } else {
                scoreHTML += "<small>" + getString(R.string.MatchingPoints) + "</small>";
            }
            scoreLabel.setText(Utilities.toSpan(scoreHTML));

            String currentProfileHTML = "<font color='" + Color.WHITE + "'>" + this.getString(R.string.CurrentProfile) + "</font><br/>";
            currentProfileHTML += "<font color='" + AppDelegate.AccentColor + "'>" + profile.name + "</font>";
            String separator = "<br/>";
            if (profile.relationType != Enum.RelationType.None) {
                currentProfileHTML += "<font color='" + Color.BLACK + "'><small>" + separator + Emoji.getRelationType(Emoji.Size.Medium) + this.getString(Utilities.getStringResourceId(this, profile.relationType.toString())) + "</small></font>";
                separator = SeparatorString;
            }
            if (profile.matchMode != null) {
                currentProfileHTML += "<font color='" + Color.BLACK + "'><small>" + separator + Emoji.getMatchingMode(Emoji.Size.Medium) + this.getString(Utilities.getStringResourceId(this, profile.matchMode.toString())) + "</small></font>";
            }
            currentProfileButton.setText(Utilities.toSpan(currentProfileHTML));

            try {
                String qrContent = "http://sayhi-app.com?_=" + Crypto.hash(Message.generate(profile).toString()).substring(0, 20);
                QRCode.getInstance().generate(qrContent, Utilities.convertDpToPx(this, 250), ErrorCorrectionLevel.Q, this);
            } catch (Exception e) {
                e.printStackTrace();
            }

            setStatusText(false);
            matchingHistoryButton.setText(String.format(this.getString(R.string.MatchingHistoryNum), UserData.getInstance().scoreMatchCount));

            currentQRPreview.animate().alpha(1.0f).setDuration(2 * FadeTransition);
            currentQRPreviewIcon.setBackgroundResource(R.drawable.icon_shape_active);
            currentQRPreviewIcon.animate().alpha(1.0f).setDuration(2 * FadeTransition);
            currentQRPreviewLogo.animate().alpha(1.0f).setDuration(2 * FadeTransition);
            scoreLabel.animate().alpha(1.0f).setDuration(2 * FadeTransition);
            statusEditText.animate().alpha(1.0f).setDuration(2 * FadeTransition);
            matchingHistoryButton.animate().alpha(1.0f).setDuration(2 * FadeTransition);
        } else {
            scoreLabel.setText("");

            if (SecureStore.appInitialized() && !UserData.getInstance().isInitialized()) {
                String currentProfileHTML = "<font color='" + Color.WHITE + "'>" + this.getString(R.string.ProfileLocked) + "</font><br/>";
                currentProfileHTML += "<font color='" + AppDelegate.AccentColor + "'>" + this.getString(R.string.PressToUnlock) + "</font>";
                currentProfileButton.setText(Utilities.toSpan(currentProfileHTML));
            } else {
                currentProfileButton.setText("");
            }

            QRCode.getInstance().generate(AppDelegate.AppName, Utilities.convertDpToPx(this, 250), ErrorCorrectionLevel.L, this);

            setStatusText(true);
            matchingHistoryButton.setText(R.string.MatchingHistory);

            currentQRPreview.animate().alpha(QRTransparent).setDuration(2 * FadeTransition);
            currentQRPreviewIcon.setBackgroundResource(R.drawable.icon_shape);
            currentQRPreviewIcon.animate().alpha(QRTransparent).setDuration(2 * FadeTransition);
            currentQRPreviewLogo.animate().alpha(QRTransparent).setDuration(2 * FadeTransition);
            scoreLabel.animate().alpha(0.0f).setDuration(2 * FadeTransition);
            statusEditText.animate().alpha(0.0f).setDuration(2 * FadeTransition);
            matchingHistoryButton.animate().alpha(1.0f).setDuration(2 * FadeTransition);
        }
    }

    public void updateColors() {
        super.updateColors();
        setupWelcomeLabel.setTextColor(AppDelegate.AccentColor);
        setupCancelButton.setTextColor(AppDelegate.AccentColor);
        setupNextButton.setTextColor(AppDelegate.AccentColor);
        updateCurrentProfileContent();
    }

    private void checkConfigurationChanged() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkConfigurationAdjusted();
            }
        }, 1000);
    }

    public void checkConfigurationAdjusted() {
        if (!AppDelegate.getInstance().configurationAdjusted) {
            return;
        }
        if (SecureStore.appInitialized()) {
            UserData.getInstance().touch(null);
            if (AppDelegate.getInstance().returnToHome()) {
                return;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.SayHi)
                .setMessage(R.string.ConfigurationAdjusted)
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
        if (!this.isFinishing()) {
            alert.show();
        }
        AppDelegate.getInstance().configurationAdjusted = false;
    }

    private void checkSpaceChanged() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(AppDelegate.getInstance().Context);
                builder.setTitle(R.string.SpaceSwitch)
                        .setMessage(R.string.SpaceSwitchedSuccessfully)
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
                if (!AppDelegate.getInstance().Context.isFinishing()) {
                    alert.show();
                }
            }
        }, 2000);
    }

    private void showContentEvent() {
        if (introCompleted) {
            showContent();
        }
    }

    private void showContent() {
        checkConfigurationChanged();
        if (SecureStore.appInitialized()) {
            settingsButton.setVisibility(View.VISIBLE);
            helpButton.setVisibility(View.VISIBLE);
            profilesButton.setVisibility(View.VISIBLE);
            if (UserData.getInstance().isInitialized()) {
                showActiveContent(null);
            } else if (!SecureStore.fingerprintUsed()) {
                UserData.getInstance().initialize(new UserData.Callback() {
                    @Override
                    public void error() {
                    }

                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void completion() {
                        showActiveContent(null);
                    }
                });
            } else if (UserData.getInstance().requestInitialize) {
                showInactiveContent(new Callback() {
                    @Override
                    public void completion() {
                        showActiveContent(null);
                    }
                });
            } else {
                showInactiveContent(null);
            }
        } else {
            showSetupContent();
        }
    }

    private void showActiveContent(final Callback callback) {
        updateCurrentProfileContent();
        currentQRPreview.setVisibility(View.VISIBLE);
        currentQRPreview.setAlpha(0.0f);
        currentQRPreview.animate().alpha(1.0f).setStartDelay(StayDelay).setDuration(FadeTransition).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (callback != null) {
                    callback.completion();
                }
            }
        });
        currentQRPreviewIcon.setVisibility(!Settings.getInstance().getLogoPlain() ? View.VISIBLE : View.GONE);
        currentQRPreviewIcon.setAlpha(0.0f);
        currentQRPreviewIcon.animate().alpha(1.0f).setStartDelay(StayDelay).setDuration(FadeTransition);
        currentQRPreviewLogo.setVisibility(Settings.getInstance().getLogo() != null ? View.VISIBLE : View.GONE);
        currentQRPreviewLogo.setAlpha(0.0f);
        currentQRPreviewLogo.animate().alpha(1.0f).setStartDelay(StayDelay).setDuration(FadeTransition);
        scoreLabel.setVisibility(View.VISIBLE);
        scoreLabel.setAlpha(0.0f);
        scoreLabel.animate().alpha(1.0f).setStartDelay(StayDelay).setDuration(FadeTransition);
        statusEditText.setVisibility(View.VISIBLE);
        statusEditText.setAlpha(0.0f);
        statusEditText.animate().alpha(1.0f).setStartDelay(StayDelay).setDuration(FadeTransition);
        matchingHistoryButton.setVisibility(View.VISIBLE);
        matchingHistoryButton.setAlpha(0.0f);
        matchingHistoryButton.animate().alpha(1.0f).setStartDelay(StayDelay).setDuration(FadeTransition);
        currentProfileButton.setVisibility(View.VISIBLE);
        currentProfileButton.setAlpha(0.0f);
        currentProfileButton.animate().alpha(1.0f).setStartDelay(StayDelay).setDuration(FadeTransition);
    }

    private void showInactiveContent(final Callback callback) {
        currentQRPreview.setVisibility(View.VISIBLE);
        currentQRPreview.setAlpha(0.0f);
        currentQRPreview.animate().alpha(QRTransparent).setStartDelay(StayDelay).setDuration(FadeTransition).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (callback != null) {
                    callback.completion();
                }
            }
        });
        currentQRPreviewIcon.setVisibility(!Settings.getInstance().getLogoPlain() ? View.VISIBLE : View.GONE);
        currentQRPreviewIcon.setAlpha(0.0f);
        currentQRPreviewIcon.animate().alpha(QRTransparent).setStartDelay(StayDelay).setDuration(FadeTransition);
        currentQRPreviewLogo.setVisibility(Settings.getInstance().getLogo() != null ? View.VISIBLE : View.GONE);
        currentQRPreviewLogo.setAlpha(0.0f);
        currentQRPreviewLogo.animate().alpha(QRTransparent).setStartDelay(StayDelay).setDuration(FadeTransition);
        scoreLabel.setVisibility(View.VISIBLE);
        scoreLabel.setAlpha(0.0f);
        scoreLabel.animate().alpha(0.0f).setStartDelay(StayDelay).setDuration(FadeTransition);
        statusEditText.setVisibility(View.VISIBLE);
        statusEditText.setAlpha(0.0f);
        statusEditText.animate().alpha(0.0f).setStartDelay(StayDelay).setDuration(FadeTransition).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                statusEditText.setVisibility(View.GONE);
            }
        });
        matchingHistoryButton.setVisibility(View.VISIBLE);
        matchingHistoryButton.setAlpha(0.0f);
        matchingHistoryButton.animate().alpha(1.0f).setStartDelay(StayDelay).setDuration(FadeTransition);
        currentProfileButton.setVisibility(View.VISIBLE);
        currentProfileButton.setAlpha(0.0f);
        currentProfileButton.animate().alpha(1.0f).setStartDelay(StayDelay).setDuration(FadeTransition);
    }

    private void showSetupContent() {
        setupContainerView.setVisibility(View.VISIBLE);
        setupContainerView.setAlpha(0.0f);
        setupContainerView.animate().setStartDelay(StayDelay).alpha(1.0f).setDuration(FadeTransition).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (setupNameFragment != null && setupNameFragment.nameEditText != null) {
                            showKeyboard(setupNameFragment.nameEditText);
                            moveNameUp();
                        }
                    }
                }, StayDelay);
            }
        });
    }

    public void moveNameUp() {
        ((RelativeLayout.LayoutParams) nameButton.getLayoutParams()).topMargin = Utilities.convertDpToPx(WelcomeActivity.this, -20);
    }

    public void moveNameDown() {
        ((RelativeLayout.LayoutParams) nameButton.getLayoutParams()).topMargin = Utilities.convertDpToPx(WelcomeActivity.this, 50);
    }

    private void setupEnd() {
        hideKeyboard();
        getWindow().setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_HIDDEN | SOFT_INPUT_ADJUST_NOTHING);
        UserData.getInstance().setInitialized(true);
        UserData.getInstance().touch(new UserData.Callback() {
            @Override
            public void error() {
            }

            @Override
            public void cancel() {
            }

            @Override
            public void completion() {
                showContent();
                setupContainerView.animate().alpha(0.0f).setDuration(FadeTransition).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        nameButton.setEnabled(true);
                        setupContainerView.setVisibility(View.GONE);
                        settingsButton.setVisibility(View.VISIBLE);
                        helpButton.setVisibility(View.VISIBLE);
                        profilesButton.setVisibility(View.VISIBLE);
                    }
                });
                NotificationCenter.getInstance().post(SetupEndNotification);
            }
        });
    }

    private void handleUserDataEvent() {
        if (introCompleted) {
            checkConfigurationChanged();
            updateCurrentProfileContent();
        }
    }

    private void setStatusText(boolean forceEmpty) {
        if (!TextUtils.isEmpty(UserData.getInstance().getStatus()) && !forceEmpty) {
            statusEditText.setText(String.format(this.getString(R.string.MarksText), UserData.getInstance().getStatus()));
        } else {
            statusEditText.setText(this.getString(R.string.Marks));
        }
    }

    public void generationDone(Bitmap bitmap) {
        currentQRPreview.setImageBitmap(bitmap);
    }

    public void onSetupTestPressed(View view) {
        Voice.getInstance().sayHi(this,
                UserData.getInstance().getFirstName(), UserData.getInstance().getGender(), true);
    }

    public void onSetupGenderMalePressed(View view) {
        UserData.getInstance().setGender(Enum.Gender.Male);
        changeGender();
    }

    public void onSetupGenderFemalePressed(View view) {
        UserData.getInstance().setGender(Enum.Gender.Female);
        changeGender();
    }

    public void changeGender() {
        if (setupGenderFragment != null) {
            if (setupGenderFragment.setupGenderMale != null) {
                setupGenderFragment.setupGenderMale.setChecked(UserData.getInstance().getGender() == Enum.Gender.Male);
            }
            if (setupGenderFragment.setupGenderFemale != null) {
                setupGenderFragment.setupGenderFemale.setChecked(UserData.getInstance().getGender() == Enum.Gender.Female);
            }
        }
    }

    public void onSetupEnglishLanguagePressed(View view) {
        UserData.getInstance().setLangCode("en");
        NotificationCenter.getInstance().post(UserDataLangChangedNotification);
    }

    public void onSetupGermanLanguagePressed(View view) {
        UserData.getInstance().setLangCode("de");
        NotificationCenter.getInstance().post(UserDataLangChangedNotification);
    }

    public void onSetupOtherLanguagePressed(View view) {
        Intent languageIntent = new Intent(this, SettingsLanguageActivity.class);
        languageIntent.putExtra("selectedCode", UserData.getInstance().getLangCode());
        languageIntent.putExtra("isPresentedModal", true);
        languageIntent.putExtra(SettingsLanguageActivity.DELEGATE, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {
                    String code = resultData.getString("code");
                    UserData.getInstance().setLangCode(code);
                    NotificationCenter.getInstance().post(UserDataLangChangedNotification);
                }
            }
        });
        this.startActivity(languageIntent, TransitionState.MODAL);
    }

    public void changeLanguage() {
        if (setupLanguageFragment != null) {
            if (setupLanguageFragment.setupLanguageEnglish != null) {
                setupLanguageFragment.setupLanguageEnglish.setChecked(UserData.getInstance().getLangCode().equals("en"));
            }
            if (setupLanguageFragment.setupLanguageGerman != null) {
                setupLanguageFragment.setupLanguageGerman.setChecked(UserData.getInstance().getLangCode().equals("de"));
            }
            if (setupLanguageFragment.setupLanguageOther != null) {
                if (!AppDelegate.BundleLangCodes.contains(UserData.getInstance().getLangCode())) {
                    setupLanguageFragment.setupLanguageOther.setText(new Locale(UserData.getInstance().getLangCode()).getDisplayLanguage());
                } else {
                    setupLanguageFragment.setupLanguageOther.setText(R.string.OtherLang);
                }
            }
        }
    }

    public void onSetupCancelPressed(View view) {
        final boolean nameEditTextHasFocus = setupNameFragment != null && setupNameFragment.nameEditText != null && setupNameFragment.nameEditText.hasFocus();
        hideKeyboard();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.FirstTimeConfiguration)
                .setMessage(R.string.SettingsConfiguration)
                .setCancelable(false)
                .setPositiveButton(R.string.Back, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (setupNameFragment != null && setupNameFragment.nameEditText != null && nameEditTextHasFocus) {
                            setupNameFragment.nameEditText.requestFocus();
                        }
                    }
                })
                .setNegativeButton(R.string.OK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setupEnd();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }

    public void onSetupNextPressed(View view) {
        if (setupContainerPageView.getCurrentItem() < setupContainerPageView.getChildCount()) {
            setupNext();
        } else {
            if (setupBirthYearFragment != null && setupBirthYearFragment.setupBirthYearPicker != null) {
                UserData.getInstance().setBirthYear(year - (setupBirthYearFragment.setupBirthYearPicker.getValue() - BaseYear));
            }
            setupEnd();
        }
    }

    private void setupPrevious() {
        if (setupContainerPageView.getCurrentItem() > 0) {
            setupContainerPageView.setCurrentItem(setupContainerPageView.getCurrentItem() - 1, true);
        }
    }

    private void setupNext() {
        if (setupContainerPageView.getCurrentItem() < setupContainerPageView.getChildCount()) {
            setupContainerPageView.setCurrentItem(setupContainerPageView.getCurrentItem() + 1, true);
        }
    }

    @Override
    public void hideKeyboard() {
        super.hideKeyboard();
        setStatusText(false);
        moveNameDown();
    }
}