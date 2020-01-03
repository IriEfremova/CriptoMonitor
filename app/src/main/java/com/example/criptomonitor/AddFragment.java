package com.example.criptomonitor;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

//Фрагмент для отображения списка всех возможных валют для выбора валют для мониторинга
//Список отображается в списке с чекбоксом для выбора нужных валют
public class AddFragment extends Fragment {
    //Визуальный список для отображения списка всех возможных валют
    private ListView listView;
    private Button buttonAdd;
    private Button buttonCancel;
    private CheckAdapter adapter;
    //Родительская активность фрагмента, которая имплементирует интерфейс взаимодействия
    DataExchanger listenerListCurrencies;

    //Создание фрагмента, задаем флаг, чтобы фрагмент не уничтожался при смене ориентации
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "AddFragment(onCreate)");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    //Привязка к активности, проверяем, что активность имплементирует нужный интерфейс
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i("CriptoMonitor", "AddFragment(onAttach)");
        if (context instanceof DataExchanger) {
            listenerListCurrencies = (DataExchanger) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement DataExchanger");
        }
    }

    //Метод на создание вью фрагмента
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "AddFragment(onCreateView)");
        View view = inflater.inflate(R.layout.fragment_add, container,false);
        buttonAdd = view.findViewById(R.id.buttonSetAdd);
        buttonCancel = view.findViewById(R.id.buttonCancelAdd);
        listView = view.findViewById(R.id.listViewAdd);

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 ArrayList<Currency> list = ((CheckAdapter)(listView.getAdapter())).getCheckCurrencies();
                ArrayList<Currency> listS = listenerListCurrencies.getMonitoringCurrencies();
                for (Currency currency : list){
                    if(listS.contains(currency) == false){
                         currency.setMinPrice(-1.0);
                        currency.setMaxPrice(-1.0);
                     }
                }
                list.clear();
                list = listenerListCurrencies.getAllCurrencies();
                for (Currency currencyS : listS){
                    for (Currency currency : list) {
                        if (currency.equals(currencyS)) {
                             currency.setMinPrice(currencyS.getMinPrice());
                             currency.setMaxPrice(currencyS.getMaxPrice());
                        }
                    }
                }
                updateListAdapter();
                listenerListCurrencies.setCheckCurrencies(null);
            }
        });

        //Обработчик нажатия кнопки, вызываем метод интерфейса для обработки всех выбранных валют
        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Currency> list = ((CheckAdapter)(listView.getAdapter())).getCheckCurrencies();
                listenerListCurrencies.setCheckCurrencies(list);
            }
        });

        //Вызываем метод обновления данных в списке
        if(listenerListCurrencies.getAllCurrencies() != null)
            updateListAdapter();
        return view;
    }

    //Метод для обновления данных в списке
    public void updateListAdapter(){
        if(listView != null) {
            //В зависимости от наличия списка всех валют и созданного адаптера списка отображаем данные
            if (adapter == null)
                adapter = new CheckAdapter(listView.getContext(), listenerListCurrencies.getAllCurrencies());
            if (listView.getAdapter() == null)
                listView.setAdapter(adapter);
            else
                ((CheckAdapter) listView.getAdapter()).notifyDataSetChanged();
        }
    }
}
