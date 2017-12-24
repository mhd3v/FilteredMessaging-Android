package mhd3v.filteredsms;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

public class CoversationActivity extends AppCompatActivity {

    ArrayList<messages> messageList;
    EditText input;
    SmsManager smsManager = SmsManager.getDefault();
    private static final int READ_SMS_PERMISSIONS_REQUEST = 1;

    //   ArrayList<String> senderMessages;
 //   ArrayList<String> senderTime;

  //  ArrayList<String> reverseSenderMessages = new ArrayList<>();
  //  ArrayList<String> reverseSenderTime = new ArrayList();

  //  ArrayList<String> userMessages;
  //  ArrayList<String> userTime;

  //  ArrayList<String> reverseUserMessages = new ArrayList<>();
  //  ArrayList<String> reverseUserTime = new ArrayList();
    String sendername;
    String sendernumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coversation);

//        Intent intent = getIntent();
//
//        sender = intent.getStringExtra("sender");
//
//        senderMessages = intent.getStringArrayListExtra("senderMessages");
//        senderTime = intent.getStringArrayListExtra("senderTime");
//
//        userMessages = intent.getStringArrayListExtra("userMessages");
//        userTime = intent.getStringArrayListExtra("userTime");
//
//        for(int i=senderMessages.size()-1; i >= 0; i--) {
//
//            reverseSenderMessages.add(senderMessages.get(i));
//            reverseSenderTime.add(senderTime.get(i));
//        }
//
//
//        for(int i=userMessages.size()-1; i >= 0; i--) {
//
//            reverseUserMessages.add(userMessages.get(i));
//            reverseUserTime.add(userTime.get(i));
//
//        }

        Intent intent = getIntent();
        Bundle args = intent.getBundleExtra("BUNDLE");
        messageList = (ArrayList<messages>) args.getSerializable("messageList");

        Collections.reverse(messageList);

        sendername = intent.getStringExtra("sendername");
        sendernumber = intent.getStringExtra("sendernumber");

//        Log.d("sender", sender);

        ListView conversation = (ListView) findViewById(R.id.conversationList);

        conversation.setAdapter(new customAdapter());

    }

    public void onSendClick(View view) {

        input = (EditText) findViewById(R.id.edittext_chatbox);

        Log.d("aqib", sendernumber);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
                getPermissionToReadSMS();
        } else {
            smsManager.sendTextMessage(sendernumber, null, input.getText().toString(), null, null);
            Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show();
        }
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

    class customAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            Log.d("test", Integer.toString(messageList.size()));
            //Log.d("test1", Integer.toString(messageList.));
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

            TextView senderName = view.findViewById(R.id.senderName);

            //senderName.setText(messageList);

//            try{
//
//                if(!(reverseSenderMessages.get(i).equals(null))){
//
//                    TextView senderMessage= view.findViewById(R.id.senderText);
//                    senderMessage.setText(reverseSenderMessages.get(i));
//                    senderMessage.setVisibility(View.VISIBLE);
//
//                    TextView senderTimeText = view.findViewById(R.id.senderTime);
//                    String time = convertDate(reverseSenderTime.get(i),"dd/MM - hh:mm aa");
//                    senderTimeText.setText(time);
//                    senderTimeText.setVisibility(View.VISIBLE);
//
//
//
//                    ImageView img = view.findViewById(R.id.image_message_profile);
//                    img.setVisibility(View.VISIBLE);
//                }
//
//            }
//            catch (Exception e) {
//
//            }
//
//            try{
//
//                if(!(reverseUserMessages.get(i).equals(null))){
//
//                    TextView userMessage= view.findViewById(R.id.userText);
//                    userMessage.setText(reverseUserMessages.get(i));
//
//                    TextView userTimeText = view.findViewById(R.id.userTime);
//
//                    String time = convertDate(reverseUserTime.get(i),"dd/MM hh:mm");
//                    userTimeText.setText(time);
//                    userTimeText.setVisibility(View.VISIBLE);
//
//                    userMessage.setVisibility(View.VISIBLE);
//                    }
//                }
//
//            catch (Exception e){
//
//            }



            if(messageList.get(i).isUserMessage){

                TextView userMessage= view.findViewById(R.id.userText);
                userMessage.setText(messageList.get(i).messageBody);
                userMessage.setVisibility(View.VISIBLE);

                TextView userTimeText = view.findViewById(R.id.userTime);
                //Log.d("time1", messageList.get(i).time);
                String time = convertDate(messageList.get(i).time,"dd/MM hh:mm aa");
                Log.d("usertime", time);
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



}
