package de.oklemenz.sayhi.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.model.Tag;
import de.oklemenz.sayhi.service.IconService;
import de.oklemenz.sayhi.service.Utilities;

import static de.oklemenz.sayhi.model.Category.CategoryStagedIcon;
import static de.oklemenz.sayhi.model.Tag.TagStagedIcon;

/**
 * Created by Oliver Klemenz on 31.10.16.
 */

public class TagCell extends RecyclerView.ViewHolder {

    public final static int TagMargin = 4 * 2;
    public final static int TagMaxWidthInset = 20;

    private int TagMaxWidth = 0;
    private int TagMaxHalfWidth = 0;

    public View container;

    @Bind(R.id.tagLayout)
    public LinearLayout layout;

    @Bind(R.id.tagIcon)
    public ImageView icon;

    @Bind(R.id.tagText)
    public TextView text;

    public TagCell(View itemView) {
        super(itemView);
        container = itemView;
        ButterKnife.bind(this, itemView);
    }

    public void setData(Tag tag) {
        text.setText(tag.getName());
        GradientDrawable drawable = (GradientDrawable) layout.getBackground();
        if (tag.selected) {
            drawable.setStroke(Utilities.convertDpToPx(AppDelegate.getInstance().Context, 1), Color.WHITE);
        } else {
            drawable.setStroke(0, Color.BLACK);
        }
        if (tag.getCategory() != null) {
            drawable = (GradientDrawable) layout.getBackground();
            DisplayMetrics metrics = AppDelegate.getInstance().Context.getResources().getDisplayMetrics();
            drawable.setCornerRadius(metrics.density * 25.0f / 2);
            drawable.setColor(tag.getCategory().bgColor);
            Bitmap iconImage = tag.getCategory().getIconImage();
            if (iconImage == null) {
                iconImage = IconService.getInstance().icon(TagStagedIcon);
            }
            icon.setImageBitmap(iconImage);
            icon.setColorFilter(tag.getCategory().textColor);
            text.setTextColor(tag.getCategory().textColor);
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (TextUtils.isEmpty(tag.getName())) {
            layoutParams.setMargins(0, 0, 0, 0);
        } else {
            int margin = Utilities.convertDpToPx(AppDelegate.getInstance().Context, 2);
            layoutParams.setMargins(margin, 0, margin, 0);
        }
        text.setLayoutParams(layoutParams);

        setMaxWidthScreen();
    }

    public void calcWidths() {
        if (TagMaxWidth == 0) {
            Point size = new Point();
            ((WindowManager) AppDelegate.getInstance().Context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(size);
            TagMaxWidth = size.x - TagMargin;
            TagMaxHalfWidth = size.x / 2 - TagMargin;
        }
    }

    public void setMaxWidthScreen() {
        calcWidths();
        text.setMaxWidth(TagMaxWidth - icon.getWidth());
    }

    public void setMaxWidthHalfScreen() {
        calcWidths();
        text.setMaxWidth(TagMaxHalfWidth - icon.getWidth());
    }

    public void setMaxWidthHalfScreenWithBias(int bias) {
        calcWidths();
        text.setMaxWidth(TagMaxHalfWidth - bias - icon.getWidth());
    }

    public void setMaxWidthScreenWithBias(int bias) {
        calcWidths();
        text.setMaxWidth(TagMaxHalfWidth - bias - icon.getWidth());
    }
}