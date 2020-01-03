package com.example.criptomonitor;

import android.os.Parcel;
import android.os.Parcelable;

//Класс хранения данных криптовалюты.
//Класс поддерживает интерфейс Parcelable, чтобы была возможность передавать объекты этого класса
//между сервисом и активностью

public class Currency implements Parcelable {
    //Наименование валюты
    private String name;
    //Текущая цена валюты, хранится для всех
    private double price;
    //Стоп-цены для мониторинга, минимальная и максимальная граница
    //если эти цена равны -1, то валюта просто отображается в списке,
    //но не проверяется на достижение границ
    private double minPrice;
    private double maxPrice;

    //Конструктор, по умолчанию текущая цена по нулям, границ нет
    public Currency(String name, double price, double minValue, double maxValue) {
        this.name = name;
        this.price = price;
        this.minPrice = minValue;
        this.maxPrice = maxValue;
    }

    //Конструктор, по умолчанию текущая цена по нулям, границ нет
    public Currency(String name, double price) {
        this.name = name;
        this.price = price;
        this.minPrice = -1.0;
        this.maxPrice = -1.0;
    }

    //Метод заменяет данные текущего объекта на данные входного
    public void replace(Currency currency) {
        this.name = currency.name;
        this.price = currency.price;
        this.minPrice = currency.minPrice;
        this.maxPrice = currency.maxPrice;
    }

    //Наименование валюты
    public void setName(String value){ this.name = value; }
    public String getName(){ return this.name;}

    //Текущая цена
    public void setPrice(double value){
        this.price = value;
    }
    public double getPrice(){ return this.price; }

    //Задаем нижнюю цену для мониторинга
    public void setMinPrice(double value){ this.minPrice = value; }
    //Получаем нижнюю цену для мониторинга
    public double getMinPrice(){ return this.minPrice; }

    //Задаем нижнюю цену для мониторинга
    public void setMaxPrice(double value){ this.maxPrice = value; }
    //Получаем нижнюю цену для мониторинга
    public double getMaxPrice(){ return this.maxPrice; }

    public void clearMinMaxPrice(){
        this.minPrice = -1.0;
        this.maxPrice = -1.0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if(obj.getClass() == this.getClass() && ((Currency)obj).getName().equals(this.name))
            return true;

        return false;
    }

    //Нижеследующие методы для поддержки parcelabel-интерфейса
    protected Currency(Parcel in) {
        this.name = in.readString();
        this.price = in.readDouble();
        this.minPrice = in.readDouble();
        this.maxPrice = in.readDouble();
    }

    public static final Creator<Currency> CREATOR = new Creator<Currency>() {
        @Override
        public Currency createFromParcel(Parcel in) {
            return new Currency(in);
        }

        @Override
        public Currency[] newArray(int size) {
            return new Currency[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeDouble(price);
        parcel.writeDouble(minPrice);
        parcel.writeDouble(maxPrice);
    }
}
