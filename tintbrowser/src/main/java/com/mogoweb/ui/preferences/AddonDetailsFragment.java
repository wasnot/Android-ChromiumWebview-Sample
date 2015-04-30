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

import java.util.List;

import com.mogoweb.R;
import com.mogoweb.addons.Addon;
import com.mogoweb.controllers.Controller;
import com.mogoweb.ui.managers.UIFactory;

import android.app.Fragment;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;

public class AddonDetailsFragment extends Fragment {
	
	public static final String EXTRA_ADDON_ID = "EXTRA_ADDON_ID";
	
	private View mContainer = null;
	
	private Addon mAddon;
	
	private TextView mName;
	private TextView mShortDesc;
	private TextView mLongDesc;
	private TextView mContact;
	
	private Switch mEnabled;
	private Button mPreferences;
	
	private TextView mCallbacks;
	private TextView mPermissions;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (mContainer == null) {
			mContainer = inflater.inflate(R.layout.addon_details_fragment, container, false);
			
			mName = (TextView) mContainer.findViewById(R.id.AddonName);
			mShortDesc = (TextView) mContainer.findViewById(R.id.AddonShortDesc);
			mLongDesc = (TextView) mContainer.findViewById(R.id.AddonLongDesc);
			mContact = (TextView) mContainer.findViewById(R.id.AddonContact);
			
			mEnabled = (Switch) mContainer.findViewById(R.id.AddonEnabled);
			mPreferences = (Button) mContainer.findViewById(R.id.AddonPreferences);
			
			mEnabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					mAddon.setEnabled(isChecked);
				}
			});
			
			mPreferences.setOnClickListener(new OnClickListener() {				
				@Override
				public void onClick(View v) {
					mAddon.showAddonSettingsActivity();
				}
			});
			
			mCallbacks = (TextView) mContainer.findViewById(R.id.AddonCallbacks);
			mPermissions = (TextView) mContainer.findViewById(R.id.AddonPermissions);
		}
		
		Bundle args = getArguments();
		if (args != null) {
			mAddon = Controller.getInstance().getAddonManager().getAddons().get(args.getInt(EXTRA_ADDON_ID));
			
			mName.setText(mAddon.getName());
			mShortDesc.setText(mAddon.getShortDescription());
			mLongDesc.setText(mAddon.getDescription());
			mContact.setText(String.format(getString(R.string.AddonDetailsContact), mAddon.getContact()));
			
			mEnabled.setChecked(mAddon.isEnabled());
			mPreferences.setEnabled(mAddon.hasSettingsPage());
			
			fillCallbacksDetails();
			fillPackagePermissions();			
			
			if (!UIFactory.isTablet(getActivity())) {
        		// The current addon name is currently shown in tablet-type preferences activity / fragments.
        		getActivity().setTitle(mAddon.getName());
        	}
		}
		
		return mContainer;
	}

	private void fillCallbacksDetails() {
		List<String> callbacks = mAddon.getUserReadbleCallbacks();
		
		StringBuilder sb = new StringBuilder();
		
		for (String callback : callbacks) {
			if (sb.length() > 0) {
				sb.append('\n');
			}
			
			sb.append("• " + callback);
		}
		
		mCallbacks.setText(sb.toString());
	}
	
	private void fillPackagePermissions() {
		ResolveInfo info = mAddon.getResolveInfo();
		
		StringBuilder sb = new StringBuilder();
		
		if ((info != null) &&
				(info.serviceInfo != null) &&
				(info.serviceInfo.packageName != null)) {
			try {
				PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(info.serviceInfo.packageName, PackageManager.GET_PERMISSIONS);
				String[] permissions = packageInfo.requestedPermissions;
				
				if ((permissions != null) &&
						(permissions.length > 0)) {
					for (int i = 0; i < permissions.length; i++) {
						if (sb.length() > 0) {
							sb.append('\n');
						}

						sb.append("• " + permissions[i]);
					}
				} else {
					sb.append(getString(R.string.AddonDetailsPermissionsNone));
				}
				
				mPermissions.setText(sb.toString());
				
			} catch (NameNotFoundException e) {
				sb.append(getString(R.string.AddonDetailsUnableToGetPermissions));
			}
		} else {
			sb.append(getString(R.string.AddonDetailsUnableToGetPermissions));
		}
		
		mPermissions.setText(sb.toString());
	}

}
