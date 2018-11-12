package de.oklemenz.sayhi.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Locale;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.model.Enum;
import de.oklemenz.sayhi.model.Match;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.Crypto;
import de.oklemenz.sayhi.service.Emoji;
import de.oklemenz.sayhi.service.QRCode;
import de.oklemenz.sayhi.service.TagHandler;
import de.oklemenz.sayhi.service.Utilities;

import static de.oklemenz.sayhi.model.UserData.BaseYear;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class HistoryAdapter extends ArrayAdapter<Match> {

    static class ViewHolder {
        View rowView;
    }

    protected final Context context;
    protected LayoutInflater inflater;

    private boolean editing = false;

    public void setEditing(boolean editing) {
        this.editing = editing;
        editingPos = -1;
    }

    public boolean isEditing() {
        return editing;
    }

    public int editingPos = -1;

    public HistoryAdapter(Context context) {
        super(context, -1, UserData.getInstance().history);
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public Match getMatch(int position) {
        return UserData.getInstance().history.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Match match = UserData.getInstance().history.get(position);
        return getRow(match, convertView, parent, position);
    }

    protected View getRow(Match match, View convertView, ViewGroup parent, int position) {
        ViewHolder holder;

        if (convertView == null || !(convertView.getTag() instanceof ViewHolder)) {
            convertView = inflater.inflate(R.layout.history_item, parent, false);
            holder = new ViewHolder();
            holder.rowView = convertView;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        View rowView = holder.rowView;

        boolean rowEditing = (editing || editingPos == position);

        Button deleteButton = (Button) rowView.findViewById(R.id.deleteButton);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) deleteButton.getLayoutParams();
        layoutParams.width = rowEditing ? RelativeLayout.LayoutParams.WRAP_CONTENT : 0;
        deleteButton.setEnabled(rowEditing);
        deleteButton.setLayoutParams(layoutParams);
        deleteButton.setTag(position);

        ImageButton matchDetailButton = (ImageButton) rowView.findViewById(R.id.matchDetailButton);
        layoutParams = (RelativeLayout.LayoutParams) matchDetailButton.getLayoutParams();
        layoutParams.width = rowEditing ? 0 : RelativeLayout.LayoutParams.WRAP_CONTENT;
        matchDetailButton.setLayoutParams(layoutParams);
        matchDetailButton.setTag(position);

        final ImageView qrCodeIcon = (ImageView) rowView.findViewById(R.id.qrCodeIcon);
        final String qrContent = match.firstName + match.gender.code + match.age();
        if (qrCodeIcon.getTag() == null || (int) qrCodeIcon.getTag() != qrContent.hashCode()) {
            qrCodeIcon.setTag(qrContent.hashCode());
            int color = match.counted ? QRCode.QRColor : QRCode.QRInactiveColor;
            String text = "";
            try {
                text = Crypto.hash(qrContent).substring(0, Math.min(16, qrContent.length()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            QRCode.getInstance().generate(text, 100, ErrorCorrectionLevel.L, color, QRCode.QRBackgroundColor, new QRCode.Delegate() {
                @Override
                public void generationDone(Bitmap qrCode) {
                    qrCodeIcon.setImageBitmap(qrCode);
                }
            });
        }

        TextView textLabel = (TextView) rowView.findViewById(R.id.textLabel);

        String textLabelHTML = !TextUtils.isEmpty(match.firstName) ? match.firstName : context.getString(R.string.QuestionMark);
        if (match.gender != Enum.Gender.None) {
            textLabelHTML += ", " + context.getString(Utilities.getStringResourceId(context, match.gender.code + "_short"));
        }
        if (match.birthYear >= BaseYear) {
            textLabelHTML += ", " + String.format(context.getString(R.string.Y), match.age());
        }
        textLabelHTML += ", " + match.langCode.toUpperCase();
        textLabelHTML += "&nbsp;&nbsp;<font color='" + AppDelegate.AccentColor + "'><small>" +
                String.format(context.getString(R.string.LikeDislikeNum),
                        Emoji.getLike(Emoji.Size.Small),
                        match.messagePosTagCount,
                        Emoji.getDislike(Emoji.Size.Small),
                        match.messageNegTagCount) + "</small></font>";
        textLabelHTML += "&nbsp;&nbsp;<font color='" + AppDelegate.AccentColor + "'><small>" +
                (!match.counted ? "<s>" : "") + String.format(match.score() == 1 ? context.getString(R.string.Pt) : context.getString(R.string.Pts), match.score()) + (!match.counted ? "</s>" : "") +
                "</small></font>";
        textLabel.setText(Utilities.toSpan(textLabelHTML, new TagHandler()));
        textLabel.setAlpha(match.counted ? 1.0f : 0.5f);

        TextView detailTextLabel = (TextView) rowView.findViewById(R.id.detailTextLabel);
        String detailTextLabelHTML = "<font color='" + Color.BLACK + "'><small>" + match.profileName + "</small></font>";
        if (match.relationType != Enum.RelationType.None) {
            detailTextLabelHTML += "<font color='" + Color.BLACK + "'><small>" + AppDelegate.SeparatorString + Emoji.getRelationType(Emoji.Size.Medium) + context.getString(Utilities.getStringResourceId(context, match.relationType.toString())) + "</small></font>";
        }
        if (match.mode != null) {
            detailTextLabelHTML += "<font color='" + Color.BLACK + "'><small>" + AppDelegate.SeparatorString + Emoji.getMatchingMode(Emoji.Size.Medium) + context.getString(Utilities.getStringResourceId(context, match.mode.toString())) + "</small></font>";
        }
        String separator = "&nbsp;&nbsp;";
        if (!TextUtils.isEmpty(match.locationCity)) {
            detailTextLabelHTML += "<font color='" + AppDelegate.AccentColor + "'><small>" + separator + match.locationCity + "</small></font>";
            separator = ", ";
        }

        long now = System.currentTimeMillis();
        String relativeDate = DateUtils.getRelativeTimeSpanString(match.date.getTime(), now, DateUtils.SECOND_IN_MILLIS).toString();
        detailTextLabelHTML += "<font color='" + AppDelegate.AccentColor + "'><small>" +
                separator + relativeDate + "</small></font>";
        detailTextLabel.setText(Utilities.toSpan(detailTextLabelHTML));
        detailTextLabel.setAlpha(match.counted ? 1.0f : 0.5f);

        return rowView;
    }
}