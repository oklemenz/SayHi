package de.oklemenz.sayhi.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class LanguageSectionAdapter extends LanguageAdapter {

    static class ViewHolder {
        View sectionView;
    }

    protected List<Map<String, String>> values = new ArrayList<>();
    protected Map<String, Integer> sections = new HashMap<>();

    public LanguageSectionAdapter(Context context, List<Map<String, String>> values, String selectedCode) {
        super(context, values, selectedCode);
        buildSections();
    }

    public List<Map<String, String>> getValues() {
        return values;
    }

    public Map<String, String> getValue(int position) {
        return values.get(position);
    }

    private void buildSections() {
        sections.clear();
        values.clear();
        ListIterator it = super.values.listIterator();
        while (it.hasNext()) {
            int position = it.nextIndex();
            Map<String, String> value = (Map<String, String>) it.next();

            String name = value.get("name");
            String firstChar = name.substring(0, 1).toUpperCase();
            if (!sections.containsKey(firstChar)) {
                sections.put(firstChar, position);
                Map<String, String> sectionItem = new HashMap<>();
                sectionItem.put("name", firstChar);
                sectionItem.put("section", "true");
                values.add(sectionItem);
            }
            values.add(value);
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getCount() {
        return values.size();
    }

    @Override
    public Map<String, String> getItem(int position) {
        return values.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Map<String, String> value = values.get(position);
        if (value != null && value.get("section") != null) {
            return getSection(value, convertView, parent);
        }
        return getRow(value, convertView, parent);
    }

    @Override
    public boolean isEnabled(int position) {
        Map<String, String> value = values.get(position);
        if (value != null && value.get("section") != null) {
            return false;
        }
        return true;
    }

    private View getSection(Map<String, String> value, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null || !(convertView.getTag() instanceof ViewHolder)) {
            convertView = inflater.inflate(R.layout.language_section, parent, false);
            holder = new ViewHolder();
            holder.sectionView = convertView;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        View sectionView = holder.sectionView;

        TextView textLabel = (TextView) sectionView.findViewById(R.id.textLabel);
        textLabel.setTextColor(AppDelegate.AccentColor);
        if (value != null) {
            textLabel.setText(value.get("name"));
        }
        return sectionView;
    }
}
