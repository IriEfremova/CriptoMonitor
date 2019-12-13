package com.example.criptomonitor;

import retrofit2.Call;
import retrofit2.http.GET;

//Интерфейс работы с библиотекой Retrofit, метод получения данных от веб-сервиса
public interface RetrofitInterface {
    @GET("/coins.json")
    public Call<JSonDataCurrencies> getData();
}



