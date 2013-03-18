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

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.Notify;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.ConnectionListener;
import android.content.Context;
import android.content.Intent;

public class ConListener implements ConnectionListener {
	private Context context; 
	private String account;
	private JTalkService service;

	public ConListener(Context context, String account) {
		this.context = context;
		this.account = account;
		this.service = JTalkService.getInstance();
	}

	public void connectionClosed() {
        context.sendBroadcast(new Intent(Constants.UPDATE));
        service.leaveAllRooms(account);
	}

	public void connectionClosedOnError(Exception e) {
        if (!service.isAuthenticated()) Notify.offlineNotify("Connection closed");
        service.leaveAllRooms(account);
        service.setState(account, "Connection closed");
        context.sendBroadcast(new Intent(Constants.UPDATE));
	}

	public void reconnectingIn(int seconds) { }
	public void reconnectionSuccessful() { }
	public void reconnectionFailed(Exception e) { }
}
