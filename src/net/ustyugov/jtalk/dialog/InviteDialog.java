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

import com.jtalk2.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

public class InviteDialog {
	private Activity activity;
	private String account;
	private String room;
	private String from;
	private String reason;
	private String password;
	
	public InviteDialog(Activity activity, String account, String room, String from, String reason, String password) {
		this.activity = activity;
		this.account = account;
		this.reason = reason;
		this.room = room;
		this.from = from;
		this.password = password;
	}
	
	public void show() {
		String str = activity.getString(R.string.Room) +": " + room + "\n" + activity.getString(R.string.From) + ": " +from + "\n" + activity.getString(R.string.Reason) + ": " + reason;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle("Invite");
        builder.setMessage(str);
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				MucDialogs.joinDialog(activity, account, room, password);
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
