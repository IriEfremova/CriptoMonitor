package com.example.criptomonitor;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;

public class ServiceSettigsFragment extends Fragment {
    private Button buttonSet;
    private Button buttonStart;
    private Button buttonStop;
    private EditText etTime;
    private DataExchanger listenerListCurrencies;

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
        View view = inflater.inflate(R.layout.fragment_service_settigs, container, false);

        buttonSet = view.findViewById(R.id.buttonSet);
        buttonStop = view.findViewById(R.id.buttonStop);
        buttonStart = view.findViewById(R.id.buttonStart);
        final boolean isServiseStart = listenerListCurrencies.isServiceStart();
        if(listenerListCurrencies != null){
            if(isServiseStart) {
                buttonStart.setEnabled(false);
                buttonStop.setEnabled(true);
            }
            else {
                buttonStart.setEnabled(true);
                buttonStop.setEnabled(false);
            }
        }
        etTime = view.findViewById(R.id.editTime);

        View.OnClickListener listener = new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(view == buttonSet) {
                    int time = Integer.getInteger(etTime.getText().toString());
                    if (time < CriptoMonitorService.TIME_RELOAD_MIN || time > CriptoMonitorService.TIME_RELOAD_MAX) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle("CriptoParser").setMessage("Диапазон изменения интервала от 15 сек до 24 часов (86400 сек)")
                                .setCancelable(false).setNegativeButton("ОК",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.show();
                    } else
                        listenerListCurrencies.setServerSettings(time, isServiseStart);
                }
                else if(view == buttonStart || view == buttonStop){
                    if (listenerListCurrencies != null)
                        listenerListCurrencies.setServerSettings(0, !isServiseStart);
                }
            }
        };
        buttonSet.setOnClickListener(listener);
        buttonStart.setOnClickListener(listener);
        buttonStop.setOnClickListener(listener);
        return view;
    }

}
