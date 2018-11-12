package de.oklemenz.sayhi.view;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.model.Category;

/**
 * Created by Oliver Klemenz on 31.10.16.
 */

public class CategoryAdapter extends RecyclerView.Adapter<CategoryCell> {

    public interface Delegate {
        int getCategoryCount(RecyclerView owner);

        Category getCategory(RecyclerView owner, int index);

        void didPressCategory(RecyclerView owner, Category category, int categoryIndex);
    }

    RecyclerView owner;
    Delegate delegate;

    public CategoryAdapter(RecyclerView owner, Delegate delegate) {
        super();
        this.owner = owner;
        this.delegate = delegate;
    }

    public void refresh() {
        this.notifyDataSetChanged();
    }

    @Override
    public CategoryCell onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_view, parent, false);
        return new CategoryCell(view);
    }

    @Override
    public void onBindViewHolder(CategoryCell holder, int position) {
        Category category = this.delegate.getCategory(this.owner, position);
        holder.setData(category);
        holder.container.setTag(position);
        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delegate.didPressCategory(owner, delegate.getCategory(owner, (int) view.getTag()), (int) view.getTag());
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.delegate.getCategoryCount(this.owner);
    }
}