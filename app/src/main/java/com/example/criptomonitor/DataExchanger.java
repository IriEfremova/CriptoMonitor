package com.example.criptomonitor;

import java.util.ArrayList;

public interface DataExchanger {
    public ArrayList<Currency> getAllCurrencies();
    public void setCheckCurrencies(ArrayList<Currency> listCurrencies);
    public ArrayList<Currency> getMonitoringCurrencies();
    public void setMonitoringCurrencies(ArrayList<Currency> listCurrencies);
    public void setServerSettings(int time, boolean turnOn);
    public boolean isServiceStart();
}
