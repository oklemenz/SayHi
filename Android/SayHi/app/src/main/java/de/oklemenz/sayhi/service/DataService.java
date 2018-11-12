package de.oklemenz.sayhi.service;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Pair;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import org.jdeferred.Deferred;
import org.jdeferred.DeferredManager;
import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.jdeferred.impl.DefaultDeferredManager;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
import org.jdeferred.multiple.OneResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.oklemenz.sayhi.model.Category;
import de.oklemenz.sayhi.model.CategoryQuery;
import de.oklemenz.sayhi.model.MatchStatus;
import de.oklemenz.sayhi.model.NewCategory;
import de.oklemenz.sayhi.model.NewTag;
import de.oklemenz.sayhi.model.Settings;
import de.oklemenz.sayhi.model.StageCategory;
import de.oklemenz.sayhi.model.StageTag;
import de.oklemenz.sayhi.model.Tag;
import de.oklemenz.sayhi.model.TagQuery;
import de.oklemenz.sayhi.model.UserData;

import static de.oklemenz.sayhi.AppDelegate.DataServiceSetupNotification;
import static de.oklemenz.sayhi.AppDelegate.FavoriteCategoriesFetchedNotification;
import static de.oklemenz.sayhi.AppDelegate.IconsFetchedNotification;
import static de.oklemenz.sayhi.AppDelegate.LoginNotification;
import static de.oklemenz.sayhi.AppDelegate.PrimaryLangCode;
import static de.oklemenz.sayhi.AppDelegate.SettingsFetchedNotification;
import static de.oklemenz.sayhi.AppDelegate.SetupEndNotification;
import static de.oklemenz.sayhi.AppDelegate.SpaceSwitchedNotification;
import static de.oklemenz.sayhi.AppDelegate.UserDataFetchedNotification;
import static de.oklemenz.sayhi.AppDelegate.UserDataLangChangedNotification;

/**
 * Created by Oliver Klemenz on 31.10.16.
 */

public class DataService implements NotificationCenter.Observer {

    public static class NetworkErrorException extends Exception {
    }

    private static DataService instance = new DataService();

    public static DataService getInstance() {
        return instance;
    }

    public interface DatabaseRefListener<T> {
        void updated(T value);
    }

    private DatabaseReference ref;
    private DatabaseReference spaceRef;
    private DatabaseReference settingsRef;
    private DatabaseReference dataRef;
    private DatabaseReference analyticsRef;
    private DatabaseReference iconsRef;
    private DatabaseReference messagesRef;
    private DatabaseReference messageMatchRef;
    private DatabaseReference matchesRef;
    private DatabaseReference matchRef;
    private DatabaseReference scoresRef;

    private ValueEventListener settingsValueEventListener;
    private ValueEventListener messageMatchEventListener;
    private ValueEventListener matchStatusEventListener;

    public List<Category> favoriteCategories = new ArrayList<>();
    public boolean favoriteCategoriesLoaded = false;
    public boolean isSetup = false;

    private DataService() {
        NotificationCenter.getInstance().addObserver(LoginNotification, this, false);
        NotificationCenter.getInstance().addObserver(SpaceSwitchedNotification, this, false);
        NotificationCenter.getInstance().addObserver(IconsFetchedNotification, this, false);
        NotificationCenter.getInstance().addObserver(UserDataFetchedNotification, this, false);
        NotificationCenter.getInstance().addObserver(UserDataLangChangedNotification, this, false);
        NotificationCenter.getInstance().addObserver(SetupEndNotification, this, false);
    }

    @Override
    public void notify(String name, NotificationCenter.Notification notification) {
        if (name.equals(LoginNotification)) {
            loginSuccessful();
        } else if (name.equals(SpaceSwitchedNotification)) {
            spaceSwitched();
        } else if (
                name.equals(IconsFetchedNotification) || name.equals(UserDataFetchedNotification) ||
                        name.equals(UserDataLangChangedNotification) || name.equals(SetupEndNotification)) {
            fetchFavoriteCategories();
        }
    }

    private void setup(boolean spaceSwitch) {
        ref = FirebaseDatabase.getInstance().getReference();
        spaceRef = ref.child(SecureStore.getSpaceRefName());
        if (settingsRef != null) {
            if (settingsValueEventListener != null) {
                settingsRef.removeEventListener(settingsValueEventListener);
                settingsValueEventListener = null;
            }
        }
        settingsRef = spaceRef.child("settings");
        dataRef = spaceRef.child("data");
        analyticsRef = spaceRef.child("analytics");
        iconsRef = spaceRef.child("icons");
        messagesRef = spaceRef.child("messages");
        matchesRef = spaceRef.child("matches");
        scoresRef = spaceRef.child("scores");
        isSetup = true;
        NotificationCenter.getInstance().post(DataServiceSetupNotification);
        fetchSettings(spaceSwitch);
    }

    private void loginSuccessful() {
        setup(false);
    }

    private void spaceSwitched() {
        setup(true);
        fetchFavoriteCategories();
    }

    private void fetchFavoriteCategories() {
        favoriteCategoriesLoaded = false;
        if (dataRef != null) {
            CategoryQuery query = new CategoryQuery();
            query.favorite = true;
            fetchCategories(query).done(new DoneCallback<List<Category>>() {
                @Override
                public void onDone(List<Category> categories) {
                    favoriteCategories = categories;
                    favoriteCategoriesLoaded = true;
                    NotificationCenter.getInstance().post(FavoriteCategoriesFetchedNotification);
                }
            });
        }
    }

    private void fetchSettings(final boolean spaceSwitch) {
        if (settingsRef != null) {
            if (settingsValueEventListener != null) {
                settingsRef.removeEventListener(settingsValueEventListener);
                settingsValueEventListener = null;
            }
            settingsValueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Map<String, Object> settingsData = (Map<String, Object>) dataSnapshot.getValue();
                        Settings.getInstance().update(settingsData, spaceSwitch);
                        NotificationCenter.getInstance().post(SettingsFetchedNotification);
                    }
                }

                @Override
                public void onCancelled(DatabaseError e) {
                    e.toException().printStackTrace();
                }
            };
            settingsRef.addValueEventListener(settingsValueEventListener);
        }
    }

    public Promise<Map<String, String>, Exception, Void> fetchIcons(Date date) {
        Query iconsQuery = iconsRef;
        if (iconsQuery != null && date != null) {
            iconsQuery = iconsQuery.orderByChild("date").startAt(date.getTime());
        }
        final Deferred<Map<String, String>, Exception, Void> deferred = new DeferredObject<>();
        final Map<String, String> icons = new HashMap<>();
        if (iconsQuery != null) {
            iconsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            if (snapshot.hasChild("data") && snapshot.child("data").getValue() != null) {
                                icons.put(snapshot.getKey(), (String) snapshot.child("data").getValue());
                            }
                        }
                    }
                    deferred.resolve(icons);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    databaseError.toException().printStackTrace();
                    deferred.reject(databaseError.toException());
                }
            });
        } else {
            deferred.resolve(icons);
        }
        return deferred.promise();
    }

    public boolean parseIgnore(DataSnapshot dataSnapshot) {
        if (dataSnapshot.child("ignore").getValue() == null) {
            return false;
        }
        return (Boolean) dataSnapshot.child("ignore").getValue();
    }

    public Category parseCategorySnapshot(DataSnapshot dataSnapshot, String langCode) {
        return parseCategorySnapshot(dataSnapshot, langCode, false);
    }

    public Category parseCategorySnapshot(DataSnapshot dataSnapshot, String langCode, boolean suppressIgnore) {
        if (suppressIgnore || !parseIgnore(dataSnapshot)) {
            Category category = new Category(
                    dataSnapshot.getKey(),
                    langCode,
                    (String) dataSnapshot.child("name").getValue(),
                    (String) dataSnapshot.child("color").getValue(),
                    (String) dataSnapshot.child("icon").getValue(),
                    (long) dataSnapshot.child("order").getValue(),
                    (String) dataSnapshot.child("primaryLangKey").getValue(),
                    (String) dataSnapshot.child("refKey").getValue(),
                    (String) dataSnapshot.child("refPrimaryLangKey").getValue());
            category.favorite = dataSnapshot.child("favorite").getValue() == Boolean.TRUE;
            return category;
        }
        return null;
    }

    public Promise<List<Category>, Exception, Void> fetchCategories(final CategoryQuery query) {
        final String langCode = query != null && !TextUtils.isEmpty(query.langCode) ? query.langCode : UserData.getInstance().getLangCode();
        final String searchText = query != null && !TextUtils.isEmpty(query.searchText) ? Utilities.searchNormalized(query.searchText, langCode) : "";

        DatabaseReference categoryRef = dataRef != null ? dataRef.child(langCode + "/active/categories") : null;
        Query categoryQuery = categoryRef;

        if (query != null && categoryRef != null) {
            if (query.favorite) {
                categoryQuery = categoryRef.orderByChild("favorite").equalTo(true);
            } else if (query.search) {
                categoryQuery = categoryRef.orderByChild("search");
                if (!TextUtils.isEmpty(searchText)) {
                    categoryQuery = categoryQuery.startAt(searchText);
                }
            } else if (!TextUtils.isEmpty(query.name)) {
                categoryQuery = categoryRef.orderByChild("name").equalTo(query.name);
            } else if (!TextUtils.isEmpty(query.primaryLangKey)) {
                categoryQuery = categoryRef.orderByChild("primaryLangKey").equalTo(query.primaryLangKey);
            }
        }

        final Deferred<List<Category>, Exception, Void> deferred = new DeferredObject<>();
        final List<Category> categories = new ArrayList<>();
        if (categoryQuery != null) {
            categoryQuery.limitToFirst(Settings.getInstance().getQueryLimit()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Category category = parseCategorySnapshot(snapshot, langCode);
                            if (query == null || (TextUtils.isEmpty(searchText) || category.search.startsWith(searchText))) {
                                categories.add(category);
                            }
                        }
                        if (query != null && query.favorite) {
                            Iterator<Category> categoryIterator = categories.iterator();
                            while (categoryIterator.hasNext()) {
                                Category category = categoryIterator.next();
                                if (category.order <= 0) {
                                    categoryIterator.remove();
                                }
                            }
                            Collections.sort(categories, new Comparator<Category>() {
                                @Override
                                public int compare(Category category1, Category category2) {
                                    return (int) (category1.order - category2.order);
                                }
                            });
                        } else {
                            Collections.sort(categories, new Comparator<Category>() {
                                @Override
                                public int compare(Category category1, Category category2) {
                                    return category1.getName().compareTo(category2.getName());
                                }
                            });
                        }
                        Cache.getInstance().cacheCategoriesAsync(categories);
                    }
                    deferred.resolve(categories);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    databaseError.toException().printStackTrace();
                    deferred.reject(databaseError.toException());
                }
            });
        } else {
            deferred.resolve(categories);
        }
        return deferred.promise();
    }

    public Promise<Category, Exception, Void> getCategory(String key, String langCode) {
        final String finalLangCode = langCode != null ? langCode : UserData.getInstance().getLangCode();
        DatabaseReference categoryRef = dataRef != null ? dataRef.child(langCode + "/active/categories") : null;
        final Deferred<Category, Exception, Void> deferred = new DeferredObject<>();
        if (categoryRef != null) {
            if (!TextUtils.isEmpty(key)) {
                Category category = Cache.getInstance().lookupCategory(key);
                if (category != null) {
                    return deferred.resolve(category);
                }
                categoryRef.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            deferred.resolve(null);
                        } else {
                            Category category = parseCategorySnapshot(dataSnapshot, finalLangCode, true);
                            if (category != null) {
                                Cache.getInstance().cacheCategoryAsync(category);
                            }
                            deferred.resolve(category);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        databaseError.toException().printStackTrace();
                        deferred.reject(databaseError.toException());
                    }
                });
            } else {
                deferred.resolve(null);
            }
        } else {
            deferred.resolve(null);
        }
        return deferred.promise();
    }

    public Promise<List<Category>, Exception, Void> getCategories(List<String> keys, String langCode) {
        langCode = langCode != null ? langCode : UserData.getInstance().getLangCode();
        List<Promise<Category, Exception, Void>> promises = new ArrayList<>();
        for (String key : keys) {
            promises.add(getCategory(key, langCode));
        }
        final Deferred<List<Category>, Exception, Void> deferred = new DeferredObject<>();
        DeferredManager deferredManager = new DefaultDeferredManager();
        deferredManager.when((Promise[]) promises.toArray()).done(new DoneCallback<MultipleResults>() {
            @Override
            public void onDone(MultipleResults results) {
                List<Category> categories = new ArrayList<>();
                for (OneResult result : results) {
                    categories.add((Category) result.getResult());
                }
                deferred.resolve(categories);
            }
        }).fail(new FailCallback<OneReject>() {
            @Override
            public void onFail(OneReject result) {
                deferred.reject((Exception) result.getReject());
            }
        });
        return deferred.promise();
    }

    public Promise<List<Category>, Exception, Void> fetchSimilarCategories(String name, String langCode) {
        final String finalLangCode = langCode != null ? langCode : UserData.getInstance().getLangCode();
        final String searchText = Utilities.searchNormalized(name, langCode);
        DatabaseReference categoryRef = dataRef != null ? dataRef.child(langCode + "/active/categories") : null;
        Query categoryActiveQuery = categoryRef != null ? categoryRef.orderByChild("search").startAt(searchText) : null;
        DatabaseReference categoryStageRef = dataRef != null ? dataRef.child(langCode + "/stage/categories") : dataRef;
        Query categoryStageQuery = categoryStageRef != null ? categoryStageRef.orderByChild("search").startAt(searchText) : null;

        final Deferred<List<Category>, Exception, Void> activeDeferred = new DeferredObject<>();
        final List<Category> activeCategories = new ArrayList<>();
        if (categoryActiveQuery != null) {
            categoryActiveQuery.limitToFirst(Settings.getInstance().getQueryLimit()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Category category = parseCategorySnapshot(snapshot, finalLangCode);
                            if (TextUtils.isEmpty(searchText) || category.search.startsWith(searchText)) {
                                activeCategories.add(category);
                            }
                        }
                        Cache.getInstance().cacheCategoriesAsync(activeCategories);
                    }
                    activeDeferred.resolve(activeCategories);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    databaseError.toException().printStackTrace();
                    activeDeferred.reject(databaseError.toException());
                }
            });
        } else {
            activeDeferred.resolve(activeCategories);
        }
        final Deferred<List<Category>, Exception, Void> stageDeferred = new DeferredObject<>();
        final List<Category> stageCategories = new ArrayList<>();
        if (categoryStageQuery != null) {
            categoryStageQuery.limitToFirst(Settings.getInstance().getQueryLimit()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            StageCategory stageCategory = parseStageCategorySnapshot(snapshot, finalLangCode);
                            if (stageCategory != null) {
                                Category category = stageCategory.category();
                                if (TextUtils.isEmpty(searchText) || category.search.startsWith(searchText)) {
                                    stageCategories.add(category);
                                }
                            }
                        }
                    }
                    stageDeferred.resolve(stageCategories);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    databaseError.toException().printStackTrace();
                    stageDeferred.reject(databaseError.toException());
                }
            });
        } else {
            stageDeferred.resolve(stageCategories);
        }

        final Deferred<List<Category>, Exception, Void> deferred = new DeferredObject<>();
        DeferredManager deferredManager = new DefaultDeferredManager();
        deferredManager.when(activeDeferred, stageDeferred).done(new DoneCallback<MultipleResults>() {
            @Override
            public void onDone(MultipleResults results) {
                List<Category> categories = new ArrayList<>();
                for (OneResult result : results) {
                    categories.addAll((List<Category>) result.getResult());
                }
                Collections.sort(categories, new Comparator<Category>() {
                    @Override
                    public int compare(Category category1, Category category2) {
                        return category1.getName().compareTo(category2.getName());
                    }
                });
                deferred.resolve(categories);
            }
        }).fail(new FailCallback<OneReject>() {
            @Override
            public void onFail(OneReject result) {
                deferred.reject((Exception) result.getReject());
            }
        });
        return deferred.promise();
    }

    public Promise<Category, Exception, Void> completeCategory(Category category) {
        List<Category> categories = new ArrayList<>();
        categories.add(category);
        final Deferred<Category, Exception, Void> deferred = new DeferredObject<>();
        completeCategories(categories).done(new DoneCallback<List<Category>>() {
            @Override
            public void onDone(List<Category> categories) {
                deferred.resolve(categories.get(0));
            }
        }).fail(new FailCallback<Exception>() {
            @Override
            public void onFail(Exception e) {
                deferred.reject(e);
            }
        });
        return deferred.promise();
    }

    public Promise<List<Category>, Exception, Void> completeCategories(final List<Category> categories) {
        List<Promise<Void, Exception, Void>> promises = new ArrayList<>();
        for (final Category category : categories) {
            if (category.primaryLangCategory == null && !TextUtils.isEmpty(category.primaryLangKey)) {
                final Deferred<Void, Exception, Void> activeDeferred = new DeferredObject<>();
                getCategory(category.primaryLangKey, PrimaryLangCode).done(new DoneCallback<Category>() {
                    @Override
                    public void onDone(Category primaryLangCategory) {
                        if (category.primaryLangCategory == null) {
                            category.primaryLangCategory = primaryLangCategory;
                        }
                        activeDeferred.resolve(null);
                    }
                }).fail(new FailCallback<Exception>() {
                    @Override
                    public void onFail(Exception e) {
                        activeDeferred.reject(e);
                    }
                });
                promises.add(activeDeferred.promise());
                final Deferred<Void, Exception, Void> stageDeferred = new DeferredObject<>();
                getStageCategory(category.primaryLangKey, PrimaryLangCode).done(new DoneCallback<StageCategory>() {
                    @Override
                    public void onDone(StageCategory primaryLangStageCategory) {
                        if (category.primaryLangCategory == null) {
                            if (primaryLangStageCategory != null) {
                                category.primaryLangCategory = primaryLangStageCategory.category();
                            }
                        }
                        stageDeferred.resolve(null);
                    }
                }).fail(new FailCallback<Exception>() {
                    @Override
                    public void onFail(Exception e) {
                        stageDeferred.reject(e);
                    }
                });
                promises.add(stageDeferred.promise());
            }
        }
        final Deferred<List<Category>, Exception, Void> deferred = new DeferredObject<>();
        Promise[] promiseArray = promises.toArray(new Promise[0]);
        if (promiseArray.length > 0) {
            DeferredManager deferredManager = new DefaultDeferredManager();
            deferredManager.when(promiseArray).done(new DoneCallback<MultipleResults>() {
                @Override
                public void onDone(MultipleResults results) {
                    deferred.resolve(categories);
                }
            }).fail(new FailCallback<OneReject>() {
                @Override
                public void onFail(OneReject result) {
                    deferred.reject((Exception) result.getReject());
                }
            });
            return deferred.promise();
        } else {
            deferred.resolve(categories);
            return deferred;
        }
    }

    public Tag parseTagSnapshot(DataSnapshot dataSnapshot, String langCode) {
        return parseTagSnapshot(dataSnapshot, langCode, false);
    }

    public Tag parseTagSnapshot(DataSnapshot dataSnapshot, String langCode, boolean suppressIgnore) {
        if (suppressIgnore || !parseIgnore(dataSnapshot)) {
            Tag tag = new Tag(
                    dataSnapshot.getKey(),
                    langCode,
                    (String) dataSnapshot.child("name").getValue(),
                    (String) dataSnapshot.child("categoryKey").getValue(),
                    (String) dataSnapshot.child("primaryLangKey").getValue(),
                    (String) dataSnapshot.child("refKey").getValue(),
                    (String) dataSnapshot.child("refPrimaryLangKey").getValue());
            tag.favorite = dataSnapshot.child("favorite").getValue() == Boolean.TRUE;
            tag.space = SecureStore.getSpaceRefName();
            return tag;
        }
        return null;
    }

    public Promise<List<Tag>, Exception, Void> fetchTags(final TagQuery query) {
        final String langCode = query != null && !TextUtils.isEmpty(query.langCode) ? query.langCode : UserData.getInstance().getLangCode();
        final String searchText = query != null && !TextUtils.isEmpty(query.searchText) ? Utilities.searchNormalized(query.searchText, langCode) : "";

        DatabaseReference tagRef = dataRef != null ? dataRef.child(langCode + "/active/tags") : null;
        Query tagQuery = tagRef;

        if (query != null && tagRef != null) {
            if (query.favorite) {
                tagQuery = tagRef.orderByChild("favorite").equalTo(true);
            } else if (query.search) {
                tagQuery = tagRef.orderByChild("search");
                if (!TextUtils.isEmpty(searchText)) {
                    tagQuery = tagQuery.startAt(searchText);
                }
            } else if (!TextUtils.isEmpty(query.name)) {
                tagQuery = tagRef.orderByChild("name").equalTo(query.name);
            } else if (!TextUtils.isEmpty(query.categoryKey)) {
                tagQuery = tagRef.orderByChild("categoryKey").equalTo(query.categoryKey);
            } else if (query.categoryStaged) {
                tagQuery = tagRef.orderByChild("categoryStaged").equalTo(true);
            }
        }

        final Deferred<List<Tag>, Exception, Void> deferred = new DeferredObject<>();
        final List<Tag> tags = new ArrayList<>();
        if (tagQuery != null) {
            tagQuery.limitToFirst(Settings.getInstance().getQueryLimit()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Tag tag = parseTagSnapshot(snapshot, langCode);
                            if (!TextUtils.isEmpty(query.name)) {
                                if (!TextUtils.isEmpty(query.categoryKey)) {
                                    if (query.categoryKey.equals(tag.getCategoryKey())) {
                                        tags.add(tag);
                                    }
                                } else {
                                    tags.add(tag);
                                }
                            } else if (TextUtils.isEmpty(searchText) || tag.search.startsWith(searchText)) {
                                tags.add(tag);
                            }
                        }
                        Collections.sort(tags, new Comparator<Tag>() {
                            @Override
                            public int compare(Tag tag1, Tag tag2) {
                                return tag1.getName().compareTo(tag2.getName());
                            }
                        });
                        Cache.getInstance().cacheTagsAsync(tags);
                    }
                    deferred.resolve(tags);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    databaseError.toException().printStackTrace();
                    deferred.reject(databaseError.toException());
                }
            });
        } else {
            deferred.resolve(tags);
        }
        return deferred.promise();

    }

    public Promise<Boolean, Exception, Void> hasTags(String langCode) {
        langCode = langCode != null ? langCode : UserData.getInstance().getLangCode();
        DatabaseReference tagRef = dataRef != null ? dataRef.child(langCode + "/active/tags") : null;

        final Deferred<Boolean, Exception, Void> deferred = new DeferredObject<>();
        if (tagRef == null) {
            return deferred.reject(new NetworkErrorException());
        }
        Query tagQuery = tagRef.orderByKey().limitToFirst(1);
        tagQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                deferred.resolve(dataSnapshot.exists());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
                deferred.reject(databaseError.toException());
            }
        });
        return deferred.promise();
    }

    public Promise<Tag, Exception, Void> getTag(String key, String langCode) {
        final String finalLangCode = langCode != null ? langCode : UserData.getInstance().getLangCode();
        DatabaseReference tagRef = dataRef != null ? dataRef.child(langCode + "/active/tags") : null;
        final Deferred<Tag, Exception, Void> deferred = new DeferredObject<>();
        if (tagRef != null) {
            if (!TextUtils.isEmpty(key)) {
                Tag tag = Cache.getInstance().lookupTag(key);
                if (tag != null) {
                    return deferred.resolve(tag);
                }
                tagRef.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            deferred.resolve(null);
                        } else {
                            Tag tag = parseTagSnapshot(dataSnapshot, finalLangCode, true);
                            if (tag != null) {
                                Cache.getInstance().cacheTagAsync(tag);
                            }
                            deferred.resolve(tag);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        databaseError.toException().printStackTrace();
                        deferred.reject(databaseError.toException());
                    }
                });
            } else {
                deferred.resolve(null);
            }
        } else {
            deferred.resolve(null);
        }
        return deferred.promise();
    }

    public Promise<List<Tag>, Exception, Void> getTags(List<String> keys, String langCode) {
        langCode = langCode != null ? langCode : UserData.getInstance().getLangCode();
        List<Promise<Tag, Exception, Void>> promises = new ArrayList<>();
        for (String key : keys) {
            promises.add(getTag(key, langCode));
        }
        final Deferred<List<Tag>, Exception, Void> deferred = new DeferredObject<>();
        DeferredManager deferredManager = new DefaultDeferredManager();
        deferredManager.when(promises.toArray(new Promise[0])).done(new DoneCallback<MultipleResults>() {
            @Override
            public void onDone(MultipleResults results) {
                List<Tag> tags = new ArrayList<>();
                for (OneResult result : results) {
                    tags.add((Tag) result.getResult());
                }
                deferred.resolve(tags);
            }
        }).fail(new FailCallback<OneReject>() {
            @Override
            public void onFail(OneReject result) {
                deferred.reject((Exception) result.getReject());
            }
        });
        return deferred.promise();
    }

    public Promise<List<Tag>, Exception, Void> fetchSimilarTags(String name, String langCode) {
        final String finalLangCode = langCode != null ? langCode : UserData.getInstance().getLangCode();
        final String searchText = Utilities.searchNormalized(name, langCode);
        DatabaseReference tagRef = dataRef != null ? dataRef.child(langCode + "/active/tags") : null;
        Query tagActiveQuery = tagRef != null ? tagRef.orderByChild("search").startAt(searchText) : null;
        DatabaseReference tagStageRef = dataRef != null ? dataRef.child(langCode + "/stage/tags") : null;
        Query tagStageQuery = tagStageRef != null ? tagStageRef.orderByChild("search").startAt(searchText) : null;

        final Deferred<List<Tag>, Exception, Void> activeDeferred = new DeferredObject<>();
        final List<Tag> activeTags = new ArrayList<>();
        if (tagActiveQuery != null) {
            tagActiveQuery.limitToFirst(Settings.getInstance().getQueryLimit()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Tag tag = parseTagSnapshot(snapshot, finalLangCode);
                            if (TextUtils.isEmpty(searchText) || tag.search.startsWith(searchText)) {
                                activeTags.add(tag);
                            }
                        }
                        Cache.getInstance().cacheTagsAsync(activeTags);
                    }
                    activeDeferred.resolve(activeTags);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    databaseError.toException().printStackTrace();
                    activeDeferred.reject(databaseError.toException());
                }
            });
        } else {
            activeDeferred.resolve(activeTags);
        }
        final Deferred<List<Tag>, Exception, Void> stageDeferred = new DeferredObject<>();
        final List<Tag> stageTags = new ArrayList<>();
        if (tagStageQuery != null) {
            tagStageQuery.limitToFirst(Settings.getInstance().getQueryLimit()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            StageTag stageTag = parseStageTagSnapshot(snapshot, finalLangCode);
                            if (stageTag != null) {
                                Tag tag = stageTag.tag();
                                if (TextUtils.isEmpty(searchText) || tag.search.startsWith(searchText)) {
                                    stageTags.add(tag);
                                }
                            }
                        }
                    }
                    stageDeferred.resolve(stageTags);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    databaseError.toException().printStackTrace();
                    stageDeferred.reject(databaseError.toException());
                }
            });
        } else {
            stageDeferred.resolve(stageTags);
        }

        final Deferred<List<Tag>, Exception, Void> deferred = new DeferredObject<>();
        DeferredManager deferredManager = new DefaultDeferredManager();
        deferredManager.when(activeDeferred, stageDeferred).done(new DoneCallback<MultipleResults>() {
            @Override
            public void onDone(MultipleResults results) {
                List<Tag> tags = new ArrayList<>();
                for (OneResult result : results) {
                    tags.addAll((List<Tag>) result.getResult());
                }
                Collections.sort(tags, new Comparator<Tag>() {
                    @Override
                    public int compare(Tag tag1, Tag tag2) {
                        return tag1.getName().compareTo(tag2.getName());
                    }
                });
                deferred.resolve(tags);
            }
        }).fail(new FailCallback<OneReject>() {
            @Override
            public void onFail(OneReject result) {
                deferred.reject((Exception) result.getReject());
            }
        });
        return deferred.promise();
    }

    public Promise<Tag, Exception, Void> completeTag(Tag tag) {
        List<Tag> tags = new ArrayList<>();
        tags.add(tag);
        final Deferred<Tag, Exception, Void> deferred = new DeferredObject<>();
        completeTags(tags).done(new DoneCallback<List<Tag>>() {
            @Override
            public void onDone(List<Tag> tags) {
                deferred.resolve(tags.get(0));
            }
        }).fail(new FailCallback<Exception>() {
            @Override
            public void onFail(Exception e) {
                deferred.reject(e);
            }
        });
        return deferred.promise();
    }

    public Promise<List<Tag>, Exception, Void> completeTags(final List<Tag> tags) {
        List<Promise<Void, Exception, Void>> promises = new ArrayList<>();
        for (final Tag tag : tags) {
            if (tag.category == null && !TextUtils.isEmpty(tag.getCategoryKey())) {
                final Deferred<Void, Exception, Void> activeDeferred = new DeferredObject<>();
                getCategory(tag.getCategoryKey(), tag.langCode).done(new DoneCallback<Category>() {
                    @Override
                    public void onDone(Category category) {
                        if (tag.category == null) {
                            tag.category = category;
                        }
                        activeDeferred.resolve(null);
                    }
                }).fail(new FailCallback<Exception>() {
                    @Override
                    public void onFail(Exception e) {
                        activeDeferred.reject(e);
                    }
                });
                promises.add(activeDeferred.promise());
                final Deferred<Void, Exception, Void> stageDeferred = new DeferredObject<>();
                getStageCategory(tag.getCategoryKey(), tag.langCode).done(new DoneCallback<StageCategory>() {
                    @Override
                    public void onDone(StageCategory stageCategory) {
                        if (tag.category == null) {
                            if (stageCategory != null) {
                                tag.category = stageCategory.category();
                            }
                        }
                        stageDeferred.resolve(null);
                    }
                }).fail(new FailCallback<Exception>() {
                    @Override
                    public void onFail(Exception e) {
                        stageDeferred.reject(e);
                    }
                });
                promises.add(stageDeferred.promise());
            }
            if (tag.primaryLangTag == null && !TextUtils.isEmpty(tag.primaryLangKey)) {
                final Deferred<Void, Exception, Void> activeDeferred = new DeferredObject<>();
                getTag(tag.primaryLangKey, PrimaryLangCode).done(new DoneCallback<Tag>() {
                    @Override
                    public void onDone(Tag primaryLangTag) {
                        if (tag.primaryLangTag == null) {
                            tag.primaryLangTag = primaryLangTag;
                        }
                        activeDeferred.resolve(null);
                    }
                }).fail(new FailCallback<Exception>() {
                    @Override
                    public void onFail(Exception e) {
                        activeDeferred.reject(e);
                    }
                });
                promises.add(activeDeferred.promise());
                final Deferred<Void, Exception, Void> stageDeferred = new DeferredObject<>();
                getStageTag(tag.primaryLangKey, PrimaryLangCode).done(new DoneCallback<StageTag>() {
                    @Override
                    public void onDone(StageTag primaryLangStageTag) {
                        if (tag.primaryLangTag == null) {
                            if (primaryLangStageTag != null) {
                                tag.primaryLangTag = primaryLangStageTag.tag();
                            }
                        }
                        stageDeferred.resolve(null);
                    }
                }).fail(new FailCallback<Exception>() {
                    @Override
                    public void onFail(Exception e) {
                        stageDeferred.reject(e);
                    }
                });
                promises.add(stageDeferred.promise());
            }
        }
        final Deferred<List<Tag>, Exception, Void> deferred = new DeferredObject<>();
        DeferredManager deferredManager = new DefaultDeferredManager();
        deferredManager.when(promises.toArray(new Promise[0])).done(new DoneCallback<MultipleResults>() {
            @Override
            public void onDone(MultipleResults results) {
                List<Promise<Void, Exception, Void>> promises = new ArrayList<>();
                for (final Tag tag : tags) {
                    if (tag.category != null) {
                        final Category category = tag.category;
                        if (category.primaryLangCategory == null || !TextUtils.isEmpty(category.primaryLangKey)) {
                            final Deferred<Void, Exception, Void> activeDeferred = new DeferredObject<>();
                            getCategory(category.primaryLangKey, PrimaryLangCode).done(new DoneCallback<Category>() {
                                @Override
                                public void onDone(Category primaryLangCategory) {
                                    if (category.primaryLangCategory == null) {
                                        category.primaryLangCategory = primaryLangCategory;
                                    }
                                    activeDeferred.resolve(null);
                                }
                            }).fail(new FailCallback<Exception>() {
                                @Override
                                public void onFail(Exception e) {
                                    activeDeferred.reject(e);
                                }
                            });
                            promises.add(activeDeferred.promise());
                            final Deferred<Void, Exception, Void> stageDeferred = new DeferredObject<>();
                            getStageCategory(category.primaryLangKey, PrimaryLangCode).done(new DoneCallback<StageCategory>() {
                                @Override
                                public void onDone(StageCategory primaryLangStageCategory) {
                                    if (category.primaryLangCategory == null) {
                                        if (primaryLangStageCategory != null) {
                                            category.primaryLangCategory = primaryLangStageCategory.category();
                                        }
                                    }
                                    stageDeferred.resolve(null);
                                }
                            }).fail(new FailCallback<Exception>() {
                                @Override
                                public void onFail(Exception e) {
                                    stageDeferred.reject(e);
                                }
                            });
                            promises.add(stageDeferred.promise());
                        }
                    }
                }
                DeferredManager deferredManager = new DefaultDeferredManager();
                deferredManager.when(promises.toArray(new Promise[0])).done(new DoneCallback<MultipleResults>() {
                    @Override
                    public void onDone(MultipleResults results) {
                        deferred.resolve(tags);
                    }
                }).fail(new FailCallback<OneReject>() {
                    @Override
                    public void onFail(OneReject result) {
                        deferred.reject((Exception) result.getReject());
                    }
                });
            }
        }).fail(new FailCallback<OneReject>() {
            @Override
            public void onFail(OneReject result) {
                deferred.reject((Exception) result.getReject());
            }
        });
        return deferred.promise();
    }

    public Pair<String, String> parseStageSnapshot(DataSnapshot dataSnapshot) {
        return parseStageSnapshot(dataSnapshot, false);
    }

    public Pair<String, String> parseStageSnapshot(DataSnapshot dataSnapshot, boolean suppressIgnore) {
        if (suppressIgnore || !parseIgnore(dataSnapshot)) {
            return new Pair<>(dataSnapshot.getKey(), (String) dataSnapshot.child("name").getValue());
        }
        return null;
    }

    public StageCategory parseStageCategorySnapshot(DataSnapshot dataSnapshot, String langCode) {
        langCode = langCode != null ? langCode : UserData.getInstance().getLangCode();
        return parseStageCategorySnapshot(dataSnapshot, langCode, false);
    }

    public StageCategory parseStageCategorySnapshot(DataSnapshot dataSnapshot, String langCode, boolean suppressIgnore) {
        if (suppressIgnore || !parseIgnore(dataSnapshot)) {
            return new StageCategory(
                    dataSnapshot.getKey(),
                    langCode,
                    (String) dataSnapshot.child("name").getValue(),
                    (String) dataSnapshot.child("primaryLangKey").getValue(),
                    (Long) dataSnapshot.child("counter").getValue());
        }
        return null;
    }

    public Promise<StageCategory, Exception, Void> getStageCategory(String key, String langCode) {
        final String finalLangCode = langCode != null ? langCode : UserData.getInstance().getLangCode();
        DatabaseReference categoryRef = dataRef != null ? dataRef.child(langCode + "/stage/categories") : null;
        final Deferred<StageCategory, Exception, Void> deferred = new DeferredObject<>();
        if (categoryRef != null) {
            if (!TextUtils.isEmpty(key)) {
                categoryRef.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            deferred.resolve(null);
                        } else {
                            StageCategory stageCategory = parseStageCategorySnapshot(dataSnapshot, finalLangCode, true);
                            deferred.resolve(stageCategory);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        databaseError.toException().printStackTrace();
                        deferred.reject(databaseError.toException());
                    }
                });
            } else {
                deferred.resolve(null);
            }
        } else {
            deferred.resolve(null);
        }
        return deferred.promise();
    }

    public Promise<StageCategory, Exception, Void> createCategory(final NewCategory newCategory) {
        final String hash = "#C:" + newCategory.getName() + "#P:" + (newCategory.primaryLangCategory != null ? newCategory.primaryLangCategory.key : "");
        final DatabaseReference categoryRef = dataRef != null ? dataRef.child(newCategory.langCode + "/stage/categories") : null;

        final Deferred<StageCategory, Exception, Void> deferred = new DeferredObject<>();
        if (categoryRef == null) {
            return deferred.reject(new NetworkErrorException());
        }
        final Deferred<Pair<String, String>, Exception, Void> existsDeferred = new DeferredObject<>();
        Query categoryQuery = categoryRef.orderByChild("hash").equalTo(hash);
        categoryQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Pair<String, String> stage = parseStageSnapshot(snapshot);
                        existsDeferred.resolve(stage);
                        return;
                    }
                } else {
                    existsDeferred.resolve(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
                existsDeferred.reject(databaseError.toException());
            }
        });
        final Deferred<Void, Exception, Void> createDeferred = new DeferredObject<>();
        existsDeferred.promise().done(new DoneCallback<Pair<String, String>>() {
            @Override
            public void onDone(Pair<String, String> stage) {
                if (stage == null) {
                    DatabaseReference category = categoryRef.push();
                    newCategory.key = category.getKey();
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", newCategory.getName());
                    data.put("language", newCategory.langCode);
                    data.put("search", Utilities.searchNormalized(newCategory.getName(), newCategory.langCode));
                    data.put("icon", Utilities.clean(newCategory.primaryLangCategory != null ? newCategory.primaryLangCategory.getName() : newCategory.getName()).toLowerCase());
                    data.put("primaryLangKey", newCategory.primaryLangCategory != null ? newCategory.primaryLangCategory.key : "");
                    data.put("hash", hash);
                    data.put("counter", 1);
                    data.put("createdAt", ServerValue.TIMESTAMP);
                    data.put("changedAt", ServerValue.TIMESTAMP);
                    category.setValue(data).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            UserData.getInstance().addNewItemHash(hash);
                            UserData.getInstance().touch(null);
                            createDeferred.resolve(aVoid);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            e.printStackTrace();
                            createDeferred.reject(e);
                        }
                    });
                } else {
                    newCategory.key = stage.first;
                    categoryRef.child(newCategory.key).runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData currentData) {
                            if (currentData != null && currentData.getValue() != null) {
                                Map<String, Object> categoryData = (Map<String, Object>) currentData.getValue();
                                categoryData.put("counter", (Long) categoryData.get("counter") + (UserData.getInstance().hasNewItemHash(hash) == 0 ? 1 : 0));
                                categoryData.put("changedAt", ServerValue.TIMESTAMP);
                                currentData.setValue(categoryData);
                            }
                            return Transaction.success(currentData);
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean commited, DataSnapshot dataSnapshot) {
                            UserData.getInstance().addNewItemHash(hash);
                            UserData.getInstance().touch(null);
                            if (databaseError != null) {
                                Exception e = databaseError.toException();
                                e.printStackTrace();
                                createDeferred.reject(e);
                            } else {
                                createDeferred.resolve(null);
                            }
                        }
                    });
                }
            }
        }).fail(new FailCallback<Exception>() {
            @Override
            public void onFail(Exception e) {
                createDeferred.reject(e);
            }
        });
        createDeferred.promise().done(new DoneCallback<Void>() {
            @Override
            public void onDone(Void Void) {
                getStageCategory(newCategory.key, newCategory.langCode).done(new DoneCallback<StageCategory>() {
                    @Override
                    public void onDone(StageCategory category) {
                        if (category != null) {
                            Analytics.getInstance().logNewCategory(category);
                        }
                        deferred.resolve(category);
                    }
                }).fail(new FailCallback<Exception>() {
                    @Override
                    public void onFail(Exception e) {
                        deferred.reject(e);
                    }
                });
            }
        }).fail(new FailCallback<Exception>() {
            @Override
            public void onFail(Exception e) {
                deferred.reject(e);
            }
        });
        return deferred.promise();
    }

    public StageTag parseStageTagSnapshot(DataSnapshot dataSnapshot, String langCode) {
        return parseStageTagSnapshot(dataSnapshot, langCode, false);
    }

    public StageTag parseStageTagSnapshot(DataSnapshot dataSnapshot, String langCode, boolean suppressIgnore) {
        if (suppressIgnore || !parseIgnore(dataSnapshot)) {
            return new StageTag(
                    dataSnapshot.getKey(),
                    langCode,
                    (String) dataSnapshot.child("name").getValue(),
                    (String) dataSnapshot.child("categoryKey").getValue(),
                    (String) dataSnapshot.child("categoryName").getValue(),
                    (String) dataSnapshot.child("primaryLangKey").getValue(),
                    (Long) dataSnapshot.child("counter").getValue());
        }
        return null;
    }

    public Promise<StageTag, Exception, Void> getStageTag(String key, String langCode) {
        final String finalLangCode = langCode != null ? langCode : UserData.getInstance().getLangCode();
        DatabaseReference tagRef = dataRef != null ? dataRef.child(langCode + "/stage/tags") : null;
        final Deferred<StageTag, Exception, Void> deferred = new DeferredObject<>();
        if (tagRef != null) {
            if (!TextUtils.isEmpty(key)) {
                tagRef.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            deferred.resolve(null);
                        } else {
                            StageTag stageTag = parseStageTagSnapshot(dataSnapshot, finalLangCode, true);
                            deferred.resolve(stageTag);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        databaseError.toException().printStackTrace();
                        deferred.reject(databaseError.toException());
                    }
                });
            } else {
                deferred.resolve(null);
            }
        } else {
            deferred.resolve(null);
        }
        return deferred.promise();
    }

    public Promise<StageTag, Exception, Void> createTag(final NewTag newTag) {
        final String hash = "#T:" + newTag.getName() + "#C:" + newTag.category.key + "#P:" + (newTag.primaryLangTag != null ? newTag.primaryLangTag.key : "");
        final DatabaseReference tagRef = dataRef != null ? dataRef.child(newTag.langCode + "/stage/tags") : null;

        final Deferred<StageTag, Exception, Void> deferred = new DeferredObject<>();
        if (tagRef == null) {
            return deferred.reject(new NetworkErrorException());
        }
        final Deferred<Pair<String, String>, Exception, Void> existsDeferred = new DeferredObject<>();
        Query tagQuery = tagRef.orderByChild("hash").equalTo(hash);
        tagQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Pair<String, String> stage = parseStageSnapshot(snapshot);
                        existsDeferred.resolve(stage);
                        return;
                    }
                } else {
                    existsDeferred.resolve(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
                existsDeferred.reject(databaseError.toException());
            }
        });
        final Deferred<Void, Exception, Void> createDeferred = new DeferredObject<>();
        existsDeferred.promise().done(new DoneCallback<Pair<String, String>>() {
            @Override
            public void onDone(Pair<String, String> stage) {
                if (stage == null) {
                    DatabaseReference tag = tagRef.push();
                    newTag.key = tag.getKey();
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", newTag.getName());
                    data.put("language", newTag.langCode);
                    data.put("search", Utilities.searchNormalized(newTag.getName(), newTag.langCode));
                    data.put("categoryKey", newTag.category.key);
                    data.put("categoryName", newTag.category.getName());
                    data.put("primaryLangKey", newTag.primaryLangTag != null ? newTag.primaryLangTag.key : "");
                    data.put("hash", hash);
                    data.put("counter", 1);
                    data.put("createdAt", ServerValue.TIMESTAMP);
                    data.put("changedAt", ServerValue.TIMESTAMP);
                    tag.setValue(data).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            UserData.getInstance().addNewItemHash(hash);
                            UserData.getInstance().touch(null);
                            createDeferred.resolve(aVoid);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            e.printStackTrace();
                            createDeferred.reject(e);
                        }
                    });
                } else {
                    newTag.key = stage.first;
                    tagRef.child(newTag.key).runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData currentData) {
                            if (currentData != null && currentData.getValue() != null) {
                                Map<String, Object> tagData = (Map<String, Object>) currentData.getValue();
                                tagData.put("counter", (Long) tagData.get("counter") + (UserData.getInstance().hasNewItemHash(hash) == 0 ? 1 : 0));
                                tagData.put("changedAt", ServerValue.TIMESTAMP);
                                currentData.setValue(tagData);
                            }
                            return Transaction.success(currentData);
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean commited, DataSnapshot dataSnapshot) {
                            UserData.getInstance().addNewItemHash(hash);
                            UserData.getInstance().touch(null);
                            if (databaseError != null) {
                                Exception e = databaseError.toException();
                                e.printStackTrace();
                                createDeferred.reject(e);
                            } else {
                                createDeferred.resolve(null);
                            }
                        }
                    });
                }
            }
        }).fail(new FailCallback<Exception>() {
            @Override
            public void onFail(Exception e) {
                createDeferred.reject(e);
            }
        });
        createDeferred.promise().done(new DoneCallback<Void>() {
            @Override
            public void onDone(Void Void) {
                getStageTag(newTag.key, newTag.langCode).done(new DoneCallback<StageTag>() {
                    @Override
                    public void onDone(StageTag tag) {
                        if (tag != null) {
                            Analytics.getInstance().logNewTag(tag);
                        }
                        deferred.resolve(tag);
                    }
                }).fail(new FailCallback<Exception>() {
                    @Override
                    public void onFail(Exception e) {
                        deferred.reject(e);
                    }
                });
            }
        }).fail(new FailCallback<Exception>() {
            @Override
            public void onFail(Exception e) {
                deferred.reject(e);
            }
        });
        return deferred.promise();
    }

    public Promise<String, Exception, Void> createMessage(String content) {
        final Deferred<String, Exception, Void> deferred = new DeferredObject<>();
        if (messagesRef != null) {
            final DatabaseReference messageRef = messagesRef.push();
            Map<String, Object> data = new HashMap<>();
            data.put("content", content);
            data.put("match", "");
            data.put("date", ServerValue.TIMESTAMP);
            messageRef.setValue(data).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    deferred.resolve(messageRef.getKey());
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                    deferred.reject(e);
                }
            });
        } else {
            deferred.reject(new NetworkErrorException());
        }
        return deferred.promise();
    }

    public Promise<Void, Exception, Void> observeMessageMatch(String key, final DatabaseRefListener<String> databaseRefListener) {
        final Deferred<Void, Exception, Void> deferred = new DeferredObject<>();
        stopObserveMessageMatch();
        if (messagesRef != null) {
            messageMatchRef = messagesRef.child(key).child("match");
            messageMatchEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        if (deferred.state() == Promise.State.PENDING) {
                            deferred.resolve(null);
                        }
                    } else {
                        if (deferred.state() == Promise.State.PENDING) {
                            deferred.resolve(null);
                        }
                        String value = (String) dataSnapshot.getValue();
                        if (!TextUtils.isEmpty(value)) {
                            if (databaseRefListener != null) {
                                databaseRefListener.updated(value);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    if (deferred.state() == Promise.State.PENDING) {
                        deferred.reject(databaseError.toException());
                    }
                }
            };
            messageMatchRef.addValueEventListener(messageMatchEventListener);
        } else {
            deferred.reject(new NetworkErrorException());
        }
        return deferred.promise();
    }

    public void stopObserveMessageMatch() {
        if (messageMatchEventListener != null && messageMatchRef != null) {
            messageMatchRef.removeEventListener(messageMatchEventListener);
            messageMatchEventListener = null;
            messageMatchRef = null;
        }
    }

    public Promise<Void, Exception, Void> updateMessageMatch(String key, String match) {
        final Deferred<Void, Exception, Void> deferred = new DeferredObject<>();
        if (messagesRef != null) {
            final DatabaseReference messageRef = messagesRef.child(key).child("match");
            messageRef.setValue(match).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    deferred.resolve(aVoid);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                    deferred.reject(e);
                }
            });
        } else {
            deferred.reject(new NetworkErrorException());
        }
        return deferred.promise();
    }

    public Promise<String, Exception, Void> getMessageContent(String key) {
        final Deferred<String, Exception, Void> deferred = new DeferredObject<>();
        if (messagesRef != null) {
            final DatabaseReference messageRef = messagesRef.child(key).child("content");
            messageRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        deferred.resolve(null);
                    } else {
                        deferred.resolve((String) dataSnapshot.getValue());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    deferred.reject(databaseError.toException());
                }
            });
        } else {
            deferred.reject(new NetworkErrorException());
        }
        return deferred.promise();
    }

    public Promise<Void, Exception, Void> removeMessage(String key) {
        final Deferred<Void, Exception, Void> deferred = new DeferredObject<>();
        if (messagesRef != null) {
            final DatabaseReference messageRef = messagesRef.child(key);
            messageRef.setValue(null).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    deferred.resolve(aVoid);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                    deferred.reject(e);
                }
            });
        } else {
            deferred.reject(new NetworkErrorException());
        }
        return deferred.promise();
    }

    public Promise<Void, Exception, Void> createMatchPart(String key, boolean isFirst, String session) {
        final Deferred<Void, Exception, Void> deferred = new DeferredObject<>();
        if (matchesRef != null) {
            final DatabaseReference matchRef = matchesRef.child(key);
            Map<String, Object> data = new HashMap<>();
            data.put("date", ServerValue.TIMESTAMP);
            if (isFirst) {
                Map<String, Object> first = new HashMap<>();
                first.put("active", true);
                first.put("session", session != null ? session : "");
                data.put("first", first);
            } else {
                Map<String, Object> second = new HashMap<>();
                second.put("active", true);
                second.put("session", session != null ? session : "");
                data.put("second", second);
            }
            matchRef.updateChildren(data).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    deferred.resolve(aVoid);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                    deferred.reject(e);
                }
            });
        } else {
            deferred.reject(new NetworkErrorException());
        }
        return deferred.promise();
    }

    private MatchStatus parseMatchStatusSnapshot(DataSnapshot dataSnapshot) {
        MatchStatus data = new MatchStatus();
        DataSnapshot first = dataSnapshot.child("first");
        DataSnapshot second = dataSnapshot.child("second");
        data.active1 = first != null ? (Boolean) first.child("active").getValue() : null;
        data.session1 = first != null ? (String) first.child("session").getValue() : null;
        data.active2 = second != null ? (Boolean) second.child("active").getValue() : null;
        data.session2 = second != null ? (String) second.child("session").getValue() : null;
        return data;
    }

    public Promise<Void, Exception, Void> observeMatchStatus(String key, final DatabaseRefListener<MatchStatus> databaseRefListener) {
        final Deferred<Void, Exception, Void> deferred = new DeferredObject<>();
        stopObserveMatchStatus();
        if (matchesRef != null) {
            matchRef = matchesRef.child(key);
            matchStatusEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        if (deferred.state() == Promise.State.PENDING) {
                            deferred.resolve(null);
                        }
                    } else {
                        if (deferred.state() == Promise.State.PENDING) {
                            deferred.resolve(null);
                        }
                        if (databaseRefListener != null) {
                            databaseRefListener.updated(parseMatchStatusSnapshot(dataSnapshot));
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    if (deferred.state() == Promise.State.PENDING) {
                        deferred.reject(databaseError.toException());
                    }
                }
            };
            matchRef.addValueEventListener(matchStatusEventListener);
        } else {
            deferred.reject(new NetworkErrorException());
        }
        return deferred.promise();
    }

    public void stopObserveMatchStatus() {
        if (matchStatusEventListener != null && matchRef != null) {
            matchRef.removeEventListener(matchStatusEventListener);
            matchStatusEventListener = null;
            matchRef = null;
        }
    }

    public Promise<Void, Exception, Void> setMatchPartInactive(String key, boolean isFirst) {
        final Deferred<Void, Exception, Void> deferred = new DeferredObject<>();
        if (matchesRef != null) {
            final DatabaseReference matchRef;
            if (isFirst) {
                matchRef = matchesRef.child(key).child("first/active");
            } else {
                matchRef = matchesRef.child(key).child("second/active");
            }
            matchRef.setValue(false).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    deferred.resolve(aVoid);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                    deferred.reject(e);
                }
            });
        } else {
            deferred.reject(new NetworkErrorException());
        }
        return deferred.promise();
    }

    public Promise<Void, Exception, Void> removeInactiveMatch(String key) {
        final Deferred<Void, Exception, Void> deferred = new DeferredObject<>();
        if (matchesRef != null) {
            final DatabaseReference matchRef = matchesRef.child(key);
            matchRef.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData currentData) {
                    if (currentData != null && currentData.getValue() != null) {
                        Map<String, Object> data = (Map<String, Object>) currentData.getValue();
                        Map<String, Object> first = (Map<String, Object>) data.get("first");
                        Map<String, Object> second = (Map<String, Object>) data.get("second");
                        if ((first == null || !((Boolean) first.get("active"))) && (second == null || !((Boolean) second.get("active")))) {
                            currentData.setValue(null);
                            return Transaction.success(currentData);
                        } else {
                            return Transaction.abort();
                        }
                    }
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean commited, DataSnapshot dataSnapshot) {
                    if (databaseError != null) {
                        Exception e = databaseError.toException();
                        e.printStackTrace();
                        deferred.reject(e);
                    } else {
                        deferred.resolve(null);
                    }
                }
            });
        } else {
            deferred.reject(new NetworkErrorException());
        }
        return deferred.promise();
    }

    public Promise<Map<String, Object>, Exception, Void> fetchSpaceMeta(String space) {
        final Deferred<Map<String, Object>, Exception, Void> deferred = new DeferredObject<>();
        DatabaseReference spaceRef = ref != null ? ref.child(space) : null;
        if (spaceRef != null) {
            DatabaseReference spaceMetaRef = spaceRef.child("meta");
            spaceMetaRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        deferred.resolve(null);
                    } else {
                        if (!parseIgnore(dataSnapshot)) {
                            Map<String, Object> spaceMeta = (Map<String, Object>) dataSnapshot.getValue();
                            deferred.resolve(spaceMeta);
                        } else {
                            deferred.resolve(null);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    deferred.reject(databaseError.toException());
                }
            });
        } else {
            deferred.reject(new NetworkErrorException());
        }
        return deferred.promise();
    }

    public Promise<Boolean, Exception, Void> verifySpaceProtection(String space, String accessCode) {
        final Deferred<Boolean, Exception, Void> deferred = new DeferredObject<>();
        DatabaseReference spaceRef = ref != null ? ref.child(space) : null;
        if (spaceRef != null) {
            DatabaseReference spaceProtectionRef = spaceRef.child("protection").child(accessCode);
            spaceProtectionRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        deferred.resolve(false);
                    } else {
                        deferred.resolve(true);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    deferred.reject(databaseError.toException());
                }
            });
        } else {
            deferred.reject(new NetworkErrorException());
        }
        return deferred.promise();
    }

    public Promise<Void, Exception, Void> shareHighscore(String alias, long value, long count) {
        final Deferred<Void, Exception, Void> deferred = new DeferredObject<>();
        if (scoresRef != null) {
            final DatabaseReference scoreRef = scoresRef.child(UserData.getInstance().getInstallationUUID());
            Map<String, Object> data = new HashMap<>();
            data.put("alias", alias);
            data.put("value", value);
            data.put("count", count);
            scoreRef.setValue(data).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    deferred.resolve(aVoid);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                    deferred.reject(e);
                }
            });
        } else {
            deferred.reject(new NetworkErrorException());
        }
        return deferred.promise();
    }

    public Promise<List<Map<String, Object>>, Exception, Void> fetchHighscore() {
        final Deferred<List<Map<String, Object>>, Exception, Void> deferred = new DeferredObject<>();
        if (scoresRef != null) {
            final Query scoresQuery = scoresRef.orderByChild("value").limitToLast(100);
            scoresQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Map<String, Object>> scores = new ArrayList<>();
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            scores.add((Map<String, Object>) snapshot.getValue());
                        }
                    }
                    Collections.sort(scores, new Comparator<Map<String, Object>>() {
                        @Override
                        public int compare(Map<String, Object> score1, Map<String, Object> score2) {
                            return ((Long) score2.get("value")).compareTo((Long) score1.get("value"));
                        }
                    });
                    deferred.resolve(scores);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    deferred.reject(databaseError.toException());
                }
            });
        } else {
            deferred.reject(new NetworkErrorException());
        }
        return deferred.promise();
    }

    public void logEvent(String event, Bundle parameters) {
        if (analyticsRef != null) {
            Date date = new Date();
            Calendar calender = Calendar.getInstance();
            calender.setTime(date);
            int year = calender.get(Calendar.YEAR);
            int month = calender.get(Calendar.MONTH) + 1;
            int day = calender.get(Calendar.DAY_OF_MONTH);
            Map<String, Object> data = new HashMap<>();
            for (String key : parameters.keySet()) {
                data.put(key, parameters.get(key));
            }
            data.put("event", event);
            data.put("date", ServerValue.TIMESTAMP);
            data.put("year", year);
            data.put("month", month);
            data.put("day", day);
            data.put("cluster", year + "/" + String.format(Locale.getDefault(), "%02d", month));
            data.put("installation", UserData.getInstance().getInstallationUUID());
            data.put("space", SecureStore.getSpaceRefName());
            data.put("language", UserData.getInstance().getLangCode());
            data.put("gender", UserData.getInstance().getGender().code);
            data.put("birthYear", UserData.getInstance().getBirthYear());
            data.put("age", UserData.getInstance().getAge());
            data.put("defaultMatchMode", UserData.getInstance().getMatchMode().toExternalString());
            data.put("defaultMatchCode", UserData.getInstance().getMatchMode().code);
            data.put("device", "Android");
            data.put("deviceLanguage", Locale.getDefault().getLanguage());
            data.put("deviceLocale", Locale.getDefault().toString());
            analyticsRef.push().setValue(data);
        }
    }
}