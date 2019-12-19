package com.example.criptomonitor;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

//Класс элемента списка, который позволяет выбрать для мониторинга валюту
//соответствие имени валюты и чекбокса
public class CheckAdapter extends ArrayAdapter<Currency> {

    public CheckAdapter(Context context, ArrayList<Currency> arr) {
        super(context, R.layout.check_adapter, arr);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Currency currency = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.check_adapter, null);
        }

        ((TextView) convertView.findViewById(R.id.name)).setText(currency.getName());
        if(currency.getMinPrice() == -1.0)
            ((CheckBox) convertView.findViewById(R.id.on)).setChecked(false);
        else
            ((CheckBox) convertView.findViewById(R.id.on)).setChecked(true);

        View.OnClickListener chkListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(((CheckBox)view).isChecked()) {
                    currency.setMinPrice(currency.getPrice());
                    currency.setMaxPrice(currency.getPrice());
                }
                else {
                    currency.setMinPrice(-1.0);
                    currency.setMaxPrice(-1.0);
                }
            }
        };
        ((CheckBox) convertView.findViewById(R.id.on)).setOnClickListener(chkListener);

        return convertView;
    }

    public ArrayList<Currency> getCheckCurrencies() {
        ArrayList<Currency> list = new ArrayList<Currency>();
        for(int i = 0; i < getCount(); i++){
            Log.i("CriptoMonitor", "CheckAdapter(getCheckCurrencies): name " + getItem(i).getName() + "  " + getItem(i).getMaxPrice());
            if (getItem(i).getMaxPrice() != -1.0)
                list.add(getItem(i));
        }
        for (Currency curr : list) {
            Log.i("CriptoMonitor", "CheckAdapter(getCheckCurrencies): checkList " + curr.getName());
        }
        return list;
    }
}
