package de.oklemenz.sayhi.service;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.UserData;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class Mail implements SendObserver.SendListener {

    public interface Callback {
        void completion();
    }

    private static Mail instance = new Mail();

    public static Mail getInstance() {
        return instance;
    }

    private SendObserver sendObserver;
    private Callback callback;

    private static int MessageInviteTag = 1;
    private static int MailInviteTag = 2;
    private static int MailSupportTag = 3;

    public void stopSendObserver() {
        if (sendObserver != null) {
            sendObserver.stop();
            sendObserver = null;
            callback = null;
        }
    }

    public void sendInvitationMessage(BaseActivity context, final Callback callback) {
        this.callback = callback;
        //this.sendObserver = new SendObserver(context, this, "sms", 60 * 1000, MessageInviteTag); // 60 Seconds
        //this.sendObserver.start();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("sms:"));
        intent.putExtra("sms_body",
                String.format(context.getString(R.string.InviteMessage),
                        context.getString(R.string.AppStoreUrlIOS),
                        context.getString(R.string.AppStoreUrlAndroid),
                        !TextUtils.isEmpty(UserData.getInstance().getFirstName()) ? UserData.getInstance().getFirstName() : context.getString(R.string.Bye)));

        AppDelegate.getInstance().preventBackgroundProtect = true;
        try {
            context.startActivityForResult(intent, MessageInviteTag);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(context, context.getString(R.string.MessageNotConfigured), Toast.LENGTH_SHORT).show();
        }
    }

    public void sendInvitationMail(BaseActivity context, final Callback callback) {
        this.callback = callback;
        //this.sendObserver = new SendObserver(context, this, "mailto", 60 * 1000, MailInviteTag); // 60 Seconds
        //this.sendObserver.start();

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.InviteSubject));
        intent.putExtra(Intent.EXTRA_TEXT, Utilities.fromHtml(
                String.format(context.getString(R.string.InviteBody),
                        context.getString(R.string.AppStoreUrlIOS),
                        context.getString(R.string.AppStoreUrlIOS),
                        context.getString(R.string.AppStoreUrlAndroid),
                        context.getString(R.string.AppStoreUrlAndroid),
                        UserData.getInstance().getFirstName())));

        AppDelegate.getInstance().preventBackgroundProtect = true;
        try {
            context.startActivityForResult(intent, MailInviteTag);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(context, context.getString(R.string.MailNotConfigured), Toast.LENGTH_SHORT).show();
        }
    }

    public void sendSupportMail(BaseActivity context) {
        //this.sendObserver = new SendObserver(context, this, "mailto", 60 * 1000, MailSupportTag); // 60 Seconds
        //this.sendObserver.start();

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + context.getString(R.string.ContactMail)));
        intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.ContactSubject));
        intent.putExtra(Intent.EXTRA_TEXT, Utilities.fromHtml(
                String.format(context.getString(R.string.ContactBody),
                        UserData.getInstance().getFirstName())));

        AppDelegate.getInstance().preventBackgroundProtect = true;
        try {
            context.startActivityForResult(intent, MailSupportTag);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(context, context.getString(R.string.MailNotConfigured), Toast.LENGTH_SHORT).show();
        }
    }

    public void onSendEvent(String content, int tag, boolean sent) {
        if (tag == MailInviteTag) {
            UserData.getInstance().increaseInviteFriendSentCount();
            Analytics.getInstance().logInviteFriend();
        } else if (tag == MailSupportTag) {
            Analytics.getInstance().logSupportMail();
        }
        if (callback != null) {
            callback.completion();
        }
    }
}