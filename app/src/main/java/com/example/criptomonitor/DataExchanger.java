package com.example.criptomonitor;

import java.util.ArrayList;

public interface DataExchanger {
    final int LAYOUT_MAIN = 0;
    final int LAYOUT_ALL = 1;
    final int LAYOUT_ADD = 2;
    final int LAYOUT_RANGE = 3;
    final int LAYOUT_SETTINGS = 4;

    //Возвращает список всех возможных валют с веб-сервиса
    public ArrayList<Currency> getAllCurrencies();
    //Возвращает список валют, выбранных для отслеживания
    public ArrayList<Currency> getMonitoringCurrencies();
    //Устанавливает список валют, выбранных для отслеживания
    public void setCheckCurrencies(ArrayList<Currency> listCurrencies);

    //Задает настройки сервиса
    public void setServerSettings(int time);
   //Возвращает признак работы сервиса
    public int isServiceStart();
    //Возвращает признак режима работы сервиса
    public boolean isServiceStartForeground();
    //Устанавливаем признак режима работы сервиса
    public void setServiceStartForeground(int isForeground);

    //Удаляет валюту
    public void deleteCurrency(Currency currency);
    //Обновляет валюту
    public void updateCurrency(Currency currency);
    //На смену текущей валюты
    public void changeSelectionCurrency();

    //Отображение фрагмента с информацией о валюте
    public void updateRangeFragment();
    //Настраивает вес лэйаутов
    public void updateFragLayout(int typeLayout);

    //Отображает диалоговое окно с сообщением
    public void showAlertDialog(String message);
}
