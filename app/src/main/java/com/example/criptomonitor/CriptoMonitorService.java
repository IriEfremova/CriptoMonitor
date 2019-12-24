package com.example.criptomonitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
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
    //Действие-идентификатор сервиса
    public final static String CRIPTOSERVICE_ACTION = "CRIPTO_SERVICE";
    public final static String CRIPTOSERVICE_ACTION_LIST = "CRIPTO_SERVICE_LIST";
    public final static String CRIPTOSERVICE_ERROR = "CRIPTOSERVICE_ERROR";
    public final static String CRIPTOSERVICE_LIST = "CRIPTOSERVICE_LIST";
    public final static String CRIPTOSERVICE_CHANNEL = "CRIPTOSERVICE_CHANNEL";
    private final static String CHANNEL_ID = "com.example.criptomonitor";
    private final static int NOTIFICATION_ID = 1122;
    private final static int SERVICE_ID = 1122;

    public final static int TIME_RELOAD_MIN = 15000;
    public final static int TIME_RELOAD_MAX = 86400000;
    private int intervalReload;

    private Retrofit retrofit;
    private RetrofitInterface jsonApi;
    private Timer serviceTimer;
    private ServiceTimerTask serviceTask;
    private ArrayList<Currency> currenciesMonitoringList;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    private final IBinder serviceBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        CriptoMonitorService getService() {
            return CriptoMonitorService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("CriptoMonitor", "CriptoMonitorService(onBind)");
        updateAllCurrencies();
        return serviceBinder;
    }

    public void onCreate() {
        super.onCreate();
        //Инициализируем класс библиотеки для работы с веб-сервисом
        retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        jsonApi = retrofit.create(RetrofitInterface.class);
        Intent intent = new Intent(CRIPTOSERVICE_ERROR);
        currenciesMonitoringList = new ArrayList<Currency>();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder = new NotificationCompat.Builder(this);
        } else {
            if (notificationManager.getNotificationChannel(CRIPTOSERVICE_CHANNEL) == null) {
                NotificationChannel channel = new NotificationChannel(CRIPTOSERVICE_CHANNEL, "CriptoParser", NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }
            notificationBuilder = new NotificationCompat.Builder(this, CRIPTOSERVICE_CHANNEL);
        }
        notificationBuilder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("CriptoParser");

        intervalReload = TIME_RELOAD_MIN;
        serviceTimer = new Timer();
        serviceTask = new ServiceTimerTask();
        serviceTimer.schedule(serviceTask, 0, intervalReload);
    }

    public int getIntervalReload() {
        return intervalReload;
    }

    public void updateTimer(int millisec) {
        if (millisec < TIME_RELOAD_MIN)
            intervalReload = TIME_RELOAD_MIN;
        else if (millisec > TIME_RELOAD_MAX)
            intervalReload = TIME_RELOAD_MAX;
        else
            intervalReload = millisec;
        serviceTask.cancel();
        serviceTask = new ServiceTimerTask();
        serviceTimer.schedule(serviceTask, 0, intervalReload);
        Log.i("CriptoMonitor", "CriptoMonitorService(updateTimer) interval reload = " + intervalReload);
    }

    public void onChangeRangeCurrencies() {
        if (currenciesMonitoringList != null && currenciesMonitoringList.size() > 0) {
            for (Currency curr : currenciesMonitoringList) {
                //Log.i("CriptoMonitor", "CriptoMonitorService(onChangeRangeCurrencies) set price = " + curr.getPrice());
                if (curr.getPrice() >= curr.getMaxPrice()) {
                    Log.i("CriptoMonitor", "CriptoMonitorService(onChangeRangeCurrencies)");
                    notificationManager.notify(NOTIFICATION_ID, getNotification(curr.getName(), curr.getMaxPrice(), 1));
                }
                else if (curr.getPrice() <= curr.getMinPrice()) {
                    Log.i("CriptoMonitor", "CriptoMonitorService(onChangeRangeCurrencies)");
                    notificationManager.notify(NOTIFICATION_ID, getNotification(curr.getName(), curr.getMinPrice(), 0));
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
                        result.updateMonitoringCurrencies(currenciesMonitoringList);
                        onChangeRangeCurrencies();
                        if (getApplication() != null) {
                            Intent intent = new Intent(CRIPTOSERVICE_ACTION);
                            sendBroadcast(intent);
                        }
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
                    Intent intent = new Intent(CRIPTOSERVICE_ACTION);
                    intent.putExtra(CRIPTOSERVICE_ERROR, t.getLocalizedMessage());
                    sendBroadcast(intent);
                }
            });
        }
    }

    public ArrayList<Currency> getCurrenciesMonitoringList() {
        return currenciesMonitoringList;
    }

    public void updateAllCurrencies() {
        Log.i("CriptoMonitor", "CriptoMonitorService(updateAllCurrencies)");
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
                    Intent intent = new Intent(CRIPTOSERVICE_ACTION);
                    intent.putExtra(CRIPTOSERVICE_ERROR, response.message());
                    sendBroadcast(intent);
                }
            }

            //Метод, вызывающийся, когда приходит ошибка от веб-сервиса
            @Override
            public void onFailure(Call<JSonDataCurrencies> call, Throwable t) {
                Log.i("CriptoMonitor", "CriptoMonitorService(onFailure): Error getting result - " + t.getMessage());
                Intent intent = new Intent(CRIPTOSERVICE_ACTION_LIST);
                sendBroadcast(intent);
            }
        });
    }

    public Notification getNotification(String name, double price, int typeBorder) {
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (name == null)
            return notificationBuilder.setContentIntent(resultPendingIntent).build();
        else {
            String str = "Валюта достигла границы (неизвестный тип границы)...";
            if (typeBorder == 0)
                str = String.format("Валюта %s достигла нижней границы %f", name, price);
            else if (typeBorder == 1)
                str = String.format("Валюта %s достигла верхней границы %f", name, price);
            return notificationBuilder.setContentText(str).setContentIntent(resultPendingIntent).build();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        startForeground(SERVICE_ID, getNotification(null, 0.0, 0));

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
        for (Currency curr : currenciesMonitoringList)
            curr = null;
        currenciesMonitoringList = null;
    }

}
