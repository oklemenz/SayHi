package de.oklemenz.sayhi.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class LanguageAdapter extends ArrayAdapter<Map<String, String>> {

    static class ViewHolder {
        View rowView;
    }

    protected final Context context;
    protected LayoutInflater inflater;

    protected List<Map<String, String>> values;
    public String selectedCode;

    public LanguageAdapter(Context context, List<Map<String, String>> values, String selectedCode) {
        super(context, -1, values);
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        this.values = values;
        this.selectedCode = selectedCode;
    }

    public List<Map<String, String>> getValues() {
        return values;
    }

    public Map<String, String> getValue(int position) {
        return values.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Map<String, String> value = values.get(position);
        return getRow(value, convertView, parent);
    }

    protected View getRow(Map<String, String> value, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null || !(convertView.getTag() instanceof ViewHolder)) {
            convertView = inflater.inflate(R.layout.language_item, parent, false);
            holder = new ViewHolder();
            holder.rowView = convertView;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        View rowView = holder.rowView;

        TextView textLabel = (TextView) rowView.findViewById(R.id.textLabel);
        ImageView checkmarkIcon = (ImageView) rowView.findViewById(R.id.checkmarkIcon);
        checkmarkIcon.setColorFilter(AppDelegate.AccentColor);
        if (value != null) {
            textLabel.setText(value.get("name"));
            checkmarkIcon.setVisibility(value.get("code").equals(selectedCode) ? View.VISIBLE : View.GONE);
        }
        return rowView;
    }
}