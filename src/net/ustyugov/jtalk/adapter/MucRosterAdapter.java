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
import java.util.List;

import net.ustyugov.jtalk.Avatars;
import net.ustyugov.jtalk.ClientIcons;
import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.SortList;
import net.ustyugov.jtalk.activity.Chat;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.MUCUser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jtalk2.R;

public class MucRosterAdapter extends ArrayAdapter<String> {
	private JTalkService service;
	private Activity activity;
	private SharedPreferences prefs;
	private IconPicker ip;
	private int fontSize, statusSize;
	
	static class GroupHolder {
		protected TextView text;
		protected TextView counter;
		protected ImageView state;
		protected ImageView messageIcon;
	}
	
	static class ItemHolder {
		protected TextView name;
		protected TextView status;
		protected TextView counter;
		protected ImageView role;
		protected ImageView statusIcon;
		protected ImageView messageIcon;
		protected ImageView caps;
		protected ImageView avatar;
	}
	
	public MucRosterAdapter(Activity activity) {
        super(activity, R.id.name);
        this.activity = activity;
        this.service = JTalkService.getInstance();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        this.ip = service.getIconPicker();
		this.fontSize = Integer.parseInt(activity.getResources().getString(R.string.DefaultFontSize));
		try {
			this.fontSize = Integer.parseInt(prefs.getString("RosterSize", activity.getResources().getString(R.string.DefaultFontSize)));
		} catch (NumberFormatException e) { }
		this.statusSize = fontSize - 4;
    }
	
	public void update() {
		clear();
		
		Enumeration<String> keys = service.getConferencesHash().keys();
		while (keys.hasMoreElements()) {
			String group = keys.nextElement();
			add(group);
			
			if (!service.getCollapsedGroups().contains(group)) {
				List<String>users = SortList.sortParticipants(group);
				for (String user: users) {
					add(user);
				}
			}
		}
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final String item = getItem(position);
		if (item.contains("/")) {
			String group = StringUtils.parseBareAddress(item);
			String nick = StringUtils.parseResource(item);
			String status = service.getStatus(group + "/" + nick);
			Presence p = service.getRoster().getPresenceResource(group + "/" + nick);
			String role = "visitor";
			MUCUser mucUser = (MUCUser) p.getExtension("x", "http://jabber.org/protocol/muc#user");
			if (mucUser != null) role = mucUser.getItem().getRole();
			
			if (convertView == null || convertView.findViewById(R.id.status) == null) {
				LayoutInflater inflater = (LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.entry, null, false);
				
				ItemHolder holder = new ItemHolder();
				holder.name = (TextView) convertView.findViewById(R.id.name);
				holder.name.setTextSize(fontSize);
				holder.name.setTextColor(prefs.getBoolean("DarkColors", false) ? 0xFFEEEEEE : 0xFF343434);
				holder.status = (TextView) convertView.findViewById(R.id.status);
				holder.status.setTextSize(statusSize);
				holder.status.setTextColor(prefs.getBoolean("DarkColors", false) ? 0xFFBBBBBB : 0xFF555555);
				holder.role = (ImageView) convertView.findViewById(R.id.role);
				holder.role.setVisibility(View.VISIBLE);
				holder.statusIcon = (ImageView) convertView.findViewById(R.id.status_icon);
				holder.statusIcon.setVisibility(View.VISIBLE);
				
				
				if (prefs.getBoolean("LoadAvatar", false)) {
					holder.avatar = (ImageView) convertView.findViewById(R.id.contactlist_pic);
				}
				
				if (prefs.getBoolean("ShowCaps", false)) {
					holder.caps = (ImageView) convertView.findViewById(R.id.caps);
				}
				
				holder.messageIcon = (ImageView) convertView.findViewById(R.id.msg);
				holder.messageIcon.setImageBitmap(ip.getMsgBitmap());
				convertView.setTag(holder);
			}
			
			ItemHolder holder = (ItemHolder) convertView.getTag();
			holder.name.setText(nick);
			if (service.getMessagesHash().containsKey(group + "/" + nick)) {
				if (service.getMessagesHash().get(group + "/" + nick).size() > 0) holder.name.setTypeface(Typeface.DEFAULT_BOLD);
			} else holder.name.setTypeface(Typeface.DEFAULT);
			
			holder.messageIcon.setVisibility(service.getMessagesList().contains(group + "/" + nick) ? View.VISIBLE : View.GONE);
			holder.statusIcon.setImageBitmap(ip.getIconByPresence(p));
			
			holder.status.setText(status);
	        holder.status.setVisibility((prefs.getBoolean("ShowStatuses", false) && status.length() > 0) ? View.VISIBLE : View.GONE);
	        holder.role.setImageBitmap(ip.getRoleIcon(role));
			
	        if (holder.caps != null) {
				String node = service.getNode(group + "/" + nick);
				ClientIcons.loadClientIcon(activity, holder.caps, node);
			}
	        
	        if (holder.avatar != null) {
				Avatars.loadAvatar(activity, group + "%" + nick, holder.avatar);
			}
	        
			return convertView;
		} else {
			boolean joined = service.getConferencesHash().get(item).isJoined();
			int count = service.getMessagesCount(item);
			
			if (convertView == null || convertView.findViewById(R.id.state) == null) {
				LayoutInflater inflater = (LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.group, null, false);
				
				GroupHolder holder = new GroupHolder();
				holder.text = (TextView) convertView.findViewById(R.id.name);
				holder.text.setTextSize(fontSize+2);
				holder.text.setTextColor(prefs.getBoolean("DarkColors", false) ? 0xFFFFFFFF : 0xFF000000);
				holder.counter = (TextView) convertView.findViewById(R.id.msg_counter);
				holder.counter.setTextSize(fontSize);
				holder.state = (ImageView) convertView.findViewById(R.id.state);
				holder.messageIcon = (ImageView) convertView.findViewById(R.id.msg);
				holder.messageIcon.setVisibility(View.VISIBLE);
				
				convertView.setTag(holder);
				convertView.setBackgroundColor(prefs.getBoolean("DarkColors", false) ? 0x77525252 : 0xEEEEEEEE);
			}
			
			GroupHolder holder = (GroupHolder) convertView.getTag();
			holder.text.setText(item);
			holder.state.setImageResource(service.getCollapsedGroups().contains(item) ? R.drawable.close : R.drawable.open);
			holder.counter.setVisibility(count > 1 ? View.VISIBLE : View.GONE);
			holder.counter.setText(count+"");

			if (service.getMucMessagesList().contains(item)) {
				holder.messageIcon.setImageBitmap(ip.getMsgBitmap());
			} else {
				if (!joined) holder.messageIcon.setImageBitmap(ip.getOfflineBitmap());
				else holder.messageIcon.setImageBitmap(ip.getMucBitmap());
			}
			holder.messageIcon.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Intent intent = new Intent(activity, Chat.class);
					intent.putExtra("jid", item);
	           		activity.startActivity(intent);
				}
			});
			return convertView;
		}
	}
}
