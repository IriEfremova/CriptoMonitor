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

/*
Класс сервиса "CriptoMonitor".
Сервис может работать в фоновом режиме, т.е. не уничтожаться при закрытии приложения (isForegroud)
Может работать в простом режиме и будет закрываться при закрытии приложения, с ним связанного

Сервис с интервалом в intervalReload обращается к веб-сервису "http://whattomine.com", получает от него
данные, обновляет свой список отслеживаемых валют и отправляет сигнал привязанному приложению о том,
что данные обновлены. Список отслеживаемых валют проверяется на достижение текущей цены границ мониторинга.
Если одна из границ была пробита, то отправляется уведомление.
При привязывании приложения происходит автоматическая отправка приложению данных по всем возможным валютам.
 */

public class CriptoMonitorService extends Service {
    //Ссылка на веб-сервис
    private final String BASE_URL = "http://whattomine.com";
    //Константы для опеределения действий и данных сервиса
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

    //Интервал обращения сервиса к веб-сервису валют
    private int intervalReload;

    //Признаки функционирования сервиса и конкретно его режим
    private boolean isRunning = false;
    private boolean isForeground = true;

    //Объекты для работы с библиотекой Retrofit
    private Retrofit retrofit;
    private RetrofitInterface jsonApi;

    //Таймер и задание сервиса
    private Timer serviceTimer;
    private ServiceTimerTask serviceTask;

    //Список валют для отслеживания
    private ArrayList<Currency> currenciesMonitoringList;

    //Объекты для работы с уведомлениями
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    //Указатель для привязки к сервису
    private final IBinder serviceBinder = new LocalBinder();

    //Класс для связи с объектом сервиса, возвращает указатель на работающий экземпляр сервиса
    public class LocalBinder extends Binder {
        CriptoMonitorService getService() {
            return CriptoMonitorService.this;
        }
    }

    //Метод, срабатывающий при отсоединении внешнего приложения от сервиса,
    //возвращаем false, чтобы можно было повторно присоединиться
    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    //Метод, срабатывающий при повторном привязывании внешнего приложения к сервису,
    //отправляем приложению данные по всем валютам с веб-сервиса
    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        getAllCurrencies();
    }

    //Метод, срабатывающий при первоначальномпривязывании внешнего приложения к сервису,
    //отправляем приложению данные по всем валютам с веб-сервиса
    @Override
    public IBinder onBind(Intent intent) {
        getAllCurrencies();
        return serviceBinder;
    }

    //При запуске сервиса. Если нужно, то запускаем его в фоновом режиме
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if(isForeground)
            startForeground(SERVICE_ID, getNotification(null, 0.0, 0));
        isRunning = true;
        return START_NOT_STICKY;
    }

    //При отсановке сервиса, перед остановкой, если нужно, убираем его из фона
    @Override
    public boolean stopService(Intent name) {
        Log.i("CriptoMonitor", "CriptoMonitorService(stopService)");
        if(isForeground)
            stopForeground(true);
        isRunning = false;
        return super.stopService(name);
    }

    //При уничтожении
    public void onDestroy() {
        super.onDestroy();
        if(serviceTimer != null) {
            serviceTimer.cancel();
            serviceTimer = null;
        }
        if(serviceTimer != null)
            serviceTask = null;
        if(retrofit == null)
            retrofit = null;
        if(currenciesMonitoringList != null){
            for (Currency curr : currenciesMonitoringList)
                curr = null;
            currenciesMonitoringList = null;
        }
    }

    //Возвращаем список отслеживаемых валют
    public ArrayList<Currency> getCurrenciesMonitoringList() {
        return currenciesMonitoringList;
    }

    //Методы, возвращающие признаки работы сервиса (запущен или в фоновом режиме)
    public boolean isServiceRunning(){
        return isRunning;
    }
    public boolean isServiceForeground(){
        return isForeground;
    }

    //Устанавливаем режим работы (фоновый или простой)
    public void setServiceForeground(boolean isForeground){
        this.isForeground = isForeground;
        if(isForeground) {
            Log.i("CriptoMonitor", "CriptoMonitorService(setServiceForeground): stopForeground");
            stopForeground(true);
        }
        else {
            Log.i("CriptoMonitor", "CriptoMonitorService(setServiceForeground): startForeground");
            startForeground(SERVICE_ID, getNotification(null, 0.0, 0));
        }
    }

    //Геттер и сеттер для интервала обращения сервиса к веб-сервису
    public int getIntervalReload() {
        return intervalReload;
    }
    public void setIntervalReload(int millisec) {
        if (millisec < TIME_RELOAD_MIN)
            intervalReload = TIME_RELOAD_MIN;
        else if (millisec > TIME_RELOAD_MAX)
            intervalReload = TIME_RELOAD_MAX;
        else
            intervalReload = millisec;
        //Перезапускаем таймер с заданием
        serviceTask.cancel();
        serviceTask = new ServiceTimerTask();
        serviceTimer.schedule(serviceTask, 0, intervalReload);
        Log.i("CriptoMonitor", "CriptoMonitorService(updateTimer) interval reload = " + intervalReload);
    }

    //Создаем экземпляр сервиса
    public void onCreate() {
        super.onCreate();
        currenciesMonitoringList = new ArrayList<Currency>();

        //Инициализируем класс библиотеки для работы с веб-сервисом
        retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        jsonApi = retrofit.create(RetrofitInterface.class);

        //настраиваем работу с уведомлениями в зависимости от версии
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder = new NotificationCompat.Builder(this);
        } else {
            if (notificationManager.getNotificationChannel(CRIPTOSERVICE_CHANNEL) == null) {
                NotificationChannel channel = new NotificationChannel(CRIPTOSERVICE_CHANNEL, "CriptoMonitor", NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }
            notificationBuilder = new NotificationCompat.Builder(this, CRIPTOSERVICE_CHANNEL);
        }
        notificationBuilder.setSmallIcon(R.drawable.ic_cripto_notif ).setContentTitle("CriptoMonitor");

        //Настраиваем таймер по умолчанию и запускаем задание
        intervalReload = TIME_RELOAD_MIN;
        serviceTimer = new Timer();
        serviceTask = new ServiceTimerTask();
        serviceTimer.schedule(serviceTask, 0, intervalReload);
    }

    //Метод для проверки достижения границ
    public void isReachedRangeCurrencies() {
        if (currenciesMonitoringList != null && currenciesMonitoringList.size() > 0) {
            for (Currency curr : currenciesMonitoringList) {
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

    //Метод получения спика всех возможных валют на веб-сервисе
    public void getAllCurrencies() {
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

    //Формируем нужное уведомление
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
            return notificationBuilder.setContentText(str).build();
        }
    }

    //Задание сервису, выполняется через intervalReload
    class ServiceTimerTask extends TimerTask {
        public void run() {
            Call<JSonDataCurrencies> call = jsonApi.getData();
            call.enqueue(new Callback<JSonDataCurrencies>() {
                //Метод, вызывающийся, когда приходит ответ от веб-сервиса
                @Override
                public void onResponse(Call<JSonDataCurrencies> call, Response<JSonDataCurrencies> response) {
                    if (response.isSuccessful()) {
                        JSonDataCurrencies result = response.body();
                        //Обновляем данные списка отслеживаемых валют
                        result.updateMonitoringCurrencies(currenciesMonitoringList);
                        //Проверяем достижение текущей цены границ
                        isReachedRangeCurrencies();
                        //Если есть приложение, то отправляем сигнал о том, что данные обновлены
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
}
