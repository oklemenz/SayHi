package de.oklemenz.sayhi.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.shawnlin.numberpicker.NumberPicker;

import java.util.Calendar;

import butterknife.Bind;
import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.Enum;
import de.oklemenz.sayhi.model.Profile;
import de.oklemenz.sayhi.model.Settings;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.Emoji;
import de.oklemenz.sayhi.service.SwipeGestureRecognizer;
import de.oklemenz.sayhi.service.Utilities;
import de.oklemenz.sayhi.view.ProfileAdapter;

import static de.oklemenz.sayhi.model.UserData.BaseYear;

/**
 * Created by Oliver Klemenz on 15.02.17.
 */

public class ProfileActivity extends BaseActivity {

    public interface Callback {
        void completion(int index);
    }

    private int year = Calendar.getInstance().get(Calendar.YEAR);

    @Bind(R.id.editButton)
    Button editButton;

    @Bind(R.id.addButton)
    ImageButton addButton;

    @Bind(R.id.profileList)
    ListView profileList;

    private boolean editing = false;
    private int editingPos = -1;

    protected GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        profileList.setAdapter(new ProfileAdapter(this));

        profileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!editing) {
                    displayProfile(getProfileAdapter().getProfile(position));
                } else {
                    setEditing(false);
                }
            }
        });

        gestureDetector = new GestureDetector(this, new SwipeGestureRecognizer(profileList, new SwipeGestureRecognizer.Callback() {
            @Override
            public void swipeLeft(int position) {
                if (editingPos == -1) {
                    if (!editing) {
                        setEditingPos(position);
                        refresh();
                    }
                } else {
                    setEditingPos(-1);
                    refresh();
                }
            }

            @Override
            public void swipeRight(int position) {
                setEditingPos(-1);
                refresh();
            }
        }));
        profileList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    MotionEvent cancelEvent = MotionEvent.obtain(event);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                    v.onTouchEvent(cancelEvent);
                    return true;
                }
                return false;
            }
        });

        updateContent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UserData.getInstance().sortProfiles();
        refresh();
    }

    public void onHelpPressed(View view) {
        showHelp(view, R.layout.profile_help);
    }

    protected Dialog showHelp(View view, View helpView) {
        TextView profileEditMore = (TextView) helpView.findViewById(R.id.profileEditMore);
        profileEditMore.setText(Utilities.toSpan(String.format(this.getString(R.string.EditMore),
                Emoji.getRelationType(Emoji.Size.Medium),
                Emoji.getMatchingMode(Emoji.Size.Medium))));
        return super.showHelp(view, helpView);
    }

    public void updateContent() {
        if (Settings.getInstance().getDisableNewProfiles()) {
            addButton.setVisibility(View.INVISIBLE);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) addButton.getLayoutParams();
            layoutParams.width = 0;
            addButton.setLayoutParams(layoutParams);
        }
    }

    public void onAddProfilePressed(View view) {
        setEditing(false);

        if (UserData.getInstance().profiles.size() > 0 && UserData.getInstance().getBirthYear() < BaseYear) {
            addEnterBirthYearAlert(new Callback() {
                @Override
                public void completion(int index) {
                    showNewProfile();
                }
            });
        } else {
            showNewProfile();
        }
    }

    public void showNewProfile() {
        addProfile(new Callback() {
            @Override
            public void completion(int index) {
                refresh();
                profileList.smoothScrollToPosition(0);
            }
        });
    }

    public void onEditPressed(View view) {
        setEditing(!editing);
    }

    public void addProfile(final Callback callback) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint(R.string.Name);
        input.post(new Runnable() {
            @Override
            public void run() {
                showKeyboard(input);
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.NewProfile)
                .setMessage(R.string.EnterNameForThisProfile)
                .setCancelable(false)
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String name = input.getText().toString().trim();
                        int index = UserData.getInstance().addProfile(name);
                        if (callback != null) {
                            callback.completion(index);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!TextUtils.isEmpty(s));
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        input.setLayoutParams(layoutParams);
        int marginPx = Utilities.convertDpToPx(this, 20);
        alert.setView(input, marginPx, 0, marginPx, 0);
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                input.getBackground().setColorFilter(AppDelegate.AccentColor, PorterDuff.Mode.SRC_ATOP);
                alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }

    public void renameProfile(final Profile profile, final Callback callback) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setText(profile.name);
        input.setHint(R.string.Name);
        input.post(new Runnable() {
            @Override
            public void run() {
                input.setSelection(input.getText().length());
                showKeyboard(input);
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.RenameProfile)
                .setMessage(R.string.ProvideProfileName)
                .setCancelable(false)
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String name = input.getText().toString().trim();
                        int index = UserData.getInstance().renameProfile(profile, name);
                        if (callback != null) {
                            callback.completion(index);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!TextUtils.isEmpty(s));
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        input.setLayoutParams(layoutParams);
        int marginPx = Utilities.convertDpToPx(this, 20);
        alert.setView(input, marginPx, 0, marginPx, 0);
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                input.getBackground().setColorFilter(AppDelegate.AccentColor, PorterDuff.Mode.SRC_ATOP);
                alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }

    public void copyProfile(final Profile profile, final Callback callback) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setText(String.format(this.getString(R.string.New), profile.name));
        input.setHint(R.string.Name);
        input.post(new Runnable() {
            @Override
            public void run() {
                input.setSelection(input.getText().length());
                showKeyboard(input);
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.CopyProfile)
                .setMessage(R.string.EnterNameForNewProfile)
                .setCancelable(false)
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String name = input.getText().toString().trim();
                        int index = UserData.getInstance().copyProfile(profile, name);
                        if (callback != null) {
                            callback.completion(index);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!TextUtils.isEmpty(s));
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        input.setLayoutParams(layoutParams);
        int marginPx = Utilities.convertDpToPx(this, 20);
        alert.setView(input, marginPx, 0, marginPx, 0);
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                input.getBackground().setColorFilter(AppDelegate.AccentColor, PorterDuff.Mode.SRC_ATOP);
                alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }

    public void setRelationType(final Profile profile, Callback callback) {
        Intent relationTypeIntent = new Intent(this, RelationTypeActivity.class);
        relationTypeIntent.putExtra("selectedCode", profile.relationType.code);
        relationTypeIntent.putExtra(RelationTypeActivity.DELEGATE, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {
                    String code = resultData.getString("code");
                    UserData.getInstance().changeRelationType(profile, Enum.RelationType.fromCode(code));
                }
            }
        });
        this.startActivity(relationTypeIntent, TransitionState.MODAL);
    }

    public void changeMatchingModeProfile(final Profile profile, final Callback callback) {
        TextView titleView = new TextView(this);
        String titleHTML = this.getString(R.string.OverrideMatchingMode) + "<br><small>" + this.getString(R.string.ChooseCustomProfileMatchingMode) + "</small>";
        titleView.setText(Utilities.toSpan(titleHTML));
        titleView.setGravity(Gravity.START);
        titleView.setTextSize(20);
        titleView.setTextColor(Color.BLACK);
        titleView.setPadding(40, 20, 10, 10);
        titleView.invalidate();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCustomTitle(titleView)
                .setCancelable(false)
                .setNeutralButton(R.string.DefaultSetting, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        int index = UserData.getInstance().changeMatchingModeProfile(profile, null);
                        if (callback != null) {
                            callback.completion(index);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                })
                .setItems(new CharSequence[]{
                        this.getString(R.string.basic_1),
                        this.getString(R.string.exact_2),
                        this.getString(R.string.adapt_3),
                        this.getString(R.string.tries_4),
                        this.getString(R.string.open_5)
                }, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int code) {
                        int index = UserData.getInstance().changeMatchingModeProfile(profile, Enum.MatchMode.fromCode(code + 1));
                        if (callback != null) {
                            callback.completion(index);
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

    public void onItemDeletePressed(View view) {
        final Profile profile = getProfileAdapter().getProfile((int) view.getTag());
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setTitle(R.string.Profile)
                .setMessage(String.format(this.getString(R.string.DeleteProfile), profile.name))
                .setCancelable(false)
                .setPositiveButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setEditing(false);
                    }
                })
                .setNeutralButton(R.string.Delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setEditing(false);
                        UserData.getInstance().removeProfile(profile);
                        refresh();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }

    public void onItemMorePressed(View view) {
        final Profile profile = getProfileAdapter().getProfile((int) view.getTag());
        TextView titleView = new TextView(this);
        String titleHTML = profile.name + "<br><small>" + this.getString(R.string.SelectAnAction) + "</small>";
        titleView.setText(Utilities.toSpan(titleHTML));
        titleView.setGravity(Gravity.START);
        titleView.setTextSize(20);
        titleView.setTextColor(Color.BLACK);
        titleView.setPadding(40, 20, 10, 10);
        titleView.invalidate();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCustomTitle(titleView)
                .setCancelable(false)
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setEditing(false);
                    }
                })
                .setItems(new CharSequence[]{
                        this.getString(R.string.CopyProfile),
                        this.getString(R.string.RenameProfile),
                        Utilities.toSpan(String.format(this.getString(R.string.SetRelationTypeIcon), Emoji.getRelationType(Emoji.Size.ExtraLarge))),
                        Utilities.toSpan(String.format(this.getString(R.string.OverrideMatchingModeIcon), Emoji.getMatchingMode(Emoji.Size.ExtraLarge)))
                }, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int code) {
                        switch (code) {
                            case 0:
                                setEditing(false);
                                copyProfile(profile, new Callback() {
                                    @Override
                                    public void completion(int index) {
                                        refresh();
                                    }
                                });
                                break;
                            case 1:
                                setEditing(false);
                                renameProfile(profile, new Callback() {
                                    @Override
                                    public void completion(int index) {
                                        refresh();
                                    }
                                });
                                break;
                            case 2:
                                setEditing(false);
                                setRelationType(profile, new Callback() {
                                    @Override
                                    public void completion(int index) {
                                        refresh();
                                    }
                                });
                                break;
                            case 3:
                                setEditing(false);
                                changeMatchingModeProfile(profile, new Callback() {
                                    @Override
                                    public void completion(int index) {
                                        refresh();
                                    }
                                });
                                break;
                        }
                    }
                });

        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }

    public void onProfileDetailPressed(View view) {
        Profile profile = getProfileAdapter().getProfile((int) view.getTag());
        if (profile != null) {
            displayProfile(profile);
        }
    }

    public void onItemActivePressed(View view) {
        Profile profile = getProfileAdapter().getProfile((int) view.getTag());
        if (profile != null) {
            UserData.getInstance().setCurrentProfile(profile);
            UserData.getInstance().touch(null);
            refresh();
        }
    }

    private void displayProfile(Profile profile) {
        Intent tagIntent = new Intent(this, TagActivity.class);
        tagIntent.putExtra("profile", profile.id);
        tagIntent.putExtra("backLabel", this.getString(R.string.Profiles));
        this.startActivity(tagIntent, TransitionState.SHOW);
    }

    private void addEnterBirthYearAlert(final Callback callback) {
        final NumberPicker birthYearPicker = new NumberPicker(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Utilities.convertDpToPx(this, 80));
        birthYearPicker.setLayoutParams(layoutParams);
        birthYearPicker.setTextColor(AppDelegate.AccentColor);
        birthYearPicker.setDividerColor(ContextCompat.getColor(this, R.color.colorPrimary));
        birthYearPicker.setDividerDistance(Utilities.convertDpToPx(this, 18));
        birthYearPicker.setDividerThickness(Utilities.convertDpToPx(this, 0.5f));
        birthYearPicker.setTextSize(Utilities.spToPx(16f));
        birthYearPicker.setWheelItemCount(5);
        birthYearPicker.setWrapSelectorWheel(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.ActivateMultipleProfiles)
                .setMessage(R.string.ProvideBirthYear)
                .setCancelable(false)
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        UserData.getInstance().setBirthYear(year - (birthYearPicker.getValue() - BaseYear));
                        if (callback != null) {
                            callback.completion(0);
                        }
                    }
                })
                .setNegativeButton(R.string.SkipOnce, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        UserData.getInstance().setBirthYear(0);
                        if (callback != null) {
                            callback.completion(0);
                        }
                    }
                })
                .setNeutralButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        UserData.getInstance().setBirthYear(0);
                    }
                });
        final AlertDialog alert = builder.create();
        String[] birthYearDisplayValues = new String[year - BaseYear + 1];
        for (int i = 0; i < birthYearDisplayValues.length; i++) {
            birthYearDisplayValues[i] = Integer.toString(year - i);
        }
        birthYearPicker.setMinValue(BaseYear);
        birthYearPicker.setMaxValue(year);
        birthYearPicker.setDisplayedValues(birthYearDisplayValues);
        birthYearPicker.setWrapSelectorWheel(false);
        if (UserData.getInstance().getBirthYear() >= BaseYear) {
            birthYearPicker.setValue(BaseYear + (year - UserData.getInstance().getBirthYear()));
        } else {
            birthYearPicker.setValue(BaseYear + 30);
        }
        birthYearPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                UserData.getInstance().setBirthYear(year - (newVal - BaseYear));
            }
        });
        int marginPx = Utilities.convertDpToPx(this, 20);
        alert.setView(birthYearPicker, marginPx, 0, marginPx, 0);
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }

    private ProfileAdapter getProfileAdapter() {
        return ((ProfileAdapter) profileList.getAdapter());
    }

    private void refresh() {
        getProfileAdapter().notifyDataSetChanged();
    }

    private void setEditing(boolean editing) {
        this.editing = editing;
        this.editingPos = -1;
        if (this.editing) {
            editButton.setText(R.string.Done);
        } else {
            editButton.setText(R.string.Edit);
        }
        getProfileAdapter().setEditing(editing);
        refresh();
    }

    private void setEditingPos(int position) {
        this.editing = position >= 0;
        this.editingPos = position;
        if (this.editing) {
            editButton.setText(R.string.Done);
        } else {
            editButton.setText(R.string.Edit);
        }
        getProfileAdapter().setEditing(false);
        getProfileAdapter().editingPos = position;
        refresh();
    }
}
