package com.example.criptomonitor;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

//Класс адаптера списка, который отображает имя валюты и ее текущую цену
public class ItemAdapter extends ArrayAdapter<Currency> {
    private final int colorWhite = Color.WHITE;
    //Подсветка текущей строки
    private final int colorGray = Color.rgb(170,215,190);
    //Позиция текущей строки
    private int selectionPosition;

    public ItemAdapter(Context context, ArrayList<Currency> arr) {
        super(context, R.layout.item_adapter, arr);
        selectionPosition = 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Currency currency = getItem(position);
        final int pos = position;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_adapter, null);
        }
        //Заполняем строки данными
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.getDefault());
        otherSymbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("0.0000000000", otherSymbols);
        // Заполняем адаптер
        ((TextView) convertView.findViewById(R.id.name)).setText(currency.getName());
        ((TextView) convertView.findViewById(R.id.cost)).setText(df.format(currency.getPrice()));
        ((TextView) convertView.findViewById(R.id.cost)).setFocusable(false);

        //Подсвечиваем текущую строку
        if (selectionPosition == position){
            convertView.setBackgroundColor(colorGray);

        }else {
            convertView.setBackgroundColor(colorWhite);
        }
        df = null;
        return convertView;
    }

    //Метод для задания текущей строки
    void setSelectionPosition(int position){
        selectionPosition = position;
        notifyDataSetChanged();
    }

    //Метод, возвращающий номер текущей строки
    int getSelectionPosition(){
        return selectionPosition;
    }
}
