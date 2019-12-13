package com.example.criptomonitor;

import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class JSonDataCurrencies {
    //Список валют в ответе от веб-сервиса
    private JsonObject coins;

    //Метод, формирующий список всех возможных валют с их текущими ценами
    public ArrayList<Currency> getListCurrencies() {
        ArrayList<Currency> list = new ArrayList<Currency>();
        Iterator iterator = coins.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonElement> entry = (Map.Entry<String, JsonElement>) iterator.next();
            JsonElement dataCurrencies = entry.getValue();
            Currency element = new Currency(entry.getKey(), dataCurrencies.getAsJsonObject().get("exchange_rate").getAsDouble());
            list.add(element);
        }
        Log.i("CriptoMonitor", "getListCurrencies: return list size of = " + list.size());
        return list;
    }
}

