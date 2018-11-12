package de.oklemenz.sayhi.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.service.Utilities;

/**
 * Created by Oliver Klemenz on 02.11.16.
 */

public class Settings implements Serializable {

    private static final String SettingsField = AppDelegate.Namespace + ".Settings";

    private static Settings instance = new Settings();

    public static Settings getInstance() {
        return instance;
    }

    public Map<String, Object> defaultData = new HashMap<>();

    private Map<String, Object> data = new HashMap<>();
    private boolean loaded = false;
    private Bitmap logo = null;

    public boolean spaceSwitch = false;

    public Settings() {
        defaultData.put("accentColor", AppDelegate.AccentColorDefault);
        defaultData.put("disableHelp", false);
        defaultData.put("disableHelpQR", false);
        defaultData.put("disableHighscoreShare", false);
        defaultData.put("disableHighscoreShow", false);
        defaultData.put("disableNewCategories", false);
        defaultData.put("disableNewProfiles", false);
        defaultData.put("disableNewTags", false);
        defaultData.put("disableRecordAnalytics", false);
        defaultData.put("disableRecordAnalyticsDB", false);
        defaultData.put("disableSettingsHandshake", false);
        defaultData.put("disableSettingsLanguage", false);
        defaultData.put("disableSettingsMatchMode", false);
        defaultData.put("favoriteLanguages", AppDelegate.BundleLangCodes);
        defaultData.put("gradientColor1", AppDelegate.GradientColor1Default);
        defaultData.put("gradientColor2", AppDelegate.GradientColor2Default);
        defaultData.put("highscoreLocal", "");
        //defaultData.put("leftLabel", null);
        //defaultData.put("leftFallbackLabel", null);
        //defaultData.put("logo", null);
        defaultData.put("logoPlain", false);
        defaultData.put("logoZoom", 1.0);
        defaultData.put("maintenance", false);
        defaultData.put("primaryReference", false);
        defaultData.put("queryLimit", 100);
        //defaultData.put("rightLabel", null);
        //defaultData.put("rightFallbackLabel", null);
        defaultData.put("scoreCount", Arrays.asList("leftLeft", "rightRight", "leftRight", "rightLeft"));
        //defaultData.put("settingsHandshake", null);
        //defaultData.put("settingsLanguage", null);
        //defaultData.put("settingsMatchMode", null);
        //defaultData.put("terminology", null);
        data = new HashMap<>(defaultData);
        load();
    }

    public void reset() {
        logo = null;
        data = new HashMap<>(defaultData);
        SharedPreferences preferences = AppDelegate.getInstance().Context.getSharedPreferences(AppDelegate.Namespace, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(SettingsField);
        editor.commit();
        FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).setAnalyticsCollectionEnabled(true);
    }

    public void load() {
        if (loaded) {
            return;
        }
        try {
            SharedPreferences preferences = AppDelegate.getInstance().Context.getSharedPreferences(AppDelegate.Namespace, Context.MODE_PRIVATE);
            String loadedJSONString = preferences.getString(SettingsField, null);
            if (loadedJSONString != null) {
                JSONObject jsonData = new JSONObject(loadedJSONString);
                Map<String, Object> loadedData = Utilities.jsonToMap(jsonData);
                if (loadedData != null) {
                    data = loadedData;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        loaded = true;
    }

    public void store() {
        try {
            SharedPreferences preferences = AppDelegate.getInstance().Context.getSharedPreferences(AppDelegate.Namespace, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            JSONObject jsonData = new JSONObject(data);
            editor.putString(SettingsField, jsonData.toString(0));
            editor.commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void update(Map<String, Object> newData, boolean spaceSwitch) {
        reset();
        for (String name : newData.keySet()) {
            Object value = newData.get(name);
            Object newValue = null;

            if (name.equals("accentColor")) {
                newValue = value instanceof String ? ((String) value) : null;
            }
            if (name.equals("disableHelp")) {
                newValue = value instanceof Boolean ? ((Boolean) value) : null;
            }
            if (name.equals("disableHelpQR")) {
                newValue = value instanceof Boolean ? ((Boolean) value) : null;
            }
            if (name.equals("disableHighscoreShare")) {
                newValue = value instanceof Boolean ? ((Boolean) value) : null;
            }
            if (name.equals("disableHighscoreShow")) {
                newValue = value instanceof Boolean ? ((Boolean) value) : null;
            }
            if (name.equals("disableNewCategories")) {
                newValue = value instanceof Boolean ? ((Boolean) value) : null;
            }
            if (name.equals("disableNewProfiles")) {
                newValue = value instanceof Boolean ? ((Boolean) value) : null;
            }
            if (name.equals("disableNewTags")) {
                newValue = value instanceof Boolean ? ((Boolean) value) : null;
            }
            if (name.equals("disableRecordAnalytics")) {
                newValue = value instanceof Boolean ? ((Boolean) value) : null;
                if (newValue != null) {
                    FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).setAnalyticsCollectionEnabled(!((Boolean) newValue));
                }
            }
            if (name.equals("disableRecordAnalyticsDB")) {
                newValue = value instanceof Boolean ? ((Boolean) value) : null;
            }
            if (name.equals("disableSettingsHandshake")) {
                newValue = value instanceof Boolean ? ((Boolean) value) : null;
            }
            if (name.equals("disableSettingsLanguage")) {
                newValue = value instanceof Boolean ? ((Boolean) value) : null;
            }
            if (name.equals("disableSettingsMatchMode")) {
                newValue = value instanceof Boolean ? ((Boolean) value) : null;
            }
            if (name.equals("favoriteLanguages")) {
                newValue = value instanceof String ? Arrays.asList(((String) value).split(",")) : null;
            }
            if (name.equals("gradientColor1")) {
                newValue = value instanceof String ? ((String) value) : null;
            }
            if (name.equals("gradientColor2")) {
                newValue = value instanceof String ? ((String) value) : null;
            }
            if (name.equals("highscoreLocal")) {
                newValue = value instanceof String ? ((String) value) : null;
            }
            if (name.equals("leftLabel")) {
                newValue = value instanceof String ? ((String) value) : null;
            }
            if (name.equals("leftLabelFallback")) {
                newValue = value instanceof String ? ((String) value) : null;
            }
            if (name.equals("logo")) {
                newValue = value instanceof String ? ((String) value) : null;
            }
            if (name.equals("logoPlain")) {
                newValue = value instanceof Boolean ? ((Boolean) value) : null;
            }
            if (name.equals("logoZoom")) {
                newValue = value instanceof Number ? ((Number) value) : null;
            }
            if (name.equals("maintenance")) {
                newValue = value instanceof Boolean ? ((Boolean) value) : null;
            }
            if (name.equals("primaryReference")) {
                newValue = value instanceof Boolean ? ((Boolean) value) : null;
            }
            if (name.equals("queryLimit")) {
                newValue = value instanceof Long ? ((Long) value).intValue() : null;
            }
            if (name.equals("rightLabel")) {
                newValue = value instanceof String ? ((String) value) : null;
            }
            if (name.equals("rightLabelFallback")) {
                newValue = value instanceof String ? ((String) value) : null;
            }
            if (name.equals("scoreCount")) {
                newValue = value instanceof String ? Arrays.asList(((String) value).split(",")) : null;
            }
            if (name.equals("settingsHandshake")) {
                newValue = value instanceof Boolean ? ((Boolean) value) : null;
            }
            if (name.equals("settingsLanguage")) {
                newValue = value instanceof String ? ((String) value) : null;
            }
            if (name.equals("settingsMatchMode")) {
                newValue = value instanceof String ? ((String) value) : null;
            }
            if (name.equals("terminology")) {
                newValue = value instanceof String ? ((String) value) : null;
            }

            if (newValue != null) {
                data.put(name, newValue);
            }
        }

        if (!spaceSwitch) {
            if (!getDisableSettingsHandshake()) {
                data.remove("settingsHandshake");
            }
            if (!getDisableSettingsLanguage()) {
                data.remove("settingsLanguage");
            }
            if (!getDisableSettingsMatchMode()) {
                data.remove("settingsMatchMode");
            }
        }

        store();
        this.spaceSwitch = spaceSwitch;
    }

    public Map<String, String> configItems() {
        Map<String, String> configItems = new HashMap<>();
        if (getSettingsHandshake() != null) {
            configItems.put("handshake", getSettingsHandshake() ? "true" : "false");
        }
        if (getSettingsLanguage() != null) {
            configItems.put("language", getSettingsLanguage());
        }
        if (getSettingsMatchModeExternalCode() != null) {
            configItems.put("matchMode", getSettingsMatchModeExternalCode());
        }
        return configItems;
    }

    public String getAccentColor() {
        return (String) data.get("accentColor");
    }

    public Boolean getDisableHelp() {
        return (Boolean) data.get("disableHelp");
    }

    public Boolean getDisableHelpQR() {
        return (Boolean) data.get("disableHelpQR");
    }

    public Boolean getDisableHighscoreShare() {
        return (Boolean) data.get("disableHighscoreShare");
    }

    public Boolean getDisableHighscoreShow() {
        return (Boolean) data.get("disableHighscoreShow");
    }

    public boolean getDisableNewCategories() {
        return (Boolean) data.get("disableNewCategories");
    }

    public boolean getDisableNewProfiles() {
        return (Boolean) data.get("disableNewProfiles");
    }

    public boolean getDisableNewTags() {
        return (Boolean) data.get("disableNewTags");
    }

    public boolean getDisableRecordAnalytics() {
        return (Boolean) data.get("disableRecordAnalytics");
    }

    public boolean getDisableRecordAnalyticsDB() {
        return (Boolean) data.get("disableRecordAnalyticsDB");
    }

    public Boolean getDisableSettingsHandshake() {
        return (Boolean) data.get("disableSettingsHandshake");
    }

    public Boolean getDisableSettingsLanguage() {
        return (Boolean) data.get("disableSettingsLanguage");
    }

    public Boolean getDisableSettingsMatchMode() {
        return (Boolean) data.get("disableSettingsMatchMode");
    }

    public List<String> getFavoriteLanguages() {
        return (List<String>) data.get("favoriteLanguages");
    }

    public String getGradientColor1() {
        return (String) data.get("gradientColor1");
    }

    public String getGradientColor2() {
        return (String) data.get("gradientColor2");
    }

    public String getHighscoreLocal() {
        return (String) data.get("highscoreLocal");
    }

    public String getLeftLabel() {
        return (String) data.get("leftLabel");
    }

    public String getLeftLabelFallback() {
        return (String) data.get("leftLabelFallback");
    }

    public Bitmap getLogo() {
        if (logo != null) {
            return logo;
        }
        if (data.get("logo") instanceof String) {
            String logoData = (String) data.get("logo");
            byte[] bytes = Utilities.fromBase64(logoData);
            logo = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        return logo;
    }

    public boolean getLogoPlain() {
        return (Boolean) data.get("logoPlain");
    }

    public float getLogoZoom() {
        return ((Number) data.get("logoZoom")).floatValue();
    }

    public boolean getMaintenance() {
        return (Boolean) data.get("maintenance");
    }

    public boolean getPrimaryReference() {
        return (Boolean) data.get("primaryReference");
    }

    public int getQueryLimit() {
        return (Integer) data.get("queryLimit");
    }

    public String getRightLabel() {
        return (String) data.get("rightLabel");
    }

    public String getRightLabelFallback() {
        return (String) data.get("rightLabelFallback");
    }

    public List<String> getScoreCount() {
        return (List<String>) data.get("scoreCount");
    }

    public Boolean getSettingsHandshake() {
        return (Boolean) data.get("settingsHandshake");
    }

    public String getSettingsLanguage() {
        return (String) data.get("settingsLanguage");
    }

    public String getSettingsMatchModeExternalCode() {
        return (String) data.get("settingsMatchMode");
    }

    public Enum.MatchMode getSettingsMatchMode() {
        if (getSettingsMatchModeExternalCode() != null) {
            return Enum.MatchMode.fromExternalCode(getSettingsMatchModeExternalCode());
        }
        return null;
    }

    public String getTerminology() {
        return (String) data.get("terminology");
    }
}