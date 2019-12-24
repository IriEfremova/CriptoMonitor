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


public class AddFragment extends Fragment {
    private AddFragment fragmentAdd;
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
        if(listenerListCurrencies.getAllCurrencies() != null) {
            if (adapter == null)
                adapter = new CheckAdapter(listView.getContext(), listenerListCurrencies.getAllCurrencies());

            if (((ItemAdapter) listView.getAdapter()) == null)
                listView.setAdapter(adapter);
            else
                ((CheckAdapter) listView.getAdapter()).notifyDataSetChanged();
        }
        return view;
    }

    public void updateListAdapter(){
        if(adapter == null) {
            adapter = new CheckAdapter(listView.getContext(), listenerListCurrencies.getAllCurrencies());
        }
        if (((ItemAdapter) listView.getAdapter()) == null)
            listView.setAdapter(adapter);
        else
            ((CheckAdapter) listView.getAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "AddFragment(onCreate)");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        Log.i("CriptoMonitor", "AddFragment(onDestroy)");
        super.onDestroy();
        fragmentAdd = null;
    }
}
