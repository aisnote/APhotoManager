/*
 * Copyright (c) 2015-2020 by k3b.
 *
 * This file is part of AndroFotoFinder and of ToGoZip.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */

package de.k3b.android.widget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.util.Locale;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.util.UserTheme;

/**
 * An activity that can change the locale (language) of its content.
 *
 * Inspired by http://stackoverflow.com/questions/13181847/change-the-locale-at-runtime
 *
 * Created by k3b on 07.01.2016.
 */
public abstract class LocalizedActivity extends ActivityWithCallContext {
    /**
     * if this.recreationId != LocalizedActivity.currentRecreationId : activity must be recreated in on resume
     */
    private static int currentRecreationId = 0;
    private int recreationId = 0;

    /** if myLocale != Locale.Default : activity must be recreated in on resume */
    private Locale myLocale = null;

    /**
     * All activities will be recreated in on resume. I.E. after basic configuration change.
     */
    public static void setMustRecreate() {
        LocalizedActivity.currentRecreationId++;
    }

    /**
     * Set Activity-s locale to SharedPreferences-setting.
     * Must be called before
     */
    public static void fixThemeAndLocale(Activity context) {
        UserTheme.setTheme(context);
        fixLocale(context);
    }

    private static void fixLocale(Context context) {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        String language = prefs.getString(Global.PREF_KEY_USER_LOCALE, "");
        Locale locale = Global.systemLocale; // in case that setting=="use android-locale"
        if ((language != null) && (language.length() > 0)) {
            // i.e. "de" for german or "pt-BR" for portogeese in brasilia
            String[] languageParts = language.split("-");
            locale = (languageParts.length == 1) ? new Locale(language) : new Locale(languageParts[0], languageParts[1]); // overwrite "use android-locale"
        }

        if (locale != null) {
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            Resources resources = context.getResources();
            resources.updateConfiguration(config, resources.getDisplayMetrics());
            // recreate();

            if (context instanceof LocalizedActivity) {
                final LocalizedActivity localizedActivity = (LocalizedActivity) context;
                localizedActivity.myLocale = locale;
                localizedActivity.recreationId = LocalizedActivity.currentRecreationId;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        fixThemeAndLocale(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Locale has changed by other Activity ?
        if (mustRecreate()) {
            myLocale = null;
            recreate(LocalizedActivity.this);
        }
    }

    protected boolean mustRecreate() {
        return ((this.recreationId != LocalizedActivity.currentRecreationId) ||
                (this.myLocale != null) && (this.myLocale.getLanguage() != Locale.getDefault().getLanguage()));
    }

    /** force all open activity to recreate */
    public static void recreate(Activity child) {
        Activity context = child;
        while (context != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                context.recreate();
            } else {
                // https://stackoverflow.com/questions/11495130/android-recreate-functions-in-api-7
                context.startActivity(new Intent(context, context.getClass()));
                context.finish();
            }
            context = context.getParent();
        }
    }

    @Override public String toString() {
        return getClass().getSimpleName();
    }
}
