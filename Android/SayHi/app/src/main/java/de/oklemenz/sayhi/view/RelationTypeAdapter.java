package de.oklemenz.sayhi.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.model.Enum;
import de.oklemenz.sayhi.service.Utilities;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class RelationTypeAdapter extends ArrayAdapter<Enum.RelationType> {

    protected final Context context;
    protected LayoutInflater inflater;

    public String selectedCode;

    public RelationTypeAdapter(Context context, String selectedCode) {
        super(context, -1, Enum.RelationType.list);
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        this.selectedCode = selectedCode;
    }

    public Enum.RelationType getRelationType(int position) {
        return Enum.RelationType.list.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Enum.RelationType relationType = Enum.RelationType.list.get(position);
        return getRow(relationType, parent);
    }

    protected View getRow(Enum.RelationType relationType, ViewGroup parent) {
        View rowView = inflater.inflate(R.layout.relation_type_item, parent, false);
        TextView textLabel = (TextView) rowView.findViewById(R.id.textLabel);
        ImageView checkmarkIcon = (ImageView) rowView.findViewById(R.id.checkmarkIcon);
        checkmarkIcon.setColorFilter(AppDelegate.AccentColor);
        if (relationType != null) {
            textLabel.setText(context.getString(Utilities.getStringResourceId(context, relationType.toString())));
            checkmarkIcon.setVisibility(relationType.code.equals(selectedCode) ? View.VISIBLE : View.GONE);
        }
        return rowView;
    }
}