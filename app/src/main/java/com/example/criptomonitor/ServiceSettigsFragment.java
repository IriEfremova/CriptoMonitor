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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

public class ServiceSettigsFragment extends Fragment {
    private Button buttonSet;
    private EditText etTime;
    private TextView serviceName;
    private TextView serviceStatus;
    private CheckBox checkForeground;
    private DataExchanger listenerListCurrencies;

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
        checkForeground = view.findViewById(R.id.checkForeground);
        serviceName = view.findViewById(R.id.textSerName);
        serviceStatus = view.findViewById(R.id.testSerStatus);
        etTime = view.findViewById(R.id.editTime);

        serviceName.setText("CriptoMonitor");
        checkForeground.setChecked(listenerListCurrencies.isServiceStartForeground());
        checkForeground.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (listenerListCurrencies != null) {
                    listenerListCurrencies.setServiceStartForeground(b);
                }
            }
        });

        buttonSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listenerListCurrencies != null) {
                    if (view == buttonSet) {
                        int time = Integer.parseInt(etTime.getText().toString());
                        if (time < CriptoMonitorService.TIME_RELOAD_MIN || time > CriptoMonitorService.TIME_RELOAD_MAX) {
                            listenerListCurrencies.showAlertDialog("Диапазон изменения интервала от 15 сек до 24 часов (86400 сек)");
                        } else
                            listenerListCurrencies.setServerSettings(time);
                    }
                }
            }
        });
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "ServiceSettigsFragment(onCreate)");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(listenerListCurrencies != null){
            int interval = listenerListCurrencies.isServiceStart();
            if( interval != -1) {
                etTime.setText(String.valueOf(interval));
                if(listenerListCurrencies.isServiceStartForeground())
                    serviceStatus.setText("Запущен в фоновом режиме");
                else
                    serviceStatus.setText("Запущен в стандартном режиме");
            }else {
                etTime.setText("");
                serviceStatus.setText("Остановлен");
            }
        }
    }
}
