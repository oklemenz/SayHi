package de.oklemenz.sayhi.model;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import de.oklemenz.sayhi.service.Cache;
import de.oklemenz.sayhi.service.Utilities;

/**
 * Created by Oliver Klemenz on 31.10.16.
 */

public class Tag implements Serializable {

    public static String TagStagedIcon = "tag";

    public String key = "";
    public String langCode = "";
    private String name = "";

    public void setName(String name) {
        this.name = name;
        this.search = Utilities.searchNormalized(name, langCode);
    }

    public String getName() {
        return this.name;
    }

    private String categoryKey;

    public void setCategoryKey(String categoryKey) {
        this.categoryKey = categoryKey;
        this.category = null;
    }

    public String getCategoryKey() {
        return this.categoryKey;
    }

    public String primaryLangKey = "";
    public String refKey = "";
    private String refPrimaryLangKey;

    public void setRefPrimaryLangKey(String refPrimaryLangKey) {
        this.refPrimaryLangKey = refPrimaryLangKey;
    }

    public String getRefPrimaryLangKey() {
        return this.refPrimaryLangKey != null ? this.refPrimaryLangKey : this.refKey;
    }

    public String search = "";
    public boolean favorite = false;
    public boolean selected = false;
    public boolean stage = false;

    public String space = "";

    public Tag(String key, String langCode, String name, String categoryKey, String primaryLangKey) {
        this(key, langCode, name, categoryKey, primaryLangKey, null, null);
    }

    public Tag(String key, String langCode, String name, String categoryKey, String primaryLangKey, String refKey, String refPrimaryLangKey) {
        this(key, langCode, name, categoryKey, primaryLangKey, refKey, refPrimaryLangKey, false);
    }

    public Tag(String key, String langCode, String name, String categoryKey, String primaryLangKey, String refKey, String refPrimaryLangKey, boolean favorite) {
        this.key = key;
        this.langCode = langCode;
        this.setName(name);
        this.categoryKey = categoryKey;
        this.primaryLangKey = primaryLangKey;
        if (refKey != null) {
            this.refKey = refKey;
        }
        if (refPrimaryLangKey != null) {
            this.refPrimaryLangKey = refPrimaryLangKey;
        }
        this.favorite = favorite;
    }

    public Category category;

    public Category getCategory() {
        if (!TextUtils.isEmpty(categoryKey)) {
            if (category == null) {
                category = Cache.getInstance().lookupCategory(categoryKey);
            }
            return category;
        }
        return null;
    }

    public Tag primaryLangTag;

    public Tag getPrimaryLangTag() {
        if (!TextUtils.isEmpty(primaryLangKey)) {
            if (primaryLangTag == null) {
                primaryLangTag = Cache.getInstance().lookupTag(primaryLangKey);
            }
            return primaryLangTag;
        }
        return null;
    }

    public String getEffectiveKey() {
        if (refPrimaryLangKey != null) {
            return refPrimaryLangKey;
        }
        return !TextUtils.isEmpty(primaryLangKey) ? primaryLangKey : key;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("k", key);
        json.put("l", langCode);
        json.put("n", name);
        json.put("c", categoryKey);
        json.put("p", primaryLangKey);
        json.put("r", refKey != null ? refKey : "");
        json.put("rp", refPrimaryLangKey != null ? refPrimaryLangKey : "");
        json.put("s", space);
        return json;
    }

    public String toJSONString() throws JSONException {
        return toJSON().toString(0);
    }

    public static Tag fromJSON(JSONObject json) throws JSONException {
        Tag tag = new Tag(
                json.getString("k"),
                json.getString("l"),
                json.getString("n"),
                json.getString("c"),
                json.getString("p"),
                !TextUtils.isEmpty(json.getString("r")) ? json.getString("r") : null,
                !TextUtils.isEmpty(json.getString("rp")) ? json.getString("rp") : null);
        tag.space = json.getString("s");
        return tag;
    }

    public static Tag fromJSONString(String jsonString) throws JSONException {
        return fromJSON(new JSONObject(jsonString));
    }

    public static Tag lookupKey(String key) {
        Tag tag = Cache.getInstance().lookupTag(key);
        if (tag == null) {
            tag = UserData.getInstance().ownTags.get(key);
        }
        return tag;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Tag) {
            if (((Tag) obj).key.equals(this.key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

}