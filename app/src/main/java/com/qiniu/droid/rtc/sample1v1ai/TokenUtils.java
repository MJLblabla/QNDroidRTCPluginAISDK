package com.qiniu.droid.rtc.sample1v1ai;

import android.util.Log;

import com.qiniu.util.Auth;

import java.io.UnsupportedEncodingException;

public class TokenUtils {

    private static final String ENCODING = "UTF-8";

    static String appKey = "";
    static String appId = "";
    static String appSecretKey = "";

    public static String resetToken() {

        long exp = System.currentTimeMillis() / 1000 + 67 * 60 * 60 * 12;
        String scr = appId + ":" + exp;
        // String scr = appId+ ":" +1623843922;
        try {
            String encodedSrc = com.qiniu.util.Base64.encodeToString(scr.getBytes(ENCODING), com.qiniu.util.Base64.URL_SAFE).trim();
            String tokenAi= "QD " + Auth.create(appKey, appSecretKey).sign(encodedSrc) + ":" + encodedSrc;
            Log.d("mjl","tokenAi "+tokenAi);
            return tokenAi;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String signUrlToToken(String url) {
        return Auth.create(appKey, appSecretKey).sign(url);// "${appKey}:${sha1}"
    }
}
