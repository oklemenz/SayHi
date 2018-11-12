package de.oklemenz.sayhi.view;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.oklemenz.sayhi.R;

/**
 * Created by Oliver Klemenz on 30.03.17.
 */

public class QRHelpPagerAdapter extends PagerAdapter {

    private Context context;
    private LayoutInflater inflater;

    public QRHelpPagerAdapter(Activity context) {
        this.context = context;
        this.inflater = context.getLayoutInflater();
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((View) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = null;
        switch (position) {
            case 0:
                view = inflater.inflate(R.layout.qr_code_help_first, container, false);
                break;
            case 1:
                view = inflater.inflate(R.layout.qr_code_help_second, container, false);
                break;
            case 2:
                view = inflater.inflate(R.layout.qr_code_help_third, container, false);
                break;
        }

        view.setBackground(null);
        ((ViewPager) container).addView(view);

        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((ViewPager) container).removeView((View) object);
    }
}
