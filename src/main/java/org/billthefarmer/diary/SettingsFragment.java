////////////////////////////////////////////////////////////////////////////////
//
//  Diary - Personal diary for Android
//
//  Copyright (C) 2017	Bill Farmer
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//  Bill Farmer	 william j farmer [at] yahoo [dot] co [dot] uk.
//
///////////////////////////////////////////////////////////////////////////////

package org.billthefarmer.diary;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

/**
 * <p>
 *      The type Settings fragment is for use when there is a list of preferences.
 *      These preferences will be populated in the settings menu.
 * </p>
 *
 */
@SuppressWarnings("deprecation")
public class SettingsFragment extends android.preference.PreferenceFragment
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    public final static String STYLES = "file:///android_asset/styles.css";
    public final static String SCRIPT = "file:///android_asset/script.js";
    public final static String CSS_STYLES = "css/styles.css";
    public final static String TEXT_CSS = "text/css";
    public final static String JS_SCRIPT = "js/script.js";
    public final static String TEXT_JAVASCRIPT = "text/javascript";
    public final static String DIARY = "Diary";

    private String folder = DIARY;
    // On create

    /**
     * <p>When the class is made this will run.</p>
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(getActivity());

        // Get folder summary
        EditTextPreference folder =
            (EditTextPreference) findPreference(Settings.PREF_FOLDER);

        // Set folder in text view
        folder.setSummary(preferences.getString(Settings.PREF_FOLDER,
                                                Diary.DIARY));
        // Get index preference
        DatePickerPreference entry =
            (DatePickerPreference) findPreference(Settings.PREF_INDEX_PAGE);

        // Get value
        long value = preferences.getLong(Settings.PREF_INDEX_PAGE,
                                         DatePickerPreference.DEFAULT_VALUE);
        Date date = new Date(value);

        // Set summary
        DateFormat format = DateFormat.getDateInstance();
        String s = format.format(date);
        entry.setSummary(s);

        // Get template preference
        entry =
            (DatePickerPreference) findPreference(Settings.PREF_TEMPLATE_PAGE);

        // Get value
        value = preferences.getLong(Settings.PREF_TEMPLATE_PAGE,
                                    DatePickerPreference.DEFAULT_VALUE);
        date = new Date(value);

        // Set summary
        s = format.format(date);
        entry.setSummary(s);

        // Get about summary
        Preference about = findPreference(Settings.PREF_ABOUT);
        String sum = about.getSummary().toString();

        // Set version in text view
        s = String.format(sum, BuildConfig.VERSION_NAME);
        about.setSummary(s);
    }

    // on Resume

    /**
     * <p>This runs after the class has been paused.</p>
     */
    @Override
    public void onResume()
    {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * <p>This runs when the class is paused.</p>
     */
    // on Pause
    @Override
    public void onPause()
    {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * <p>Returns a true or false based on what was pressed on the preference screen object.</p>
     *
     * @param preferenceScreen
     * @param preference
     * @return                  boolean for whether or not something on in the settings menu is clicked.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference)
    {
        String key = preference.getKey();
        boolean result =
            super.onPreferenceTreeClick(preferenceScreen, preference);

        // Set home as up
        if (preference instanceof PreferenceScreen)
        {
            Dialog dialog = ((PreferenceScreen) preference).getDialog();
            ActionBar actionBar = dialog.getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        //if the edit styles option is selected, launch the editor to read the css file
        if(key.equals(Settings.PREF_EDITSTYLES)){
            //            Log.d("something","launching edit styles");
            //            editStyles();
            File file = new File(getHome(), CSS_STYLES);
            Uri uri = Uri.fromFile(file);
            startActivity(new Intent(Intent.ACTION_EDIT, uri, getActivity(), Editor.class));
        }
        //if the edit script option is selected, launch the editor to read the js file
        if(key.equals(Settings.PREF_EDITSCRIPT)){
            //Diary d = new Diary();
            //            Log.d("something","launching edit script");
            //            editScript();

            File file = new File(getHome(), JS_SCRIPT);
            Uri uri = Uri.fromFile(file);
            startActivity(new Intent(Intent.ACTION_EDIT, uri, getActivity(), Editor.class));
        }

        return result;
    }

    /**
     * <p>
     *     Whenever a preference in the settings menu is changed, the key to that setting is sent to this.
     *     This allows variables in the application to be set and saved.
     * </p>
     *
     * @param preferences the current preference that was editted
     * @param key         the key to which the edited preference has
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences,
                                          String key)
    {
        if (key.equals(Settings.PREF_FOLDER))
        {
            // Get folder summary
            EditTextPreference folder =
                (EditTextPreference) findPreference(key);

            // Set folder in text view
            folder.setSummary(preferences.getString(key, Diary.DIARY));
        }

        if (key.equals(Settings.PREF_INDEX_PAGE))
        {
            // Get index preference
            DatePickerPreference entry =
                (DatePickerPreference) findPreference(key);

            // Get value
            long value =
                preferences.getLong(key, DatePickerPreference.DEFAULT_VALUE);
            Date date = new Date(value);

            // Set summary
            DateFormat format = DateFormat.getDateInstance();
            String s = format.format(date);
            entry.setSummary(s);
        }

        if (key.equals(Settings.PREF_TEMPLATE_PAGE))
        {
            // Get template preference
            DatePickerPreference entry =
                (DatePickerPreference) findPreference(key);

            // Get value
            long value =
                preferences.getLong(key, DatePickerPreference.DEFAULT_VALUE);
            Date date = new Date(value);

            // Set summary
            DateFormat format = DateFormat.getDateInstance();
            String s = format.format(date);
            entry.setSummary(s);
        }

        if (key.equals(Settings.PREF_DARK_THEME))
        {
            if (Build.VERSION.SDK_INT != Build.VERSION_CODES.M)
                getActivity().recreate();
        }
    }

    /**
     * <p>This runs to create files or edit files.</p>
     * @return File this file is where the main directory is.
     */
    private File getHome()
    {
        File file = new File(folder);
        if (file.isAbsolute() && file.isDirectory() && file.canWrite())
            return file;

        return new File(Environment.getExternalStorageDirectory(), folder);
    }
}
