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

import net.ustyugov.jtalk.Avatars;
import net.ustyugov.jtalk.ClientIcons;
import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.adapter.RosterAdapter.ItemHolder;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.jtalk2.R;

public class ChatsSpinnerAdapter extends ArrayAdapter<String> implements SpinnerAdapter {
	private JTalkService service;
	private SharedPreferences prefs;
	private Activity activity;
	
	public ChatsSpinnerAdapter(Activity activity) {
		super(activity, R.id.name);
        this.service = JTalkService.getInstance();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(service);
        this.activity = activity;
    }
	
	public void update() {
		clear();
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
        View v = convertView;
        String jid = service.getCurrentJid();
		
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.sherlock_spinner_item, null);
        }
        
        String name = jid;
        if (service.getConferencesHash().containsKey(jid)) {
        	name = StringUtils.parseName(jid);
        } else if (service.getConferencesHash().containsKey(StringUtils.parseBareAddress(jid))) {
        	name = StringUtils.parseResource(jid);
        } else {
        	RosterEntry re = JTalkService.getInstance().getRoster().getEntry(jid);
            if (re != null) name = re.getName();
            if (name == null || name.equals("")) name = jid;
        }
        
        TextView label = (TextView) v.findViewById(android.R.id.text1);
        label.setText(name);
        if (service.getComposeList().contains(jid)) {
    		label.setTextColor(0xFFFF0000);
    	} else label.setTextColor(prefs.getBoolean("DarkColors", false) ? 0xFFFFFFFF : 0xFF000000);
        
        return v;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		boolean isMuc = false;
		IconPicker iconPicker = service.getIconPicker();
		int fontSize = Integer.parseInt(service.getResources().getString(R.string.DefaultFontSize));
		try {
			fontSize = Integer.parseInt(prefs.getString("RosterSize", service.getResources().getString(R.string.DefaultFontSize)));
		} catch (NumberFormatException e) { }
		int statusSize = fontSize - 4;
		
		String jid = getItem(position);
		String status;
		String name;
		if (service.getConferencesHash().containsKey(jid)) {
			isMuc = true;
			name = StringUtils.parseName(jid);
			MultiUserChat muc = service.getConferencesHash().get(jid);
			status = muc.getSubject();
		} else {
			if (service.getComposeList().contains(jid)) status = service.getString(R.string.Composes);
			else status = service.getStatus(jid);
			
			RosterEntry re = service.getRoster().getEntry(jid);
			if (re != null) {
				name = re.getName();
				if (name == null || name.length() <= 0 ) name = jid;
			} else {
				name = StringUtils.parseResource(jid);
				status = service.getStatus(jid);
			}
		}
		
		Presence presence = service.getPresence(jid);
		int count = service.getMessagesCount(jid);
		
		ItemHolder holder;
		if (convertView == null || convertView.findViewById(R.id.status) == null) {
			LayoutInflater inflater = activity.getLayoutInflater();
			convertView = inflater.inflate(R.layout.entry, null, false);
			holder = new ItemHolder();
			
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.name.setTextSize(fontSize);
			if (service.isHighlight(jid)) holder.name.setTextColor(0xFFFF0000);
			else holder.name.setTextColor(prefs.getBoolean("DarkColors", false) ? 0xFFEEEEEE : 0xFF343434);
			
			if (prefs.getBoolean("ShowStatuses", false)) {
				holder.status = (TextView) convertView.findViewById(R.id.status);
				holder.status.setTextSize(statusSize);
				holder.status.setTextColor(prefs.getBoolean("DarkColors", false) ? 0xFFBBBBBB : 0xFF555555);
			}
			
			holder.counter = (TextView) convertView.findViewById(R.id.msg_counter);
			holder.counter.setTextSize(fontSize);
			holder.messageIcon = (ImageView) convertView.findViewById(R.id.msg);
			holder.messageIcon.setImageBitmap(iconPicker.getMsgBitmap());
			holder.statusIcon = (ImageView) convertView.findViewById(R.id.status_icon);
			holder.statusIcon.setVisibility(View.VISIBLE);
			if (prefs.getBoolean("LoadAvatar", false)) {
				holder.avatar = (ImageView) convertView.findViewById(R.id.contactlist_pic);
				holder.avatar.setVisibility(View.INVISIBLE);
			}
			
			if (prefs.getBoolean("ShowCaps", false) && !isMuc) {
				holder.caps = (ImageView) convertView.findViewById(R.id.caps);
			}
			convertView.setTag(holder);
		} else {
			holder = (ItemHolder) convertView.getTag();
		}
		
        holder.name.setText(name);
        holder.name.setTypeface(Typeface.DEFAULT_BOLD);
        
        if (holder.status != null && status != null) {
        	holder.status.setVisibility(status.length() > 0 ? View.VISIBLE : View.GONE);
        	holder.status.setText(status);
        }
		
        if (count > 0) {
        	holder.messageIcon.setVisibility(View.VISIBLE);
			holder.counter.setVisibility(View.VISIBLE);
			holder.counter.setText(count+"");
		} else {
			holder.messageIcon.setVisibility(View.GONE);
			holder.counter.setVisibility(View.GONE);
		}
        
        if (holder.caps != null) {
        	if (!isMuc) {
        		String node = service.getNode(jid);
    			ClientIcons.loadClientIcon(activity, holder.caps, node);
        	} else holder.caps.setVisibility(View.GONE);
		}
        
        if (holder.avatar != null) {
			if (!isMuc) Avatars.loadAvatar(activity, jid, holder.avatar);
			else holder.avatar.setVisibility(View.GONE);
		}
        
		if (!isMuc) holder.statusIcon.setImageBitmap(iconPicker.getIconByPresence(presence));
		else holder.statusIcon.setImageBitmap(iconPicker.getMucBitmap());
		return convertView;
	}
}
