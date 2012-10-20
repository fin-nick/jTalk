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

import java.util.Iterator;

import net.ustyugov.jtalk.IgnoreList;
import net.ustyugov.jtalk.activity.CommandsActivity;
import net.ustyugov.jtalk.activity.SendFileActivity;
import net.ustyugov.jtalk.activity.vcard.VCardActivity;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;

import com.jtalk2.R;

public class ContactMenuDialogs {

    public static void ContactMenu(final Activity activity, final RosterEntry entry) {
    	final JTalkService service = JTalkService.getInstance();
    	
    	CharSequence[] items;
    	if (service.getMessagesHash().containsKey(entry.getUser())) {
    		items = new CharSequence[9];
    		items[8] = activity.getString(R.string.Close);
    	}
    	else items = new CharSequence[8];
        items[0] = activity.getString(R.string.Info);
        items[1] = activity.getString(R.string.Edit);
        items[2] = activity.getString(R.string.SendStatus);
        items[3] = activity.getString(R.string.SendFile);
        items[4] = activity.getString(R.string.Subscribtion);
        items[5] = activity.getString(R.string.AddInIgnoreList);
        items[6] = activity.getString(R.string.ExecuteCommand);
        items[7] = activity.getString(R.string.Remove);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(entry.getUser());
        builder.setItems(items, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String jid = entry.getUser();
		    	String name = entry.getName();
		    	String group = activity.getResources().getString(R.string.Nogroup);
		    	if (!entry.getGroups().isEmpty()) {
		    		Iterator<RosterGroup> it = entry.getGroups().iterator();
		    		if (it.hasNext()) group = it.next().getName();
		    	}
		    	
		        switch (which) {
		        	case 0:
		        		Intent infoIntent = new Intent(activity, VCardActivity.class);
		        		infoIntent.putExtra("jid", jid);
		        		activity.startActivity(infoIntent);
		        		break;
		        	case 1:
			         	RosterDialogs.editDialog(activity, jid, name, group);
			         	break;
		        	case 2:
		        		RosterDialogs.changeStatusDialog(activity, jid);
		        		break;
		        	case 3:
			        	 Intent intent = new Intent(activity, SendFileActivity.class);
			        	 intent.putExtra("jid", jid);
			        	 activity.startActivity(intent);
			 	        break;
		        	case 4:
			        	 RosterDialogs.subscribtionDialog(activity, jid);
			        	 break;
		        	case 5:
			        	 new IgnoreList.UpdateIgnoreList().execute(jid);
			        	 break;
		        	case 6:
			        	 RosterDialogs.resourceDialog(activity, jid);
			        	 break;
		        	case 7:
					    service.removeContact(jid);
					    Intent i = new Intent(net.ustyugov.jtalk.Constants.UPDATE);
			         	activity.sendBroadcast(i);
			 	        break;
		        	case 8:
		        		service.setChatState(jid, ChatState.gone);
			        	if (service.getMessagesHash().containsKey(jid)) service.getMessagesHash().remove(jid);
						if (service.getCurrentJid().equals(jid)) service.sendBroadcast(new Intent(net.ustyugov.jtalk.Constants.FINISH));
						else service.sendBroadcast(new Intent(net.ustyugov.jtalk.Constants.UPDATE));
			        	break;
		        }
			}
        });
        builder.create().show();
    }
    
    public static void MucContactMenu(final Activity activity, final String group, final String nick) {
    	final JTalkService service = JTalkService.getInstance();
		final MultiUserChat muc = service.getConferencesHash().get(group);
		
		CharSequence[] items = new CharSequence[3];
		items[0] = activity.getString(R.string.Info);
		items[1] = activity.getString(R.string.ExecuteCommand);
		items[2] = activity.getString(R.string.Actions);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.Actions);
        builder.setItems(items, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
		        switch(which) {
		        	case 0:
		        		Intent infoIntent = new Intent(activity, VCardActivity.class);
		        		infoIntent.putExtra("jid", group + "/" + nick);
		        		activity.startActivity(infoIntent);
		        		break;
		        	case 1:
		        		Intent intent = new Intent(activity, CommandsActivity.class);
		    			intent.putExtra("jid", group + "/" + nick);
		    	        activity.startActivity(intent);
		        		break;
		        	case 2:
		        		new MucAdminMenu(activity, muc, nick).show();
		        		break;
		        }
			}
        });
        builder.create().show();
    }
}
