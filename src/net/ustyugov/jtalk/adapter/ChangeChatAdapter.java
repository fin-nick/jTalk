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

package net.ustyugov.jtalk.adapter;

import java.util.List;

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import com.jtalk2.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ChangeChatAdapter extends ArrayAdapter<String> {
	private JTalkService service;
	
	public ChangeChatAdapter(Context context, List<String> list) {
		super(context, R.layout.selector);
        this.service = JTalkService.getInstance();
        
        for(int i = 0; i < list.size(); i++) {
			add(list.get(i));
		}
    }
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
        View v = convertView;
        final String jid = getItem(position);
        String name = jid;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
        IconPicker ip = service.getIconPicker();
        
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.selector, null);
        }
        
        TextView label = (TextView) v.findViewById(R.id.item);
        if (service.isHighlight(jid)) label.setTextColor(0xFFAA2323);
        else {
        	if (Build.VERSION.SDK_INT >= 11) {
            	label.setTextColor(prefs.getBoolean("DarkColors", false) ? 0xFFFFFFFF : 0xFF000000);
            } else label.setTextColor(0xFF232323);
        }
        
		ImageView msg  = (ImageView) v.findViewById(R.id.msg);
		msg.setImageBitmap(ip.getMsgBitmap());
		ImageView icon = (ImageView) v.findViewById(R.id.status);
		ImageView close = (ImageView) v.findViewById(R.id.close);
        
        if (service.getConferencesHash().containsKey(jid)) {
        	icon.setImageBitmap(ip.getMucBitmap());
        	msg.setVisibility(service.getMucMessagesList().contains(jid) ? View.VISIBLE : View.GONE);
    		
    		close.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					remove(jid);
					service.leaveRoom(jid);
					if (service.getCurrentJid().equals(jid)) service.sendBroadcast(new Intent(Constants.FINISH));
					else service.sendBroadcast(new Intent(Constants.PRESENCE_CHANGED));
				}
        		
        	});
        } else {
        	close.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					remove(jid);
					if (service.getMessagesHash().containsKey(jid)) service.getMessagesHash().remove(jid);
					if (service.getCurrentJid().equals(jid)) service.sendBroadcast(new Intent(Constants.FINISH));
					else service.sendBroadcast(new Intent(Constants.UPDATE));
				}
        		
        	});
        	
        	if (service.getConferencesHash().containsKey(StringUtils.parseBareAddress(jid))) {
        		name = StringUtils.parseResource(jid);
        	} else {
        		name = jid;
        		RosterEntry re = JTalkService.getInstance().getRoster().getEntry(jid);
                if (re != null && re.getName() != null && re.getName().length() > 0) name = re.getName();
        	}
            
            Presence presence = service.getPresence(jid);
        	icon.setImageBitmap(ip.getIconByPresence(presence));
    		
        	msg.setVisibility(service.getMessagesList().contains(jid) ? View.VISIBLE : View.GONE);
        }
        
        label.setText(name);
        return v;
    }
}
