package com.example.criptomonitor;

import android.app.Notification;
import android.app.PendingIntent;
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
    public final static String CRIPTOSERVICE_ACTION = "CRIPTO_SERVICE";
    public final static String CRIPTOSERVICE_ERROR = "CRIPTOSERVICE_ERROR";
    public final static String CRIPTOSERVICE_LIST = "CRIPTOSERVICE_LIST";
    private final static int NOTIFICATION_ID = 1122;
    private final static int CHANNEL_ID = 456;
    private final static int MSG_UPDATE_LIST = 110;
    public final static int TIME_RELOAD_MIN = 15000;
    public final static int TIME_RELOAD_MAX = 86400000;
    private int intervalReload;

    private Retrofit retrofit;
    private RetrofitInterface jsonApi;
    private Timer serviceTimer;
    private ServiceTimerTask serviceTask;
    private ArrayList<Currency> currenciesMonitoringList;

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
        Intent intent = new Intent(CRIPTOSERVICE_ERROR);
        currenciesMonitoringList = new ArrayList<Currency>();

        intervalReload = TIME_RELOAD_MIN;
        serviceTimer = new Timer();
        serviceTask = new ServiceTimerTask();
        serviceTimer.schedule(serviceTask, 0, intervalReload);
    }

    public void updateTimer(int millisec){
        Log.i("CriptoMonitor", "CriptoMonitorService(updateTimer) period = " + millisec);
        if(millisec < TIME_RELOAD_MIN)
            intervalReload = TIME_RELOAD_MIN;
        else if(millisec > TIME_RELOAD_MAX)
            intervalReload = TIME_RELOAD_MAX;
        else
            intervalReload = millisec;
        serviceTask.cancel();
        serviceTask = new ServiceTimerTask();
        serviceTimer.schedule(serviceTask, 0, intervalReload);
    }
/*
    public void updateListAllCurrencies(Currency currency, boolean clear){
        for (int i = 0; i < currenciesAllList.size(); i++) {
            Currency curr = currenciesAllList.get(i);
            if (curr.equals(currency) == true) {
                if (clear) {
                    curr.setMaxPrice(-1.0);
                    curr.setMinPrice(-1.0);
                } else {
                    curr.setMaxPrice(currency.getMaxPrice());
                    curr.setMinPrice(currency.getMinPrice());
                }
                break;
            }
        }
    }
*/

    public void onChangeRangeCurrencies(){
        /*
        if(currenciesAllList != null && currenciesAllList.size() > 0 && currenciesMonitoringList != null && currenciesMonitoringList.size() > 0) {
            for (Currency curr : currenciesMonitoringList) {
                int index = currenciesAllList.indexOf(curr);
                if (index != -1) {
                    Currency item = currenciesAllList.get(index);
                    curr.setPrice(item.getPrice());
                    //Log.i("CriptoMonitor", "CriptoMonitorService(onChangeRangeCurrencies) set price = " + curr.getPrice());
                    if (curr.getPrice() >= curr.getMaxPrice())
                        Log.i("CriptoMonitor", "CriptoMonitorService(onChangeRangeCurrencies)");
                        //notificationMng.notify(CHANNEL_ID, getNotification(item.getName(), item.getMaxPrice(), 1));
                    else if (curr.getPrice() <= curr.getMinPrice())
                        Log.i("CriptoMonitor", "CriptoMonitorService(onChangeRangeCurrencies)");
                        //notificationMng.notify(CHANNEL_ID, getNotification(item.getName(), item.getMinPrice(), 0));
                }
            }
        }
        */
    }

    public void setCurrenciesMonitoringList(ArrayList<Currency> list) {
        currenciesMonitoringList = list;
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
                        //currenciesAllList.clear();
                        result.updateMonitoringCurrencies(currenciesMonitoringList);
                        onChangeRangeCurrencies();
                        Intent intent = new Intent(CRIPTOSERVICE_ACTION);
                        sendBroadcast(intent);
                    } else {
                        Log.i("CriptoMonitor", "CriptoMonitorService(onResponse): error code: " + response.code());
                        Intent intent = new Intent(CRIPTOSERVICE_ACTION);
                        intent.putExtra(CRIPTOSERVICE_ERROR, response.message());
                        sendBroadcast(intent);
                    }
                }

                //Метод, вызывающийся, когда приходит ошибка от веб-сервиса
                @Override
                public void onFailure(Call<JSonDataCurrencies> call, Throwable t) {
                    Log.i("CriptoMonitor", "CriptoMonitorService(onFailure): Error getting result - " + t.getMessage());
                    if(getApplication() != null) {
                        Intent intent = new Intent(CRIPTOSERVICE_ACTION);
                        intent.putExtra(CRIPTOSERVICE_ERROR, t.getLocalizedMessage());
                        sendBroadcast(intent);
                    }
                }
            });
        }
    }

    public ArrayList<Currency> getCurrenciesMonitoringList() {
        return currenciesMonitoringList;
    }

    public void updateAllCurrencies() {
        Call<JSonDataCurrencies> call = jsonApi.getData();
        call.enqueue(new Callback<JSonDataCurrencies>() {
            //Метод, вызывающийся, когда приходит ответ от веб-сервиса
            @Override
            public void onResponse(Call<JSonDataCurrencies> call, Response<JSonDataCurrencies> response) {
                if (response.isSuccessful()) {
                    JSonDataCurrencies result = response.body();
                    Intent intent = new Intent(CRIPTOSERVICE_ACTION);
                    intent.putParcelableArrayListExtra(CRIPTOSERVICE_LIST, result.getAllCurrencies());
                    sendBroadcast(intent);
                } else {
                    Log.i("CriptoMonitor", "CriptoMonitorService(onResponse): error code: " + response.code());
                }
            }

            //Метод, вызывающийся, когда приходит ошибка от веб-сервиса
            @Override
            public void onFailure(Call<JSonDataCurrencies> call, Throwable t) {
                Log.i("CriptoMonitor", "CriptoMonitorService(onFailure): Error getting result - " + t.getMessage());
                if(getApplication() != null) {
                    Intent intent = new Intent(CRIPTOSERVICE_ACTION);
                    sendBroadcast(intent);
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification mNotification = new Notification();
        mNotification.tickerText = "Servis of CriptoMonitor";
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        startForeground(NOTIFICATION_ID, mNotification);

        return START_NOT_STICKY;
    }

    @Override
    public boolean stopService(Intent name) {
        stopForeground(true);
        return super.stopService(name);
    }

    public void onDestroy() {
        super.onDestroy();
        serviceTimer.cancel();
        serviceTimer = null;
        serviceTask = null;
        retrofit = null;
        for(Currency curr : currenciesMonitoringList)
            curr = null;
        currenciesMonitoringList = null;
    }

}
