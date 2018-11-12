package de.oklemenz.sayhi.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.model.Category;

import static de.oklemenz.sayhi.view.CategoryCell.CategoryMaxWidthInset;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class SimilarCategoriesAdapter extends ArrayAdapter<Category> {

    static class ViewHolder {
        CategoryCell catgoryView;
        CategoryCell primaryLangCategoryView;
    }

    protected final Context context;
    protected LayoutInflater inflater;

    public SimilarCategoriesAdapter(Context context, List<Category> categories) {
        super(context, -1, categories);
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Category category = getItem(position);

        ViewHolder holder;

        if (convertView == null || !(convertView.getTag() instanceof CategoryCell)) {
            convertView = inflater.inflate(R.layout.similar_category_item_view, parent, false);
            holder = new ViewHolder();
            holder.catgoryView = new CategoryCell(convertView.findViewById(R.id.categoryItemView));
            holder.catgoryView.setMaxWidthHalfScreenWithBias(CategoryMaxWidthInset);
            holder.primaryLangCategoryView = new CategoryCell(convertView.findViewById(R.id.primaryLangCategoryItemView));
            holder.primaryLangCategoryView.setMaxWidthHalfScreenWithBias(CategoryMaxWidthInset);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.catgoryView.setData(category);
        if (category.primaryLangCategory != null) {
            holder.primaryLangCategoryView.setData(category.primaryLangCategory);
            holder.primaryLangCategoryView.itemView.setVisibility(View.VISIBLE);
            convertView.findViewById(R.id.categoryPrimaryLangLabel).setVisibility(View.VISIBLE);
        } else {
            holder.primaryLangCategoryView.itemView.setVisibility(View.GONE);
            convertView.findViewById(R.id.categoryPrimaryLangLabel).setVisibility(View.GONE);
        }

        return convertView;
    }
}