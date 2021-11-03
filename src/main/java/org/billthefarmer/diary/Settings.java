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

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;


// Settings

/**
 * This is the activity that runs or maybe it was replaced with the settings fragment,
 * but the global variables are still used elsewhere such as Diary and SettingsFragment.
 *
 */
public class Settings extends Activity
{
    public final static String PREF_ABOUT = "pref_about";
    public final static String PREF_CUSTOM = "pref_custom";
    public final static String PREF_FOLDER = "pref_folder";
    public final static String PREF_EXTERNAL = "pref_external";
    public final static String PREF_MARKDOWN = "pref_markdown";
    public final static String PREF_EDITSTYLES = "pref_styles";
    public final static String PREF_EDITSCRIPT = "pref_script";
    public final static String PREF_USE_INDEX = "pref_use_index";
    public final static String PREF_INDEX_PAGE = "pref_index_page";
    public final static String PREF_USE_TEMPLATE = "pref_use_template";
    public final static String PREF_TEMPLATE_PAGE = "pref_template_page";
    public final static String PREF_COPY_MEDIA = "pref_copy_media";
    public final static String PREF_DARK_THEME = "pref_dark_theme";

    /**
     * <p>When the class is made this will run.</p>
     *
     * @param savedInstanceState
     */
    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        boolean darkTheme =
            preferences.getBoolean(PREF_DARK_THEME, false);

        if (darkTheme)
            setTheme(R.style.AppDarkTheme);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
            .replace(android.R.id.content, new SettingsFragment())
            .commit();

        // Enable back navigation on action bar
        ActionBar actionBar = getActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.settings);
        }
    }

    /**
     * <p>Whenever a physical button is pressed this also gets run.</p>
     * @param item the iterm being selected
     * @return true if something is supposed to happen when the item is pressed.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Home, finish
        if (item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }

        return false;
    }

}
