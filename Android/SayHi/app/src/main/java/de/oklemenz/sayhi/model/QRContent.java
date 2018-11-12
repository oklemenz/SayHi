package de.oklemenz.sayhi.model;

import android.text.TextUtils;

import java.io.Serializable;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class QRContent implements Serializable {

    public static int MatchMessageLength = 20; // 20: Firebase Key
    public static int MatchSessionLength = 20; // 20: Session Key

    private static int MatchMaxLength = MatchMessageLength + MatchSessionLength;

    public String message;
    public String session;

    public QRContent(String message, String session) {
        this.message = message;
        this.session = session;
    }

    @Override
    public String toString() {
        String[] parts = {
                message.substring(0, MatchMessageLength),
                session.substring(0, MatchSessionLength)
        };
        return TextUtils.join("", parts);
    }

    public static QRContent generate(String message, String session) {
        return new QRContent(
                message,
                session
        );
    }

    public static QRContent parse(String text) {
        if (text.length() == MatchMaxLength) {
            String message = text.substring(0, MatchMessageLength);
            String session = text.substring(MatchMessageLength, MatchMaxLength);
            return new QRContent(
                    message,
                    session);
        } else if (text.length() == MatchMessageLength) {
            return new QRContent(
                    text,
                    ""
            );
        }
        return null;
    }
}