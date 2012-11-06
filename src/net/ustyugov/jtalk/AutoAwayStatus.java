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

import java.util.Date;
import java.util.TimerTask;

import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.packet.Presence;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

import com.jtalk2.R;

public class AutoAwayStatus extends TimerTask {
	@Override
	public void run() {
		JTalkService service = JTalkService.getInstance();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
		String currentMode = prefs.getString("currentMode", "available");
		String currentStatus = prefs.getString("currentStatus", "");
		int currentPriority = prefs.getInt("currentPriority", 0);
		
		if (prefs.getBoolean("AutoStatus", false) && !service.getAutoStatus()) { // TODO && isAuthenticated
			if (currentMode.equals("available") || currentMode.equals("chat")) {
				Presence presence = new Presence(Presence.Type.available);
				presence.setStatus(currentStatus);
				presence.setMode(Presence.Mode.valueOf(currentMode));
				presence.setPriority(currentPriority);
				service.setOldPresence(presence);
				service.setAutoStatus(true);
				
				String text = prefs.getString("AutoStatusTextAway", service.getResources().getString(R.string.AutoStatusTextAway));
				if (text.contains("%time%")) {
					Date date = new java.util.Date();
		            date.setTime(Long.parseLong(System.currentTimeMillis()+""));
		            String time = DateFormat.getTimeFormat(service).format(date);
					text = text.replace("%time%", time);
				}
				int priority = 5;
				try {
					priority = Integer.parseInt(prefs.getString("AutoStatusPriorityAway", 5+""));
				} catch(Exception e) { priority = 5; }
				service.sendPresence(text, Presence.Mode.away.name(), priority);
			}
		}
	}
}
