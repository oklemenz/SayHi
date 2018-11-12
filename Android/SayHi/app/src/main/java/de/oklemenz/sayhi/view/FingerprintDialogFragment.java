package de.oklemenz.sayhi.view;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.support.annotation.RequiresApi;
import android.view.View;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;

import static android.content.Context.FINGERPRINT_SERVICE;
import static android.hardware.fingerprint.FingerprintManager.FINGERPRINT_ERROR_CANCELED;

/**
 * Created by Oliver Klemenz on 02.03.17.
 */
@TargetApi(Build.VERSION_CODES.M)
public class FingerprintDialogFragment extends DialogFragment {

    public interface Delegate {
        void onAuthCancel();

        void onAuthSuccess();

        void onAuthFailed();
    }

    private Delegate delegate;
    private FingerprintManager.CryptoObject cryptoObject;
    private FingerprintHandler fingerprintHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(false);
        setRetainInstance(true);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogFragment);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());

        builder.setTitle(R.string.FingerprintFor)
                .setCancelable(false)
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (delegate != null) {
                            delegate.onAuthCancel();
                        }
                    }
                });
        final AlertDialog alert = builder.create();

        View view = getActivity().getLayoutInflater().inflate(R.layout.fingerprint_dialog, null, false);
        alert.setView(view);
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(AppDelegate.AccentColor);
            }
        });
        return alert;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fingerprintHandler != null) {
            fingerprintHandler.startAuth(cryptoObject);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fingerprintHandler != null) {
            fingerprintHandler.stopAuth();
        }
    }

    public void setCryptoObject(FingerprintManager.CryptoObject cryptoObject, final Delegate delegate) {
        this.cryptoObject = cryptoObject;
        this.delegate = delegate;
        fingerprintHandler = new FingerprintHandler() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                if (delegate != null) {
                    delegate.onAuthSuccess();
                }
                dismiss();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (delegate != null) {
                    if (errorCode == FINGERPRINT_ERROR_CANCELED) { // Fingerprint operation canceled.
                        delegate.onAuthCancel();
                    } else {
                        delegate.onAuthFailed();
                    }
                }
                dismiss();
            }
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    class FingerprintHandler extends FingerprintManager.AuthenticationCallback {

        CancellationSignal signal;

        @Override
        public void onAuthenticationError(int errorCode, CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);
        }

        @Override
        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
            super.onAuthenticationHelp(helpCode, helpString);
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
        }

        public void startAuth(FingerprintManager.CryptoObject cryptoObject) {
            FingerprintManager fingerprintManager = (FingerprintManager) AppDelegate.getInstance().Context.getSystemService(FINGERPRINT_SERVICE);
            signal = new CancellationSignal();
            try {
                fingerprintManager.authenticate(cryptoObject, signal, 0, this, null);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        public void stopAuth() {
            if (signal != null) {
                signal.cancel();
                signal = null;
            }
        }
    }
}
