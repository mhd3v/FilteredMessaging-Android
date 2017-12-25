package mhd3v.filteredsms;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CoversationActivity extends AppCompatActivity {

    ArrayList<messages> messageList;
    EditText input;

    SmsManager smsManager;

    static boolean askMainToRefresh = false;

    static boolean active = false;

    String sender;
    String senderName;


    customAdapter adapter;
    static Intent intent;


    static CoversationActivity conversationInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coversation);


        intent = getIntent();
        Bundle args = intent.getBundleExtra("BUNDLE");
        messageList = (ArrayList<messages>) args.getSerializable("messageList");

        Collections.reverse(messageList);

        sender = intent.getStringExtra("sender");
        senderName = intent.getStringExtra("senderName");

        ListView conversation = (ListView) findViewById(R.id.conversationList);
        adapter = new customAdapter();

        conversation.setAdapter(adapter);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(toolbar != null) {
            setSupportActionBar(toolbar);

            if(senderName.equals(""))
                getSupportActionBar().setTitle(sender);
            else
            getSupportActionBar().setTitle(senderName);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                finish();
            }
        });

    }



    public static void refreshMain() {
        //askMainToRefresh = true;
        MainActivity mainInstance  = MainActivity.getInstance();
        mainInstance.refreshInbox = true;
    }

    class customAdapter extends BaseAdapter {


        @Override
        public int getCount() {
            return messageList.size() ;
        }

        public void updateMessageList(ArrayList<messages> newlist) {
            messageList.clear();
            messageList.addAll(newlist);
            this.notifyDataSetChanged();
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

    @Override
    public void onStart() {
        super.onStart();
        conversationInstance = this;
    }


    public void onSendClick(View view) {

            smsManager = SmsManager.getDefault();

            input = (EditText) findViewById(R.id.edittext_chatbox);


            if(!(input.getText().toString().trim().length() == 0)){

                smsManager.sendTextMessage(sender, null, input.getText().toString(), null, null);

                messages newSms = new messages(input.getText().toString() ,Long.toString(System.currentTimeMillis()));

                newSms.isUserMessage = true;

                ArrayList<messages> newMessageList = new ArrayList<>();

                newMessageList.addAll(messageList);

                newMessageList.add(newSms);

                adapter.updateMessageList(newMessageList);

                Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show();

                try  { //close keyboard
                    InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                } catch (Exception e) {

                }

                input.setText("");
                refreshMain();
            }

            else{
                Toast.makeText(this, "Please enter a message body", Toast.LENGTH_LONG).show();
            }



    }

    @Override
    public void onStop() {
        super.onStop();
        //active = false;
        conversationInstance = null;

    }



}
