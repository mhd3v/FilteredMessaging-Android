package mhd3v.filteredsms;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Mahad on 11/27/2017.
 */

public class UnknownFragment extends Fragment {

    ArrayList<sms> smsList;

    UnknownFragment.customAdapter unknownAdapter;
    ListView unknownList;
    UnknownFragment thisInstance;

    boolean[] selectedViews;
    String[] threadsToDelete;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_unknown, container, false);

        MainActivity activity = (MainActivity) getActivity();
        activity.setUnknownInstance(this);

        thisInstance = this;

        unknownList = view.findViewById(R.id.unknownList);

        smsList = activity.getUnknownSms();

        selectedViews = new boolean[smsList.size()];
        threadsToDelete = new String[smsList.size()];

        unknownAdapter = new customAdapter();
        activity.setUnknownAdapter(unknownAdapter);
        unknownList.setAdapter(unknownAdapter);

        setDefaultListener();

        unknownList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
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

        unknownList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                ImageView unknownSenderSelected = view.findViewById(R.id.unknownSenderSelected);

                if(!selectedViews[position]) {
                    unknownSenderSelected.setVisibility(View.VISIBLE);
                    selectedViews[position] = true;
                    threadsToDelete[position] = smsList.get(position).threadId;
                }

                else{
                    unknownSenderSelected.setVisibility(View.GONE);
                    selectedViews[position] = false;
                    threadsToDelete[position] = null;
                }

            }
        });
    }


    void setDefaultListener(){

        unknownList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                Intent intent = new Intent(getActivity(), ConversationActivity.class);
                Bundle args = new Bundle();
                args.putSerializable("messageList", (Serializable) smsList.get(position).messages);
                intent.putExtra("sender", smsList.get(position).sender);
                intent.putExtra("BUNDLE", args);
                intent.putExtra("sender", smsList.get(position).sender);

                if(!(smsList.get(position).senderName.equals("")))
                    intent.putExtra("senderName", smsList.get(position).senderName);
                else
                    intent.putExtra("senderName", "");

                intent.putExtra("threadId", smsList.get(position).threadId);
                intent.putExtra("blacklisted", smsList.get(position).blacklisted);
                intent.putExtra("read", smsList.get(position).read);

                intent.setAction("frag2");

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


    class customAdapter extends BaseAdapter {

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

            view = getActivity().getLayoutInflater().inflate(R.layout.fragments_list, null);

            TextView sender = view.findViewById(R.id.sender);

            if(!(smsList.get(i).senderName.equals("")))
                sender.setText(smsList.get(i).senderName);
            else
                sender.setText(smsList.get(i).sender);

            if(smsList.get(i).read == 0)
                sender.setTypeface(null, Typeface.BOLD);
            else
                sender.setTypeface(null, Typeface.NORMAL);

            TextView time = view.findViewById(R.id.time);
            String lastSenderMessageTime = smsList.get(i).messages.get(0).time;
            lastSenderMessageTime = convertDate(lastSenderMessageTime, "E dd/MM - hh:mm aa");
            time.setText(lastSenderMessageTime);

            TextView text = view.findViewById(R.id.textbody);

            if(smsList.get(i).messages.get(0).messageBody.length() >= 50)
                text.setText(TextUtils.substring(smsList.get(i).messages.get(0).messageBody, 0, 50) + "...");
            else
                text.setText(smsList.get(i).messages.get(0).messageBody);


            ImageView contactPicture = view.findViewById(R.id.contactPicture);

            ImageView unknownSelected = view.findViewById(R.id.unknownSenderSelected);

            if(selectedViews[i])
                unknownSelected.setVisibility(View.VISIBLE);

            contactPicture.setImageResource(R.drawable.unknownsender);

            return view;
        }


        public String convertDate(String dateInMilliseconds, String dateFormat) {
            return DateFormat.format(dateFormat, Long.parseLong(dateInMilliseconds)).toString();
        }
    }




}
