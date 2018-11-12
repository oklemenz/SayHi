package de.oklemenz.sayhi.view;

import android.support.v4.view.ViewPager;
import android.view.MotionEvent;
import android.view.View;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import me.relex.circleindicator.CircleIndicator;

/**
 * Created by Oliver Klemenz on 30.03.17.
 */

public class QRCodeHelp {

    @Bind(R.id.helpContainerPageView)
    public ViewPager helpContainerPageView;

    @Bind(R.id.helpContainerPageIndicatorView)
    public CircleIndicator helpContainerPageIndicatorView;

    public BaseActivity context;
    public View view;

    public QRCodeHelp(View view, BaseActivity context) {
        this.view = view;
        this.context = context;
        ButterKnife.bind(this, view);

        showContent();
    }

    private void showContent() {
        helpContainerPageView.setAdapter(new QRHelpPagerAdapter(context));
        helpContainerPageIndicatorView.setViewPager(helpContainerPageView);
        helpContainerPageIndicatorView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (event.getX() > helpContainerPageIndicatorView.getX() + helpContainerPageIndicatorView.getWidth() / 2.0) {
                        helpNext();
                    } else {
                        helpPrevious();
                    }
                }
                return true;
            }
        });
    }

    private void helpPrevious() {
        if (helpContainerPageView.getCurrentItem() > 0) {
            helpContainerPageView.setCurrentItem(helpContainerPageView.getCurrentItem() - 1, true);
        }
    }

    private void helpNext() {
        if (helpContainerPageView.getCurrentItem() < helpContainerPageView.getChildCount()) {
            helpContainerPageView.setCurrentItem(helpContainerPageView.getCurrentItem() + 1, true);
        }
    }
}
