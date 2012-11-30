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

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.adapter.UsersAdapter;

import com.jtalk2.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class UsersDialog implements DialogInterface.OnClickListener {
	private Context context;
	private UsersAdapter adapter;
    private String account;
	private String group;

	public UsersDialog(Context context, String account, String group) {
		this.context = context;
        this.account = account;
		this.group = group;
	}
	
	public void show() {
		adapter = new UsersAdapter(context, account, group);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.PasteNick);
		builder.setAdapter(adapter, this);
        builder.create().show();
	}
	
	public void onClick(DialogInterface dialog, int which) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String separator = prefs.getString("nickSeparator", ", ");
		String nick = adapter.getItem(which).getName();
		
		Intent intent = new Intent(Constants.PASTE_TEXT);
		intent.putExtra("text", nick + separator);
		context.sendBroadcast(intent);
	}
}
