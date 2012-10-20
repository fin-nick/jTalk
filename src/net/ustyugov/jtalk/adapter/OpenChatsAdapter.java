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

import java.util.Enumeration;

import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import com.jtalk2.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class OpenChatsAdapter extends ArrayAdapter<String> {
	private JTalkService service;
	private boolean isFragment;
	
	public OpenChatsAdapter(Context context, boolean isFragment) {
		super(context, R.id.name);
        this.service = JTalkService.getInstance();
        this.isFragment = isFragment;
    }
	
	public void update() {
		clear();
		add("");
		Enumeration<String> chatEnum = service.getMessagesHash().keys();
		while (chatEnum.hasMoreElements()) {
			add(chatEnum.nextElement());
		}
		
		Enumeration<String> groupEnum = service.getConferencesHash().keys();
		while(groupEnum.hasMoreElements()) {
			add(groupEnum.nextElement());
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {	
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
		IconPicker ip = service.getIconPicker();
		boolean minimal = prefs.getBoolean("CompactMode", true);
		
        View v = convertView;
        String jid = getItem(position);
        String name = jid;
        int fontSize = Integer.parseInt(service.getResources().getString(R.string.DefaultFontSize));
		try {
			fontSize = Integer.parseInt(prefs.getString("RosterSize", service.getResources().getString(R.string.DefaultFontSize)));
		} catch (NumberFormatException e) { }
		
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.entry, null);
        }
        
        if (position == 0) {
        	ImageView roleImage = (ImageView) v.findViewById(R.id.role);
			roleImage.setVisibility(View.GONE);
			TextView counter = (TextView) v.findViewById(R.id.msg_counter);
			counter.setVisibility(View.GONE);
			ImageView msg  = (ImageView) v.findViewById(R.id.msg);
			msg.setVisibility(View.GONE);
			
			ImageView icon = (ImageView)v.findViewById(R.id.status_icon);
	      	if (minimal && service.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
	      		icon.setVisibility(View.GONE);
	      	} else {
	      		icon.setVisibility(View.VISIBLE);
	      		icon.setImageBitmap(ip.getMsgBitmap());
	      	}
			
			TextView label = (TextView) v.findViewById(R.id.name);
			label.setTypeface(Typeface.DEFAULT_BOLD);
	       	label.setTextSize(fontSize);
	        label.setText("Chats: " + (getCount()-1));
	        if (prefs.getBoolean("DarkColors", false)) {
    			label.setTextColor(0xFFFFFFFF);
    			v.setBackgroundColor(0x77525252);
    		}
    		else {
    			label.setTextColor(0xFF000000);
    			v.setBackgroundColor(0xEEEEEEEE);
    		}
        } else {
        	if (service.getConferencesHash().containsKey(jid)) {
            	name = StringUtils.parseName(jid);
            } else if (service.getConferencesHash().containsKey(StringUtils.parseBareAddress(jid))) {
            	name = StringUtils.parseResource(jid);
            } else {
            	RosterEntry re = JTalkService.getInstance().getRoster().getEntry(jid);
                if (re != null) name = re.getName();
                if (name == null || name.equals("")) name = jid;
            }
            
            TextView label = (TextView) v.findViewById(R.id.name);
           	label.setTextSize(fontSize);
            label.setText(name);
           	if (service.getComposeList().contains(jid)) label.setTextColor(0xFFAA2323);
           	else if (service.isHighlight(jid)) label.setTextColor(0xFFAA2323);
    		else label.setTextColor(prefs.getBoolean("DarkColors", false) ? 0xFFEEEEEE : 0xFF343434);
           	
            ImageView icon = (ImageView)v.findViewById(R.id.status_icon);
            if (minimal && service.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && !isFragment) {
            	icon.setVisibility(View.GONE);
            } else {
            	icon.setVisibility(View.VISIBLE);
            	if (service.getConferencesHash().containsKey(jid)) {
                	icon.setImageBitmap(ip.getMucBitmap());
                } else {
                	Presence presence = service.getPresence(jid);
                	icon.setImageBitmap(ip.getIconByPresence(presence));
                }
            }
        	
            TextView counter = (TextView) v.findViewById(R.id.msg_counter);
    		counter.setTextSize(fontSize);
            int count = service.getMessagesCount(jid);
    		if (count > 1) {
    			counter.setVisibility(View.VISIBLE);
    			counter.setText(count+"");
    		} else counter.setVisibility(View.GONE);
    		
            ImageView msg  = (ImageView) v.findViewById(R.id.msg);
            msg.setImageBitmap(ip.getMsgBitmap());
            if (service.getConferencesHash().containsKey(jid)) {
            	msg.setVisibility(service.getMucMessagesList().contains(jid) ? View.VISIBLE : View.GONE);
            } else {
            	msg.setVisibility(service.getMessagesList().contains(jid) ? View.VISIBLE : View.GONE);
            }
            
            if (jid.equals(service.getCurrentJid())) {
            	label.setTypeface(Typeface.DEFAULT_BOLD);
            	if (prefs.getBoolean("DarkColors", false)) {
        			label.setTextColor(0xFFFFFFFF);
        			v.setBackgroundColor(0x77525252);
        		}
        		else {
        			label.setTextColor(0xFF000000);
        			v.setBackgroundColor(0xEEEEEEEE);
        		}
            } else {
            	label.setTypeface(Typeface.DEFAULT);
            	v.setBackgroundColor(0x00000000);
            }
        }
        return v;
	}
}
