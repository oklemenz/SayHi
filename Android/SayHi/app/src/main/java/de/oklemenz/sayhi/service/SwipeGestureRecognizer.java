package de.oklemenz.sayhi.service;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ListView;

/**
 * Created by Oliver Klemenz on 30.08.17.
 */

public class SwipeGestureRecognizer extends GestureDetector.SimpleOnGestureListener {

    public interface Callback {
        void swipeLeft(int position);

        void swipeRight(int position);
    }

    private ListView listView;
    private Callback callback;

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    public SwipeGestureRecognizer(ListView listView, Callback callback) {
        this.listView = listView;
        this.callback = callback;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        int pos = this.listView.pointToPosition((int) e1.getX(), (int) e1.getY());

        try {
            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;

            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                if (callback != null) {
                    callback.swipeLeft(pos);
                }
            } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                if (callback != null) {
                    callback.swipeRight(pos);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
