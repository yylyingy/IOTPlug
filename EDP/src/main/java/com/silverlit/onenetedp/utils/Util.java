package com.silverlit.onenetedp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Yangyl on 2016/10/21.
 */

public class Util {

    public static final String API_KEY = "DhSSAeg0ufNI047x6alRwhahcnQ=";
    public static final String DEV_ID   = "3507219";
    public static boolean isNetWorkAvailable(Context context){
        if (context != null){
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null){
                NetworkInfo info = cm.getActiveNetworkInfo();
                return info != null && info.isConnected();
            }
        }
        return false;
    }
}
