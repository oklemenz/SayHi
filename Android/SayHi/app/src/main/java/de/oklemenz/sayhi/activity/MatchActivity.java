package de.oklemenz.sayhi.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xiaofeng.flowlayoutmanager.FlowLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.Enum;
import de.oklemenz.sayhi.model.Match;
import de.oklemenz.sayhi.model.Tag;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.Emoji;
import de.oklemenz.sayhi.service.Utilities;
import de.oklemenz.sayhi.view.TagAdapter;

import static de.oklemenz.sayhi.model.UserData.BaseYear;

/**
 * Created by Oliver Klemenz on 15.02.17.
 */

public class MatchActivity extends BaseActivity implements TagAdapter.Delegate {

    @Bind(R.id.bothPosTagLabel)
    TextView bothPosTagLabel;
    @Bind(R.id.bothNegTagLabel)
    TextView bothNegTagLabel;
    @Bind(R.id.onlyPosTagLabel)
    TextView onlyPosTagLabel;
    @Bind(R.id.onlyNegTagLabel)
    TextView onlyNegTagLabel;

    @Bind(R.id.bothPosTagView)
    RecyclerView bothPosTagView;
    @Bind(R.id.bothNegTagView)
    RecyclerView bothNegTagView;
    @Bind(R.id.onlyPosTagView)
    RecyclerView onlyPosTagView;
    @Bind(R.id.onlyNegTagView)
    RecyclerView onlyNegTagView;

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
    @Bind(R.id.leftContainer)
    LinearLayout leftContainer;
    @Bind(R.id.rightContainer)
    LinearLayout rightContainer;

    @Bind(R.id.horizontalDividerLine)
    View horizontalDividerLine;
    @Bind(R.id.vertialDividerContainer)
    View vertialDividerContainer;

    @Bind(R.id.titleLabel)
    TextView titleLabel;

    @Bind(R.id.statusLabel)
    TextView statusLabel;

    @Bind(R.id.locationButton)
    Button locationButton;

    @Bind((R.id.infoButton))
    ImageButton infoButton;

    private int matchIndex = 0;
    private Match match;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.match);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorBottomBar));
        }

        Intent intent = getIntent();
        matchIndex = intent.getIntExtra("match", 0);
        match = UserData.getInstance().getMatch(matchIndex);

        updateTitle();
        updateLabels();
        updateContent();
        updateLayout();
    }

    public void updateTitle() {
        if (match != null) {
            String title = !TextUtils.isEmpty(match.firstName) ? match.firstName : this.getString(R.string.QuestionMark);
            if (match.gender != Enum.Gender.None) {
                title += ", " + this.getString(Utilities.getStringResourceId(this, match.gender.code + "_short"));
            }
            if (match.birthYear >= BaseYear) {
                title += ", " + String.format(this.getString(R.string.Y), match.age());
            }
            title += ", " + match.langCode.toUpperCase();
            String titleHTML = "<b><font color='" + AppDelegate.AccentColor + "'>" + title + "</font></b>";
            String subTitle = match.profileName;
            if (match.relationType != Enum.RelationType.None) {
                subTitle += AppDelegate.SeparatorString + Emoji.getRelationType(Emoji.Size.Medium) + this.getString(Utilities.getStringResourceId(this, match.relationType.toString()));
            }
            if (match.mode != null) {
                subTitle += AppDelegate.SeparatorString + Emoji.getMatchingMode(Emoji.Size.Medium) + this.getString(Utilities.getStringResourceId(this, match.mode.toString()));
            }
            if (!TextUtils.isEmpty(subTitle)) {
                titleHTML += "<br/><small>" + subTitle + "</small>";
            }
            titleLabel.setTextColor(Color.BLACK);
            titleLabel.setText(Utilities.toSpan(titleHTML));
            titleLabel.setTypeface(null, Typeface.NORMAL);
        }
    }

    public void updateContent() {
        bothPosTagView.setLayoutManager(new FlowLayoutManager());
        TagAdapter bothPosTagAdapter = new TagAdapter(bothPosTagView, this, true, match.mode != Enum.MatchMode.Basic);
        bothPosTagView.setAdapter(bothPosTagAdapter);

        bothNegTagView.setLayoutManager(new FlowLayoutManager());
        TagAdapter bothNegTagAdapter = new TagAdapter(bothNegTagView, this, true, match.mode != Enum.MatchMode.Basic);
        bothNegTagView.setAdapter(bothNegTagAdapter);

        onlyPosTagView.setLayoutManager(new FlowLayoutManager());
        TagAdapter onlyPosTagAdapter = new TagAdapter(onlyPosTagView, this, true, match.mode == Enum.MatchMode.Open);
        onlyPosTagView.setAdapter(onlyPosTagAdapter);

        onlyNegTagView.setLayoutManager(new FlowLayoutManager());
        TagAdapter onlyNegTagAdapter = new TagAdapter(onlyNegTagView, this, true, match.mode == Enum.MatchMode.Open);
        onlyNegTagView.setAdapter(onlyNegTagAdapter);

        if (!TextUtils.isEmpty(match.status)) {
            statusLabel.setText(String.format(this.getString(R.string.MarksText), match.status));
        } else {
            statusLabel.setVisibility(View.GONE);
        }

        if ((!TextUtils.isEmpty(match.locationCity))) {
            locationButton.setText(match.locationCity);
        }

        if (TextUtils.isEmpty(match.locationLatitude) || TextUtils.isEmpty(match.locationLongitude)) {
            locationButton.setVisibility(View.GONE);
        }

        boolean bottomVisible = !TextUtils.isEmpty(match.status) || (!TextUtils.isEmpty(match.locationLatitude) && !TextUtils.isEmpty(match.locationLongitude));

        if (!bottomVisible) {
            bottomContainer.setVisibility(View.GONE);
        }

        if (TextUtils.isEmpty(match.matchLangCode) || match.matchLangCode.equals(match.langCode)) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) infoButton.getLayoutParams();
            layoutParams.width = 0;
            infoButton.setLayoutParams(layoutParams);
        }
    }

    public void updateLabels() {
        bothPosTagLabel.setText(Utilities.toSpan(String.format(this.getString(R.string.BothLikeNum),
                Emoji.getLike(Emoji.Size.Large),
                Emoji.getLike(Emoji.Size.Large),
                match.bothPosTags.size(),
                match.profilePosTagCount,
                match.messagePosTagCount)));
        bothNegTagLabel.setText(Utilities.toSpan(String.format(this.getString(R.string.BothDislikeNum),
                Emoji.getDislike(Emoji.Size.Large),
                Emoji.getDislike(Emoji.Size.Large),
                match.bothNegTags.size(),
                match.profileNegTagCount,
                match.messageNegTagCount)));
        onlyPosTagLabel.setText(Utilities.toSpan(String.format(this.getString(R.string.OnlyLikeNum),
                Emoji.getLike(Emoji.Size.Large),
                Emoji.getDislike(Emoji.Size.Large),
                match.onlyPosTags.size(),
                match.profilePosTagCount,
                match.messagePosTagCount)));
        onlyNegTagLabel.setText(Utilities.toSpan(String.format(this.getString(R.string.OnlyDislikeNum),
                Emoji.getDislike(Emoji.Size.Large),
                Emoji.getLike(Emoji.Size.Large),
                match.onlyNegTags.size(),
                match.profileNegTagCount,
                match.messageNegTagCount)));
    }

    public void updateLayout() {
        switch (match.mode) {
            case Basic:
            case Exact:
                middleContainer.setVisibility(View.GONE);
                horizontalDividerLine.setVisibility(View.GONE);
                if (match.mode == Enum.MatchMode.Basic) {
                    topRightContainer.setVisibility(View.GONE);
                    topDividerLine.setVisibility(View.GONE);
                }
                break;
            case Adapt:
                rightContainer.setVisibility(View.GONE);
                vertialDividerContainer.setVisibility(View.GONE);
                break;
            case Tries:
                leftContainer.setVisibility(View.GONE);
                vertialDividerContainer.setVisibility(View.GONE);
                break;
            case Open:
                break;
        }
    }

    public void onHelpPressed(View view) {
        showHelp(view, R.layout.match_help);
    }

    protected Dialog showHelp(View view, View helpView) {
        if (TextUtils.isEmpty(match.status)) {
            helpView.findViewById(R.id.statusHelp).setVisibility(View.GONE);
            helpView.findViewById(R.id.statusHelpArrow).setVisibility(View.GONE);
        }
        if (TextUtils.isEmpty(match.locationLatitude) || TextUtils.isEmpty(match.locationLongitude)) {
            helpView.findViewById(R.id.locationHelp).setVisibility(View.GONE);
            helpView.findViewById(R.id.locationHelpArrow).setVisibility(View.GONE);
        }
        switch (match.mode) {
            case Basic:
                helpView.findViewById(R.id.topContainerRight).setVisibility(View.GONE);
            case Exact:
                helpView.findViewById(R.id.middleContainer).setVisibility(View.GONE);
                break;
            case Adapt:
                helpView.findViewById(R.id.rightContainer).setVisibility(View.GONE);
                break;
            case Tries:
                helpView.findViewById(R.id.leftContainer).setVisibility(View.GONE);
                break;
            case Open:
                break;
        }
        ((TextView) helpView.findViewById(R.id.showsTagsBothLike)).setText(this.getTermString("ShowsTagsBothLike", Emoji.getLike(Emoji.Size.None)));
        if (TextUtils.isEmpty(((TextView) helpView.findViewById(R.id.showsTagsBothLike)).getText())) {
            helpView.findViewById(R.id.showsTagsBothLikeArrow).setVisibility(View.INVISIBLE);
        }
        ((TextView) helpView.findViewById(R.id.showsTagsBothDislike)).setText(this.getTermString("ShowsTagsBothDislike", Emoji.getDislike(Emoji.Size.None)));
        if (TextUtils.isEmpty(((TextView) helpView.findViewById(R.id.showsTagsBothDislike)).getText())) {
            helpView.findViewById(R.id.showsTagsBothDislikeArrow).setVisibility(View.INVISIBLE);
        }
        ((TextView) helpView.findViewById(R.id.showsTagsLikeDislike)).setText(this.getTermString("ShowsTagsLikeDislike", Emoji.getLike(Emoji.Size.None), Emoji.getDislike(Emoji.Size.None)));
        if (TextUtils.isEmpty(((TextView) helpView.findViewById(R.id.showsTagsLikeDislike)).getText())) {
            helpView.findViewById(R.id.showsTagsLikeDislikeArrow).setVisibility(View.INVISIBLE);
        }
        ((TextView) helpView.findViewById(R.id.showsTagsDislikeLike)).setText(this.getTermString("ShowsTagsDislikeLike", Emoji.getDislike(Emoji.Size.None), Emoji.getLike(Emoji.Size.None)));
        if (TextUtils.isEmpty(((TextView) helpView.findViewById(R.id.showsTagsDislikeLike)).getText())) {
            helpView.findViewById(R.id.showsTagsDislikeLikeArrow).setVisibility(View.INVISIBLE);
        }
        return super.showHelp(view, helpView);
    }

    public void onCurrentProfilePressed(View view) {
        Intent tagIntent = new Intent(this, TagActivity.class);
        tagIntent.putExtra("profile", UserData.getInstance().currentProfileId);
        tagIntent.putExtra("backLabel", this.getString(R.string.Match));
        tagIntent.putExtra("readOnly", true);
        startActivity(tagIntent, TransitionState.SHOW);
    }

    public void onLocationPressed(View view) {
        Intent locationIntent = new Intent(this, LocationActivity.class);
        locationIntent.putExtra("match", matchIndex);
        startActivity(locationIntent, TransitionState.SHOW);
    }

    public int getTagCount(RecyclerView owner) {
        if (owner == bothPosTagView) {
            return match.bothPosTags.size();
        } else if (owner == bothNegTagView) {
            return match.bothNegTags.size();
        } else if (owner == onlyPosTagView) {
            return match.onlyPosTags.size();
        } else if (owner == onlyNegTagView) {
            return match.onlyNegTags.size();
        }
        return 0;
    }

    public Tag getTag(RecyclerView owner, int index) {
        if (owner == bothPosTagView) {
            return match.bothPosTags.get(index);
        } else if (owner == bothNegTagView) {
            return match.bothNegTags.get(index);
        } else if (owner == onlyPosTagView) {
            return match.onlyPosTags.get(index);
        } else if (owner == onlyNegTagView) {
            return match.onlyNegTags.get(index);
        }
        return null;
    }

    public List<Tag> getTags(RecyclerView owner) {
        if (owner == bothPosTagView) {
            return match.bothPosTags;
        } else if (owner == bothNegTagView) {
            return match.bothNegTags;
        } else if (owner == onlyPosTagView) {
            return match.onlyPosTags;
        } else if (owner == onlyNegTagView) {
            return match.onlyNegTags;
        }
        return new ArrayList<>();
    }

    public void updateTags(RecyclerView owner, List<Tag> tags) {
    }

    public void didEndDrop(RecyclerView view, RecyclerView sourceView, Tag tag, int position, boolean didMoveOut) {
    }

    public void onInfoPressed(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MatchActivity.this);
        builder.setTitle(R.string.MatchingDetails)
                .setMessage(String.format(
                        this.getString(R.string.MatchingDifferentLanguage),
                        new Locale(match.matchLangCode).getDisplayLanguage(),
                        new Locale(match.langCode).getDisplayLanguage()))
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
}