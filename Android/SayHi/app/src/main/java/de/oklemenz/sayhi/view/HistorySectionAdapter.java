package de.oklemenz.sayhi.view;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.model.Match;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.Utilities;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class HistorySectionAdapter extends HistoryAdapter {

    static class ViewHolder {
        View sectionView;
    }

    protected List<Object> values = new ArrayList<>();
    protected Map<Date, Integer> sections = new HashMap<>();
    protected Set<Date> dayDates = new HashSet<>();

    public HistorySectionAdapter(Context context) {
        super(context);
        buildSections();
    }

    public void buildSections() {
        dayDates.clear();
        sections.clear();
        values.clear();
        Collections.sort(UserData.getInstance().history, new Comparator<Match>() {
            @Override
            public int compare(Match match1, Match match2) {
                return match2.date.compareTo(match1.date);
            }
        });
        ListIterator it = UserData.getInstance().history.listIterator();
        while (it.hasNext()) {
            int position = it.nextIndex();
            Match match = (Match) it.next();

            Date dayDate = Utilities.dayDate(match.date);
            if (!dayDates.contains(dayDate)) {
                dayDates.add(dayDate);
                sections.put(dayDate, position);
                values.add(dayDate);
            }
            values.add(match);
        }
    }

    public Match getMatch(int position) {
        Object value = values.get(position);
        if (value instanceof Match) {
            return (Match) value;
        }
        return null;
    }

    public int getMatchIndex(int position) {
        Match match = getMatch(position);
        if (match != null) {
            return UserData.getInstance().history.indexOf(match);
        }
        return -1;
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
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean isEnabled(int position) {
        return !(values.get(position) instanceof Date);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Object value = values.get(position);
        if (value instanceof Match) {
            return getRow((Match) value, convertView, parent, position);
        }
        return getSection((Date) value, convertView, parent);
    }

    private View getSection(Date date, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null || !(convertView.getTag() instanceof ViewHolder)) {
            convertView = inflater.inflate(R.layout.history_section, parent, false);
            holder = new ViewHolder();
            holder.sectionView = convertView;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        View sectionView = holder.sectionView;

        TextView textLabel = (TextView) sectionView.findViewById(R.id.textLabel);
        textLabel.setTextColor(AppDelegate.AccentColor);

        long now = System.currentTimeMillis();
        String sectionDate = DateUtils.getRelativeTimeSpanString(date.getTime(), now, DateUtils.DAY_IN_MILLIS).toString();
        textLabel.setText(sectionDate);

        return sectionView;
    }
}