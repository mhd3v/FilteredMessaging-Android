package mhd3v.filteredsms;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.support.design.widget.FloatingActionButton;
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
    private static final int CALL_PHONE_PERMISSIONS_REQUEST = 3;

    SQLiteDatabase filteredDatabase;

    Toolbar tb;

    MenuItem cancelButton;
    MenuItem deleteButton;

    MenuItem selectAllFiltered;
    MenuItem selectAllUnfiltered;

    boolean deletionMode = false;
    boolean firstRun = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pb = (ProgressBar) findViewById(R.id.progressBar2);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED)
            getPermissionToReadSMS();

        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED)
            getPermissionToReadContacts();

        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED)
            getPermissionToCallPhone();

        else
            loadDatabase();

        if(!firstRun)
            loadLayout();

    }

    private void loadDatabase(){

        firstRun = false;

        createOrOpenDb();

        Cursor threadCursor = filteredDatabase.rawQuery("select * from messageTable;", null);

        if(threadCursor.moveToFirst()){

//            Cursor c = filteredDatabase.rawQuery("SELECT distinct thread_id FROM filteredThreads",null);
//            Log.d("filtered_threads", Integer.toString(c.getCount()));

            threadCursor = filteredDatabase.rawQuery("select thread_id, filtered_status, blacklisted,read from filteredThreads ORDER BY date_string DESC;", null);

            threadCursor.moveToFirst();
            openExistingDatabase(threadCursor);
            filteredDatabase.close();
        }

        else {
            firstRun = true;
            refreshSmsInbox();
        }

    }

    private void loadLayout() {
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

            Cursor c = filteredDatabase.rawQuery("select * from messageTable where thread_id =" + cursor.getString(cursor.getColumnIndex("thread_id"))+ " ORDER BY date_string DESC;", null);

            if(c.moveToFirst()){

                if(cursor.getString(cursor.getColumnIndex("filtered_status")).equals("filtered")) {

                    String thread_Id = c.getString(c.getColumnIndex("thread_id"));

                    sms newSms = new sms(c.getString(c.getColumnIndex("address")), thread_Id);

                    newSms.senderName = c.getString(c.getColumnIndex("sender_name"));

                    newSms.read = Integer.parseInt(cursor.getString(cursor.getColumnIndex("read")));

                    do{
                        messages message = new messages(c.getString(c.getColumnIndex("body")), c.getString(c.getColumnIndex("date_string")));

                        if(c.getString(c.getColumnIndex("type")).equals("2")){
                            message.isUserMessage = true;
                        }

                        newSms.messages.add(message);
                    }
                    while(c.moveToNext());

                    if((cursor.getInt(cursor.getColumnIndex("blacklisted")) == 0))
                        knownSms.add(newSms);

                    else{
                        newSms.blacklisted = 1;
                        unknownSms.add(newSms);
                    }

                }

                else if(cursor.getString(cursor.getColumnIndex("filtered_status")).equals("unfiltered")){

                    String thread_Id = c.getString(c.getColumnIndex("thread_id"));

                    sms newSms = new sms(c.getString(c.getColumnIndex("address")), thread_Id);
                    newSms.senderName = c.getString(c.getColumnIndex("sender_name"));

                    newSms.read = Integer.parseInt(cursor.getString(cursor.getColumnIndex("read")));

                    do{

                        messages message = new messages(c.getString(c.getColumnIndex("body")), c.getString(c.getColumnIndex("date_string")));

                        if(c.getString(c.getColumnIndex("type")).equals("2")){
                            message.isUserMessage = true;
                        }

                        newSms.messages.add(message);
                    }
                    while(c.moveToNext());


                    if((cursor.getInt(cursor.getColumnIndex("blacklisted")) == 0))
                        knownSms.add(newSms);

                    else{
                        newSms.blacklisted = 1;
                        unknownSms.add(newSms);
                    }

                    //unknownSms.add(newSms);

                }
            }

            c.close();
        }
        while(cursor.moveToNext());
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

                firstRunText = (TextView) findViewById(R.id.firstRunText);
                fab = (FloatingActionButton) findViewById(R.id.floatingActionButton);

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
                            contactName = getContactName(MainActivity.this, newSms.sender);

                            ContentValues filteredThreadsCv = new ContentValues();

                            if(isContact){

                                filteredThreadsCv.put("thread_id",threadId);
                                filteredThreadsCv.put("filtered_status","filtered");
                                filteredThreadsCv.put("date_string", date);
                                filteredThreadsCv.put("blacklisted", 0);
                                //System.out.println("readstatus" + cursor.getString(cursor.getColumnIndex("read")));
                                filteredThreadsCv.put("read", cursor.getString(cursor.getColumnIndex("read")));
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
                                filteredThreadsCv.put("blacklisted", 1);
                                filteredThreadsCv.put("read", cursor.getString(cursor.getColumnIndex("read")));
                                filteredDatabase.insert("filteredThreads", null, filteredThreadsCv);

                                newSms.senderName = "";
                                unknownSms.add(newSms);

                                newSms.blacklisted = 1;
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

                                //update to whitelisted since user replied in this thread
                                ContentValues newCv = new ContentValues();
                                newCv.put("blacklisted", 0);
                                newCv.put("filtered_status","filtered");
                                filteredDatabase.update("filteredThreads", newCv, "thread_id =" + threadId, null);
                                //===

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
                            contactName = getContactName(MainActivity.this, newSms.sender);

                            ContentValues filteredThreadsCv = new ContentValues();

                            if(isContact){

                                filteredThreadsCv.put("thread_id",threadId);
                                filteredThreadsCv.put("filtered_status","filtered");
                                filteredThreadsCv.put("date_string", date);
                                filteredThreadsCv.put("blacklisted", 0);
                                filteredThreadsCv.put("read", cursor.getString(cursor.getColumnIndex("read")));
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
                                filteredThreadsCv.put("filtered_status","filtered");
                                filteredThreadsCv.put("date_string", date);
                                filteredThreadsCv.put("blacklisted", 0);
                                filteredThreadsCv.put("read", cursor.getString(cursor.getColumnIndex("read")));
                                filteredDatabase.insert("filteredThreads", null, filteredThreadsCv);

                                cv.put("sender", "unknown");
                                cv.put("sender_name","");
                                filteredDatabase.insertOrThrow("messageTable", null, cv);

                                //newSms.blacklisted = 1;
                                newSms.senderName = "";
                                knownSms.add(newSms);
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

                knownSms.clear();
                unknownSms.clear();
                smsList.clear();

                loadDatabase();
                loadLayout();

                pb.setVisibility(View.GONE);
                firstRunText.setVisibility(View.GONE);
                fab.setVisibility(View.VISIBLE);

            }
        }.execute();

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

            Cursor threadCursor = filteredDatabase.rawQuery("select thread_id, filtered_status, blacklisted, read from filteredThreads ORDER BY date_string DESC;", null);

            threadCursor.moveToFirst();

            openExistingDatabase(threadCursor);

            filteredDatabase.close();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            //----experimental-------
            unknownInstance.selectedViews = new boolean[unknownSms.size()];
            unknownInstance.threadsToDelete = new String[unknownSms.size()];
            Arrays.fill(unknownInstance.selectedViews, Boolean.FALSE);
            Arrays.fill(unknownInstance.threadsToDelete, null);

            knownInstance.selectedViews = new boolean[knownSms.size()];
            knownInstance.threadsToDelete = new String[knownSms.size()];
            Arrays.fill(knownInstance.selectedViews, Boolean.FALSE);
            Arrays.fill(knownInstance.threadsToDelete, null);
            //-----------------------

            refreshFragments();

        }
    }

    void setupFragments(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());

        adapter.addFragment(new KnownFragment(), "Filtered");
        adapter.addFragment(new UnknownFragment(), "Unfiltered");

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

    public void getPermissionToCallPhone() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.CALL_PHONE)) {
                    Toast.makeText(this, "Please allow permission!", Toast.LENGTH_SHORT).show();
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
        // Make sure it's our original READ_CONTACTS request
        if (requestCode == READ_SMS_PERMISSIONS_REQUEST) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED ) {

                if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED)
                    getPermissionToReadContacts();

                else if(ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                        != PackageManager.PERMISSION_GRANTED)
                    getPermissionToCallPhone();
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
                else if(ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                        != PackageManager.PERMISSION_GRANTED)
                    getPermissionToCallPhone();
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

        if (requestCode == CALL_PHONE_PERMISSIONS_REQUEST ) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED ) {

                if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                        != PackageManager.PERMISSION_GRANTED)
                    getPermissionToReadSMS();
                else if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED)
                    getPermissionToReadContacts();
                else{
                    createOrOpenDb();
                    refreshSmsInbox();
                    loadLayout();
                }

            }

            else {
                Toast.makeText(this, "Call Phone permission denied", Toast.LENGTH_SHORT).show();
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

        selectAllFiltered = tb.getMenu().findItem(R.id.selectAllFiltered);
        selectAllUnfiltered = tb.getMenu().findItem(R.id.selectAllUnfiltered);

        cancelButton = tb.getMenu().findItem(R.id.cancelButton);
        deleteButton = tb.getMenu().findItem(R.id.deleteButton);

        deletionMode = true;

        unknownInstance.setDeletionModeClickListener();
        knownInstance.setDeletionModeClickListener();

        tb.setTitle("Deletion Mode");

        cancelButton.setVisible(true);
        deleteButton.setVisible(true);

        selectAllFiltered.setVisible(true);
        selectAllUnfiltered.setVisible(true);

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

                if (defaultSmsApp.equals("mhd3v.filteredsms")){

                    final ArrayList<String> threadIds = new ArrayList<>();

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

        selectAllFiltered.setVisible(false);
        selectAllUnfiltered.setVisible(false);

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

}