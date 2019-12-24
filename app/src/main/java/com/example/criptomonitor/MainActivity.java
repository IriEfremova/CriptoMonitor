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
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements DataExchanger {
    private MainFragment mainFragment;
    private RangeFragment rangeFragment;
    private AddFragment addFragment;
    private ServiceSettigsFragment settingsFragment;
    //Флаг привязки сервиса к активности
    private boolean isServiceBound = false;
    private boolean isGetAllData = false;
    private CriptoMonitorService criptoService;
    private Intent intentService;
    //Объект-приемник событий от сервиса
    private BroadcastReceiver serviceReciever;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CriptoMonitorService.LocalBinder binder = (CriptoMonitorService.LocalBinder) iBinder;
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
        if (intentService == null)
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

        if (serviceReciever == null) {
            serviceReciever = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.i("CriptoMonitor", "BroadcastReceiver(onReceive)");

                    if (intent.hasExtra(CriptoMonitorService.CRIPTOSERVICE_ERROR)) {
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
                    if (intent.hasExtra(CriptoMonitorService.CRIPTOSERVICE_LIST)) {
                        Log.i("CriptoMonitor", "BroadcastReceiver(onReceive): CRIPTOSERVICE_LIST");
                        ArrayList<Currency> list = intent.getParcelableArrayListExtra(CriptoMonitorService.CRIPTOSERVICE_LIST);
                        mainFragment.setListAllCurrencies(list);
                        mainFragment.updateMonitoringFromDB(criptoService.getCurrenciesMonitoringList());
                        criptoService.updateTimer(mainFragment.getIntervalReloadService());
                        isGetAllData = true;
                    }
                    if (mainFragment.getListAllCurrencies() != null) {
                        Log.i("CriptoMonitor", "BroadcastReceiver(onReceive): updateListAdapter");
                        mainFragment.updateListAdapter();
                    }
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
                setFragSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setFragSettings() {
        Log.i("CriptoMonitor", "MainActivity(setFragSettings)");
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (settingsFragment == null) {
            settingsFragment = new ServiceSettigsFragment();
            transaction.add(R.id.fragLayout1, settingsFragment, "settingsFragment");
            transaction.hide(settingsFragment);
            transaction.commit();
            transaction = getSupportFragmentManager().beginTransaction();
        }

        transaction.addToBackStack(null);
        if (mainFragment != null && mainFragment.isVisible())
            transaction.hide(mainFragment);
        transaction.show(settingsFragment);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            updateFragLayout(DataExchanger.LAYOUT_MAIN);
            transaction.hide(rangeFragment);
        }
        transaction.commit();


    }

    public void updateFragLayout(int mode) {
        FrameLayout frm1 = findViewById(R.id.fragLayout1);
        FrameLayout frm2 = findViewById(R.id.fragLayout2);

        if (mode == DataExchanger.LAYOUT_MAIN) {
            frm1.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 0));
            frm2.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 0));
        }
        if (mode == DataExchanger.LAYOUT_RANGE) {
            frm1.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 1));
            frm2.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 0));
        }
        if (mode == DataExchanger.LAYOUT_ALL) {
            frm1.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 1));
            frm2.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 1));
        }
    }

    private void setFragMain() {
        Log.i("CriptoMonitor", "MainActivity(setFragMain)");
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if(addFragment != null) {
            if (addFragment.isHidden())
                transaction.hide(addFragment);
            else
                transaction.show(addFragment);
        }

        if (settingsFragment != null) {
            if (settingsFragment.isHidden())
                transaction.hide(settingsFragment);
            else
                transaction.show(settingsFragment);
        }

        if (mainFragment == null) {
            mainFragment = new MainFragment();
            transaction.add(R.id.fragLayout1, mainFragment, "mainFragment");
        } else {
            if(mainFragment.isHidden())
                transaction.hide(mainFragment);
            else
                transaction.show(mainFragment);
        }

        if(mainFragment.isHidden() == false) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                updateFragLayout(DataExchanger.LAYOUT_ALL);
                if (rangeFragment == null) {
                    rangeFragment = new RangeFragment();
                    transaction.add(R.id.fragLayout2, rangeFragment, "rangeFragment");
                } else
                    transaction.show(rangeFragment);
            } else {
                updateFragLayout(DataExchanger.LAYOUT_MAIN);
                if (rangeFragment != null && rangeFragment.isHidden())
                    transaction.hide(rangeFragment);
            }
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
        if (mainFragment != null && mainFragment.isVisible())
            transaction.hide(mainFragment);
        transaction.show(addFragment);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            updateFragLayout(DataExchanger.LAYOUT_MAIN);
            transaction.hide(rangeFragment);
        }
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
        if (criptoService != null && isServiceBound) {
            return criptoService.getCurrenciesMonitoringList();
        } else {
            Log.i("CriptoMonitor", "MainActivity(getMonitoringCurrencies): not bind service");
            return null;
        }
    }

    @Override
    public void setServerSettings(int time, boolean turnOn) {
        if (time == 0) {
            if (turnOn == false) {
                if (isServiceBound && criptoService != null) {
                    unregisterReceiver(serviceReciever);
                    unbindService(serviceConnection);
                    isServiceBound = false;
                    stopService(intentService);
                } else
                    Log.i("CriptoMonitor", "MainActivity(getMonitoringCurrencies): not bind service");
            } else {
                startService(intentService);
                if (isServiceBound == false) {
                    bindService(intentService, serviceConnection, BIND_AUTO_CREATE);
                    isServiceBound = true;
                } else
                    Log.i("CriptoMonitor", "MainActivity(getMonitoringCurrencies): service is already bind");
                IntentFilter filter = new IntentFilter(CriptoMonitorService.CRIPTOSERVICE_ACTION);
                registerReceiver(serviceReciever, filter);
            }
        } else {
            criptoService.updateTimer(time);
        }
    }

    @Override
    public ArrayList<Currency> getAllCurrencies() {
        return mainFragment.getListAllCurrencies();
    }

    @Override
    public void deleteCurrency(Currency currency) {
        ArrayList<Currency> listService = null;
        if (isServiceBound) {
            listService = criptoService.getCurrenciesMonitoringList();
            Iterator it = listService.iterator();
            while (it.hasNext()) {
                Currency curr = (Currency) it.next();
                if (currency.equals(curr)) {
                    it.remove();
                    mainFragment.deleteCurrenyFromList(curr);
                }
            }
        } else
            Log.i("CriptoMonitor", "MainActivity(setCheckCurrencies): not bind service");

    }

    @Override
    public void setCheckCurrencies(ArrayList<Currency> listCurrencies) {
        ArrayList<Currency> listService = null;
        if (isServiceBound) {
            listService = criptoService.getCurrenciesMonitoringList();
            Iterator it = listService.iterator();
            while (it.hasNext()) {
                Currency curr = (Currency) it.next();
                if (listCurrencies.contains(curr) == false) {
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
                }
            }

        } else
            Log.i("CriptoMonitor", "MainActivity(setCheckCurrencies): not bind service");

        setFragMain();
        mainFragment.updateListAdapter();
    }

    public int isServiceStart() {
        if (isServiceBound && criptoService != null)
            return criptoService.getIntervalReload();
        else
            return -1;
    }

    @Override
    public void updateRange() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (rangeFragment == null) {
                rangeFragment = new RangeFragment();
                transaction.add(R.id.fragLayout2, rangeFragment, "rangeFragment");
                transaction.hide(rangeFragment);
                transaction.commit();
                transaction = getSupportFragmentManager().beginTransaction();
            }
            transaction.addToBackStack(null);
            transaction.hide(mainFragment);
            updateFragLayout(DataExchanger.LAYOUT_RANGE);
            transaction.show(rangeFragment);
            changeSelectionCurrency();
            transaction.commit();
        }
    }

    @Override
    public void changeSelectionCurrency() {
        if (rangeFragment != null) {
            rangeFragment.setSelectionCurrency(mainFragment.getSelectionCurrency());
        }
    }
}

