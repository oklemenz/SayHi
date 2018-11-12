package de.oklemenz.sayhi.model;

import java.io.Serializable;

import de.oklemenz.sayhi.service.Utilities;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class NewCategory implements Serializable {

    public String key = "";
    public String langCode = "";
    private String name = "";

    public void setName(String name) {
        this.name = Utilities.capitalize(name);
    }

    public String getName() {
        return name;
    }

    public Category primaryLangCategory;

    public Category getCategory() {
        return new Category(
                key,
                langCode,
                name,
                Category.CategoryStagedColor,
                Category.CategoryStagedIcon,
                0,
                primaryLangCategory != null ? primaryLangCategory.key : "",
                null,
                null);
    }
}
