package mhd3v.filteredsms;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private SectionsPageAdapter mSectionsPageAdapter;

    private ViewPager mViewPager;

    ArrayList<sms> smsList = new ArrayList<>();

    ArrayList<String> senderList = new ArrayList<>();

    Menu menu;
    private static final int READ_SMS_PERMISSIONS_REQUEST = 1;
    
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




    }



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
    }

    public void refreshSmsInbox() {
        ContentResolver contentResolver = getContentResolver();
        Cursor smsInboxCursor = contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, null);
        int indexBody = smsInboxCursor.getColumnIndex("body");
        int indexAddress = smsInboxCursor.getColumnIndex("address");
        if (indexBody < 0 || !smsInboxCursor.moveToFirst()) return;

        Cursor smsOutboxCursor = contentResolver.query(
                Uri.parse("content://sms/sent"), null, null, null, null);
        smsOutboxCursor.moveToFirst();


        do {


            boolean found = false;

            for(int i = 0; i < smsList.size(); i++){
                if(smsList.get(i).sender.equals(smsInboxCursor.getString(indexAddress))){
                    smsList.get(i).addNew(smsInboxCursor.getString(indexBody));
                    found = true;
                }

            }
            if(found == false) {

                sms newSms = new sms(smsInboxCursor.getString(indexAddress), smsInboxCursor.getString(indexBody));

                smsList.add(newSms);
            }


        } while (smsInboxCursor.moveToNext());

        do {


            boolean found = false;

            for(int i = 0; i < smsList.size(); i++){
                if(smsList.get(i).sender.equals(smsOutboxCursor.getString(indexAddress))){
                    smsList.get(i).addNewUserMessage(smsOutboxCursor.getString(indexBody));
                    found = true;
                }

            }
            if(found == false) {

                sms newSms = new sms(smsOutboxCursor.getString(indexAddress), null);

                newSms.addNewUserMessage(smsOutboxCursor.getString(indexBody));

                smsList.add(newSms);
            }


        } while (smsOutboxCursor.moveToNext());

        setSMS(smsList);
    }

    void setSMS(ArrayList<sms> smsList){
        this.smsList = smsList;
    }

    public ArrayList<sms> getSms() {
        return smsList;
    }


}
