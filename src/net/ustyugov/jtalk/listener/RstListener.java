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

package net.ustyugov.jtalk.listener;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import android.preference.PreferenceManager;
import net.ustyugov.jtalk.Avatars;
import net.ustyugov.jtalk.MessageItem;
import net.ustyugov.jtalk.MessageLog;
import net.ustyugov.jtalk.service.JTalkService;

import net.ustyugov.jtalk.Constants;

import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.content.Intent;
import android.text.format.DateFormat;

import com.jtalk2.R;

public class RstListener implements RosterListener {
	private JTalkService service;
	private String account;

	public RstListener(String account) {
		this.service = JTalkService.getInstance();
		this.account = account;
	}

    @Override
    public void entriesAdded(Collection<String> addresses) {
    	Intent intent = new Intent(Constants.UPDATE);
       	service.sendBroadcast(intent);
    }

    @Override
    public void entriesDeleted(Collection<String> addresses) {
    	Intent intent = new Intent(Constants.UPDATE);
       	service.sendBroadcast(intent);
    }
    
    @Override
    public void entriesUpdated(Collection<String> addresses) {
       	Intent intent = new Intent(Constants.UPDATE);
       	service.sendBroadcast(intent);
    }

    @Override
    public void presenceChanged(Presence presence) {
    	String[] statusArray = service.getResources().getStringArray(R.array.statusArray);
    	String jid  = StringUtils.parseBareAddress(presence.getFrom());

    	Presence.Mode mode = presence.getMode();
    	if (mode == null) mode = Presence.Mode.available;

    	String status = presence.getStatus();
    	if (status != null && status.length() > 0) status = "(" + status + ")";
    	else status = "";

      	Date date = new java.util.Date();
        date.setTime(Long.parseLong(System.currentTimeMillis()+""));
        String time = DateFormat.getTimeFormat(service).format(date);

      	MessageItem item = new MessageItem(account, jid);
		if (presence.isAvailable()) {
            item.setBody(statusArray[getPosition(mode)] + " " + status);

            if (PreferenceManager.getDefaultSharedPreferences(service).getBoolean("LoadAllAvatars", false)) {
                Avatars.loadAvatar(account, jid);
            }
        }
		else {
			item.setBody(statusArray[5] + " " + status);
		}
        item.setName(jid);
        item.setTime(time);
        item.setType(MessageItem.Type.status);
        item.setId(System.currentTimeMillis()+"");

        MessageLog.writeMessage(account, jid, item);
        service.sendBroadcast(new Intent(Constants.PRESENCE_CHANGED).putExtra("jid", jid));
        service.sendBroadcast(new Intent(Constants.UPDATE));
    }
    
    private int getPosition(Presence.Mode m) {
    	if (m == Presence.Mode.available) return 0;
    	else if (m == Presence.Mode.chat) return 4;
    	else if (m == Presence.Mode.away) return 1;
    	else if (m == Presence.Mode.xa)   return 2;
    	else if (m == Presence.Mode.dnd)  return 3;
    	else return 5;
    }
}
