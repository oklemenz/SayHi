package de.oklemenz.sayhi.service;

import java.io.UnsupportedEncodingException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Oliver Klemenz on 09.11.16.
 */

public class AESCrypt {

    private final Cipher cipher;
    private final SecretKeySpec key;
    private AlgorithmParameterSpec spec;

    private static final String SHARED_IV = "oklemenz07101214";
    private static final String AESMode = "AES/CBC/PKCS7Padding"; // KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7;

    public AESCrypt(String passcode) throws Exception {
        byte[] keyBytes = Crypto.saltKey(passcode);
        cipher = Cipher.getInstance(AESMode);
        key = new SecretKeySpec(keyBytes, "AES"); // KeyProperties.KEY_ALGORITHM_AES
        spec = getIV();
    }

    private AlgorithmParameterSpec getIV() throws UnsupportedEncodingException {
        byte[] iv = SHARED_IV.getBytes("UTF-8");
        IvParameterSpec ivParameterSpec;
        ivParameterSpec = new IvParameterSpec(iv);
        return ivParameterSpec;
    }

    public String encrypt(String plainText) throws Exception {
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Crypto.toBase64(encrypted);
    }

    public String decrypt(String cryptedText) throws Exception {
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        byte[] bytes = Crypto.fromBase64(cryptedText);
        byte[] decrypted = cipher.doFinal(bytes);
        return new String(decrypted, "UTF-8");
    }
}