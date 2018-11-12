package de.oklemenz.sayhi.service;

import android.os.Bundle;
import android.text.TextUtils;

import com.google.firebase.analytics.FirebaseAnalytics;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.model.Category;
import de.oklemenz.sayhi.model.Match;
import de.oklemenz.sayhi.model.Profile;
import de.oklemenz.sayhi.model.Settings;
import de.oklemenz.sayhi.model.StageCategory;
import de.oklemenz.sayhi.model.StageTag;
import de.oklemenz.sayhi.model.Tag;
import de.oklemenz.sayhi.model.UserData;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class Analytics {

    private static Analytics instance = new Analytics();

    public static Analytics getInstance() {
        return instance;
    }

    private Bundle parameters(Profile profile) {
        Bundle parameters = new Bundle();
        String matchMode = profile.matchMode != null ? profile.matchMode.toExternalString() : "";
        int matchCode = profile.matchMode != null ? profile.matchMode.code : -1;
        parameters.putString("profileMatchMode", matchMode);
        parameters.putInt("profileMatchCode", matchCode);
        parameters.putString("profileRelationType", profile.relationType.code);
        parameters.putInt("profilePosTagCount", profile.posTags.size());
        parameters.putInt("profileNegTagCount", profile.negTags.size());
        return parameters;
    }

    private Bundle parameters(Tag tag) {
        Bundle parameters = new Bundle();
        parameters.putString("tagKey", tag.key);
        parameters.putString("tagName", tag.getName());
        parameters.putString("tagPrimaryLangKey", tag.primaryLangKey);
        parameters.putString("tagRefKey", tag.refKey != null ? tag.refKey : "");
        parameters.putString("tagRefPrimaryLangKey", tag.getRefPrimaryLangKey() != null ? tag.getRefPrimaryLangKey() : "");
        parameters.putString("tagEffectiveKey", tag.getEffectiveKey());
        parameters.putString("categoryKey", tag.getCategoryKey());
        parameters.putString("categoryName", tag.getCategory() != null ? tag.getCategory().getName() : "");
        parameters.putString("categoryPrimaryLangKey", tag.getCategory() != null ? tag.getCategory().primaryLangKey : "");
        parameters.putString("categoryRefKey", tag.getCategory() != null ? tag.getCategory().refKey : "");
        parameters.putString("categoryRefPrimLangKey", tag.getCategory() != null ? tag.getCategory().getRefPrimaryLangKey() : "");
        return parameters;
    }

    private Bundle parameters(Category category) {
        Bundle parameters = new Bundle();
        parameters.putString("categoryKey", category.key);
        parameters.putString("categoryName", category.getName());
        parameters.putString("categoryPrimaryLangKey", category.primaryLangKey);
        parameters.putString("categoryRefKey", category.refKey != null ? category.refKey : "");
        parameters.putString("categoryRefPrimLangKey", category.getRefPrimaryLangKey() != null ? category.getRefPrimaryLangKey() : "");
        parameters.putString("categoryEffectiveKey", category.getEffectiveKey());
        return parameters;
    }

    private Bundle parameters(StageTag tag) {
        Bundle parameters = new Bundle();
        parameters.putString("tagKey", tag.key);
        parameters.putString("tagName", tag.name);
        parameters.putString("categoryKey", tag.categoryKey);
        parameters.putString("categoryName", tag.categoryName);
        parameters.putLong("counter", tag.counter);
        return parameters;
    }

    private Bundle parameters(StageCategory category) {
        Bundle parameters = new Bundle();
        parameters.putString("categoryKey", category.key);
        parameters.putString("categoryName", category.name);
        parameters.putLong("counter", category.counter);
        return parameters;
    }

    private Bundle parameters(Match match) {
        Bundle parameters = new Bundle();
        parameters.putString("matchDate", Utilities.getISO8601StringForDate(match.date));
        parameters.putString("matchMode", match.mode.toString());
        parameters.putInt("matchCode", match.mode.code);
        parameters.putBoolean("matchHandshake", match.handshake);
        parameters.putString("profileRelationType", match.relationType.code);
        parameters.putInt("profilePosTagCount", match.profilePosTagCount);
        parameters.putInt("profileNegTagCount", match.profileNegTagCount);
        parameters.putString("locationLongitude", match.locationLongitude);
        parameters.putString("locationLatitude", match.locationLatitude);
        parameters.putString("locationStreet", match.locationStreet);
        parameters.putString("locationCity", match.locationCity);
        parameters.putString("locationCountry", match.locationCountry);
        parameters.putString("messageLanguage", match.langCode);
        parameters.putString("messageGender", match.gender.code);
        parameters.putInt("messageBirthYear", match.birthYear);
        parameters.putInt("messageAge", match.age());
        parameters.putString("messageInstallation", match.installationUUID);
        parameters.putInt("messagePosTagCount", match.messagePosTagCount);
        parameters.putInt("messageNegTagCount", match.messageNegTagCount);
        parameters.putString("matchLeftLeftTagKeys", TextUtils.join(",", match.getBothPosTagEffectiveKeys()));
        parameters.putString("matchRightRightTagKeys", TextUtils.join(",", match.getBothNegTagEffectiveKeys()));
        parameters.putString("matchLeftRightTagKeys", TextUtils.join(",", match.getOnlyPosTagEffectiveKeys()));
        parameters.putString("matchRightLeftTagKeys", TextUtils.join(",", match.getOnlyNegTagEffectiveKeys()));
        return parameters;
    }

    public void logFirstStart() {
        Bundle parameters = new Bundle();
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("first_start", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            DataService.getInstance().logEvent("first_start", parameters);
        }
    }

    public void logAddProfile(Profile profile) {
        Bundle parameters = parameters(profile);
        parameters.putLong(FirebaseAnalytics.Param.VALUE, 1);
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("profile", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            DataService.getInstance().logEvent("profile", parameters);
        }
    }

    public void logChangeProfile(Profile profile) {
        Bundle parameters = parameters(profile);
        parameters.putLong(FirebaseAnalytics.Param.VALUE, 0);
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("profile", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            DataService.getInstance().logEvent("profile", parameters);
        }
    }

    public void logRemoveProfile(Profile profile) {
        Bundle parameters = parameters(profile);
        parameters.putLong(FirebaseAnalytics.Param.VALUE, -1);
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("profile", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            DataService.getInstance().logEvent("profile", parameters);
        }
    }

    public void logPositive(Tag tag, long previousValue) {
        Bundle parameters = parameters(tag);
        parameters.putLong(FirebaseAnalytics.Param.VALUE, 1);
        parameters.putLong("previousValue", previousValue);
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("tag_assign", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            DataService.getInstance().logEvent("tag_assign", parameters);
        }
    }

    public void logNegative(Tag tag, long previousValue) {
        Bundle parameters = parameters(tag);
        parameters.putLong(FirebaseAnalytics.Param.VALUE, -1);
        parameters.putLong("previousValue", previousValue);
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("tag_assign", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            DataService.getInstance().logEvent("tag_assign", parameters);
        }
    }

    public void logNeutral(Tag tag, long previousValue) {
        Bundle parameters = parameters(tag);
        parameters.putLong(FirebaseAnalytics.Param.VALUE, 0);
        parameters.putLong("previousValue", previousValue);
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("tag_assign", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            DataService.getInstance().logEvent("tag_assign", parameters);
        }
    }

    public void logMatch(Match match, String session, String messageSession) {
        Bundle parameters = parameters(match);
        parameters.putString("matchSession", session);
        parameters.putString("messageSession", messageSession);
        parameters.putString("matchLeftLeftTagKeys", Utilities.prefix(parameters.getString("matchLeftLeftTagKeys"), 100));
        parameters.putString("matchRightRightTagKeys", Utilities.prefix(parameters.getString("matchRightRightTagKeys"), 100));
        parameters.putString("matchLeftRightTagKeys", Utilities.prefix(parameters.getString("matchLeftRightTagKeys"), 100));
        parameters.putString("matchRightLeftTagKeys", Utilities.prefix(parameters.getString("matchRightLeftTagKeys"), 100));
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("match", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            parameters = parameters(match);
            DataService.getInstance().logEvent("match", parameters);
        }
    }

    public void logNewTag(StageTag tag) {
        Bundle parameters = parameters(tag);
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("tag_new", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            DataService.getInstance().logEvent("tag_new", parameters);
        }
    }

    public void logNewCategory(StageCategory category) {
        Bundle parameters = parameters(category);
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("category_new", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            DataService.getInstance().logEvent("category_new", parameters);
        }
    }

    public void logInviteFriend() {
        Bundle parameters = new Bundle();
        parameters.putInt("count", UserData.getInstance().inviteFriendSentCount);
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("invite_friend", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            DataService.getInstance().logEvent("invite_friend", parameters);
        }
    }

    public void logRateApp() {
        Bundle parameters = new Bundle();
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("rate_app", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            DataService.getInstance().logEvent("rate_app", parameters);
        }
    }

    public void logSupportMail() {
        Bundle parameters = new Bundle();
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("support_feedback", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            DataService.getInstance().logEvent("support_feedback", parameters);
        }
    }

    public void logRecordVoice() {
        Bundle parameters = new Bundle();
        parameters.putLong(FirebaseAnalytics.Param.VALUE, 1);
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("record_voice", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            DataService.getInstance().logEvent("record_voice", parameters);
        }
    }

    public void logRerecordVoice() {
        Bundle parameters = new Bundle();
        parameters.putLong(FirebaseAnalytics.Param.VALUE, 0);
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("record_voice", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            DataService.getInstance().logEvent("record_voice", parameters);
        }
    }

    public void logRemoveRecordedVoice() {
        Bundle parameters = new Bundle();
        parameters.putLong(FirebaseAnalytics.Param.VALUE, -1);
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("record_voice", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            DataService.getInstance().logEvent("record_voice", parameters);
        }
    }

    public void logSpaceSwitched(String space) {
        Bundle parameters = new Bundle();
        parameters.putString("space", space);
        if (!Settings.getInstance().getDisableRecordAnalytics()) {
            FirebaseAnalytics.getInstance(AppDelegate.getInstance().Context).logEvent("space_switch", parameters);
        }
        if (!Settings.getInstance().getDisableRecordAnalyticsDB()) {
            DataService.getInstance().logEvent("space_switch", parameters);
        }
    }
}