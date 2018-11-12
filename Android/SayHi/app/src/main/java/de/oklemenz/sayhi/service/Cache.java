package de.oklemenz.sayhi.service;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Handler;

import org.json.JSONException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.model.Category;
import de.oklemenz.sayhi.model.Tag;

/**
 * Created by Oliver Klemenz on 21.02.17.
 */

public class Cache extends SQLiteOpenHelper {

    private static final int DatabaseVersion = 1;
    private static final String DatabaseName = "cache.db";

    private static Cache instance = new Cache();

    public static Cache getInstance() {
        return instance;
    }

    private static final String DbTag = "tag_view";
    private static final String DBCategory = "category_view";
    private static final String DbKey = "key";
    private static final String DbData = "data";

    private Map<String, Tag> tags = new HashMap<>();
    private Map<String, Category> categories = new HashMap<>();

    private Set<String> cachedTagKeys = new HashSet<>();
    private Set<String> cachedCategoryKeys = new HashSet<>();

    private Cache() {
        super(AppDelegate.getInstance().Context, DatabaseName, null, DatabaseVersion);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + DbTag + " (" +
                DbKey + " TEXT PRIMARY KEY," +
                DbData + " TEXT)"
        );
        db.execSQL("CREATE TABLE " + DBCategory + " (" +
                DbKey + " TEXT PRIMARY KEY," +
                DbData + " TEXT)"
        );
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public Tag lookupTag(String key) {
        Tag tag = null;
        Cursor cursor = getReadableDatabase().query(DbTag, new String[]{DbData}, DbKey + " = ?", new String[]{key}, null, null, null);
        while (cursor.moveToNext()) {
            String data = cursor.getString(cursor.getColumnIndexOrThrow(DbData));
            try {
                tag = Tag.fromJSONString(data);
                break;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return tag;
    }

    public void cacheTag(Tag tag) {
        if (cachedTagKeys.contains(tag.key)) {
            return;
        }
        cachedTagKeys.add(tag.key);
        try {
            ContentValues values = new ContentValues();
            values.put(DbKey, tag.key);
            values.put(DbData, tag.toJSONString());
            this.getWritableDatabase().replace(DbTag, null, values);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void cacheTagAsync(final Tag tag) {
        if (cachedTagKeys.contains(tag.key)) {
            return;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        cacheTag(tag);
                    }
                });
            }
        }, 100);
    }

    public void cacheTagsAsync(final List<Tag> tags) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (Tag tag : tags) {
                            cacheTag(tag);
                        }
                    }
                });
            }
        }, 500);
    }

    public Category lookupCategory(String key) {
        Category category = null;
        Cursor cursor = getReadableDatabase().query(DBCategory, new String[]{DbData}, DbKey + " = ?", new String[]{key}, null, null, null);
        while (cursor.moveToNext()) {
            String data = cursor.getString(cursor.getColumnIndexOrThrow(DbData));
            try {
                category = Category.fromJSONString(data);
                break;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return category;
    }

    public void cacheCategory(Category category) {
        if (cachedCategoryKeys.contains(category.key)) {
            return;
        }
        cachedCategoryKeys.add(category.key);
        try {
            ContentValues values = new ContentValues();
            values.put(DbKey, category.key);
            values.put(DbData, category.toJSONString());
            this.getWritableDatabase().replace(DBCategory, null, values);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void cacheCategoryAsync(final Category category) {
        if (cachedCategoryKeys.contains(category.key)) {
            return;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        cacheCategory(category);
                    }
                });
            }
        }, 100);
    }

    public void cacheCategoriesAsync(final List<Category> categories) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (Category category : categories) {
                            cacheCategory(category);
                        }
                    }
                });
            }
        }, 500);
    }
}