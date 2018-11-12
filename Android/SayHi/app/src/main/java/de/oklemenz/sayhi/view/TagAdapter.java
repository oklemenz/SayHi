package de.oklemenz.sayhi.view;

import android.content.ClipData;
import android.support.v7.widget.RecyclerView;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.model.Tag;

/**
 * Created by Oliver Klemenz on 31.10.16.
 */

public class TagAdapter extends RecyclerView.Adapter<TagCell> {

    public interface Delegate {
        int getTagCount(RecyclerView view);

        Tag getTag(RecyclerView view, int index);

        List<Tag> getTags(RecyclerView view);

        void updateTags(RecyclerView view, List<Tag> tags);

        void didEndDrop(RecyclerView view, RecyclerView sourceView, Tag tag, int position, boolean didMoveOut);
    }

    RecyclerView owner;
    Delegate delegate;
    boolean readOnly;
    boolean halfWidth;

    public boolean suppressInsideDragAndDrop = false;
    public boolean suppressTagAdd = false;

    public TagAdapter(RecyclerView owner, Delegate delegate, boolean readOnly, boolean halfWidth) {
        super();
        this.owner = owner;
        this.delegate = delegate;
        this.readOnly = readOnly;
        this.halfWidth = halfWidth;
    }

    public void refresh() {
        this.notifyDataSetChanged();
    }

    @Override
    public TagCell onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.tag_view, parent, false);
        TagCell tagCell = new TagCell(view);
        if (halfWidth) {
            tagCell.setMaxWidthHalfScreen();
        } else {
            tagCell.setMaxWidthScreen();
        }
        return tagCell;
    }

    @Override
    public void onBindViewHolder(TagCell holder, int position) {
        Tag tag = this.delegate.getTag(this.owner, position);
        holder.setData(tag);
        holder.container.setTag(position);
        if (!readOnly) {
            holder.container.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ClipData data = ClipData.newPlainText("", "");
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                    view.startDrag(data, shadowBuilder, view, 0);
                    view.setVisibility(View.INVISIBLE);
                    return true;
                }
            });
            holder.container.setOnDragListener(new DragListener(this));
        }
    }

    @Override
    public int getItemCount() {
        return this.delegate.getTagCount(this.owner);
    }

    public List<Tag> getTags() {
        return this.delegate.getTags(this.owner);
    }

    public void updateTags(List<Tag> tags) {
        this.delegate.updateTags(this.owner, tags);
    }

    public static class DragListener implements View.OnDragListener {

        TagAdapter adapter;

        public DragListener(TagAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public boolean onDrag(View view, DragEvent event) {
            int action = event.getAction();

            View sourceView;
            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    break;

                case DragEvent.ACTION_DRAG_ENTERED:
                    break;

                case DragEvent.ACTION_DRAG_EXITED:
                    break;

                case DragEvent.ACTION_DROP:

                    int positionSource = -1;
                    int positionTarget = -1;

                    View viewSource = (View) event.getLocalState();

                    RecyclerView target;
                    if (view.getId() == R.id.tagContainer) {
                        target = (RecyclerView) view.getParent();
                        positionTarget = (int) view.getTag();
                    } else {
                        target = (RecyclerView) view;
                    }

                    TagAdapter adapterTarget = (TagAdapter) target.getAdapter();
                    RecyclerView source = (RecyclerView) viewSource.getParent();
                    TagAdapter adapterSource = (TagAdapter) source.getAdapter();

                    if (source != target || !adapterTarget.suppressInsideDragAndDrop) {
                        positionSource = (int) viewSource.getTag();
                        Tag tag = adapterSource.getTags().get(positionSource);

                        List<Tag> tagsSource = adapterSource.getTags();
                        tagsSource.remove(positionSource);

                        adapterSource.updateTags(tagsSource);
                        adapterSource.notifyItemRemoved(positionSource);
                        adapterSource.notifyItemRangeChanged(positionSource, tagsSource.size() - positionSource);

                        if (!adapterTarget.suppressTagAdd) {
                            List<Tag> tagsTarget = adapterTarget.getTags();
                            if (positionTarget >= 0) {
                                tagsTarget.add(positionTarget, tag);
                            } else {
                                tagsTarget.add(tag);
                                positionTarget = tagsTarget.size() - 1;
                            }
                            adapterTarget.updateTags(tagsTarget);
                            adapterTarget.notifyItemInserted(positionTarget);
                            adapterTarget.notifyItemRangeChanged(positionTarget, tagsTarget.size() - positionTarget);
                            view.setVisibility(View.VISIBLE);
                        }

                        adapter.delegate.didEndDrop(adapter.owner, source, tag, positionTarget, false);
                    }

                    sourceView = (View) event.getLocalState();
                    sourceView.setVisibility(View.VISIBLE);
                    break;

                case DragEvent.ACTION_DRAG_ENDED:
                    sourceView = (View) event.getLocalState();
                    sourceView.setVisibility(View.VISIBLE);
                    break;

                default:
                    break;
            }

            return true;
        }
    }
}