package de.oklemenz.sayhi.model;

import java.io.Serializable;

import de.oklemenz.sayhi.service.Utilities;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class NewTag implements Serializable {

    public boolean assignPos = true;

    public String key = "";
    public String langCode = "";
    private String name = "";

    public void setName(String name) {
        this.name = Utilities.capitalize(name);
    }

    public String getName() {
        return name;
    }

    public Category category;
    public boolean categoryNew;
    public Tag primaryLangTag;
    public Category primaryLangCategory;

    public Tag getTag() {
        Tag tag = new Tag(
                key,
                langCode,
                name,
                category.key,
                primaryLangTag != null ? primaryLangTag.key : "");
        tag.category = category;
        return tag;
    }
}
