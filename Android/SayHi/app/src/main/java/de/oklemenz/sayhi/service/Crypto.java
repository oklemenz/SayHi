package de.oklemenz.sayhi.service;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.view.FingerprintDialogFragment;

/**
 * Created by Oliver Klemenz on 03.11.16.
 */

public class Crypto {

    interface Delegate {
        void onCancel();

        void onDone(String result);

        void onError();
    }

    private static final String ServiceName = "de.oklemenz.SayHi";

    private static final String PreferenceKeyField = "aesKey";
    private static final String PreferenceIvField = "aesIv";

    private static final String AndroidKeyStore = "AndroidKeyStore";

    private static final String AESMode = "AES/CBC/PKCS7Padding"; // KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7;
    private static final String RSAMode = "RSA/ECB/PKCS1Padding"; // KeyProperties.KEY_ALGORITHM_RSA + "/" + KeyProperties.BLOCK_MODE_ECB + "/" + KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1;

    private static final String RSAKeyAlias = "de.oklemenz.SayHi.RSA";
    private static final String AESKeyAlias = "de.oklemenz.SayHi.AES";
    private static final String AESKeyFingerprintAlias = "de.oklemenz.SayHi.AESFingerprint";

    private static Crypto instance = new Crypto();

    public static Crypto getInstance() {
        return instance;
    }

    private KeyStore keyStore;
    private String cipherAESAlias;
    private Cipher cipherAES;
    private Cipher cipherRSA;

    public boolean authenticationRequired = true;

    private Crypto() {
        try {
            this.keyStore = KeyStore.getInstance(AndroidKeyStore);
            this.keyStore.load(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SecretKeySpec getRSASecuredAESKey() throws Exception {
        SharedPreferences pref = AppDelegate.getInstance().Context.getSharedPreferences(ServiceName, Context.MODE_PRIVATE);
        String aesKey = pref.getString(PreferenceKeyField, null);

        if (aesKey == null) {
            byte[] key = new byte[16];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(key);

            aesKey = encryptRSA(Base64.encodeToString(key, Base64.NO_WRAP));

            SharedPreferences.Editor edit = pref.edit();
            edit.putString(PreferenceKeyField, aesKey);
            edit.commit();
        }

        byte[] key = Base64.decode(decryptRSA(aesKey), Base64.NO_WRAP);
        return new SecretKeySpec(key, "AES");
    }

    private AlgorithmParameterSpec initIV() throws Exception {
        SharedPreferences pref = AppDelegate.getInstance().Context.getSharedPreferences(ServiceName, Context.MODE_PRIVATE);
        String aesIv = pref.getString(PreferenceIvField, null);
        if (aesIv == null) {
            byte[] iv = new byte[16];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);
            storeIV(iv);
        }
        return new IvParameterSpec(Base64.decode(aesIv, Base64.NO_WRAP));
    }

    private void storeIV(byte[] iv) {
        SharedPreferences pref = AppDelegate.getInstance().Context.getSharedPreferences(ServiceName, Context.MODE_PRIVATE);
        String aesIv = Base64.encodeToString(iv, Base64.NO_WRAP);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString(PreferenceIvField, aesIv);
        edit.commit();
    }

    private String encryptAES(String text) throws Exception {
        byte[] encodedBytes = cipherAES.doFinal(text.getBytes("UTF-8"));
        return Base64.encodeToString(encodedBytes, Base64.NO_WRAP);
    }

    private String decryptAES(String encryptedText) throws Exception {
        byte[] decodedBytes = cipherAES.doFinal(Base64.decode(encryptedText, Base64.NO_WRAP));
        return new String(decodedBytes, 0, decodedBytes.length, "UTF-8");
    }

    private String encryptRSA(String text) throws Exception {
        initCipherRSA();
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(RSAKeyAlias, null);
        cipherRSA.init(Cipher.ENCRYPT_MODE, privateKeyEntry.getCertificate().getPublicKey());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipherRSA);
        cipherOutputStream.write(text.getBytes("UTF-8"));
        cipherOutputStream.close();
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
    }

    private String decryptRSA(String encryptedText) throws Exception {
        initCipherRSA();
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(RSAKeyAlias, null);
        cipherRSA.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());
        CipherInputStream cipherInputStream = new CipherInputStream(
                new ByteArrayInputStream(Base64.decode(encryptedText, Base64.NO_WRAP)), cipherRSA);
        ArrayList<Byte> values = new ArrayList<>();
        int nextByte;
        while ((nextByte = cipherInputStream.read()) != -1) {
            values.add((byte) nextByte);
        }
        byte[] decodedBytes = new byte[values.size()];
        for (int i = 0; i < decodedBytes.length; i++) {
            decodedBytes[i] = values.get(i);
        }
        return new String(decodedBytes, 0, decodedBytes.length, "UTF-8");
    }

    private Cipher initCipherRSA() throws Exception {
        if (cipherRSA == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cipherRSA = Cipher.getInstance(RSAMode);
            } else {
                cipherRSA = Cipher.getInstance(RSAMode, "AndroidOpenSSL");
            }
        }
        return cipherRSA;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private Cipher initCipherAES(int mode, boolean useFingerprint) throws Exception {
        String keyAlias = useFingerprint ? AESKeyFingerprintAlias : AESKeyAlias;
        if (!keyAlias.equals(cipherAESAlias)) {
            cipherAES = null;
            authenticationRequired = true;
        }
        if (cipherAES == null) {
            cipherAES = Cipher.getInstance(AESMode);
        }
        Key key = keyStore.getKey(keyAlias, null);
        if (mode == Cipher.ENCRYPT_MODE) {
            cipherAES.init(mode, key);
            IvParameterSpec ivParams = cipherAES.getParameters().getParameterSpec(IvParameterSpec.class);
            byte[] iv = ivParams.getIV();
            storeIV(iv);
        } else {
            cipherAES.init(mode, key, initIV());
        }
        cipherAESAlias = keyAlias;
        return cipherAES;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initAESKey(boolean useFingerprint) throws Exception {
        String keyAlias = useFingerprint ? AESKeyFingerprintAlias : AESKeyAlias;
        if (!keyStore.containsAlias(keyAlias)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore);
            KeyGenParameterSpec.Builder keyGenParameterSpecBuilder = new KeyGenParameterSpec.Builder(keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
            if (useFingerprint) {
                keyGenParameterSpecBuilder.setUserAuthenticationRequired(true);
                keyGenParameterSpecBuilder.setUserAuthenticationValidityDurationSeconds(Integer.MAX_VALUE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    keyGenParameterSpecBuilder.setInvalidatedByBiometricEnrollment(false);
                }
            }
            keyGenerator.init(keyGenParameterSpecBuilder.build());
            keyGenerator.generateKey();
        }
    }

    @SuppressWarnings("deprecation")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void initRSAKeys() throws Exception {
        if (!keyStore.containsAlias(RSAKeyAlias)) {
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, 9999);
            KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(AppDelegate.getInstance().Context)
                    .setAlias(RSAKeyAlias)
                    .setSubject(new X500Principal("CN=" + ServiceName + ", O=Android Authority"))
                    .setSerialNumber(BigInteger.ONE)
                    .setStartDate(start.getTime())
                    .setEndDate(end.getTime())
                    .build();
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
            generator.initialize(spec);
            generator.generateKeyPair();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void encrypt(final String text, boolean useFingerprint, final Delegate delegate) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                initAESKey(false /*useFingerprint*/);
                initCipherAES(Cipher.ENCRYPT_MODE, false /*useFingerprint*/);
                if (useFingerprint && authenticationRequired) {
                    authenticate(new FingerprintDialogFragment.Delegate() {
                        @Override
                        public void onAuthCancel() {
                            if (delegate != null) {
                                delegate.onCancel();
                            }
                        }

                        @Override
                        public void onAuthSuccess() {
                            authenticationRequired = false;
                            try {
                                if (delegate != null) {
                                    delegate.onDone(encryptAES(text));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                if (delegate != null) {
                                    delegate.onDone(null);
                                }
                            }
                        }

                        @Override
                        public void onAuthFailed() {
                            if (delegate != null) {
                                delegate.onDone(null);
                            }
                        }
                    });
                } else {
                    if (delegate != null) {
                        delegate.onDone(encryptAES(text));
                    }
                }
            } else {
                initRSAKeys();
                cipherAES = Cipher.getInstance(AESMode, "BC");
                cipherAES.init(Cipher.ENCRYPT_MODE, getRSASecuredAESKey(), initIV());
                if (delegate != null) {
                    delegate.onDone(encryptAES(text));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (delegate != null) {
                delegate.onError();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void decrypt(final String encryptedText, boolean useFingerprint, final Delegate delegate) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                initAESKey(false /*useFingerprint*/);
                initCipherAES(Cipher.DECRYPT_MODE, false /*useFingerprint*/);
                if (useFingerprint && authenticationRequired) {
                    authenticate(new FingerprintDialogFragment.Delegate() {
                        @Override
                        public void onAuthCancel() {
                            if (delegate != null) {
                                delegate.onCancel();
                            }
                        }

                        @Override
                        public void onAuthSuccess() {
                            authenticationRequired = false;
                            try {
                                if (delegate != null) {
                                    delegate.onDone(decryptAES(encryptedText));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                if (delegate != null) {
                                    delegate.onDone(null);
                                }
                            }
                        }

                        @Override
                        public void onAuthFailed() {
                            if (delegate != null) {
                                delegate.onDone(null);
                            }
                        }
                    });
                } else {
                    if (delegate != null) {
                        delegate.onDone(decryptAES(encryptedText));
                    }
                }
            } else {
                initRSAKeys();
                cipherAES = Cipher.getInstance(AESMode, "BC");
                cipherAES.init(Cipher.DECRYPT_MODE, getRSASecuredAESKey(), initIV());
                if (delegate != null) {
                    delegate.onDone(decryptAES(encryptedText));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (delegate != null) {
                delegate.onError();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void authenticate(final FingerprintDialogFragment.Delegate delegate) {
        FingerprintDialogFragment fingerprintDialogFragment = new FingerprintDialogFragment();
        fingerprintDialogFragment.setCryptoObject(new FingerprintManager.CryptoObject(cipherAES), delegate);
        fingerprintDialogFragment.show(((Activity) AppDelegate.getInstance().Context).getFragmentManager(), "fingerprintDialogFragment");
    }

    public boolean isOpen() {
        return cipherAES != null || cipherRSA != null;
    }

    public void close() {
        cipherAES = null;
        cipherRSA = null;
        authenticationRequired = true;
    }

    public static byte[] saltKey(String key) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(key.getBytes("UTF-8"));
        byte[] keyBytes = new byte[32];
        System.arraycopy(digest.digest(), 0, keyBytes, 0, keyBytes.length);
        return keyBytes;
    }

    public static String hash(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(text.getBytes("UTF-8"));
        byte[] bytes = digest.digest();
        return String.format("%0" + (bytes.length * 2) + "x", new BigInteger(1, bytes));
    }

    public static byte[] generateRandom(int length) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
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
}