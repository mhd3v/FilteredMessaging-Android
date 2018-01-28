package mhd3v.filteredsms;

import android.Manifest;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity{

    private SectionsPageAdapter mSectionsPageAdapter;

    Tab1Fragment knownInstance;
    Tab2Fragment unknownInstance;

    Tab1Fragment.customAdapter knownAdapter;
    Tab2Fragment.customAdapter unknownAdapter;

    private ViewPager mViewPager;

    ArrayList<sms> knownSms = new ArrayList<>();
    ArrayList<sms> unknownSms = new ArrayList<>();
    ArrayList<sms> smsList = new ArrayList<>();

    boolean isContact;
    static boolean refreshInbox = false;
    static boolean active = false;
    static MainActivity inst;

    ProgressBar pb;

    private static final int READ_SMS_PERMISSIONS_REQUEST = 1;
    private static final int READ_CONTACTS_PERMISSIONS_REQUEST = 2;

    SQLiteDatabase filteredDatabase;

    Toolbar tb;

    MenuItem cancelButtonFiltered;
    MenuItem cancelButtonUnfiltered;
    MenuItem cancelButton;
    MenuItem deleteButton;

    boolean deletionMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED)
            getPermissionToReadSMS();


        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED)
            getPermissionToReadContacts();



        else {
            this.deleteDatabase("filteredDatabase");

            createOrOpenDb();


            Cursor threadCursor = filteredDatabase.rawQuery("select * from messageTable;", null);

            if(threadCursor.moveToFirst()){

                threadCursor = filteredDatabase.rawQuery("select DISTINCT thread_id, filtered_status from (select thread_id, filtered_status, date_string " +
                        "from filteredThreads ORDER BY date_string DESC) " +
                        "ORDER BY date_string DESC;", null);

                threadCursor.moveToFirst();
                openExistingDatabase(threadCursor);
            }

            else
                refreshSmsInbox();

            filteredDatabase.close();
        }

        loadLayout();

    }

    private void loadLayout() {

        pb = (ProgressBar) findViewById(R.id.progressBar2);

        //release any held notifications
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.apply();
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager)findViewById(R.id.container);
        setupFragments(mViewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        tb = (Toolbar) findViewById(R.id.toolbar);
        tb.setTitle("Filtered Messaging");

        tb.inflateMenu(R.menu.menu_main);
    }

    private void createOrOpenDb() {

        filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

        filteredDatabase.execSQL("CREATE TABLE IF NOT EXISTS messageTable " +
                "(thread_id VARCHAR, address VARCHAR, body VARCHAR, type INT" +
                ", date VARCHAR, date_string VARCHAR, sender VARCHAR, sender_name VARCHAR );");
    }

    private void openExistingDatabase(Cursor cursor) {

        do{

            //Log.d("thread_id", cursor.getString(cursor.getColumnIndex("thread_id")));

            Cursor c = filteredDatabase.rawQuery("select * from messageTable where thread_id =" + cursor.getString(cursor.getColumnIndex("thread_id"))+ " ORDER BY date_string DESC;", null);

            if(c.moveToFirst()){

                if(cursor.getString(cursor.getColumnIndex("filtered_status")).equals("filtered")) {

                    String thread_Id = c.getString(c.getColumnIndex("thread_id"));

                    sms newSms = new sms(c.getString(c.getColumnIndex("address")), thread_Id);

                    newSms.senderName = c.getString(c.getColumnIndex("sender_name"));


                    do{
                        messages message = new messages(c.getString(c.getColumnIndex("body")), c.getString(c.getColumnIndex("date_string")));

                        if(c.getString(c.getColumnIndex("type")).equals("2")){
                            message.isUserMessage = true;
                        }

                        newSms.messages.add(message);
                    }
                    while(c.moveToNext());

                    knownSms.add(newSms);


                }

                else if(cursor.getString(cursor.getColumnIndex("filtered_status")).equals("unfiltered")){

                    String thread_Id = c.getString(c.getColumnIndex("thread_id"));

                    sms newSms = new sms(c.getString(c.getColumnIndex("address")), thread_Id);
                    newSms.senderName = c.getString(c.getColumnIndex("sender_name"));

                    do{

                        messages message = new messages(c.getString(c.getColumnIndex("body")), c.getString(c.getColumnIndex("date_string")));

                        if(c.getString(c.getColumnIndex("type")).equals("2")){
                            message.isUserMessage = true;
                        }

                        newSms.messages.add(message);
                    }
                    while(c.moveToNext());


                    unknownSms.add(newSms);

                }
            }

            c.close();
        }
        while(cursor.moveToNext());
        cursor.close();

    }

    public void refreshSmsInbox() {

        Cursor cursor = getContentResolver().query(Uri
                .parse("content://sms"), null, null, null, null);

        int indexBody = cursor.getColumnIndex("body");
        int indexAddress = cursor.getColumnIndex("address");
        if (indexBody < 0 || !cursor.moveToFirst()) return;

        String type = Integer.toString(cursor.getColumnIndex("type"));

        do {

            isContact = false;

            if (cursor.getString(Integer.parseInt(type)).equalsIgnoreCase("1")) {

                //received messages

                boolean found = false;

                ContentValues cv = new ContentValues();

                for (int i = 0; i < smsList.size(); i++) {

                    if (smsList.get(i).threadId.equals(cursor.getString(cursor.getColumnIndex("thread_id")))) {
                        String date = cursor.getString(cursor
                                .getColumnIndex("date"));

                        String dateTime = convertDate(date,"yyyy/MM/dd hh:mm:ss");


                        int threadId= cursor.getInt(cursor.getColumnIndex("thread_id"));

                        if(smsList.get(i).knownThread)
                            cv.put("sender", "known");
                        else
                            cv.put("sender", "unknown");

                        cv.put("date", dateTime);
                        cv.put("date_string", date);
                        cv.put("address", cursor.getString(indexAddress));
                        cv.put("body", cursor.getString(indexBody));
                        cv.put("thread_id", threadId);
                        cv.put("type",1);

                        filteredDatabase.insert("messageTable", null ,cv );

                        smsList.get(i).addNewSenderMessage(cursor.getString(indexBody), date);
                        found = true;
                    }

                }
                if (!found) {

                    String date = cursor.getString(cursor
                            .getColumnIndex("date"));
                    String dateTime = convertDate(date,"yyyy/MM/dd hh:mm:ss");

                    sms newSms = new sms(cursor.getString(indexAddress), cursor.getString(cursor.getColumnIndex("thread_id")));
                    newSms.addNewSenderMessage(cursor.getString(indexBody), date);

                    smsList.add(newSms);

                    int threadId= cursor.getInt(cursor.getColumnIndex("thread_id"));

                    cv.put("date", dateTime);
                    cv.put("date_string", date);
                    cv.put("address", cursor.getString(indexAddress));
                    cv.put("body", cursor.getString(indexBody));
                    cv.put("thread_id", threadId);
                    cv.put("type",1);


                    String contactName;
                    contactName = getContactName(this, newSms.sender);

                    //SQLiteDatabase filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

                    filteredDatabase.execSQL("CREATE TABLE IF NOT EXISTS filteredThreads " +
                            "(thread_id VARCHAR, filtered_status VARCHAR, date_string VARCHAR);");

                    ContentValues filteredThreadsCv = new ContentValues();

                    if(isContact){

                        filteredThreadsCv.put("thread_id",threadId);
                        filteredThreadsCv.put("filtered_status","filtered");
                        filteredThreadsCv.put("date_string", date);
                        filteredDatabase.insert("filteredThreads", null, filteredThreadsCv);

                        newSms.senderName = contactName;
                        newSms.knownThread = true;
                        knownSms.add(newSms);

                        cv.put("sender", "known");
                        cv.put("sender_name",contactName);

                        filteredDatabase.insertOrThrow("messageTable", null, cv);

                    }
                    else {

                        filteredThreadsCv.put("thread_id",threadId);
                        filteredThreadsCv.put("filtered_status","unfiltered");
                        filteredThreadsCv.put("date_string", date);
                        filteredDatabase.insert("filteredThreads", null, filteredThreadsCv);

                        unknownSms.add(newSms);

                        cv.put("sender", "unknown");
                        cv.put("sender_name","");

                        filteredDatabase.insertOrThrow("messageTable", null, cv);

                    }


                }

            }

            else if (cursor.getString(Integer.parseInt(type)).equalsIgnoreCase("2")) {

                //sent messages

                boolean found = false;

                ContentValues cv = new ContentValues();

                for (int i = 0; i < smsList.size(); i++) {

                    if (smsList.get(i).threadId.equals(cursor.getString(cursor.getColumnIndex("thread_id")))) {

                        String date = cursor.getString(cursor.getColumnIndex("date"));
                        String dateTime = convertDate(date,"yyyy/MM/dd hh:mm:ss");

                        int threadId= cursor.getInt(cursor.getColumnIndex("thread_id"));

                        if(smsList.get(i).knownThread)
                            cv.put("sender", "known");
                        else
                            cv.put("sender", "unknown");

                        cv.put("date", dateTime);
                        cv.put("date_string", date);
                        cv.put("address", cursor.getString(indexAddress));
                        cv.put("body", cursor.getString(indexBody));
                        cv.put("thread_id", threadId);
                        cv.put("type",2);

                        filteredDatabase.insert("messageTable", null, cv );

                        smsList.get(i).addNewUserMessage(cursor.getString(indexBody), date);
                        found = true;
                    }
                }

                if (!found) {
                    String date = cursor.getString(cursor
                            .getColumnIndex("date"));

                    String dateTime = convertDate(date,"yyyy/MM/dd hh:mm:ss");

                    sms newSms = new sms(cursor.getString(indexAddress),cursor.getString(cursor.getColumnIndex("thread_id")));

                    newSms.addNewUserMessage(cursor.getString(indexBody), date);

                    smsList.add(newSms);

                    int threadId= cursor.getInt(cursor.getColumnIndex("thread_id"));

                    cv.put("date", dateTime);
                    cv.put("date_string", date);
                    cv.put("address", cursor.getString(cursor.getColumnIndex("address")));
                    cv.put("body", cursor.getString(indexBody));
                    cv.put("thread_id", threadId);
                    cv.put("type",2);


                    String contactName;
                    contactName = getContactName(this, newSms.sender);

                    filteredDatabase.execSQL("CREATE TABLE IF NOT EXISTS filteredThreads " +
                            "(thread_id VARCHAR, filtered_status VARCHAR, date_string VARCHAR);");

                    ContentValues filteredThreadsCv = new ContentValues();

                    if(isContact){

                        filteredThreadsCv.put("thread_id",threadId);
                        filteredThreadsCv.put("filtered_status","filtered");
                        filteredThreadsCv.put("date_string", date);
                        filteredDatabase.insert("filteredThreads", null, filteredThreadsCv);

                        cv.put("sender", "known");
                        cv.put("sender_name",contactName);
                        newSms.knownThread = true;
                        filteredDatabase.insertOrThrow("messageTable", null, cv);

                        newSms.senderName = contactName;
                        knownSms.add(newSms);
                    }
                    else {

                        filteredThreadsCv.put("thread_id",threadId);
                        filteredThreadsCv.put("filtered_status","unfiltered");
                        filteredThreadsCv.put("date_string", date);
                        filteredDatabase.insert("filteredThreads", null, filteredThreadsCv);

                        cv.put("sender", "unknown");
                        cv.put("sender_name","");
                        filteredDatabase.insertOrThrow("messageTable", null, cv);

                        unknownSms.add(newSms);
                    }

                }

            }

        } while (cursor.moveToNext());

        cursor.close();

    }


    void refreshFragments(){

        ArrayList<sms> newKnownList = new ArrayList<>();
        ArrayList<sms> newUnknownList = new ArrayList<>();

        newKnownList.addAll(getKnownSms());
        newUnknownList.addAll(getUnknownSms());

        knownInstance.smsList.clear();
        knownInstance.smsList.addAll(newKnownList);
        knownAdapter.notifyDataSetChanged();

        unknownInstance.smsList.clear();
        unknownInstance.smsList.addAll(newUnknownList);
        unknownAdapter.notifyDataSetChanged();

        knownInstance.knownList.setVisibility(View.VISIBLE);
        unknownInstance.unknownList.setVisibility(View.VISIBLE);
        pb.setVisibility(View.GONE);

        refreshInbox = false;

    }

    public void setSettingOnClick(MenuItem item) {

        startActivity(new Intent(MainActivity.this, SettingsActivity.class));

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
            smsList.clear();
            unknownSms.clear();

            filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

            Cursor threadCursor = filteredDatabase.rawQuery("select DISTINCT thread_id, filtered_status from (select thread_id, filtered_status, date_string " +
                    "from filteredThreads ORDER BY date_string DESC) " +
                    "ORDER BY date_string DESC;", null);


            threadCursor.moveToFirst();

            openExistingDatabase(threadCursor);


            filteredDatabase.close();

            return null;

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            refreshFragments();

        }
    }

    void setupFragments(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());

        adapter.addFragment(new Tab1Fragment(), "Filtered");
        adapter.addFragment(new Tab2Fragment(), "Unfiltered");

        viewPager.setAdapter(adapter);
    }


    public void getPermissionToReadSMS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.READ_SMS)) {
                    Toast.makeText(this, "Please allow permission!", Toast.LENGTH_SHORT).show();
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_SMS},
                        READ_SMS_PERMISSIONS_REQUEST);
            }
        }

        else
            getPermissionToReadContacts();


    }

    public void getPermissionToReadContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.READ_CONTACTS)) {
                    Toast.makeText(this, "Please allow permission!", Toast.LENGTH_SHORT).show();
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                        READ_CONTACTS_PERMISSIONS_REQUEST);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        // Make sure it's our original READ_CONTACTS request
        if (requestCode == READ_SMS_PERMISSIONS_REQUEST) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED ) {

                if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED)
                    getPermissionToReadContacts();
                else{
                    createOrOpenDb();
                    refreshSmsInbox();
                    loadLayout();

                }


            }

            else {
                Toast.makeText(this, "Read SMS permission denied", Toast.LENGTH_SHORT).show();
            }

        }

        if (requestCode == READ_CONTACTS_PERMISSIONS_REQUEST ) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED ) {

                if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                        != PackageManager.PERMISSION_GRANTED)
                    getPermissionToReadSMS();
                else{
                    createOrOpenDb();
                    refreshSmsInbox();
                    loadLayout();
                }

            }

            else {
                Toast.makeText(this, "Read Contacts permission denied", Toast.LENGTH_SHORT).show();
            }

        }

    }

    public String convertDate(String dateInMilliseconds,String dateFormat) {
        return DateFormat.format(dateFormat, Long.parseLong(dateInMilliseconds)).toString();
    }

    public ArrayList<sms> getKnownSms() {

        return knownSms;
    }
    public ArrayList<sms> getUnknownSms() {

        return unknownSms;
    }

    public ArrayList<sms> getSmsList() {

        return smsList;
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

    public void setKnownInstance(Tab1Fragment knownInstance) {
        this.knownInstance = knownInstance;
    }

    public void setUnknownInstance(Tab2Fragment unknownInstance) {
        this.unknownInstance = unknownInstance;
    }

    public void setKnownAdapter(Tab1Fragment.customAdapter adapter) {

        knownAdapter = adapter;
    }

    public void setUnknownAdapter(Tab2Fragment.customAdapter adapter) {
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

        cancelButton = tb.getMenu().findItem(R.id.cancelButton);
        deleteButton = tb.getMenu().findItem(R.id.deleteButton);

        deletionMode = true;

        unknownInstance.setDeletionModeClickListener();
        knownInstance.setDeletionModeClickListener();

        tb.setTitle("Deletion Mode");
        cancelButton.setVisible(true);
        deleteButton.setVisible(true);

        tb.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
                tb.setTitle("Filtered Messaging");

            }
        });

        tb.findViewById(R.id.deleteButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(MainActivity.this);

                if (defaultSmsApp.equals("mhd3v.filteredsms")){

                    final ArrayList<String> threadIds = new ArrayList<String>();

                    filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

                    for(int i=0; i< knownInstance.threadsToDelete.length; i++){
                        if(knownInstance.threadsToDelete[i] != null)
                            threadIds.add(knownInstance.threadsToDelete[i]);
                    }

                    for(int i=0; i< unknownInstance.threadsToDelete.length; i++){
                        if(unknownInstance.threadsToDelete[i] != null)
                            threadIds.add(unknownInstance.threadsToDelete[i]);
                    }

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Deleting Conversations")
                            .setMessage("Are you sure you want to delete "+ threadIds.size() + " conversation(s)?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    for(int i =0; i<threadIds.size(); i++ ){
                                        getContentResolver().delete(Uri.parse("content://sms/conversations/" + threadIds.get(i)),null,null);
                                        filteredDatabase.execSQL("delete from messageTable where thread_id = " + threadIds.get(i)+ ";");
                                    }
                                    filteredDatabase.close();
                                    cancelDeletionMode();
                                    refreshOnExtraThread();
                                }
                            }).setNegativeButton("No", null).show();
                }

                else
                    Toast.makeText(MainActivity.this, "Set as default app to delete messages!", Toast.LENGTH_SHORT).show();

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
        tb.setTitle("Filtered Messaging");


    }




}