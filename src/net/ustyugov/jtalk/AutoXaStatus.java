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

public class AutoXaStatus extends TimerTask {
	@Override
	public void run() {
		JTalkService service = JTalkService.getInstance();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
		
		if (prefs.getBoolean("AutoStatus", false) && service.getAutoStatus()) {
			service.setAutoStatus(true);
			String text = prefs.getString("AutoStatusTextXa", service.getResources().getString(R.string.AutoStatusTextXa));
			if (text.contains("%time%")) {
				Date date = new java.util.Date();
	            date.setTime(Long.parseLong(System.currentTimeMillis()+""));
	            String time = DateFormat.getTimeFormat(service).format(date);
				text = text.replace("%time%", time);
			}
			int priority = 3;
			try {
				priority = Integer.parseInt(prefs.getString("AutoStatusPriorityXa", 3+""));
			} catch(Exception e) { priority = 3; }
			service.sendPresence(text, Presence.Mode.xa.name(), priority);
		}
	}
}
