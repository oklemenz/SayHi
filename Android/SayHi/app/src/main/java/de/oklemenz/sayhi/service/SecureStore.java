package de.oklemenz.sayhi.service;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.analytics.FirebaseAnalytics;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.BuildConfig;
import de.oklemenz.sayhi.model.UserData;

import static de.oklemenz.sayhi.AppDelegate.Namespace;
import static de.oklemenz.sayhi.AppDelegate.SpaceSwitchedNotification;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class SecureStore {

    public interface Delegate {
        void onCancel();

        void onComplete(String userData);
    }

    private static final String AppInitialized = Namespace + ".AppInit";
    private static final String UserDataField = Namespace + ".UserData";
    private static final String FingerprintField = Namespace + ".UseFingerprint";
    private static final String SpaceField = Namespace + ".Space";

    public static final String StandardSpace = BuildConfig.DEBUG ? "Dev" : "Standard";

    private static SecureStore instance = new SecureStore();

    public static SecureStore getInstance() {
        return instance;
    }

    public boolean isOpen() {
        return Crypto.getInstance().isOpen();
    }

    public void close() {
        Crypto.getInstance().close();
    }

    private SecureStore() {
    }

    public void store(final String userData, final Delegate delegate) {
        Crypto.getInstance().encrypt(userData, false, new Crypto.Delegate() {
            @Override
            public void onCancel() {
                if (delegate != null) {
                    delegate.onCancel();
                }
            }

            @Override
            public void onDone(String encryptedUserData) {
                storeInternal(encryptedUserData);
                if (delegate != null) {
                    delegate.onComplete(userData);
                }
            }

            public void onError() {
                if (delegate != null) {
                    delegate.onComplete(null);
                }
            }
        });
    }

    private void storeInternal(String userData) {
        if (userData != null) {
            SharedPreferences preferences = AppDelegate.getInstance().Context.getSharedPreferences(AppDelegate.Namespace, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(UserDataField, userData);
            editor.commit();
        }
    }

    public void load(final Delegate delegate) {
        SharedPreferences preferences = AppDelegate.getInstance().Context.getSharedPreferences(AppDelegate.Namespace, Context.MODE_PRIVATE);
        String userData = preferences.getString(UserDataField, null);
        if (userData != null) {
            Crypto.getInstance().decrypt(userData, fingerprintUsed(), new Crypto.Delegate() {
                @Override
                public void onCancel() {
                    if (delegate != null) {
                        delegate.onCancel();
                    }
                }

                @Override
                public void onDone(String userData) {
                    if (delegate != null) {
                        delegate.onComplete(userData);
                    }
                }

                @Override
                public void onError() {
                    if (delegate != null) {
                        delegate.onComplete(null);
                    }
                }
            });
        } else {
            if (delegate != null) {
                delegate.onComplete(null);
            }
        }
    }

    public static void setUseFingerprint(boolean state) {
        SharedPreferences preferences = AppDelegate.getInstance().Context.getSharedPreferences(AppDelegate.Namespace, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(FingerprintField, state);
        editor.commit();
    }

    public static boolean fingerprintUsed() {
        SharedPreferences preferences = AppDelegate.getInstance().Context.getSharedPreferences(AppDelegate.Namespace, Context.MODE_PRIVATE);
        return Utilities.authEnabled(AppDelegate.getInstance().Context) && preferences.getBoolean(FingerprintField, false);
    }

    public static void setAppInitialized() {
        SharedPreferences preferences = AppDelegate.getInstance().Context.getSharedPreferences(AppDelegate.Namespace, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(AppInitialized, true);
        editor.commit();
    }

    public static boolean appInitialized() {
        SharedPreferences preferences = AppDelegate.getInstance().Context.getSharedPreferences(AppDelegate.Namespace, Context.MODE_PRIVATE);
        return preferences.getBoolean(AppInitialized, false);
    }

    public static String getSpace() {
        SharedPreferences preferences = AppDelegate.getInstance().Context.getSharedPreferences(AppDelegate.Namespace, Context.MODE_PRIVATE);
        String space = preferences.getString(SpaceField, null);
        if (space != null) {
            return space;
        }
        switchSpace(StandardSpace, true);
        return StandardSpace;
    }

    public static String getSpaceRefName() {
        return getSpace().trim().toLowerCase();
    }

    public static void switchSpace(String newSpace, boolean suppressNotification) {
        SharedPreferences preferences = AppDelegate.getInstance().Context.getSharedPreferences(AppDelegate.Namespace, Context.MODE_PRIVATE);
        String currentSpace = preferences.getString(SpaceField, null);
        if (currentSpace == null || !currentSpace.equals(newSpace)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(SpaceField, newSpace);
            editor.commit();
            UserData.getInstance().spaceSwitched(newSpace);
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).setUserProperty("space", getSpaceRefName());
            if (!suppressNotification) {
                Analytics.getInstance().logSpaceSwitched(newSpace);
                NotificationCenter.getInstance().post(SpaceSwitchedNotification);
            }
        }
    }

    public static void switchToStandardSpace() {
        switchSpace(StandardSpace, false);
    }

    public static String getStandardSpaceName(Context context) {
        return Utilities.getStringResourceId(context, StandardSpace) != 0 ?
                context.getString(Utilities.getStringResourceId(context, StandardSpace)) : StandardSpace;
    }
}