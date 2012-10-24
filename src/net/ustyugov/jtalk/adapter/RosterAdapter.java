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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.ustyugov.jtalk.Avatars;
import net.ustyugov.jtalk.ClientIcons;
import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.RosterItem;
import net.ustyugov.jtalk.SortList;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.packet.Presence;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jtalk2.R;

public class RosterAdapter extends ArrayAdapter<RosterItem> {
	private JTalkService service;
	private Activity activity;
	private IconPicker iconPicker;
	private SharedPreferences prefs;
	private int fontSize, statusSize;
	
	static class GroupHolder {
		protected TextView text;
		protected ImageView state;
	}
	
	static class ItemHolder {
		protected TextView name;
		protected TextView status;
		protected TextView counter;
		protected ImageView statusIcon;
		protected ImageView messageIcon;
		protected ImageView caps;
		protected ImageView avatar;
	}
	
	public RosterAdapter(Activity activity) {
        super(activity, R.id.name);
        this.service = JTalkService.getInstance();
        this.activity = activity;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		this.fontSize = Integer.parseInt(activity.getResources().getString(R.string.DefaultFontSize));
		try {
			this.fontSize = Integer.parseInt(prefs.getString("RosterSize", activity.getResources().getString(R.string.DefaultFontSize)));
		} catch (NumberFormatException e) { }
		this.statusSize = fontSize - 4;
    }
	
	public void update() {
		this.service = JTalkService.getInstance();
		this.iconPicker = service.getIconPicker();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		boolean hideOffline = prefs.getBoolean("hideOffline", false);
		clear();
		
		if (service != null && service.getRoster() != null && service.isAuthenticated()) {
			Roster roster = service.getRoster();
			if (prefs.getBoolean("ShowGroups", true)) {
				Collection<RosterGroup> groups = roster.getGroups();
				for (RosterGroup group: groups) {
					List<String> list = new ArrayList<String>();
					Collection<RosterEntry> entrys = group.getEntries();
					for (RosterEntry re: entrys) {
						String jid = re.getUser();
						Presence.Type presenceType = service.getType(jid);
						if (hideOffline) {
	   		  				if (presenceType != Presence.Type.unavailable) list.add(jid);
	   		  			} else {
	   		  				list.add(jid);
	   		  			}
					}
					if (list.size() > 0) {
						String name = group.getName();
						RosterItem item = new RosterItem();
						item.setGroup(true);
						item.setName(name);
						add(item);
						if (service.getCollapsedGroups().contains(name)) item.setCollapsed(true);
						else {
							list = SortList.sortSimpleContacts(list);
							for (String jid: list) {
								RosterEntry re = roster.getEntry(jid);
								RosterItem i = new RosterItem();
								i.setGroup(false);
								i.setEntry(re);
								add(i);
							}
						}
					}
				}
				
				List<String> list = new ArrayList<String>();
				Collection<RosterEntry> entrys = roster.getUnfiledEntries();
				for (RosterEntry re: entrys) {
					String jid = re.getUser();
					Presence.Type presenceType = service.getType(jid);
					if (hideOffline) {
   		  				if (presenceType != Presence.Type.unavailable) list.add(jid);
   		  			} else {
   		  				list.add(jid);
   		  			}
				}
				
				if (list.size() > 0) {
					String name = activity.getString(R.string.Nogroup);
					RosterItem item = new RosterItem();
					item.setGroup(true);
					item.setName(name);
					add(item);
					if (service.getCollapsedGroups().contains(name)) item.setCollapsed(true);
					else {
						list = SortList.sortSimpleContacts(list);
						for (String jid: list) {
							RosterEntry re = roster.getEntry(jid);
							RosterItem i = new RosterItem();
							i.setGroup(false);
							i.setEntry(re);
							add(i);
						}
					}
				}
			} else {
				List<String> list = new ArrayList<String>();
   				Iterator<RosterEntry> it = roster.getEntries().iterator();
   				while(it.hasNext()) {
   					String jid = it.next().getUser();
   					Presence.Type presenceType = service.getType(jid);
   		  			if (hideOffline) {
   		  				if (presenceType != Presence.Type.unavailable) list.add(jid);
   		  			} else {
   		  				list.add(jid);
   		  			}
   				}
   				list = SortList.sortSimpleContacts(list);
   				
   				for (String jid: list) {
					RosterEntry re = roster.getEntry(jid);
					RosterItem i = new RosterItem();
					i.setGroup(false);
					i.setEntry(re);
					add(i);
				}
			}
		}
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		RosterItem ri = getItem(position);
		if (ri.isGroup()) {
			GroupHolder holder;
			if (convertView == null || convertView.findViewById(R.id.state) == null) {
				LayoutInflater inflater = activity.getLayoutInflater();
				convertView = inflater.inflate(R.layout.group, null, false);
				
				holder = new GroupHolder();
	            holder.text = (TextView) convertView.findViewById(R.id.name);
	            holder.text.setTextSize(fontSize);
	            holder.text.setTextColor(prefs.getBoolean("DarkColors", false) ? 0xFFFFFFFF : 0xFF000000);
	            holder.state = (ImageView) convertView.findViewById(R.id.state);
	            convertView.setTag(holder);
	            convertView.setBackgroundColor(prefs.getBoolean("DarkColors", false) ? 0x77525252 : 0xEEEEEEEE);
			} else {
				holder = (GroupHolder) convertView.getTag();
			}
	        holder.text.setText(ri.getName());
			holder.state.setImageResource(ri.isCollapsed() ? R.drawable.close : R.drawable.open);
			return convertView;
		} else {
			RosterEntry re = ri.getEntry();
			String jid = re.getUser();
			String name = re.getName();
			if (name == null || name.length() <= 0 ) name = jid;
			
			Presence presence = service.getPresence(jid);
			String status = service.getStatus(jid);
			if (service.getComposeList().contains(jid)) status = service.getString(R.string.Composes);
			
			int count = service.getMessagesCount(jid);
			
			ItemHolder holder;
			if (convertView == null || convertView.findViewById(R.id.status) == null) {
				LayoutInflater inflater = activity.getLayoutInflater();
				convertView = inflater.inflate(R.layout.entry, null, false);
				holder = new ItemHolder();
				holder.name = (TextView) convertView.findViewById(R.id.name);
				holder.name.setTextColor(prefs.getBoolean("DarkColors", false) ? 0xFFEEEEEE : 0xFF343434);
				holder.name.setTextSize(fontSize);
				
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
				}
				
				if (prefs.getBoolean("ShowCaps", false)) {
					holder.caps = (ImageView) convertView.findViewById(R.id.caps);
				}
				convertView.setTag(holder);
			} else {
				holder = (ItemHolder) convertView.getTag();
			}
			
	        holder.name.setText(name);
	        if (service.getMessagesHash().containsKey(jid)) {
				if (service.getMessagesHash().get(jid).size() > 0) holder.name.setTypeface(Typeface.DEFAULT_BOLD);
			} else holder.name.setTypeface(Typeface.DEFAULT);
	        
	        if (holder.status != null) {
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
				String node = service.getNode(jid);
				ClientIcons.loadClientIcon(activity, holder.caps, node);
			}
	        
	        if (holder.avatar != null) {
				Avatars.loadAvatar(activity, jid, holder.avatar);
			}
	        
			if (iconPicker != null) holder.statusIcon.setImageBitmap(iconPicker.getIconByPresence(presence));
			return convertView;
		}
	}
}
