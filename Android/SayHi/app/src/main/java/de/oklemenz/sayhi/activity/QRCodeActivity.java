package de.oklemenz.sayhi.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.jdeferred.AlwaysCallback;
import org.jdeferred.Deferred;
import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.Bind;
import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.Match;
import de.oklemenz.sayhi.model.MatchStatus;
import de.oklemenz.sayhi.model.Message;
import de.oklemenz.sayhi.model.Profile;
import de.oklemenz.sayhi.model.QRContent;
import de.oklemenz.sayhi.model.Settings;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.AESCrypt;
import de.oklemenz.sayhi.service.Analytics;
import de.oklemenz.sayhi.service.Crypto;
import de.oklemenz.sayhi.service.DataService;
import de.oklemenz.sayhi.service.NotificationCenter;
import de.oklemenz.sayhi.service.QRCode;
import de.oklemenz.sayhi.service.SecureStore;
import de.oklemenz.sayhi.service.Utilities;
import de.oklemenz.sayhi.service.Voice;
import de.oklemenz.sayhi.view.QRCodeHelp;

import static de.oklemenz.sayhi.AppDelegate.AppSharedAPIKey;
import static de.oklemenz.sayhi.AppDelegate.QRCodeRecognizedNotification;
import static de.oklemenz.sayhi.AppDelegate.UpdateLocationNotification;
import static de.oklemenz.sayhi.AppDelegate.UserDataMatchNotification;

/**
 * Created by Oliver Klemenz on 03.11.16.
 */

public class QRCodeActivity extends BaseActivity {

    private static int DelayGreetingDuration = 1000;
    private static int DelayMatchDuration = 500;

    private static Address address;
    private static Location location;

    public static List<ErrorCorrectionLevel> correctionLevelList = Arrays.asList(
            ErrorCorrectionLevel.L,
            ErrorCorrectionLevel.M,
            ErrorCorrectionLevel.Q,
            ErrorCorrectionLevel.H);

    private String correctionLevelString(ErrorCorrectionLevel correctionLevel) {
        switch (correctionLevel) {
            case L:
                return "L";
            case M:
                return "M";
            case Q:
                return "Q";
            case H:
                return "H";
        }
        return "L";
    }

    @Bind(R.id.previewContainer)
    RelativeLayout previewContainer;
    @Bind(R.id.qrCodeImageView)
    ImageView qrCodeImageView;
    @Bind(R.id.cameraPreviewContainer)
    RelativeLayout cameraPreviewContainer;

    @Bind(R.id.correctionLevelMinus)
    Button correctionLevelMinus;
    @Bind(R.id.correctionLevelPlus)
    Button correctionLevelPlus;
    @Bind(R.id.correctionLevelLabel)
    TextView correctionLevelLabel;

    @Bind(R.id.infoButton)
    ImageButton infoButton;

    @Bind(R.id.tagButton)
    ImageButton tagButton;

    @Bind(R.id.pulseView)
    View pulseView;
    @Bind(R.id.pulseLabel)
    TextView pulseLabel;

    @Bind(R.id.activityIndicator)
    ProgressBar activityIndicator;
    @Bind(R.id.activityLabel)
    TextView activityLabel;

    private QRCodeHelp qrCodeHelp;
    private boolean qrCodeHelpSwipe;
    private float qrCodeHelpRawX;
    private float qrCodeHelpRawY;

    private boolean pulsating = false;
    private boolean stopPulsating = false;

    private String session = "";
    private Map<String, Boolean> sessions = new HashMap<>();
    private String messageKey = "";
    private String matchMessageKey = "";
    private String matchKey = "";
    private boolean matchIsFirst = false;
    private String lastInfoMsgKey = "";

    private Geocoder geocoder;

    private Profile profile;

    private ErrorCorrectionLevel correctionLevel = ErrorCorrectionLevel.L;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qr_code);

        Intent intent = getIntent();
        List<String> hideButtons = intent.getStringArrayListExtra("hideButtons");
        if (hideButtons != null) {
            hideBarButtonItems(hideButtons);
        }
        int profileId = intent.getIntExtra("profile", -1);
        profile = UserData.getInstance().getProfile(profileId);

        updateLabel();
        updateContent();

        NotificationCenter.getInstance().addObserver(UpdateLocationNotification, this);
        AppDelegate.getInstance().initLocation();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void updateContent() {
        clearInfo();
        activityIndicator.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        activityIndicator.setVisibility(View.VISIBLE);

        initSession();
    }

    private void updateLabel() {
        correctionLevelLabel.setText(this.getString(Utilities.getStringResourceId(this, correctionLevelString(correctionLevel))));
    }

    private boolean shouldShowQRHelpFirst() {
        return !UserData.getInstance().qrHelpFirstShown && !Settings.getInstance().getDisableHelpQR();
    }

    private boolean showQRHelpFirst() {
        if (shouldShowQRHelpFirst()) {
            UserData.getInstance().qrHelpFirstShown = true;
            UserData.getInstance().touch(null);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    showHelp(null, R.layout.qr_code_help);
                }
            }, 500);
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        clearInfo();
    }

    public void hideBarButtonItems(List<String> buttons) {
        for (String button : buttons) {
            switch (button) {
                case "tag":
                    tagButton.setVisibility(View.INVISIBLE);
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) tagButton.getLayoutParams();
                    layoutParams.width = 0;
                    tagButton.setLayoutParams(layoutParams);
                    break;
            }
        }
    }

    public void onHelpPressed(View view) {
        showHelp(view, R.layout.qr_code_help);
    }

    protected Dialog showHelp(View view, View helpView) {
        TextView qrCodeRecognitionActive = (TextView) helpView.findViewById(R.id.qrCodeRecognitionActive);
        if (!pulsating) {
            qrCodeRecognitionActive.setText(R.string.QRCodeRecognitionNotActive);
        }
        qrCodeHelp = new QRCodeHelp(helpView, this);
        final Dialog dialog = super.showHelp(view, helpView);
        qrCodeHelp.helpContainerPageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        qrCodeHelpSwipe = false;
                        qrCodeHelpRawX = event.getRawX();
                        qrCodeHelpRawY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.sqrt(Math.pow(event.getRawX() - qrCodeHelpRawX, 2) - Math.pow(event.getRawY() - qrCodeHelpRawY, 2)) > 10) {
                            qrCodeHelpSwipe = true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (!qrCodeHelpSwipe) {
                            dialog.dismiss();
                            return true;
                        }
                        break;
                }
                return false;
            }
        });
        return dialog;
    }

    public void onInfoPressed(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(QRCodeActivity.this);
        builder.setTitle(R.string.QRCodeGeneration)
                .setMessage(this.getString(Utilities.getStringResourceId(this, lastInfoMsgKey)))
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

    public void onMinusPressed(View view) {
        int index = correctionLevelList.indexOf(correctionLevel);
        if (index > 0) {
            index--;
            correctionLevel = correctionLevelList.get(index);
        }
        generateQR();
        checkCorrectionLevel();
        updateLabel();
    }

    public void onPlusPressed(View view) {
        int index = correctionLevelList.indexOf(correctionLevel);
        if (index < correctionLevelList.size() - 1) {
            index++;
            correctionLevel = correctionLevelList.get(index);
        }
        generateQR();
        checkCorrectionLevel();
        updateLabel();
    }

    public void checkCorrectionLevel() {
        int index = correctionLevelList.indexOf(correctionLevel);
        correctionLevelMinus.setEnabled(index > 0);
        correctionLevelMinus.setAlpha(index > 0 ? 1.0f : 0.5f);
        correctionLevelPlus.setEnabled(index < correctionLevelList.size() - 1);
        correctionLevelPlus.setAlpha(index < correctionLevelList.size() - 1 ? 1.0f : 0.5f);
    }

    public void onCurrentProfilePressed(View view) {
        Intent tagIntent = new Intent(this, TagActivity.class);
        tagIntent.putExtra("profile", UserData.getInstance().currentProfileId);
        tagIntent.putExtra("backLabel", this.getString(R.string.Hi));
        tagIntent.putExtra("readOnly", true);
        startActivity(tagIntent, TransitionState.SHOW);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startReadQR();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopReadQR();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroySession();
        Voice.getInstance().clearSpeak();
    }

    private void startPulse() {
        if (!pulsating) {
            pulseShrink();
        }
        pulsating = true;
    }

    private void pulseShrink() {
        if (stopPulsating) {
            return;
        }
        pulseView.animate().scaleX(0.75f).scaleY(0.75f).setDuration(1000).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                pulseGrow();
            }
        });
    }

    private void pulseGrow() {
        if (stopPulsating) {
            return;
        }
        pulseView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(1000).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                pulseShrink();
            }
        });
    }

    @Override
    public void notify(String name, NotificationCenter.Notification notification) {
        super.notify(name, notification);
        if (name.equals(UpdateLocationNotification)) {
            locationUpdated(notification);
        } else if (name.equals(QRCodeRecognizedNotification)) {
            qrCodeRecognized(notification);
        }
    }

    private void locationUpdated(NotificationCenter.Notification notification) {
        final Location location = (Location) notification.userInfo.get("location");
        QRCodeActivity.location = location;
        if (geocoder == null) {
            geocoder = new Geocoder(this, Locale.getDefault());
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    List<Address> addresses = geocoder.getFromLocation(
                            location.getLatitude(),
                            location.getLongitude(),
                            1);
                    if (addresses != null && addresses.size() > 0) {
                        address = addresses.get(0);
                    }
                } catch (IOException | IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void startReadQR() {
        stopPulsating = false;
        Point size = new Point();
        ((WindowManager) AppDelegate.getInstance().Context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(size);
        int ratio = size.x / size.y;
        int width = previewContainer.getWidth() * ratio;
        int height = previewContainer.getHeight();
        QRCode.getInstance().startRead(this, cameraPreviewContainer, width, height, new QRCode.Callback() {
            @Override
            public void completed(boolean started) {
                if (started) {
                    startPulse();
                    AppDelegate.getInstance().startLocation();
                    showQRHelpFirst();
                } else {
                    pulseLabel.setVisibility(View.VISIBLE);
                }
            }
        });
        NotificationCenter.getInstance().addObserver(QRCodeRecognizedNotification, this);
    }

    private void stopReadQR() {
        pulsating = false;
        stopPulsating = true;
        QRCode.getInstance().stopRead();
        AppDelegate.getInstance().stopLocation();
        NotificationCenter.getInstance().removeObserver(this);
    }

    public void checkActivity() {
        if (activityIndicator.getVisibility() == View.VISIBLE) {
            activityLabel.setVisibility(View.VISIBLE);
        }
    }

    public void initSession() {
        session = Utilities.prefix(Crypto.toBase64(Crypto.generateRandom(QRContent.MatchSessionLength)), QRContent.MatchSessionLength);
        String messageContent = generateMessageContent(session);
        DataService.getInstance().createMessage(messageContent).then(new DoneCallback<String>() {
            @Override
            public void onDone(final String messageKey) {
                QRCodeActivity.this.messageKey = messageKey;
                generateQR();
                checkCorrectionLevel();
                activityIndicator.setVisibility(View.GONE);
                activityLabel.setVisibility(View.GONE);
                if (!UserData.getInstance().getMatchHandshake()) {
                    DataService.getInstance().observeMessageMatch(messageKey, new DataService.DatabaseRefListener<String>() {
                        @Override
                        public void updated(String matchMessageKey) {
                            observeMatchStatus(matchMessageKey, null);
                        }
                    }).fail(new FailCallback<Exception>() {
                        @Override
                        public void onFail(Exception result) {
                            info("UnexpectedErrorOccurred");
                        }
                    });
                }
            }
        }).fail(new FailCallback<Exception>() {
            @Override
            public void onFail(Exception result) {
                AlertDialog.Builder builder = new AlertDialog.Builder(QRCodeActivity.this);
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
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkActivity();
            }
        }, 3000);
    }

    public void destroySession() {
        removeMessage();
        removeMatch();
        session = "";
    }

    public void removeMessage() {
        DataService.getInstance().stopObserveMessageMatch();
        if (!TextUtils.isEmpty(messageKey)) {
            DataService.getInstance().removeMessage(messageKey);
            messageKey = "";
        }
    }

    public void removeMatch() {
        DataService.getInstance().stopObserveMatchStatus();
        if (!TextUtils.isEmpty(matchKey)) {
            final String currentMatchKey = matchKey;
            DataService.getInstance().setMatchPartInactive(currentMatchKey, matchIsFirst).always(new AlwaysCallback<Void, Exception>() {
                @Override
                public void onAlways(Promise.State state, Void resolved, Exception rejected) {
                    DataService.getInstance().removeInactiveMatch(currentMatchKey);
                }
            });
            matchKey = "";
            matchIsFirst = false;
        }
    }

    public String generateMessageContent(String passcode) {
        try {
            return new AESCrypt(passcode).encrypt(Message.generate(profile).toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Message parseMessageContent(String content, String passcode) {
        try {
            return Message.parse(new AESCrypt(passcode).decrypt(content));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Deferred<String, Exception, Void> readMessageContent(String messageKey) {
        final Deferred<String, Exception, Void> deferred = new DeferredObject<>();
        DataService.getInstance().getMessageContent(messageKey).then(new DoneCallback<String>() {
            @Override
            public void onDone(String messageContent) {
                deferred.resolve(messageContent);
            }
        }).fail(new FailCallback<Exception>() {
            @Override
            public void onFail(Exception e) {
                deferred.reject(e);
            }
        });
        return deferred;
    }

    public void generateQRImage(final QRCode.Delegate delegate) {
        String content = QRContent.generate(messageKey, session).toString();
        try {
            String encryptedContent = new AESCrypt(AppSharedAPIKey).encrypt(content);
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            QRCode.getInstance().generate(encryptedContent, size.x, correctionLevel, delegate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public QRContent parseQRImage(String content) {
        try {
            String decryptedContent = new AESCrypt(AppSharedAPIKey).decrypt(content);
            return QRContent.parse(decryptedContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void generateQR() {
        generateQRImage(new QRCode.Delegate() {
            @Override
            public void generationDone(Bitmap bitmap) {
                if (bitmap != null) {
                    qrCodeImageView.setImageBitmap(null);
                    qrCodeImageView.setImageBitmap(bitmap);
                } else {
                    String message = QRCodeActivity.this.getString(R.string.QRCodeGeneration) + "\n" + QRCodeActivity.this.getString(R.string.QRCodeImageNotAvailable);
                    Toast.makeText(QRCodeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void qrCodeRecognized(NotificationCenter.Notification notification) {
        String infoMsgKey = "InvalidQR";
        String content = (String) notification.userInfo.get("qrText");
        if (!TextUtils.isEmpty(content)) {
            QRContent qrContent = parseQRImage(content);
            if (qrContent != null) {
                if (!TextUtils.isEmpty(qrContent.message) && !TextUtils.isEmpty(qrContent.session) && !qrContent.session.equals(session)) {
                    observeMatchStatus(qrContent.message, qrContent.session);
                    return;
                } else {
                    infoMsgKey = "MatchModeNotOpen";
                }
            }
        }
        info(infoMsgKey);
        matchMessageKey = "";
    }

    public void observeMatchStatus(final String matchMessageKey, final String matchMessageSession) {
        if (!this.matchMessageKey.equals(matchMessageKey)) {
            this.matchMessageKey = matchMessageKey;
            readMessageContent(matchMessageKey).then(new DoneCallback<String>() {
                @Override
                public void onDone(final String messageContent) {
                    if (messageContent != null) {
                        if (!UserData.getInstance().getMatchHandshake()) {
                            DataService.getInstance().updateMessageMatch(matchMessageKey, messageKey).fail(new FailCallback<Exception>() {
                                @Override
                                public void onFail(Exception result) {
                                    info("UnexpectedErrorOccurred");
                                }
                            });
                        }
                        removeMatch();
                        matchKey = calcMatchKey(matchMessageKey);
                        matchIsFirst = isMatchFirst(matchMessageKey);
                        DataService.getInstance().createMatchPart(matchKey, matchIsFirst, !UserData.getInstance().getMatchHandshake() ? session : "").done(new DoneCallback<Void>() {
                            @Override
                            public void onDone(Void result) {
                                info("MatchIsProcessed", true);
                                DataService.getInstance().observeMatchStatus(matchKey, new DataService.DatabaseRefListener<MatchStatus>() {
                                    @Override
                                    public void updated(MatchStatus matchStatus) {
                                        if (matchStatus.active1 != null && matchStatus.active2 != null) {
                                            String passcode = matchMessageSession;
                                            if (passcode == null) {
                                                if (matchIsFirst) {
                                                    passcode = matchStatus.session2;
                                                } else {
                                                    passcode = matchStatus.session1;
                                                }
                                            }
                                            if (passcode != null && !TextUtils.isEmpty(passcode)) {
                                                Message message = parseMessageContent(messageContent, passcode);
                                                if (message != null) {
                                                    if (message.space.equals(SecureStore.getSpaceRefName())) {
                                                        if (!sessions.containsKey(passcode)) {
                                                            sessions.put(passcode, true);
                                                            matched(message, passcode, matchIsFirst);
                                                        } else {
                                                            info("AlreadyMatched");
                                                        }
                                                    } else {
                                                        info("DifferentSpace");
                                                    }
                                                } else {
                                                    info("InvalidQR");
                                                }
                                            } else {
                                                info("WaitingForOtherSide", true);
                                            }
                                        } else {
                                            info("WaitingForOtherSide", true);
                                        }
                                    }
                                }).fail(new FailCallback<Exception>() {
                                    @Override
                                    public void onFail(Exception result) {
                                        info("UnexpectedErrorOccurred");
                                    }
                                });
                            }
                        }).fail(new FailCallback<Exception>() {
                            @Override
                            public void onFail(Exception e) {
                                info("UnexpectedErrorOccurred");
                            }
                        });
                    } else {
                        info("DifferentSpace");
                    }
                }
            }).fail(new FailCallback<Exception>() {
                @Override
                public void onFail(Exception result) {
                    info("UnexpectedErrorOccurred");
                }
            });
        }
    }

    public boolean isMatchFirst(String matchMessageKey) {
        return messageKey.compareTo(matchMessageKey) <= 0;
    }

    public String calcMatchKey(String matchMessageKey) {
        if (isMatchFirst(matchMessageKey)) {
            return messageKey + matchMessageKey;
        } else {
            return matchMessageKey + messageKey;
        }
    }

    public void matched(final Message message, String matchMessageSession, boolean matchIsFirst) {
        matchMessageKey = "";
        clearInfo();
        stopReadQR();
        DataService.getInstance().stopObserveMatchStatus();
        Match match = Match.calculateMatch(profile, message);
        if (location != null) {
            match.locationLatitude = "" + location.getLatitude();
            match.locationLongitude = "" + location.getLongitude();
        }
        if (address != null) {
            match.locationName = address.getFeatureName();
            match.locationStreet = address.getAddressLine(0);
            if (match.locationStreet == null) {
                match.locationStreet = address.getThoroughfare();
                if (!TextUtils.isEmpty(address.getSubThoroughfare())) {
                    match.locationStreet += " " + address.getSubThoroughfare();
                }
            }
            if (match.locationStreet.contains(match.locationName)) {
                match.locationName = match.locationStreet;
            }
            match.locationCity = address.getLocality();
            match.locationCountry = address.getCountryName();
        }
        final int matchIndex = UserData.getInstance().addMatch(match);
        NotificationCenter.getInstance().post(UserDataMatchNotification);
        Analytics.getInstance().logMatch(match, session, matchMessageSession);
        if (matchIsFirst) {
            notifyMatch();
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyMatch();
                }
            }, DelayGreetingDuration);
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent matchIntent = new Intent(QRCodeActivity.this, MatchActivity.class);
                matchIntent.putExtra("backLabel", QRCodeActivity.this.getString(R.string.Hi));
                matchIntent.putExtra("match", matchIndex);
                startActivity(matchIntent, TransitionState.SHOW);
            }
        }, DelayMatchDuration);
    }

    public void notifyMatch() {
        clearInfo();
        if (UserData.getInstance().matchVibrate && Utilities.vibrateEnabled(this)) {
            Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(400);
        }
        if (UserData.getInstance().matchPlayGreeting) {
            if (UserData.getInstance().greetingVoice != null && UserData.getInstance().greetingVoice.length > 0) {
                if (!Voice.getInstance().replay(this, UserData.getInstance().greetingVoice)) {
                    Voice.getInstance().sayHi(this, UserData.getInstance().getFirstName(), UserData.getInstance().getGender(), false);
                }
            } else {
                Voice.getInstance().sayHi(this, UserData.getInstance().getFirstName(), UserData.getInstance().getGender(), false);
            }
        }
    }

    public void info(String key) {
        info(key, false);
    }

    public void info(String key, boolean suppressVibrate) {
        infoButton.setEnabled(true);
        infoButton.setAlpha(1.0f);
        if (!lastInfoMsgKey.equals(key)) {
            lastInfoMsgKey = key;
            if (!suppressVibrate && !TextUtils.isEmpty(lastInfoMsgKey)) {
                if (UserData.getInstance().matchVibrate && Utilities.vibrateEnabled(this)) {
                    Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
                    long[] pattern = {0, 200, 100, 200};
                    vibrator.vibrate(pattern, -1);
                }
            }
        }
    }

    public void clearInfo() {
        infoButton.setEnabled(false);
        infoButton.setAlpha(0.5f);
        this.lastInfoMsgKey = "";
    }
}