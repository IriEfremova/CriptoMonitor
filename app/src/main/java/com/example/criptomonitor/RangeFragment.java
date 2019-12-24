package com.example.criptomonitor;


import android.app.AlertDialog;
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

import java.text.DecimalFormat;
import java.text.ParseException;


public class RangeFragment extends Fragment {
    private RangeFragment fragmentRange;

    private Button btnSet;
    private TextView tvName;
    private TextView tvPrice;
    private EditText etMin;
    private EditText etMax;
    private Currency currency;
    DecimalFormat df = new DecimalFormat("0.0000000000");

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "RangeFragment(onCreateView)");
        View view = inflater.inflate(R.layout.fragment_range, container, false);

        btnSet = view.findViewById(R.id.buttonSet);
        tvName = view.findViewById(R.id.textViewName);
        tvPrice = view.findViewById(R.id.textViewPrice);
        etMin = view.findViewById(R.id.editMin);
        etMax = view.findViewById(R.id.editMax);
        etMin.setFocusable(false);
        etMax.setFocusable(false);

        btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
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
                    if (flag) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle("CriptoParser").setMessage(str)
                                .setCancelable(false).setNegativeButton("ОК",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.show();
                    } else {
                        currency.setMinPrice(valueMin);
                        currency.setMaxPrice(valueMax);
                    }
                } catch (NumberFormatException e) {
                    Log.i("CriptoMonitor", "RangeFragment(onCreateView):" + e.getMessage());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });
        if (currency != null) {
            tvName.setText(currency.getName());
            tvPrice.setText(df.format(currency.getPrice()));
            etMax.setText(df.format(currency.getMaxPrice()));
            etMin.setText(df.format(currency.getMinPrice()));
        }
        return view;
    }

    @Override
    public void onStart() {
        Log.i("CriptoMonitor", "RangeFragment(onStart)");
        super.onStart();
    }

    public void setSelectionCurrency(Currency mainFragCurrency) {
        currency = mainFragCurrency;
        if (getView() != null) {
            Log.i("CriptoParser", "RangeCurrenciesFragment(setSelectionCurrency): Currencies name = " + currency.getName() + " min range = " + currency.getMinPrice());
            tvName.setText(currency.getName());
            tvPrice.setText(df.format(currency.getPrice()));
            etMax.setText(df.format(currency.getMaxPrice()));
            etMin.setText(df.format(currency.getMinPrice()));
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i("CriptoMonitor", "RangeFragment(onCreate)");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        Log.i("CriptoMonitor", "RangeFragment(onDestroy)!!!!!!!!!!!");
        super.onDestroy();
    }
}