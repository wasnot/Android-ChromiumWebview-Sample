/*
 * Tint Browser for Android
 *
 * Copyright (C) mogoweb.
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

package com.mogoweb.ui.components;

import java.util.List;
import java.util.UUID;

import org.apache.http.HeaderElement;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import com.mogoweb.R;
import com.mogoweb.addons.AddonMenuItem;
import com.mogoweb.controllers.Controller;
import com.mogoweb.model.DownloadItem;
import com.mogoweb.ui.activities.TintBrowserActivity;
import com.mogoweb.ui.dialogs.DownloadConfirmDialog;
import com.mogoweb.ui.fragments.BaseWebViewFragment;
import com.mogoweb.ui.managers.UIManager;
import com.mogoweb.utils.ApplicationUtils;
import com.mogoweb.utils.Constants;
import com.mogoweb.utils.UrlUtils;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import com.mogoweb.chrome.CookieManager;
import com.mogoweb.chrome.DownloadListener;
import com.mogoweb.chrome.WebSettings;
import com.mogoweb.chrome.WebSettings.PluginState;
import com.mogoweb.chrome.WebView;
import android.widget.Toast;

public class CustomWebView extends WebView implements DownloadListener, DownloadConfirmDialog.IUserActionListener {

	private UIManager mUIManager;
	private Context mContext;
	private BaseWebViewFragment mParentFragment;

	private boolean mIsLoading = false;
	private boolean mPrivateBrowsing = false;

	public CustomWebView(UIManager uiManager, boolean privateBrowsing) {
		this(uiManager.getMainActivity(), null, privateBrowsing);
		mUIManager = uiManager;
	}

	// Used only by edit mode (UI designer)
	public CustomWebView(Context context, AttributeSet attrs) {
		super(context, attrs, android.R.attr.webViewStyle);
		mContext = context;
	}

	public CustomWebView(Context context, AttributeSet attrs, boolean privateBrowsing) {
		super(context, attrs, android.R.attr.webViewStyle);
		mPrivateBrowsing = privateBrowsing;

		mContext = context;

		if (!isInEditMode()) {
			loadSettings();
			setupContextMenu();
		}
	}

	public void setParentFragment(BaseWebViewFragment parentFragment) {
		mParentFragment = parentFragment;
	}

	public BaseWebViewFragment getParentFragment() {
		return mParentFragment;
	}

	public UUID getParentFragmentUUID() {
		return mParentFragment.getUUID();
	}

	public boolean isLoading() {
		return mIsLoading;
	}

	public boolean isPrivateBrowsingEnabled() {
		return mPrivateBrowsing;
	}

	@Override
	public void loadUrl(String url) {
		if ((url != null) &&
    			(url.length() > 0)) {

			if (UrlUtils.isUrl(url)) {
    			url = UrlUtils.checkUrl(url);
    		} else {
    			url = UrlUtils.getSearchUrl(mContext, url);
    		}

			super.loadUrl(url);
		}
	}

	public void loadRawUrl(String url) {
		super.loadUrl(url);
	}

	public void onClientPageStarted(String url) {
		mIsLoading = true;

		if (!isPrivateBrowsingEnabled()) {
			Controller.getInstance().getAddonManager().onPageStarted(mContext, this, url);
		}
	}

	public void onClientPageFinished(String url) {
		mIsLoading = false;

		if (!isPrivateBrowsingEnabled()) {
			Controller.getInstance().getAddonManager().onPageFinished(mContext, this, url);
		}

		mUIManager.onClientPageFinished(this, url);
	}

	@SuppressLint("SetJavaScriptEnabled")
	public void loadSettings() {
		WebSettings settings = getSettings();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

		settings.setJavaScriptEnabled(prefs.getBoolean(Constants.PREFERENCE_ENABLE_JAVASCRIPT, true));
		settings.setLoadsImagesAutomatically(prefs.getBoolean(Constants.PREFERENCE_ENABLE_IMAGES, true));
		settings.setUseWideViewPort(prefs.getBoolean(Constants.PREFERENCE_USE_WIDE_VIEWPORT, true));
		settings.setLoadWithOverviewMode(prefs.getBoolean(Constants.PREFERENCE_LOAD_WITH_OVERVIEW, false));

		settings.setGeolocationEnabled(prefs.getBoolean(Constants.PREFERENCE_ENABLE_GEOLOCATION, true));
		settings.setSaveFormData(prefs.getBoolean(Constants.PREFERENCE_REMEMBER_FORM_DATA, true));
		settings.setSavePassword(prefs.getBoolean(Constants.PREFERENCE_REMEMBER_PASSWORDS, true));

		settings.setTextZoom(prefs.getInt(Constants.PREFERENCE_TEXT_SCALING, 100));

		int minimumFontSize = prefs.getInt(Constants.PREFERENCE_MINIMUM_FONT_SIZE, 1);
		settings.setMinimumFontSize(minimumFontSize);
		settings.setMinimumLogicalFontSize(minimumFontSize);

		settings.setUserAgentString(prefs.getString(Constants.PREFERENCE_USER_AGENT, Constants.USER_AGENT_ANDROID));
//		settings.setPluginState(PluginState.valueOf(prefs.getString(Constants.PREFERENCE_PLUGINS, PluginState.ON_DEMAND.toString())));

		CookieManager.getInstance().setAcceptCookie(prefs.getBoolean(Constants.PREFERENCE_ACCEPT_COOKIES, true));

		settings.setSupportZoom(true);
		settings.setDisplayZoomControls(false);
		settings.setBuiltInZoomControls(true);
		settings.setSupportMultipleWindows(true);
		settings.setEnableSmoothTransition(true);

		if (mPrivateBrowsing) {
			settings.setGeolocationEnabled(false);
			settings.setSaveFormData(false);
			settings.setSavePassword(false);

			settings.setAppCacheEnabled(false);
			settings.setDatabaseEnabled(false);
			settings.setDomStorageEnabled(false);
		} else {
			// HTML5 API flags
			settings.setAppCacheEnabled(true);
			settings.setDatabaseEnabled(true);
			settings.setDomStorageEnabled(true);

			// HTML5 configuration settings.
			settings.setAppCacheMaxSize(3 * 1024 * 1024);
			settings.setAppCachePath(mContext.getDir("appcache", 0).getPath());
			settings.setDatabasePath(mContext.getDir("databases", 0).getPath());
			settings.setGeolocationDatabasePath(mContext.getDir("geolocation", 0).getPath());
		}

		setLongClickable(true);
		setDownloadListener(this);
	}

	@Override
	public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
		DownloadItem item = new DownloadItem(url);
		item.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));

		String fileName = item.getFileName();
		BasicHeader header = new BasicHeader("Content-Disposition", contentDisposition);
		HeaderElement[] helelms = header.getElements();
		if (helelms.length > 0) {
		    HeaderElement helem = helelms[0];
		    if (helem.getName().equalsIgnoreCase("attachment")) {
		        NameValuePair nmv = helem.getParameterByName("filename");
		        if (nmv != null) {
		        	fileName = nmv.getValue();
		        }
		    }
		}
		item.setFilename(fileName);
		item.setIncognito(isPrivateBrowsingEnabled());

		DownloadConfirmDialog dialog = new DownloadConfirmDialog(getContext())
			.setDownloadItem(item)
			.setCallbackListener(this);
		dialog.show();
	}

	@Override
	public void onAcceptDownload(DownloadItem item) {
		long id = ((DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(item);
		item.setId(id);

		Controller.getInstance().getDownloadsList().add(item);

		Toast.makeText(mContext, String.format(mContext.getString(R.string.DownloadStart), item.getFileName()), Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onDenyDownload() {
	}

	private Intent createIntent(String action, int actionId, int hitTestResult, String url) {
		Intent result = new Intent(getContext(), TintBrowserActivity.class);
		result.setAction(action);
		result.putExtra(Constants.EXTRA_ACTION_ID, actionId);
		result.putExtra(Constants.EXTRA_HIT_TEST_RESULT, hitTestResult);
		result.putExtra(Constants.EXTRA_URL, url);
		result.putExtra(Constants.EXTRA_INCOGNITO, isPrivateBrowsingEnabled());

		return result;
	}

	private void createContributedContextMenu(ContextMenu menu, int hitTestResult, String url) {
		if (!isPrivateBrowsingEnabled()) {
			MenuItem item;

			List<AddonMenuItem> contributedItems = Controller.getInstance().getAddonManager().getContributedLinkContextMenuItems(this, hitTestResult, url);
			for (AddonMenuItem contribution : contributedItems) {
				item = menu.add(0, contribution.getAddon().getMenuId(), 0, contribution.getMenuItem());
				item.setIntent(createIntent(Constants.ACTION_BROWSER_CONTEXT_MENU, contribution.getAddon().getMenuId(), hitTestResult, url));
			}
		}
	}

	private void setupContextMenu() {
		setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				HitTestResult result = ((WebView) v).getHitTestResult();
				int resultType = result.getType();

				if ((resultType == HitTestResult.ANCHOR_TYPE) ||
						(resultType == HitTestResult.IMAGE_ANCHOR_TYPE) ||
						(resultType == HitTestResult.SRC_ANCHOR_TYPE) ||
						(resultType == HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) {

					MenuItem item = menu.add(0, TintBrowserActivity.CONTEXT_MENU_OPEN, 0, R.string.ContextMenuOpen);
					item.setIntent(createIntent(Constants.ACTION_BROWSER_CONTEXT_MENU, TintBrowserActivity.CONTEXT_MENU_OPEN, resultType, result.getExtra()));

					item = menu.add(0, TintBrowserActivity.CONTEXT_MENU_OPEN_IN_NEW_TAB, 0, R.string.ContextMenuOpenNewTab);
					item.setIntent(createIntent(Constants.ACTION_BROWSER_CONTEXT_MENU, TintBrowserActivity.CONTEXT_MENU_OPEN_IN_NEW_TAB, resultType, result.getExtra()));

					item = menu.add(0, TintBrowserActivity.CONTEXT_MENU_OPEN_IN_BACKGROUND, 0, R.string.ContextMenuOpenInBackground);
					item.setIntent(createIntent(Constants.ACTION_BROWSER_CONTEXT_MENU, TintBrowserActivity.CONTEXT_MENU_OPEN_IN_BACKGROUND, resultType, result.getExtra()));

					item = menu.add(0, TintBrowserActivity.CONTEXT_MENU_COPY, 0, R.string.ContextMenuCopyLinkUrl);
					item.setIntent(createIntent(Constants.ACTION_BROWSER_CONTEXT_MENU, TintBrowserActivity.CONTEXT_MENU_COPY, resultType, result.getExtra()));

					item = menu.add(0, TintBrowserActivity.CONTEXT_MENU_DOWNLOAD, 0, R.string.ContextMenuDownload);
					item.setIntent(createIntent(Constants.ACTION_BROWSER_CONTEXT_MENU, TintBrowserActivity.CONTEXT_MENU_DOWNLOAD, resultType, result.getExtra()));

					item = menu.add(0, TintBrowserActivity.CONTEXT_MENU_SHARE, 0, R.string.ContextMenuShareLinkUrl);
					item.setIntent(createIntent(Constants.ACTION_BROWSER_CONTEXT_MENU, TintBrowserActivity.CONTEXT_MENU_SHARE, resultType, result.getExtra()));

					createContributedContextMenu(menu, resultType, result.getExtra());

					menu.setHeaderTitle(result.getExtra());

				} else if (resultType == HitTestResult.IMAGE_TYPE) {

					MenuItem item = menu.add(0, TintBrowserActivity.CONTEXT_MENU_OPEN, 0, R.string.ContextMenuViewImage);
					item.setIntent(createIntent(Constants.ACTION_BROWSER_CONTEXT_MENU, TintBrowserActivity.CONTEXT_MENU_OPEN, resultType, result.getExtra()));

					item = menu.add(0, TintBrowserActivity.CONTEXT_MENU_OPEN_IN_NEW_TAB, 0, R.string.ContextMenuViewImageInNewTab);
					item.setIntent(createIntent(Constants.ACTION_BROWSER_CONTEXT_MENU, TintBrowserActivity.CONTEXT_MENU_OPEN_IN_NEW_TAB, resultType, result.getExtra()));

					item = menu.add(0, TintBrowserActivity.CONTEXT_MENU_COPY, 0, R.string.ContextMenuCopyImageUrl);
					item.setIntent(createIntent(Constants.ACTION_BROWSER_CONTEXT_MENU, TintBrowserActivity.CONTEXT_MENU_COPY, resultType, result.getExtra()));

					item = menu.add(0, TintBrowserActivity.CONTEXT_MENU_DOWNLOAD, 0, R.string.ContextMenuDownloadImage);
					item.setIntent(createIntent(Constants.ACTION_BROWSER_CONTEXT_MENU, TintBrowserActivity.CONTEXT_MENU_DOWNLOAD, resultType, result.getExtra()));

					item = menu.add(0, TintBrowserActivity.CONTEXT_MENU_SHARE, 0, R.string.ContextMenuShareImageUrl);
					item.setIntent(createIntent(Constants.ACTION_BROWSER_CONTEXT_MENU, TintBrowserActivity.CONTEXT_MENU_SHARE, resultType, result.getExtra()));

					createContributedContextMenu(menu, resultType, result.getExtra());

					menu.setHeaderTitle(result.getExtra());

				} else if (resultType == HitTestResult.EMAIL_TYPE) {

					Intent sendMail = new Intent(Intent.ACTION_VIEW, Uri.parse(WebView.SCHEME_MAILTO + result.getExtra()));

					MenuItem item = menu.add(0, TintBrowserActivity.CONTEXT_MENU_SEND_MAIL, 0, R.string.ContextMenuSendEmail);
					item.setIntent(sendMail);

					item = menu.add(0, TintBrowserActivity.CONTEXT_MENU_COPY, 0, R.string.ContextMenuCopyEmailUrl);
					item.setIntent(createIntent(Constants.ACTION_BROWSER_CONTEXT_MENU, TintBrowserActivity.CONTEXT_MENU_COPY, resultType, result.getExtra()));

					item = menu.add(0, TintBrowserActivity.CONTEXT_MENU_SHARE, 0, R.string.ContextMenuShareEmailUrl);
					item.setIntent(createIntent(Constants.ACTION_BROWSER_CONTEXT_MENU, TintBrowserActivity.CONTEXT_MENU_SHARE, resultType, result.getExtra()));

					createContributedContextMenu(menu, resultType, result.getExtra());

					menu.setHeaderTitle(result.getExtra());

				}
			}
		});
	}
}
