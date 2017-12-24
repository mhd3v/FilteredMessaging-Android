package mhd3v.filteredsms;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;

import static android.R.attr.value;

public class MainActivity extends AppCompatActivity {

    private SectionsPageAdapter mSectionsPageAdapter;

    private ViewPager mViewPager;

    ArrayList<sms> knownSms = new ArrayList<>();

    ArrayList<sms> unknownSms = new ArrayList<>();

    ArrayList<sms> smsList = new ArrayList<>();

    boolean isContact;

    Menu menu;

    private static final int READ_SMS_PERMISSIONS_REQUEST = 1;
    private static final int READ_CONTACTS_PERMISSIONS_REQUEST = 1;

    static MainActivity inst;

    ArrayAdapter arrayAdapter;

 //   Tab1Fragment t1 = new Tab1Fragment();

    public static MainActivity instance() {
        return inst;
    }

    static boolean active = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            getPermissionToReadSMS();
        } else
            refreshSmsInbox();

        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager)findViewById(R.id.container);
        setupFragments(mViewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        Toolbar tb = (Toolbar) findViewById(R.id.toolbar);
        tb.setTitle("Filtered Messaging");

        tb.inflateMenu(R.menu.menu_main);

        tb.findViewById(R.id.settingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);

            }
        });

        tb.findViewById(R.id.newmessagebutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NewMessage.class);
                startActivity(intent);

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        active = true;
        inst = this;
    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }

  /*  public void updateInbox(final String smsMessage) {

        customAdapter c1 = new customAdapter();


        arrayAdapter.insert(smsMessage, 0);
        arrayAdapter.notifyDataSetChanged();
    } */

    void setupFragments(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());

        adapter.addFragment(new Tab1Fragment(), "Known");
        adapter.addFragment(new Tab2Fragment(), "Unknown");

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
    }

    @android.support.annotation.RequiresApi(api = Build.VERSION_CODES.M)
    public void getPermissionToReadContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_CONTACTS)) {
                Toast.makeText(this, "Please allow permission!", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                    READ_CONTACTS_PERMISSIONS_REQUEST);

        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        // Make sure it's our original READ_CONTACTS request
        if (requestCode == READ_SMS_PERMISSIONS_REQUEST) {
            if (grantResults.length == 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Read SMS permission granted", Toast.LENGTH_SHORT).show();
                refreshSmsInbox();
            } else {
                Toast.makeText(this, "Read SMS permission denied", Toast.LENGTH_SHORT).show();
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        if (requestCode == READ_CONTACTS_PERMISSIONS_REQUEST) {
            if (grantResults.length == 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Read SMS permission granted", Toast.LENGTH_SHORT).show();
                refreshSmsInbox();
            } else {
                Toast.makeText(this, "Read SMS permission denied", Toast.LENGTH_SHORT).show();
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void refreshSmsInbox() {


        Cursor cursor = getContentResolver().query(Uri
                .parse("content://sms"), null, null, null, null);

//        Uri uri = Uri.parse("content://sms/");
//        cursor = contentResolver.query(uri, null, "thread_id=" + value, null, "date asc");

        int indexBody = cursor.getColumnIndex("body");
        int indexAddress = cursor.getColumnIndex("address");
        //String threadId = ;
        if (indexBody < 0 || !cursor.moveToFirst()) return;


        String type = Integer.toString(cursor.getColumnIndex("type"));


        do {

            if (cursor.getString(Integer.parseInt(type)).equalsIgnoreCase("1")) {

                // System.out.println("ThreadID"+ cursor.getString(cursor.getColumnIndex("thread_id")));
                //received messages

                boolean found = false;

                for (int i = 0; i < smsList.size(); i++) {

                    if (smsList.get(i).threadId.equals(cursor.getString(cursor.getColumnIndex("thread_id")))) {
                        String date = cursor.getString(cursor
                                .getColumnIndex("date"));
                        smsList.get(i).addNewSenderMessage(cursor.getString(indexBody), date);
                        found = true;
                    }

                }
                if (found == false) {

                    String date = cursor.getString(cursor
                            .getColumnIndex("date"));

                    sms newSms = new sms(cursor.getString(indexAddress), cursor.getString(cursor.getColumnIndex("thread_id")));
                    newSms.addNewSenderMessage(cursor.getString(indexBody), date);
                    smsList.add(newSms);
                }

            } else if (cursor.getString(Integer.parseInt(type)).equalsIgnoreCase("2")) {

                //recieved messaged

                boolean found = false;

                for (int i = 0; i < smsList.size(); i++) {

                    if (smsList.get(i).threadId.equals(cursor.getString(cursor.getColumnIndex("thread_id")))) {

                        String date = cursor.getString(cursor.getColumnIndex("date"));

                        smsList.get(i).addNewUserMessage(cursor.getString(indexBody), date);
                        found = true;
                    }
                }

                if (found == false) {
                    String date = cursor.getString(cursor
                            .getColumnIndex("date"));

                    sms newSms = new sms(cursor.getString(indexAddress),cursor.getString(cursor.getColumnIndex("thread_id")));

                    newSms.addNewUserMessage(cursor.getString(indexBody), date);

                    smsList.add(newSms);

                }

            }

        } while (cursor.moveToNext());

        setSmsLists(smsList);

    }

    void setSmsLists(ArrayList<sms> smsList){
       // this.smsList = smsList;

        for(int i=0; i< smsList.size(); i++){

            isContact = false;

            String contactName;
            contactName = getContactName(this, smsList.get(i).sender);

            if(isContact == true){

                smsList.get(i).sender = contactName;
                knownSms.add(smsList.get(i));
            }
            else {

                unknownSms.add(smsList.get(i));
            }

        }

    }

    public ArrayList<sms> getKnownSms() {
        return knownSms;
    }
    public ArrayList<sms> getUnknownSms() {
        return unknownSms;
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



//    String getFormattedNumber(String originalNumber) {
//
//        String formattedNumber = "+92";
//
//        if (Character.toString(originalNumber.charAt(0)).equals("0")) { //if user sent message without entering the area code
//
//            for (int j = 1; j < originalNumber.length(); j++) {
//                formattedNumber = formattedNumber + Character.toString(originalNumber.charAt(j));
//            }
//
//            return formattedNumber;
//
//        }
//
//        else
//            return originalNumber;
//
//    }


}
