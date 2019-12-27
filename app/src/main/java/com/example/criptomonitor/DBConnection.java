package com.example.criptomonitor;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

public class DBConnection extends SQLiteOpenHelper {

    private static DBConnection instance;

    private DBConnection(Context context) {
        super(context, "CurrenciesDB", null, 1);
    }

    public static DBConnection getInstance(Context context) {
        if (instance == null) {
            instance = new DBConnection(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table Currencies ("
                + "id integer primary key autoincrement,"
                + "name text,"
                + "min_price real,"
                + "max_price real" + ");");
        db.execSQL("create table Settings ("
                + "id integer primary key autoincrement,"
                + "name text,"
                + "value integer" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }

    //Возвращаем интервал работы сервиса
    public int getInterval() {
        Cursor c = getWritableDatabase().query("Settings", null, null, null, null, null, null);
        int nameColIndex = c.getColumnIndex("name");
        // ставим позицию курсора на первую строку выборки
        if (c.moveToFirst()) {
            do {
                String name = c.getString(nameColIndex);
                if (name.equals("interval")) {
                    // определяем номера столбцов по имени в выборке
                    int valueColIndex = c.getColumnIndex("value");
                    c.close();
                    return c.getInt(valueColIndex);
                }
            } while (c.moveToNext());
        }
        c.close();
        return -1;
    }

    //Возвращаем признак работы сервиса
    //0 - в стандартном режиме
    //1 - в фоновом режиме
    public int getForeground() {
        Cursor c = getWritableDatabase().query("Settings", null, null, null, null, null, null);
        int nameColIndex = c.getColumnIndex("name");
        // ставим позицию курсора на первую строку выборки
        if (c.moveToFirst()) {
            do {
                String name = c.getString(nameColIndex);
                if (name.equals("foreground")) {
                    // определяем номера столбцов по имени в выборке
                    int valueColIndex = c.getColumnIndex("value");
                    c.close();
                    return c.getInt(valueColIndex);
                }
            } while (c.moveToNext());
        }
        c.close();
        return -1;
    }

    //Обновляем данные входного списка данными из БД по отслеживаемым валютам
    public void updateListMonitoringCurrencies(ArrayList<Currency> list) {
        Cursor c = getWritableDatabase().query("Currencies", null, null, null, null, null, null);
        // ставим позицию курсора на первую строку выборки
        if (c.moveToFirst()) {
            // определяем номера столбцов по имени в выборке
            int nameColIndex = c.getColumnIndex("name");
            do {
                boolean flagContains = false;
                int minPriceColIndex = c.getColumnIndex("min_price");
                int maxPriceColIndex = c.getColumnIndex("max_price");

                for (int i = 0; i < list.size(); i++) {
                    if (c.getString(nameColIndex).equals(list.get(i).getName())) {
                        list.get(i).setMaxPrice(c.getDouble(maxPriceColIndex));
                        list.get(i).setMinPrice(c.getDouble(minPriceColIndex));
                        flagContains = true;
                        break;
                    }
                }
                if (flagContains == false) {
                    list.add(new Currency(c.getString(nameColIndex), 0.0, c.getDouble(minPriceColIndex), c.getDouble(maxPriceColIndex)));
                }

            } while (c.moveToNext());
        }
        c.close();
        Log.i("CriptoMonitor", "DBConnection(updateListMonitoringCurrencies): List count = " + list.size());
    }

    //Записываем данные в таблицу настроек
    public void insertSettings(String name, int value) {
        String sql = String.format(Locale.US, "INSERT INTO Settings(name, value) VALUES('%s',%f)", name, value);
        Log.i("CriptoMonitor", "DBConnection(insertSettings): " + sql);
        getWritableDatabase().execSQL(sql);
    }

    //Записываем данные по валюте в БД
    public void insertCurrency(Currency curr) {
        String sql = String.format(Locale.US, "INSERT INTO Currencies(name, min_price, max_price) VALUES('%s',%f,%f)", curr.getName(), curr.getMinPrice(), curr.getMaxPrice());
        Log.i("CriptoMonitor", "DBConnection(insertCurrency): " + sql);
        getWritableDatabase().execSQL(sql);
    }

    //Удаляем данные по валюте из БД
    public void deleteCurrency(Currency curr) {
        String sql = String.format(Locale.US, "DELETE FROM Currencies WHERE name='%s'", curr.getName());
        Log.i("CriptoMonitor", "DBConnection(deleteCurrency): " + sql);
        getWritableDatabase().execSQL(sql);
    }

    //Обновляем данные по валюте в БД
    public void updateCurrency(Currency curr) {
        String sql = String.format(Locale.US, "UPDATE Currencies SET max_price=%f, min_price=%f WHERE name='%s'", curr.getMaxPrice(), curr.getMinPrice(), curr.getName());
        Log.i("CriptoMonitor", "DBConnection(updateCurrency): " + sql);
        getWritableDatabase().execSQL(sql);
    }

    //Закрываем соединение с БД
    public void closeDB() {
        getWritableDatabase().close();
    }
}
