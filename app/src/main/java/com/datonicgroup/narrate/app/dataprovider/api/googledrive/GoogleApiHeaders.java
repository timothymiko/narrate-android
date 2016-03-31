package com.datonicgroup.narrate.app.dataprovider.api.googledrive;

import android.text.TextUtils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by timothymiko on 1/13/16.
 */
public class GoogleApiHeaders implements Interceptor {

    private GoogleAccessToken token = new GoogleAccessToken();

    @Override
    public Response intercept(Chain chain) throws IOException {

        String authToken = token.token;

        if (TextUtils.isEmpty(authToken)) {
            boolean successfulRenew = token.renew();
            if (successfulRenew) {
                authToken = token.token;
            } else {
                return null;
            }
        }

        Request original = chain.request();
        Request request = original.newBuilder()
                .header("Accept", "applicaton/json; image/*")
                .header("Authorization", "Bearer " + authToken)
                .build();

        Response response = chain.proceed(request);

        if (response.code() == 403) {
            token.invalidate();
            boolean successfulRenew = token.renew();

            if (successfulRenew) {
                authToken = token.token;

                request = original.newBuilder()
                        .header("Accept", "applicaton/json; image/*")
                        .header("Authorization", "Bearer " + authToken)
                        .build();

                return chain.proceed(request);
            }
        }

        return response;
    }
}
