package com.example.criptomonitor;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private MainFragment mainFragment;
    private RangeFragment rangeFragment;
    private AddFragment addFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("CriptoParser", "MainActivity(onCreate)");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainFragment = (MainFragment) getSupportFragmentManager().findFragmentByTag("mainFragment");
        rangeFragment = (RangeFragment) getSupportFragmentManager().findFragmentByTag("rangeFragment");
        addFragment = (AddFragment) getSupportFragmentManager().findFragmentByTag("addFragment");

        if (mainFragment == null) {
            mainFragment = new MainFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragLayout1, mainFragment,"mainFragment").commit();
        }

        if (rangeFragment == null) {
            rangeFragment = new RangeFragment();
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                getSupportFragmentManager().beginTransaction().add(R.id.fragLayout2, rangeFragment, "rangeFragment").commit();
        }
        else{
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                getSupportFragmentManager().beginTransaction().show(rangeFragment).commit();
            else
                getSupportFragmentManager().beginTransaction().hide(rangeFragment).commit();
        }

    }

    @Override
    protected void onDestroy() {
        Log.i("CriptoParser", "MainActivity(onDestroy)");
        super.onDestroy();
    }
}
