package de.oklemenz.sayhi.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.service.Utilities;

/**
 * Created by Oliver Klemenz on 25.04.17.
 */

public class QROverlayView extends View {

    private PointF[] points;
    private Paint paint;

    public QROverlayView(Context context) {
        super(context);
        init();
    }

    public QROverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public QROverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(AppDelegate.AccentColor);
        paint.setStrokeWidth(Utilities.convertDpToPx(AppDelegate.getInstance().Context, 2.0f));
        paint.setStyle(Paint.Style.STROKE);
    }

    public void setPoints(PointF[] points) {
        this.points = points;
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (points != null && points.length >= 2) {
            boolean set = false;
            float minX = 0;
            float minY = 0;
            float maxX = 0;
            float maxY = 0;
            for (PointF point : points) {
                if (!set) {
                    minX = point.x;
                    minY = point.y;
                    maxX = point.x;
                    maxY = point.y;
                } else {
                    minX = Math.min(minX, point.x);
                    minY = Math.min(minY, point.y);
                    maxX = Math.max(maxX, point.x);
                    maxY = Math.max(maxY, point.y);
                }
                set = true;
            }
            canvas.drawRect(minX, minY, maxX, maxY, paint);
        }
    }
}