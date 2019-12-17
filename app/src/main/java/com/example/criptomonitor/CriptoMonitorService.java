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
    public final static String CRIPTOPARSER_ACTION = "CRIPTO_SERVICE";
    //Ярлык для данных, которые передает сервис
    public final static String CRIPTOPARSER_DATA = "CurrenciesList";
    private final static int NOTIFICATION_ID = 1122;
    private final int CHANNEL_ID = 456;
    static final int MSG_UPDATE_LIST = 110;

    private Retrofit retrofit;
    RetrofitInterface jsonApi;
    private Timer serviceTimer;
    private ServiceTimerTask serviceTask;
    private ArrayList<Currency> currenciesMonitoringList;
    private ArrayList<Currency> currenciesAllList;
    Intent activityIntent;

    private final IBinder serviceBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        CriptoMonitorService getService() {
            return CriptoMonitorService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("CriptoMonitor", "CriptoMonitorService(onBind)");
        return serviceBinder;
    }

    public void onCreate() {
        super.onCreate();
        Log.i("CriptoMonitor", "CriptoMonitorService(onCreate)");
        //Инициализируем класс библиотеки для работы с веб-сервисом
        retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        jsonApi = retrofit.create(RetrofitInterface.class);
        Intent intent = new Intent(CRIPTOPARSER_ACTION);
        currenciesMonitoringList = new ArrayList<Currency>();
        currenciesAllList = new ArrayList<Currency>();

        serviceTimer = new Timer();
        serviceTask = new ServiceTimerTask();
        serviceTimer.schedule(serviceTask, 0, 15000);
    }

    public void updateTimer(int millisec){
        serviceTimer.cancel();
        serviceTimer.schedule(serviceTask, 0, millisec);
    }

    public void onChangeRangeCurrencies(){
        if(currenciesAllList != null && currenciesAllList.size() > 0 && currenciesMonitoringList != null && currenciesMonitoringList.size() > 0) {
            for (Currency curr : currenciesMonitoringList) {
                int index = currenciesAllList.indexOf(curr);
                if (index != -1) {
                    Currency item = currenciesAllList.get(index);
                    curr.setPrice(item.getPrice());
                    Log.i("CriptoMonitor", "CriptoMonitorService(onChangeRangeCurrencies) set price = " + curr.getPrice());
                    if (curr.getPrice() >= curr.getMaxPrice())
                        Log.i("CriptoMonitor", "CriptoMonitorService(onChangeRangeCurrencies)");
                        //notificationMng.notify(CHANNEL_ID, getNotification(item.getName(), item.getMaxPrice(), 1));
                    else if (curr.getPrice() <= curr.getMinPrice())
                        Log.i("CriptoMonitor", "CriptoMonitorService(onChangeRangeCurrencies)");
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
                        currenciesAllList.clear();
                        result.updateListCurrencies(currenciesAllList);
                        Log.i("CriptoMonitor", "CriptoMonitorService(onResponse): count of currencies = " + currenciesAllList.size());
                        onChangeRangeCurrencies();
                        Intent intent = new Intent(CRIPTOPARSER_ACTION);
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
                        activityIntent = new Intent(CRIPTOPARSER_ACTION);
                        activityIntent.putParcelableArrayListExtra(CRIPTOPARSER_DATA, null);
                        // сообщаем о старте задачи
                        //sendBroadcast(activityIntent);
                    }
                }
            });
        }
    }

    public ArrayList<Currency> getCurrenciesMonitoringList() {
        return currenciesMonitoringList;
    }

    public void setCurrenciesMonitoringList(ArrayList<Currency> list) {
        currenciesMonitoringList = list;
    }

    public ArrayList<Currency> getCurrenciesAllList() {
        return currenciesAllList;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("CriptoMonitor", "CriptoMonitorService(onStartCommand)");
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
        Log.d("CriptoMonitor", "CriptoMonitorService(stopService)");
        stopForeground(true);
        return super.stopService(name);
    }

    public void onDestroy() {
        super.onDestroy();
        serviceTimer.cancel();
        serviceTimer = null;
        serviceTask = null;
        Log.d("CriptoMonitor", "CriptoMonitorService(MyService onDestroy)");
    }

}
