package com.intervigil.micdroid.helper;

import android.view.View;

public class AdHelper {

    private AdHelper() {}

    public static void updateAdView(View ad, boolean enabled) {
        if (ad != null) {
            ad.setVisibility(View.GONE);
        }
    }
}