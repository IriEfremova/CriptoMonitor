package com.example.criptomonitor;

import java.util.ArrayList;

public interface DataExchanger {
    final int LAYOUT_MAIN = 0;
    final int LAYOUT_ALL = 1;
    final int LAYOUT_ADD = 2;
    final int LAYOUT_RANGE = 3;
    final int LAYOUT_SETTINGS = 4;

    //Возвращает список всех возможных валют с веб-сервиса
    ArrayList<Currency> getAllCurrencies();
    //Возвращает список валют, выбранных для отслеживания
    ArrayList<Currency> getMonitoringCurrencies();
    //Устанавливает список валют, выбранных для отслеживания
    void setCheckCurrencies(ArrayList<Currency> listCurrencies);

    //Задает настройки сервиса
    void setServerSettings(int time);
   //Возвращает признак работы сервиса
    int isServiceStart();
    //Возвращает признак режима работы сервиса
    boolean isServiceStartForeground();
    //Устанавливаем признак режима работы сервиса
    void setServiceStartForeground(int isForeground);

    //Удаляет валюту
    void deleteCurrency(Currency currency);
    //Обновляет валюту
    void updateCurrency(Currency currency);
    //На смену текущей валюты
    void changeSelectionCurrency();

    //Отображение фрагмента с информацией о валюте
    void updateRangeFragment();
    //Настраивает вес лэйаутов
    void updateFragLayout(int typeLayout);

    //Отображает диалоговое окно с сообщением
    void showAlertDialog(String message);
}
