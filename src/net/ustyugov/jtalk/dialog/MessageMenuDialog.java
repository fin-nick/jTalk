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
import net.ustyugov.jtalk.MessageItem;
import net.ustyugov.jtalk.activity.DataFormActivity;
import net.ustyugov.jtalk.service.JTalkService;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;

import com.jtalk2.R;

import java.util.ArrayList;
import java.util.List;

public class MessageMenuDialog implements OnItemLongClickListener, OnClickListener {
    private Activity activity;
    private String account;
    private String jid;
    private MessageItem message;
    private JTalkService service;

    public MessageMenuDialog(Activity activity, String account, String jid) {
        this.activity = activity;
        this.account = account;
        this.jid = jid;
        this.service = JTalkService.getInstance();
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
    	CharSequence[] items;
    	message = (MessageItem) parent.getAdapter().getItem(position);

    	if (message.getName().equals(activity.getResources().getString(R.string.Me))) {
    		items = new CharSequence[6];
    		items[4] = activity.getString(R.string.Edit);
    	}
    	else if (message.containsCaptcha()) { 
    		items = new CharSequence[6];
    		items[4] = "Captcha";
    	}
    	else if (service.getConferencesHash(account).containsKey(jid)) { 
    		items = new CharSequence[6];
    		items[4] = activity.getString(R.string.Reply);
    	}
    	else items = new CharSequence[5];

        if (message.isSelected()) items[0] = activity.getString(R.string.DeselectMessage);
        else items[0] = activity.getString(R.string.SelectMessage);
    	items[1] = activity.getString(R.string.Quote);
        items[2] = activity.getString(R.string.Copy);
        items[3] = activity.getString(R.string.SelectText);
        items[items.length-1] = activity.getString(R.string.DeselectAllMessages);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.Actions);
        builder.setItems(items, this);
        builder.create().show();
        return true;
    }

    public void onClick(DialogInterface dialog, int which) { 
        switch (which) {
            case 0:
                message.select(!message.isSelected());
                activity.sendBroadcast(new Intent(Constants.RECEIVED));
                break;
        	case 1:
            	MessageDialogs.QuoteDialog(activity, jid, message);
        		break;
        	case 2:
        		MessageDialogs.CopyDialog(activity, jid, message);
 	        	break;
        	case 3:
        		MessageDialogs.SelectTextDialog(activity, message);
        	 	break;
        	case 4:
        		if (message.containsCaptcha()) {
        			String id = message.getId(); 
                	
                	JTalkService.getInstance().addDataForm(id, message.getForm());
                	Intent in = new Intent(activity, DataFormActivity.class);
                	in.putExtra("id", id);
                	in.putExtra("cap", true);
    		        in.putExtra("jid", message.getName());
    		        in.putExtra("bob", message.getBob().getData());
    		        in.putExtra("cid", message.getBob().getCid());
    				activity.startActivity(in);
                    break;
        		} else if (message.getName().equals(activity.getString(R.string.Me))) {
        			MessageDialogs.EditMessageDialog(activity, account, message, jid);
                    break;
        		} else if (service.getConferencesHash(account).containsKey(jid)) {
        			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        			String separator = prefs.getString("nickSeparator", ", ");
        			
        			Intent intent = new Intent(Constants.PASTE_TEXT);
                    intent.putExtra("text", message.getName() + separator);
                    activity.sendBroadcast(intent);
                    break;
        		} else {

                }
            case 5:
                List<MessageItem> messages = new ArrayList<MessageItem>();
                if (service.getConferencesHash(account).containsKey(jid)) {
                    if (service.getMucMessagesHash(account).containsKey(jid)) {
                        messages = service.getMucMessagesHash(account).get(jid);
                    }
                } else {
                    if (service.getMessagesHash(account).containsKey(jid)) {
                        messages = service.getMessagesHash(account).get(jid);
                    }
                }
                for (MessageItem item : messages) {
                    if (item.isSelected()) {
                        item.select(false);
                    }
                }
                activity.sendBroadcast(new Intent(Constants.RECEIVED));
                break;
        }
    }
}
