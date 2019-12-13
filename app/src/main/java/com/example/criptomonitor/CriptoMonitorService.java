package com.example.criptomonitor;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class CriptoMonitorService extends Service {
    //Ссылка на веб-сервис
    private final String BASE_URL = "http://whattomine.com";
    private final String CHANNEL_NAME = "criptoparser_channel";
    //Действие-идентификатор сервиса
    public final static String CRIPTOPARSER_ACTION = "CRIPTO_SERVICE";
    //Ярлык для данных, которые передает сервис
    public final static String CRIPTOPARSER_DATA = "CurrenciesList";
    private final int CHANNEL_ID = 456;
    static final int MSG_UPDATE_LIST = 110;

    private Retrofit retrofit;
    RetrofitInterface jsonApi;
    private Timer serviceTimer;
    private ArrayList<Currency> currenciesArrayList;
    Intent activityIntent;

    private final IBinder serviceBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        CriptoMonitorService getService() {
            return CriptoMonitorService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    public void onCreate() {
        super.onCreate();
        //Инициализируем класс библиотеки для работы с веб-сервисом
        retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        jsonApi = retrofit.create(RetrofitInterface.class);
        Intent intent = new Intent(CRIPTOPARSER_ACTION);

        serviceTimer = new Timer();
        ServiceTimerTask serviceTask = new ServiceTimerTask();
        serviceTimer.schedule(serviceTask, 0, 15000);
    }

    public void onChangeRangeCurrencies(ArrayList<Currency> list){
        if(list != null && list.size() > 0 && currenciesArrayList != null && currenciesArrayList.size() > 0) {
            for (Currency curr : currenciesArrayList) {
                int index = list.indexOf(curr);
                if (index != -1) {
                    Currency item = list.get(index);
                    if (item.getPrice() >= curr.getMaxPrice())
                        Log.i("CriptoMonitor", "CriptoMonitorService(onChangeRangeCurrencies)");
                        //notificationMng.notify(CHANNEL_ID, getNotification(item.getName(), item.getMaxPrice(), 1));
                    else if (item.getPrice() <= curr.getMinPrice())
                        Log.i("CriptoMonitor", "CriptoMonitorService:onChangeRangeCurrencies");
                        //notificationMng.notify(CHANNEL_ID, getNotification(item.getName(), item.getMinPrice(), 0));
                }
            }
        }
    }

    class ServiceTimerTask extends TimerTask {
        public void run() {
            Call<JSonDataCurrencies> call = jsonApi.getData();
            call.enqueue(new Callback<JSonDataCurrencies>() {
                //Метод, вызывающийся, когда приходит ответ от веб-сервиса
                @Override
                public void onResponse(Call<JSonDataCurrencies> call, Response<JSonDataCurrencies> response) {
                    if (response.isSuccessful()) {
                        JSonDataCurrencies result = response.body();
                        ArrayList<Currency> list = result.getListCurrencies();
                        Log.i("CriptoMonitor", "CriptoMonitorService(onResponse): count of currencies = " + list.size());
                        onChangeRangeCurrencies(list);
                        if(getApplication() != null) {
                            activityIntent = new Intent(CRIPTOPARSER_ACTION);
                            activityIntent.putParcelableArrayListExtra(CRIPTOPARSER_DATA, list);
                            sendBroadcast(activityIntent);
                        }
                    } else {
                        Log.i("CriptoMonitor", "RetrofitService: error code: " + response.code());
                    }
                }

                //Метод, вызывающийся, когда приходит ошибка от веб-сервиса
                @Override
                public void onFailure(Call<JSonDataCurrencies> call, Throwable t) {
                    Log.i("CriptoMonitor", "onFailure: Error getting result - " + t.getMessage());
                    if(getApplication() != null) {
                        activityIntent = new Intent(CRIPTOPARSER_ACTION);
                        activityIntent.putParcelableArrayListExtra(CRIPTOPARSER_DATA, null);
                        // сообщаем о старте задачи
                        sendBroadcast(activityIntent);
                    }
                }
            });
        }
    }

}
