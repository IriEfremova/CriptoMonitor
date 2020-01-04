package com.example.criptomonitor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;


//Фрагмент для отображения данных по конкретной валюте
public class RangeFragment extends Fragment {
    private Button btnSet;
    private Button btnCancel;
    private TextView tvName;
    private TextView tvPrice;
    private EditText etMin;
    private EditText etMax;
    //Ссылка на текущую валюту
    private Currency currency;
    //Формат отображения данных
    DecimalFormat df;
    //Интерфейс обмена данными между активностью и фрагментом
    private DataExchanger listenerListCurrencies;

    //Создание фрагмента, задаем флаг, чтобы фрагмент не уничтожался при смене ориентации
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "RangeFragment(onCreate)");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.getDefault());
        otherSymbols.setDecimalSeparator('.');
        df = new DecimalFormat("0.0000000000", otherSymbols);
    }

    //Привязка к активности, проверяем, что активность имплементирует нужный интерфейс
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

    //Метод на создание вью фрагмента
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "RangeFragment(onCreateView)");
        View view = inflater.inflate(R.layout.fragment_range, container, false);

        //Компоненты фрагмента
        btnSet = view.findViewById(R.id.buttonSetRange);
        btnCancel = view.findViewById(R.id.buttonCancelRange);
        tvName = view.findViewById(R.id.textViewName);
        tvPrice = view.findViewById(R.id.textViewPrice);
        etMin = view.findViewById(R.id.editMin);
        etMax = view.findViewById(R.id.editMax);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currency != null) {
                    etMax.setText(df.format(currency.getMaxPrice()));
                    etMin.setText(df.format(currency.getMinPrice()));
                    listenerListCurrencies.updateCurrency(null);
                }
            }
        });

        //Обработчик нажатия на кнопку
        btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    //Контролируем введенные данные
                    Number num = df.parse(etMin.getText().toString());
                    Double valueMin = num.doubleValue();
                    num = df.parse(etMax.getText().toString());
                    Double valueMax = num.doubleValue();
                    String str = "";
                    boolean flag = false;
                    if (valueMax < currency.getPrice()) {
                        flag = true;
                        str = "Верхняя граница мониторинга не может быть меньше, чем текущая цена";
                    }
                    if (valueMin > currency.getPrice()) {
                        flag = true;
                        str = "Нижняя граница мониторинга не может быть больше, чем текущая цена";
                    }
                    if (flag)
                        listenerListCurrencies.showAlertDialog(str);
                     else {
                        //Если все введено верно, то меняем границы текущей валюты
                        currency.setMinPrice(valueMin);
                        currency.setMaxPrice(valueMax);
                        listenerListCurrencies.updateCurrency(currency);
                    }
                } catch (NumberFormatException e) {
                    Log.i("CriptoMonitor", "RangeFragment(onCreateView):" + e.getMessage());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });
        //Если текущая валюта выбрана, то заполняем данные по ней
        if (currency != null)
            setSelectionCurrency(currency);
        return view;
    }

    //Метод для заполнения фрагмента данными по текущей валюте
    public void setSelectionCurrency(Currency mainFragCurrency) {
        if(mainFragCurrency != null) {
            currency = mainFragCurrency;
            if (tvName != null) {
                tvName.setText(currency.getName());
                tvPrice.setText(df.format(currency.getPrice()));
                if(etMax.getText() == null || etMax.getText().equals(""))
                    etMax.setText(df.format(currency.getMaxPrice()));
                if(etMin.getText() == null || etMin.getText().equals(""))
                    etMin.setText(df.format(currency.getMinPrice()));
            }
        }
    }
}