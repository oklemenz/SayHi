package de.oklemenz.sayhi.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.Category;
import de.oklemenz.sayhi.model.CategoryQuery;
import de.oklemenz.sayhi.model.NewCategory;
import de.oklemenz.sayhi.model.NewTag;
import de.oklemenz.sayhi.model.Settings;
import de.oklemenz.sayhi.model.StageTag;
import de.oklemenz.sayhi.model.Tag;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.DataService;
import de.oklemenz.sayhi.service.Emoji;
import de.oklemenz.sayhi.service.Utilities;
import de.oklemenz.sayhi.view.CategoryCell;
import de.oklemenz.sayhi.view.TagCell;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;
import static de.oklemenz.sayhi.activity.NewCategoryActivity.CategoryMaxWidthFromScreenBias;

/**
 * Created by Oliver Klemenz on 15.02.17.
 */

public class NewTagActivity extends BaseActivity {

    public interface Callback {
        void completion();
    }

    public final static String DELEGATE = "delegate";

    public final static int TagMaxWidthFromScreenBias = 100;

    @Bind(R.id.doneButton)
    Button doneButton;

    @Bind(R.id.cancelButton)
    Button cancelButton;

    @Bind(R.id.titleLabel)
    TextView titleLabel;

    @Bind(R.id.assignmentLike)
    ToggleButton assignmentLike;
    @Bind(R.id.assignmentDislike)
    ToggleButton assignmentDislike;

    @Bind(R.id.languageLabel)
    TextView languageLabel;

    @Bind(R.id.nameEditText)
    EditText nameEditText;

    @Bind(R.id.categoryLabel)
    TextView categoryLabel;

    @Bind(R.id.categoryContainer)
    LinearLayout categoryContainer;

    @Bind(R.id.primaryLanguageLabel)
    TextView primaryLanguageLabel;

    @Bind(R.id.primaryLangTagContainer)
    LinearLayout primaryLangTagContainer;
    @Bind(R.id.primaryLangCategoryContainer)
    LinearLayout primaryLangCategoryContainer;

    @Bind(R.id.addPrimaryLangTagButton)
    ImageButton addPrimaryLangTagButton;

    @Bind(R.id.assignmentCell)
    TableRow assignmentCell;
    @Bind(R.id.assignmentButtonsCell)
    TableRow assignmentButtonsCell;

    @Bind(R.id.primaryReferenceCell)
    TableRow primaryReferenceCell;
    @Bind(R.id.primaryReferenceLangCell)
    TableRow primaryReferenceLangCell;
    @Bind(R.id.primaryReferenceTagCell)
    TableRow primaryReferenceTagCell;
    @Bind(R.id.primaryReferenceCategoryCell)
    TableRow primaryReferenceCategoryCell;
    @Bind(R.id.primaryReferenceHintCell)
    TableRow primaryReferenceHintCell;

    private boolean primaryMode = false;

    private boolean hidePrimaryReferencePrimaryLang() {
        return contentLangCode.equals(AppDelegate.PrimaryLangCode) || !Settings.getInstance().getPrimaryReference();
    }

    private boolean hidePrimaryReferencePrimaryMode() {
        return primaryMode || !Settings.getInstance().getPrimaryReference();
    }

    private String contentLangCode = UserData.getInstance().getLangCode();

    private View primaryLangTagView;
    private TagCell primaryLangTagCell;
    private View categoryView;
    private CategoryCell categoryCell;
    private View primaryLangCategoryView;
    private CategoryCell primaryLangCategoryCell;

    private NewTag newTag = new NewTag();

    private Timer timer;
    private boolean submitted = false;
    private boolean checking = false;
    private boolean cancelled = false;
    private List<Tag> similarTags = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_tag);

        Intent intent = getIntent();
        if (intent.getStringExtra("contentLangCode") != null) {
            contentLangCode = intent.getStringExtra("contentLangCode");
        }
        newTag.langCode = contentLangCode;
        if (intent.getBooleanExtra("primaryMode", false)) {
            primaryMode = true;
        }
        if (intent.getSerializableExtra("primaryLangCategory") != null) {
            newTag.category = (Category) intent.getSerializableExtra("primaryLangCategory");
        }
        if (intent.getBooleanExtra("categoryNew", false)) {
            newTag.categoryNew = true;
        }

        updateTitle();
        updateContent();
        updateLabels();

        getWindow().setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_HIDDEN | SOFT_INPUT_ADJUST_RESIZE);
    }

    private void updateTitle() {
        String titleHTML = "<b><font color='" + AppDelegate.AccentColor + "'>" + this.getString(R.string.NewTag) + "</font></b>";
        titleHTML += "<br/><font color='" + Color.BLACK + "'><small>" + new Locale(contentLangCode).getDisplayLanguage() + "</font></small>";
        titleLabel.setText(Utilities.toSpan(titleHTML));
    }

    private void updateLabels() {
        if (Settings.getInstance().getLeftLabel() != null) {
            assignmentLike.setText(Utilities.toSpan(Emoji.getLike(Emoji.Size.Medium)));
        } else {
            assignmentLike.setText(Utilities.toSpan(this.getTermString("ILike", Emoji.getLike(Emoji.Size.Medium))));
        }
        if (Settings.getInstance().getRightLabel() != null) {
            assignmentDislike.setText(Utilities.toSpan(Emoji.getDislike(Emoji.Size.Medium)));
        } else {
            assignmentDislike.setText(Utilities.toSpan(this.getTermString("IDislike", Emoji.getDislike(Emoji.Size.Medium))));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkDoneActive();
    }

    private void updateContent() {

        languageLabel.setText(new Locale(contentLangCode).getDisplayLanguage());
        primaryLanguageLabel.setText(new Locale(AppDelegate.PrimaryLangCode).getDisplayLanguage());

        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                newTag.setName(s.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
                doneButton.setEnabled(false);
                if (timer != null) {
                    timer.cancel();
                }
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                checkDoneActive();
                            }
                        });
                    }
                }, 500);
            }
        });

        if (primaryMode) {
            assignmentCell.setVisibility(View.GONE);
            assignmentButtonsCell.setVisibility(View.GONE);
        }

        if (hidePrimaryReferencePrimaryLang()) {
            primaryReferenceCell.setVisibility(View.GONE);
            primaryReferenceLangCell.setVisibility(View.GONE);
            primaryReferenceTagCell.setVisibility(View.GONE);
            primaryReferenceCategoryCell.setVisibility(View.GONE);
            primaryReferenceHintCell.setVisibility(View.GONE);
        }

        if (newTag.category != null) {
            setCategory(newTag.category);
        }

        nameEditText.postDelayed(new Runnable() {
            @Override
            public void run() {
                nameEditText.requestFocus();
                showKeyboard(nameEditText);
            }
        }, 500);
    }

    public void onDonePressed(View view) {
        submitted = true;
        if (timer != null) {
            timer.cancel();
        }
        doneButton.setEnabled(false);
        cancelButton.setEnabled(false);
        hideKeyboard();
        checkSpelling(new Callback() {
            @Override
            public void completion() {
                checkSimilarTag(new Callback() {
                    @Override
                    public void completion() {
                        createTag();
                    }
                });
            }
        });
    }

    public void checkSpelling(final Callback callback) {
        TextServicesManager tsm = (TextServicesManager) getSystemService(TEXT_SERVICES_MANAGER_SERVICE);
        SpellCheckerSession session = tsm.newSpellCheckerSession(null, new Locale(contentLangCode), new SpellCheckerSession.SpellCheckerSessionListener() {
            @Override
            public void onGetSuggestions(SuggestionsInfo[] results) {
            }

            @Override
            public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
                boolean misspelledWord = false;
                for (SentenceSuggestionsInfo result : results) {
                    int n = result.getSuggestionsCount();
                    for (int i = 0; i < n; i++) {
                        if ((result.getSuggestionsInfoAt(i).getSuggestionsAttributes() & SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO) != SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO) {
                            continue;
                        }
                        misspelledWord = true;
                        break;
                    }
                    if (misspelledWord) {
                        break;
                    }
                }
                final boolean finalMisspelledWord = misspelledWord;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalMisspelledWord) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(NewTagActivity.this);
                            builder.setTitle(R.string.WordSpellingCheck)
                                    .setMessage(R.string.MisspelledWordsDetected)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            if (callback != null) {
                                                callback.completion();
                                            }
                                        }
                                    })
                                    .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            submitCancel();
                                            nameEditText.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    nameEditText.requestFocus();
                                                    showKeyboard(nameEditText);
                                                }
                                            }, 500);
                                        }
                                    });
                            final AlertDialog alert = builder.create();
                            alert.setOnShowListener(new DialogInterface.OnShowListener() {
                                @Override
                                public void onShow(DialogInterface dialogInterface) {
                                    alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                                    alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(AppDelegate.AccentColor);
                                }
                            });
                            alert.show();
                        } else {
                            if (callback != null) {
                                callback.completion();
                            }
                        }
                    }
                });

            }
        }, true);
        if (session != null) {
            session.getSentenceSuggestions(new TextInfo[]{new TextInfo(nameEditText.getText().toString())}, 1);
        } else {
            if (callback != null) {
                callback.completion();
            }
        }
    }

    public void createTag() {
        DataService.getInstance().createTag(newTag).done(new DoneCallback<StageTag>() {
            @Override
            public void onDone(StageTag result) {
                ResultReceiver receiver = getIntent().getParcelableExtra(DELEGATE);
                Bundle resultData = new Bundle();
                resultData.putSerializable("newTag", newTag);
                receiver.send(Activity.RESULT_OK, resultData);
                finish();
            }
        }).fail(new FailCallback<Exception>() {
            @Override
            public void onFail(Exception result) {
                doneButton.setEnabled(true);
                submitCancel();

                AlertDialog.Builder builder = new AlertDialog.Builder(NewTagActivity.this);
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

    public void didSelectCategory(Category category, NewCategory newCategory) {
        if (!hidePrimaryReferencePrimaryMode() && !(category.primaryLangKey.equals(newTag.primaryLangCategory != null ? newTag.primaryLangCategory.key : ""))) {
            if (newTag.primaryLangTag != null) {
                newTag.primaryLangTag.setCategoryKey("");
            }
            if (primaryLangTagView != null) {
                primaryLangTagView.setVisibility(View.GONE);
            }

            newTag.primaryLangCategory = null;
            if (primaryLangCategoryView != null) {
                primaryLangCategoryView.setVisibility(View.GONE);
            }
        }
        newTag.categoryNew = newCategory != null;
        if (newCategory != null && newCategory.primaryLangCategory != null) {
            category.deriveFrom(newCategory.primaryLangCategory);
            setPrimaryLangCategory(newCategory.primaryLangCategory);
        }
        setCategory(category);
    }

    public void setCategory(Category category) {
        category.selected = true;
        newTag.category = category;

        if (!hidePrimaryReferencePrimaryMode() && !hidePrimaryReferencePrimaryLang() && !TextUtils.isEmpty(category.primaryLangKey) && newTag.primaryLangCategory == null) {
            DataService.getInstance().getCategory(category.primaryLangKey, AppDelegate.PrimaryLangCode).done(new DoneCallback<Category>() {
                @Override
                public void onDone(Category category) {
                    if (category != null) {
                        setPrimaryLangCategory(category);
                    }
                }
            });
        }

        categoryLabel.setVisibility(View.GONE);
        if (categoryCell == null) {
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            categoryView = inflater.inflate(R.layout.category_view, null, false);
            categoryView.setPadding(0, 0, 0, 0);
            categoryCell = new CategoryCell(categoryView);
            categoryCell.setMaxWidthScreenWithBias(CategoryMaxWidthFromScreenBias);
            categoryContainer.addView(categoryView);
        }
        categoryView.setVisibility(View.VISIBLE);
        categoryCell.setData(category);

        checkDoneActive();
    }

    private void setPrimaryLangCategory(Category category) {
        category.selected = true;
        newTag.primaryLangCategory = category;

        if (primaryLangCategoryCell == null) {
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            primaryLangCategoryView = inflater.inflate(R.layout.category_view, null, false);
            primaryLangCategoryView.setPadding(0, 0, 0, 0);
            primaryLangCategoryCell = new CategoryCell(primaryLangCategoryView);
            primaryLangCategoryCell.setMaxWidthScreenWithBias(CategoryMaxWidthFromScreenBias);
            primaryLangCategoryContainer.addView(primaryLangCategoryView);
        }
        primaryLangCategoryView.setVisibility(View.VISIBLE);
        primaryLangCategoryCell.setData(category);
    }

    public void onCancelPressed(View view) {
        cancelled = true;
        finish();
    }

    public void didCreateNewTag(NewTag newTag) {
        setPrimaryLangTag(newTag.getTag());
        this.newTag.categoryNew = newTag.categoryNew;
    }

    public void didUseExistingTag(Tag tag, NewTag newTag) {
        setPrimaryLangTag(tag);
    }

    public void setPrimaryLangTag(Tag tag) {
        if ((newTag.primaryLangCategory != null && !newTag.primaryLangCategory.key.equals(tag.getCategoryKey())) ||
                (newTag.primaryLangCategory == null && tag.getCategoryKey() != null)) {
            newTag.category = null;
            if (categoryView != null) {
                categoryView.setVisibility(View.GONE);
            }
        }

        newTag.primaryLangTag = null;
        if (primaryLangTagView != null) {
            primaryLangTagView.setVisibility(View.GONE);
        }

        newTag.primaryLangCategory = null;
        if (primaryLangCategoryView != null) {
            primaryLangCategoryView.setVisibility(View.GONE);
        }

        tag.selected = true;
        if (tag.category != null) {
            tag.category.selected = true;
        }

        newTag.primaryLangTag = tag;
        newTag.primaryLangCategory = tag.category;

        if (tag.category != null) {
            setPrimaryLangCategory(tag.category);
        }
        if (!hidePrimaryReferencePrimaryMode() && !hidePrimaryReferencePrimaryLang() && !TextUtils.isEmpty(tag.getCategoryKey()) && newTag.category == null) {
            CategoryQuery query = new CategoryQuery();
            query.primaryLangKey = tag.getCategoryKey();
            query.langCode = contentLangCode;
            DataService.getInstance().fetchCategories(query).done(new DoneCallback<List<Category>>() {
                @Override
                public void onDone(List<Category> categories) {
                    if (categories.size() > 0) {
                        setCategory(categories.get(0));
                    }
                }
            });
        }

        updatePrimaryLangTag(tag);
    }

    public void updatePrimaryLangTag(Tag tag) {
        addPrimaryLangTagButton.setImageResource(R.drawable.arrow_right);
        if (primaryLangTagCell == null) {
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            primaryLangTagView = inflater.inflate(R.layout.tag_view, null, false);
            primaryLangTagView.setPadding(0, 0, 0, 0);
            primaryLangTagCell = new TagCell(primaryLangTagView);
            primaryLangTagCell.setMaxWidthScreenWithBias(TagMaxWidthFromScreenBias);
            primaryLangTagContainer.addView(primaryLangTagView);
        }
        primaryLangTagView.setVisibility(View.VISIBLE);
        primaryLangTagCell.setData(tag);
    }

    public void checkDoneActive() {
        doneButton.setEnabled(false);
        if (timer != null) {
            timer.cancel();
        }
        if (submitted || checking || cancelled) {
            return;
        }
        if (!TextUtils.isEmpty(newTag.getName()) && newTag.category != null && (hidePrimaryReferencePrimaryLang() || newTag.primaryLangTag != null)) {
            checking = true;
            DataService.getInstance().fetchSimilarTags(newTag.getName(), contentLangCode).done(new DoneCallback<List<Tag>>() {
                @Override
                public void onDone(List<Tag> tags) {
                    similarTags = tags;
                    doneButton.setEnabled(true);
                    checking = false;
                }
            });
        }
    }

    public void checkSimilarTag(final Callback callback) {
        if (similarTags.size() > 0) {
            AlertDialog.Builder builder;
            final Tag activeTag = findSameActiveTag();
            final Tag stageTag = findSameStageTag();
            if (activeTag != null) {
                builder = new AlertDialog.Builder(NewTagActivity.this);
                builder.setTitle(R.string.TagCreation)
                        .setMessage(String.format(this.getString(R.string.TagAlreadyExists), newTag.getName(), newTag.category != null ? newTag.category.getName() : ""))
                        .setCancelable(false)
                        .setPositiveButton(R.string.UseExistingTag, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                didUseSimilarTag(activeTag);
                            }
                        });
            } else if (stageTag != null) {
                if (callback != null) {
                    callback.completion();
                }
                return;
            } else {
                builder = new AlertDialog.Builder(NewTagActivity.this);
                builder.setTitle(R.string.TagCreation)
                        .setMessage(R.string.SimilarTagsExist)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ShowSimilar, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                doneButton.setEnabled(true);
                                submitCancel();
                                Intent similarTagsActivity = new Intent(NewTagActivity.this, SimilarTagsActivity.class);
                                similarTagsActivity.putExtra("tags", (Serializable) similarTags);
                                similarTagsActivity.putExtra("contentLangCode", contentLangCode);
                                similarTagsActivity.putExtra(NewTagActivity.DELEGATE, new ResultReceiver(new Handler()) {
                                    @Override
                                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                                        super.onReceiveResult(resultCode, resultData);
                                        if (resultCode == Activity.RESULT_OK) {
                                            boolean useOwn = resultData.getBoolean("useOwn", false);
                                            Tag tag = (Tag) resultData.getSerializable("tag");
                                            if (useOwn) {
                                                createTag();
                                            } else if (tag != null) {
                                                didUseSimilarTag(tag);
                                            }
                                        }
                                    }
                                });
                                NewTagActivity.this.startActivity(similarTagsActivity, TransitionState.SHOW);
                            }
                        })
                        .setNeutralButton(R.string.Continue, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (callback != null) {
                                    callback.completion();
                                }
                            }
                        });

            }
            builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    doneButton.setEnabled(true);
                    submitCancel();
                    nameEditText.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            nameEditText.requestFocus();
                            showKeyboard(nameEditText);
                        }
                    }, 500);
                }
            });

            final AlertDialog alert = builder.create();
            alert.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                    alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(AppDelegate.AccentColor);
                    if (alert.getButton(DialogInterface.BUTTON_NEUTRAL) != null) {
                        alert.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(AppDelegate.AccentColor);
                    }
                }
            });
            alert.show();
        } else {
            if (callback != null) {
                callback.completion();
            }
        }
    }

    public Tag findSameActiveTag() {
        for (Tag tag : similarTags) {
            if (!tag.stage && tag.getName().equals(newTag.getName()) &&
                    tag.getCategoryKey().equals(newTag.category != null ? newTag.category.key : null) &&
                    tag.primaryLangKey.equals(newTag.primaryLangTag != null ? newTag.primaryLangTag.key : "")) {
                return tag;
            }
        }
        return null;
    }

    public Tag findSameStageTag() {
        for (Tag tag : similarTags) {
            if (tag.stage && tag.getName().equals(newTag.getName()) &&
                    tag.getCategoryKey().equals(newTag.category != null ? newTag.category.key : null) &&
                    tag.primaryLangKey.equals(newTag.primaryLangTag != null ? newTag.primaryLangTag.key : "")) {
                return tag;
            }
        }
        return null;
    }

    public void didUseOwnTag() {
        createTag();
    }

    public void didUseSimilarTag(final Tag tag) {
        DataService.getInstance().completeTag(tag).done(new DoneCallback<Tag>() {
            @Override
            public void onDone(Tag tag) {
                ResultReceiver receiver = getIntent().getParcelableExtra(DELEGATE);
                Bundle resultData = new Bundle();
                resultData.putSerializable("tag", tag);
                resultData.putSerializable("newTag", newTag);
                receiver.send(Activity.RESULT_OK, resultData);
                finish();
            }
        });
    }

    public void submitCancel() {
        submitted = false;
        cancelButton.setEnabled(true);
    }

    public void onCreatePrimaryLangTagPressed(View view) {
        Intent newTagActivity = new Intent(this, NewTagActivity.class);
        newTagActivity.putExtra("primaryMode", true);
        newTagActivity.putExtra("contentLangCode", AppDelegate.PrimaryLangCode);
        newTagActivity.putExtra("primaryLangCategory", newTag.primaryLangCategory);
        newTagActivity.putExtra("categoryNew", newTag.categoryNew);
        newTagActivity.putExtra(NewTagActivity.DELEGATE, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {
                    Tag tag = (Tag) resultData.getSerializable("tag");
                    NewTag newTag = (NewTag) resultData.getSerializable("newTag");
                    if (tag != null) {
                        didUseExistingTag(tag, newTag);
                    } else if (newTag != null) {
                        didCreateNewTag(newTag);
                    }
                }
            }
        });
        this.startActivity(newTagActivity, TransitionState.SHOW);
    }

    public void onHelpPressed(View view) {
        showHelp(view, R.layout.new_tag_help);
    }

    protected Dialog showHelp(View view, View helpView) {
        if (primaryMode) {
            helpView.findViewById(R.id.assignmentHelp).setVisibility(View.GONE);
            helpView.findViewById(R.id.assignmentHelpArrow).setVisibility(View.GONE);

            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) helpView.findViewById(R.id.enterNameArrow).getLayoutParams();
            layoutParams.setMargins(Utilities.convertDpToPx(this, 60),
                    Utilities.convertDpToPx(this, 130), 0, 0);
            helpView.findViewById(R.id.enterNameArrow).setLayoutParams(layoutParams);

            layoutParams = (RelativeLayout.LayoutParams) helpView.findViewById(R.id.selectCategoryArrow).getLayoutParams();
            layoutParams.setMargins(0,
                    Utilities.convertDpToPx(this, 175), Utilities.convertDpToPx(this, 25), 0);
            helpView.findViewById(R.id.selectCategoryArrow).setLayoutParams(layoutParams);
        }
        if (hidePrimaryReferencePrimaryLang()) {
            helpView.findViewById(R.id.primaryRefHelp).setVisibility(View.GONE);
            helpView.findViewById(R.id.primaryRefHelpArrow).setVisibility(View.GONE);
        }
        return super.showHelp(view, helpView);
    }

    public void onSelectCategoryPressed(View view) {
        Intent searchCategoryIntent = new Intent(this, SearchCategoryActivity.class);
        searchCategoryIntent.putExtra("contentLangCode", contentLangCode);
        searchCategoryIntent.putExtra("backLabel", this.getString(R.string.NewTag));
        if (newTag.category != null) {
            searchCategoryIntent.putExtra("selectedCategoryKey", newTag.category.key);
        }
        if (newTag.categoryNew) {
            searchCategoryIntent.putExtra("primaryLangCategory", newTag.primaryLangCategory);
        }
        searchCategoryIntent.putExtra(SearchCategoryActivity.DELEGATE, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {
                    Category category = (Category) resultData.getSerializable("category");
                    NewCategory newCategory = (NewCategory) resultData.getSerializable("newCategory");
                    didSelectCategory(category, newCategory);
                }
            }
        });
        this.startActivity(searchCategoryIntent, TransitionState.SHOW);
    }

    public void onAssignmentPressed(View view) {
        hideKeyboard();
        if (view == assignmentLike) {
            newTag.assignPos = true;
        } else if (view == assignmentDislike) {
            newTag.assignPos = false;
        }
        updateAssignmentSegmentedControl();
    }

    public void updateAssignmentSegmentedControl() {
        if (newTag.assignPos) {
            assignmentLike.setChecked(true);
            assignmentDislike.setChecked(false);
        } else {
            assignmentLike.setChecked(false);
            assignmentDislike.setChecked(true);
        }
        updateLabels();
    }
}
