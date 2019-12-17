package com.example.criptomonitor;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;


public class AddFragment extends Fragment {
    //Визуальный список для отображения списка всех возможных валют
    private ListView listView;
    private Button buttonAdd;
    private CheckAdapter adapter;
    DataExchanger listenerListCurrencies;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "AddFragment(onCreateView)");
        View view = inflater.inflate(R.layout.fragment_add, container,false);
        buttonAdd = view.findViewById(R.id.buttonAdd);
        listView = view.findViewById(R.id.listViewAdd);

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Currency> list = ((CheckAdapter)(listView.getAdapter())).getCheckCurrencies();
                listenerListCurrencies.setCheckCurrencies(list);
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        Log.i("CriptoMonitor", "AddFragment(onStart)");
        super.onStart();
        ArrayList<Currency> list = listenerListCurrencies.getAllCurrencies();
        if(adapter == null) {
            adapter = new CheckAdapter(listView.getContext(), list);
            // устанавливаем для списка адаптер
            listView.setAdapter(adapter);

        }
        else{
            ((CheckAdapter) listView.getAdapter()).clear();
            ((CheckAdapter) listView.getAdapter()).addAll(list);
            ((CheckAdapter) listView.getAdapter()).notifyDataSetChanged();
        }

    }

}
