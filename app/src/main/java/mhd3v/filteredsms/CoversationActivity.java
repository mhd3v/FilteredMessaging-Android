package mhd3v.filteredsms;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
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
    ArrayList<String> senderTime;

    ArrayList<String> reverseSenderMessages = new ArrayList<>();
    ArrayList<String> reverseSenderTime = new ArrayList();

    ArrayList<String> userMessages;
    ArrayList<String> userTime;

    ArrayList<String> reverseUserMessages = new ArrayList<>();
    ArrayList<String> reverseUserTime = new ArrayList();
    String sender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coversation);

        Intent intent = getIntent();

        sender = intent.getStringExtra("sender");

        senderMessages = intent.getStringArrayListExtra("senderMessages");
        senderTime = intent.getStringArrayListExtra("senderTime");

        userMessages = intent.getStringArrayListExtra("userMessages");
        userTime = intent.getStringArrayListExtra("userTime");

        for(int i=senderMessages.size()-1; i >= 0; i--) {

            reverseSenderMessages.add(senderMessages.get(i));
            reverseSenderTime.add(senderTime.get(i));
        }


        for(int i=userMessages.size()-1; i >= 0; i--) {

            reverseUserMessages.add(userMessages.get(i));
            reverseUserTime.add(userTime.get(i));

        }


        ListView conversation = (ListView) findViewById(R.id.conversationList);

        conversation.setAdapter(new customAdapter());

    }

    class customAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return senderMessages.size() ;
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

            senderName.setText(sender);

            try{

                if(!(reverseSenderMessages.get(i).equals(null))){

                    TextView senderMessage= view.findViewById(R.id.senderText);
                    senderMessage.setText(reverseSenderMessages.get(i));
                    senderMessage.setVisibility(View.VISIBLE);

                    TextView senderTimeText = view.findViewById(R.id.senderTime);
                    String time = convertDate(reverseSenderTime.get(i),"dd/MM - hh:mm aa");
                    senderTimeText.setText(time);
                    senderTimeText.setVisibility(View.VISIBLE);



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

                    TextView userTimeText = view.findViewById(R.id.userTime);

                    String time = convertDate(reverseUserTime.get(i),"dd/MM hh:mm");
                    userTimeText.setText(time);
                    userTimeText.setVisibility(View.VISIBLE);

                    userMessage.setVisibility(View.VISIBLE);
                    }
                }

            catch (Exception e){

            }

            return view;
        }

        public String convertDate(String dateInMilliseconds,String dateFormat) {
            return DateFormat.format(dateFormat, Long.parseLong(dateInMilliseconds)).toString();
        }

    }
}
