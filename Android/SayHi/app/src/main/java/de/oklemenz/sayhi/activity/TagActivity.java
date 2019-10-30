package de.oklemenz.sayhi.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.xiaofeng.flowlayoutmanager.FlowLayoutManager;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.Bind;
import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.Category;
import de.oklemenz.sayhi.model.Enum;
import de.oklemenz.sayhi.model.NewTag;
import de.oklemenz.sayhi.model.Profile;
import de.oklemenz.sayhi.model.Settings;
import de.oklemenz.sayhi.model.Tag;
import de.oklemenz.sayhi.model.TagQuery;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.Analytics;
import de.oklemenz.sayhi.service.DataService;
import de.oklemenz.sayhi.service.Emoji;
import de.oklemenz.sayhi.service.Mail;
import de.oklemenz.sayhi.service.NotificationCenter;
import de.oklemenz.sayhi.service.SecureStore;
import de.oklemenz.sayhi.service.Utilities;
import de.oklemenz.sayhi.view.CategoryAdapter;
import de.oklemenz.sayhi.view.TagAdapter;

import static android.view.View.GONE;
import static android.view.View.TEXT_ALIGNMENT_GRAVITY;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;
import static de.oklemenz.sayhi.AppDelegate.FavoriteCategoriesFetchedNotification;
import static de.oklemenz.sayhi.AppDelegate.NoTagAlertShown;
import static de.oklemenz.sayhi.AppDelegate.SeparatorString;
import static de.oklemenz.sayhi.model.Category.CategoryFavorite;
import static de.oklemenz.sayhi.model.Category.CategoryMore;
import static de.oklemenz.sayhi.model.Category.CategoryOwn;
import static de.oklemenz.sayhi.model.Category.CategorySearch;
import static de.oklemenz.sayhi.model.Category.CategoryStaged;

/**
 * Created by Oliver Klemenz on 03.11.16.
 */

public class TagActivity extends BaseActivity implements TagAdapter.Delegate, CategoryAdapter.Delegate, SearchView.OnQueryTextListener {

    public interface Callback {
        void completion();
    }

    @Bind(R.id.tagActivityIndicator)
    ProgressBar tagActivityIndicator;

    @Bind(R.id.categoryActivityIndicator)
    ProgressBar categoryActivityIndicator;

    @Bind(R.id.titleLabel)
    TextView titleLabel;

    @Bind(R.id.addButton)
    ImageButton addButton;

    @Bind(R.id.hiButton)
    Button hiButton;

    @Bind(R.id.posTagLabel)
    TextView posTagLabel;

    @Bind(R.id.posTagView)
    RecyclerView posTagView;

    @Bind(R.id.negTagLabel)
    TextView negTagLabel;

    @Bind(R.id.negTagView)
    RecyclerView negTagView;

    @Bind(R.id.tagLabel)
    TextView tagLabel;

    @Bind(R.id.tagSearch)
    SearchView tagSearch;

    @Bind(R.id.tagView)
    RecyclerView tagView;

    @Bind(R.id.categoryView)
    RecyclerView categoryView;

    @Bind(R.id.topContainer)
    LinearLayout topContainer;
    @Bind(R.id.topLeftContainer)
    LinearLayout topLeftContainer;
    @Bind(R.id.topRightContainer)
    LinearLayout topRightContainer;
    @Bind(R.id.topDividerLine)
    View topDividerLine;
    @Bind(R.id.middleContainer)
    LinearLayout middleContainer;
    @Bind(R.id.bottomContainer)
    RelativeLayout bottomContainer;

    @Bind(R.id.statusLabel)
    TextView statusLabel;

    private List<Category> categories = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();

    private TagQuery tagQuery = new TagQuery();

    private Profile profile;

    private boolean fetchMoreTags = false;
    private Category selectedCategory;

    private boolean readOnly = false;

    private Callback mailCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tag);

        Intent intent = getIntent();
        profile = UserData.getInstance().getProfile(intent.getIntExtra("profile", 0));
        readOnly = intent.getBooleanExtra("readOnly", false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorBottomBar));
            }
        }

        tagActivityIndicator.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        categoryActivityIndicator.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        categoryActivityIndicator.setVisibility(View.VISIBLE);

        updateTitle(profile.matchMode);
        updateContent();
        updateLayout();

        if (readOnly) {
            makeReadOnly();
        }

        setCategories();
        checkTagExists();

        NotificationCenter.getInstance().addObserver(FavoriteCategoriesFetchedNotification, this);

        getWindow().setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_HIDDEN | SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        UserData.getInstance().increaseInviteFriendSentCount();
        Analytics.getInstance().logInviteFriend();
        if (mailCallback != null) {
            mailCallback.completion();
        }
        mailCallback = null;
        Mail.getInstance().stopSendObserver();
        AppDelegate.getInstance().preventBackgroundProtect = false;
    }

    @Override
    public void notify(String name, NotificationCenter.Notification notification) {
        super.notify(name, notification);
        if (name.equals(FavoriteCategoriesFetchedNotification)) {
            setCategories();
        }
    }

    private void updateTitle(Enum.MatchMode matchMode) {
        if (profile != null) {
            if (profile.relationType != Enum.RelationType.None || matchMode != null) {
                String subTitleHTML = "";
                String separator = "";
                if (profile.relationType != Enum.RelationType.None) {
                    subTitleHTML += "<font color='" + Color.BLACK + "'><small>" + separator + Emoji.getRelationType(Emoji.Size.Medium) + this.getString(Utilities.getStringResourceId(this, profile.relationType.toString())) + "</small></font>";
                    separator = SeparatorString;
                }
                if (matchMode != null) {
                    subTitleHTML += "<font color='" + Color.BLACK + "'><small>" + separator + Emoji.getMatchingMode(Emoji.Size.Medium) + this.getString(Utilities.getStringResourceId(this, matchMode.toString())) + "</small></font>";
                }
                String titleHTML = "<b><font color='" + AppDelegate.AccentColor + "'>" + profile.name + "</font></b>";
                if (!TextUtils.isEmpty(subTitleHTML)) {
                    titleHTML += "<br/><small>" + subTitleHTML + "</small>";
                }
                titleLabel.setText(Utilities.toSpan(titleHTML));
                titleLabel.setTypeface(null, Typeface.NORMAL);
            } else {
                titleLabel.setText(profile.name);
                titleLabel.setTypeface(null, Typeface.BOLD);
            }
        }
    }

    private void updateContent() {
        Enum.MatchMode matchMode = profile.getEffectiveMatchMode();

        posTagView.setLayoutManager(new FlowLayoutManager());
        TagAdapter posTagAdapter = new TagAdapter(posTagView, this, readOnly, matchMode != Enum.MatchMode.Basic);
        posTagView.setAdapter(posTagAdapter);
        if (!readOnly) {
            posTagView.setOnDragListener(new TagAdapter.DragListener(posTagAdapter));
        }

        negTagView.setLayoutManager(new FlowLayoutManager());
        TagAdapter negTagAdapter = new TagAdapter(negTagView, this, readOnly, matchMode != Enum.MatchMode.Basic);
        negTagView.setAdapter(negTagAdapter);
        if (!readOnly) {
            negTagView.setOnDragListener(new TagAdapter.DragListener(negTagAdapter));
        }

        tagView.setLayoutManager(new FlowLayoutManager());
        TagAdapter tagAdapter = new TagAdapter(tagView, this, readOnly, false);
        tagAdapter.suppressInsideDragAndDrop = true;
        tagAdapter.suppressTagAdd = true;
        tagView.setAdapter(tagAdapter);
        if (!readOnly) {
            tagView.setOnDragListener(new TagAdapter.DragListener(tagAdapter));
        }

        categoryView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoryView.setAdapter(new CategoryAdapter(categoryView, this));

        tagSearch.setOnQueryTextListener(this);
        AutoCompleteTextView searchText = (AutoCompleteTextView) tagSearch.findViewById(tagSearch.getContext().getResources().getIdentifier("android:id/search_src_text", null, null));
        searchText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tagSearch.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));

        if (Settings.getInstance().getDisableNewTags()) {
            addButton.setVisibility(View.INVISIBLE);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) addButton.getLayoutParams();
            layoutParams.width = 0;
            addButton.setLayoutParams(layoutParams);
        }
    }

    public void updateLayout() {
        if (profile.getEffectiveMatchMode() == Enum.MatchMode.Basic) {
            topRightContainer.setVisibility(GONE);
            topDividerLine.setVisibility(GONE);
        }
    }

    public void makeReadOnly() {
        addButton.setVisibility(View.INVISIBLE);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) addButton.getLayoutParams();
        layoutParams.width = 0;
        addButton.setLayoutParams(layoutParams);
        hiButton.setVisibility(View.INVISIBLE);
        layoutParams = (RelativeLayout.LayoutParams) hiButton.getLayoutParams();
        layoutParams.width = 0;
        hiButton.setLayoutParams(layoutParams);

        tagActivityIndicator.setVisibility(GONE);
        categoryActivityIndicator.setVisibility(GONE);
        tagView.setVisibility(GONE);
        categoryView.setVisibility(GONE);
        middleContainer.setVisibility(GONE);

        if (!TextUtils.isEmpty(UserData.getInstance().getStatus())) {
            statusLabel.setVisibility(View.VISIBLE);
            statusLabel.setText(String.format(this.getString(R.string.MarksText), UserData.getInstance().getStatus()));
            statusLabel.setGravity(Gravity.CENTER | Gravity.CENTER_VERTICAL);
            statusLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        } else {
            bottomContainer.setVisibility(GONE);
        }

        updateTitle(profile.getEffectiveMatchMode());

        readOnly = true;
    }

    private void setCategories() {
        updateLabels();
        if (!DataService.getInstance().favoriteCategoriesLoaded) {
            return;
        }

        categories.clear();
        categories.add(CategoryFavorite);
        categories.add(CategorySearch);
        categories.add(CategoryOwn);
        categories.add(CategoryStaged);
        categories.add(CategoryMore);
        categories.addAll(DataService.getInstance().favoriteCategories);

        if (DataService.getInstance().favoriteCategoriesLoaded && categoryActivityIndicator.getVisibility() != GONE) {
            categoryActivityIndicator.setVisibility(GONE);
        }

        for (Category category : categories) {
            category.selected = false;
        }

        selectedCategory = categories.get(0);
        selectedCategory.selected = true;

        refreshCategories();
        tagQuery.favorite = true;
        fetchTags();
    }

    private void checkTagExists() {
        if (!NoTagAlertShown) {
            DataService.getInstance().hasTags(null).then(new DoneCallback<Boolean>() {
                @Override
                public void onDone(Boolean exists) {
                    if (!exists) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(TagActivity.this);
                        builder.setTitle(R.string.LanguageInformation)
                                .setMessage(R.string.NoTagsForLanguage)
                                .setCancelable(false)
                                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                    }
                                });
                        final AlertDialog alert = builder.create();
                        alert.setOnShowListener(new DialogInterface.OnShowListener() {
                            @Override
                            public void onShow(DialogInterface dialogInterface) {
                                alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                            }
                        });
                        alert.show();
                        NoTagAlertShown = true;
                    }
                }
            }).fail(new FailCallback<Exception>() {
                @Override
                public void onFail(Exception result) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(TagActivity.this);
                    builder.setTitle(R.string.NetworkError)
                            .setMessage(R.string.UnexpectedErrorOccurred)
                            .setCancelable(false)
                            .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
                    final AlertDialog alert = builder.create();
                    alert.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                        }
                    });
                    alert.show();
                }
            });
        }
    }

    private void refreshCategories() {
        updateLabels();
        categoryView.getAdapter().notifyDataSetChanged();
    }

    private void clearTags() {
        tags.clear();
        refreshTags();
    }

    private void fetchTags() {
        clearTags();
        String searchText = Utilities.searchNormalized(tagQuery.searchText, UserData.getInstance().getLangCode());
        if (!tagQuery.own) {
            if (!tagQuery.search || !TextUtils.isEmpty(searchText)) {
                tagActivityIndicator.setVisibility(View.VISIBLE);
                DataService.getInstance().fetchTags(tagQuery).then(new DoneCallback<List<Tag>>() {
                    @Override
                    public void onDone(List<Tag> tags) {
                        addTags(tags);
                    }
                });
            }
        } else {
            List<Tag> tags = new ArrayList<>();
            for (Tag tag : UserData.getInstance().ownTags.values()) {
                if (tag.space.equals(SecureStore.getSpaceRefName()) &&
                        tag.langCode.equals(UserData.getInstance().getLangCode()) &&
                        (TextUtils.isEmpty(searchText) || tag.search.startsWith(searchText))) {
                    tags.add(tag);
                }
                Collections.sort(tags, new Comparator<Tag>() {
                    @Override
                    public int compare(Tag tag1, Tag tag2) {
                        return tag1.getName().compareTo(tag2.getName());
                    }
                });
                addTags(tags);
            }
        }
    }

    private void addTags(List<Tag> tags) {
        tagActivityIndicator.setVisibility(GONE);
        for (Tag tag : tags) {
            boolean posTagFound = false;
            for (Tag posTag : profile.posTags) {
                if (posTag.getEffectiveKey().equals(tag.getEffectiveKey())) {
                    posTagFound = true;
                    break;
                }
            }
            boolean negTagFound = false;
            for (Tag negTag : profile.negTags) {
                if (negTag.getEffectiveKey().equals(tag.getEffectiveKey())) {
                    negTagFound = true;
                    break;
                }
            }
            if (!(posTagFound || negTagFound)) {
                this.tags.add(tag);
            }
        }
        refreshTags();
    }

    private void refreshTags() {
        updateLabels();
        tagView.scrollToPosition(0);
        tagView.getAdapter().notifyDataSetChanged();
        fetchMoreTags = false;
    }

    private void refreshPosNegTags() {
        updateLabels();
        posTagView.getAdapter().notifyDataSetChanged();
        negTagView.getAdapter().notifyDataSetChanged();
        fetchTags();
    }

    private void refreshPosTags() {
        posTagView.getAdapter().notifyDataSetChanged();
    }

    private void refreshNegTags() {
        negTagView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public boolean onQueryTextSubmit(String searchText) {
        tagQuery.searchText = searchText;
        fetchTags();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String searchText) {
        tagQuery.searchText = searchText;
        fetchTags();
        return true;
    }

    public int getTagCount(RecyclerView owner) {
        if (owner == posTagView) {
            return profile.posTags.size();
        } else if (owner == negTagView) {
            return profile.negTags.size();
        } else if (owner == tagView) {
            return tags.size();
        }
        return 0;
    }

    public Tag getTag(RecyclerView owner, int index) {
        if (owner == posTagView) {
            return profile.posTags.get(index);
        } else if (owner == negTagView) {
            return profile.negTags.get(index);
        } else if (owner == tagView) {
            return tags.get(index);
        }
        return null;
    }

    public List<Tag> getTags(RecyclerView owner) {
        if (owner == posTagView) {
            return profile.posTags;
        } else if (owner == negTagView) {
            return profile.negTags;
        } else if (owner == tagView) {
            return tags;
        }
        return new ArrayList<>();
    }

    public void updateTags(RecyclerView owner, List<Tag> tags) {
        if (owner == posTagView) {
            profile.posTags = tags;
        } else if (owner == negTagView) {
            profile.negTags = tags;
        } else if (owner == tagView) {
            return;
        }
    }

    public void didEndDrop(final RecyclerView view, RecyclerView sourceView, Tag tag, final int position, boolean didMoveOut) {
        int previousValue = 0;
        if (sourceView == tagView) {
            previousValue = 0;
        } else if (sourceView == posTagView) {
            previousValue = 1;
        } else if (sourceView == negTagView) {
            previousValue = 0;
        }
        if (view == tagView) {
            if (didMoveOut) {
                fetchTags();
            } else {
                fetchTags();
            }
            Analytics.getInstance().logNeutral(tag, previousValue);
            refreshPosNegTags();
        } else if (view == posTagView) {
            Analytics.getInstance().logPositive(tag, previousValue);
            refreshPosTags();
        } else if (view == negTagView) {
            Analytics.getInstance().logNegative(tag, previousValue);
            refreshNegTags();
        }
        updateLabels();
        profile.touch(null);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (position < 0 && view.getAdapter().getItemCount() - 1 >= 0) {
                    view.smoothScrollToPosition(view.getAdapter().getItemCount() - 1);
                }
            }
        }, 500);
    }

    private void updateLabels() {
        posTagLabel.setText(Utilities.toSpan(String.format(this.getString(R.string.LikeNum), Emoji.getLike(Emoji.Size.Large), profile.posTags.size())));
        negTagLabel.setText(Utilities.toSpan(String.format(this.getString(R.string.DislikeNum), Emoji.getDislike(Emoji.Size.Large), profile.negTags.size())));

        if (selectedCategory == null) {
            tagLabel.setText(this.getString(R.string.ChooseFromDots));
        } else if (selectedCategory.mark > 0) {
            if (selectedCategory.mark == CategoryFavorite.mark) {
                tagLabel.setText(this.getString(R.string.ChooseFromFavorites));
            } else if (selectedCategory.mark == CategorySearch.mark) {
                tagLabel.setText(this.getString(R.string.ChooseFromSearch));
            } else if (selectedCategory.mark == CategoryOwn.mark) {
                tagLabel.setText(this.getString(R.string.ChooseFromOwn));
            } else if (selectedCategory.mark == CategoryStaged.mark) {
                tagLabel.setText(this.getString(R.string.ChooseFromUncategorized));
            } else if (selectedCategory.mark == CategoryMore.mark) {
                tagLabel.setText(this.getString(R.string.ChooseFromDots));
            }
        } else {
            tagLabel.setText(String.format(this.getString(R.string.ChooseFromCategory), selectedCategory.getName()));
        }
    }

    public int getCategoryCount(RecyclerView owner) {
        return categories.size();
    }

    public Category getCategory(RecyclerView owner, int index) {
        return categories.get(index);
    }

    public void didPressCategory(RecyclerView owner, Category category, int categoryIndex) {
        hideKeyboard();
        tagSearch.clearFocus();
        selectCategory(category);
    }

    public void selectCategory(Category category) {
        if (category.equals(selectedCategory) && category.mark != CategoryMore.mark) {
            return;
        }

        for (Category aCategory : categories) {
            aCategory.selected = false;
        }
        selectedCategory = category;
        selectedCategory.selected = true;

        boolean searchHidden = selectedCategory.mark == CategoryFavorite.mark;

        tagSearch.setQuery("", false);
        if (!searchHidden) {
            if (selectedCategory.mark == CategorySearch.mark) {
                tagSearch.setQueryHint(this.getString(R.string.Search));
            } else {
                tagSearch.setQueryHint(this.getString(R.string.Filter));
            }
        }

        tagQuery.searchText = "";
        tagQuery.favorite = selectedCategory.mark == CategoryFavorite.mark;
        tagQuery.search = selectedCategory.mark == CategorySearch.mark;
        tagQuery.own = selectedCategory.mark == CategoryOwn.mark;
        tagQuery.categoryStaged = selectedCategory.mark == CategoryStaged.mark;
        tagQuery.categoryKey = selectedCategory.mark == 0 ? category.key : "";

        int searchHeight = searchHidden ? 0 : Utilities.convertDpToPx(this, 35);
        ValueAnimator valueAnimator = ValueAnimator.ofInt(tagSearch.getHeight(), searchHeight);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                tagSearch.getLayoutParams().height = (int) animation.getAnimatedValue();
                tagSearch.requestLayout();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                tagView.smoothScrollToPosition(0);
                refreshCategories();
                if (selectedCategory.mark == CategoryMore.mark) {
                    clearTags();
                    openCategorySearch();
                } else {
                    fetchTags();
                    if (selectedCategory.mark == CategorySearch.mark) {
                        tagSearch.post(new Runnable() {
                            @Override
                            public void run() {
                                tagSearch.requestFocus();
                            }
                        });
                    }
                }
            }
        });
        valueAnimator.setDuration(200);
        valueAnimator.start();
    }

    private void openCategorySearch() {
        Intent searchCategoryIntent = new Intent(this, SearchCategoryActivity.class);
        searchCategoryIntent.putExtra("readOnly", true);
        searchCategoryIntent.putExtra(SearchCategoryActivity.DELEGATE, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {
                    Category category = (Category) resultData.getSerializable("category");
                    if (category != null) {
                        selectedCategory = category;
                        tagQuery.categoryKey = category.key;
                        refreshCategories();
                        fetchTags();
                    }
                }
            }
        });
        this.startActivity(searchCategoryIntent, TransitionState.MODAL);
    }

    private void didCreateTag(NewTag newTag) {
        assignTag(newTag.getTag(), newTag.assignPos);
    }

    private void didUseExistingTag(Tag tag, NewTag newTag) {
        assignTag(tag, newTag.assignPos);
    }

    private void assignTag(Tag tag, final boolean assignPos) {
        Tag existingPosTag = null;
        for (Tag posTag : profile.posTags) {
            if (posTag.getEffectiveKey().equals(tag.getEffectiveKey())) {
                existingPosTag = posTag;
                break;
            }
        }
        Tag existingNegTag = null;
        for (Tag negTag : profile.negTags) {
            if (negTag.getEffectiveKey().equals(tag.getEffectiveKey())) {
                existingNegTag = negTag;
                break;
            }
        }
        if (assignPos) {
            if (existingPosTag == null) {
                profile.posTags.add(tag);
            }
            if (existingNegTag != null) {
                profile.negTags.remove(existingNegTag);
            }

        } else {
            if (existingNegTag == null) {
                profile.negTags.add(tag);
            }
            if (existingPosTag != null) {
                profile.posTags.remove(existingPosTag);
            }
        }
        UserData.getInstance().addOwnTag(tag);
        profile.touch(null);
        refreshPosNegTags();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (assignPos) {
                    posTagView.smoothScrollToPosition(posTagView.getAdapter().getItemCount() - 1);
                } else {
                    negTagView.smoothScrollToPosition(negTagView.getAdapter().getItemCount() - 1);
                }
            }
        }, 500);
    }

    public void onHelpPressed(View view) {
        if (!readOnly) {
            showHelp(view, R.layout.tag_help);
        } else {
            showHelp(view, R.layout.tag_readonly_help);
        }
    }

    protected Dialog showHelp(View view, View helpView) {
        if (Settings.getInstance().getDisableNewTags()) {
            helpView.findViewById(R.id.addNewTagLabel).setVisibility(View.GONE);
            helpView.findViewById(R.id.addNewTagArrow).setVisibility(View.GONE);
        }
        if (readOnly) {
            if (TextUtils.isEmpty(UserData.getInstance().getStatus())) {
                helpView.findViewById(R.id.statusHelp).setVisibility(View.GONE);
                helpView.findViewById(R.id.statusHelpArrow).setVisibility(View.GONE);
            }
        }
        TextView dropTagsTopicsLike = (TextView) helpView.findViewById(R.id.dropTagsTopicsLike);
        if (dropTagsTopicsLike != null) {
            dropTagsTopicsLike.setText(this.getTermString("DropTagsTopicsLike", Emoji.getLike(Emoji.Size.None)));
            if (TextUtils.isEmpty(dropTagsTopicsLike.getText())) {
                helpView.findViewById(R.id.dropTagsTopicsLikeArrow).setVisibility(View.INVISIBLE);
            }
        }
        TextView dropTagsTopicsDislike = (TextView) helpView.findViewById(R.id.dropTagsTopicsDislike);
        if (dropTagsTopicsDislike != null) {
            dropTagsTopicsDislike.setText(this.getTermString("DropTagsTopicsDislike", Emoji.getDislike(Emoji.Size.None)));
            if (TextUtils.isEmpty(dropTagsTopicsDislike.getText())) {
                helpView.findViewById(R.id.dropTagsTopicsDislikeArrow).setVisibility(View.INVISIBLE);
            }
        }
        TextView showsTagsLike = (TextView) helpView.findViewById(R.id.showsTagsLike);
        if (showsTagsLike != null) {
            showsTagsLike.setText(this.getTermString("ShowsTagsLike", Emoji.getLike(Emoji.Size.None)));
            if (TextUtils.isEmpty(showsTagsLike.getText())) {
                helpView.findViewById(R.id.showsTagsLikeArrow).setVisibility(View.INVISIBLE);
            }
        }
        TextView showsTagsDislike = (TextView) helpView.findViewById(R.id.showsTagsDislike);
        if (showsTagsDislike != null) {
            showsTagsDislike.setText(this.getTermString("ShowsTagsDislike", Emoji.getDislike(Emoji.Size.None)));
            if (TextUtils.isEmpty(showsTagsDislike.getText())) {
                helpView.findViewById(R.id.showsTagsDislikeArrow).setVisibility(View.INVISIBLE);
            }
        }
        if (profile.getEffectiveMatchMode() == Enum.MatchMode.Basic) {
            helpView.findViewById(R.id.topContainerRight).setVisibility(View.GONE);
        }
        return super.showHelp(view, helpView);
    }

    public void onSayHiPressed(View view) {
        Intent qrCodeIntent = new Intent(this, QRCodeActivity.class);
        qrCodeIntent.putExtra("profile", profile.id);
        ArrayList<String> hideButtons = new ArrayList<>();
        hideButtons.add("tag");
        qrCodeIntent.putStringArrayListExtra("hideButtons", hideButtons);
        TagActivity.this.startActivity(qrCodeIntent, TransitionState.MODAL);
    }

    public void onCreateTagPressed(View view) {
        final Intent createTagIntent = new Intent(TagActivity.this, NewTagActivity.class);
        createTagIntent.putExtra(NewTagActivity.DELEGATE, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {
                    Tag tag = (Tag) resultData.getSerializable("tag");
                    NewTag newTag = (NewTag) resultData.getSerializable("newTag");
                    if (tag != null) {
                        didUseExistingTag(tag, newTag);
                    } else {
                        didCreateTag(newTag);
                    }
                }
            }
        });
        if (UserData.getInstance().inviteFriendSentCount < 2) {
            inviteFriendAlert(new Callback() {
                @Override
                public void completion() {
                startActivity(createTagIntent, TransitionState.MODAL);
                }
            });
        } else {
            startActivity(createTagIntent, TransitionState.MODAL);
        }
    }

    public void inviteFriendAlert(final Callback callback) {
        mailCallback = callback;
        TextView titleView = new TextView(this);
        int messageId = UserData.getInstance().inviteFriendSentCount == 0 ? R.string.InviteFriends1 : R.string.InviteFriends2;
        String titleHTML = this.getString(R.string.ActivateOwnTags) + "<br><small>" + this.getString(messageId) + "</small>";
        titleView.setText(Utilities.toSpan(titleHTML));
        titleView.setGravity(Gravity.START);
        titleView.setTextSize(20);
        titleView.setTextColor(Color.BLACK);
        titleView.setPadding(40, 20, 10, 10);
        titleView.invalidate();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCustomTitle(titleView)
                .setCancelable(false)
                .setNegativeButton(R.string.SkipOnce, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (callback != null) {
                            callback.completion();
                        }
                    }
                })
                .setNeutralButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                })
                .setItems(new CharSequence[]{
                        this.getString(R.string.WriteMail),
                        this.getString(R.string.SendMessage)
                }, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int code) {
                        switch (code) {
                            case 0:
                                Mail.getInstance().sendInvitationMail(TagActivity.this, null);
                                break;
                            case 1:
                                Mail.getInstance().sendInvitationMessage(TagActivity.this, null);
                                break;
                        }
                    }
                });

        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }
}