package de.oklemenz.sayhi.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.model.Category;
import de.oklemenz.sayhi.service.IconService;
import de.oklemenz.sayhi.service.Utilities;

import static de.oklemenz.sayhi.model.Category.CategoryStagedIcon;

/**
 * Created by Oliver Klemenz on 31.10.16.
 */

public class CategoryCell extends RecyclerView.ViewHolder {

    public static int CategoryMargin = 4 * 2;
    public static int CategoryMaxWidthInset = 20;

    private int CategoryMaxWidth = 0;
    private int CategoryMaxHalfWidth = 0;

    public View container;

    @Bind(R.id.categoryLayout)
    public LinearLayout layout;

    @Bind(R.id.categoryIcon)
    public ImageView icon;

    @Bind(R.id.categoryText)
    public TextView text;

    public CategoryCell(View itemView) {
        super(itemView);
        container = itemView;
        ButterKnife.bind(this, itemView);
    }

    public void setData(Category category) {
        GradientDrawable drawable = (GradientDrawable) layout.getBackground();
        if (category.selected) {
            drawable.setStroke(Utilities.convertDpToPx(AppDelegate.getInstance().Context, 1), Color.WHITE);
        } else {
            drawable.setStroke(0, Color.BLACK);
        }
        drawable.setColor(category.bgColor);
        Bitmap iconImage = category.getIconImage();
        if (iconImage == null) {
            iconImage = IconService.getInstance().icon(CategoryStagedIcon);
        }
        icon.setImageBitmap(iconImage);
        icon.setColorFilter(category.textColor);
        text.setTextColor(category.textColor);
        text.setText(category.getName());

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (TextUtils.isEmpty(category.getName())) {
            layoutParams.setMargins(0, 0, 0, 0);
        } else {
            int margin = Utilities.convertDpToPx(AppDelegate.getInstance().Context, 2);
            layoutParams.setMargins(margin, 0, margin, 0);
        }
        text.setLayoutParams(layoutParams);

        setMaxWidthScreen();
    }

    public void calcWidths() {
        if (CategoryMaxWidth == 0) {
            Point size = new Point();
            ((WindowManager) AppDelegate.getInstance().Context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(size);
            CategoryMaxWidth = size.x - CategoryMargin;
            CategoryMaxHalfWidth = size.x / 2 - CategoryMargin;
        }
    }

    public void setMaxWidthScreen() {
        calcWidths();
        text.setMaxWidth(CategoryMaxWidth - icon.getWidth());
    }

    public void setMaxWidthHalfScreen() {
        calcWidths();
        text.setMaxWidth(CategoryMaxHalfWidth - icon.getWidth());
    }

    public void setMaxWidthHalfScreenWithBias(int bias) {
        calcWidths();
        text.setMaxWidth(CategoryMaxHalfWidth - bias - icon.getWidth());
    }

    public void setMaxWidthScreenWithBias(int bias) {
        calcWidths();
        text.setMaxWidth(CategoryMaxHalfWidth - bias - icon.getWidth());
    }
}