package de.oklemenz.sayhi.model;

import java.io.Serializable;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class CategoryQuery implements Serializable {

    public boolean favorite = false;
    public boolean search = false;
    public String searchText = "";
    public String name = "";
    public String langCode = "";
    public String primaryLangKey = "";
}