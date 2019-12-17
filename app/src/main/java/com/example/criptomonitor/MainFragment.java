package com.example.criptomonitor;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
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
    private ArrayList<Currency> listMonitoringCurrencies;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "MainFragment(onCreateView)");
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        listView = (ListView) view.findViewById(R.id.myListView);
        listView.setDuplicateParentStateEnabled(true);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                ((ItemAdapter) (listView.getAdapter())).setSelectionPosition(i);
                return false;
            }
        });
        //Регистрируем контекстное меню
        registerForContextMenu(listView);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "MainFragment(onCreate)");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        // создаем объект для работы с БД
        dbHelper = dbHelper.getInstance(getActivity());
        listMonitoringCurrencies = new ArrayList<>();
        dbHelper.updateListMonitoringCurrencies(listMonitoringCurrencies);
    }

    @Override
    public void onDestroy() {
        Log.i("CriptoMonitor", "MainFragment(onDestroy)");
        super.onDestroy();
        dbHelper.close();
        dbHelper = null;
        listMonitoringCurrencies.clear();
        listMonitoringCurrencies = null;
    }

    public void deleteCurrenyFromList(Currency currency) {
        dbHelper.deleteCurrency(currency);
    }

    public void insertCurrencyInList(Currency currency) {
        dbHelper.insertCurrency(currency);
    }

    public ArrayList<Currency> getCurrenciesMonitoringList(){
        return listMonitoringCurrencies;
    }

    public void updateListAdapter() {
        Log.i("CriptoMonitor", "MainFragment(updateListAdapter)");
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        if (listenerListCurrencies.getAllCurrencies() == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("CriptoMonitor").setMessage("Ошибка передачи данных списка валют с сервера = ")
                    .setCancelable(false).setNegativeButton("ОК",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
        else {
            if(adapter == null) {
                Log.i("CriptoMonitor", "MainFragment(updateListAdapter): create null adapter");
                adapter = new ItemAdapter(listView.getContext(), listenerListCurrencies.getMonitoringCurrencies());
            }

            if (((ItemAdapter) listView.getAdapter()) == null) {
                Log.i("CriptoMonitor", "MainFragment(updateListAdapter): set adapter");
                // устанавливаем для списка адаптер
                listView.setAdapter(adapter);

            } else {
                Log.i("CriptoMonitor", "MainFragment(updateListAdapter): update adapter");
                //((ItemAdapter) listView.getAdapter()).clear();
                //((ItemAdapter) listView.getAdapter()).addAll(listenerListCurrencies.getMonitoringCurrencies());
                ((ItemAdapter) listView.getAdapter()).notifyDataSetChanged();
            }

            ((ItemAdapter) (listView.getAdapter())).setSelectionPosition(0);
        }
    }
}




