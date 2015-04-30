/*
 * Tint Browser for Android
 * 
 * Copyright (C) 2012 - to infinity and beyond J. Devauchelle and contributors.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package com.mogoweb.ui.preferences;

import com.mogoweb.R;
import com.mogoweb.ui.managers.UIFactory;
import com.mogoweb.utils.ApplicationUtils;
import com.mogoweb.utils.Constants;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class GeneralPreferencesFragment extends PreferenceFragment {
	
	private OnSharedPreferenceChangeListener mListener;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_general_settings);
        
        PreferenceCategory oldPhoneUIcategory = (PreferenceCategory) findPreference("PREFERENCE_CATEGORY_OLD_PHONE_UI");
        PreferenceCategory newPhoneUIcategory = (PreferenceCategory) findPreference("PREFERENCE_CATEGORY_NEW_PHONE_UI");
        PreferenceCategory tabletUIcategory = (PreferenceCategory) findPreference("PREFERENCE_CATEGORY_TABLET_UI");
        
        switch (UIFactory.getUIType(getActivity())) {
        case PHONE:
        	getPreferenceScreen().removePreference(oldPhoneUIcategory);
        	getPreferenceScreen().removePreference(tabletUIcategory);
        	break;
        
        case LEGACY_PHONE:
        	getPreferenceScreen().removePreference(newPhoneUIcategory);
        	getPreferenceScreen().removePreference(tabletUIcategory);
        	break;
        
        case TABLET:
        	getPreferenceScreen().removePreference(newPhoneUIcategory);
        	getPreferenceScreen().removePreference(oldPhoneUIcategory);
        	break;
        }
        
        mListener = new OnSharedPreferenceChangeListener() {

			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				if (Constants.PREFERENCE_UI_TYPE.equals(key)) {
					askForRestart();
				}				
			}			
		};
		
		PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(mListener);
	}

	@Override
	public void onDestroy() {
		PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(mListener);
		super.onDestroy();
	}
	
	private void askForRestart() {
		ApplicationUtils.showYesNoDialog(getActivity(),
				android.R.drawable.ic_dialog_alert,
				R.string.RestartDialogTitle,
				R.string.RestartDialogMessage,
				new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Activity activity = getActivity();
				
				PendingIntent intent = PendingIntent.getActivity(activity.getBaseContext(), 0, new Intent(activity.getIntent()), activity.getIntent().getFlags());
				AlarmManager mgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
				mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, intent);
				System.exit(2);
			}
		});
	}

}
