package com.example.criptomonitor;

import android.content.ComponentName;
import android.content.Context;
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


public class MainFragment extends Fragment {
    private MainFragment fragmentMain;

    //Пункты контекстного меню
    private final int SET_RANGE = 1;
    private final int DELETE = 2;
    //Визуальный список для отображения списка валют, которые мониторим
    private ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "MainFragment(onCreateView)");
        View view = inflater.inflate(R.layout.fragment_main, container,false);

        listView = (ListView) view.findViewById(R.id.myListView);
        listView.setDuplicateParentStateEnabled(true);
        //Регистрируем контекстное меню
        registerForContextMenu(listView);

        //!!!!ItemAdapter adapter = new ItemAdapter(listView.getContext(), ((MainActivity)(getActivity())).getListTrackCurrencies());
        // устанавливаем для списка адаптер
        //!!!!listView.setAdapter(adapter);
        /*
        ((ItemAdapter)(listView.getAdapter())).setSelectionPosition(0);
        listView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((ItemAdapter) (listView.getAdapter())).setSelectionPosition(position);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                ((ItemAdapter)(listView.getAdapter())).setSelectionPosition(i);
                return false;
            }
        });
        */
        return view;
    }

    @Override
    public void onStart() {
        Log.i("CriptoMonitor", "MainFragment(onStart)");
        super.onStart();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "MainFragment(onCreate)");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        Log.i("CriptoMonitor", "MainFragment(onDestroy)");
        super.onDestroy();
    }
}




