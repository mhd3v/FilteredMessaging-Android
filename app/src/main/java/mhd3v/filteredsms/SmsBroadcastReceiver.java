package mhd3v.filteredsms;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.text.format.DateFormat;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Mahad on 12/22/2017.
 */

public class SmsBroadcastReceiver extends BroadcastReceiver {
    NotificationManager mNotificationManager;
    public static final String SMS_BUNDLE = "pdus";
    String address;
    String smsBody;
    String threadId;
    boolean isContact;
    Cursor cursor;
    NotificationCompat.InboxStyle inboxStyle;
    SQLiteDatabase filteredDatabase;

    int blackListStatus;

    private static final String ACTION_SMS_NEW = "android.provider.Telephony.SMS_RECEIVED";


    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();

        if (ACTION_SMS_NEW.equals(action)) {

            Bundle intentExtras = intent.getExtras();

            if (intentExtras != null) {

                SmsMessage smsMessage;

                if (Build.VERSION.SDK_INT >= 19) { //KITKAT
                    SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                    smsMessage = msgs[0];
                }

                else {
                    Object pdus[] = (Object[]) intentExtras.get("pdus");
                    smsMessage = SmsMessage.createFromPdu((byte[]) pdus[0]);
                }


                smsBody = smsMessage.getMessageBody();
                address = smsMessage.getOriginatingAddress();

                String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(context);

                if (defaultSmsApp.equals("mhd3v.filteredsms")) {

                    ContentValues cv = new ContentValues();
                    cv.put("address", address);
                    cv.put("body", smsBody);
                    context.getContentResolver().insert(Uri.parse("content://sms/inbox"), cv);
                }

                System.out.print("New message arrived");

                ContentValues cv = new ContentValues();

                filteredDatabase = context.openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

                cursor = context.getContentResolver().query(Uri.parse("content://sms"), null, null, null, null);
                cursor.moveToFirst();

                String date = cursor.getString(cursor.getColumnIndex("date"));
                threadId = cursor.getString(cursor.getColumnIndex("thread_id"));

                cv.put("thread_id", threadId);
                cv.put("date_string", date);
                cv.put("type", 1);
                cv.put("address", address);
                cv.put("body", smsBody);
                cv.put("failed", 0);

                isContact = false;
                String contactName = getContactName(context, address);

                ContentValues filteredThreadsCv = new ContentValues();
                ContentValues existingThreadCv = new ContentValues();

                existingThreadCv.put("read",0); //need to update read status whether new thread or previous updated
                filteredThreadsCv.put("read",0);

                Cursor blackListCheckCursor = filteredDatabase.rawQuery("select blacklisted from filteredThreads where thread_id = " + threadId +";",null);

                boolean messageFromNewSender = false;

                if(!(blackListCheckCursor.moveToFirst())){ //no entry in filtered_threads so a new message
                    messageFromNewSender = true;
                }


                if (isContact){
                    filteredThreadsCv.put("thread_id", threadId);
                    filteredThreadsCv.put("filtered_status","filtered");
                    filteredThreadsCv.put("date_string", date);

                    if(messageFromNewSender){ //if message from new sender and in contact list
                        filteredThreadsCv.put("blacklisted", 0);
                        blackListStatus = 0;
                        filteredDatabase.insert("filteredThreads", null, filteredThreadsCv); //insert
                    }

                    else{ //message from a contact whose thread already exists, don't update blacklisted column in this case

                        blackListStatus = blackListCheckCursor.getInt(blackListCheckCursor.getColumnIndex("blacklisted"));

                        existingThreadCv.put("date_string", date);
                        filteredDatabase.update("filteredThreads", existingThreadCv, "thread_id =" + threadId, null); //update

                    }

                    cv.put("sender_name", contactName);
                }

                else {
                    filteredThreadsCv.put("thread_id",threadId);
                    filteredThreadsCv.put("filtered_status","unfiltered");
                    filteredThreadsCv.put("date_string", date);

                    if(messageFromNewSender){ //if message from new sender and not in contact list
                        filteredThreadsCv.put("blacklisted", 1);
                        blackListStatus = 1;
                        filteredDatabase.insert("filteredThreads", null, filteredThreadsCv); //insert
                    }

                    else{ //message from a sender whose thread already exists, don't update blacklisted column in this case

                        blackListStatus = blackListCheckCursor.getInt(blackListCheckCursor.getColumnIndex("blacklisted"));

                        existingThreadCv.put("date_string", date);
                        filteredDatabase.update("filteredThreads", existingThreadCv, "thread_id =" + threadId, null); //update

                    }

                    cv.put("sender_name", "");
                }

                filteredDatabase.insertOrThrow("messageTable", null, cv);

                blackListCheckCursor.close();
                cursor.close();
                filteredDatabase.close();

                MainActivity mainActivityInstance = MainActivity.inst;
                ConversationActivity conversationInstance = ConversationActivity.conversationInstance;

                if (conversationInstance != null) { //conversation thread is active

                    if (conversationInstance.threadId.equals(threadId)) {

                            Message newSms = new Message(smsBody, Long.toString(System.currentTimeMillis()));

                            conversationInstance.messageList.add(newSms);

                            conversationInstance.adapter.notifyDataSetChanged();

                            ConversationActivity.refreshMain();

                            if(!conversationInstance.active)
                                setNotfication(context);

                            //if conversation instance is active and open, then each new message is marked as read
                            filteredDatabase = context.openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);
                            ContentValues readCv = new ContentValues();
                            readCv.put("read", 1);
                            filteredDatabase.update("filteredThreads", readCv, "thread_id=" + threadId, null);
                            filteredDatabase.close();
                            //---


                    }

                    else{
                        setNotfication(context);
                        ConversationActivity.refreshMain();
                    }

                }

                else if (mainActivityInstance != null) {

                    if (MainActivity.active)
                        mainActivityInstance.refreshOnExtraThread();

                    else {

                        MainActivity.refreshInbox = true;
                        setNotfication(context);
                    }
                }

                else  //MainActivity not instantiated
                    setNotfication(context);

                }
            }

        }


    void setNotfication(Context context){

        if(blackListStatus == 0){

            String contactName = getContactName(context, address);

            Intent conversationThreadIntent = new Intent(context, ConversationActivity.class);
            conversationThreadIntent.setAction("android.intent.action.NotificationClicked");
            conversationThreadIntent.putExtra("threadId", threadId);

            if(isContact)
                conversationThreadIntent.putExtra("senderName",contactName);
            else
                conversationThreadIntent.putExtra("senderName","");

            conversationThreadIntent.putExtra("sender",address);

            PendingIntent conversationThreadPendingIntent =
                    PendingIntent.getActivity(
                            context,
                            0,
                            conversationThreadIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

            inboxStyle = new NotificationCompat.InboxStyle();

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

            SharedPreferences.Editor editor = sp.edit();

            editor.putString(threadId, sp.getString(threadId,"")+smsBody+"\n");

            editor.apply();

            String previousNotification = sp.getString(threadId,"");

            String[] result = previousNotification.split("\n");


            for (String aResult : result) {

                if (!(aResult.equals(null)))
                    inboxStyle.addLine(aResult);
            }

            inboxStyle.setBigContentTitle(contactName);

            String CHANNEL_ID = threadId;// The id of the channel.
            CharSequence name = "New message";// The user-visible name of the channel.
            int importance = 0;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                importance = NotificationManager.IMPORTANCE_HIGH;
            }


            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context, threadId)
                            .setSmallIcon(R.drawable.main_icon_nobg)
                            .setNumber(result.length)
                            .setContentTitle(contactName)
                            .setContentText(result[result.length-1])
                            .setStyle(inboxStyle)
                            .setContentIntent(conversationThreadPendingIntent)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);

            int id = Integer.parseInt(threadId); //assign ID on base of threadID

            mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                mNotificationManager.createNotificationChannel(mChannel);
            }

            mNotificationManager.notify(id, mBuilder.build());

        }


    }

    public String convertDate(String dateInMilliseconds,String dateFormat) {
        return DateFormat.format(dateFormat, Long.parseLong(dateInMilliseconds)).toString();
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

