package mhd3v.filteredsms;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

import static android.R.attr.data;
import static android.R.id.input;
import static android.provider.OpenableColumns.DISPLAY_NAME;

public class NewMessage extends AppCompatActivity {

    final int PICK_CONTACT=1;

    Intent intent;
    private String SimState = "";

    EditText senderEt;
    EditText messageEt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_message);

        senderEt = ((EditText) findViewById(R.id.edittext_contactnumber));
        messageEt = ((EditText) findViewById(R.id.edittext_chatbox));

    }

    public void onAddContactIcon(View view) {

        intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
        startActivityForResult(intent, PICK_CONTACT);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        EditText textPhone = (EditText) findViewById(R.id.edittext_contactnumber);

        // TODO Auto-generated method stub
        if(resultCode == RESULT_OK){
            if(requestCode == PICK_CONTACT){
                Uri returnUri = data.getData();
                Cursor cursor = getContentResolver().query(returnUri, null, null, null, null);

                if(cursor.moveToNext()){
                    int columnIndex_ID = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                    String contactID = cursor.getString(columnIndex_ID);

                    int columnIndex_HASPHONENUMBER = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                    String stringHasPhoneNumber = cursor.getString(columnIndex_HASPHONENUMBER);

                    if(stringHasPhoneNumber.equalsIgnoreCase("1")){
                        Cursor cursorNum = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactID,
                                null,
                                null);

                        //Get the first phone number
                        if(cursorNum.moveToNext()){
                            int columnIndex_number = cursorNum.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            String stringNumber = cursorNum.getString(columnIndex_number);
                            textPhone.setText(stringNumber);
                        }

                    }else{
                        textPhone.setText("NO Phone Number");
                    }


                }else{
                    Toast.makeText(getApplicationContext(), "NO data!", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public static void refreshMain() {
        //askMainToRefresh = true;
        MainActivity mainInstance  = MainActivity.getInstance();
        mainInstance.refreshInbox = true;
    }

    public void onSendClick(View view) {

        String sendernumber = senderEt.getText().toString();
        String messagebody = messageEt.getText().toString();

        if(!(sendernumber.length() == 0 && messagebody.length() == 0)){

            sendSms(sendernumber, messagebody);


        }

        else{
            if(sendernumber.length() == 0)
                Toast.makeText(this, "Please enter a phone number or select one from contacts", Toast.LENGTH_LONG).show();
            if(messagebody.length() == 0)
                Toast.makeText(this, "Please enter a message body", Toast.LENGTH_LONG).show();

        }

    }

    void updateDB(String address, String message, String time){

        String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this);

        if(defaultSmsApp.equals("mhd3v.filteredsms")){
            ContentValues values = new ContentValues();
            values.put("address", address);//sender name
            values.put("body", message);
            this.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
        }

        refreshMain();

        finish();


    }

    private void sendSms(final String address, final String message) {
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
                                updateDB(address, message, Long.toString(System.currentTimeMillis()));
                                break;
                            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                                Toast.makeText(getBaseContext(), "Generic failure", Toast.LENGTH_LONG).show();
                                break;
                            case SmsManager.RESULT_ERROR_NO_SERVICE:
                                Toast.makeText(getBaseContext(), "No service", Toast.LENGTH_LONG).show();
                                break;
                            case SmsManager.RESULT_ERROR_NULL_PDU:
                                Toast.makeText(getBaseContext(), "Null PDU", Toast.LENGTH_LONG).show();
                                break;
                            case SmsManager.RESULT_ERROR_RADIO_OFF:
                                Toast.makeText(getBaseContext(), "Radio off", Toast.LENGTH_LONG).show();
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