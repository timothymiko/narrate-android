package com.datonicgroup.narrate.app.dataprovider.api.googledrive;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.exceptions.InvalidAccountTypeException;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.exceptions.MissingGoogleAccountException;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.exceptions.PermissionsException;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;

import java.io.IOException;

/**
 * Created by timothymiko on 1/12/16.
 */
public class GoogleAccountsService {

    public static void setAccount(Account account) throws InvalidAccountTypeException {
        if (!account.type.startsWith("com.google")) {
            throw new InvalidAccountTypeException("com.google", account.type);
        }

        Settings.setGoogleAccountName(account.name);
    }

    private static Account getAccount() throws MissingGoogleAccountException, PermissionsException {
        String accountName = Settings.getGoogleAccountName();

        if (ContextCompat.checkSelfPermission(GlobalApplication.getAppContext(), Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            throw new PermissionsException(Manifest.permission.GET_ACCOUNTS);
        } else {

            AccountManager manager = AccountManager.get(GlobalApplication.getAppContext());
            Account[] accounts = manager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);

            for (Account acc: accounts) {
                if (acc.name.equals(accountName)) {
                    return acc;
                }
            }

            throw new MissingGoogleAccountException("GoogleAccountsService");

        }
    }

    /**
     * This method is used to retrieve an Oauth2 token to authenticate with Google's REST APIs.
     * For some reason, using Google Play Service's
     *
     * @param scope This is the Authorization scope that defines the permissions for the Oauth token.
     *              For more info on the scopes for Google Drive, see:
     *              https://developers.google.com/drive/v3/web/about-auth
     *
     * @return Oauth2 token that can be used as a Bearer to authenticate with Google's REST APIs
     */
    public static String getAuthToken(String scope) throws MissingGoogleAccountException, PermissionsException, IOException, GoogleAuthException {
        Account acc = getAccount();
        String token = GoogleAuthUtil.getToken(GlobalApplication.getAppContext(), acc, "oauth2:" + scope);
        return token;
    }

    public static boolean invalidateToken(String token) throws IOException, GoogleAuthException {
        GoogleAuthUtil.clearToken(GlobalApplication.getAppContext(), token);
        return true;
    }
}
