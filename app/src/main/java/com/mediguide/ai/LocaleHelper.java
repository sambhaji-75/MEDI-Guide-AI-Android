package com.mediguide.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREF_NAME = "MediGuideSettings";
    private static final String LANGUAGE_KEY = "selected_language";

    public static String getLanguage(Context context) {
        SharedPreferences preferences =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return preferences.getString(LANGUAGE_KEY, "en");
    }

    public static void saveLanguage(Context context, String languageCode) {
        SharedPreferences preferences =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        preferences.edit()
                .putString(LANGUAGE_KEY, languageCode)
                .apply();
    }

    public static Context applyLanguage(Context context) {
        String languageCode = getLanguage(context);

        Locale locale = Locale.forLanguageTag(languageCode);
        Locale.setDefault(locale);

        Configuration configuration =
                new Configuration(context.getResources().getConfiguration());

        configuration.setLocale(locale);

        return context.createConfigurationContext(configuration);
    }
}
