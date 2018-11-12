package de.oklemenz.sayhi.model;

import java.io.Serializable;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class StageCategory implements Serializable {

    public String key = "";
    public String langCode = "";
    public String name = "";
    public String primaryLangKey = "";
    public Long counter = 1L;

    public StageCategory(String key, String langCode, String name, String primaryLangKey, Long counter) {
        this.key = key;
        this.langCode = langCode;
        this.name = name;
        this.primaryLangKey = primaryLangKey;
        this.counter = counter;
    }

    public Category category() {
        Category category = new Category(
                key,
                langCode,
                name,
                Category.CategoryStagedColor,
                Category.CategoryStagedIcon,
                0,
                primaryLangKey);
        category.stage = true;
        return category;
    }
}
