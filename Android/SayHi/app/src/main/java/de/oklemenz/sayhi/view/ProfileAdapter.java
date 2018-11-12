package de.oklemenz.sayhi.view;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.model.Enum;
import de.oklemenz.sayhi.model.Profile;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.Emoji;
import de.oklemenz.sayhi.service.Utilities;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class ProfileAdapter extends ArrayAdapter<Profile> {

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

    public ProfileAdapter(Context context) {
        super(context, -1, UserData.getInstance().profiles);
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public Profile getProfile(int position) {
        return UserData.getInstance().profiles.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Profile profile = UserData.getInstance().profiles.get(position);
        return getRow(profile, convertView, parent, position);
    }

    protected View getRow(Profile profile, View convertView, ViewGroup parent, int position) {
        ViewHolder holder;

        if (convertView == null || !(convertView.getTag() instanceof ViewHolder)) {
            convertView = inflater.inflate(R.layout.profile_item, parent, false);
            holder = new ViewHolder();
            holder.rowView = convertView;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        View rowView = holder.rowView;

        boolean rowEditing = (editing || editingPos == position);

        Button moreButton = (Button) rowView.findViewById(R.id.moreButton);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) moreButton.getLayoutParams();
        layoutParams.width = rowEditing ? RelativeLayout.LayoutParams.WRAP_CONTENT : 0;
        moreButton.setEnabled(rowEditing);
        moreButton.setLayoutParams(layoutParams);
        moreButton.setTag(position);

        Button deleteButton = (Button) rowView.findViewById(R.id.deleteButton);
        layoutParams = (RelativeLayout.LayoutParams) deleteButton.getLayoutParams();
        layoutParams.width = rowEditing && profile.id != UserData.getInstance().currentProfileId ? RelativeLayout.LayoutParams.WRAP_CONTENT : 0;
        deleteButton.setEnabled(rowEditing && profile.id != UserData.getInstance().currentProfileId);
        deleteButton.setLayoutParams(layoutParams);
        deleteButton.setTag(position);

        ImageButton profileDetailButton = (ImageButton) rowView.findViewById(R.id.profileDetailButton);
        layoutParams = (RelativeLayout.LayoutParams) profileDetailButton.getLayoutParams();
        layoutParams.width = rowEditing ? 0 : RelativeLayout.LayoutParams.WRAP_CONTENT;
        profileDetailButton.setLayoutParams(layoutParams);
        profileDetailButton.setTag(position);

        ImageButton activeProfileButton = (ImageButton) rowView.findViewById(R.id.activeProfileButton);
        activeProfileButton.setColorFilter(AppDelegate.AccentColor);
        if (profile.id == UserData.getInstance().currentProfileId) {
            activeProfileButton.setImageResource(R.drawable.circle_checked);
        } else {
            activeProfileButton.setImageResource(R.drawable.circle);
        }
        activeProfileButton.setTag(position);

        TextView textLabel = (TextView) rowView.findViewById(R.id.textLabel);
        long now = System.currentTimeMillis();
        String relativeDate = DateUtils.getRelativeTimeSpanString(profile.date.getTime(), now, DateUtils.SECOND_IN_MILLIS).toString();
        String labelHTML = profile.name + "&nbsp;&nbsp;<font color='" + AppDelegate.AccentColor + "'><small>" + relativeDate + "</small></font>";
        String detailLabelHTML = "";
        String separator = "";
        if (profile.relationType != Enum.RelationType.None) {
            detailLabelHTML += "<font color='" + Color.BLACK + "'><small>" + Emoji.getRelationType(Emoji.Size.Medium) + context.getString(Utilities.getStringResourceId(context, profile.relationType.toString())) + "</small></font>";
            separator = AppDelegate.SeparatorString;
        }
        if (profile.matchMode != null) {
            detailLabelHTML += "<font color='" + Color.BLACK + "'><small>" + separator + Emoji.getMatchingMode(Emoji.Size.Medium) + context.getString(Utilities.getStringResourceId(context, profile.matchMode.toString())) + "</small></font>";
        }
        if (!TextUtils.isEmpty(detailLabelHTML)) {
            labelHTML += "<br/>" + detailLabelHTML;
        }
        textLabel.setText(Utilities.toSpan(labelHTML));

        return rowView;
    }
}