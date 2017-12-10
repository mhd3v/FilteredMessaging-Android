package mhd3v.filteredsms;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class CoversationActivity extends AppCompatActivity {

    ArrayList<String> senderMessages;
    ArrayList<String> reverseSenderMessages = new ArrayList<>();

    ArrayList<String> userMessages;
    ArrayList<String> reverseUserMessages = new ArrayList<>();
    String sender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coversation);

        Intent intent = getIntent();

        //int userMessagesSize = intent.getStringArrayExtra("userMessages").length;
        sender = intent.getStringExtra("sender");

        Log.d("sender123", sender);


       // senderMessages = new String[senderMessagesSize];
        //userMessages = new String[userMessagesSize];


        //Log.d("userMessages", Integer.toString(userMessagesSize));

        senderMessages = intent.getStringArrayListExtra("senderMessages");
        userMessages = intent.getStringArrayListExtra("userMessages");

        for(int i=senderMessages.size()-1; i >= 0; i--) {

            reverseSenderMessages.add(senderMessages.get(i));
        }

        for(int i=userMessages.size()-1; i >= 0; i--) {

            reverseUserMessages.add(userMessages.get(i));
        }





        // senderMessages = reversedMessages;

        //Log.d("sender11", senderMessages.get(0));

        //userMessages = intent.getStringArrayExtra("userMessages");

        ListView conversation = (ListView) findViewById(R.id.conversationList);

        conversation.setAdapter(new customAdapter());

    }

    class customAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return senderMessages.size() ; //set array adapter next six days from today
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
//
            view = getLayoutInflater().inflate(R.layout.conversation_list,null);

            TextView senderName = view.findViewById(R.id.senderName);

            senderName.setText(sender);



            try{

                if(!(reverseSenderMessages.get(i).equals(null))){
                    TextView senderMessage= view.findViewById(R.id.senderText);
                    senderMessage.setText(reverseSenderMessages.get(i));
                    senderMessage.setVisibility(View.VISIBLE);
                    ImageView img = view.findViewById(R.id.image_message_profile);
                    img.setVisibility(View.VISIBLE);
                }

            }
            catch (Exception e) {

            }

            try{

                if(!(reverseUserMessages.get(i).equals(null))){
                    TextView userMessage= view.findViewById(R.id.userText);
                    userMessage.setText(reverseUserMessages.get(i));
                    userMessage.setVisibility(View.VISIBLE);
                    }
                }

            catch (Exception e){

            }


//            TextView time= view.findViewById(R.id.time);
//            TextView text= view.findViewById(R.id.textbody);

           // text.setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum eget interdum enim, vitae elementum tortor. Vivamus elementum mauris in metus aliquam, sed placerat nibh pharetra.");

           // ImageView contactPicture = view.findViewById(R.id.contactPicture);

            //contactPicture.setImageResource(R.drawable.unknownsender);


            return view;
        }
    }
}
