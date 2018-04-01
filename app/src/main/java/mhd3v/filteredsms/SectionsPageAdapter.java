package mhd3v.filteredsms;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mahad on 11/27/2017.
 */

public class SectionsPageAdapter extends FragmentPagerAdapter {

    private final List<Fragment> mFragmentList = new ArrayList<>(); //list to keep track of fragments

    private final List<String> mFragmentTitleList = new ArrayList<>(); //list to keep track of fragment titles

    public void addFragment(Fragment fragment,  String title) {
        mFragmentList.add(fragment);
        mFragmentTitleList.add(title);
    }

    public SectionsPageAdapter(FragmentManager fm) { //default constructor
        super(fm);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mFragmentTitleList.get(position);
    }

    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position); //return fragment at list[pos]
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

}


