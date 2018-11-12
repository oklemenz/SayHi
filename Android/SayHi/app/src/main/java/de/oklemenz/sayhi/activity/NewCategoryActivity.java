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
import android.widget.TableRow;
import android.widget.TextView;

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
import de.oklemenz.sayhi.model.NewCategory;
import de.oklemenz.sayhi.model.Settings;
import de.oklemenz.sayhi.model.StageCategory;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.DataService;
import de.oklemenz.sayhi.service.Utilities;
import de.oklemenz.sayhi.view.CategoryCell;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;

/**
 * Created by Oliver Klemenz on 15.02.17.
 */

public class NewCategoryActivity extends BaseActivity {

    public interface Callback {
        void completion();
    }

    public final static String DELEGATE = "delegate";

    public final static int CategoryMaxWidthFromScreenBias = 140;

    @Bind(R.id.doneButton)
    Button doneButton;

    @Bind(R.id.cancelButton)
    Button cancelButton;

    @Bind(R.id.titleLabel)
    TextView titleLabel;

    @Bind(R.id.languageLabel)
    TextView languageLabel;

    @Bind(R.id.nameEditText)
    EditText nameEditText;

    @Bind(R.id.primaryLanguageLabel)
    TextView primaryLanguageLabel;

    @Bind(R.id.addPrimaryLangCategoryButton)
    ImageButton addPrimaryLangCategoryButton;

    @Bind(R.id.primaryLangCategoryContainer)
    LinearLayout primaryLangCategoryContainer;

    @Bind(R.id.primaryReferenceCell)
    TableRow primaryReferenceCell;
    @Bind(R.id.primaryReferenceLangCell)
    TableRow primaryReferenceLangCell;
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
    private Category primaryLangCategory;

    private View primaryLangCategoryView;
    private CategoryCell primaryLangCategoryCell;

    private NewCategory newCategory = new NewCategory();

    private Timer timer;
    private boolean submitted = false;
    private boolean checking = false;
    private boolean cancelled = false;
    private List<Category> similarCategories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_category);

        Intent intent = getIntent();
        if (intent.getStringExtra("contentLangCode") != null) {
            contentLangCode = intent.getStringExtra("contentLangCode");
        }
        newCategory.langCode = contentLangCode;
        if (intent.getBooleanExtra("primaryMode", false)) {
            primaryMode = true;
        }
        primaryLangCategory = (Category) intent.getSerializableExtra("primaryLangCategory");

        updateTitle();
        updateContent();

        getWindow().setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_HIDDEN | SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkDoneActive();
    }

    private void updateTitle() {
        String titleHTML = "<b><font color='" + AppDelegate.AccentColor + "'>" + this.getString(R.string.NewCategory) + "</font></b>";
        titleHTML += "<br/><font color='" + Color.BLACK + "'><small>" + new Locale(contentLangCode).getDisplayLanguage() + "</font></small>";
        titleLabel.setText(Utilities.toSpan(titleHTML));
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
                newCategory.setName(s.toString());
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

        if (hidePrimaryReferencePrimaryLang()) {
            primaryReferenceCell.setVisibility(View.GONE);
            primaryReferenceLangCell.setVisibility(View.GONE);
            primaryReferenceCategoryCell.setVisibility(View.GONE);
            primaryReferenceHintCell.setVisibility(View.GONE);
        }

        if (primaryLangCategory != null) {
            setPrimaryLangCategory(primaryLangCategory);
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
                checkSimilarCategory(new Callback() {
                    @Override
                    public void completion() {
                        createCategory();
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
                            AlertDialog.Builder builder = new AlertDialog.Builder(NewCategoryActivity.this);
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

    public void createCategory() {
        DataService.getInstance().createCategory(newCategory).done(new DoneCallback<StageCategory>() {
            @Override
            public void onDone(StageCategory result) {
                ResultReceiver receiver = getIntent().getParcelableExtra(DELEGATE);
                Bundle resultData = new Bundle();
                resultData.putSerializable("newCategory", newCategory);
                receiver.send(Activity.RESULT_OK, resultData);
                finish();
            }
        }).fail(new FailCallback<Exception>() {
            @Override
            public void onFail(Exception result) {
                doneButton.setEnabled(true);
                submitCancel();

                AlertDialog.Builder builder = new AlertDialog.Builder(NewCategoryActivity.this);
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

    public void onCancelPressed(View view) {
        cancelled = true;
        finish();
    }

    public void didCreateNewCategory(NewCategory newCategory) {
        setPrimaryLangCategory(newCategory.getCategory());
    }

    public void didUseExistingCategory(Category category, NewCategory newCategory) {
        setPrimaryLangCategory(category);
    }

    public void setPrimaryLangCategory(Category category) {
        category.selected = true;
        newCategory.primaryLangCategory = category;

        addPrimaryLangCategoryButton.setImageResource(R.drawable.arrow_right);
        if (primaryLangCategoryCell == null) {
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            primaryLangCategoryView = inflater.inflate(R.layout.category_view, null, false);
            primaryLangCategoryView.setPadding(0, 0, 0, 0);
            primaryLangCategoryCell = new CategoryCell(primaryLangCategoryView);
            primaryLangCategoryCell.setMaxWidthScreenWithBias(CategoryMaxWidthFromScreenBias);
            primaryLangCategoryContainer.addView(primaryLangCategoryView);
        }
        primaryLangCategoryCell.setData(category);
    }

    public void checkDoneActive() {
        doneButton.setEnabled(false);
        if (timer != null) {
            timer.cancel();
        }
        if (submitted || checking || cancelled) {
            return;
        }
        if (!TextUtils.isEmpty(newCategory.getName()) && (hidePrimaryReferencePrimaryLang() || newCategory.primaryLangCategory != null)) {
            checking = true;
            DataService.getInstance().fetchSimilarCategories(newCategory.getName(), contentLangCode).done(new DoneCallback<List<Category>>() {
                @Override
                public void onDone(List<Category> categories) {
                    similarCategories = categories;
                    doneButton.setEnabled(true);
                    checking = false;
                }
            });
        }
    }

    public void checkSimilarCategory(final Callback callback) {
        if (similarCategories.size() > 0) {
            AlertDialog.Builder builder;
            final Category activeCategory = findSameActiveCategory();
            final Category stageCategory = findSameStageCategory();
            if (activeCategory != null) {
                builder = new AlertDialog.Builder(NewCategoryActivity.this);
                builder.setTitle(R.string.CategoryCreation)
                        .setMessage(String.format(this.getString(R.string.CategoryAlreadyExists), newCategory.getName()))
                        .setCancelable(false)
                        .setPositiveButton(R.string.UseExistingCategory, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                didUseSimilarCategory(activeCategory);
                            }
                        });
            } else if (stageCategory != null) {
                if (callback != null) {
                    callback.completion();
                }
                return;
            } else {
                builder = new AlertDialog.Builder(NewCategoryActivity.this);
                builder.setTitle(R.string.CategoryCreation)
                        .setMessage(R.string.SimilarCategoriesExist)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ShowSimilar, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                doneButton.setEnabled(true);
                                submitCancel();
                                Intent similarCategoriesActivity = new Intent(NewCategoryActivity.this, SimilarCategoriesActivity.class);
                                similarCategoriesActivity.putExtra("categories", (Serializable) similarCategories);
                                similarCategoriesActivity.putExtra("contentLangCode", contentLangCode);
                                similarCategoriesActivity.putExtra(NewCategoryActivity.DELEGATE, new ResultReceiver(new Handler()) {
                                    @Override
                                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                                        super.onReceiveResult(resultCode, resultData);
                                        if (resultCode == Activity.RESULT_OK) {
                                            boolean useOwn = resultData.getBoolean("useOwn", false);
                                            Category category = (Category) resultData.getSerializable("category");
                                            if (useOwn) {
                                                createCategory();
                                            } else if (category != null) {
                                                didUseSimilarCategory(category);
                                            }
                                        }
                                    }
                                });
                                NewCategoryActivity.this.startActivity(similarCategoriesActivity, TransitionState.SHOW);
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

    public Category findSameActiveCategory() {
        for (Category category : similarCategories) {
            if (!category.stage && category.getName().equals(newCategory.getName()) &&
                    category.primaryLangKey.equals(newCategory.primaryLangCategory != null ? newCategory.primaryLangCategory.key : "")) {
                return category;
            }
        }
        return null;
    }

    public Category findSameStageCategory() {
        for (Category category : similarCategories) {
            if (category.stage && category.getName().equals(newCategory.getName()) &&
                    category.primaryLangKey.equals(newCategory.primaryLangCategory != null ? newCategory.primaryLangCategory.key : "")) {
                return category;
            }
        }
        return null;
    }

    public void didUseOwnCategory() {
        createCategory();
    }

    public void didUseSimilarCategory(final Category category) {
        DataService.getInstance().completeCategory(category).done(new DoneCallback<Category>() {
            @Override
            public void onDone(Category result) {
                ResultReceiver receiver = getIntent().getParcelableExtra(DELEGATE);
                Bundle resultData = new Bundle();
                resultData.putSerializable("category", category);
                resultData.putSerializable("newCategory", newCategory);
                receiver.send(Activity.RESULT_OK, resultData);
                finish();
            }
        });
    }

    public void submitCancel() {
        submitted = false;
        cancelButton.setEnabled(true);
    }

    public void onHelpPressed(View view) {
        showHelp(view, R.layout.new_category_help);
    }

    protected Dialog showHelp(View view, View helpView) {
        if (hidePrimaryReferencePrimaryLang()) {
            helpView.findViewById(R.id.primaryRefHelp).setVisibility(View.GONE);
            helpView.findViewById(R.id.primaryRefHelpArrow).setVisibility(View.GONE);
        }
        return super.showHelp(view, helpView);
    }

    public void onCreatePrimaryLangCategoryPressed(View view) {
        Intent newCategoryActivity = new Intent(this, NewCategoryActivity.class);
        newCategoryActivity.putExtra("primaryMode", true);
        newCategoryActivity.putExtra("contentLangCode", AppDelegate.PrimaryLangCode);
        newCategoryActivity.putExtra(NewCategoryActivity.DELEGATE, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {
                    Category category = (Category) resultData.getSerializable("category");
                    NewCategory newCategory = (NewCategory) resultData.getSerializable("newCategory");
                    if (category != null) {
                        didUseExistingCategory(category, newCategory);
                    } else if (newCategory != null) {
                        didCreateNewCategory(newCategory);
                    }
                }
            }
        });
        this.startActivity(newCategoryActivity, TransitionState.SHOW);
    }
}