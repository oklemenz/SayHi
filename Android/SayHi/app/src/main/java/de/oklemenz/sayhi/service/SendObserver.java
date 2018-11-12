package de.oklemenz.sayhi.service;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class SendObserver extends ContentObserver {

    public static final int NoTimeout = -1;

    private static final Handler handler = new Handler();
    private static Uri uri;

    private static final String ColumnType = "type";
    private static final String[] Projection = {ColumnType};
    private static final int MessageTypeSent = 2;

    private SendListener sendListener = null;
    private ContentResolver resolver = null;

    private String content;
    private long timeout = NoTimeout;
    private boolean sent = false;
    private boolean timedOut = false;

    private int tag = 0;

    public SendObserver(Context context, SendListener sendListener, String content, long timeout, int tag) {
        super(handler);
        uri = Uri.parse("content://" + content + "/");
        this.resolver = context.getContentResolver();
        this.sendListener = sendListener;
        this.timeout = timeout;
        this.tag = tag;
    }

    private Runnable runOut = new Runnable() {
        @Override
        public void run() {
            if (!sent) {
                timedOut = true;
                callback();
            }
        }
    };

    public void start() {
        if (resolver != null) {
            resolver.registerContentObserver(uri, true, this);
            if (timeout > NoTimeout) {
                handler.postDelayed(runOut, timeout);
            }
        }
    }

    public void stop() {
        if (resolver != null) {
            resolver.unregisterContentObserver(this);
            resolver = null;
            sendListener = null;
        }
    }

    private void callback() {
        if (sendListener != null) {
            sendListener.onSendEvent(content, tag, sent);
        }
        stop();
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        onChange(selfChange);
    }

    @Override
    public void onChange(boolean selfChange) {
        if (sent || timedOut) {
            return;
        }
        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, Projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int type = cursor.getInt(cursor.getColumnIndex(ColumnType));
                if (type == MessageTypeSent) {
                    sent = true;
                    callback();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public interface SendListener {
        void onSendEvent(String content, int tag, boolean sent);
    }
}