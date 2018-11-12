package de.oklemenz.sayhi.service;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.view.QROverlayView;

import static de.oklemenz.sayhi.AppDelegate.QRCodeRecognizedNotification;

/**
 * Created by Oliver Klemenz on 03.11.16.
 */

public class QRCode implements QRCodeReaderView.OnQRCodeReadListener {

    public interface Callback {
        void completed(boolean started);
    }

    public interface Delegate {
        void generationDone(Bitmap bitmap);
    }

    public static int QRColor = Color.BLACK;
    public static int QRInactiveColor = Color.argb(75, 0, 0, 0);
    public static int QRBackgroundColor = Color.argb(125, 255, 255, 255);

    private static QRCode instance = new QRCode();

    public static QRCode getInstance() {
        return instance;
    }

    private BaseActivity context;
    private ViewGroup cameraContainer;
    private QRCodeReaderView cameraPreview;
    private QROverlayView qrCodeFrameView;

    private Timer timer;

    private QRCode() {
    }

    public void startRead(final BaseActivity context, final ViewGroup cameraContainer, int width, int height, final Callback callback) {
        this.context = context;
        context.askForPermission(Manifest.permission.CAMERA, BaseActivity.PERMISSION_ACCESS_CAMERA, new BaseActivity.PermissionDelegate() {
            @Override
            public void permissionResult(String permission, int requestCode, boolean granted) {
                if (requestCode == BaseActivity.PERMISSION_ACCESS_CAMERA) {
                    if (granted) {
                        if (QRCode.this.cameraContainer == null || QRCode.this.cameraContainer != cameraContainer) {
                            stopRead();
                            QRCode.this.cameraContainer = cameraContainer;
                            cameraPreview = new QRCodeReaderView(context);
                            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.MATCH_PARENT);
                            int margin = Utilities.convertDpToPx(context, 1);
                            layoutParams.setMargins(margin, margin, margin, margin);
                            cameraPreview.setLayoutParams(layoutParams);
                            cameraPreview.setAutofocusInterval(1000L);
                            cameraPreview.setOnQRCodeReadListener(QRCode.this);
                            cameraPreview.setFrontCamera();
                            cameraPreview.setQRDecodingEnabled(true);
                            cameraContainer.addView(cameraPreview);
                            qrCodeFrameView = new QROverlayView(context);
                            qrCodeFrameView.setLayoutParams(layoutParams);
                            cameraContainer.addView(qrCodeFrameView);
                        }
                        cameraPreview.startCamera();
                        if (callback != null) {
                            callback.completed(true);
                        }
                    } else {
                        alertCameraError(context);
                        if (callback != null) {
                            callback.completed(false);
                        }
                    }
                }
            }
        });
    }

    public void stopRead() {
        if (cameraPreview != null) {
            cameraPreview.stopCamera();
        }
    }

    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        if (!TextUtils.isEmpty(text)) {
            qrCodeFrameView.setPoints(points);
            NotificationCenter.Notification notification = new NotificationCenter.Notification();
            notification.userInfo.put("qrText", text);
            NotificationCenter.getInstance().post(QRCodeRecognizedNotification, notification);
            if (timer != null) {
                timer.cancel();
            }
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    QRCode.this.context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            qrCodeFrameView.setPoints(null);
                        }
                    });
                }
            }, 500);
        }
    }

    private void alertCameraError(final BaseActivity context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.QRCodeGeneration)
                .setMessage(context.getString(R.string.CameraError))
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNeutralButton(context.getString(R.string.Settings), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Utilities.openAppSettings(context);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }

    public void generate(String text, final int size, ErrorCorrectionLevel correctionLevel, final Delegate delegate) {
        generate(text, size, correctionLevel, QRColor, QRBackgroundColor, delegate);
    }

    public void generate(String text, final int size, ErrorCorrectionLevel correctionLevel, final int color, final int backgroundColor, final Delegate delegate) {
        final ErrorCorrectionLevel finalCorrectionLevel = correctionLevel != null ? correctionLevel : ErrorCorrectionLevel.L;
        new AsyncTask<String, Long, Bitmap>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Bitmap doInBackground(String... params) {
                String text = params[0];
                try {
                    return encodeAsBitmap(text, size, finalCorrectionLevel, color, backgroundColor);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                super.onPostExecute(result);
                delegate.generationDone(result);
            }

        }.execute(text);
    }

    private Bitmap encodeAsBitmap(String text, int size, ErrorCorrectionLevel correctionLevel, int color, int backgroundColor) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, correctionLevel);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? color : backgroundColor;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, size, 0, 0, w, h);
        return bitmap;
    }
}