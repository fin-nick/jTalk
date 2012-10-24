/*
 * Copyright (C) 2012, Igor Ustyugov <igor@ustyugov.net>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/
 */

package net.ustyugov.jtalk.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.service.JTalkService;

import com.jtalk2.R;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
//	private CheckBoxPreference compression;
	private CheckBoxPreference autoCollapse;
	private EditTextPreference delayAway;
	private EditTextPreference rosterColumns;
	private EditTextPreference textAway;
	private EditTextPreference delayXa;
	private EditTextPreference textXa;
	private EditTextPreference priorityAway;
	private EditTextPreference priorityXa;
	private ListPreference smilespack;
	private ListPreference iconspack;
	private SharedPreferences  prefs;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		CharSequence[] smiles;
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		addPreferencesFromResource(R.xml.preferences);  // TODO!
		
		File file = new File(Constants.PATH_SMILES);
		file.mkdirs();
		File[] files = file.listFiles();
		if (files != null) {
			smiles = new CharSequence[files.length + 1];
			for (int i = 0; i < files.length; i++) {
				smiles[i+1] = files[i].getName();
			}
		}
		else smiles = new CharSequence[1];
		smiles[0] = "default";
		
		List<CharSequence> icons = new ArrayList<CharSequence>();
		icons.add("default");
		List<CharSequence> names = new ArrayList<CharSequence>();
		names.add("default");
		
		List<PackageInfo> list = getPackageManager().getInstalledPackages(0);
		for (PackageInfo pi : list) {
			String pn = pi.packageName;
			if (pn.startsWith("com.jtalk2.iconpack.")) {
				icons.add(pn);
				
				try {
					Resources res = getPackageManager().getResourcesForApplication(pn);
					int resID = res.getIdentifier(pn + ":string/app_name", null, null);
					names.add(res.getString(resID));
				} catch (Exception e) {
					names.add(pn);
				}
			}
		}
		
		autoCollapse = (CheckBoxPreference) getPreferenceScreen().findPreference("CollapseBigMessages");
		autoCollapse.setEnabled(prefs.getBoolean("EnableCollapseMessages", true) ? true : false);
		
		delayAway = (EditTextPreference) getPreferenceScreen().findPreference("AutoStatusAway");
		rosterColumns = (EditTextPreference) getPreferenceScreen().findPreference("RosterColumns");
		textAway = (EditTextPreference) getPreferenceScreen().findPreference("AutoStatusTextAway");
		delayXa  = (EditTextPreference) getPreferenceScreen().findPreference("AutoStatusXa");
		textXa   = (EditTextPreference) getPreferenceScreen().findPreference("AutoStatusTextXa");
		priorityAway = (EditTextPreference) getPreferenceScreen().findPreference("AutoStatusPriorityAway");
		priorityXa = (EditTextPreference) getPreferenceScreen().findPreference("AutoStatusPriorityXa");
//		compression  = (CheckBoxPreference) getPreferenceScreen().findPreference("UseCompression"); 
		smilespack = (ListPreference) getPreferenceScreen().findPreference("SmilesPack");
		smilespack.setEntries(smiles);
		smilespack.setEntryValues(smiles);
		if (smiles.length == 1) smilespack.setValue("default");
		
		iconspack = (ListPreference) getPreferenceScreen().findPreference("IconPack");
		iconspack.setEntries(names.toArray(new CharSequence[1]));
		iconspack.setEntryValues(icons.toArray(new CharSequence[1]));
		if (icons.size() == 1) iconspack.setValue("default");
		
//		compression.setEnabled(prefs.getBoolean("EnableTls", true) ? true : false);
		rosterColumns.setEnabled(prefs.getBoolean("ShowGroups", true) ? false : true);
		delayAway.setEnabled(prefs.getBoolean("AutoStatus", false) ? true : false);
		textAway.setEnabled(prefs.getBoolean("AutoStatus", false) ? true : false);
		delayXa.setEnabled(prefs.getBoolean("AutoStatus", false) ? true : false);
		textXa.setEnabled(prefs.getBoolean("AutoStatus", false) ? true : false);
		priorityAway.setEnabled(prefs.getBoolean("AutoStatus", false) ? true : false);
		priorityXa.setEnabled(prefs.getBoolean("AutoStatus", false) ? true : false);
		
		if (smiles.length > 0) {
			smilespack.setEnabled(prefs.getBoolean("ShowSmiles", true) ? true : false);
		} else smilespack.setEnabled(false);
		
		getPreferenceScreen().findPreference("version").setSummary(R.string.version);
		getPreferenceScreen().findPreference("build").setSummary(R.string.build);
	}
		
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
		
	@Override
	public void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
		
	@Override
	public void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		smilespack.setEnabled(prefs.getBoolean("ShowSmiles", true) ? true : false);
		autoCollapse.setEnabled(prefs.getBoolean("EnableCollapseMessages", true) ? true : false);
		rosterColumns.setEnabled(prefs.getBoolean("ShowGroups", true) ? false : true);
		delayAway.setEnabled(prefs.getBoolean("AutoStatus", false) ? true : false);
		textAway.setEnabled(prefs.getBoolean("AutoStatus", false) ? true : false);
		delayXa.setEnabled(prefs.getBoolean("AutoStatus", false) ? true : false);
		textXa.setEnabled(prefs.getBoolean("AutoStatus", false) ? true : false);
		priorityAway.setEnabled(prefs.getBoolean("AutoStatus", false) ? true : false);
		priorityXa.setEnabled(prefs.getBoolean("AutoStatus", false) ? true : false);
		
		String iconPack = prefs.getString("IconPack", "default");
		IconPicker ip = JTalkService.getInstance().getIconPicker();
		if (ip != null && !iconPack.equals(ip.getPackName())) ip.loadIconPack();
		setResult(RESULT_OK);
	}
}
