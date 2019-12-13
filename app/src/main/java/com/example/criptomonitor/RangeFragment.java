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

    Button btnSet;
    TextView tvName;
    TextView tvPrice;
    EditText etMin;
    EditText etMax;
    Currency currency;
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
                        //!!!!((MainActivity)getActivity()).setFragMain();
                    }
                } catch (NumberFormatException e) {
                    Log.i("CriptoMonitor", "RangeFragment(onCreateView):" + e.getMessage());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        Log.i("CriptoMonitor", "RangeFragment(onStart)");
        super.onStart();
    }
/*
        currency = ((MainActivity) getActivity()).getSelectedCurrencies();
        if (currencies != null) {
            Log.i("CriptoParser", "RangeCurrenciesFragment(onStart): Currencies name = " + currencies.getName() + " min range = " + currencies.getMinPrice());
            tvName.setText(currencies.getName());
            tvPrice.setText(df.format(currencies.getPrice()));
            etMax.setText(df.format(currencies.getMaxPrice()));
            etMin.setText(df.format(currencies.getMinPrice()));
        } else
            Log.i("CriptoParser", "RangeCurrenciesFragment(onCreateView): No selected currency");

    }
    }
*/

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