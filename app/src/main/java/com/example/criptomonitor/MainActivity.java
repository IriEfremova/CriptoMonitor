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
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements DataExchanger {
    //Фрагменты
    private MainFragment mainFragment;
    private RangeFragment rangeFragment;
    private AddFragment addFragment;
    private ServiceSettigsFragment settingsFragment;

    //Сервис
    private CriptoMonitorService criptoService;
    //Флаг привязки сервиса к активности
    private boolean isServiceBound = false;
    //Флаг привязки ресивера к сервису
    private boolean isResieverRegister = false;

    //Намерение для сервиса
    private Intent intentService;
    //Приемник событий от сервиса
    private BroadcastReceiver serviceReciever;

    //Создаем соединение с сервисом
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

    //Метод, проверяющий действующее соединение с интернетом
    public boolean isInternetConnect() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    //Создаем окно главной активности
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "MainActivity(onCreate)");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Запускаем сервис и получаем привязку к нему
        if (intentService == null)
            intentService = new Intent(this, CriptoMonitorService.class);
        startService(intentService);
        if (isServiceBound == false) {
            Log.i("CriptoMonitor", "MainActivity(onCreate): bindService");
            bindService(intentService, serviceConnection, BIND_AUTO_CREATE);
        }

        //Создаем фрагменты
        mainFragment = (MainFragment) getSupportFragmentManager().findFragmentByTag("mainFragment");
        rangeFragment = (RangeFragment) getSupportFragmentManager().findFragmentByTag("rangeFragment");
        addFragment = (AddFragment) getSupportFragmentManager().findFragmentByTag("addFragment");
        settingsFragment = (ServiceSettigsFragment) getSupportFragmentManager().findFragmentByTag("settingsFragment");
        setFragMain();

        //Создаем приемник для обработки событий сервиса
        if (serviceReciever == null) {
            serviceReciever = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.i("CriptoMonitor", "BroadcastReceiver(onReceive)");
                    //Если пришла ошибка, то сообщаем об этом
                    if (intent.hasExtra(CriptoMonitorService.CRIPTOSERVICE_ERROR)) {
                        //Если есть действующее соединение с интернетом, то регистрируем ресивер, иначе сообшаем о проблеме
                        if (isInternetConnect()) {
                            showAlertDialog("Ошибка передачи данных списка валют с сервера = " + intent.getStringExtra(CriptoMonitorService.CRIPTOSERVICE_ERROR));
                        } else {
                            showAlertDialog("Нет подключения к сети Internet...");
                        }
                    }
                    //Если получили список всех валют с ценами, то отправляем его в главный фрагмент и обновляем данные по отслеживаемым валютам
                    if (intent.hasExtra(CriptoMonitorService.CRIPTOSERVICE_LIST)) {
                        ArrayList<Currency> list = intent.getParcelableArrayListExtra(CriptoMonitorService.CRIPTOSERVICE_LIST);
                        mainFragment.setListAllCurrencies(list);
                        mainFragment.updateMonitoringFromDB(criptoService.getCurrenciesMonitoringList());
                        criptoService.setIntervalReload(mainFragment.getIntervalReloadService());
                        criptoService.setServiceForeground(mainFragment.getServiceStartForeground());
                    }
                    //Если уже заполнили список всех валют, то просто обновляем данные по отслеживаемым валютам
                    if (mainFragment != null && mainFragment.getListAllCurrencies() != null) {
                        mainFragment.updateListAdapter();
                    }
                }
            };
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("CriptoMonitor", "MainActivity(onStart)");
        if (serviceReciever != null && isResieverRegister == false) {
            IntentFilter filter = new IntentFilter(CriptoMonitorService.CRIPTOSERVICE_ACTION);
            registerReceiver(serviceReciever, filter);
            isResieverRegister = true;
        }
        //Если версия не поддерживает фрагменты, то сообщаем о проблеме
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.DONUT) {
            showAlertDialog("Невозможно запустить программу на версии Android меньше 4.0( Donut)");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isResieverRegister) {
            //Отсоединяем ресивер
            unregisterReceiver(serviceReciever);
            isResieverRegister = false;
        }
    }

    //Создаем меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.DONUT) {
            MenuItem item = menu.findItem(R.id.action_new);
            item.setEnabled(false);
            item = menu.findItem(R.id.action_settings);
            item.setEnabled(false);
        }

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //Метод, обрабатывающий выбор пункта меню
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            //Если выбрали пункт "Add new Currencies"
            case R.id.action_new:
                if (addFragment == null || addFragment.isHidden() == true)
                    setSubFragment(DataExchanger.LAYOUT_ADD);
                return true;
            //Если выбрали пункт "Stop Service"
            case R.id.action_settings:
                if (settingsFragment == null || settingsFragment.isHidden() == true)
                    setSubFragment(DataExchanger.LAYOUT_SETTINGS);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    //Метод, отвечающий за настройку параметров лэйаутов для фрагментов
    public void updateFragLayout(int mode) {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && mode != DataExchanger.LAYOUT_ALL)
            return;

        FrameLayout frm1 = findViewById(R.id.fragLayout1);
        FrameLayout frm2 = findViewById(R.id.fragLayout2);
        //Для портретной ориентации весь экран отдаем под первый лэйаут
        if (mode == DataExchanger.LAYOUT_MAIN) {
            frm1.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 0));
            frm2.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 0));
        }
        //Если ориентация альбомная, то экран делим пополам для обоих лэйаутов
        else if (mode == DataExchanger.LAYOUT_ALL) {
            frm1.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 1));
            frm2.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 1));
        } else {
            frm1.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 1));
            frm2.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 0));
        }
    }

    //Метод, отвечающий за отображение дочернего фрагмента
    private void setSubFragment(int typeLayout) {
        Log.i("CriptoMonitor", "MainActivity(setSubFragment) " + getSupportFragmentManager().getBackStackEntryCount());
        Fragment fragment = null;
        String nameFrag = "";

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        switch (typeLayout) {
            case DataExchanger.LAYOUT_ADD: {
                if (addFragment == null) {
                    Log.i("CriptoMonitor", "MainActivity(setSubFragment):create new addfragment ");
                    addFragment = new AddFragment();
                }
                fragment = addFragment;
                nameFrag = "addFragment";
                break;
            }
            case DataExchanger.LAYOUT_RANGE: {
                if (rangeFragment == null)
                    rangeFragment = new RangeFragment();
                fragment = rangeFragment;
                nameFrag = "rangeFragment";
                break;
            }
            case DataExchanger.LAYOUT_SETTINGS: {
                if (settingsFragment == null)
                    settingsFragment = new ServiceSettigsFragment();
                fragment = settingsFragment;
                nameFrag = "settingsFragment";
                break;
            }
        }

        if (fragment.isAdded() == false) {
            Log.i("CriptoMonitor", "MainActivity(setSubFragment): added");
            transaction.add(R.id.fragLayout2, fragment, nameFrag);
            transaction.hide(fragment);
            transaction.commit();
            transaction = getSupportFragmentManager().beginTransaction();
        }

        //Уже в стеке отображаем нужный фрагмент
        transaction.addToBackStack(null);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            updateFragLayout(DataExchanger.LAYOUT_RANGE);
            if (mainFragment.isHidden() == false)
                transaction.hide(mainFragment);
        }

        switch (typeLayout) {
            case DataExchanger.LAYOUT_ADD: {
                if (rangeFragment != null && rangeFragment.isHidden() == false) {
                    transaction.hide(rangeFragment);
                }
                if (settingsFragment != null && settingsFragment.isHidden() == false)
                    transaction.hide(settingsFragment);
                break;
            }
            case DataExchanger.LAYOUT_RANGE: {
                if (addFragment != null && addFragment.isHidden() == false)
                    transaction.hide(addFragment);
                if (settingsFragment != null && settingsFragment.isHidden() == false)
                    transaction.hide(settingsFragment);
                break;
            }
            case DataExchanger.LAYOUT_SETTINGS: {
                if (addFragment != null && addFragment.isHidden() == false)
                    transaction.hide(addFragment);
                if (rangeFragment != null && rangeFragment.isHidden() == false)
                    transaction.hide(rangeFragment);
                break;
            }
        }
        transaction.show(fragment);
        if (typeLayout == DataExchanger.LAYOUT_ADD) {
            ((AddFragment) fragment).updateListAdapter();
        }
        transaction.commit();
        Log.i("CriptoMonitor", "MainActivity(setSubFragment) end" + getSupportFragmentManager().getBackStackEntryCount());
    }

    //Первоначальное распределение фрагментов по главной активности
    private void setFragMain() {
        Log.i("CriptoMonitor", "MainActivity(setFragMain) " + getSupportFragmentManager().getBackStackEntryCount());
        if (getSupportFragmentManager().getBackStackEntryCount() > 0)
            getSupportFragmentManager().popBackStack();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //Обрабатываем главный фрагмент, если не создан - создаем, иначе просто обрабатываем свойство видимости
        if (mainFragment == null) {
            mainFragment = new MainFragment();
            transaction.add(R.id.fragLayout1, mainFragment, "mainFragment");
        } else
            transaction.show(mainFragment);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            updateFragLayout(DataExchanger.LAYOUT_MAIN);
            if (rangeFragment != null && rangeFragment.isHidden() == false)
                transaction.hide(rangeFragment);
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            updateFragLayout(DataExchanger.LAYOUT_ALL);
            if (rangeFragment == null) {
                rangeFragment = new RangeFragment();
                transaction.add(R.id.fragLayout2, rangeFragment, "rangeFragment");
            } else
                transaction.show(rangeFragment);
        }
        transaction.commit();

        if (addFragment != null && addFragment.isHidden() == false) {
            setSubFragment(DataExchanger.LAYOUT_ADD);
            Log.i("CriptoMonitor", "MainActivity(setFragMain): add fragment ");
        } else if (settingsFragment != null && settingsFragment.isHidden() == false)
            setSubFragment(DataExchanger.LAYOUT_SETTINGS);

    }

    @Override
    public void updateRangeFragment() {
        if (rangeFragment == null || rangeFragment.isVisible() == false)
            setSubFragment(DataExchanger.LAYOUT_RANGE);
        changeSelectionCurrency();
    }

    @Override
    protected void onDestroy() {
        Log.i("CriptoMonitor", "MainActivity(onDestroy)");
        super.onDestroy();

        if (isServiceBound) {
            Log.i("CriptoMonitor", "MainActivity(onDestroy): unbindService");
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
    public void setServerSettings(int time) {
        if (criptoService != null && criptoService.isServiceRunning()) {
            criptoService.setIntervalReload(time);
            mainFragment.setServiceInterval(time);
        }
    }

    @Override
    public ArrayList<Currency> getAllCurrencies() {
        Log.i("CriptoMonitor", "MainActivity(getAllCurrencies): count = " + mainFragment.getListAllCurrencies().size());
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
            Log.i("CriptoMonitor", "MainActivity(deleteCurrency): not bind service");

    }

    @Override
    public void setCheckCurrencies(ArrayList<Currency> listCurrencies) {
        if (listCurrencies == null) {
            getSupportFragmentManager().popBackStack();
        } else {

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
                        //Добавляем новую валюту в список для отслеживания. Границы  раздвигаем от текущей цены на 5%
                        Currency newCurr = new Currency(curr.getName(), curr.getPrice(), curr.getPrice() * 0.95, curr.getPrice() * 1.05);
                        listService.add(newCurr);
                        mainFragment.insertCurrencyInList(newCurr);
                        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && rangeFragment != null && rangeFragment.isHidden()) {
                            getSupportFragmentManager().beginTransaction().show(rangeFragment).commit();
                        }
                    }
                }

            } else
                Log.i("CriptoMonitor", "MainActivity(setCheckCurrencies): not bind service");

            getSupportFragmentManager().popBackStack();
            mainFragment.updateListAdapter();
        }
    }

    public int isServiceStart() {
        if (criptoService != null && criptoService.isServiceRunning())
            return criptoService.getIntervalReload();
        else
            return -1;
    }

    public boolean isServiceStartForeground() {
        if (criptoService != null && criptoService.isServiceRunning())
            return criptoService.isServiceForeground();
        else
            return false;
    }

    public void setServiceStartForeground(int isForeground) {
        mainFragment.setServiceStartForeground(isForeground);
        if (criptoService != null && criptoService.isServiceRunning())
            criptoService.setServiceForeground(isForeground == 0 ? false : true);
        else
            Log.i("CriptoMonitor", "MainActivity(setServiceStartForeground): service is not running");
    }

    @Override
    public void changeSelectionCurrency() {
        if (rangeFragment != null) {
            if (mainFragment.getSelectionCurrency() == null)
                getSupportFragmentManager().beginTransaction().hide(rangeFragment).commit();
            else
                rangeFragment.setSelectionCurrency(mainFragment.getSelectionCurrency());
        }
    }

    //Метод для отображения диалогового окна с сообщением
    public void showAlertDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("CriptoMonitor").setMessage(message).setCancelable(false).setNegativeButton("ОК",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void updateCurrency(Currency currency) {
        if (currency == null) {
            //Возвращаемся назад, ели портретная ориентация
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                getSupportFragmentManager().popBackStack();
        } else {
            if (mainFragment != null) {
                mainFragment.updateCurrency(currency);
            }
        }
    }
}

