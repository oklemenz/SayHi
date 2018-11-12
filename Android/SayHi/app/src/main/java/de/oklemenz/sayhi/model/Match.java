package de.oklemenz.sayhi.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import de.oklemenz.sayhi.service.Utilities;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class Match implements Serializable {

    private int year = Calendar.getInstance().get(Calendar.YEAR);

    public Date date = new Date();

    public Enum.MatchMode mode = Enum.MatchMode.Open;
    public boolean handshake = false;
    public String profileName = "";
    public String matchLangCode = "";
    public Enum.RelationType relationType = Enum.RelationType.None;
    public int profilePosTagCount = 0;
    public int profileNegTagCount = 0;

    public String locationLatitude = "";
    public String locationLongitude = "";
    public String locationName = "";
    public String locationStreet = "";
    public String locationCity = "";
    public String locationCountry = "";

    public String langCode = "";
    public String firstName = "";
    public Enum.Gender gender = Enum.Gender.None;
    public int birthYear = 0;
    public String status = "";
    public String installationUUID = "";
    public int messagePosTagCount = 0;
    public int messageNegTagCount = 0;

    public List<Tag> bothPosTags = new ArrayList<>();
    public List<Tag> bothNegTags = new ArrayList<>();
    public List<Tag> onlyPosTags = new ArrayList<>();
    public List<Tag> onlyNegTags = new ArrayList<>();

    public long bothPosScore = 0;
    public long bothNegScore = 0;
    public long onlyPosScore = 0;
    public long onlyNegScore = 0;

    public boolean counted = true;

    public long score() {
        return bothPosScore + bothNegScore + onlyPosScore + onlyNegScore;
    }

    public int age() {
        if (birthYear >= UserData.BaseYear && birthYear <= year) {
            return year - birthYear;
        }
        return 0;
    }

    public List<String> getBothPosTagKeys() {
        List<String> bothPosTagKeys = new ArrayList<>();
        for (Tag tag : bothPosTags) {
            bothPosTagKeys.add(tag.key);
        }
        return bothPosTagKeys;
    }

    public List<String> getBothNegTagKeys() {
        List<String> bothNegTagKeys = new ArrayList<>();
        for (Tag tag : bothNegTags) {
            bothNegTagKeys.add(tag.key);
        }
        return bothNegTagKeys;
    }

    public List<String> getOnlyPosTagKeys() {
        List<String> onlyPosTagKeys = new ArrayList<>();
        for (Tag tag : onlyPosTags) {
            onlyPosTagKeys.add(tag.key);
        }
        return onlyPosTagKeys;
    }

    public List<String> getOnlyNegTagKeys() {
        List<String> onlyNegTagKeys = new ArrayList<>();
        for (Tag tag : onlyNegTags) {
            onlyNegTagKeys.add(tag.key);
        }
        return onlyNegTagKeys;
    }

    public List<String> getBothPosTagEffectiveKeys() {
        List<String> bothPosTagKeys = new ArrayList<>();
        for (Tag tag : bothPosTags) {
            bothPosTagKeys.add(tag.getEffectiveKey());
        }
        return bothPosTagKeys;
    }

    public List<String> getBothNegTagEffectiveKeys() {
        List<String> bothNegTagKeys = new ArrayList<>();
        for (Tag tag : bothNegTags) {
            bothNegTagKeys.add(tag.getEffectiveKey());
        }
        return bothNegTagKeys;
    }

    public List<String> getOnlyPosTagEffectiveKeys() {
        List<String> onlyPosTagKeys = new ArrayList<>();
        for (Tag tag : onlyPosTags) {
            onlyPosTagKeys.add(tag.getEffectiveKey());
        }
        return onlyPosTagKeys;
    }

    public List<String> getOnlyNegTagEffectiveKeys() {
        List<String> onlyNegTagKeys = new ArrayList<>();
        for (Tag tag : onlyNegTags) {
            onlyNegTagKeys.add(tag.getEffectiveKey());
        }
        return onlyNegTagKeys;
    }

    public List<String> getBothPosTagNames() {
        List<String> bothPosTagNames = new ArrayList<>();
        for (Tag tag : bothPosTags) {
            bothPosTagNames.add(tag.getName());
        }
        return bothPosTagNames;
    }

    public List<String> getBothNegTagNames() {
        List<String> bothNegTagNames = new ArrayList<>();
        for (Tag tag : bothNegTags) {
            bothNegTagNames.add(tag.getName());
        }
        return bothNegTagNames;
    }

    public List<String> getOnlyPosTagNames() {
        List<String> onlyPosTagNames = new ArrayList<>();
        for (Tag tag : onlyPosTags) {
            onlyPosTagNames.add(tag.getName());
        }
        return onlyPosTagNames;
    }

    public List<String> getOnlyNegTagNames() {
        List<String> onlyNegTagNames = new ArrayList<>();
        for (Tag tag : onlyNegTags) {
            onlyNegTagNames.add(tag.getName());
        }
        return onlyNegTagNames;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("d", Utilities.getISO8601StringForDate(date));
        json.put("m", mode.code);
        json.put("mh", handshake);
        json.put("p", profileName);
        json.put("ml", matchLangCode);
        json.put("r", relationType.code);
        json.put("ppt", profilePosTagCount);
        json.put("pnt", profileNegTagCount);
        json.put("la", locationLatitude);
        json.put("lo", locationLongitude);
        json.put("ln", locationName);
        json.put("ls", locationStreet);
        json.put("lci", locationCity);
        json.put("lco", locationCountry);
        json.put("l", langCode);
        json.put("n", firstName);
        json.put("g", gender.code);
        json.put("y", birthYear);
        json.put("s", status);
        json.put("i", installationUUID);
        json.put("mpt", messagePosTagCount);
        json.put("mnt", messageNegTagCount);
        json.put("bpt", new JSONArray(getBothPosTagKeys()));
        json.put("bnt", new JSONArray(getBothNegTagKeys()));
        json.put("opt", new JSONArray(getOnlyPosTagKeys()));
        json.put("ont", new JSONArray(getOnlyNegTagKeys()));
        json.put("sbp", bothPosScore);
        json.put("sbn", bothNegScore);
        json.put("sop", onlyPosScore);
        json.put("son", onlyNegScore);
        json.put("c", counted);
        return json;
    }

    public String toJSONString() throws JSONException {
        return toJSON().toString(0);
    }

    public static Match fromJSON(JSONObject json) throws JSONException {
        Match match = new Match();

        match.date = Utilities.parseISO8601String(json.getString("d"));

        match.mode = Enum.MatchMode.fromCode(json.getInt("m"));
        match.handshake = json.getBoolean("mh");
        match.profileName = json.getString("p");
        match.matchLangCode = json.has("ml") ? json.getString("ml") : "";
        match.relationType = json.getString("r") != null ? Enum.RelationType.fromCode(json.getString("r")) : Enum.RelationType.None;
        match.profilePosTagCount = json.getInt("ppt");
        match.profileNegTagCount = json.getInt("pnt");
        match.locationLatitude = json.getString("la");
        match.locationLongitude = json.getString("lo");
        match.locationName = json.getString("ln");
        match.locationStreet = json.getString("ls");
        match.locationCity = json.getString("lci");
        match.locationCountry = json.getString("lco");

        match.langCode = json.getString("l");
        match.firstName = json.getString("n");
        match.gender = json.getString("g") != null ? Enum.Gender.fromCode(json.getString("g")) : Enum.Gender.None;
        match.birthYear = json.getInt("y") >= UserData.BaseYear ? json.getInt("y") : 0;
        match.status = json.getString("s");
        match.installationUUID = json.getString("i");
        match.messagePosTagCount = json.getInt("mpt");
        match.messageNegTagCount = json.getInt("mnt");

        JSONArray bothPosTagKeys = json.getJSONArray("bpt");
        for (int i = 0; i < bothPosTagKeys.length(); i++) {
            Tag tag = Tag.lookupKey(bothPosTagKeys.getString(i));
            if (tag != null) {
                match.bothPosTags.add(tag);
            }
        }

        JSONArray bothNegTagKeys = json.getJSONArray("bnt");
        for (int i = 0; i < bothNegTagKeys.length(); i++) {
            Tag tag = Tag.lookupKey(bothNegTagKeys.getString(i));
            if (tag != null) {
                match.bothNegTags.add(tag);
            }
        }

        JSONArray onlyPosTagKeys = json.getJSONArray("opt");
        for (int i = 0; i < onlyPosTagKeys.length(); i++) {
            Tag tag = Tag.lookupKey(onlyPosTagKeys.getString(i));
            if (tag != null) {
                match.onlyPosTags.add(tag);
            }
        }

        JSONArray onlyNegTagKeys = json.getJSONArray("ont");
        for (int i = 0; i < onlyNegTagKeys.length(); i++) {
            Tag tag = Tag.lookupKey(onlyNegTagKeys.getString(i));
            if (tag != null) {
                match.onlyNegTags.add(tag);
            }
        }

        match.bothPosScore = json.getLong("sbp");
        match.bothNegScore = json.getLong("sbn");
        match.onlyPosScore = json.getLong("sop");
        match.onlyNegScore = json.getLong("son");

        match.counted = json.getBoolean("c");

        return match;
    }

    public static Match fromJSONString(String jsonString) throws JSONException {
        return fromJSON(new JSONObject(jsonString));
    }

    public static Match calculateMatch(Profile profile, final Message message) {
        Match match = new Match();
        match.date = new Date();

        match.mode = Enum.MatchMode.fromCode(Math.min(profile.matchMode != null ? profile.matchMode.code : UserData.getInstance().getMatchMode().code, message.matchMode.code));
        if (Settings.getInstance().getDisableSettingsMatchMode() && Settings.getInstance().getSettingsMatchMode() != null) {
            match.mode = Settings.getInstance().getSettingsMatchMode();
        }
        match.handshake = UserData.getInstance().getMatchHandshake() || message.matchHandshake;
        match.profileName = profile.name;
        match.matchLangCode = UserData.getInstance().getLangCode();
        match.relationType = profile.relationType;
        match.profilePosTagCount = profile.posTags.size();
        match.profileNegTagCount = profile.negTags.size();

        match.langCode = message.langCode;
        match.firstName = message.firstName;
        match.gender = message.gender;
        match.birthYear = message.birthYear;
        match.status = message.status;
        match.messagePosTagCount = message.posTags.size();
        match.messageNegTagCount = message.negTags.size();

        for (Tag tag : profile.posTags) {
            if (message.posTags.contains(tag.getEffectiveKey())) {
                match.bothPosTags.add(tag);
            }
        }
        Collections.sort(match.bothPosTags, new Comparator<Tag>() {
            @Override
            public int compare(Tag tag1, Tag tag2) {
                return Math.max(message.posTags.indexOf(tag1.getEffectiveKey()), 0) - Math.max(message.posTags.indexOf(tag2.getEffectiveKey()), 0);
            }
        });

        for (Tag tag : profile.negTags) {
            if (message.negTags.contains(tag.getEffectiveKey())) {
                match.bothNegTags.add(tag);
            }
        }
        Collections.sort(match.bothNegTags, new Comparator<Tag>() {
            @Override
            public int compare(Tag tag1, Tag tag2) {
                return Math.max(message.negTags.indexOf(tag1.getEffectiveKey()), 0) - Math.max(message.negTags.indexOf(tag2.getEffectiveKey()), 0);
            }
        });

        if (match.mode == Enum.MatchMode.Adapt || match.mode == Enum.MatchMode.Open) {
            for (Tag tag : profile.posTags) {
                if (message.negTags.contains(tag.getEffectiveKey())) {
                    match.onlyPosTags.add(tag);
                }
            }
            Collections.sort(match.onlyPosTags, new Comparator<Tag>() {
                @Override
                public int compare(Tag tag1, Tag tag2) {
                    return Math.max(message.negTags.indexOf(tag1.getEffectiveKey()), 0) - Math.max(message.negTags.indexOf(tag2.getEffectiveKey()), 0);
                }
            });
        }

        if (match.mode == Enum.MatchMode.Tries || match.mode == Enum.MatchMode.Open) {
            for (Tag tag : profile.negTags) {
                if (message.posTags.contains(tag.getEffectiveKey())) {
                    match.onlyNegTags.add(tag);
                }
            }
            Collections.sort(match.onlyNegTags, new Comparator<Tag>() {
                @Override
                public int compare(Tag tag1, Tag tag2) {
                    return Math.max(message.posTags.indexOf(tag1.getEffectiveKey()), 0) - Math.max(message.posTags.indexOf(tag2.getEffectiveKey()), 0);
                }
            });
        }

        if (Settings.getInstance().getScoreCount().contains("leftLeft")) {
            match.bothPosScore = match.bothPosTags.size();
        }
        if (Settings.getInstance().getScoreCount().contains("rightRight")) {
            match.bothNegScore = match.bothNegTags.size();
        }
        if (Settings.getInstance().getScoreCount().contains("leftRight")) {
            match.onlyPosScore = match.onlyPosTags.size();
        }
        if (Settings.getInstance().getScoreCount().contains("rightLeft")) {
            match.onlyNegScore = match.onlyNegTags.size();
        }

        return match;
    }
}
