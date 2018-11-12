package de.oklemenz.sayhi.base;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.shawnlin.numberpicker.NumberPicker;

import butterknife.ButterKnife;
import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.model.Settings;
import de.oklemenz.sayhi.service.Emoji;
import de.oklemenz.sayhi.service.NotificationCenter;
import de.oklemenz.sayhi.service.Utilities;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;
import static de.oklemenz.sayhi.AppDelegate.AccentColor;
import static de.oklemenz.sayhi.AppDelegate.ColorsSetNotification;
import static de.oklemenz.sayhi.AppDelegate.SettingsFetchedNotification;

/**
 * Created by Oliver Klemenz on 15.02.17.
 */

public class BaseActivity extends FragmentActivity implements NotificationCenter.Observer, ActivityCompat.OnRequestPermissionsResultCallback {

    public enum TransitionState {
        SHOW,
        MODAL
    }

    public static final int PERMISSION_ACCESS_LOCATION = 1;
    public static final int PERMISSION_RECORD_AUDIO = 2;
    public static final int PERMISSION_ACCESS_CAMERA = 3;

    private PermissionDelegate permissionDelegate;
    protected boolean resumed = false;

    public interface PermissionDelegate {
        void permissionResult(String permission, int requestCode, boolean granted);
    }

    interface TraverseViewDelegate {
        void didTraverseView(View view);
    }

    protected View rootView;
    private Dialog helpDialog;
    private Dialog coverDialog;
    private TransitionState transitionState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppDelegate.getInstance().setContext(this);
    }

    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        ButterKnife.bind(this);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        NotificationCenter.getInstance().addObserver(ColorsSetNotification, this);
        NotificationCenter.getInstance().addObserver(SettingsFetchedNotification, this);

        rootView = this.findViewById(R.id.root);
        if (rootView != null) {
            rootView.setBackground(null);
        }

        updateBackButton();
        updateHelpButton();
        updateColors();
        hideKeyboardOnScroll();

        getWindow().setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_HIDDEN | SOFT_INPUT_ADJUST_NOTHING);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        Intent intent = getIntent();
        transitionState = (TransitionState) intent.getSerializableExtra("transitionState");
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideKeyboard();
        AppDelegate.getInstance().setContext(this);
        resumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideKeyboard();
        resumed = false;
    }

    public void protect() {
        //showCover();
    }

    public void unprotect() {
        //hideCover();
    }

    public String backLabel() {
        Intent intent = getIntent();
        String backLabel = intent.getStringExtra("backLabel");
        if (backLabel == null) {
            backLabel = this.getString(R.string.Back);
        }
        return backLabel;
    }

    public void updateBackButton() {
        Button backButton = (Button) rootView.findViewById(R.id.backButton);
        if (backButton != null) {
            if (backLabel() != null) {
                backButton.setText(Utilities.fromHtml(Emoji.getBack(Emoji.Size.Large) + "" + backLabel()));
            }
        }
    }

    public void updateHelpButton() {
        if (Settings.getInstance().getDisableHelp() != null && Settings.getInstance().getDisableHelp()) {
            View helpButton = rootView.findViewById(R.id.helpButton);
            if (helpButton != null) {
                helpButton.getLayoutParams().width = 0;
                helpButton.setVisibility(View.INVISIBLE);
            }
        } else {
            View helpButton = rootView.findViewById(R.id.helpButton);
            if (helpButton != null) {
                if (helpButton.getVisibility() == View.INVISIBLE) {
                    helpButton.getLayoutParams().width = Utilities.convertDpToPx(this, 34);
                    helpButton.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void updateMaintenanceMode() {
        if (Settings.getInstance().getMaintenance()) {
            Dialog dialog = showCover();
            TextView maintenanceMode = (TextView) dialog.findViewById(R.id.maintenanceMode);
            maintenanceMode.setVisibility(View.VISIBLE);
        } else {
            hideCover();
        }
    }

    public Dialog showCover() {
        if (coverDialog == null) {
            hideKeyboard();
            scrollToTop(true);
            final Dialog dialog = new Dialog(this, R.style.CoverDialog);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            View maintenaceView = this.getLayoutInflater().inflate(R.layout.cover_view, null);
            dialog.setContentView(maintenaceView);
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnKeyListener(new Dialog.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    return true;
                }
            });
            TextView maintenanceMode = (TextView) dialog.findViewById(R.id.maintenanceMode);
            maintenanceMode.setVisibility(View.GONE);
            dialog.show();
            coverDialog = dialog;
        }
        return coverDialog;
    }

    public void hideCover() {
        if (coverDialog != null) {
            coverDialog.dismiss();
            coverDialog = null;
        }
    }

    public void updateColors() {
        if (AppDelegate.AccentColor != 0) {
            applyColors(this, rootView);
            int[] colors = new int[]{
                    AppDelegate.GradientColor1,
                    AppDelegate.GradientColor2};
            GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
            getWindow().setBackgroundDrawable(gradientDrawable);
        } else {
            getWindow().setBackgroundDrawableResource(R.drawable.gradient);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationCenter.getInstance().removeObserver(this);
    }

    @Override
    public void notify(String name, NotificationCenter.Notification notification) {
        if (name.equals(ColorsSetNotification)) {
            updateColors();
        } else if (name.equals(SettingsFetchedNotification)) {
            updateHelpButton();
            updateMaintenanceMode();
        }
    }

    public void onBackPressed(View view) {
        hideKeyboard();
        finish();
    }

    public void onDonePressed(View view) {
        hideKeyboard();
        finish();
    }

    public void onClosePressed(View view) {
        hideKeyboard();
        finish();
    }

    public void onHelpPressed(View view) {
    }

    public void onEditPressed(View view) {
    }

    public static void traverseView(View view, TraverseViewDelegate delegate) {
        if (view == null) {
            return;
        }
        if (delegate != null) {
            delegate.didTraverseView(view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View subView = viewGroup.getChildAt(i);
                traverseView(subView, delegate);
            }
        }
    }

    public static void applyColors(final Context context, View view) {
        traverseView(view, new TraverseViewDelegate() {
            @Override
            public void didTraverseView(View view) {
                applyViewColors(context, view);
            }
        });
    }

    public static void applyViewColors(Context context, View view) {
        if (view instanceof ToggleButton) {
            ToggleButton toggleButton = (ToggleButton) view;
            toggleButton.setTextColor(AppDelegate.toggleTextColor);
            StateListDrawable background = (StateListDrawable) toggleButton.getBackground();
            if (background != null) {
                DrawableContainer.DrawableContainerState containerState = (DrawableContainer.DrawableContainerState) background.getConstantState();
                Drawable[] drawableItems = containerState.getChildren();
                GradientDrawable checkedDrawable = (GradientDrawable) drawableItems[0];
                checkedDrawable.setColor(AppDelegate.AccentColor);
                checkedDrawable.setStroke(Utilities.convertDpToPx(context, 1), AppDelegate.AccentColor);
                GradientDrawable uncheckedDrawable = (GradientDrawable) drawableItems[1];
                uncheckedDrawable.setStroke(Utilities.convertDpToPx(context, 1), AppDelegate.AccentColor);
            }
        } else if (view instanceof Switch) {
            Switch switchView = (Switch) view;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                DrawableCompat.setTintList(DrawableCompat.wrap(switchView.getThumbDrawable()), AppDelegate.switchThumbColor);
                DrawableCompat.setTintList(DrawableCompat.wrap(switchView.getTrackDrawable()), AppDelegate.switchTrackColor);
            }
        } else if (view instanceof ImageButton) {
            ImageButton imageButton = ((ImageButton) view);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    view.getTag() instanceof String && view.getTag().equals("applyColors")) {
                imageButton.setColorFilter(AppDelegate.AccentColor);
            }
        } else if (view instanceof Button) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP &&
                    view.getTag() instanceof String && view.getTag().equals("ignoreApplyColors")) {
                return;
            }
            Button button = ((Button) view);
            if (button.getCurrentTextColor() == ContextCompat.getColor(context, R.color.colorAccent)) {
                button.setTextColor(AppDelegate.AccentColor);
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                int[] attrs = new int[]{R.attr.selectableItemBackground};
                TypedArray typedArray = context.obtainStyledAttributes(attrs);
                int backgroundResource = typedArray.getResourceId(0, 0);
                button.setBackgroundResource(backgroundResource);
                typedArray.recycle();
                int padding = Utilities.convertDpToPx(AppDelegate.getInstance().Context, 10);
                button.setPadding(padding, padding, padding, padding);
            }
        } else if (view instanceof EditText) {
            EditText textEdit = (EditText) view;
            if (textEdit.getBackground() != null) {
                textEdit.getBackground().setColorFilter(AccentColor, PorterDuff.Mode.SRC_ATOP);
            }
        } else if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (textView.getCurrentTextColor() == ContextCompat.getColor(context, R.color.colorAccent)) {
                textView.setTextColor(AppDelegate.AccentColor);
            }
        } else if (view instanceof NumberPicker) {
            NumberPicker numberPicker = (NumberPicker) view;
            if (numberPicker.getTextColor() != Color.WHITE) {
                numberPicker.setTextColor(AppDelegate.AccentColor);
            }
        }
    }

    public final String getTermString(String name, Object... args) {
        if (Settings.getInstance().getTerminology() != null) {
            name += "_" + Settings.getInstance().getTerminology();
        } else if (Settings.getInstance().getLeftLabel() != null || Settings.getInstance().getRightLabel() != null) {
            return "";
        }
        int id = Utilities.getStringResourceId(this, name);
        if (id == 0) {
            return "";
        }
        return String.format(this.getString(id), args);
    }

    public void hideHelp() {
        if (helpDialog != null) {
            helpDialog.dismiss();
        }
        helpDialog = null;
    }

    protected Dialog showHelp(View view, View helpView) {
        hideKeyboard();
        scrollToTop(true);
        final Dialog dialog = new Dialog(this, R.style.HelpDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ColorDrawable backgroundColor = new ColorDrawable(Color.BLACK);
        backgroundColor.setAlpha((int) (0.7 * 255.0));
        dialog.getWindow().setBackgroundDrawable(backgroundColor);
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        helpView.setFitsSystemWindows(true);
        dialog.setContentView(helpView);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        RelativeLayout helpViewRoot = (RelativeLayout) dialog.findViewById(R.id.help);
        helpViewRoot.setBackground(null);
        helpViewRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                helpDialog = null;
            }
        });
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        helpDialog = dialog;
        return dialog;
    }

    protected void showHelp(View view, int helpView) {
        showHelp(view, this.getLayoutInflater().inflate(helpView, null));
    }

    protected void scrollToTop(boolean animated) {
        ScrollView scrollView = (ScrollView) this.findViewById(R.id.scrollView);
        if (scrollView != null) {
            if (animated) {
                scrollView.smoothScrollTo(0, 0);
            } else {
                scrollView.scrollTo(0, 0);
            }
        }
    }

    protected void hideKeyboardOnScroll() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            final ScrollView scrollView = (ScrollView) this.findViewById(R.id.scrollView);
            if (scrollView != null) {
                scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                    @Override
                    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                        if (scrollView instanceof de.oklemenz.sayhi.view.ScrollView) {
                            if (!((de.oklemenz.sayhi.view.ScrollView) scrollView).isOverScrollByTouchEvent) {
                                return;
                            }
                        }
                        hideKeyboard();
                    }
                });
            }
        }
    }

    public void hideKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public void showKeyboard(View view) {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    public void askForPermission(String permission, int requestCode, PermissionDelegate delegate) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            permissionDelegate = null;
            if (delegate != null) {
                delegate.permissionResult(permission, requestCode, true);
            }
        } else {
            permissionDelegate = delegate;
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            int grantResult = grantResults[i];
            if (permissionDelegate != null) {
                permissionDelegate.permissionResult(permission, requestCode, grantResult == PackageManager.PERMISSION_GRANTED);
                permissionDelegate = null;
                return;
            }
        }
    }

    @Override
    public boolean onCreateThumbnail(Bitmap outBitmap, Canvas canvas) {
        return false;
        /*
        canvas.drawColor(Color.LTGRAY);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(70);
        canvas.drawText(this.getString(R.string.Hi), 50, 50, paint);
        return true;
        */
    }

    @Override
    public void startActivity(@RequiresPermission Intent intent) {
        startActivity(intent, (TransitionState)null);
    }

    public void startActivity(@RequiresPermission Intent intent, TransitionState transitionState) {
        if (transitionState != null && intent.getSerializableExtra("transitionState") == null) {
            intent.putExtra("transitionState", transitionState);
        }
        super.startActivity(intent);
        if (transitionState == TransitionState.SHOW) {
            overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        } else if (transitionState == TransitionState.MODAL) {
            overridePendingTransition(R.anim.slide_show, R.anim.slide_hide);
        }
    }

    @Override
    public void startActivityForResult(@RequiresPermission Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode, (TransitionState)null);
    }

    public void startActivityForResult(@RequiresPermission Intent intent, int requestCode, TransitionState transitionState) {
        if (transitionState != null && intent.getSerializableExtra("transitionState") == null) {
            intent.putExtra("transitionState", transitionState);
        }
        super.startActivityForResult(intent, requestCode);
        if (transitionState == TransitionState.SHOW) {
            overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        } else if (transitionState == TransitionState.MODAL) {
            overridePendingTransition(R.anim.slide_show, R.anim.slide_hide);
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (transitionState == TransitionState.SHOW) {
            overridePendingTransition(R.anim.slide_out_back, R.anim.slide_in_back);
        } else if (transitionState == TransitionState.MODAL) {
            overridePendingTransition(R.anim.slide_hide_back, R.anim.slide_show_back);
        }
    }
}