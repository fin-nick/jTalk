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

import net.ustyugov.jtalk.activity.RosterActivity;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.InvitationListener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jtalk2.R;

public class InviteListener implements InvitationListener {
	private static int N = 6;
	
	@Override
	public void invitationReceived(Connection conn, String room, String inviter, String reason, String password, Message message) {
		JTalkService service = JTalkService.getInstance();
		
    	Intent i = new Intent(service, RosterActivity.class);
    	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("invite", true);
        i.putExtra("room", room);
        i.putExtra("from", inviter);
        i.putExtra("reason", reason);
        i.putExtra("password", password);
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        
        String str = service.getString(R.string.InviteTo) + " " + room; 
	        
        Notification notification = new Notification(R.drawable.muc, str, System.currentTimeMillis());
        Log.i("Invite", str);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(service, service.getString(R.string.Invite), str, contentIntent);
        
    	NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(N, notification);
	}
}
