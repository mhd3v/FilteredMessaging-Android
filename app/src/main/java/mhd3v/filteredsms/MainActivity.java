package mhd3v.filteredsms;

import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import static mhd3v.filteredsms.R.menu.menu_main;

public class MainActivity extends AppCompatActivity {

    private SectionsPageAdapter mSectionsPageAdapter;

    private ViewPager mViewPager;

    Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager)findViewById(R.id.container);
        setupFragments(mViewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        Toolbar tb = (Toolbar) findViewById(R.id.toolbar);
        tb.setTitle("Filtered Messaging");

        tb.inflateMenu(R.menu.menu_main);


    }

    void setupFragments(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());

        adapter.addFragment(new Tab1Fragment(), "Known");
        adapter.addFragment(new Tab2Fragment(), "Unknown");

        viewPager.setAdapter(adapter);
    }




}
