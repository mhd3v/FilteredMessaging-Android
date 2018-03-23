package mhd3v.filteredsms;

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

public class Tab1Fragment extends Fragment {

    ArrayList<sms> smsList = new ArrayList<>();

    customAdapter knownAdapter;
    ListView knownList;
    Tab1Fragment thisInstance;

    boolean[] selectedViews;
    String[] threadsToDelete;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.tab1_fragment, container, false);

        MainActivity activity = (MainActivity) getActivity();
        activity.setKnownInstance(this);

        thisInstance = this;

        knownList = view.findViewById(R.id.knownList);

        //if(smsList.isEmpty())
        smsList = activity.getKnownSms();

        selectedViews = new boolean[smsList.size()];
        threadsToDelete = new String[smsList.size()];

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

                ImageView contactPicture = view.findViewById(R.id.contactPicture);
                ImageView knownSenderSelected = view.findViewById(R.id.knownSenderSelected);

                if(!selectedViews[position]) {
//                    contactPicture.setImageResource(R.drawable.knownsenderselected);
                    knownSenderSelected.setVisibility(View.VISIBLE);
                    selectedViews[position] = true;
                    threadsToDelete[position] = smsList.get(position).threadId;
                }

                else{
//                    contactPicture.setImageResource(R.drawable.knownsender);
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
                args.putSerializable("messageList", smsList.get(position).messages);
                intent.putExtra("BUNDLE",args);

                intent.putExtra("sender", smsList.get(position).sender);
                intent.putExtra("senderName", smsList.get(position).senderName);
                intent.putExtra("threadId", smsList.get(position).threadId);
                intent.putExtra("blacklisted", smsList.get(position).blacklisted);
                intent.putExtra("read", smsList.get(position).read);


                intent.setAction("frag1");

                startActivity(intent);

            }
        });

    }

    public String[] getAllThreadIds(){
        String allThreads[] = new String[smsList.size()];
        for(int i = 0; i < threadsToDelete.length; i++)
            allThreads[i] = smsList.get(i).threadId;
        return allThreads;
    }


    public  class customAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return smsList.size();
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

            view = getActivity().getLayoutInflater().inflate(R.layout.custom_list,null);

            TextView sender= view.findViewById(R.id.sender);

            if(!(smsList.get(i).senderName.equals("")))
                sender.setText(smsList.get(i).senderName);
            else
                sender.setText(smsList.get(i).sender);

            if(smsList.get(i).read == 0)
                sender.setTypeface(null, Typeface.BOLD);

            TextView time= view.findViewById(R.id.time);
            String lastSenderMessageTime = smsList.get(i).messages.get(0).time;
            lastSenderMessageTime = convertDate(lastSenderMessageTime, "dd/MM - hh:mm aa");
            time.setText(lastSenderMessageTime);

            TextView text= view.findViewById(R.id.textbody);
            if(smsList.get(i).messages.get(0).messageBody.length() >= 50)
                text.setText(TextUtils.substring(smsList.get(i).messages.get(0).messageBody, 0, 50) + "...");
            else
                text.setText(smsList.get(i).messages.get(0).messageBody);

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
