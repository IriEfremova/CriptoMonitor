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
import android.support.v4.app.FragmentManager;
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
                            showAlertDialogCancel("Нет подключения к сети Internet...");
                        }
                    }
                    //Если получили список всех валют с ценами, то отправляем его в главный фрагмент и обновляем данные по отслеживаемым валютам
                    //Если получили этот сигнал, значит произошла привязка к работающему сервису
                    if (intent.hasExtra(CriptoMonitorService.CRIPTOSERVICE_LIST)) {
                        ArrayList<Currency> list = intent.getParcelableArrayListExtra(CriptoMonitorService.CRIPTOSERVICE_LIST);
                        mainFragment.setListAllCurrencies(list);
                        mainFragment.updateMonitoringFromDB(criptoService.getCurrenciesMonitoringList());
                        criptoService.setIntervalReload(mainFragment.getIntervalReloadService());
                        criptoService.setServiceForeground(mainFragment.getServiceStartForeground());
                        if(settingsFragment != null)
                            settingsFragment.updateStatusText();
                    }
                    //Если уже заполнили список всех валют, то просто обновляем данные по отслеживаемым валютам
                    if (mainFragment != null && mainFragment.getListAllCurrencies() != null) {
                        mainFragment.updateListAdapter();
                    }
                }
            };
        }
    }

    //На старт активности регистрируем ресивер
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
        //Негде потестировать, поэтому пока здесь
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.DONUT) {
            showAlertDialog("Невозможно запустить программу на версии Android меньше 4.0( Donut)");
        }
    }

    //На останов активности
    @Override
    protected void onStop() {
        super.onStop();
        if (isResieverRegister) {
            //Отсоединяем ресивер
            unregisterReceiver(serviceReciever);
            isResieverRegister = false;
        }
    }

    //При уничтожении активности отвязываем сервим
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
                if (addFragment == null || addFragment.isVisible() == false)
                    setSubFragment(DataExchanger.LAYOUT_ADD);
                return true;
            //Если выбрали пункт "Stop Service"
            case R.id.action_settings:
                if (settingsFragment == null || settingsFragment.isVisible() == false)
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
        Log.i("CriptoMonitor", "MainActivity(setSubFragment)");
        Fragment fragment = null;
        String nameFrag = "";

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        switch (typeLayout) {
            //Для фрагмента со списком валют для добавления
            case DataExchanger.LAYOUT_ADD: {
                if (addFragment == null)
                    addFragment = new AddFragment();
                fragment = addFragment;
                nameFrag = "addFragment";
                break;
            }
            //Для фрагмента с настройкой валюты
            case DataExchanger.LAYOUT_RANGE: {
                if (rangeFragment == null)
                    rangeFragment = new RangeFragment();
                fragment = rangeFragment;
                nameFrag = "rangeFragment";
                break;
            }
            //Для фрагмента с настройками сервиса
            case DataExchanger.LAYOUT_SETTINGS: {
                if (settingsFragment == null)
                    settingsFragment = new ServiceSettigsFragment();
                fragment = settingsFragment;
                nameFrag = "settingsFragment";
                break;
            }
        }

        //Если фрагмент не добавлен, то сделаем это
        if (fragment.isAdded() == false) {
            transaction.add(R.id.fragLayout2, fragment, nameFrag);
            transaction.hide(fragment);
            transaction.commit();
            transaction = getSupportFragmentManager().beginTransaction();
        }

        //Уже в стеке отображаем нужный фрагмент
        transaction.addToBackStack(null);

        //Для портретной ориентации скрываем второй фрагмент с настройками валюты
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            updateFragLayout(DataExchanger.LAYOUT_RANGE);
            if (mainFragment.isVisible())
                transaction.hide(mainFragment);
        }

        //Скрываем ненужные фрагменты перед отображением нужного
        switch (typeLayout) {
            case DataExchanger.LAYOUT_ADD: {
                if (rangeFragment != null)
                    transaction.hide(rangeFragment);
                if (settingsFragment != null)
                    transaction.hide(settingsFragment);
                break;
            }
            case DataExchanger.LAYOUT_RANGE: {
                if (addFragment != null)
                    transaction.hide(addFragment);
                if (settingsFragment != null)
                    transaction.hide(settingsFragment);
                break;
            }
            case DataExchanger.LAYOUT_SETTINGS: {
                if (addFragment != null)
                    transaction.hide(addFragment);
                if (rangeFragment != null)
                    transaction.hide(rangeFragment);
                break;
            }
        }
        //Отображаем фрагмент
        transaction.show(fragment);
        if (typeLayout == DataExchanger.LAYOUT_ADD) {
            ((AddFragment) fragment).updateListAdapter();
        }
        transaction.commit();
    }

    //Первоначальное распределение фрагментов по главной активности
    private void setFragMain() {
        Log.i("CriptoMonitor", "MainActivity(setFragMain)");
        //Если была транзакция, то откатываемся, чтобы при первоначальном отображении очистить стэк
        if (getSupportFragmentManager().getBackStackEntryCount() > 0)
            getSupportFragmentManager().popBackStack();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //Обрабатываем главный фрагмент, если не создан - создаем, иначе просто обрабатываем свойство видимости
        if (mainFragment == null) {
            mainFragment = new MainFragment();
            transaction.add(R.id.fragLayout1, mainFragment, "mainFragment");
        } else
            transaction.show(mainFragment);

        //Для портретно ориентации скрываем второй фрагмент, а для альбомной - показываем
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            updateFragLayout(DataExchanger.LAYOUT_MAIN);
            if (rangeFragment != null) {
                Log.i("CriptoMonitor", "MainActivity(setFragMain): ORIENTATION_PORTRAIT hide");
                transaction.hide(rangeFragment);
            }
            else
                Log.i("CriptoMonitor", "MainActivity(setFragMain): ORIENTATION_PORTRAIT show ");
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            updateFragLayout(DataExchanger.LAYOUT_ALL);
            if (rangeFragment == null) {
                rangeFragment = new RangeFragment();
                transaction.add(R.id.fragLayout2, rangeFragment, "rangeFragment");
                //transaction.hide(rangeFragment);
            }
        }
        transaction.commit();

        if (addFragment != null && addFragment.isHidden() == false) {
            setSubFragment(DataExchanger.LAYOUT_ADD);
            Log.i("CriptoMonitor", "MainActivity(setFragMain): add fragment ");
        } else if (settingsFragment != null && settingsFragment.isHidden() == false)
            setSubFragment(DataExchanger.LAYOUT_SETTINGS);
    }

    //Метод интерфейса для отображение фрагмента с информацией о валюте
    @Override
    public void updateRangeFragment() {
        Log.i("CriptoMonitor", "MainActivity(updateRangeFragment)");
        if (rangeFragment == null || rangeFragment.isVisible() == false)
            setSubFragment(DataExchanger.LAYOUT_RANGE);
        changeSelectionCurrency();
    }

    //Метод интерфейса - возвращает список валют, выбранных для отслеживания
    @Override
    public ArrayList<Currency> getMonitoringCurrencies() {
        if (criptoService != null && isServiceBound) {
            return criptoService.getCurrenciesMonitoringList();
        } else {
            Log.i("CriptoMonitor", "MainActivity(getMonitoringCurrencies): not bind service");
            return null;
        }
    }

    //Метод интерфейса - задает настройки сервиса
    @Override
    public void setServerSettings(int time) {
        if (criptoService != null && criptoService.isServiceRunning()) {
            criptoService.setIntervalReload(time);
            mainFragment.setServiceInterval(time);
        }
    }

    //Метод интерфейса - возвращает список всех возможных валют с веб-сервиса
    @Override
    public ArrayList<Currency> getAllCurrencies() {
        return mainFragment.getListAllCurrencies();
    }

    //Метод интерфейса - удаляет валюту
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

    //Метод интерфейса - устанавливает список валют, выбранных для отслеживания
    @Override
    public void setCheckCurrencies(ArrayList<Currency> listCurrencies) {
        //Если отменили действие, то просто откатываемся назад
        if (listCurrencies == null) {
            getSupportFragmentManager().popBackStack();
        } else {
            ArrayList<Currency> listService = null;
            //Удаляем те, с которых сняли выбор
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

                //Добавляем те, которые выбрали
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
            //Закрываем фрагмент со списком и обновляем список отслеживаемых валют
            getSupportFragmentManager().popBackStack();
            mainFragment.updateListAdapter();
        }
    }

    //Метод интерфейса - возвращает признак работы сервиса
    @Override
    public int isServiceStart() {
        if (criptoService != null && criptoService.isServiceRunning())
            return criptoService.getIntervalReload();
        else
            return -1;
    }

    //Метод интерфейса - возвращает признак режима работы сервиса
    @Override
    public boolean isServiceStartForeground() {
        if (criptoService != null && criptoService.isServiceRunning())
            return criptoService.isServiceForeground();
        else
            return false;
    }

    //Метод интерфейса - устанавливаем признак режима работы сервиса
    @Override
    public void setServiceStartForeground(int isForeground) {
        mainFragment.setServiceStartForeground(isForeground);
        if (criptoService != null && criptoService.isServiceRunning())
            criptoService.setServiceForeground(isForeground == 0 ? false : true);
        else
            Log.i("CriptoMonitor", "MainActivity(setServiceStartForeground): service is not running");
    }

    //Метод интерфейса на смену текущей валюты
    @Override
    public void changeSelectionCurrency() {
        Log.i("CriptoMonitor", "MainActivity(changeSelectionCurrency)");
        if (rangeFragment != null) {
            if (rangeFragment.getSelectionCurrency() != mainFragment.getSelectionCurrency())
                rangeFragment.setSelectionCurrency(mainFragment.getSelectionCurrency());
        }
    }

    //Метод для отображения диалогового окна с сообщением
    @Override
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

    //Метод для отображения диалогового окна с сообщением и возможностью выйти из приложения
    public void showAlertDialogCancel(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("CriptoMonitor").setMessage(message).setCancelable(false).setNegativeButton(R.string.repeat,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        if (serviceConnection != null && isServiceBound) {
                            unbindService(serviceConnection);
                            isServiceBound = false;
                        }
                        bindService(intentService, serviceConnection, BIND_AUTO_CREATE);
                    }
                }).setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                criptoService.stopService(intentService);
                finish();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    //Метод интерфейса - обновляет валюту
    @Override
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

