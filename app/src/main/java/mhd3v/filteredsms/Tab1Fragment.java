package mhd3v.filteredsms;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Created by Mahad on 11/27/2017.
 */

public class Tab1Fragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.tab1_fragment, container, false);

        ListView knownList = (ListView) view.findViewById(R.id.knownList);

        knownList.setAdapter(new customAdapter());

        return view;
    }


    class customAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return 6; //set array adapter next six days from today
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
            view = getActivity().getLayoutInflater().inflate(R.layout.custom_list,null);

            TextView sender= view.findViewById(R.id.sender);

            sender.setText("Known Sender");

            TextView time= view.findViewById(R.id.time);
            TextView text= view.findViewById(R.id.textbody);

            text.setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum eget interdum enim, vitae elementum tortor. Vivamus elementum mauris in metus aliquam, sed placerat nibh pharetra.");

            ImageView contactPicture = view.findViewById(R.id.contactPicture);
            contactPicture.setImageResource(R.drawable.knownsender);

            return view;
        }
    }


}
