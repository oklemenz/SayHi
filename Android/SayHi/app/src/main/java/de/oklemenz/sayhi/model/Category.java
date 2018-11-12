package de.oklemenz.sayhi.model;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import de.oklemenz.sayhi.service.Cache;
import de.oklemenz.sayhi.service.IconService;
import de.oklemenz.sayhi.service.Utilities;

/**
 * Created by Oliver Klemenz on 31.10.16.
 */

public class Category implements Serializable {

    public static String CategoryStagedIcon = "staged";
    public static String CategoryStagedColor = "#999999";

    public static Category CategoryFavorite = new Category(1, "favorite");
    public static Category CategorySearch = new Category(2, "search");
    public static Category CategoryOwn = new Category(3, "own");
    public static Category CategoryStaged = new Category(4, "staged");
    public static Category CategoryMore = new Category(5, "more");

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

    private String color = "";

    public void setColor(String color) {
        this.color = color;
        if (!TextUtils.isEmpty(color)) {
            this.bgColor = Color.parseColor(color);
            this.textColor = Utilities.isColorLight(this.bgColor) ? Color.BLACK : Color.WHITE;
        } else {
            this.bgColor = Color.TRANSPARENT;
            this.textColor = Color.WHITE;
        }
    }

    public String getColor() {
        return color;
    }

    private String icon = "";

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return this.icon;
    }

    public long order = 0;

    public String primaryLangKey = "";
    public String refKey = "";
    private String refPrimaryLangKey;

    public void setRefPrimaryLangKey(String refPrimaryLangKey) {
        this.refPrimaryLangKey = refPrimaryLangKey;
    }

    public String getRefPrimaryLangKey() {
        return this.refPrimaryLangKey != null ? this.refPrimaryLangKey : this.refKey;
    }

    public int bgColor = Color.WHITE;
    public int textColor = Color.BLACK;
    private transient Bitmap iconImage;

    public Bitmap getIconImage() {
        if (!TextUtils.isEmpty(icon)) {
            if (iconImage == null) {
                iconImage = IconService.getInstance().icon(icon);
            }
        }
        return iconImage;
    }

    public String search = "";
    public boolean favorite = false;
    public boolean selected = false;
    public boolean stage = false;
    public int mark = 0;

    public String space = "";

    public Category(String key, String langCode, String name, String color, String icon, long order, String primaryLangKey) {
        this(key, langCode, name, color, icon, order, primaryLangKey, null, null);
    }

    public Category(String key, String langCode, String name, String color, String icon, long order, String primaryLangKey, String refKey, String refPrimaryLangKey) {
        this(key, langCode, name, color, icon, order, primaryLangKey, refKey, refPrimaryLangKey, false);
    }

    public Category(String key, String langCode, String name, String color, String icon, long order, String primaryLangKey, String refKey, String refPrimaryLangKey, boolean favorite) {
        this.key = key;
        this.langCode = langCode;
        this.setName(name);
        this.setColor(color);
        this.setIcon(icon);
        this.order = order;
        this.primaryLangKey = primaryLangKey;
        if (refKey != null) {
            this.refKey = refKey;
        }
        if (refPrimaryLangKey != null) {
            this.refPrimaryLangKey = refPrimaryLangKey;
        }
        this.favorite = favorite;
    }

    public Category(int mark, String icon) {
        this("", "", "", "", icon, 0, "");
        this.mark = mark;
    }

    public Category primaryLangCategory;

    public Category getPrimaryLangCategory() {
        if (!TextUtils.isEmpty(primaryLangKey)) {
            if (primaryLangCategory == null) {
                primaryLangCategory = Cache.getInstance().lookupCategory(primaryLangKey);
            }
            return primaryLangCategory;
        }
        return null;
    }

    public String getEffectiveKey() {
        if (refPrimaryLangKey != null) {
            return refPrimaryLangKey;
        }
        return !TextUtils.isEmpty(primaryLangKey) ? primaryLangKey : key;
    }

    public void deriveFrom(Category category) {
        this.color = category.color;
        this.icon = category.icon;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("k", key);
        json.put("l", langCode);
        json.put("n", name);
        json.put("c", color);
        json.put("i", icon);
        json.put("o", order);
        json.put("p", primaryLangKey);
        json.put("r", refKey != null ? refKey : "");
        json.put("rp", refPrimaryLangKey != null ? refPrimaryLangKey : "");
        json.put("s", space);
        return json;
    }

    public String toJSONString() throws JSONException {
        return toJSON().toString(0);
    }

    public static Category fromJSON(JSONObject json) throws JSONException {
        Category category = new Category(
                json.getString("k"),
                json.getString("l"),
                json.getString("n"),
                json.getString("c"),
                json.getString("i"),
                json.getInt("o"),
                json.getString("p"),
                !TextUtils.isEmpty(json.getString("r")) ? json.getString("r") : null,
                !TextUtils.isEmpty(json.getString("rp")) ? json.getString("rp") : null);
        category.space = json.getString("s");
        return category;
    }

    public static Category fromJSONString(String jsonString) throws JSONException {
        return fromJSON(new JSONObject(jsonString));
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Category) {
            if (((Category) obj).key.equals(this.key)) {
                return true;
            }
        }
        return false;
    }
}