package mhd3v.filteredsms;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
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

public class Tab2Fragment extends Fragment {

    ArrayList<sms> smsList;

    Tab2Fragment.customAdapter unknownAdapter;
    ListView unknownList;
    Tab2Fragment thisInstance;

    boolean[] selectedViews;
    String[] threadsToDelete;
    static boolean deletionMode = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.tab2_fragment, container, false);

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

                deletionMode = true;

                MainActivity main = (MainActivity) getActivity();

                if(main.knownInstance.deletionMode){  //if tab1 is in deletion mode
                    main.cancelDeletionMode(main.knownInstance, main.cancelButtonFiltered);
                }

                main.setDeletionMode(thisInstance);


                if(deletionMode){

                    unknownList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                            ImageView contactPicture = view.findViewById(R.id.contactPicture);

                            if(!selectedViews[position]) {
                                contactPicture.setImageResource(R.drawable.unknownsenderselected);
                                selectedViews[position] = true;
                                threadsToDelete[position] = smsList.get(position).sender;
                            }

                            else{
                                contactPicture.setImageResource(R.drawable.unknownsender);
                                selectedViews[position] = false;
                                threadsToDelete[position] = null;
                            }

                        }
                    });
                }


                return false;
            }
        });



        return view;
    }


    void setDefaultListener(){

        unknownList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                Intent intent = new Intent(getActivity(), CoversationActivity.class);
                Bundle args = new Bundle();
                args.putSerializable("messageList", (Serializable) smsList.get(position).messages);
                intent.putExtra("sender", smsList.get(position).sender);
                intent.putExtra("BUNDLE", args);

                intent.putExtra("sender", smsList.get(position).sender);
                intent.putExtra("senderName", "");
                intent.putExtra("threadId", smsList.get(position).threadId);

                intent.setAction("frag2");

                startActivity(intent);

            }
        });

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

            view = getActivity().getLayoutInflater().inflate(R.layout.custom_list, null);

            TextView sender = view.findViewById(R.id.sender);

            sender.setText(smsList.get(i).sender);

            TextView time = view.findViewById(R.id.time);
            String lastSenderMessageTime = smsList.get(i).messages.get(0).time;
            lastSenderMessageTime = convertDate(lastSenderMessageTime, "dd/MM - hh:mm aa");
            time.setText(lastSenderMessageTime);

            TextView text = view.findViewById(R.id.textbody);

            if(smsList.get(i).messages.get(0).messageBody.length() >= 50)
                text.setText(TextUtils.substring(smsList.get(i).messages.get(0).messageBody, 0, 50) + "...");
            else
                text.setText(smsList.get(i).messages.get(0).messageBody);


            ImageView contactPicture = view.findViewById(R.id.contactPicture);

            if(!selectedViews[i])
                contactPicture.setImageResource(R.drawable.unknownsender);
            else
                contactPicture.setImageResource(R.drawable.unknownsenderselected);



            return view;
        }


        public String convertDate(String dateInMilliseconds, String dateFormat) {
            return DateFormat.format(dateFormat, Long.parseLong(dateInMilliseconds)).toString();
        }
    }




}
