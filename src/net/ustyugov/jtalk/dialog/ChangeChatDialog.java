/*
 * Copyright (C) 2014, Igor Ustyugov <igor@ustyugov.net>
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
import net.ustyugov.jtalk.MessageItem;
import net.ustyugov.jtalk.RosterItem;
import net.ustyugov.jtalk.activity.Chat;
import net.ustyugov.jtalk.adapter.ChangeChatAdapter;
import net.ustyugov.jtalk.service.JTalkService;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;

import com.jtalk2.R;
import org.jivesoftware.smackx.ChatState;

import java.util.ArrayList;

public class ChangeChatDialog {
	public static void show(final Activity activity) {
		final JTalkService service = JTalkService.getInstance();
		final ChangeChatAdapter adapter = new ChangeChatAdapter(service);
		if (adapter.getCount() < 1) return;

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.GoTo);
		builder.setAdapter(adapter, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				RosterItem item = adapter.getItem(which);
				String jid;
				if (item.isMuc()) jid = item.getName();
				else jid = item.getEntry().getUser();
				Intent intent = new Intent(activity, Chat.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.putExtra("jid", jid);
				intent.putExtra("account", item.getAccount());
				activity.startActivity(intent);
			}
		});
        builder.setPositiveButton(R.string.CloseAll, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    RosterItem item = adapter.getItem(i);
                    String account = item.getAccount();
                    String jid;
                    if (item.isMuc()) {
                        jid = item.getName();
                        service.leaveRoom(account, jid);
                    }
                    else {
                        jid = item.getEntry().getUser();
                        service.setChatState(account, jid, ChatState.gone);
                        service.removeActiveChat(account, jid);
                        service.setMessageList(account, jid, new ArrayList<MessageItem>());
                    }
                    if (service.getCurrentJid().equals(jid)) service.sendBroadcast(new Intent(Constants.FINISH));
                }
                service.sendBroadcast(new Intent(Constants.UPDATE));
            }
        });
        builder.create().show();
	}
}
