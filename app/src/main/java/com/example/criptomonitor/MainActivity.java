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
                    if (intent.hasExtra(CriptoMonitorService.CRIPTOSERVICE_ERROR))
                        showAlertDialog("Ошибка передачи данных списка валют с сервера = " + intent.getStringExtra(CriptoMonitorService.CRIPTOSERVICE_ERROR));
                    //Если получили список всех валют с ценами, то отправляем его в главный фрагмент и обновляем данные по отслеживаемым валютам
                    if (intent.hasExtra(CriptoMonitorService.CRIPTOSERVICE_LIST)) {
                        ArrayList<Currency> list = intent.getParcelableArrayListExtra(CriptoMonitorService.CRIPTOSERVICE_LIST);
                        mainFragment.setListAllCurrencies(list);
                        mainFragment.updateMonitoringFromDB(criptoService.getCurrenciesMonitoringList());
                        criptoService.setIntervalReload(mainFragment.getIntervalReloadService());
                    }
                    //Если уже заполнили список всех валют, то просто обновляем данные по отслеживаемым валютам
                    if (mainFragment.getListAllCurrencies() != null) {
                        mainFragment.updateListAdapter();
                    }
                }
            };
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        //Если есть действующее соединение с интернетом, то регистрируем ресивер, иначе сообшаем о проблеме
        if (isInternetConnect()) {
            IntentFilter filter = new IntentFilter(CriptoMonitorService.CRIPTOSERVICE_ACTION);
            registerReceiver(serviceReciever, filter);
        } else {
            showAlertDialog("Нет подключения к сети Internet...");
        }
        //Если версия не поддерживает фрагменты, то сообщаем о проблеме
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.DONUT) {
            showAlertDialog("Невозможно запустить программу на версии Android меньше 4.0( Donut)...");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Отсоединяем ресивер
        unregisterReceiver(serviceReciever);
    }

    //Создаем меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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

    //Метод, отвечающий за настройку параметров лэйаутов для фрагментов
    public void updateFragLayout(int mode) {
        Log.i("CriptoMonitor", "MainActivity(updateFragLayout)11");
        FrameLayout frm1 = findViewById(R.id.fragLayout1);
        FrameLayout frm2 = findViewById(R.id.fragLayout2);
        Log.i("CriptoMonitor", "MainActivity(updateFragLayout)22");
        //Для главного фрагмента весь экран отдаем под первый лэйаут в портретной ориентации
        if (mode == DataExchanger.LAYOUT_MAIN) {
            Log.i("CriptoMonitor", "MainActivity(updateFragLayout)33");
            frm1.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 0));
            frm2.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 0));
            Log.i("CriptoMonitor", "MainActivity(updateFragLayout)44");
        }
        //Для фрагмента конкретной валюты весь экран отдаем под второй лэйаут в портретной ориентации
        if (mode == DataExchanger.LAYOUT_RANGE) {
            frm1.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 1));
            frm2.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 0));
        }
        //Если ориентация альбомная, то экран делим пополам для обоих лэйаутов
        if (mode == DataExchanger.LAYOUT_ALL) {
            frm1.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 1));
            frm2.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 1));
        }
    }

    //Метод, отвечающий за отображение фрагмента с настройками сервиса
    private void setFragSettings() {
        Log.i("CriptoMonitor", "MainActivity(setFragSettings)");
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //Если фрагмент еще не создан, то без стека добавляем его и опять скрываем
        //чтобы при нажатии кнопки назад не было пустого фрагмента
        if (settingsFragment == null) {
            settingsFragment = new ServiceSettigsFragment();
            transaction.add(R.id.fragLayout1, settingsFragment, "settingsFragment");
            transaction.hide(settingsFragment);
            transaction.commit();
            transaction = getSupportFragmentManager().beginTransaction();
        }

        //Уже в стеке скрываем главный фрагмент и фрагмент валюты и отображаем с настройками
        transaction.addToBackStack(null);
        if (mainFragment != null && mainFragment.isVisible()) {
            transaction.hide(mainFragment);
        }
        transaction.show(settingsFragment);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if(rangeFragment != null) {
                updateFragLayout(DataExchanger.LAYOUT_MAIN);
                transaction.hide(rangeFragment);
            }
        }
        transaction.commit();
    }

    //Метод, отвечающий за возврат главного фрагмента из дочернего
    //пока без стека скрываем дочерний и отображаем главный фрагмент
    private void setMainFromFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (addFragment != null) {
            if (addFragment.isHidden() == false)
                transaction.hide(addFragment);
        }
        if (settingsFragment != null) {
            if (settingsFragment.isHidden() == false)
                transaction.hide(settingsFragment);
        }
        if (mainFragment.isHidden())
            transaction.show(mainFragment);
        transaction.commit();
    }


    //Первоначальное распределение фрагментов по главной активности
    private void setFragMain() {
        Log.i("CriptoMonitor", "MainActivity(setFragMain)");
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        //Если дочерние активности уже созданы, то при необходимости отображаем их
        //такое возможно при смене ориентации экрана
        if (addFragment != null) {
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

        //Обрабатываем главный фрагмент, если не создан - создаем, иначе просто обрабатываем свойство видимости
        if (mainFragment == null) {
            mainFragment = new MainFragment();
            transaction.add(R.id.fragLayout1, mainFragment, "mainFragment");
        } else {
            if (mainFragment.isHidden())
                transaction.hide(mainFragment);
            else
                transaction.show(mainFragment);
        }

        //Отрабатываем альбомную ориентацию, добавляем если нужно фрагмент с инфой о валюте
        if (mainFragment.isHidden() == false) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (mainFragment != null && mainFragment.getSelectionCurrency() != null) {
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
        }

        transaction.commit();
    }

    //Установка фрагмента со списоком всех возможных валют
    private void setFragAdd() {
        Log.i("CriptoMonitor", "MainActivity(setFragAdd)");
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //Без стека при необходимости создаем фрагмент и добавляем его
        //чтобы при нажатии кнопки назад не появлялся пустой фрагмент
        if (addFragment == null) {
            addFragment = new AddFragment();
            transaction.add(R.id.fragLayout1, addFragment, "addFragment");
            transaction.hide(addFragment);
            transaction.commit();
            transaction = getSupportFragmentManager().beginTransaction();
        }
        //И в стеке
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
        if (criptoService != null && criptoService.isServiceRunning())
            criptoService.setIntervalReload(time);
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

        setMainFromFragment();
        mainFragment.updateListAdapter();
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

    public void setServiceStartForeground(boolean isForeground) {
        if (criptoService != null && criptoService.isServiceRunning())
            criptoService.setServiceForeground(isForeground);
        else
            Log.i("CriptoMonitor", "MainActivity(setCheckCurrencies): service is not running");
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

    //Метод для отображения диалогового окна с сообщением
    public void showAlertDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setTitle("CriptoMonitor").setMessage(message).setCancelable(false).setNegativeButton("ОК",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

}

