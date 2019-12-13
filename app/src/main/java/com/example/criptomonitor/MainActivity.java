package com.example.criptomonitor;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.Scanner;

public class MainActivity extends AppCompatActivity {
    private MainFragment mainFragment;
    private RangeFragment rangeFragment;
    private AddFragment addFragment;
    //Флаг привязки сервиса к активности
    private boolean isServiceBound = false;
    CriptoMonitorService criptoService;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i("CriptoMonitor", "ServiceConnection(onServiceConnected)");
            CriptoMonitorService.LocalBinder binder = (CriptoMonitorService.LocalBinder)iBinder;
            criptoService = binder.getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i("CriptoMonitor", "ServiceConnection(onServiceDisconnected)");
            isServiceBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "MainActivity(onCreate)");
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

        Intent intentService = new Intent(this, CriptoMonitorService.class);
        if (isServiceBound == false) {
            bindService(intentService, serviceConnection, BIND_AUTO_CREATE);
        }

    }

    @Override
    protected void onDestroy() {
        Log.i("CriptoMonitor", "MainActivity(onDestroy)");
        super.onDestroy();

        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
}
