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

package net.ustyugov.jtalk.dialog;

import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bookmark.BookmarkManager;
import org.jivesoftware.smackx.bookmark.BookmarkedConference;

import com.jtalk2.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;

public class InviteDialog {
	private Activity activity;
	private String room;
	private String from;
	private String reason;
	private String password;
	
	public InviteDialog(Activity activity, String room, String from, String reason, String password) {
		this.activity = activity;
		this.reason = reason;
		this.room = room;
		this.from = from;
		this.password = password;
	}
	
	public void show() {
		String str = activity.getString(R.string.Room) +": " + room + "\n" + activity.getString(R.string.From) + ": " +from + "\n" + activity.getString(R.string.Reason) + ": " + reason;
		Log.i("Invite", str);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle("Invite");
        builder.setMessage(str);
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				new Thread() {
					@Override
					public void run() {
						JTalkService service = JTalkService.getInstance();
						String jid = service.getConnection().getUser();
						if (jid != null) {
							String nick = StringUtils.parseName(jid);
							try {
								BookmarkManager bm = BookmarkManager.getBookmarkManager(service.getConnection());
								for(BookmarkedConference bc : bm.getBookmarkedConferences()) {
									if (bc.getJid().equals(room)) {
										String n = bc.getNickname();
										if (n != null && n.length() > 0) nick = n;
									}
								}
							} catch (XMPPException e) { }
							
							service.joinRoom(room, nick, password);
						}
					}
				}.start();
			}
        });
        builder.setNegativeButton("No", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
        });
        builder.create().show();
	}
}
