package com.datonicgroup.narrate.app.dataprovider.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;

/**
 * Created by timothymiko on 1/12/16.
 */
public class ServiceGenerator {

    private ServiceGenerator() {
    }

    public static <S> S createService(Class<S> serviceClass, Interceptor apiHeaders, String baseUrl) {

        OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
        if (apiHeaders != null) {
            httpBuilder.interceptors().add(apiHeaders);
        }

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .create();

        Retrofit.Builder builder = new Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(httpBuilder.build())
                        .addConverterFactory(GsonConverterFactory.create(gson));

        return builder.build().create(serviceClass);
    }
}
