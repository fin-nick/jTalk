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

package net.ustyugov.jtalk.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import com.jtalk2.R;
import net.ustyugov.jtalk.service.JTalkService;
import org.jivesoftware.smack.packet.Presence;

import java.util.Date;

public class ScreenStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String currentMode = prefs.getString("currentMode", "available");
        String currentStatus = prefs.getString("currentStatus", "");
        int currentPriority = prefs.getInt("currentPriority", 0);

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            if (prefs.getBoolean("AutoStatus", false) && prefs.getBoolean("AutoStatusOnDisplayOn", false) && prefs.getBoolean("AutoStatusOnDisplayOff", false)) {
                JTalkService service = JTalkService.getInstance();
                if (!service.isAuthenticated()) return;
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
                        priority = Integer.parseInt(prefs.getString("AutoStatusPriorityAway", 0+""));
                    } catch(Exception e) { priority = 5; }
                    service.sendPresence(text, Presence.Mode.away.name(), priority);
                }
            }
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            JTalkService service = JTalkService.getInstance();
            if (!service.isAuthenticated() || !service.getAutoStatus()) return;
            if (prefs.getBoolean("AutoStatus", false) && prefs.getBoolean("AutoStatusOnDisplayOn", false)) {
                Presence presence = service.getOldPresence();
                service.sendPresence(presence.getStatus(), presence.getMode().name(), presence.getPriority());
                service.resetTimer();
            }
        }
    }
}
