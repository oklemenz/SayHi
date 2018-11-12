package de.oklemenz.sayhi.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class HighscoreAdapter extends ArrayAdapter<Map<String, Object>> {

    static class ViewHolder {
        View rowView;
    }

    protected final Context context;
    protected LayoutInflater inflater;

    protected List<Map<String, Object>> values;

    public HighscoreAdapter(Context context, List<Map<String, Object>> values) {
        super(context, -1, values);
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        this.values = values;
    }

    public List<Map<String, Object>> getValues() {
        return values;
    }

    public Map<String, Object> getValue(int position) {
        return values.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Map<String, Object> value = values.get(position);
        return getRow(value, position, convertView, parent);
    }

    protected View getRow(Map<String, Object> value, int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null || !(convertView.getTag() instanceof ViewHolder)) {
            convertView = inflater.inflate(R.layout.highscore_item, parent, false);
            holder = new ViewHolder();
            holder.rowView = convertView;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        View rowView = holder.rowView;

        TextView textLabel = (TextView) rowView.findViewById(R.id.textLabel);
        TextView valueLabel = (TextView) rowView.findViewById(R.id.valueLabel);
        if (value != null) {
            textLabel.setText((position + 1) + ". " + value.get("alias"));
            long scoreValue = (Long) value.get("value");
            long scoreCount = (Long) value.get("count");
            valueLabel.setText(String.format(context.getString(scoreValue == 1 ? R.string.PtPlain : R.string.PtsPlain) + " (%2$d)", scoreValue, scoreCount));
            valueLabel.setTextColor(AppDelegate.AccentColor);
        }
        return rowView;
    }
}