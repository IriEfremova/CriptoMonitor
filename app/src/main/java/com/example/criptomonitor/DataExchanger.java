package com.example.criptomonitor;

import java.util.ArrayList;

public interface DataExchanger {
    final int LAYOUT_MAIN = 0;
    final int LAYOUT_RANGE = 1;
    final int LAYOUT_ALL = 2;

    public ArrayList<Currency> getAllCurrencies();
    public void setCheckCurrencies(ArrayList<Currency> listCurrencies);
    public ArrayList<Currency> getMonitoringCurrencies();
    public void setServerSettings(int time);
    public int isServiceStart();
    public void changeSelectionCurrency();
    public void deleteCurrency(Currency currency);
    public void updateRange();
    public void updateFragLayout(int mode);
    public boolean isServiceStartForeground();
    public void setServiceStartForeground(boolean isForeground);
    public void showAlertDialog(String message);
}
