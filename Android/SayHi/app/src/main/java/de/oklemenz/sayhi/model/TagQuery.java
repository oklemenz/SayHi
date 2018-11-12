package de.oklemenz.sayhi.model;

import java.io.Serializable;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class TagQuery implements Serializable {

    public boolean favorite = false;
    public boolean search = false;
    public String searchText = "";
    public boolean own = false;
    public String categoryKey = "";
    public boolean categoryStaged = false;
    public String name = "";
    public String langCode = "";
}
