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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import net.ustyugov.jtalk.activity.Chat;
import net.ustyugov.jtalk.adapter.ChangeChatAdapter;
import net.ustyugov.jtalk.service.JTalkService;

import com.jtalk2.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;

public class ChangeChatDialog {
	public static void show(final Activity activity) {
		final JTalkService service = JTalkService.getInstance();
		List<String> list = new ArrayList<String>();
		
		Enumeration<String> chatEnum = service.getMessagesHash().keys();
		while (chatEnum.hasMoreElements()) {
			list.add(chatEnum.nextElement());
		}
		
		Enumeration<String> e = service.getConferencesHash().keys();
		while(e.hasMoreElements()) {
			list.add(e.nextElement());
		}
		
		if (list.size() > 0) {
			final ChangeChatAdapter adapter = new ChangeChatAdapter(service, list);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setTitle(R.string.GoTo);
			builder.setAdapter(adapter, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String jid = adapter.getItem(which);
					Intent intent = new Intent(activity, Chat.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.putExtra("jid", jid);
					activity.startActivity(intent);
				}
			});
	        builder.create().show();
		}
	}
}
