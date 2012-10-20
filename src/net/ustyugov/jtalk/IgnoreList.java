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

package net.ustyugov.jtalk;

import java.util.ArrayList;
import java.util.List;

import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.PrivacyList;
import org.jivesoftware.smack.PrivacyListManager;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.PrivacyItem;


import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

public class IgnoreList {
	public final static String LISTNAME = "Ignore-List";
	
	public static class CreateIgnoreList extends AsyncTask<Void, Void, Void> {
		private JTalkService service;
		private PrivacyListManager plm;
		
		@Override
		protected Void doInBackground(Void... arg0) {
			try {
				boolean exist = false;
				service = JTalkService.getInstance();
				plm = PrivacyListManager.getInstanceFor(service.getConnection());
				PrivacyList[] list = plm.getPrivacyLists();
				for (PrivacyList l : list) {
					if (l.toString().equals(LISTNAME)) exist = true;
				}
				if (!exist) {
					PrivacyItem item = new PrivacyItem("group", false, 0);
					item.setValue("Ignore-List");
					item.setFilterIQ(true);
					item.setFilterMessage(true);
					item.setFilterPresence_in(true);
					item.setFilterPresence_out(true);
					
					List<PrivacyItem> items = new ArrayList<PrivacyItem>();
					items.add(item);
					plm.createPrivacyList(LISTNAME, items);
				}
			} catch (Exception e) { }
			return null;
		}
		
		@Override
		protected void onPostExecute(Void param) {
			try {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
				if (prefs.getBoolean("ActivateIgnoreList", false)) plm.setActiveListName(LISTNAME);
				if (prefs.getBoolean("SetIgnoreListDefault", false)) plm.setDefaultListName(LISTNAME);
			} catch(XMPPException e) { }
		}
	}
	public static class UpdateIgnoreList extends AsyncTask<String, Void, Void> {
		public final static String LISTNAME = "Ignore-List";
		JTalkService service;
		PrivacyListManager plm;
		
		@Override
		protected Void doInBackground(String... jids) {
			String jid = jids[0];
			try {
				service = JTalkService.getInstance();
				plm = PrivacyListManager.getInstanceFor(service.getConnection());
				PrivacyList list = plm.getPrivacyList(LISTNAME);
				List<PrivacyItem> items = list.getItems();
				
				PrivacyItem item = new PrivacyItem("jid", false, items.size());
				item.setValue(jid);
				item.setFilterIQ(true);
				item.setFilterMessage(true);
				item.setFilterPresence_in(true);
				item.setFilterPresence_out(true);
				
				items.add(item);
				plm.updatePrivacyList(LISTNAME, items);
			} catch (XMPPException e) { Log.e("UpdateIgnoreList", e.getLocalizedMessage()); }
			return null;
		}
	}
}
