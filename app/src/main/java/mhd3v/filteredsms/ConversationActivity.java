package mhd3v.filteredsms;

import android.Manifest;
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
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
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
import java.util.Arrays;
import java.util.Collections;


public class ConversationActivity extends AppCompatActivity {

    ArrayList<Message> messageList;
    EditText input;

    Toolbar toolbar;

    boolean active;

    String sender;
    String senderName;
    String threadId;

    int blacklisted;
    int read;

    boolean cameFromNotification = false;
    boolean newestMessageSelected = true;

    customAdapter adapter;

    static Intent intent;

    private String SimState = "";

    MenuItem blacklistButton;
    MenuItem whitelistButton;
    MenuItem addToContactsButton;
    MenuItem callButton;
    MenuItem deleteButton;
    MenuItem cancelButton;

    SQLiteDatabase filteredDatabase;

    static ConversationActivity conversationInstance;

    int pendingIntentCount;

    private static final int CALL_PHONE_PERMISSIONS_REQUEST = 3;

    ListView conversation;

    Boolean editMode = false;

    String[] messagesToDelete;

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
                    Message newMessage = new Message(cursor.getString(indexBody), cursor.getString(cursor.getColumnIndex("date")));
                    messageList.add(newMessage);
                } else if (cursor.getString(Integer.parseInt(type)).equalsIgnoreCase("2")) {
                    Message newMessage = new Message(cursor.getString(indexBody), cursor.getString(cursor.getColumnIndex("date")));
                    newMessage.isUserMessage = true;
                    messageList.add(newMessage);
                }
            }
            while (cursor.moveToNext());

        } else {
            Bundle args = intent.getBundleExtra("BUNDLE");
            messageList = (ArrayList<Message>) args.getSerializable("messageList");
        }

        Collections.reverse(messageList);

        messagesToDelete = new String[messageList.size()];

        conversation = findViewById(R.id.conversationList);

        adapter = new customAdapter();

        conversation.setAdapter(adapter);

        toolbar = findViewById(R.id.toolbar);

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

        conversation.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {

                setDeletionMode();

                return false;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.conversation_menu, menu);


        blacklistButton = toolbar.getMenu().findItem(R.id.blacklistbutton);
        whitelistButton = toolbar.getMenu().findItem(R.id.whitelistbutton);
        addToContactsButton = toolbar.getMenu().findItem(R.id.addToContacts);
        deleteButton = toolbar.getMenu().findItem(R.id.deleteButtonConversation);
        cancelButton = toolbar.getMenu().findItem(R.id.cancelButtonConversation);
        callButton = toolbar.getMenu().findItem(R.id.callButton);

        if (blacklisted == 0)
            blacklistButton.setVisible(true);
        else
            whitelistButton.setVisible(true);

        if (senderName.equals(""))
            addToContactsButton.setVisible(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }


    public static void refreshMain() {

        MainActivity.refreshInbox = true;
    }

    public void addToWhiteList(MenuItem item) {

        filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

        ContentValues cv = new ContentValues();

        cv.put("blacklisted", 0);

        filteredDatabase.update("filteredThreads", cv, "thread_id =" + threadId, null);

        Toast.makeText(this, "Added to filtered", Toast.LENGTH_SHORT).show();

        filteredDatabase.close();

        whitelistButton.setVisible(false);
        blacklistButton.setVisible(true);

        refreshMain();

    }

    public void addToBlackList(MenuItem item) {

        filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

        ContentValues cv = new ContentValues();

        cv.put("blacklisted", 1);

        filteredDatabase.update("filteredThreads", cv, "thread_id =" + threadId, null);

        Toast.makeText(this, "Added to unfiltered", Toast.LENGTH_SHORT).show();

        filteredDatabase.close();

        whitelistButton.setVisible(true);
        blacklistButton.setVisible(false);

        refreshMain();

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
                        time = convertDate(messageList.get(i).time,"E dd/MM hh:mm aa");
                        userTimeText.setText(time);
                        userTimeText.setVisibility(View.VISIBLE);
                    }
                    else
                        failedText.setVisibility(View.VISIBLE);

                }

                if(messagesToDelete[i] != null){
                    Drawable backgroundDrawable = userMessage.getBackground();
                    DrawableCompat.setTint(backgroundDrawable, getResources().getColor(R.color.messageSelected));
                }

                else{
                    Drawable backgroundDrawable = userMessage.getBackground();
                    DrawableCompat.setTint(backgroundDrawable, Color.WHITE);

                }


            }
            else{

                TextView senderMessage= view.findViewById(R.id.senderText);
                senderMessage.setText((messageList.get(i).messageBody).trim());
                senderMessage.setVisibility(View.VISIBLE);

                TextView senderTimeText = view.findViewById(R.id.senderTime);
                String time = convertDate(messageList.get(i).time,"E dd/MM - hh:mm aa");
                senderTimeText.setText(time);
                senderTimeText.setVisibility(View.VISIBLE);


                ImageView img = view.findViewById(R.id.image_message_profile);
                img.setVisibility(View.VISIBLE);

                if(messagesToDelete[i] != null){
                    Drawable backgroundDrawable = senderMessage.getBackground();
                    DrawableCompat.setTint(backgroundDrawable, getResources().getColor(R.color.messageSelected));
                }

                else{
                    Drawable backgroundDrawable = senderMessage.getBackground();
                    DrawableCompat.setTint(backgroundDrawable, getResources().getColor(R.color.colorPrimary));

                }
            }

            return view;
        }

        String convertDate(String dateInMilliseconds, String dateFormat) {
            return DateFormat.format(dateFormat, Long.parseLong(dateInMilliseconds)).toString();
        }

    }

    public void onSendClick(View view) {

        input = findViewById(R.id.edittext_chatbox);


        if(!(input.getText().toString().trim().length() == 0)){

            Message newSms = new Message(input.getText().toString() ,Long.toString(System.currentTimeMillis()));

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

        if(editMode){
            cancelDeletionMode();
        }

        else if(cameFromNotification){
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.putExtra("cameFromNotification", true);
            startActivity(mainIntent);
            this.finish();
        }
        else
            super.onBackPressed();
    }

    void updateViewsAndDB(boolean status, Message newSms){

        if(status){

            String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this);

            if(defaultSmsApp.equals("mhd3v.filteredsms")){
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

    private void sendSms(final Message newSms) {
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

    public void callPhone(MenuItem item) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED)
            getPermissionToCallPhone();

        else{
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + sender));

            startActivity(callIntent);
        }


    }

    public void getPermissionToCallPhone() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.CALL_PHONE)) {
                    Toast.makeText(this, "Please allow permission to make phone calls!", Toast.LENGTH_SHORT).show();
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CALL_PHONE},
                        CALL_PHONE_PERMISSIONS_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {


        if (requestCode == CALL_PHONE_PERMISSIONS_REQUEST ) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {

                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + sender));

                startActivity(callIntent);
            }

            else {
                Toast.makeText(this, "Call Phone permission denied", Toast.LENGTH_SHORT).show();
            }

        }

    }

    void setDeletionModeClickListener(){

        conversation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ConstraintLayout cL = view.findViewById(R.id.messageBox);


                if(messagesToDelete[position] == null) {

                    if(messageList.get(position).isUserMessage){
                        //view.findViewById(R.id.userText).setBackgroundColor(Color.LTGRAY);
                        Drawable backgroundDrawable = view.findViewById(R.id.userText).getBackground();
                        DrawableCompat.setTint(backgroundDrawable, getResources().getColor(R.color.messageSelected));

                    }

                    else{
                        Drawable backgroundDrawable = view.findViewById(R.id.senderText).getBackground();
                        DrawableCompat.setTint(backgroundDrawable, getResources().getColor(R.color.messageSelected));
                    }

                    messagesToDelete[position] = messageList.get(position).time;

                    if(position == messageList.size()-1)  //if last index, then last message is being removed
                        newestMessageSelected = true;

                }

                else{

                    if(messageList.get(position).isUserMessage){
                        Drawable backgroundDrawable = view.findViewById(R.id.userText).getBackground();
                        DrawableCompat.setTint(backgroundDrawable, Color.WHITE);
                    }
                    else{
                        Drawable backgroundDrawable = view.findViewById(R.id.senderText).getBackground();
                        DrawableCompat.setTint(backgroundDrawable, getResources().getColor(R.color.colorPrimary));
                    }

                    messagesToDelete[position] = null;

                    if(position == messageList.size()-1)
                        newestMessageSelected = false;
                }

            }
        });
    }

    void setDeletionMode(){

        editMode = true;

        setDeletionModeClickListener();

        getSupportActionBar().setTitle("Edit Mode");

        callButton.setVisible(false);
        deleteButton.setVisible(true);
        cancelButton.setVisible(true);

        setDeletionModeClickListener();

        cancelButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                cancelDeletionMode();

                return false;
            }
        });

        deleteButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                deleteMessages();

                return false;
            }
        });
    }

    void cancelDeletionMode() {

        editMode = false;

        callButton.setVisible(true);
        deleteButton.setVisible(false);
        cancelButton.setVisible(false);
        editMode = false;

        Arrays.fill(messagesToDelete, null);

        adapter.notifyDataSetChanged();

        conversation.setOnItemClickListener(null);

        if (senderName.equals(""))
            getSupportActionBar().setTitle(sender);
        else
            getSupportActionBar().setTitle(senderName);

    }

    void deleteMessages(){

        String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(ConversationActivity.this);


        if (defaultSmsApp.equals("mhd3v.filteredsms")){

            filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

            final ArrayList<String> selectedMessagesList = new ArrayList<>();
            final ArrayList<Integer> markedPositionsList = new ArrayList<>();

            for(int i = 0; i < messagesToDelete.length; i++){
                if(messagesToDelete[i] != null){
                    selectedMessagesList.add(messagesToDelete[i]);
                    markedPositionsList.add(i);
                }
            }

            if(selectedMessagesList.size()== 0)
                Toast.makeText(conversationInstance, "Select some messages!", Toast.LENGTH_SHORT).show();

            else{

                new AlertDialog.Builder(ConversationActivity.this)
                        .setTitle("Deleting Messages")
                        .setMessage("Are you sure you want to delete "+ selectedMessagesList.size() + " messages(s)?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                for(int i = 0; i < selectedMessagesList.size(); i++ ) {
                                    getContentResolver().delete(Uri.parse("content://sms"), "thread_id =" + threadId + " and date= " + selectedMessagesList.get(i), null); //android db

                                    filteredDatabase.execSQL("delete from messageTable where thread_id = " + threadId + " and date_string=" + selectedMessagesList.get(i) + ";"); //messageTable

                                    messageList.remove(markedPositionsList.get(i) - i); //index needs to be decreased each time an item is deleted since size of messageList decreases
                                }

                                if(newestMessageSelected){ //if the newest message is selected then we have to update attributes for new newest message

                                    Cursor c = filteredDatabase.rawQuery("select * from messageTable where thread_id = " + threadId + " order by date_string", null);

                                    if(c.moveToLast()){ //newest message will have the largest time string val

                                        ContentValues filteredThreadsCv = new ContentValues();
                                        ContentValues messageTableCv = new ContentValues();

                                        //updating the sender_name table, since only it is used to setup the fragments
                                        if(senderName.equals(""))
                                            messageTableCv.put("sender_name", "");
                                        else
                                            messageTableCv.put("sender_name", senderName);

                                        filteredThreadsCv.put("date_string", c.getString(c.getColumnIndex("date_string"))); //updating filteredThreads table entry

                                        filteredDatabase.update("filteredThreads", filteredThreadsCv,  "thread_id =" + threadId, null);

                                        filteredDatabase.update("messageTable", messageTableCv,  "thread_id =" + threadId + " and date_string ="
                                                + c.getString(c.getColumnIndex("date_string")), null); //adding sender_name to new newest message
                                    }

                                    else{
                                        filteredDatabase.delete("filteredThreads", "thread_id =" +threadId, null); //all messages delete. Remove thread entry from filtered threads
                                        finish(); //all messages in the thread deleted
                                    }

                                    c.close();
                                }

                                filteredDatabase.close();

                                cancelDeletionMode();

                                refreshMain();
                            }

                        }).setNegativeButton("No", null).show();

            }


        }

        else{

            View parentLayout = findViewById(R.id.messageBox);
            final int DEF_SMS_REQ = 0;

            Snackbar.make(parentLayout, "Set as default app to delete messages!", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Change", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
                            startActivityForResult(intent, DEF_SMS_REQ);
                        }
                    }).show();
        }

    }

}