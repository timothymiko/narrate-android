package com.datonicgroup.narrate.app.dataprovider.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by timothymiko on 1/13/16.
 */
public abstract class AccessToken {

    public interface AccessTokenRenewalCallback {
        void onSuccess();
        void onFailure();
    }

    public String token;
    protected long expiration;
    protected boolean mIsRenewing;
    protected List<AccessTokenRenewalCallback> mRenewalCallbacks = new ArrayList<>();

    public boolean isExpired() {
        return System.currentTimeMillis() > expiration;
    }

    public abstract void renew(AccessTokenRenewalCallback callback);
    public abstract boolean invalidate();
}
