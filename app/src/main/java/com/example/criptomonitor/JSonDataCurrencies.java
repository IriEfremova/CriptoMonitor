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
    public void updateMonitoringCurrencies(ArrayList<Currency> listFromService) {
        Iterator iterator = coins.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonElement> entry = (Map.Entry<String, JsonElement>) iterator.next();
            JsonElement dataCurrencies = entry.getValue();
            int index = -1;
            for(int i = 0; i < listFromService.size(); i++){
                if(listFromService.get(i).getName().equals(entry.getKey())) {
                    index = i;
                    break;
                }
            }
            if(index != -1)
                listFromService.get(index).setPrice(dataCurrencies.getAsJsonObject().get("exchange_rate").getAsDouble());
        }
    }

    public ArrayList<Currency> getAllCurrencies() {
        ArrayList<Currency> list = new ArrayList<Currency>();
        Iterator iterator = coins.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonElement> entry = (Map.Entry<String, JsonElement>) iterator.next();
            JsonElement dataCurrencies = entry.getValue();
            Currency element = new Currency(entry.getKey(), dataCurrencies.getAsJsonObject().get("exchange_rate").getAsDouble(), -1.0, -1.0);
            list.add(element);
        }
        Log.i("CriptoMonitor", "JSonDataCurrencies(getAllCurrencies): size of list = " + list.size());
        return list;
    }

}

