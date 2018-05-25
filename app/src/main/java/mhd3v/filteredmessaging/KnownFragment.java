package mhd3v.filteredmessaging;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.text.TextUtils;

import java.util.ArrayList;


/**
 * Created by Mahad on 11/27/2017.
 */

public class KnownFragment extends Fragment {

    ArrayList<SMSThread> SMSThreadList = new ArrayList<>();

    customAdapter knownAdapter;
    ListView knownList;
    KnownFragment thisInstance;

    boolean[] selectedViews;
    String[] threadsToDelete;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_known, container, false);

        MainActivity activity = (MainActivity) getActivity();
        activity.setKnownInstance(this);

        thisInstance = this;

        knownList = view.findViewById(R.id.knownList);

        //if(smsList.isEmpty())
        SMSThreadList = activity.getKnownSms();

        selectedViews = new boolean[SMSThreadList.size()];
        threadsToDelete = new String[SMSThreadList.size()];

        knownAdapter = new customAdapter();

        activity.setKnownAdapter(knownAdapter);

        knownList.setAdapter(knownAdapter);

        setDefaultListener();

        knownList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {

                MainActivity main = (MainActivity) getActivity();

                if(!main.deletionMode){
                    main.setDeletionMode();
                }

                return false;
            }
        });

        return view;
    }

    void setDeletionModeClickListener(){

        knownList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ImageView knownSenderSelected = view.findViewById(R.id.knownSenderSelected);

                if(!selectedViews[position]) {
                    knownSenderSelected.setVisibility(View.VISIBLE);
                    selectedViews[position] = true;
                    threadsToDelete[position] = SMSThreadList.get(position).threadId;
                }

                else{
                    knownSenderSelected.setVisibility(View.GONE);
                    selectedViews[position] = false;
                    threadsToDelete[position] = null;
                }

            }
        });
    }

    public void setDefaultListener() {

        knownList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(getActivity(), ConversationActivity.class);
                Bundle args = new Bundle();
                args.putSerializable("messageList", SMSThreadList.get(position).messageList);
                intent.putExtra("BUNDLE",args);

                intent.putExtra("sender", SMSThreadList.get(position).sender);
                intent.putExtra("senderName", SMSThreadList.get(position).senderName);
                intent.putExtra("threadId", SMSThreadList.get(position).threadId);
                intent.putExtra("blacklisted", SMSThreadList.get(position).blacklisted);
                intent.putExtra("read", SMSThreadList.get(position).read);


                intent.setAction("frag1");

                startActivity(intent);

            }
        });

    }

    public String[] getAllThreadIds(){
        String allThreads[] = new String[SMSThreadList.size()];
        for(int i = 0; i < threadsToDelete.length; i++)
            allThreads[i] = SMSThreadList.get(i).threadId;
        return allThreads;
    }


    public  class customAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return SMSThreadList.size();
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

            view = getActivity().getLayoutInflater().inflate(R.layout.fragments_list,null);

            TextView sender= view.findViewById(R.id.sender);

            if(!(SMSThreadList.get(i).senderName.equals("")))
                sender.setText(SMSThreadList.get(i).senderName);
            else
                sender.setText(SMSThreadList.get(i).sender);

            if(SMSThreadList.get(i).read == 0)
                sender.setTypeface(null, Typeface.BOLD);

            TextView time= view.findViewById(R.id.time);
            String lastSenderMessageTime = SMSThreadList.get(i).messageList.get(0).time;
            lastSenderMessageTime = convertDate(lastSenderMessageTime, "E dd/MM - hh:mm aa");
            time.setText(lastSenderMessageTime);

            TextView text= view.findViewById(R.id.textbody);
            if(SMSThreadList.get(i).messageList.get(0).messageBody.length() >= 50)
                text.setText(TextUtils.substring(SMSThreadList.get(i).messageList.get(0).messageBody, 0, 50) + "...");
            else
                text.setText(SMSThreadList.get(i).messageList.get(0).messageBody);

            ImageView contactPicture = view.findViewById(R.id.contactPicture);

            ImageView knownSelected = view.findViewById(R.id.knownSenderSelected);

            if(selectedViews[i])
                knownSelected.setVisibility(View.VISIBLE);

            contactPicture.setImageResource(R.drawable.knownsender);

            return view;
        }

        public String convertDate(String dateInMilliseconds,String dateFormat) {
            return DateFormat.format(dateFormat, Long.parseLong(dateInMilliseconds)).toString();
        }


    }




}
