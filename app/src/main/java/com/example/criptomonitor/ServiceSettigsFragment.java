package com.example.criptomonitor;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

public class ServiceSettigsFragment extends Fragment {
    private Button buttonSet;
    private Button buttonStart;
    private Button buttonStop;
    private EditText etTime;
    private TextView serviceName;
    private TextView serviceStatus;
    private DataExchanger listenerListCurrencies;
    private int serviseInterval;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i("CriptoMonitor", "ServiceSettigsFragment(onAttach)");
        if (context instanceof DataExchanger) {
            listenerListCurrencies = (DataExchanger) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement DataExchanger");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "ServiceSettigsFragment(onCreateView)");
        View view = inflater.inflate(R.layout.fragment_service_settigs, container, false);

        buttonSet = view.findViewById(R.id.buttonSet);
        buttonStop = view.findViewById(R.id.buttonStop);
        buttonStart = view.findViewById(R.id.buttonStart);
        serviceName = view.findViewById(R.id.textSerName);
        serviceStatus = view.findViewById(R.id.testSerStatus);
        etTime = view.findViewById(R.id.editTime);

        serviceName.setText("CriptoMonitor");
        serviseInterval = listenerListCurrencies.isServiceStart();

        if(listenerListCurrencies != null){
            if(serviseInterval != -1) {
                buttonStart.setEnabled(false);
                buttonStop.setEnabled(true);
                etTime.setText(String.valueOf(serviseInterval));
                serviceStatus.setText("Запущен");
            }
            else {
                buttonStart.setEnabled(true);
                buttonStop.setEnabled(false);
                serviceStatus.setText("Остановлен");
            }
        }

        View.OnClickListener listener = new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                boolean isServiseStart = (serviseInterval != -1 ? true : false);
                if(view == buttonSet) {
                    int time = Integer.parseInt(etTime.getText().toString());
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
                    if (listenerListCurrencies != null) {
                        listenerListCurrencies.setServerSettings(0, !isServiseStart);
                        if(isServiseStart == true) {
                            serviceStatus.setText("Остановлен");
                            buttonStart.setEnabled(true);
                            buttonStop.setEnabled(false);
                        }
                        else {
                            buttonStart.setEnabled(false);
                            buttonStop.setEnabled(true);
                            serviceStatus.setText("Запущен");
                        }
                    }
                }
            }
        };
        buttonSet.setOnClickListener(listener);
        buttonStart.setOnClickListener(listener);
        buttonStop.setOnClickListener(listener);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "ServiceSettigsFragment(onCreate)");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
}
