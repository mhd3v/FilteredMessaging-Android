package mhd3v.filteredsms;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Mahad on 12/22/2017.
 */

public class SmsBroadcastReceiver extends BroadcastReceiver {
    NotificationManager mNotificationManager;
    public static final String SMS_BUNDLE = "pdus";
    String address;
    String smsBody;
    boolean isContact;

    private static final String ACTION_SMS_NEW = "android.provider.Telephony.SMS_RECEIVED";


    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();

        if (ACTION_SMS_NEW.equals(action)) {

            Bundle intentExtras = intent.getExtras();

            if (intentExtras != null) {
                Object[] sms = (Object[]) intentExtras.get(SMS_BUNDLE);

                for (int i = 0; i < sms.length; ++i) {
                    String format = intentExtras.getString("format");
                    SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) sms[i], format);

                    smsBody = smsMessage.getMessageBody().toString();
                    address = smsMessage.getOriginatingAddress();

                    Log.d("mahad", smsBody);

                    String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(context);

                    if (defaultSmsApp.equals("mhd3v.filteredsms")) {

                            ContentValues values = new ContentValues();
                            values.put("address", address);
                            values.put("body", smsBody);
                            context.getContentResolver().insert(Uri.parse("content://sms/inbox"), values);

                    }

                    Cursor cursor = context.getContentResolver().query(Uri
                            .parse("content://sms"), null, null, null, null);

                    cursor.moveToFirst();

                    MainActivity mainActivityInstance = MainActivity.inst;
                    CoversationActivity conversationInstance = CoversationActivity.conversationInstance;

                    if (conversationInstance != null) {

                        if (conversationInstance.threadId.equals(cursor.getString(cursor.getColumnIndex("thread_id")))) {

                            messages newSms = new messages(smsBody, Long.toString(System.currentTimeMillis()));

                            ArrayList<messages> newMessageList = new ArrayList<>();

                            newMessageList.addAll(conversationInstance.messageList);
                            newMessageList.add(newSms);

                            conversationInstance.adapter.updateMessageList(newMessageList);

                            CoversationActivity.refreshMain();

                        } else
                            CoversationActivity.refreshMain();

                    } else if (mainActivityInstance != null) {

                        if (mainActivityInstance.active)
                            mainActivityInstance.refreshOnExtraThread();

                        else {

                            mainActivityInstance.refreshInbox = true;

                            isContact = false;

                            String contactName = getContactName(context, address);

                            if (isContact == true) {

                                Intent conversationThreadIntent = new Intent(context, CoversationActivity.class);
                                conversationThreadIntent.setAction("android.intent.action.NotificationClicked");
                                conversationThreadIntent.putExtra("threadId", cursor.getString(cursor.getColumnIndex("thread_id")));
                                conversationThreadIntent.putExtra("senderName",contactName);
                                conversationThreadIntent.putExtra("sender",address);

                                PendingIntent conversationThreadPendingIntent =
                                        PendingIntent.getActivity(
                                                context,
                                                0,
                                                conversationThreadIntent,
                                                PendingIntent.FLAG_UPDATE_CURRENT
                                        );


                                NotificationCompat.Builder mBuilder =
                                        new NotificationCompat.Builder(context)
                                                .setSmallIcon(R.drawable.main_icon_nobg)
                                                .setContentTitle(contactName)
                                                .setContentText(smsBody)
                                                .setContentIntent(conversationThreadPendingIntent)
                                                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);

                                int id = (int) System.currentTimeMillis();


                                mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                mNotificationManager.notify(id, mBuilder.build());

                            }

                        }

                    } else {

                        isContact = false;

                        String contactName = getContactName(context, address);

                        if (isContact == true) {

                            Intent conversationThreadIntent = new Intent(context, CoversationActivity.class);
                            conversationThreadIntent.setAction("android.intent.action.NotificationClicked");
                            conversationThreadIntent.putExtra("threadId", cursor.getString(cursor.getColumnIndex("thread_id")));
                            conversationThreadIntent.putExtra("senderName",contactName);
                            conversationThreadIntent.putExtra("sender",address);

                            PendingIntent conversationThreadPendingIntent =
                                    PendingIntent.getActivity(
                                            context,
                                            0,
                                            conversationThreadIntent,
                                            PendingIntent.FLAG_UPDATE_CURRENT
                                    );


                            NotificationCompat.Builder mBuilder =
                                    new NotificationCompat.Builder(context)
                                            .setSmallIcon(R.drawable.main_icon_nobg)
                                            .setContentTitle(contactName)
                                            .setContentText(smsBody)
                                            .setContentIntent(conversationThreadPendingIntent)
                                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);

                            int id = (int) System.currentTimeMillis();


                            mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                            mNotificationManager.notify(id, mBuilder.build());
                        }
                    }


                }
            }

        }



    }

    public String getContactName(Context context, String phoneNo) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNo));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) {
            return phoneNo;
        }
        String Name = phoneNo;
        if (cursor.moveToFirst()) {
            isContact = true;
            Name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return Name;
    }




}

