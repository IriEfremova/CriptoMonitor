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
    private Button buttonCancel;
    private EditText etTime;
    private TextView serviceName;
    private TextView serviceStatus;
    private CheckBox checkForeground;
    //Интерфейс обмена данными между активностью и фрагментом
    private DataExchanger listenerListCurrencies;

    //Создание фрагмента, задаем флаг, чтобы фрагмент не уничтожался при смене ориентации
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "ServiceSettigsFragment(onCreate)");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    //Привязка к активности, проверяем, что активность имплементирует нужный интерфейс
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

    //Метод на создание вью фрагмента
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "ServiceSettigsFragment(onCreateView)");
        View view = inflater.inflate(R.layout.fragment_service_settigs, container, false);

        buttonSet = view.findViewById(R.id.buttonSetSett);
        buttonCancel = view.findViewById(R.id.buttonCancelSett);
        checkForeground = view.findViewById(R.id.checkForeground);
        serviceName = view.findViewById(R.id.textSerName);
        serviceStatus = view.findViewById(R.id.testSerStatus);
        etTime = view.findViewById(R.id.editTime);

        serviceName.setText("CriptoMonitor");

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Возвращаемся назад
                updateStatusText();
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        //Обработчик нажатия кнопки, отправляем активности заданный интервал обращения сервиса к веб-сервису
        buttonSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listenerListCurrencies != null) {
                    if (view == buttonSet) {
                        int time = 1000 * Integer.parseInt(etTime.getText().toString());
                        if (time < CriptoMonitorService.TIME_RELOAD_MIN || time > CriptoMonitorService.TIME_RELOAD_MAX) {
                            listenerListCurrencies.showAlertDialog("Диапазон изменения интервала от 15 сек до 24 часов (86400 сек)");
                        } else {
                            listenerListCurrencies.setServerSettings(time);
                            listenerListCurrencies.setServiceStartForeground(checkForeground.isChecked() ? 1 : 0);
                            updateStatusText();
                        }
                    }
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            }
        });
        return view;
    }

    //При визуализации фрагмента обновляем статус сервиса
    @Override
    public void onStart() {
        Log.i("CriptoMonitor", "ServiceSettigsFragment(onStart)");
        super.onStart();
        updateStatusText();
    }

    //Метод для обновления статуса сервиса
    public void updateStatusText() {
        Log.i("CriptoMonitor", "ServiceSettigsFragment(updateStatusText)");
        if (listenerListCurrencies != null) {
            checkForeground.setChecked(listenerListCurrencies.isServiceStartForeground());
            int interval = listenerListCurrencies.isServiceStart();
            if (interval != -1) {
                etTime.setText(String.valueOf(interval / 1000));
                if (listenerListCurrencies.isServiceStartForeground())
                    serviceStatus.setText("Запущен в фоновом режиме");
                else
                    serviceStatus.setText("Запущен в стандартном режиме");
            } else {
                etTime.setText("undefined");
                serviceStatus.setText("Остановлен");
            }
        }
    }
}
