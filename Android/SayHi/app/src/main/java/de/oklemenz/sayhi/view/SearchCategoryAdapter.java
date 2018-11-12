package de.oklemenz.sayhi.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.util.List;

import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.model.Category;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class SearchCategoryAdapter extends ArrayAdapter<Category> {

    protected final Context context;
    protected LayoutInflater inflater;

    protected String selectedCategoryKey;

    public SearchCategoryAdapter(Context context, List<Category> categories, String selectedCategoryKey) {
        super(context, -1, categories);
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.selectedCategoryKey = selectedCategoryKey;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Category category = getItem(position);
        category.selected = category.key.equals(selectedCategoryKey);

        CategoryCell holder;

        if (convertView == null || !(convertView.getTag() instanceof CategoryCell)) {
            convertView = inflater.inflate(R.layout.category_item_view, parent, false);
            View categoryView = convertView.findViewById(R.id.categoryContainer);
            holder = new CategoryCell(categoryView);
            convertView.setTag(holder);
        } else {
            holder = (CategoryCell) convertView.getTag();
        }

        ImageView checkmarkIcon = (ImageView) convertView.findViewById(R.id.checkmarkIcon);
        if (checkmarkIcon != null) {
            checkmarkIcon.setVisibility(category.selected ? View.VISIBLE : View.GONE);
        }

        holder.setData(category);
        return convertView;
    }
}