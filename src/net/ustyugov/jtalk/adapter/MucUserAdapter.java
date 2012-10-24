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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.SortList;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.MUCUser;

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

import com.jtalk2.R;

public class MucUserAdapter extends ArrayAdapter<String> {
	private String group;
	
	public MucUserAdapter(Context context, String group) {
		super(context, R.id.name);
		this.group = group;
        update();
	}
	
	public void setGroup(String group) {
		this.group = group;
	}
	
	public void update() {
		JTalkService service = JTalkService.getInstance();
		clear();
		add("");
		if (group != null && service.getConferencesHash().containsKey(group)) {
			List<String> users = new ArrayList<String>();
			Iterator<Presence> it = service.getRoster().getPresences(group);
			while (it.hasNext()) {
				Presence p = it.next();
				users.add(StringUtils.parseResource(p.getFrom()));
			}
			
			users = SortList.sortParticipantsInChat(group, users);
			for (String user: users) {
				add(user);
			}
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		JTalkService service = JTalkService.getInstance();
		IconPicker ip = service.getIconPicker();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
		boolean minimal = prefs.getBoolean("CompactMode", true);
		int fontSize = Integer.parseInt(service.getResources().getString(R.string.DefaultFontSize));
		try {
			fontSize = Integer.parseInt(prefs.getString("RosterSize", service.getResources().getString(R.string.DefaultFontSize)));
		} catch (NumberFormatException e) { }
		
		View v = convertView;
		if (v == null) {
            LayoutInflater vi = (LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.entry, null);
        }
		
		if (position == 0) {
			ImageView roleImage = (ImageView) v.findViewById(R.id.role);
			roleImage.setVisibility(View.GONE);
			
			ImageView icon = (ImageView)v.findViewById(R.id.status_icon);
	      	if (minimal && service.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
	      		icon.setVisibility(View.GONE);
	      	} else {
	      		icon.setVisibility(View.VISIBLE);
	      		icon.setImageBitmap(ip.getMucBitmap());
	      	}
			
			TextView label = (TextView) v.findViewById(R.id.name);
			label.setTypeface(Typeface.DEFAULT_BOLD);
	       	label.setTextSize(fontSize);
	        label.setText("Users: " + (getCount()-1));
	        if (prefs.getBoolean("DarkColors", false)) {
    			label.setTextColor(0xFFFFFFFF);
    			v.setBackgroundColor(0x77525252);
    		}
    		else {
    			label.setTextColor(0xFF000000);
    			v.setBackgroundColor(0xEEEEEEEE);
    		}
		} else {
			v.setBackgroundColor(0x00000000);
			
			boolean color = prefs.getBoolean("ColoredBar", true);
			
	        String nick = getItem(position);
			
	        Presence p = service.getRoster().getPresenceResource(group + "/" + nick);
	        Presence.Type type = p.getType();
	        Presence.Mode mode = p.getMode();
	        
	      	ImageView icon = (ImageView)v.findViewById(R.id.status_icon);
	      	if (minimal && service.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
	      		icon.setVisibility(View.GONE);
	      	} else {
	      		icon.setVisibility(View.VISIBLE);
	      		icon.setImageBitmap(ip.getIconByPresence(p));
	      	}
	       	
	      	ImageView roleImage = (ImageView) v.findViewById(R.id.role);
	      	if (minimal && service.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
	      		roleImage.setVisibility(View.GONE);
	      	} else {
	      		roleImage.setVisibility(View.VISIBLE);
	      		String role = "visitor";
	    		MUCUser mucUser = (MUCUser) p.getExtension("x", "http://jabber.org/protocol/muc#user");
	    		if (mucUser != null) role = mucUser.getItem().getRole();
	    		roleImage.setImageBitmap(ip.getRoleIcon(role));
	      	}
	       	
	       	ImageView msg  = (ImageView) v.findViewById(R.id.msg);
	       	msg.setImageBitmap(ip.getMsgBitmap());
	        msg.setVisibility(service.getMessagesCount(group + "/" + nick) > 0 ? View.VISIBLE : View.GONE);
			
	       	TextView label = (TextView) v.findViewById(R.id.name);
	       	label.setTextSize(fontSize);
	        label.setText(nick);
	        if (service.getComposeList().contains(group)) label.setTextColor(0xFFAA2323);
			else label.setTextColor(prefs.getBoolean("DarkColors", false) ? 0xFFEEEEEE : 0xFF343434);
	        if (color) {
	        	if (type == Presence.Type.available) {
	       			if(mode == Presence.Mode.away) {
	       				label.setTextColor(0xFF22bcef);
	       			} else if (mode == Presence.Mode.xa) {
	       				label.setTextColor(0xFF3c788c);
	       			} else if (mode == Presence.Mode.dnd) {
	       				label.setTextColor(0xFFee0000);
	       			} else if (mode == Presence.Mode.chat) {
	       				label.setTextColor(0xFF008e00);
	       			}
	       		}
	        }
	       	
	        if (service.getMessagesHash().containsKey(group + "/" + nick)) label.setTypeface(Typeface.DEFAULT_BOLD);
			else label.setTypeface(Typeface.DEFAULT);
		}
        return v;
    }
}
