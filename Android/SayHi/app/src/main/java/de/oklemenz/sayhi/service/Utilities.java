package de.oklemenz.sayhi.service;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.Settings;

import static android.content.Context.FINGERPRINT_SERVICE;
import static android.content.Context.KEYGUARD_SERVICE;

/**
 * Created by Oliver Klemenz on 14.02.17.
 */

public class Utilities {

    public static boolean vibrateEnabled(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        return vibrator != null && vibrator.hasVibrator();
    }

    public static boolean fingerprintEnabled(Context context) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return false;
            } else {
                KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(KEYGUARD_SERVICE);
                FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(FINGERPRINT_SERVICE);
                if (!fingerprintManager.isHardwareDetected()) {
                    return false;
                }
                if (!fingerprintManager.hasEnrolledFingerprints()) {
                    return false;
                }
                if (!keyguardManager.isKeyguardSecure()) {
                    return false;
                }
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean authEnabled(Context context) {
        return fingerprintEnabled(context);
    }

    public static String getEmojiByUnicode(int unicode) {
        return new String(Character.toChars(unicode));
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html) {
        return fromHtml(html, null);
    }

    public static Spanned fromHtml(String html, Html.TagHandler tagHandler) {
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY, new Emoji.ImageGetter(), tagHandler);
        } else {
            result = Html.fromHtml(html, new Emoji.ImageGetter(), tagHandler);
        }
        return result;
    }

    public static Spanned toSpan(String html) {
        html = html.replace("\n", "<br/>");
        return new SpannableString(Utilities.fromHtml(html));
    }

    public static Spanned toSpan(String html, Html.TagHandler tagHandler) {
        html = html.replace("\n", "<br/>");
        return new SpannableString(Utilities.fromHtml(html, tagHandler));
    }

    @SuppressWarnings("deprecation")
    public static Drawable getDrawable(Context context, int id, Resources.Theme theme) {
        Drawable result;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            result = context.getResources().getDrawable(id, theme);
        } else {
            result = context.getResources().getDrawable(id);
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    public static ColorStateList getColorStateList(Context context, int id, Resources.Theme theme) {
        ColorStateList result;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            result = context.getResources().getColorStateList(id, theme);
        } else {
            result = context.getResources().getColorStateList(id);
        }
        return result;
    }

    public static String readInputStream(InputStream inputStream) {
        try {
            StringBuilder inputStringBuilder = new StringBuilder();
            BufferedReader bufferedReader;
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line = bufferedReader.readLine();
            while (line != null) {
                inputStringBuilder.append(line);
                inputStringBuilder.append("\n");
                line = bufferedReader.readLine();
            }
            return inputStringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getHexColor(Context context, int color) {
        return "#" + Integer.toHexString(ContextCompat.getColor(context, color) & 0x00ffffff);
    }

    public static String getAlphaHexColor(Context context, int color) {
        return "#" + Integer.toHexString(ContextCompat.getColor(context, color));
    }

    public static boolean isColorLight(int color) {
        double brightness = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return brightness >= 0.75;
    }

    public static int lighterColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = (int) Math.min(Color.red(color) + (Color.red(color) * factor), 255);
        int g = (int) Math.min(Color.green(color) + (Color.green(color) * factor), 255);
        int b = (int) Math.min(Color.blue(color) + (Color.blue(color) * factor), 255);
        return Color.argb(a, r, g, b);
    }

    public static int darkerColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = (int) Math.max(Color.red(color) - (Color.red(color) * factor), 0);
        int g = (int) Math.max(Color.green(color) - (Color.green(color) * factor), 0);
        int b = (int) Math.max(Color.blue(color) - (Color.blue(color) * factor), 0);
        return Color.argb(a, r, g, b);
    }

    public static String searchNormalized(String text, String langCode) {
        String searchNormalized = text.toLowerCase();
        searchNormalized = Normalizer.normalize(searchNormalized, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        searchNormalized = searchNormalized.replaceAll("\\s+", "");
        searchNormalized = searchNormalized.replaceAll("(.)\1+", "$1");
        searchNormalized = searchNormalized.trim();
        if (!TextUtils.isEmpty(searchNormalized)) {
            return searchNormalized;
        }
        return text.trim().toLowerCase();
    }

    public static String getISO8601StringForDate(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public static Date parseISO8601String(String isoDate) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return dateFormat.parse(isoDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date dayDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static int convertDpToPx(Context context, float dp) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm);
    }

    public static float dpToPx(float dp) {
        return dp * AppDelegate.getInstance().Context.getResources().getDisplayMetrics().density;
    }

    public static float pxToDp(float px) {
        return px / AppDelegate.getInstance().Context.getResources().getDisplayMetrics().density;
    }

    public static float spToPx(float sp) {
        return sp * AppDelegate.getInstance().Context.getResources().getDisplayMetrics().scaledDensity;
    }

    public static float pxToSp(float px) {
        return px / AppDelegate.getInstance().Context.getResources().getDisplayMetrics().scaledDensity;
    }

    public static int getLocalizedRawResource(Context context, String resourceName, int defaultResourceId) {
        if (AppDelegate.BundleLangCodes.contains(Locale.getDefault().getLanguage())) {
            return context.getResources().getIdentifier(resourceName + "_" + Locale.getDefault().getLanguage(), "raw", context.getPackageName());
        }
        return defaultResourceId;
    }

    public static int getStringResourceId(Context context, String name) {
        return context.getResources().getIdentifier(name, "string", context.getPackageName());
    }

    public static int getDrawableResourceId(Context context, String name) {
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
    }

    public static String readHML(Context context, int resourceId) {
        InputStream inputStream = context.getResources().openRawResource(resourceId);
        String html = Utilities.readInputStream(inputStream);
        html = html.replace("#000000", Settings.getInstance().getAccentColor());
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
        return header + "\n" + html;
    }

    public static String condense(String text) {
        String[] parts = text.trim().split("\\s+");
        List<String> nonEmptyParts = new ArrayList<>();
        for (String part : parts) {
            if (!TextUtils.isEmpty(part)) {
                nonEmptyParts.add(part);
            }
        }
        return TextUtils.join(" ", nonEmptyParts);
    }

    public static String uppercaseFirst(String text) {
        if (!TextUtils.isEmpty(text)) {
            return Character.toUpperCase(text.charAt(0)) + text.substring(1);
        }
        return text;
    }

    public static String lowercaseFirst(String text) {
        if (!TextUtils.isEmpty(text)) {
            return Character.toLowerCase(text.charAt(0)) + text.substring(1);
        }
        return text;
    }

    public static String capitalize(String text) {
        String[] parts = condense(text).split("\\s+");
        List<String> capitalizedParts = new ArrayList<>();
        for (String part : parts) {
            capitalizedParts.add(uppercaseFirst(part));
        }
        return TextUtils.join(" ", capitalizedParts);
    }

    public static String prefix(String text, int length) {
        return text.substring(0, Math.min(text.length(), length));
    }

    public static String clean(String text) {
        return condense(text.replaceAll("[^a-zA-Z0-9]", "_"));
    }

    public static Map<String, Object> jsonToMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap<>();
        Iterator keys = object.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            map.put(key, jsonFromData(object.get(key)));
        }
        return map;
    }

    public static List<Object> jsonToList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(jsonFromData(array.get(i)));
        }
        return list;
    }

    private static Object jsonFromData(Object json) throws JSONException {
        if (json == JSONObject.NULL) {
            return null;
        } else if (json instanceof JSONObject) {
            return jsonToMap((JSONObject) json);
        } else if (json instanceof JSONArray) {
            return jsonToList((JSONArray) json);
        } else {
            return json;
        }
    }

    public static String toBase64(byte[] bytes) {
        try {
            return new String(Base64.encode(bytes, Base64.NO_WRAP), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static byte[] fromBase64(String base64) {
        return Base64.decode(base64, Base64.NO_WRAP);
    }

    public static byte[] readFile(Context context, String filename) {
        ByteArrayOutputStream bos = null;
        try {
            InputStream is = new FileInputStream(new File(filename));
            bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int bytesRead = 0;
            while ((bytesRead = is.read(b)) != -1) {
                bos.write(b, 0, bytesRead);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    public static boolean deleteFile(Context context, String filename) {
        File file = new File(filename);
        return file.delete();
    }

    public static Map<String, String> queryParameters(Uri uri) {
        if (uri == null) {
            return null;
        }
        Map<String, String> queryParams = new HashMap<>();
        for (String name : uri.getQueryParameterNames()) {
            queryParams.put(name, uri.getQueryParameter(name));
        }
        return queryParams;
    }

    public static void openAppSettings(BaseActivity context) {
        final Intent intent = new Intent();
        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(intent);
    }
}