package de.oklemenz.sayhi;

/*
 * Open by URI
 * adb shell am start -a "android.intent.action.VIEW" -d "sayhi://?space=sap\&accessCode=0a95adbf8581859ae0cc477127abeaf4ad89916405c41855af8fbc482e1634e8\&language=en\&status=Test\&matchMode=open\&handshake=true\&profile=R%26I\&profileRelation=Colleague\&profileMatchMode=try"
 */

/*
 * Simulate Geo Location
 *  telnet 127.0.0.1 5554
 *  auth <auth_token> (see file .emulator_console_auth_token)
 *  geo fix <longitude> <latitude>
 */

/*
 * Emulator Fingerprint (Security Settings and during Auth in app)
 *  telnet 127.0.0.1 5554
 *  auth <auth_token> (see file .emulator_console_auth_token)
 *  finger touch 1
 */

import android.Manifest;
import android.app.AlertDialog;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.jdeferred.DoneCallback;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.oklemenz.sayhi.activity.WelcomeActivity;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.Enum;
import de.oklemenz.sayhi.model.Profile;
import de.oklemenz.sayhi.model.Settings;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.DataService;
import de.oklemenz.sayhi.service.Foreground;
import de.oklemenz.sayhi.service.IconService;
import de.oklemenz.sayhi.service.NotificationCenter;
import de.oklemenz.sayhi.service.SecureStore;
import de.oklemenz.sayhi.service.Utilities;

/**
 * Created by Oliver Klemenz on 03.03.17.
 */

public class AppDelegate extends Application implements NotificationCenter.Observer, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public static String AppName = "SAY Hi!";
    public static String Namespace = "de.oklemenz.sayhi";
    public static String AppSharedAPIKey = "9d159156-bcdd-4dbc-9016-df6fcfd30964";
    public static String PrimaryLangCode = "en";
    public static List<String> BundleLangCodes = Arrays.asList("en", "de");
    public static String SeparatorString = " • ";

    public static String LoginNotification = "LoginNotification";
    public static String DataServiceSetupNotification = "DataServiceSetupNotification";
    public static String SpaceSwitchedNotification = "SpaceSwitchedNotification";
    public static String ColorsSetNotification = "ColorsSetNotification";
    public static String SettingsFetchedNotification = "SettingsFetchedNotification";
    public static String IconsFetchedNotification = "IconsFetchedNotification";
    public static String FavoriteCategoriesFetchedNotification = "FavoriteCategoriesFetchedNotification";
    public static String ShowContentNotification = "ShowContentNotification";
    public static String SetupEndNotification = "SetupEndNotification";
    public static String QRCodeRecognizedNotification = "QRCodeRecognizedNotification";
    public static String UserDataFetchNotification = "UserDataFetchNotification";
    public static String UserDataFetchedNotification = "UserDataFetchedNotification";
    public static String UserDataChangedNotification = "UserDataChangedNotification";
    public static String UserDataStoredNotification = "UserDataStoredNotification";
    public static String UserDataClearedNotification = "UserDataClearedNotification";
    public static String UserDataLangChangedNotification = "UserDataLangChangedNotification";
    public static String UserDataMatchNotification = "UserDataMatchNotification";
    public static String UpdateLocationNotification = "UpdateLocationNotification";

    public static String AccentColorDefault = "#385B7D";
    public static String GradientColor1Default = "#89BDAB";
    public static String GradientColor2Default = "#E8DFB3";

    public static int AccentColor = 0;
    public static int GradientColor1 = 0;
    public static int GradientColor2 = 0;

    public static ColorStateList toggleTextColor;
    public static ColorStateList switchThumbColor;
    public static ColorStateList switchTrackColor;

    public static Boolean NoTagAlertShown = false;

    public static String uid;

    public boolean suppressReturnToHome = false;
    public BaseActivity Context;

    public void setContext(BaseActivity context) {
        Context = context;
        suppressReturnToHome = false;
    }

    private static AppDelegate instance;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private long LocationInterval = 2 * 1000;

    public static AppDelegate getInstance() {
        return instance;
    }

    private Foreground.Listener foregroundListener;

    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authListener;

    public boolean preventBackgroundProtect = false;
    private boolean isAppFirstStart = true;
    private boolean isAppInBackground = false;
    private boolean protectImmediately = false;
    private Thread backgroundTaskThread;

    public Map<String, String> spaceItems;
    public Map<String, String> configItems;
    public Map<String, String> settingsItems;
    public boolean settingsSpaceSwitch;

    private boolean locationServiceConnected = false;
    private boolean locationServiceStarted = false;
    public boolean configurationAdjusted = false;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        foregroundListener = new Foreground.Listener() {

            public void onBecameForeground() {
                isAppInBackground = false;
                backgroundTaskThread = null;

                if (Context != null) {
                    Context.unprotect();
                }

                if (!UserData.getInstance().isInitialized() || protectImmediately) {
                    final boolean finalIsAppFirstStart = isAppFirstStart;
                    Handler handler = new Handler(AppDelegate.getInstance().Context.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!finalIsAppFirstStart) {
                                returnToHome();
                                if (!SecureStore.fingerprintUsed()) {
                                    NotificationCenter.getInstance().post(ShowContentNotification);
                                }
                            }
                        }
                    });
                }
                protectImmediately = false;
                isAppFirstStart = false;
            }

            public void onBecameBackground() {
                protectImmediately = false;
                if (isAppInBackground && !preventBackgroundProtect) {

                    if (Context != null) {
                        Context.protect();
                    }

                    protectImmediately = UserData.getInstance().passcodeTimeout == Enum.PasscodeTimeout.Min0;

                    UserData.getInstance().touch(new UserData.Callback() {
                        @Override
                        public void error() {
                            protect();
                        }

                        @Override
                        public void cancel() {
                            protect();
                        }

                        @Override
                        public void completion() {
                            protect();
                        }
                    });
                }
            }
        };

        Foreground.get(this).addListener(foregroundListener);

        NotificationCenter.getInstance().addObserver(DataServiceSetupNotification, this, false);
        NotificationCenter.getInstance().addObserver(SettingsFetchedNotification, this, false);
        NotificationCenter.getInstance().addObserver(UserDataFetchedNotification, this, false);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Foreground.get(this).removeListener(foregroundListener);
        logout();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(final int level) {
        super.onTrimMemory(level);
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            isAppInBackground = true;
        }
    }

    public void notify(String name, NotificationCenter.Notification notification) {
        if (name.equals(DataServiceSetupNotification)) {
            dataServiceSetup();
        } else if (name.equals(SettingsFetchedNotification)) {
            settingsFetched();
        } else if (name.equals(UserDataFetchedNotification)) {
            userDataFetched();
        }
    }

    public void logon() {
        DataService.getInstance();
        SecureStore.getSpace();
        auth = FirebaseAuth.getInstance();
        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    if (AppDelegate.uid == null) {
                        AppDelegate.uid = user.getUid();
                        NotificationCenter.getInstance().post(LoginNotification, null);
                        IconService.getInstance().fetch();
                    }
                } else {
                    AppDelegate.uid = null;
                }

                FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).setUserProperty("deviceLanguage", Locale.getDefault().getLanguage());
                FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).setUserProperty("deviceLocale", Locale.getDefault().toString());
            }
        };
        auth.addAuthStateListener(authListener);
        signInAnonymously();
    }

    private void signInAnonymously() {
        auth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful()) {
                    Toast.makeText(Context, Context.getString(R.string.NetworkError) + "\n\n" +
                            Context.getString(R.string.UnexpectedErrorOccurred), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void logout() {
        auth.signOut();
        if (authListener != null) {
            auth.removeAuthStateListener(authListener);
        }
    }

    public void initialize() {
        updateColors();
        logon();
    }

    public void setQueryUri(final Uri queryUri) {
        this.spaceItems = Utilities.queryParameters(queryUri);
        this.configItems = Utilities.queryParameters(queryUri);

        if (queryUri != null) {
            if (DataService.getInstance().isSetup) {
                dataServiceSetup();
            }
            if (!SecureStore.appInitialized() || UserData.getInstance().isInitialized()) {
                applyConfiguration();
            } else {
                UserData.getInstance().requestInitialize = true;
                NotificationCenter.getInstance().post(ShowContentNotification);
            }
        }
    }

    private void settingsFetched() {
        settingsItems = Settings.getInstance().configItems();
        settingsSpaceSwitch = Settings.getInstance().spaceSwitch;
        if (!SecureStore.appInitialized() || UserData.getInstance().isInitialized()) {
            applyConfiguration();
        }
        updateColors();
    }

    public void updateColors() {
        AccentColor = Color.parseColor(Settings.getInstance().getAccentColor());
        GradientColor1 = Color.parseColor(Settings.getInstance().getGradientColor1());
        GradientColor2 = Color.parseColor(Settings.getInstance().getGradientColor2());

        toggleTextColor = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_enabled},
                        new int[]{}
                },
                new int[]{
                        Color.WHITE,
                        Color.GRAY,
                        AppDelegate.AccentColor,
                }
        );
        switchThumbColor = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked, android.R.attr.state_enabled},
                        new int[]{android.R.attr.state_checked, -android.R.attr.state_enabled},
                        new int[]{-android.R.attr.state_checked, android.R.attr.state_enabled},
                        new int[]{-android.R.attr.state_checked, -android.R.attr.state_enabled},
                        new int[]{}
                },
                new int[]{
                        AppDelegate.AccentColor,
                        Color.GRAY,
                        Color.WHITE,
                        Color.GRAY,
                        Color.WHITE,
                }
        );
        switchTrackColor = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_enabled},
                        new int[]{}
                },
                new int[]{
                        AppDelegate.AccentColor,
                        Color.GRAY,
                        Color.GRAY
                }
        );

        NotificationCenter.getInstance().post(ColorsSetNotification);
    }

    private void dataServiceSetup() {
        if (spaceItems != null) {
            final String space = spaceItems.get("space");
            final String accessCode = spaceItems.get("accessCode");
            if (space != null) {
                final String spaceRefName = space.trim().toLowerCase();
                if (!TextUtils.isEmpty(spaceRefName)) {
                    DataService.getInstance().fetchSpaceMeta(spaceRefName).then(new DoneCallback<Map<String, Object>>() {
                        @Override
                        public void onDone(Map<String, Object> meta) {
                            if (meta != null) {
                                if ((Boolean) meta.get("protected")) {
                                    DataService.getInstance().verifySpaceProtection(spaceRefName, accessCode).then(new DoneCallback<Boolean>() {
                                        @Override
                                        public void onDone(Boolean verified) {
                                            if (verified) {
                                                SecureStore.switchSpace(space, false);
                                                returnToHome();
                                            }
                                        }
                                    });
                                } else {
                                    SecureStore.switchSpace(space, false);
                                    returnToHome();
                                }
                            }
                        }
                    });
                }
            }
            spaceItems = null;
        }
    }

    private void userDataFetched() {
        applyConfiguration();
    }

    public void applyConfiguration() {
        boolean configChanged = false;

        if (configItems != null) {
            if (applyConfigItems(configItems, false)) {
                configChanged = true;
            }
            configItems = null;
        }
        if (settingsItems != null) {
            applyConfigItems(settingsItems, true);
            settingsItems = null;
            settingsSpaceSwitch = false;
        }

        if (configChanged) {
            configurationAdjusted = true;
        }

        configItems = null;
    }

    public boolean applyConfigItems(Map<String, String> configItems, boolean fromSettings) {
        boolean configChanged = false;

        String language = configItems.get("language");
        if (language != null && Arrays.asList(Locale.getISOLanguages()).indexOf(language) > -1) {
            if (fromSettings || !Settings.getInstance().getDisableSettingsLanguage()) {
                if (!UserData.getInstance().getLangCode().equals(language)) {
                    UserData.getInstance().setLangCode(language);
                    NotificationCenter.getInstance().post(UserDataLangChangedNotification);
                    configChanged = true;
                }
            }
        }

        String status = configItems.get("status");
        if (status != null) {
            if (!UserData.getInstance().getStatus().equals(status)) {
                UserData.getInstance().setStatus(status);
                configChanged = true;
            }
        }

        String matchModeExternalCode = configItems.get("matchMode");
        if (matchModeExternalCode != null) {
            if (fromSettings || !Settings.getInstance().getDisableSettingsMatchMode()) {
                Enum.MatchMode matchMode = Enum.MatchMode.fromExternalCode(matchModeExternalCode);
                if (matchMode != null) {
                    if (!UserData.getInstance().getMatchMode().equals(matchMode)) {
                        UserData.getInstance().setMatchMode(matchMode);
                        configChanged = true;
                    }
                }
            }
        }

        String handshakeString = configItems.get("handshake");
        if (handshakeString != null) {
            if (fromSettings || !Settings.getInstance().getDisableSettingsHandshake()) {
                boolean matchHandshake = handshakeString.toLowerCase().equals("true");
                if (UserData.getInstance().getMatchHandshake() != matchHandshake) {
                    UserData.getInstance().setMatchHandshake(matchHandshake);
                    configChanged = true;
                }
            }
        }

        String profileName = configItems.get("profile");
        if (profileName != null && !TextUtils.isEmpty(profileName)) {
            Profile profile = UserData.getInstance().createProfile(profileName);
            UserData.getInstance().setCurrentProfile(profile);
            configChanged = true;
        }

        String profileRelationTypeCode = configItems.get("profileRelation");
        if (profileRelationTypeCode != null) {
            Enum.RelationType relationType = Enum.RelationType.fromDescription(profileRelationTypeCode);
            if (relationType != null) {
                if (UserData.getInstance().currentProfile() != null) {
                    if (UserData.getInstance().currentProfile().relationType != relationType) {
                        UserData.getInstance().currentProfile().relationType = relationType;
                        configChanged = true;
                    }
                }
            }
        }

        String profileMatchModeExternalCode = configItems.get("profileMatchMode");
        if (profileMatchModeExternalCode != null) {
            Enum.MatchMode matchMode = Enum.MatchMode.fromExternalCode(profileMatchModeExternalCode);
            if (matchMode != null) {
                if (UserData.getInstance().currentProfile() != null) {
                    if (UserData.getInstance().currentProfile().matchMode != matchMode) {
                        UserData.getInstance().currentProfile().matchMode = matchMode;
                        configChanged = true;
                    }
                }
            }
        }

        return configChanged;
    }

    public void protect() {
        stopLocation();
        if (protectImmediately) {
            clear();
        } else {
            createTimer(UserData.getInstance().passcodeTimeout.code * 60 * 1000);
        }
    }

    private void createTimer(final int timeout) {
        backgroundTaskThread = new Thread() {
            public void run() {
                Looper.prepare();

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (backgroundTaskThread != null) {
                            clear();
                        }
                        handler.removeCallbacks(this);
                        Looper.myLooper().quit();
                    }
                }, timeout);

                Looper.loop();
            }
        };
        backgroundTaskThread.start();
    }

    public boolean returnToHome() {
        if (Context != null) {
            Context.hideHelp();
        }
        if (!(Context instanceof WelcomeActivity)) {
            if (!suppressReturnToHome) {
                Intent intent = new Intent(Context, WelcomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("skipIntro", true);
                AppDelegate.getInstance().Context.startActivity(intent);
                suppressReturnToHome = true;
            }
            return true;
        }
        return false;
    }

    public void clear() {
        UserData.getInstance().clear();
        backgroundTaskThread = null;
    }

    public static void openAppInStore(BaseActivity context) {
        String appId = context.getPackageName(); // BuildConfig.APPLICATION_ID
        Intent rateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appId));
        boolean marketFound = false;

        final List<ResolveInfo> otherApps = context.getPackageManager().queryIntentActivities(rateIntent, 0);
        for (ResolveInfo otherApp : otherApps) {
            if (otherApp.activityInfo.applicationInfo.packageName.equals("com.android.vending")) {
                ActivityInfo otherAppActivity = otherApp.activityInfo;
                ComponentName componentName = new ComponentName(
                        otherAppActivity.applicationInfo.packageName,
                        otherAppActivity.name
                );

                rateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                rateIntent.setComponent(componentName);
                context.startActivity(rateIntent);
                marketFound = true;
                break;
            }
        }
        if (!marketFound) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.AppStoreUrlAndroid)));
            context.startActivity(webIntent);
        }
    }

    public void initLocation() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    public void startLocation() {
        if (googleApiClient != null && !locationServiceStarted) {
            googleApiClient.connect();
        }
        locationServiceStarted = true;
        if (locationServiceConnected) {
            fetchLocation();
        }
    }

    public void stopLocation() {
        if (googleApiClient != null) {
            googleApiClient.disconnect();
            locationServiceConnected = false;
        }
        locationServiceStarted = false;
    }

    @Override
    public void onConnected(@Nullable Bundle connectionHint) {
        locationServiceConnected = true;
        if (locationServiceStarted) {
            fetchLocation();
        }
    }

    private void fetchLocation() {
        Context.askForPermission(Manifest.permission.ACCESS_FINE_LOCATION, BaseActivity.PERMISSION_ACCESS_LOCATION, new BaseActivity.PermissionDelegate() {
            @Override
            @SuppressWarnings("MissingPermission")
            public void permissionResult(String permission, int requestCode, boolean granted) {
                if (requestCode == BaseActivity.PERMISSION_ACCESS_LOCATION) {
                    if (granted) {
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                                if (location != null) {
                                    onLocationChanged(location);
                                }
                                try {
                                    locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                                            .setInterval(LocationInterval).setFastestInterval(LocationInterval);
                                    LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, AppDelegate.this, Looper.getMainLooper());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } else {
                        alertLocationError(Context);
                    }
                }
            }
        });
    }

    public void onLocationChanged(final Location location) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("location", location);
                NotificationCenter.getInstance().post(UpdateLocationNotification, new NotificationCenter.Notification(userInfo));
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    private void alertLocationError(final BaseActivity context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.TagMatching)
                .setMessage(context.getString(R.string.LocationError) + "\n\n" + context.getString(R.string.LocationUsageDescription))
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNeutralButton(context.getString(R.string.Settings), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Utilities.openAppSettings(context);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }
}
