package de.oklemenz.sayhi.model;

import android.text.TextUtils;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.service.Analytics;
import de.oklemenz.sayhi.service.NotificationCenter;
import de.oklemenz.sayhi.service.SecureStore;
import de.oklemenz.sayhi.service.Utilities;

import static de.oklemenz.sayhi.AppDelegate.BundleLangCodes;
import static de.oklemenz.sayhi.AppDelegate.PrimaryLangCode;
import static de.oklemenz.sayhi.AppDelegate.UserDataChangedNotification;
import static de.oklemenz.sayhi.AppDelegate.UserDataClearedNotification;
import static de.oklemenz.sayhi.AppDelegate.UserDataFetchNotification;
import static de.oklemenz.sayhi.AppDelegate.UserDataFetchedNotification;
import static de.oklemenz.sayhi.AppDelegate.UserDataStoredNotification;
import static de.oklemenz.sayhi.service.SecureStore.StandardSpace;

/**
 * Created by Oliver Klemenz on 31.10.16.
 */

public class UserData implements Serializable {

    public interface Callback {
        void error();

        void cancel();

        void completion();
    }

    private static String DefaultProfileName = AppDelegate.getInstance().Context.getString(R.string.Standard);

    public static int NameMaxLength = 30;
    public static int StatusMaxLength = 30;
    public static int BaseYear = 1900;

    private static UserData instance = new UserData();

    public static UserData getInstance() {
        return instance;
    }

    private int year = Calendar.getInstance().get(Calendar.YEAR);

    private boolean initialized = false;

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
        this.requestInitialize = false;
        if (initialized) {
            initStandardProfile();
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    private String installationUUID = "";

    public void setInstallationUUID(String installationUUID) {
        this.installationUUID = installationUUID;
        FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).setUserProperty("installation", installationUUID);
    }

    public String getInstallationUUID() {
        return this.installationUUID;
    }

    private String langCode = PrimaryLangCode;

    public void setLangCode(String langCode) {
        this.langCode = langCode;
        FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).setUserProperty("language", langCode);
    }

    public String getLangCode() {
        return this.langCode;
    }

    private String firstName = "";

    public void setFirstName(String firstName) {
        this.firstName = firstName.trim();
    }

    public String getFirstName() {
        return this.firstName;
    }

    private Enum.Gender gender = Enum.Gender.None;

    public void setGender(Enum.Gender gender) {
        this.gender = gender;
        FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).setUserProperty("gender", gender.code);
    }

    public Enum.Gender getGender() {
        return this.gender;
    }

    private int birthYear = 0;

    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
        FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).setUserProperty("birthYear", Integer.toString(birthYear));
        FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).setUserProperty("age", Integer.toString(getAge()));
    }

    public int getBirthYear() {
        return birthYear;
    }

    public int getAge() {
        if (birthYear >= BaseYear && birthYear <= year) {
            return year - birthYear;
        }
        return 0;
    }

    private String status = "";

    public void setStatus(String status) {
        status = status.replaceAll("[" + AppDelegate.getInstance().Context.getString(R.string.Marks) + "]", " ");
        this.status = status.trim();
    }

    public String getStatus() {
        return this.status;
    }

    private Enum.MatchMode matchMode = Enum.MatchMode.Open;

    public void setMatchMode(Enum.MatchMode matchMode) {
        this.matchMode = matchMode;
        FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).setUserProperty("matchMode", matchMode.toExternalString());
        FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).setUserProperty("matchCode", Integer.toString(matchMode.code));
    }

    public Enum.MatchMode getMatchMode() {
        return this.matchMode;
    }

    private boolean matchHandshake = false;

    public void setMatchHandshake(boolean matchHandshake) {
        this.matchHandshake = matchHandshake;
        FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).setUserProperty("matchHandshake", Boolean.toString(this.matchHandshake));
    }

    public boolean getMatchHandshake() {
        return this.matchHandshake;
    }

    private boolean fingerprint = false;

    public void setFingerprint(boolean fingerprint) {
        this.fingerprint = fingerprint;
        if (initialized) {
            SecureStore.setUseFingerprint(fingerprint);
        }
    }

    public boolean getFingerprint() {
        return fingerprint;
    }

    public boolean matchVibrate = Utilities.vibrateEnabled(AppDelegate.getInstance().Context);
    public boolean matchPlayGreeting = true;
    public Enum.PasscodeTimeout passcodeTimeout = Enum.PasscodeTimeout.Min5;
    public int inviteFriendSentCount = 0;
    public int currentProfileId = 0;

    public byte[] greetingVoice;
    public List<Profile> profiles = new ArrayList<>();
    public List<Match> history = new ArrayList<>();

    public long scoreMatchCount = 0;
    public long bothPosScore = 0;
    public long bothNegScore = 0;
    public long onlyPosScore = 0;
    public long onlyNegScore = 0;

    public long matchScore() {
        return bothPosScore + bothNegScore + onlyPosScore + onlyNegScore;
    }

    public String highscoreLocal = "";

    public long localScore = 0;

    public long shareScore() {
        return matchScore() - localScore;
    }

    public long scoreLocalCount = 0;

    public long scoreShareCount() {
        return scoreMatchCount - scoreLocalCount;
    }

    public Map<String, Integer> newItemsHash = new HashMap<>();
    public Map<String, Tag> ownTags = new HashMap<>();

    public boolean qrHelpFirstShown = false;

    public Map<String, Object> standardSettings = new HashMap<>();

    public boolean requestInitialize = false;

    public UserData() {
        setup();
        NotificationCenter.getInstance().addObserver(UserDataFetchNotification, new NotificationCenter.Observer() {
            @Override
            public void notify(String name, NotificationCenter.Notification notification) {
                handleFetch(notification);
            }
        }, false);
        NotificationCenter.getInstance().addObserver(UserDataChangedNotification, new NotificationCenter.Observer() {
            @Override
            public void notify(String name, NotificationCenter.Notification notification) {
                handleStore(notification);
            }
        }, false);
    }

    private void setup() {
        setInstallationUUID(UUID.randomUUID().toString());

        setLangCode(PrimaryLangCode);
        if (!TextUtils.isEmpty(Locale.getDefault().getLanguage()) && BundleLangCodes.contains(Locale.getDefault().getLanguage())) {
            setLangCode(Locale.getDefault().getLanguage());
        }
        setGender(Enum.Gender.None);
        setBirthYear(0);
        setMatchMode(Enum.MatchMode.Open);
        setMatchHandshake(false);

        if (!SecureStore.appInitialized()) {
            SecureStore.setUseFingerprint(fingerprint);
        }
    }

    public void initialize(Callback callback) {
        Map<String, Object> userInfo = new HashMap<>();
        if (callback != null) {
            userInfo.put("callback", callback);
        }
        NotificationCenter.getInstance().post(UserDataFetchNotification, new NotificationCenter.Notification(userInfo));
    }

    public void touch(Callback callback) {
        Map<String, Object> userInfo = new HashMap<>();
        if (callback != null) {
            userInfo.put("callback", callback);
        }
        NotificationCenter.getInstance().post(UserDataChangedNotification, new NotificationCenter.Notification(userInfo));
    }

    public int addProfile(String name) {
        int index = 0;
        createProfile(name);
        touch(null);
        return index;
    }

    public Profile createProfile(String name) {
        Profile profile = new Profile(nextProfileId(), name);
        Analytics.getInstance().logChangeProfile(profile);
        profiles.add(0, profile);
        return profile;
    }

    public int removeProfile(final Profile profile) {
        for (Profile aProfile : profiles) {
            if (aProfile.id == profile.id) {
                if (currentProfileId == profile.id) {
                    currentProfileId = 0;
                }
                profiles.remove(profile);
                Analytics.getInstance().logRemoveProfile(profile);
                touch(null);
                return profiles.indexOf(profile);
            }
        }
        return -1;
    }

    public int renameProfile(final Profile profile, String name) {
        profile.name = name;
        Analytics.getInstance().logChangeProfile(profile);
        touch(null);
        return profiles.indexOf(profile);
    }

    public int copyProfile(final Profile profile, String name) {
        int index = 0;
        try {
            Profile profileCopy = Profile.fromJSON(profile.toJSON());
            profileCopy.id = nextProfileId();
            profileCopy.name = name;
            profileCopy.date = new Date();
            Analytics.getInstance().logAddProfile(profile);
            profiles.add(index, profileCopy);
            touch(null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return index;
    }

    public int changeMatchingModeProfile(final Profile profile, Enum.MatchMode matchMode) {
        profile.matchMode = matchMode;
        Analytics.getInstance().logChangeProfile(profile);
        touch(null);
        return profiles.indexOf(profile);
    }

    public int changeRelationType(Profile profile, Enum.RelationType relationType) {
        profile.relationType = relationType;
        Analytics.getInstance().logChangeProfile(profile);
        touch(null);
        return profiles.indexOf(profile);
    }

    public void setCurrentProfile(Profile profile) {
        currentProfileId = profile.id;
    }

    public void increaseInviteFriendSentCount() {
        inviteFriendSentCount++;
        touch(null);
    }

    public void sortProfiles() {
        Collections.sort(profiles, new Comparator<Profile>() {
            @Override
            public int compare(Profile profile1, Profile profile2) {
                return profile2.date.compareTo(profile1.date);
            }
        });
    }

    public void initStandardProfile() {
        if (profiles.size() == 0) {
            Profile profile = new Profile(nextProfileId(), DefaultProfileName);
            profiles.add(profile);
            currentProfileId = profile.id;
        }
    }

    public Profile currentProfile() {
        for (Profile profile : profiles) {
            if (profile.id == currentProfileId) {
                return profile;
            }
        }
        return null;
    }

    public Profile getProfile(int profileId) {
        for (Profile aProfile : profiles) {
            if (aProfile.id == profileId) {
                return aProfile;
            }
        }
        return null;
    }

    public int nextProfileId() {
        int maxId = 0;
        for (Profile profile : profiles) {
            maxId = Math.max(maxId, profile.id);
        }
        return maxId + 1;
    }

    public void addOwnTag(Tag tag) {
        tag.space = SecureStore.getSpaceRefName();
        ownTags.put(tag.key, tag);
    }

    public int addMatch(Match match) {
        if (installationExistsInDay(match.installationUUID)) {
            match.counted = false;
        } else {
            scoreMatchCount++;
            bothPosScore += match.bothPosScore;
            bothNegScore += match.bothNegScore;
            onlyPosScore += match.onlyPosScore;
            onlyNegScore += match.onlyNegScore;
        }
        history.add(match);
        touch(null);
        return history.indexOf(match);
    }

    public boolean installationExistsInDay(String installationUUID) {
        for (Match match : history) {
            if (match.installationUUID.equals(installationUUID) &&
                    Math.abs((System.currentTimeMillis() - match.date.getTime()) / 1000) <= 60 * 24 * 24) {
                return true;
            }
        }
        return false;
    }

    public void removeMatch(Match match) {
        history.remove(match);
        touch(null);
    }

    public Match getMatch(int index) {
        if (index >= 0 && index < history.size()) {
            return history.get(index);
        }
        return null;
    }

    public void clearHistory() {
        history.clear();
        touch(null);
    }

    public void addNewItemHash(String hash) {
        Integer newItemHash = newItemsHash.get(hash);
        if (newItemHash == null) {
            newItemsHash.put(hash, 1);
        } else {
            newItemsHash.put(hash, newItemHash + 1);
        }
    }

    public int hasNewItemHash(String hash) {
        Integer newItemHash = newItemsHash.get(hash);
        return newItemHash != null ? newItemHash : 0;
    }

    public void spaceSwitched(String space) {
        if (space.equals(StandardSpace)) {
            if (standardSettings.get("language") != null)  {
                this.langCode = (String)standardSettings.get("language");
            }
            if (standardSettings.get("matchCode") != null && (int)standardSettings.get("matchCode") >= 0) {
                this.matchMode = Enum.MatchMode.fromCode((int)standardSettings.get("matchCode"));
            }
            if (standardSettings.get("matchHandshake") != null) {
                this.matchHandshake = (Boolean)standardSettings.get("matchHandshake");
            }
        } else {
            standardSettings.put("language", this.langCode);
            standardSettings.put("matchCode", this.matchMode.code);
            standardSettings.put("matchHandshake", this.matchHandshake);
        }
        UserData.getInstance().touch(null);
    }

    private void handleStore(NotificationCenter.Notification notification) {
        Callback callback = (Callback) notification.userInfo.get("callback");
        store(callback);
    }

    private void handleFetch(NotificationCenter.Notification notification) {
        Callback callback = (Callback) notification.userInfo.get("callback");
        fetch(callback);
    }

    private void store(final Callback callback) {
        try {
            if (!initialized) {
                if (callback != null) {
                    callback.completion();
                }
                return;
            }
            String userDataString = toJSONString();
            SecureStore.getInstance().store(userDataString, new SecureStore.Delegate() {
                @Override
                public void onCancel() {
                    if (callback != null) {
                        callback.cancel();
                    }
                }

                @Override
                public void onComplete(String userData) {
                    if (userData != null) {
                        if (!SecureStore.appInitialized()) {
                            Analytics.getInstance().logFirstStart();
                            SecureStore.setAppInitialized();
                        }
                        if (callback != null) {
                            callback.completion();
                        }
                        NotificationCenter.getInstance().post(UserDataStoredNotification, null);
                    } else {
                        if (callback != null) {
                            callback.error();
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetch(final Callback callback) {
        if (initialized) {
            if (callback != null) {
                callback.completion();
            }
            return;
        }
        if (SecureStore.appInitialized()) {
            SecureStore.getInstance().load(new SecureStore.Delegate() {
                @Override
                public void onCancel() {
                    if (callback != null) {
                        callback.cancel();
                    }
                }

                @Override
                public void onComplete(String userData) {
                    try {
                        if (userData != null) {
                            JSONObject json = new JSONObject(userData);

                            if (!TextUtils.isEmpty(json.getString("i"))) {
                                installationUUID = json.getString("i");
                            }
                            langCode = json.getString("l");
                            firstName = json.getString("n");
                            gender = Enum.Gender.fromCode(json.getString("g") != null ? json.getString("g") : Enum.Gender.None.code);
                            birthYear = json.getInt("y") >= BaseYear ? json.getInt("y") : 0;
                            status = json.getString("s");
                            matchMode = Enum.MatchMode.fromCode(json.getInt("m") >= 0 ? json.getInt("m") : Enum.MatchMode.Open.code);
                            matchVibrate = json.getBoolean("mv");
                            matchPlayGreeting = json.getBoolean("mp");
                            matchHandshake = json.getBoolean("mh");
                            greetingVoice = json.getString("gv") != null ? Utilities.fromBase64(json.getString("gv")) : null;
                            fingerprint = json.getBoolean("t");
                            passcodeTimeout = Enum.PasscodeTimeout.fromCode(json.getInt("pt") >= 0 ? json.getInt("pt") : Enum.PasscodeTimeout.Min5.code);
                            inviteFriendSentCount = json.getInt("f") >= 0 ? json.getInt("f") : 0;
                            currentProfileId = json.getInt("pi") >= 0 ? json.getInt("pi") : 0;
                            newItemsHash = new HashMap<>();
                            for (Iterator<String> it = json.getJSONObject("nh").keys(); it.hasNext(); ) {
                                String key = it.next();
                                newItemsHash.put(key, json.getJSONObject("nh").getInt(key));
                            }
                            ownTags = new HashMap<>();
                            for (int i = 0; i < json.getJSONArray("ot").length(); i++) {
                                Tag tag = Tag.fromJSON(json.getJSONArray("ot").getJSONObject(i));
                                ownTags.put(tag.key, tag);
                            }
                            profiles = new ArrayList<>();
                            for (int i = 0; i < json.getJSONArray("p").length(); i++) {
                                profiles.add(Profile.fromJSON(json.getJSONArray("p").getJSONObject(i)));

                            }
                            history = new ArrayList<>();
                            for (int i = 0; i < json.getJSONArray("h").length(); i++) {
                                history.add(Match.fromJSON(json.getJSONArray("h").getJSONObject(i)));
                            }
                            scoreMatchCount = json.getLong("smc") >= 0 ? json.getLong("smc") : 0;
                            bothPosScore = json.getLong("sbp") >= 0 ? json.getLong("sbp") : 0;
                            bothNegScore = json.getLong("sbn") >= 0 ? json.getLong("sbn") : 0;
                            onlyPosScore = json.getLong("sop") >= 0 ? json.getLong("sop") : 0;
                            onlyNegScore = json.getLong("son") >= 0 ? json.getLong("son") : 0;
                            highscoreLocal = json.has("hl") && json.getString("hl") != null ? json.getString("hl") : "";
                            localScore = json.getLong("sl") >= 0 ? json.getLong("sl") : 0;
                            scoreLocalCount = json.has("slc") && json.getLong("slc") >= 0 ? json.getLong("slc") : 0;
                            qrHelpFirstShown = json.getBoolean("qhs");
                            standardSettings = new HashMap<>();
                            if (json.has("ss")) {
                                for (Iterator<String> it = json.getJSONObject("ss").keys(); it.hasNext(); ) {
                                    String key = it.next();
                                    standardSettings.put(key, json.getJSONObject("ss").get(key));
                                }
                            }

                            setInitialized(true);
                        } else {
                            recover();
                        }

                        sortProfiles();

                        if (callback != null) {
                            callback.completion();
                        }
                        NotificationCenter.getInstance().post(UserDataFetchedNotification);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (callback != null) {
                            callback.error();
                        }
                    }
                }
            });
        } else {
            sortProfiles();
            if (callback != null) {
                callback.completion();
            }
        }
    }

    public void recover() {
        clear();
        setup();
        setInitialized(true);
    }

    public void clear() {
        SecureStore.getInstance().close();
        initialized = false;

        this.installationUUID = "";
        this.langCode = PrimaryLangCode;
        this.firstName = "";
        this.gender = Enum.Gender.None;
        this.birthYear = 0;
        this.status = "";
        this.matchMode = Enum.MatchMode.Open;
        this.matchVibrate = Utilities.vibrateEnabled(AppDelegate.getInstance().Context);
        this.matchPlayGreeting = true;
        this.matchHandshake = false;
        this.greetingVoice = null;
        this.fingerprint = false;
        this.passcodeTimeout = Enum.PasscodeTimeout.Min5;
        this.inviteFriendSentCount = 0;
        this.currentProfileId = 0;
        this.newItemsHash = new HashMap<>();
        this.scoreMatchCount = 0;
        this.bothPosScore = 0;
        this.bothNegScore = 0;
        this.onlyPosScore = 0;
        this.onlyNegScore = 0;
        this.highscoreLocal = "";
        this.localScore = 0;
        this.scoreLocalCount = 0;
        this.qrHelpFirstShown = false;
        this.standardSettings = new HashMap<>();
        this.ownTags = new HashMap<>();
        this.profiles = new ArrayList<>();
        this.history = new ArrayList<>();

        NotificationCenter.getInstance().post(UserDataClearedNotification, null);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject newItemsHashJSON = new JSONObject();
        for (String key : newItemsHash.keySet()) {
            newItemsHashJSON.put(key, newItemsHash.get(key));
        }
        JSONObject standardSettingsJSON = new JSONObject();
        for (String key : standardSettings.keySet()) {
            standardSettingsJSON.put(key, standardSettings.get(key));
        }
        JSONArray ownTagsJSON = new JSONArray();
        for (Tag tag : ownTags.values()) {
            ownTagsJSON.put(tag.toJSON());
        }
        JSONArray profilesJSON = new JSONArray();
        for (Profile profile : profiles) {
            profilesJSON.put(profile.toJSON());
        }
        JSONArray historyJSON = new JSONArray();
        for (Match match : history) {
            historyJSON.put(match.toJSON());
        }
        JSONObject json = new JSONObject();
        json.put("i", installationUUID);
        json.put("l", langCode);
        json.put("n", firstName);
        json.put("g", gender.code);
        json.put("y", birthYear);
        json.put("s", status);
        json.put("m", matchMode.code);
        json.put("mv", matchVibrate);
        json.put("mp", matchPlayGreeting);
        json.put("mh", matchHandshake);
        json.put("gv", greetingVoice != null ? Utilities.toBase64(greetingVoice) : "");
        json.put("t", fingerprint);
        json.put("pt", passcodeTimeout.code);
        json.put("f", inviteFriendSentCount);
        json.put("pi", currentProfileId);
        json.put("nh", newItemsHashJSON);
        json.put("smc", scoreMatchCount);
        json.put("sbp", bothPosScore);
        json.put("sbn", bothNegScore);
        json.put("sop", onlyPosScore);
        json.put("son", onlyNegScore);
        json.put("hl", highscoreLocal);
        json.put("sl", localScore);
        json.put("slc", scoreLocalCount);
        json.put("qhs", qrHelpFirstShown);
        json.put("ss", standardSettingsJSON);
        json.put("ot", ownTagsJSON);
        json.put("p", profilesJSON);
        json.put("h", historyJSON);
        return json;
    }

    public String toJSONString() throws JSONException {
        return toJSON().toString(0);
    }
}