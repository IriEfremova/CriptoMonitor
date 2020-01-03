package com.example.criptomonitor;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;


public class MainFragment extends Fragment {
    //Пункты контекстного меню
    private final int SET_RANGE = 1;
    private final int DELETE = 2;
    //Визуальный список для отображения списка валют, которые мониторим
    private ListView listView;
    //Объект работы с БД
    private DBConnection dbHelper;
    //Интерфейс обмена данными между активностью и фрагментом
    private DataExchanger listenerListCurrencies;
    private ItemAdapter adapter;
    private ProgressBar progressBar;
    private ArrayList<Currency> listAllCurrencies;

    //Создание фрагмента, задаем флаг, чтобы фрагмент не уничтожался при смене ориентации
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "MainFragment(onCreate)");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        // создаем объект для работы с БД
        dbHelper = dbHelper.getInstance(getActivity());
    }

    @Override
    public void onDestroy() {
        Log.i("CriptoMonitor", "MainFragment(onDestroy)");
        super.onDestroy();
        dbHelper.closeDB();
        dbHelper.close();
        dbHelper = null;
        if (listAllCurrencies != null) {
            listAllCurrencies.clear();
            listAllCurrencies = null;
        }
    }

    //Привязка к активности, проверяем, что активность имплементирует нужный интерфейс
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i("CriptoMonitor", "MainFragment(onAttach)");
        if (context instanceof DataExchanger) {
            listenerListCurrencies = (DataExchanger) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement DataExchanger");
        }
    }

    //Геттер и сеттер для списка всех возможных валют от веб-сервиса
    public ArrayList<Currency> getListAllCurrencies() {
        return listAllCurrencies;
    }

    public void setListAllCurrencies(ArrayList<Currency> list) {
        if (listAllCurrencies != null) {
            for(Currency currency : list){
                int ind = listAllCurrencies.indexOf(currency);
                if(ind >= 0) {
                    listAllCurrencies.get(ind).replace(currency);
                    Log.i("CriptoMonitor", "MainFragment(setListAllCurrencies): replace" + currency.getName());
                }
                else {
                    listAllCurrencies.add(currency);
                    Log.i("CriptoMonitor", "MainFragment(setListAllCurrencies): add" + currency.getName());
                }
            }
        }
        else
            listAllCurrencies = list;
    }

    //Метод, сохраняющий в БД интервал обращения сервиса к веб-сервису
    public int getIntervalReloadService() {
        return dbHelper.getSettings(DBConnection.nameInterval);
    }

    //Метод для записи в БД режим работы сервиса
    public void setServiceStartForeground(int isForeground) {
        dbHelper.insertSettings(DBConnection.nameForeground, isForeground);
    }

    //Метод для записи в БД интервал работы сервиса
    public void setServiceInterval(int interval) {
        dbHelper.insertSettings(DBConnection.nameInterval, interval);
    }

    //Метод,возвращающий режим работы сервиса, сохраненный в БД
    public boolean getServiceStartForeground() {
        if( dbHelper.getSettings(DBConnection.nameForeground) == 0 )
            return false;
        else
            return true;
    }


    //Метод на создание вью фрагмента
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "MainFragment(onCreateView)");
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        listView = (ListView) view.findViewById(R.id.myListView);

        //Обработчик нажатия на элемент списка, вызываем метод смены текущей валюты интерфейса
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((ItemAdapter) (listView.getAdapter())).setSelectionPosition(position);
                listenerListCurrencies.changeSelectionCurrency();
            }
        });

        //Обработчик долгого нажатия на элемент списка, вызываем метод смены текущей валюты интерфейса
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
                ((ItemAdapter) (listView.getAdapter())).setSelectionPosition(position);
                listenerListCurrencies.changeSelectionCurrency();
                return false;
            }
        });
        //Регистрируем контекстное меню
        registerForContextMenu(listView);
        updateListAdapter();
        return view;
    }

    //Обновляем список отслеживаемых валют от сервера данными из БД
    public void updateMonitoringFromDB(ArrayList<Currency> serviceList) {
        dbHelper.updateListMonitoringCurrencies(serviceList);
        if (listAllCurrencies != null && listAllCurrencies.size() > 0) {
            for (int i = 0; i < serviceList.size(); i++) {
                Currency currMnt = serviceList.get(i);
                for (int j = 0; j < listAllCurrencies.size(); j++) {
                    Currency curr = listAllCurrencies.get(j);
                    if (curr.equals(currMnt) == true) {
                        curr.setMaxPrice(currMnt.getMaxPrice());
                        curr.setMinPrice(currMnt.getMinPrice());
                        break;
                    }
                }
            }
        }
    }

    //Метод удаления валюты из списка отслеживаемых и БД
    public void deleteCurrenyFromList(Currency currency) {
        dbHelper.deleteCurrency(currency);

        for (int i = 0; i < listAllCurrencies.size(); i++) {
            Currency curr = listAllCurrencies.get(i);
            if (curr.equals(currency) == true) {
                curr.setMaxPrice(-1.0);
                curr.setMinPrice(-1.0);
                break;
            }
        }
    }

    //Метод вставки валюты в список отслеживаемых и запись в БД
    public void insertCurrencyInList(Currency currency) {
        dbHelper.insertCurrency(currency);

        for (int i = 0; i < listAllCurrencies.size(); i++) {
            Currency curr = listAllCurrencies.get(i);
            if (curr.equals(currency) == true) {
                curr.setMaxPrice(currency.getMaxPrice());
                curr.setMinPrice(currency.getMinPrice());
                break;
            }
        }

    }

    //Метод обновления валюты в БД
    public void updateCurrency(Currency currency) {
        dbHelper.updateCurrency(currency);
    }

    //Метод обновления данных в адаптере визуального списка
    public void updateListAdapter() {
        Log.i("CriptoMonitor", "MainFragment(updateListAdapter)");
        if (listenerListCurrencies.getMonitoringCurrencies() != null) {
            if (adapter == null)
                adapter = new ItemAdapter(listView.getContext(), listenerListCurrencies.getMonitoringCurrencies());
            if (listView.getAdapter() == null)
                // устанавливаем для списка адаптер
                listView.setAdapter(adapter);
            else
                ((ItemAdapter) listView.getAdapter()).notifyDataSetChanged();
            listenerListCurrencies.changeSelectionCurrency();
        }
        progressBar.setVisibility(ProgressBar.INVISIBLE);
    }

    //Метод, возвращающий текущую валюту
    public Currency getSelectionCurrency() {
        if (listView != null && listView.getAdapter() != null && listView.getAdapter().getCount() > 0) {
            int pos = ((ItemAdapter) (listView.getAdapter())).getSelectionPosition();
            return (Currency) listView.getItemAtPosition(pos);
        } else
            return null;
    }

    //Создание контекстного меню для элемента списка
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        menu.add(0, SET_RANGE, 0, "Set trackable range");
        menu.add(0, DELETE, 0, "Delete currencies from list");
    }

    //Выбор пункта контекстного меню
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // пункты контекстного меню
            case SET_RANGE:
                listenerListCurrencies.updateRangeFragment();
                break;
            case DELETE:
                listenerListCurrencies.deleteCurrency(getSelectionCurrency());
                break;
        }
        return super.onContextItemSelected(item);
    }


    //При изменении признака отображения фрагмента, обновляем разметку
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden == false) {
            Log.i("CriptoMonitor", "MainFragment(onHiddenChanged)");
            listenerListCurrencies.updateFragLayout(DataExchanger.LAYOUT_MAIN);
        }
    }
}




