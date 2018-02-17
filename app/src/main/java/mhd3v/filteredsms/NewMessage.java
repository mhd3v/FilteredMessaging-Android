package mhd3v.filteredsms;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class NewMessage extends AppCompatActivity {

    final int PICK_CONTACT=1;

    Intent intent;
    private String SimState = "";

    EditText textPhone;
    EditText messageEt;
    String phone_number;

    boolean isContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_message);

        textPhone = ((EditText) findViewById(R.id.edittext_contactnumber));
        messageEt = ((EditText) findViewById(R.id.edittext_chatbox));
        phone_number = "";

        textPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textPhone.setText("");
                phone_number = "";
            }
        });
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
                            phone_number = cursorNum.getString(cursorNum.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            String contactName = cursorNum.getString(cursorNum.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                            Log.d("aqib", contactName);
                            Log.d("aqib1", phone_number);
                            textPhone.setText(contactName);
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
        MainActivity mainInstance  = MainActivity.getInstance();
        mainInstance.refreshInbox = true;
    }

    public void onSendClick(View view) {

        if(phone_number.equals(""))
            phone_number = textPhone.getText().toString();

        String sendernumber = phone_number;
        String messagebody = messageEt.getText().toString();

        if(!(sendernumber.length() == 0 && messagebody.length() == 0)){

            Button send = (Button) findViewById(R.id.button_chatbox_send);
            send.setClickable(false);

            sendSms(sendernumber, messagebody);

            try  { //close keyboard
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            } catch (Exception e) {

            }

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


        ContentValues cv = new ContentValues();

        Cursor cursor = this.getContentResolver().query(Uri.parse("content://sms"), null, null, null, null);
        cursor.moveToFirst();

        cv.put("thread_id", cursor.getString(cursor.getColumnIndex("thread_id")));
        cv.put("date_string", time);
        cv.put("type", 2);
        cv.put("address", cursor.getString(cursor.getColumnIndex("address")));
        cv.put("body", cursor.getString(cursor.getColumnIndex("body")));

        isContact = false;
        String senderName = getContactName(this, address);

        if(!isContact)
            cv.put("sender_name", "");
        else
            cv.put("sender_name", senderName);

        SQLiteDatabase filteredDatabase = openOrCreateDatabase("filteredDatabase", MODE_PRIVATE, null);

        filteredDatabase.insert("messageTable", null, cv);

        ContentValues filteredThreadCv = new ContentValues();

        filteredThreadCv.put("date_string", time);

        int nRowsEffected = filteredDatabase.update("filteredThreads", filteredThreadCv, "thread_id =" + cursor.getString(cursor.getColumnIndex("thread_id")), null); //update time for filtered_threads entry

        if(nRowsEffected == 0){ //if no entry in filteredThreads table

            filteredThreadCv.put("thread_id", cursor.getString(cursor.getColumnIndex("thread_id")));
            filteredThreadCv.put("blacklisted", 0);
            filteredThreadCv.put("filtered_status","filtered");
            filteredDatabase.insert("filteredThreads", null, filteredThreadCv);

        }

        filteredDatabase.close();

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
}