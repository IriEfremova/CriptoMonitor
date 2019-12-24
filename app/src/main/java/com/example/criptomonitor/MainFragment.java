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
    private MainFragment fragmentMain;

    //Пункты контекстного меню
    private final int SET_RANGE = 1;
    private final int DELETE = 2;
    //Визуальный список для отображения списка валют, которые мониторим
    private ListView listView;
    //Объект работы с БД
    private DBConnection dbHelper;
    private DataExchanger listenerListCurrencies;
    private ItemAdapter adapter;
    private ProgressBar progressBar;
    private ArrayList<Currency> listAllCurrencies;

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

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public ArrayList<Currency> getListAllCurrencies() {
        return listAllCurrencies;
    }

    public void setListAllCurrencies(ArrayList<Currency> list) {
        listAllCurrencies = list;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "MainFragment(onCreateView)");
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        listView = (ListView) view.findViewById(R.id.myListView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((ItemAdapter) (listView.getAdapter())).setSelectionPosition(position);
                listenerListCurrencies.changeSelectionCurrency();
            }
        });

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

        if(adapter != null){
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            listView.setAdapter(adapter);
            ((ItemAdapter) listView.getAdapter()).notifyDataSetChanged();
            listenerListCurrencies.changeSelectionCurrency();
        }
        return view;
    }

    public void updateMonitoringFromDB(ArrayList<Currency> serviceList) {
        dbHelper.updateListMonitoringCurrencies(serviceList);
        if(listAllCurrencies != null && listAllCurrencies.size() > 0) {
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
        fragmentMain = null;
        if(listAllCurrencies != null) {
            listAllCurrencies.clear();
            listAllCurrencies = null;
        }
    }

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

    public int getIntervalReloadService() {
        return dbHelper.getInterval();
    }

    public void updateListAdapter() {
        Log.i("CriptoMonitor", "MainFragment(updateListAdapter)");
        if (listenerListCurrencies.getMonitoringCurrencies() == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("CriptoMonitor").setMessage("Ошибка передачи данных отслеживаемых валют с сервера = ")
                    .setCancelable(false).setNegativeButton("ОК",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            if (adapter == null)
                adapter = new ItemAdapter(listView.getContext(), listenerListCurrencies.getMonitoringCurrencies());
            if (listView.getAdapter() == null)
                // устанавливаем для списка адаптер
                listView.setAdapter(adapter);
            else
                ((ItemAdapter) listView.getAdapter()).notifyDataSetChanged();

//          ((ItemAdapter) (listView.getAdapter())).setSelectionPosition(0);
//           listenerListCurrencies.changeSelectionCurrency();
        }

        progressBar.setVisibility(ProgressBar.INVISIBLE);
    }

    public Currency getSelectionCurrency() {
        if (listView.getAdapter() != null) {
            int pos = ((ItemAdapter) (listView.getAdapter())).getSelectionPosition();
            return (Currency) listView.getItemAtPosition(pos);
        } else
            return null;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        menu.add(0, SET_RANGE, 0, "Set trackable range");
        menu.add(0, DELETE, 0, "Delete currencies from list");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // пункты контекстного меню
            case SET_RANGE:
                listenerListCurrencies.updateRange();
                break;
            case DELETE:
                listenerListCurrencies.deleteCurrency(getSelectionCurrency());
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(hidden == false){
            listenerListCurrencies.updateFragLayout(DataExchanger.LAYOUT_MAIN);
        }
    }
}




