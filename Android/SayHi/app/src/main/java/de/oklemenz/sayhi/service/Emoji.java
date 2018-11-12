package de.oklemenz.sayhi.service;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Html;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.model.Settings;

/**
 * Created by Oliver Klemenz on 27.03.17.
 */

public class Emoji {

    public static class ImageGetter implements Html.ImageGetter {

        private static final int SizeS = 10;
        private static final int SizeM = 15;
        private static final int SizeL = 20;
        private static final int SizeXL = 25;

        public Drawable getDrawable(String source) {
            int id = 0;
            int size = SizeS;

            switch (source) {
                case "square_S.png":
                    id = R.drawable.square;
                    size = SizeS;
                    break;
                case "square_M.png":
                    id = R.drawable.square;
                    size = SizeM;
                    break;
                case "square_L.png":
                    id = R.drawable.square;
                    size = SizeL;
                    break;
                case "square_XL.png":
                    id = R.drawable.square;
                    size = SizeXL;
                    break;
                case "link_S.png":
                    id = R.drawable.link;
                    size = SizeS;
                    break;
                case "link_M.png":
                    id = R.drawable.link;
                    size = SizeM;
                    break;
                case "link_L.png":
                    id = R.drawable.link;
                    size = SizeL;
                    break;
                case "link_XL.png":
                    id = R.drawable.link;
                    size = SizeXL;
                    break;
                case "thumb_up_S.png":
                    id = R.drawable.thumb_up;
                    size = SizeS;
                    break;
                case "thumb_up_M.png":
                    id = R.drawable.thumb_up;
                    size = SizeM;
                    break;
                case "thumb_up_L.png":
                    id = R.drawable.thumb_up;
                    size = SizeL;
                    break;
                case "thumb_up_XL.png":
                    id = R.drawable.thumb_up;
                    size = SizeXL;
                    break;
                case "thumb_down_S.png":
                    id = R.drawable.thumb_down;
                    size = SizeS;
                    break;
                case "thumb_down_M.png":
                    id = R.drawable.thumb_down;
                    size = SizeM;
                    break;
                case "thumb_down_L.png":
                    id = R.drawable.thumb_down;
                    size = SizeL;
                    break;
                case "thumb_down_XL.png":
                    id = R.drawable.thumb_down;
                    size = SizeXL;
                    break;
                case "back_S.png":
                    id = R.drawable.back;
                    size = SizeS;
                    break;
                case "back_M.png":
                    id = R.drawable.back;
                    size = SizeM;
                    break;
                case "back_L.png":
                    id = R.drawable.back;
                    size = SizeL;
                    break;
                case "back_XL.png":
                    id = R.drawable.back;
                    size = SizeXL;
                    break;
            }
            Drawable drawable = Utilities.getDrawable(AppDelegate.getInstance(), id, null);
            if (drawable != null) {
                int sizeDP = Utilities.convertDpToPx(AppDelegate.getInstance().Context, size);
                drawable.setBounds(0, 0, sizeDP, sizeDP);
            }
            return drawable;
        }
    }

    public enum Size {
        None(""),
        Small("S"),
        Medium("M"),
        Large("L"),
        ExtraLarge("XL");

        public String code;

        Size(String code) {
            this.code = code;
        }
    }

    public static String getMatchingMode(Size size) {
        if (size != Size.None) {
            return "<img src='square_" + size.code + ".png'/>";
        }
        return "";
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return "\ud83d\udd33";
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return "#";
        } else {
            return "";
        }*/
    }

    public static String getRelationType(Size size) {
        if (size != Size.None) {
            return "<img src='link_" + size.code + ".png'/>";
        }
        return "";
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return "\ud83d\udd17";
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return "\ud83d\udd17";
        } else {
            return "";
        }*/
    }

    public static String getLike(Size size) {
        if (Settings.getInstance().getLeftLabel() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return Settings.getInstance().getLeftLabel();
            }
            if (Settings.getInstance().getLeftLabelFallback() != null) {
                return Settings.getInstance().getLeftLabelFallback();
            }
            return Settings.getInstance().getLeftLabel();
        }
        if (size != Size.None) {
            return "<img src='thumb_up_" + size.code + ".png'/>";
        }
        return "";
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return "\ud83d\udc4d\ud83c\udffc";
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return "▲";
        } else {
            return "▲";
        }*/
    }

    public static String getDislike(Size size) {
        if (Settings.getInstance().getRightLabel() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return Settings.getInstance().getRightLabel();
            }
            if (Settings.getInstance().getRightLabelFallback() != null) {
                return Settings.getInstance().getRightLabelFallback();
            }
            return Settings.getInstance().getRightLabel();
        }
        if (size != Size.None) {
            return "<img src='thumb_down_" + size.code + ".png'/>";
        }
        return "";
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return "\ud83d\udc4e\ud83c\udffc";
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return "▼";
        } else {
            return "▼";
        }*/
    }

    public static String getBack(Size size) {
        if (size != Size.None) {
            return "<img src='back_" + size.code + ".png'/>";
        }
        return "";
    }
}