package mhd3v.filteredsms;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static android.content.Intent.ACTION_VIEW;

public class CoversationActivity extends AppCompatActivity {

    ArrayList<messages> messageList;
    EditText input;

    SmsManager smsManager;

    static boolean active = false;

    String sender;
    String senderName;
    String threadId;

    boolean cameFromNotification = false;

    customAdapter adapter;
    static Intent intent;

    private String SimState = "";


    static CoversationActivity conversationInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coversation);

        messageList = new ArrayList<>();

        intent = getIntent();

        sender = intent.getStringExtra("sender");
        senderName = intent.getStringExtra("senderName");
        threadId = intent.getStringExtra("threadId");

        if(intent.getAction().equals("android.intent.action.NotificationClicked")){

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sp.edit();

            editor.remove(threadId);
            editor.apply();

            NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.cancel(Integer.parseInt(threadId));

            cameFromNotification = true;

            Cursor cursor = getContentResolver().query(Uri.parse("content://sms/"), null,  "thread_id=" + intent.getStringExtra("threadId"), null, null);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            cursor.moveToFirst();

                int indexBody = cursor.getColumnIndex("body");
                String type = Integer.toString(cursor.getColumnIndex("type"));

                do{

                    if (cursor.getString(Integer.parseInt(type)).equalsIgnoreCase("1")) {
                        //received messages
                        messages newMessage = new messages(cursor.getString(indexBody) ,cursor.getString(cursor.getColumnIndex("date")));
                        messageList.add(newMessage);
                    }

                    else if (cursor.getString(Integer.parseInt(type)).equalsIgnoreCase("2")) {
                        messages newMessage = new messages(cursor.getString(indexBody) ,cursor.getString(cursor.getColumnIndex("date")));
                        newMessage.isUserMessage = true;
                        messageList.add(newMessage);
                    }
                }
                while (cursor.moveToNext());

            }

        else{
            Bundle args = intent.getBundleExtra("BUNDLE");
            messageList = (ArrayList<messages>) args.getSerializable("messageList");
        }

        Collections.reverse(messageList);

        ListView conversation = (ListView) findViewById(R.id.conversationList);
        adapter = new customAdapter();

        conversation.setAdapter(adapter);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(toolbar != null) {
            setSupportActionBar(toolbar);

            if(senderName.equals(""))
                getSupportActionBar().setTitle(sender);
            else
            getSupportActionBar().setTitle(senderName);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                finish();
            }
        });

    }



    public static void refreshMain() {
        MainActivity mainInstance  = MainActivity.getInstance();
        mainInstance.refreshInbox = true;
    }



    class customAdapter extends BaseAdapter {


        @Override
        public int getCount() {
            return messageList.size() ;
        }

        public void updateMessageList(ArrayList<messages> newlist) {
            messageList.clear();
            messageList.addAll(newlist);
            this.notifyDataSetChanged();
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

                String time;

                if(!messageList.get(i).time.equals("Sending..."))
                    time = convertDate(messageList.get(i).time,"dd/MM hh:mm aa");
                else
                    time = "Sending...";
                userTimeText.setText(time);
                userTimeText.setVisibility(View.VISIBLE);


            }
            else{

                TextView senderMessage= view.findViewById(R.id.senderText);
                senderMessage.setText(messageList.get(i).messageBody);
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

            smsManager = SmsManager.getDefault();

            input = (EditText) findViewById(R.id.edittext_chatbox);


            if(!(input.getText().toString().trim().length() == 0)){

                messages newSms = new messages(input.getText().toString() ,"Sending...");

                newSms.isUserMessage = true;

                ArrayList<messages> newMessageList = new ArrayList<>();

                newMessageList.addAll(messageList);

                newMessageList.add(newSms);

                adapter.updateMessageList(newMessageList);

                sendSms(sender,input.getText().toString(), newSms);

                try  { //close keyboard
                    InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                } catch (Exception e) {

                }

                input.setText("");
                refreshMain();
            }

            else{
                Toast.makeText(this, "Please enter a message body", Toast.LENGTH_LONG).show();
            }



    }

    @Override
    public void onStart() {
        super.onStart();
        active = true;
        conversationInstance = this;
    }

    @Override
    public void onStop() {
        super.onStop();
        conversationInstance = null;

        if(cameFromNotification){
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

    }

    void updateViewsAndDB(String address, String message, String time, messages newSms, boolean status){

        if(status){

            String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this);

            if(defaultSmsApp.equals("mhd3v.filteredsms")){
                ContentValues values = new ContentValues();
                values.put("address", address);
                values.put("body", message);
                this.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
            }


            ArrayList<messages> newMessageList = new ArrayList<>();
            newMessageList.addAll(messageList);
            newMessageList.get(newMessageList.size()-1).time = time;

            adapter.updateMessageList(newMessageList);

        }

        else{

            ArrayList<messages> newMessageList = new ArrayList<>();
            newMessageList.addAll(messageList);
            newMessageList.remove(newMessageList.size()-1);

            adapter.updateMessageList(newMessageList);
        }

    }


    private void sendSms(final String address, final String message, final messages newSms) {
        if (simExists()) {
            try {
                String SENT = "SMS_SENT";

                PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);

                registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context arg0, Intent arg1) {
                        int resultCode = getResultCode();
                        switch (resultCode) {
                            case Activity.RESULT_OK:
                                Toast.makeText(getBaseContext(), "SMS sent", Toast.LENGTH_LONG).show();
                                updateViewsAndDB(address, message, Long.toString(System.currentTimeMillis()), newSms, true);
                                break;
                            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                                Toast.makeText(getBaseContext(), "Generic failure", Toast.LENGTH_LONG).show();
                                updateViewsAndDB(address, message, Long.toString(System.currentTimeMillis()), newSms, false);
                                break;
                            case SmsManager.RESULT_ERROR_NO_SERVICE:
                                Toast.makeText(getBaseContext(), "No service", Toast.LENGTH_LONG).show();
                                updateViewsAndDB(address, message, Long.toString(System.currentTimeMillis()), newSms, false);
                                break;
                            case SmsManager.RESULT_ERROR_NULL_PDU:
                                Toast.makeText(getBaseContext(), "Null PDU", Toast.LENGTH_LONG).show();
                                updateViewsAndDB(address, message, Long.toString(System.currentTimeMillis()), newSms, false);
                                break;
                            case SmsManager.RESULT_ERROR_RADIO_OFF:
                                Toast.makeText(getBaseContext(), "Radio off", Toast.LENGTH_LONG).show();
                                updateViewsAndDB(address, message, Long.toString(System.currentTimeMillis()), newSms, false);
                                break;
                        }
                    }
                }, new IntentFilter(SENT));

                SmsManager smsMgr = SmsManager.getDefault();
                smsMgr.sendTextMessage(address, null, message, sentPI, null);

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
