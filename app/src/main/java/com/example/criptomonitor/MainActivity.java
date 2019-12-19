package com.example.criptomonitor;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
    ArrayList<Currency> listAllCurrencies;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CriptoMonitorService.LocalBinder binder = (CriptoMonitorService.LocalBinder)iBinder;
            criptoService = binder.getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
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

                    if(intent.hasExtra(CriptoMonitorService.CRIPTOSERVICE_ERROR)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("CriptoMonitor").setMessage("Ошибка передачи данных списка валют с сервера = " + intent.getStringExtra(CriptoMonitorService.CRIPTOSERVICE_ERROR))
                                .setCancelable(false).setNegativeButton("ОК",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                    if(intent.hasExtra(CriptoMonitorService.CRIPTOSERVICE_LIST)) {
                        listAllCurrencies = intent.getParcelableArrayListExtra(CriptoMonitorService.CRIPTOSERVICE_LIST);
                    }
                    if(getMonitoringCurrencies() != mainFragment.getCurrenciesMonitoringList()) {
                        setMonitoringCurrencies(mainFragment.getCurrenciesMonitoringList());
                        criptoService.updateTimer(mainFragment.getIntervalReloadService());
                    }
                    mainFragment.updateListAdapter();
                }
            };
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(CriptoMonitorService.CRIPTOSERVICE_ACTION);
        registerReceiver(serviceReciever, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
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
        Log.i("CriptoMonitor", "MainActivity(setFragMain)");
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //transaction.addToBackStack(null);

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
        Log.i("CriptoMonitor", "MainActivity(setFragAdd)");
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (addFragment == null) {
            addFragment = new AddFragment();
            transaction.add(R.id.fragLayout1, addFragment, "addFragment");
            transaction.hide(addFragment);
            transaction.commit();
            transaction = getSupportFragmentManager().beginTransaction();
        }

        transaction.addToBackStack(null);
        if(mainFragment != null && mainFragment.isVisible())
            transaction.hide(mainFragment);
        transaction.show(addFragment);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            transaction.hide(rangeFragment);
        transaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isServiceBound) {
           unbindService(serviceConnection);
            isServiceBound = false;
        }
        serviceReciever = null;
        intentService = null;
        for(Currency curr : listAllCurrencies)
            curr = null;
        listAllCurrencies = null;
    }

    @Override
    public ArrayList<Currency> getMonitoringCurrencies() {
        if(criptoService != null && isServiceBound) {

            ArrayList<Currency> listService = criptoService.getCurrenciesMonitoringList();
            return criptoService.getCurrenciesMonitoringList();
        }
        else {
           Log.i("CriptoMonitor", "MainActivity(getMonitoringCurrencies): not bind service");
           return null;
        }
    }

    public void updateListAllCurrencies(Currency currency, boolean clear){
        for (int i = 0; i < listAllCurrencies.size(); i++) {
            Currency curr = listAllCurrencies.get(i);
            if (curr.equals(currency) == true) {
                if (clear) {
                    curr.setMaxPrice(-1.0);
                    curr.setMinPrice(-1.0);
                } else {
                    curr.setMaxPrice(currency.getMaxPrice());
                    curr.setMinPrice(currency.getMinPrice());
                }
                break;
            }
        }
    }

    @Override
    public void setMonitoringCurrencies(ArrayList<Currency> listCurrencies) {
        if(criptoService != null && isServiceBound) {
            criptoService.setCurrenciesMonitoringList(listCurrencies);
            for(Currency curr : listCurrencies)
                updateListAllCurrencies(curr,false);
        }
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
        return listAllCurrencies;
    }

    @Override
    public void setCheckCurrencies(ArrayList<Currency> listCurrencies) {
        ArrayList<Currency> listService = null;
        if(isServiceBound) {
            listService = criptoService.getCurrenciesMonitoringList();
        }
        else
            Log.i("CriptoMonitor", "MainActivity(setCheckCurrencies): not bind service");
        boolean flagContains;

        Iterator it = listService.iterator();
        while (it.hasNext())
        {
            Currency curr = (Currency) it.next();
            if (listCurrencies.contains(curr) == false) {
                updateListAllCurrencies(curr, true);
                it.remove();
                mainFragment.deleteCurrenyFromList(curr);
            }
        }

        for (Currency curr : listCurrencies) {
            if (listService.contains(curr) == false) {
                //Добавляем новую валюту в список для отслеживания. Границы раздвигаем от текущей цены на 5%
                Currency newCurr = new Currency(curr.getName(), curr.getPrice(), curr.getPrice() * 0.95, curr.getPrice() * 1.05);
                listService.add(newCurr);
                mainFragment.insertCurrencyInList(newCurr);
                updateListAllCurrencies(newCurr, false);
            }
        }

        listService = criptoService.getCurrenciesMonitoringList();
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

