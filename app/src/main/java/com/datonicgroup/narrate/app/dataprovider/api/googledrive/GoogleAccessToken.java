package com.datonicgroup.narrate.app.dataprovider.api.googledrive;

import com.datonicgroup.narrate.app.dataprovider.api.AccessToken;
import com.datonicgroup.narrate.app.util.DateUtil;

import retrofit2.http.PATCH;

/**
 * Created by timothymiko on 1/13/16.
 */
public class GoogleAccessToken extends AccessToken {

    public GoogleAccessToken() {
        renew();
    }

    @Override
    public void renew(final AccessTokenRenewalCallback callback) {

        if (callback != null) {
            mRenewalCallbacks.add(callback);
        }

        if (!mIsRenewing) {
            mIsRenewing = true;

            new Thread(new Runnable() {
                @Override
                public void run() {

                    boolean success = renew();

                    if (success) {
                        for (int i = 0; i < mRenewalCallbacks.size(); i++) {
                            mRenewalCallbacks.get(i).onSuccess();
                        }
                    } else {
                        for (int i = 0; i < mRenewalCallbacks.size(); i++) {
                            mRenewalCallbacks.get(i).onFailure();
                        }
                    }
                }
            });
        }
    }

    public boolean renew() {
        try {

            String newAccessToken = GoogleAccountsService.getAuthToken("https://www.googleapis.com/auth/drive.appfolder");
            token = newAccessToken;

            if (newAccessToken != null) {
                expiration = System.currentTimeMillis() + (DateUtil.HOUR_IN_MILLISECONDS);

                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }
    }

    @Override
    public boolean invalidate() {
        try {

            return GoogleAccountsService.invalidateToken(token);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
