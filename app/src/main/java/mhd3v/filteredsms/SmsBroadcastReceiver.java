package mhd3v.filteredsms;


import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
    Cursor cursor;

    public void onReceive(Context context, Intent intent) {
        Bundle intentExtras = intent.getExtras();

        if (intentExtras != null) {
            Object[] sms = (Object[]) intentExtras.get(SMS_BUNDLE);
            String smsMessageStr = "";
            for (int i = 0; i < sms.length; ++i) {
                String format = intentExtras.getString("format");
                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) sms[i], format);


                smsBody = smsMessage.getMessageBody().toString();
                address = smsMessage.getOriginatingAddress();

                smsMessageStr += "SMS From: " + address + "\n";
                smsMessageStr += smsBody + "\n";

                String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(context);

                if(defaultSmsApp.equals("mhd3v.filteredsms")){
                    ContentValues values = new ContentValues();
                    values.put("address", address);//sender name
                    values.put("body", smsBody);
                    context.getContentResolver().insert(Uri.parse("content://sms/inbox"), values);
                }

                cursor = context.getContentResolver().query(Uri
                        .parse("content://sms"), null, null, null, null);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                cursor.moveToFirst();

            }

            MainActivity mainActivityInstance = MainActivity.inst;

            CoversationActivity conversationInstance = CoversationActivity.conversationInstance;

            if (conversationInstance != null) {

                if(conversationInstance.threadId.equals(cursor.getString(cursor.getColumnIndex("thread_id")))){

                    messages newSms = new messages(smsBody ,Long.toString(System.currentTimeMillis()));

                    ArrayList<messages> newMessageList = new ArrayList<>();

                    newMessageList.addAll(conversationInstance.messageList);

                    newMessageList.add(newSms);

                    conversationInstance.adapter.updateMessageList(newMessageList);

                    CoversationActivity.refreshMain();

                }

                else {

                    CoversationActivity.refreshMain();

                }


            }

            else if(mainActivityInstance.active){

                mainActivityInstance.refreshOnExtraThread();

            }

            else if(!mainActivityInstance.active){

                mainActivityInstance.refreshInbox = true;

                isContact = false;

                String contactName = getContactName(context, address);

                if(isContact == true) {

                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context)
                                    .setSmallIcon(R.drawable.ic_android_black_24dp)
                                    .setContentTitle(contactName)
                                    .setContentText(smsBody);


                    mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


                    mNotificationManager.notify(001, mBuilder.build());

                }

            }

            else {

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

