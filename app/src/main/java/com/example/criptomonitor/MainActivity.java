package com.example.criptomonitor;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements DataExchanger{
    private MainFragment mainFragment;
    private RangeFragment rangeFragment;
    private AddFragment addFragment;
    private ServiceSettigsFragment settingsFragment;
    //Флаг привязки сервиса к активности
    private boolean isServiceBound = false;
    private CriptoMonitorService criptoService;
    private Intent intentService;
    //Объект-приемник событий от сервиса
    private BroadcastReceiver serviceReciever;

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
        if(intentService == null)
            intentService = new Intent(this, CriptoMonitorService.class);
        startService(intentService);
        if (isServiceBound == false) {
            bindService(intentService, serviceConnection, BIND_AUTO_CREATE);
        }
        mainFragment = (MainFragment) getSupportFragmentManager().findFragmentByTag("mainFragment");
        rangeFragment = (RangeFragment) getSupportFragmentManager().findFragmentByTag("rangeFragment");
        addFragment = (AddFragment) getSupportFragmentManager().findFragmentByTag("addFragment");
        settingsFragment = (ServiceSettigsFragment) getSupportFragmentManager().findFragmentByTag("settingsFragment");
        setFragMain();

        if(serviceReciever == null) {
            serviceReciever = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.i("CriptoMonitor", "BroadcastReceiver(onReceive)");
                    if(getMonitoringCurrencies() != mainFragment.getCurrenciesMonitoringList())
                        setMonitoringCurrencies(mainFragment.getCurrenciesMonitoringList());
                    mainFragment.updateListAdapter();
                }
            };
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(CriptoMonitorService.CRIPTOPARSER_ACTION);
        Log.i("CriptoMonitor", "MainActivity(onStart): register reciever");
        registerReceiver(serviceReciever, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("CriptoMonitor", "MainActivity(onStop): unregister reciever");
        unregisterReceiver(serviceReciever);
    }

    //Создаем меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //Метод, вызываемый при выборе какого-либо пункта меню
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            //Если выбрали пункт "Add new Currencies"
            case R.id.action_new:
                  setFragAdd();
                return true;
            //Если выбрали пункт "Stop Service"
            case R.id.action_settings:
                setFragSetings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setFragSetings() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.addToBackStack(null);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            transaction.hide(mainFragment);
            if (settingsFragment == null) {
                settingsFragment = new ServiceSettigsFragment();
                transaction.add(R.id.fragLayout2, settingsFragment, "settingsFragment");
            }
            else
                transaction.show(settingsFragment);
        }
        transaction.commit();
    }

    private void setFragRange() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.addToBackStack(null);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            transaction.hide(mainFragment);
            if (rangeFragment == null) {
                rangeFragment = new RangeFragment();
                transaction.add(R.id.fragLayout2, rangeFragment, "rangeFragment");
            }
            else
                transaction.show(rangeFragment);
        }
        transaction.commit();
    }

    private void setFragMain() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.addToBackStack(null);

        if (mainFragment == null) {
            mainFragment = new MainFragment();
            transaction.add(R.id.fragLayout1, mainFragment,"mainFragment");
        }
        else
            transaction.show(mainFragment);

        if(addFragment != null && addFragment.isVisible())
            transaction.hide(addFragment);

        if(settingsFragment != null && settingsFragment.isVisible())
            transaction.hide(settingsFragment);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (rangeFragment == null) {
                rangeFragment = new RangeFragment();
                transaction.add(R.id.fragLayout2, rangeFragment, "rangeFragment");
            }
            else
                transaction.show(rangeFragment);
        }
        transaction.commit();
    }


    private void setFragAdd() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.addToBackStack(null);
        transaction.hide(mainFragment);
        if (addFragment == null) {
            addFragment = new AddFragment();
            transaction.add(R.id.fragLayout1, addFragment, "addFragment");
        }
        else
            transaction.show(addFragment);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            transaction.hide(rangeFragment);
        transaction.commit();
    }

    @Override
    protected void onDestroy() {
        Log.i("CriptoMonitor", "MainActivity(onDestroy)");
        super.onDestroy();

        if (isServiceBound) {
           unbindService(serviceConnection);
            isServiceBound = false;
        }
        serviceReciever = null;
        intentService = null;
    }

    @Override
    public ArrayList<Currency> getMonitoringCurrencies() {
        if(criptoService != null && isServiceBound) {

            ArrayList<Currency> listService = criptoService.getCurrenciesMonitoringList();
            for (Currency curr : listService) {
                Log.i("CriptoMonitor", "MainActivity(setCheckCurrencies): 1111currency from service " + curr.getName());
            }


            Log.i("CriptoMonitor", "MainActivity(getMonitoringCurrencies): size of list = " + criptoService.getCurrenciesMonitoringList().size());
            return criptoService.getCurrenciesMonitoringList();
        }
        else {
           Log.i("CriptoMonitor", "MainActivity(getMonitoringCurrencies): not bind service");
           return null;
        }
    }

    @Override
    public void setMonitoringCurrencies(ArrayList<Currency> listCurrencies) {
        if(criptoService != null && isServiceBound)
            criptoService.setCurrenciesMonitoringList(listCurrencies);
        else
            Log.i("CriptoMonitor", "MainActivity(setMonitoringCurrencies): not bind service");
    }

    @Override
    public void setServerSettings(int time, boolean turnOn) {
        if(time == 0) {
            if (isServiceBound && criptoService != null) {
                unbindService(serviceConnection);
                isServiceBound = false;
                stopService(intentService);
            }
            else {
                startService(intentService);
                if (isServiceBound == false) {
                    bindService(intentService, serviceConnection, BIND_AUTO_CREATE);
                }
            }
        }

     }

    @Override
    public ArrayList<Currency> getAllCurrencies() {
        if (criptoService != null && isServiceBound) {
            return criptoService.getCurrenciesAllList();
        }
        else {
            Log.i("CriptoMonitor", "MainActivity(getAllCurrencies): not bind service");
            return null;
        }
    }

    @Override
    public void setCheckCurrencies(ArrayList<Currency> listCurrencies) {
        ArrayList<Currency> listService = null;
        if(isServiceBound) {
            listService = criptoService.getCurrenciesMonitoringList();
            for (Currency curr : listService) {
                Log.i("CriptoMonitor", "MainActivity(setCheckCurrencies): currency from service " + curr.getName());
            }
        }
        else
            Log.i("CriptoMonitor", "MainActivity(setCheckCurrencies): not bind service");
        boolean flagContains;

        Iterator it = listService.iterator();
        while (it.hasNext())
        {
            Currency curr = (Currency) it.next();
            if (listCurrencies.contains(curr) == false)
                it.remove();
            mainFragment.deleteCurrenyFromList(curr);
        }

        for (Currency curr : listCurrencies) {
            if (listService.contains(curr) == false) {
                //Добавляем новую валюту в список для отслеживания. Границы раздвигаем от текущей цены на 5%
                Currency newCurr = new Currency(curr.getName(), curr.getPrice(), curr.getPrice() * 0.95, curr.getPrice() * 1.05);
                listService.add(newCurr);
                mainFragment.insertCurrencyInList(newCurr);
            }
        }

        listService = criptoService.getCurrenciesMonitoringList();
        for (Currency curr : listService) {
            Log.i("CriptoMonitor", "MainActivity(setCheckCurrencies): 000currency from service " + curr.getName());
        }

        setFragMain();
        mainFragment.updateListAdapter();
    }

    public boolean isServiceStart(){
        if(isServiceBound && criptoService!= null)
            return true;
        else
            return false;
    }
}

