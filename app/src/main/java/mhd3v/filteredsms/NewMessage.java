package mhd3v.filteredsms;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import static android.R.attr.data;
import static android.R.id.input;
import static android.provider.OpenableColumns.DISPLAY_NAME;

public class NewMessage extends AppCompatActivity {

    final int PICK_CONTACT=1;

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_message);

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

    public void onSendClick(View view) {

        String sendernumber = ((EditText) findViewById(R.id.edittext_contactnumber)).getText().toString();

        String messagebody = ((EditText) findViewById(R.id.edittext_chatbox)).getText().toString();

        SmsManager smsManager = SmsManager.getDefault();

        smsManager.sendTextMessage(sendernumber, null, messagebody, null, null);
        Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, MainActivity.class);
        finish();
        startActivity(intent);

    }


   /* @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (PICK_CONTACT) :
                if (resultCode == Activity.RESULT_OK && intent != null) {
                    Uri contactData = data.getData();

                    final String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY;

                    final String FILTER = DISPLAY_NAME + " NOT LIKE '%@%'";

                    final String ORDER = String.format("%1$s COLLATE NOCASE", DISPLAY_NAME);

                    final String[] PROJECTION = {
                            ContactsContract.Contacts._ID,
                            DISPLAY_NAME,
                            ContactsContract.Contacts.HAS_PHONE_NUMBER
                    };
                    ContentResolver cr = getContentResolver();
                    Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, PROJECTION, FILTER, null, ORDER);

                    String contact = "0";

                    if(cursor.moveToFirst()){
                        String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                        Cursor cp = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                        if (cp != null && cp.moveToFirst()) {
                                contact = cp.getString(cp.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                cp.close();
                        }
          //          }

         //           Cursor c =  getContentResolver().query(contactData, null, null, null, null);
         //           if (c.moveToFirst()) {
         //               String contact = c.getString(c.getColumnIndexOrThrow(ContactsContract.Data.));

                            // TODO Whatever you want to do with the selected contact.
                            EditText editText = (EditText) findViewById(R.id.edittext_contactnumber);
                            editText.setText(contact);

                        }
                   //     c.close();}
                }
                break;
        }
    } */
}