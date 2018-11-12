package de.oklemenz.sayhi.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

import org.jdeferred.DoneCallback;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.oklemenz.sayhi.AppDelegate;

import static de.oklemenz.sayhi.AppDelegate.IconsFetchedNotification;
import static de.oklemenz.sayhi.AppDelegate.Namespace;
import static de.oklemenz.sayhi.AppDelegate.SpaceSwitchedNotification;

/**
 * Created by Oliver Klemenz on 21.02.17.
 */

public class IconService extends SQLiteOpenHelper {

    private static String IconFetchDateField = Namespace + ".IconFetchDate";

    private static final int DatabaseVersion = 1;
    private static final String DatabaseName = "icons.db";

    private static IconService instance = new IconService();

    public static IconService getInstance() {
        return instance;
    }

    private static final String DbIcon = "icon";
    private static final String DbKey = "key";
    private static final String DbData = "data";

    private Map<String, Bitmap> icons = new HashMap<>();

    private IconService() {
        super(AppDelegate.getInstance().Context, DatabaseName, null, DatabaseVersion);
        NotificationCenter.getInstance().addObserver(SpaceSwitchedNotification, new NotificationCenter.Observer() {
            @Override
            public void notify(String name, NotificationCenter.Notification notification) {
                clear();
                fetch();
            }
        }, false);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + DbIcon + " (" +
                DbKey + " TEXT PRIMARY KEY," +
                DbData + " TEXT)"
        );
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void clear() {
        SharedPreferences preferences = AppDelegate.getInstance().Context.getSharedPreferences(AppDelegate.Namespace, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(IconFetchDateField, null);
        editor.commit();
    }

    public void reset() {
        clear();
        IconService.this.getWritableDatabase().delete(DbIcon, null, null);
        fetch();
    }

    public void fetch() {
        final SharedPreferences preferences = AppDelegate.getInstance().Context.getSharedPreferences(AppDelegate.Namespace, Context.MODE_PRIVATE);
        Date date = null;
        String isoDate = preferences.getString(IconFetchDateField, null);
        if (isoDate != null) {
            date = Utilities.parseISO8601String(isoDate);
        }
        DataService.getInstance().fetchIcons(date).then(new DoneCallback<Map<String, String>>() {
            @Override
            public void onDone(Map<String, String> icons) {
                if (icons.size() > 0) {
                    for (Map.Entry<String, String> entry : icons.entrySet()) {
                        ContentValues values = new ContentValues();
                        values.put(DbKey, entry.getKey());
                        values.put(DbData, entry.getValue());
                        IconService.this.getWritableDatabase().replace(DbIcon, null, values);
                    }

                    Date date = new Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000); // 2 hour adjustment, e.g. daylight saving time (DST)
                    String isoDate = Utilities.getISO8601StringForDate(date);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(IconFetchDateField, isoDate);
                    editor.commit();
                    NotificationCenter.getInstance().post(IconsFetchedNotification);
                }
            }
        });
    }

    public Bitmap icon(String name) {
        int id = Utilities.getDrawableResourceId(AppDelegate.getInstance().Context, name);
        if (id > 0) {
            return BitmapFactory.decodeResource(AppDelegate.getInstance().Context.getResources(), id);
        }
        DisplayMetrics metrics = AppDelegate.getInstance().Context.getResources().getDisplayMetrics();
        if (metrics.density > DisplayMetrics.DENSITY_DEFAULT) {
            name = name + "@2x";
        }
        if (icons.containsKey(name)) {
            return icons.get(name);
        }
        Cursor cursor = getReadableDatabase().query(DbIcon, new String[]{DbData}, DbKey + " = ?", new String[]{name}, null, null, null);
        while (cursor.moveToNext()) {
            String data = cursor.getString(cursor.getColumnIndexOrThrow(DbData));
            byte[] bytes = Utilities.fromBase64(data);
            Bitmap icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            icons.put(name, icon);
            return icon;
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return null;
    }
}