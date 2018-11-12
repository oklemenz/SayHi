package de.oklemenz.sayhi.model;

import java.io.Serializable;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class StageTag implements Serializable {

    public String key = "";
    public String langCode = "";
    public String name = "";
    public String categoryKey = "";
    public String categoryName = "";
    public String primaryLangKey = "";
    public Long counter = 1L;

    public StageTag(String key, String langCode, String name, String categoryKey, String categoryName, String primaryLangKey, Long counter) {
        this.key = key;
        this.langCode = langCode;
        this.name = name;
        this.categoryKey = categoryKey;
        this.categoryName = categoryName;
        this.primaryLangKey = primaryLangKey;
        this.counter = counter;
    }

    public Tag tag() {
        Tag tag = new Tag(
                key,
                langCode,
                name,
                categoryKey,
                primaryLangKey);
        tag.stage = true;
        return tag;
    }
}
