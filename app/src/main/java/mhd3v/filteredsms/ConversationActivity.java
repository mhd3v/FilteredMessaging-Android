package mhd3v.filteredsms;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;


public class ConversationActivity extends AppCompatActivity {

    ArrayList<messages> messageList;
    EditText input;

    Toolbar toolbar;

    boolean active;

    String sender;
    String senderName;
    String threadId;

    int blacklisted;
    int read;

    boolean cameFromNotification = false;

    customAdapter adapter;
    static Intent intent;

    private String SimState = "";

    MenuItem blacklistbutton;
    MenuItem whitelistbutton;
    MenuItem addToContactsButton;

    SQLiteDatabase filteredDatabase;

    static ConversationActivity conversationInstance;

    int pendingIntentCount;

    BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coversation);

        active = true;

        messageList = new ArrayList<>();

        intent = getIntent();

        sender = intent.getStringExtra("sender");
        senderName = intent.getStringExtra("senderName");
        threadId = intent.getStringExtra("threadId");
        blacklisted = intent.getIntExtra("blacklisted", 0);
        read = intent.getIntExtra("read", 0);

        //--- update read status
        filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

        if(read == 0){
            ContentValues cv = new ContentValues();
            cv.put("read", 1);
            filteredDatabase.update("filteredThreads", cv, "thread_id =" + threadId, null);

            //update Android's SMS db
            getContentResolver().update(Uri.parse("content://sms"),cv, "thread_id="+threadId, null);

            if(!cameFromNotification)
                refreshMain();
        }

        filteredDatabase.close();
        //-------

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.apply();

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        if (intent.getAction().equals("android.intent.action.NotificationClicked")) {

            cameFromNotification = true;

            Cursor cursor = getContentResolver().query(Uri.parse("content://sms/"), null, "thread_id=" + threadId, null, null);

            cursor.moveToFirst();

            int indexBody = cursor.getColumnIndex("body");
            String type = Integer.toString(cursor.getColumnIndex("type"));

            do {

                if (cursor.getString(Integer.parseInt(type)).equalsIgnoreCase("1")) {
                    //received messages
                    messages newMessage = new messages(cursor.getString(indexBody), cursor.getString(cursor.getColumnIndex("date")));
                    messageList.add(newMessage);
                } else if (cursor.getString(Integer.parseInt(type)).equalsIgnoreCase("2")) {
                    messages newMessage = new messages(cursor.getString(indexBody), cursor.getString(cursor.getColumnIndex("date")));
                    newMessage.isUserMessage = true;
                    messageList.add(newMessage);
                }
            }
            while (cursor.moveToNext());

        } else {
            Bundle args = intent.getBundleExtra("BUNDLE");
            messageList = (ArrayList<messages>) args.getSerializable("messageList");
        }

        Collections.reverse(messageList);

        ListView conversation = (ListView) findViewById(R.id.conversationList);

        conversation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                if(messageList.get(position).failed){
                    new AlertDialog.Builder(ConversationActivity.this)
                            .setMessage("Try again?")
                            .setCancelable(false)
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    messageList.remove(position);
                                    adapter.notifyDataSetChanged();
                                    SQLiteDatabase filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);
                                    filteredDatabase.delete("messageTable","date_string=" + messageList.get(position).time, null);
                                    filteredDatabase.close();


                                }
                            })
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int id) {

                                    messageList.remove(position);
                                    adapter.notifyDataSetChanged();
                                    SQLiteDatabase filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);
                                    filteredDatabase.delete("messageTable","date_string=" + messageList.get(position).time, null);
                                    filteredDatabase.close();
                                    sendSms(messageList.get(position));

                                }
                            }).show();

                }

            }
        });

        adapter = new customAdapter();

        conversation.setAdapter(adapter);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (senderName.equals(""))
                getSupportActionBar().setTitle(sender);
            else
                getSupportActionBar().setTitle(senderName);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.conversation_menu, menu);


        blacklistbutton = toolbar.getMenu().findItem(R.id.blacklistbutton);
        whitelistbutton = toolbar.getMenu().findItem(R.id.whitelistbutton);
        addToContactsButton = toolbar.getMenu().findItem(R.id.addToContacts);

        if (blacklisted == 0)
            blacklistbutton.setVisible(true);
        else
            whitelistbutton.setVisible(true);

        if (senderName.equals(""))
            addToContactsButton.setVisible(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }


    public static void refreshMain() {
        MainActivity mainInstance = MainActivity.getInstance();
        mainInstance.refreshInbox = true;
    }

    public void addToWhiteList(MenuItem item) {

        filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

        ContentValues cv = new ContentValues();

        cv.put("blacklisted", 0);

        filteredDatabase.update("filteredThreads", cv, "thread_id =" + threadId, null);

        Toast.makeText(this, "Added to filtered", Toast.LENGTH_SHORT).show();

        filteredDatabase.close();

        whitelistbutton.setVisible(false);
        blacklistbutton.setVisible(true);

        refreshMain();

    }

    public void addToBlackList(MenuItem item) {

        filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

        ContentValues cv = new ContentValues();

        cv.put("blacklisted", 1);

        filteredDatabase.update("filteredThreads", cv, "thread_id =" + threadId, null);

        Toast.makeText(this, "Added to unfiltered", Toast.LENGTH_SHORT).show();

        filteredDatabase.close();

        whitelistbutton.setVisible(true);
        blacklistbutton.setVisible(false);

        refreshMain();

    }

    public void callPhone(MenuItem item) {

        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + sender));

        startActivity(callIntent);

    }


    public void addToContacts(MenuItem item) {

        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, sender);
        startActivity(intent);

    }


    class customAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return messageList.size() ;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            view = getLayoutInflater().inflate(R.layout.conversation_list,null);

            if(messageList.get(i).isUserMessage){

                TextView userMessage= view.findViewById(R.id.userText);
                userMessage.setText(messageList.get(i).messageBody);
                userMessage.setVisibility(View.VISIBLE);

                TextView userTimeText = view.findViewById(R.id.userTime);
                TextView sendingText = view.findViewById(R.id.sendingText);
                TextView failedText  = view.findViewById(R.id.failedText);

                String time;

                if(messageList.get(i).sending)
                    sendingText.setVisibility(View.VISIBLE);

                else{
                    if(!messageList.get(i).failed){ //successfully sent message
                        time = convertDate(messageList.get(i).time,"dd/MM hh:mm aa");
                        userTimeText.setText(time);
                        userTimeText.setVisibility(View.VISIBLE);
                    }
                    else
                        failedText.setVisibility(View.VISIBLE);

                }

            }
            else{

                TextView senderMessage= view.findViewById(R.id.senderText);
                senderMessage.setText((messageList.get(i).messageBody).trim());
                senderMessage.setVisibility(View.VISIBLE);

                TextView senderTimeText = view.findViewById(R.id.senderTime);
                String time = convertDate(messageList.get(i).time,"dd/MM - hh:mm aa");
                senderTimeText.setText(time);
                senderTimeText.setVisibility(View.VISIBLE);


                ImageView img = view.findViewById(R.id.image_message_profile);
                img.setVisibility(View.VISIBLE);
            }

            return view;
        }

        public String convertDate(String dateInMilliseconds,String dateFormat) {
            return DateFormat.format(dateFormat, Long.parseLong(dateInMilliseconds)).toString();
        }

    }


    public void onSendClick(View view) {

        input = (EditText) findViewById(R.id.edittext_chatbox);


        if(!(input.getText().toString().trim().length() == 0)){

            messages newSms = new messages(input.getText().toString() ,Long.toString(System.currentTimeMillis()));

            newSms.sending = true;

            newSms.isUserMessage = true;

            messageList.add(newSms);

            adapter.notifyDataSetChanged();

            sendSms(newSms);

            try  { //close keyboard
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            } catch (Exception e) {

            }

            input.setText("");
            if(!cameFromNotification)
                refreshMain();
        }

        else{
            Toast.makeText(this, "Please enter a message body", Toast.LENGTH_LONG).show();
        }


    }

    @Override
    public void onStart() {
        super.onStart();
        conversationInstance = this;
    }

    @Override
    protected void onStop() {
        super.onStop();
        active = false;
    }

    @Override
    public void onBackPressed() {

        if(cameFromNotification){
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.putExtra("cameFromNotification", true);
            startActivity(mainIntent);
            this.finish();
        }
        else
            super.onBackPressed();
    }

    void updateViewsAndDB(boolean status, messages newSms){

        if(status){

            String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this);

            if(defaultSmsApp.equals("mhd3v.filteredsms")){
                Log.d("test", "coming in here");
                ContentValues values = new ContentValues();
                values.put("address", sender);
                values.put("body", newSms.messageBody);
                this.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
            }


            ContentValues cv = new ContentValues();

            Cursor cursor = this.getContentResolver().query(Uri.parse("content://sms"), null, null, null, null);
            cursor.moveToFirst();

            cv.put("thread_id", cursor.getString(cursor.getColumnIndex("thread_id")));
            cv.put("date_string", newSms.time);
            cv.put("type", 2);
            cv.put("address", cursor.getString(cursor.getColumnIndex("address")));
            cv.put("body", cursor.getString(cursor.getColumnIndex("body")));
            cv.put("failed", 0);

            if(senderName.equals(""))
                cv.put("sender_name", "");
            else
                cv.put("sender_name", senderName);

            SQLiteDatabase filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

            filteredDatabase.insert("messageTable", null, cv);

            ContentValues filteredThreadCv = new ContentValues();

            filteredThreadCv.put("date_string", newSms.time);

            int nRowsEffected = filteredDatabase.update("filteredThreads", filteredThreadCv, "thread_id =" + threadId, null); //update time for filtered_threads entry

            if(nRowsEffected == 0){ //if no entry in filteredThreads table

                filteredThreadCv.put("thread_id", threadId);
                filteredThreadCv.put("blacklisted", 0);
                filteredThreadCv.put("filtered_status","filtered");
                filteredDatabase.insert("filteredThreads", null, filteredThreadCv);

            }

            filteredDatabase.close();

            newSms.failed = false;
            newSms.sending = false;

            adapter.notifyDataSetChanged();

        }

        else{

            ContentValues cv = new ContentValues();

            cv.put("thread_id", threadId);
            cv.put("date_string", newSms.time);
            cv.put("type", 2);
            cv.put("address", sender);
            cv.put("body", newSms.messageBody);
            cv.put("failed", 1);

            if(senderName.equals(""))
                cv.put("sender_name", "");
            else
                cv.put("sender_name", senderName);

            SQLiteDatabase filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

            filteredDatabase.insert("messageTable", null, cv);

            filteredDatabase.close();

            newSms.failed = true;
            newSms.sending = false;
            adapter.notifyDataSetChanged();

        }

    }


    private void sendSms(final messages newSms) {
        if (simExists()) {
            try {
                String SENT = "SMS_SENT";

                PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);

                registerReceiver(new BroadcastReceiver() {


                    int i = 0;
                    @Override
                    public void onReceive(Context arg0, Intent arg1) {
                        int resultCode = getResultCode();
                        if(i < pendingIntentCount){
                            switch (resultCode) {

                                case Activity.RESULT_OK:
                                    //Toast.makeText(getBaseContext(), "SMS sent", Toast.LENGTH_LONG).show();
                                    if(i == pendingIntentCount-1){
                                        updateViewsAndDB( true, newSms);
                                        unregisterReceiver(this);
                                    }

                                    i++;
                                    break;

                                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                                    Toast.makeText(getBaseContext(), "Generic failure", Toast.LENGTH_LONG).show();
                                    if(i == pendingIntentCount-1){
                                        updateViewsAndDB( false, newSms);
                                        unregisterReceiver(this);
                                    }

                                    i++;
                                    break;

                                case SmsManager.RESULT_ERROR_NO_SERVICE:
                                    Toast.makeText(getBaseContext(), "No service", Toast.LENGTH_LONG).show();
                                    if(i == pendingIntentCount-1){
                                        updateViewsAndDB( false, newSms);
                                        unregisterReceiver(this);
                                    }
                                    i++;
                                    break;

                                case SmsManager.RESULT_ERROR_NULL_PDU:
                                    Toast.makeText(getBaseContext(), "Null PDU", Toast.LENGTH_LONG).show();
                                    if(i == pendingIntentCount-1){
                                        updateViewsAndDB( false, newSms);
                                        unregisterReceiver(this);
                                    }
                                    i++;
                                    break;

                                case SmsManager.RESULT_ERROR_RADIO_OFF:
                                    Toast.makeText(getBaseContext(), "Radio off", Toast.LENGTH_LONG).show();
                                    if(i == pendingIntentCount-1){
                                        updateViewsAndDB( false, newSms);
                                        unregisterReceiver(this);
                                    }
                                    i++;
                                    break;
                            }
                        }

                    }
                }, new IntentFilter(SENT));

                SmsManager sms = SmsManager.getDefault();

                if(newSms.messageBody.length() <= 160){
                    pendingIntentCount = 1;
                    sms.sendTextMessage(sender, null,newSms.messageBody, sentPI, null);
                }

                else{
                    ArrayList<String> msgsplit=sms.divideMessage(newSms.messageBody);
                    ArrayList<PendingIntent> listOfIntents = new ArrayList<PendingIntent>();

                    pendingIntentCount = msgsplit.size();

                    for (int k=0; k < msgsplit.size(); k++){
                        listOfIntents.add(sentPI);
                    }

                    sms.sendMultipartTextMessage(sender,null,msgsplit, listOfIntents, null);
                }


            } catch (Exception e) {
                Toast.makeText(this, e.getMessage() + "!\n" + "Failed to send SMS", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, SimState + " " + "Cannot send SMS", Toast.LENGTH_LONG).show();
        }
    }




    public boolean simExists()
    {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int SIM_STATE = telephonyManager.getSimState();

        if(SIM_STATE == TelephonyManager.SIM_STATE_READY)
            return true;
        else
        {
            switch(SIM_STATE)
            {
                case TelephonyManager.SIM_STATE_ABSENT: //SimState = "No Sim Found!";
                    break;
                case TelephonyManager.SIM_STATE_NETWORK_LOCKED: //SimState = "Network Locked!";
                    break;
                case TelephonyManager.SIM_STATE_PIN_REQUIRED: //SimState = "PIN Required to access SIM!";
                    break;
                case TelephonyManager.SIM_STATE_PUK_REQUIRED: //SimState = "PUK Required to access SIM!"; // Personal Unblocking Code
                    break;
                case TelephonyManager.SIM_STATE_UNKNOWN: //SimState = "Unknown SIM State!";
                    break;
            }
            return false;
        }
    }


}