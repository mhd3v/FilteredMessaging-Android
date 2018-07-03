package mhd3v.filteredmessaging;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Filter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;



public class MainActivity extends AppCompatActivity{

    private SectionsPageAdapter mSectionsPageAdapter;

    KnownFragment knownInstance;
    UnknownFragment unknownInstance;

    KnownFragment.customAdapter knownAdapter;
    UnknownFragment.customAdapter unknownAdapter;

    ArrayList<SMSThread> knownSms = new ArrayList<>();
    ArrayList<SMSThread> unknownSms = new ArrayList<>();
    ArrayList<SMSThread> SMSThreadList = new ArrayList<>();

    boolean isContact;
    static boolean refreshInbox = false;
    static boolean active = false;
    static MainActivity inst;

    ProgressBar pb;

    SQLiteDatabase filteredDatabase;

    Toolbar tb;

    MenuItem cancelButton;
    MenuItem deleteButton;

    MenuItem selectAllFilteredButton;
    MenuItem selectAllUnfilteredButton;

    boolean deletionMode = false;
    boolean dbUpdated = true;
    boolean cameFromNotification = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        boolean cameFromInitialActivities = getIntent().getBooleanExtra("cameFromFirst", false);

        String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this);

        if(defaultSmsApp.equals("com.mhd3v.filteredmessaging") || cameFromInitialActivities){ //if permission was denied for default SMS app don't ask again for that session
            setContentView(R.layout.activity_main);
            loadActivity();
        }

        else{
            finish();
            startActivity(new Intent(this, DefaultAppActivity.class));
        }

        if(getIntent().getAction() != null){

            if((getIntent().getAction().equals("android.intent.action.SENDTO"))){

                String receiver = getIntent().getData().getSchemeSpecificPart(); // get phone number sent by the intent
                Intent intent = new Intent(this, NewMessage.class);
                intent.putExtra("receiver", receiver);
                startActivity(intent);

            }
        }

    }

    void loadActivity(){

        cameFromNotification = getIntent().getBooleanExtra("cameFromNotification", false);

        //release any held notifications
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.apply();
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        pb = findViewById(R.id.progressBar2);

        loadDatabase();

        if(dbUpdated && !cameFromNotification)
            loadLayout();
    }

    private void loadDatabase(){

        dbUpdated = true;

        createOrOpenDb();

        Cursor threadCursor = filteredDatabase.rawQuery("select * from messageTable;", null);
        Cursor cursor = getContentResolver().query(Uri
                .parse("content://sms"), null, null, null, null);

        Log.d("test1", Integer.toString(threadCursor.getCount()));
        Log.d("test2", Integer.toString(cursor.getCount()));

        if(threadCursor.getCount() == cursor.getCount()){

            if(cursor.getCount() != 0){

                cursor.close();

                threadCursor = filteredDatabase.rawQuery("select thread_id, filtered_status, blacklisted,read from filteredThreads ORDER BY date_string DESC;", null);

                threadCursor.moveToFirst();
                openExistingDatabase(threadCursor);
                filteredDatabase.close();
            }

        }

        else {
            dbUpdated = false;
            this.deleteDatabase("filteredDatabase");
            createOrOpenDb();
            refreshSmsInbox();
        }

    }

    private void loadLayout() {

        if(mSectionsPageAdapter == null){ //this check insures that menu isn't populated once again if rebuildDatabase was called

            mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());

            ViewPager mViewPager = findViewById(R.id.container);
            setupFragments(mViewPager);

            TabLayout tabLayout = findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(mViewPager);

            tb = findViewById(R.id.toolbar);
            tb.setTitle("Filtered Messaging");

            tb.inflateMenu(R.menu.menu_main);

        }

    }

    private void createOrOpenDb() {

        filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

        filteredDatabase.execSQL("CREATE TABLE IF NOT EXISTS messageTable " +
                "(thread_id VARCHAR, address VARCHAR, body VARCHAR, type INT" +
                ", date_string VARCHAR, sender_name VARCHAR, failed INTEGER );");
    }

    private void openExistingDatabase(Cursor cursor) {

        ArrayList<Integer> spamMessagesLenghts = new ArrayList<>();

        do{

            Cursor c = filteredDatabase.rawQuery("select * from messageTable where thread_id =" + cursor.getString(cursor.getColumnIndex("thread_id"))+ " ORDER BY date_string DESC;", null);

            if(c.moveToFirst()){

                if(cursor.getString(cursor.getColumnIndex("filtered_status")).equals("filtered")) {

                    String thread_Id = c.getString(c.getColumnIndex("thread_id"));

                    SMSThread newSMSThread = new SMSThread(c.getString(c.getColumnIndex("address")), thread_Id);

                    newSMSThread.senderName = c.getString(c.getColumnIndex("sender_name"));

                    newSMSThread.read = Integer.parseInt(cursor.getString(cursor.getColumnIndex("read")));


                    do{
                        Message message = new Message(c.getString(c.getColumnIndex("body")), c.getString(c.getColumnIndex("date_string")));

                        if(c.getString(c.getColumnIndex("type")).equals("2")){
                            message.isUserMessage = true;
                        }

                        if(c.getString(c.getColumnIndex("failed")).equals("1"))
                            message.failed = true;

                        newSMSThread.messageList.add(message);
                    }
                    while(c.moveToNext());

                    if((cursor.getInt(cursor.getColumnIndex("blacklisted")) == 0))
                        knownSms.add(newSMSThread);

                    else{
                        newSMSThread.blacklisted = 1;
                        unknownSms.add(newSMSThread);
                    }

                }

                else if(cursor.getString(cursor.getColumnIndex("filtered_status")).equals("unfiltered")){

                    String thread_Id = c.getString(c.getColumnIndex("thread_id"));

                    SMSThread newSMSThread = new SMSThread(c.getString(c.getColumnIndex("address")), thread_Id);
                    newSMSThread.senderName = c.getString(c.getColumnIndex("sender_name"));

                    newSMSThread.read = Integer.parseInt(cursor.getString(cursor.getColumnIndex("read")));

                    do{

                        //filteration algo check
                        Log.d("message", c.getString(c.getColumnIndex("body")).substring(0,3 ).replaceAll(System.getProperty("line.separator"), "") + " | " + Integer.toString(c.getString(c.getColumnIndex("body")).length()));
                        spamMessagesLenghts.add(c.getString(c.getColumnIndex("body")).length());
                        //======================

                        Message message = new Message(c.getString(c.getColumnIndex("body")), c.getString(c.getColumnIndex("date_string")));

                        if(c.getString(c.getColumnIndex("type")).equals("2")){
                            message.isUserMessage = true;
                        }

                        if(c.getString(c.getColumnIndex("failed")).equals("1"))
                            message.failed = true;

                        newSMSThread.messageList.add(message);
                    }
                    while(c.moveToNext());


                    if((cursor.getInt(cursor.getColumnIndex("blacklisted")) == 0))
                        knownSms.add(newSMSThread);

                    else{
                        newSMSThread.blacklisted = 1;
                        unknownSms.add(newSMSThread);
                    }

                    //unknownSms.add(newSms);

                }
            }

            c.close();
        }
        while(cursor.moveToNext());

        float sum = 0;

        for(int i = 0; i < spamMessagesLenghts.size(); i ++){
            sum += spamMessagesLenghts.get(i);
        }

        Log.d("message 1", Integer.toString(spamMessagesLenghts.size()));
        System.out.println("Avg spam message size: " + sum/spamMessagesLenghts.size());

        cursor.close();

    }

    @SuppressLint("StaticFieldLeak")
    public void refreshSmsInbox() {

        new AsyncTask<Void, Void, Void>  (){

            TextView firstRunText;
            FloatingActionButton fab;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                firstRunText = findViewById(R.id.firstRunText);
                fab = findViewById(R.id.floatingActionButton);

                fab.setVisibility(View.GONE);
                firstRunText.setVisibility(View.VISIBLE);
                pb.setVisibility(View.VISIBLE);

            }

            @Override
            protected Void doInBackground(Void... voids) {

                    Cursor cursor = getContentResolver().query(Uri
                            .parse("content://sms"), null, null, null, null);

                    int indexBody = cursor.getColumnIndex("body");
                    int indexAddress = cursor.getColumnIndex("address");
                    if (indexBody < 0 || !cursor.moveToFirst()) return null;

                    String type = Integer.toString(cursor.getColumnIndex("type"));

                    filteredDatabase.execSQL("CREATE TABLE IF NOT EXISTS filteredThreads " +
                            "(thread_id VARCHAR, filtered_status VARCHAR, date_string VARCHAR, blacklisted INTEGER DEFAULT 0, read INTEGER);");

                    do {

                        isContact = false;

                        if (cursor.getString(Integer.parseInt(type)).equalsIgnoreCase("1")) {

                            //received messages

                            boolean found = false;

                            ContentValues cv = new ContentValues();

                            for (int i = 0; i < SMSThreadList.size(); i++) {

                                if (SMSThreadList.get(i).threadId.equals(cursor.getString(cursor.getColumnIndex("thread_id")))) {
                                    String date = cursor.getString(cursor
                                            .getColumnIndex("date"));


                                    int threadId= cursor.getInt(cursor.getColumnIndex("thread_id"));

                                    cv.put("date_string", date);
                                    cv.put("address", cursor.getString(indexAddress));
                                    cv.put("body", cursor.getString(indexBody));
                                    cv.put("thread_id", threadId);
                                    cv.put("type",1);
                                    cv.put("failed",0);

                                    filteredDatabase.insert("messageTable", null ,cv );

                                    SMSThreadList.get(i).addNewSenderMessage(cursor.getString(indexBody), date);
                                    found = true;
                                }

                            }
                            if (!found) {

                                String date = cursor.getString(cursor
                                        .getColumnIndex("date"));

                                SMSThread newSMSThread = new SMSThread(cursor.getString(indexAddress), cursor.getString(cursor.getColumnIndex("thread_id")));
                                newSMSThread.addNewSenderMessage(cursor.getString(indexBody), date);

                                SMSThreadList.add(newSMSThread);

                                int threadId= cursor.getInt(cursor.getColumnIndex("thread_id"));

                                cv.put("date_string", date);
                                cv.put("address", cursor.getString(indexAddress));
                                cv.put("body", cursor.getString(indexBody));
                                cv.put("thread_id", threadId);
                                cv.put("type",1);
                                cv.put("failed",0);

                                String contactName;
                                contactName = getContactName(MainActivity.this, newSMSThread.sender);

                                ContentValues filteredThreadsCv = new ContentValues();

                                if(isContact || Filteration.checkMessage(cursor.getString(indexBody))){

                                    filteredThreadsCv.put("thread_id",threadId);
                                    filteredThreadsCv.put("filtered_status","filtered");
                                    filteredThreadsCv.put("date_string", date);
                                    filteredThreadsCv.put("blacklisted", 0);
                                    filteredThreadsCv.put("read", cursor.getString(cursor.getColumnIndex("read")));
                                    filteredDatabase.insert("filteredThreads", null, filteredThreadsCv);

                                    newSMSThread.senderName = contactName;

                                    knownSms.add(newSMSThread);

                                    if(isContact)
                                        cv.put("sender_name",contactName);
                                    else
                                        cv.put("sender_name","");

                                    filteredDatabase.insertOrThrow("messageTable", null, cv);

                                }
                                else {

                                    filteredThreadsCv.put("thread_id",threadId);
                                    filteredThreadsCv.put("filtered_status","unfiltered");
                                    filteredThreadsCv.put("date_string", date);
                                    filteredThreadsCv.put("blacklisted", 1);
                                    filteredThreadsCv.put("read", cursor.getString(cursor.getColumnIndex("read")));
                                    filteredDatabase.insert("filteredThreads", null, filteredThreadsCv);

                                    newSMSThread.senderName = "";
                                    unknownSms.add(newSMSThread);

                                    newSMSThread.blacklisted = 1;

                                    cv.put("sender_name","");

                                    filteredDatabase.insertOrThrow("messageTable", null, cv);

                                }


                            }

                        }

                        else if (cursor.getString(Integer.parseInt(type)).equalsIgnoreCase("2")) {

                            //sent messages

                            boolean found = false;

                            ContentValues cv = new ContentValues();

                            for (int i = 0; i < SMSThreadList.size(); i++) {

                                if (SMSThreadList.get(i).threadId.equals(cursor.getString(cursor.getColumnIndex("thread_id")))) {

                                    String date = cursor.getString(cursor.getColumnIndex("date"));

                                    int threadId= cursor.getInt(cursor.getColumnIndex("thread_id"));

                                    cv.put("date_string", date);
                                    cv.put("address", cursor.getString(indexAddress));
                                    cv.put("body", cursor.getString(indexBody));
                                    cv.put("thread_id", threadId);
                                    cv.put("type",2);
                                    cv.put("failed",0);

                                    filteredDatabase.insert("messageTable", null, cv );

                                    //update to whitelisted since user replied in this thread
                                    ContentValues newCv = new ContentValues();
                                    newCv.put("blacklisted", 0);
                                    newCv.put("filtered_status","filtered");
                                    filteredDatabase.update("filteredThreads", newCv, "thread_id =" + threadId, null);
                                    //===

                                    SMSThreadList.get(i).addNewUserMessage(cursor.getString(indexBody), date);
                                    found = true;
                                }
                            }

                            if (!found) {
                                String date = cursor.getString(cursor
                                        .getColumnIndex("date"));

                                SMSThread newSMSThread = new SMSThread(cursor.getString(indexAddress),cursor.getString(cursor.getColumnIndex("thread_id")));

                                newSMSThread.addNewUserMessage(cursor.getString(indexBody), date);

                                SMSThreadList.add(newSMSThread);

                                int threadId= cursor.getInt(cursor.getColumnIndex("thread_id"));

                                cv.put("date_string", date);
                                cv.put("address", cursor.getString(cursor.getColumnIndex("address")));
                                cv.put("body", cursor.getString(indexBody));
                                cv.put("thread_id", threadId);
                                cv.put("type",2);
                                cv.put("failed",0);


                                String contactName;
                                contactName = getContactName(MainActivity.this, newSMSThread.sender);

                                ContentValues filteredThreadsCv = new ContentValues();

                                if(isContact){

                                    filteredThreadsCv.put("thread_id",threadId);
                                    filteredThreadsCv.put("filtered_status","filtered");
                                    filteredThreadsCv.put("date_string", date);
                                    filteredThreadsCv.put("blacklisted", 0);
                                    filteredThreadsCv.put("read", cursor.getString(cursor.getColumnIndex("read")));
                                    filteredDatabase.insert("filteredThreads", null, filteredThreadsCv);

                                    cv.put("sender_name",contactName);

                                    filteredDatabase.insertOrThrow("messageTable", null, cv);

                                    newSMSThread.senderName = contactName;
                                    knownSms.add(newSMSThread);
                                }
                                else {

                                    filteredThreadsCv.put("thread_id",threadId);
                                    filteredThreadsCv.put("filtered_status","filtered");
                                    filteredThreadsCv.put("date_string", date);
                                    filteredThreadsCv.put("blacklisted", 0);
                                    filteredThreadsCv.put("read", cursor.getString(cursor.getColumnIndex("read")));
                                    filteredDatabase.insert("filteredThreads", null, filteredThreadsCv);


                                    cv.put("sender_name","");
                                    filteredDatabase.insertOrThrow("messageTable", null, cv);

                                    //newSms.blacklisted = 1;
                                    newSMSThread.senderName = "";
                                    knownSms.add(newSMSThread);
                                }

                            }

                        }

                    } while (cursor.moveToNext());

                    Log.d("result", "done");

                    cursor.close();



                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {

                super.onPostExecute(aVoid);
                filteredDatabase.close();

                pb.setVisibility(View.GONE);
                firstRunText.setVisibility(View.GONE);
                fab.setVisibility(View.VISIBLE);

                knownSms.clear();
                unknownSms.clear();
                SMSThreadList.clear();

                loadDatabase();
                loadLayout();

                knownInstance.knownList.setVisibility(View.VISIBLE);
                unknownInstance.unknownList.setVisibility(View.VISIBLE);

            }
        }.execute();

    }

    void refreshFragments(){

        ArrayList<SMSThread> newKnownList = new ArrayList<>();
        ArrayList<SMSThread> newUnknownList = new ArrayList<>();

        newKnownList.addAll(getKnownSms());
        newUnknownList.addAll(getUnknownSms());

        knownInstance.SMSThreadList.clear();
        knownInstance.SMSThreadList.addAll(newKnownList);
        knownAdapter.notifyDataSetChanged();

        unknownInstance.SMSThreadList.clear();
        unknownInstance.SMSThreadList.addAll(newUnknownList);
        unknownAdapter.notifyDataSetChanged();

        knownInstance.knownList.setVisibility(View.VISIBLE);
        unknownInstance.unknownList.setVisibility(View.VISIBLE);
        pb.setVisibility(View.GONE);

        refreshInbox = false;

    }

    public void setSettingOnClick(MenuItem item) {

        startActivity(new Intent(MainActivity.this, SettingsActivity.class));

    }

    public void rebuildDatabase(MenuItem item) {

        this.deleteDatabase("filteredDatabase");
        knownInstance.knownList.setVisibility(View.GONE);
        unknownInstance.unknownList.setVisibility(View.GONE);
        loadActivity();

    }


    class refreshInboxOnNewThread extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if(knownInstance!=null){
                knownInstance.knownList.setVisibility(View.GONE);
                unknownInstance.unknownList.setVisibility(View.GONE);
                pb.setVisibility(View.VISIBLE);
            }


        }

        @Override
        protected Void doInBackground(Void... params) {

            knownSms.clear();
            SMSThreadList.clear();
            unknownSms.clear();

            filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

            Cursor threadCursor = filteredDatabase.rawQuery("select thread_id, filtered_status, blacklisted, read from filteredThreads ORDER BY date_string DESC;", null);

            threadCursor.moveToFirst();

            openExistingDatabase(threadCursor);

            filteredDatabase.close();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if(cameFromNotification)
                loadLayout();

            refreshFragments();
        }
    }

    void setupFragments(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());

        adapter.addFragment(new KnownFragment(), "Filtered");
        adapter.addFragment(new UnknownFragment(), "Unfiltered");

        viewPager.setAdapter(adapter);
    }

    public String convertDate(String dateInMilliseconds,String dateFormat) {
        return DateFormat.format(dateFormat, Long.parseLong(dateInMilliseconds)).toString();
    }

    public ArrayList<SMSThread> getKnownSms() {

        return knownSms;
    }
    public ArrayList<SMSThread> getUnknownSms() {

        return unknownSms;
    }

    public ArrayList<SMSThread> getSMSThreadList() {

        return SMSThreadList;
    }

    public String getContactName(Context context, String phoneNo) {
        Log.d("phoneNo", phoneNo);
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


    @Override
    public void onStart() {

        super.onStart();
        active = true;
        inst = this;

        if(refreshInbox){
            refreshOnExtraThread();
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }

    public void setKnownInstance(KnownFragment knownInstance) {
        this.knownInstance = knownInstance;
    }

    public void setUnknownInstance(UnknownFragment unknownInstance) {
        this.unknownInstance = unknownInstance;
    }

    public void setKnownAdapter(KnownFragment.customAdapter adapter) {

        knownAdapter = adapter;
    }

    public void setUnknownAdapter(UnknownFragment.customAdapter adapter) {
        unknownAdapter = adapter;
    }

    public void refreshOnExtraThread() {

        new refreshInboxOnNewThread().execute();
    }

    public void fabClicked(View view) {
        Intent intent = new Intent(MainActivity.this, NewMessage.class);
        startActivity(intent);
    }


    public static MainActivity getInstance(){
        return inst;
    }

    void setDeletionMode(){

        selectAllFilteredButton = tb.getMenu().findItem(R.id.selectAllFiltered);
        selectAllUnfilteredButton = tb.getMenu().findItem(R.id.selectAllUnfiltered);

        cancelButton = tb.getMenu().findItem(R.id.cancelButton);
        deleteButton = tb.getMenu().findItem(R.id.deleteButton);

        deletionMode = true;

        unknownInstance.setDeletionModeClickListener();
        knownInstance.setDeletionModeClickListener();

        tb.setTitle("Deletion Mode");

        cancelButton.setVisible(true);
        deleteButton.setVisible(true);

        selectAllFilteredButton.setVisible(true);
        selectAllUnfilteredButton.setVisible(true);

        tb.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelDeletionMode();
            }
        });

        tb.findViewById(R.id.deleteButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(MainActivity.this);

                if (defaultSmsApp.equals("com.mhd3v.filteredmessaging")) {

                    final ArrayList<String> threadIds = new ArrayList<>();

                    filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

                    for (int i = 0; i < knownInstance.threadsToDelete.length; i++) {
                        if (knownInstance.threadsToDelete[i] != null)
                            threadIds.add(knownInstance.threadsToDelete[i]);
                    }

                    for (int i = 0; i < unknownInstance.threadsToDelete.length; i++) {
                        if (unknownInstance.threadsToDelete[i] != null)
                            threadIds.add(unknownInstance.threadsToDelete[i]);
                    }

                    if (threadIds.size() == 0)
                        Toast.makeText(MainActivity.this, "Select some threads to delete!", Toast.LENGTH_SHORT).show();

                    else {

                        String alertMessage;

                        if(threadIds.size() > 1)
                            alertMessage = "Are you sure you want to delete " + threadIds.size() + " conversations?";
                        else
                            alertMessage = "Are you sure you want to delete the selected conversation?";

                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Deleting Conversations")
                                .setMessage(alertMessage)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        for (int i = 0; i < threadIds.size(); i++) {
                                            getContentResolver().delete(Uri.parse("content://sms/conversations/" + threadIds.get(i)), null, null);
                                            filteredDatabase.execSQL("delete from messageTable where thread_id = " + threadIds.get(i) + ";");
                                        }
                                        filteredDatabase.close();
                                        cancelDeletionMode();
                                        refreshOnExtraThread();
                                    }
                                }).setNegativeButton("No", null).show();
                    }
                }

                else{

                    View parentLayout = findViewById(R.id.main_content);
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
        });

    }


    void cancelDeletionMode( ){

        deletionMode = false;

        Arrays.fill(knownInstance.selectedViews, Boolean.FALSE);
        Arrays.fill(unknownInstance.selectedViews, Boolean.FALSE);

        Arrays.fill(knownInstance.threadsToDelete, null);
        Arrays.fill(unknownInstance.threadsToDelete, null);

        knownInstance.setDefaultListener();
        unknownInstance.setDefaultListener();

        knownInstance.knownAdapter.notifyDataSetChanged();
        unknownInstance.unknownAdapter.notifyDataSetChanged();

        cancelButton.setVisible(false);
        deleteButton.setVisible(false);

        selectAllFilteredButton.setVisible(false);
        selectAllUnfilteredButton.setVisible(false);

        tb.setTitle("Filtered Messaging");

    }

    public void selectAllFiltered(MenuItem item) {
        Arrays.fill(knownInstance.selectedViews, Boolean.TRUE);
        knownInstance.threadsToDelete = knownInstance.getAllThreadIds();
        knownInstance.knownAdapter.notifyDataSetChanged();
    }

    public void selectAllUnfiltered(MenuItem item) {
        Arrays.fill(unknownInstance.selectedViews, Boolean.TRUE);
        unknownInstance.threadsToDelete = unknownInstance.getAllThreadIds();
        unknownInstance.unknownAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {

        if(!deletionMode)
            super.onBackPressed();
        else
            cancelDeletionMode();

    }

    public void getSpamMessagesLength(MenuItem mItem){

        ArrayList<Integer> spamMessagesLenghts = new ArrayList<>();

        for(int i = 0; i < unknownSms.size(); i++){
            for(int j = 0; j < unknownSms.get(i).messageList.size(); j++){
                spamMessagesLenghts.add(unknownSms.get(i).messageList.get(j).messageBody.length());
            }
        }

        float sum = 0;

        for (int item : spamMessagesLenghts) {
            sum += item;
        }
        Log.d("message 2", Integer.toString(spamMessagesLenghts.size()));
        System.out.println("Average Spam Message Size" + sum/spamMessagesLenghts.size());
        Toast.makeText(this, "Average Spam Message Length " + sum/spamMessagesLenghts.size(), Toast.LENGTH_SHORT).show();

    }

}