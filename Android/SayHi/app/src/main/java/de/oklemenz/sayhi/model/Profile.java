package de.oklemenz.sayhi.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.oklemenz.sayhi.service.Cache;
import de.oklemenz.sayhi.service.Utilities;

/**
 * Created by Oliver Klemenz on 31.10.16.
 */

public class Profile implements Serializable {

    public int id = 0;
    public String name = "";
    public Enum.RelationType relationType = Enum.RelationType.None;
    public Enum.MatchMode matchMode = null;
    public Date date = new Date();

    public List<Tag> posTags = new ArrayList<>();
    public List<Tag> negTags = new ArrayList<>();

    public Profile(int id, String name) {
        this.id = id;
        this.name = name;
        this.date = new Date();
    }

    public List<String> getPosTagKeys() {
        List<String> posTagKeys = new ArrayList<>();
        for (Tag tag : posTags) {
            posTagKeys.add(tag.key);
        }
        return posTagKeys;
    }

    public List<String> getNegTagKeys() {
        List<String> negTagKeys = new ArrayList<>();
        for (Tag tag : negTags) {
            negTagKeys.add(tag.key);
        }
        return negTagKeys;
    }

    public List<String> getPosTagEffectiveKeys() {
        List<String> posTagKeys = new ArrayList<>();
        for (Tag tag : posTags) {
            posTagKeys.add(tag.getEffectiveKey());
        }
        return posTagKeys;
    }

    public List<String> getNegTagEffectiveKeys() {
        List<String> negTagKeys = new ArrayList<>();
        for (Tag tag : negTags) {
            negTagKeys.add(tag.getEffectiveKey());
        }
        return negTagKeys;
    }

    public Enum.MatchMode getEffectiveMatchMode() {
        Enum.MatchMode matchMode = this.matchMode != null ? this.matchMode : UserData.getInstance().getMatchMode();
        if (Settings.getInstance().getDisableSettingsMatchMode() && Settings.getInstance().getSettingsMatchMode() != null) {
            matchMode = Settings.getInstance().getSettingsMatchMode();
        }
        return matchMode;
    }

    public void touch(UserData.Callback callback) {
        this.date = new Date();
        UserData.getInstance().touch(callback);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("i", id);
        json.put("n", name);
        json.put("r", relationType.code);
        json.put("m", matchMode != null ? matchMode.code : -1);
        json.put("d", Utilities.getISO8601StringForDate(date));
        json.put("pt", new JSONArray(getPosTagKeys()));
        json.put("nt", new JSONArray(getNegTagKeys()));
        return json;
    }

    public String toJSONString() throws JSONException {
        return toJSON().toString(0);
    }

    public static Profile fromJSON(JSONObject json) throws JSONException {
        Profile profile = new Profile(
                json.getInt("i"),
                json.getString("n"));
        profile.relationType = json.getString("r") != null ? Enum.RelationType.fromCode(json.getString("r")) : Enum.RelationType.None;
        int matchModeInt = json.getInt("m");
        if (matchModeInt >= 0) {
            profile.matchMode = Enum.MatchMode.fromCode(matchModeInt);
        }
        profile.date = Utilities.parseISO8601String(json.getString("d"));

        JSONArray posTagKeys = json.getJSONArray("pt");
        for (int i = 0; i < posTagKeys.length(); i++) {
            Tag tag = Cache.getInstance().lookupTag(posTagKeys.getString(i));
            if (tag != null) {
                profile.posTags.add(tag);
            }
        }

        JSONArray negTagKeys = json.getJSONArray("nt");
        for (int i = 0; i < negTagKeys.length(); i++) {
            Tag tag = Cache.getInstance().lookupTag(negTagKeys.getString(i));
            if (tag != null) {
                profile.negTags.add(tag);
            }
        }

        return profile;
    }

    public static Profile fromJSONString(String jsonString) throws JSONException {
        return fromJSON(new JSONObject(jsonString));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Profile) {
            if (((Profile) obj).id == this.id) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public String toString() {
        return name;
    }
}