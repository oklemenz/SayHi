package de.oklemenz.sayhi.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.model.Tag;

import static de.oklemenz.sayhi.view.CategoryCell.CategoryMaxWidthInset;
import static de.oklemenz.sayhi.view.TagCell.TagMaxWidthInset;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class SimilarTagsAdapter extends ArrayAdapter<Tag> {

    static class ViewHolder {
        TagCell tagView;
        TagCell primaryLangTagView;
        CategoryCell catgoryView;
        CategoryCell primaryLangCategoryView;
    }

    protected final Context context;
    protected LayoutInflater inflater;

    public SimilarTagsAdapter(Context context, List<Tag> tags) {
        super(context, -1, tags);
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Tag tag = getItem(position);

        ViewHolder holder;

        if (convertView == null || !(convertView.getTag() instanceof TagCell)) {
            convertView = inflater.inflate(R.layout.similar_tag_item_view, parent, false);
            holder = new ViewHolder();
            holder.tagView = new TagCell(convertView.findViewById(R.id.tagItemView));
            holder.tagView.setMaxWidthHalfScreenWithBias(TagMaxWidthInset);
            holder.primaryLangTagView = new TagCell(convertView.findViewById(R.id.primaryLangTagItemView));
            holder.primaryLangTagView.setMaxWidthHalfScreenWithBias(TagMaxWidthInset);
            holder.catgoryView = new CategoryCell(convertView.findViewById(R.id.categoryItemView));
            holder.catgoryView.setMaxWidthHalfScreenWithBias(CategoryMaxWidthInset);
            holder.primaryLangCategoryView = new CategoryCell(convertView.findViewById(R.id.primaryLangCategoryItemView));
            holder.primaryLangCategoryView.setMaxWidthHalfScreenWithBias(CategoryMaxWidthInset);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tagView.setData(tag);

        if (tag.primaryLangTag != null) {
            holder.primaryLangTagView.setData(tag.primaryLangTag);
            holder.primaryLangTagView.itemView.setVisibility(View.VISIBLE);
            convertView.findViewById(R.id.tagPrimaryLangLabel).setVisibility(View.VISIBLE);
        } else {
            holder.primaryLangTagView.itemView.setVisibility(View.GONE);
            convertView.findViewById(R.id.tagPrimaryLangLabel).setVisibility(View.GONE);
        }

        if (tag.category != null) {
            holder.catgoryView.setData(tag.category);
            holder.catgoryView.itemView.setVisibility(View.VISIBLE);
            if (tag.category.primaryLangCategory != null) {
                holder.primaryLangCategoryView.setData(tag.category.primaryLangCategory);
                holder.primaryLangCategoryView.itemView.setVisibility(View.VISIBLE);
                convertView.findViewById(R.id.categoryPrimaryLangLabel).setVisibility(View.VISIBLE);
            } else {
                holder.primaryLangCategoryView.itemView.setVisibility(View.GONE);
                convertView.findViewById(R.id.categoryPrimaryLangLabel).setVisibility(View.GONE);
            }
        } else {
            holder.catgoryView.itemView.setVisibility(View.GONE);
            holder.primaryLangCategoryView.itemView.setVisibility(View.GONE);
            convertView.findViewById(R.id.categoryPrimaryLangLabel).setVisibility(View.GONE);
        }

        return convertView;
    }
}