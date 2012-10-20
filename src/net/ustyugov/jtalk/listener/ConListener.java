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

	public ConListener(Context c) {
		this.context = c;
	}

	public void connectionClosed() {
        context.sendBroadcast(new Intent(net.ustyugov.jtalk.Constants.UPDATE));
        context.sendBroadcast(new Intent(Constants.CONNECTION_STATE));
	}

	public void connectionClosedOnError(Exception e) {
		final JTalkService service = JTalkService.getInstance();
		service.setState("Connection closed");
        Notify.offlineNotify("Connection closed");
        
        context.sendBroadcast(new Intent(Constants.UPDATE));
        context.sendBroadcast(new Intent(Constants.CONNECTION_STATE));
        service.reconnect();
	}

	public void reconnectingIn(int seconds) { }
	public void reconnectionSuccessful() { }
	public void reconnectionFailed(Exception e) { }
}
