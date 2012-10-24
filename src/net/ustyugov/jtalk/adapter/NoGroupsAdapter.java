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

import net.ustyugov.jtalk.Avatars;
import net.ustyugov.jtalk.ClientIcons;
import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.RosterEntry;
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

public class NoGroupsAdapter extends ArrayAdapter<RosterEntry> {
	private JTalkService service;
	private Activity activity;
	private IconPicker iconPicker;
	private SharedPreferences prefs;
	private int fontSize, statusSize;
	
	static class ViewHolder {
		protected TextView name;
		protected TextView status;
		protected TextView counter;
		protected ImageView statusIcon;
		protected ImageView messageIcon;
		protected ImageView avatar;
		protected ImageView caps;
	}
	
	public NoGroupsAdapter(Activity activity) {
        super(activity, R.id.name);
        this.activity = activity;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		this.fontSize = Integer.parseInt(activity.getResources().getString(R.string.DefaultFontSize));
		try {
			this.fontSize = Integer.parseInt(prefs.getString("RosterSize", activity.getResources().getString(R.string.DefaultFontSize)));
		} catch (NumberFormatException e) { }
		this.statusSize = fontSize - 4;
    }
	
	public void update(List<String> list) {
		this.service = JTalkService.getInstance();
		this.iconPicker = service.getIconPicker();
		clear();
		if (service != null && service.getRoster() != null) {
			for(String jid : list) {
	        	RosterEntry re = service.getRoster().getEntry(jid);
				add(re);
			}
		}
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final RosterEntry re = getItem(position);
		String name = re.getName();
		if (name == null || name.length() <= 0 ) name = re.getUser();
		
		Presence presence = service.getPresence(re.getUser());
		String status = service.getStatus(re.getUser());
		
		int count = service.getMessagesCount(re.getUser());
		
		if(convertView == null) {		
			LayoutInflater inflater = activity.getLayoutInflater();
			convertView = inflater.inflate(R.layout.entry, null, false);
			
			ViewHolder holder = new ViewHolder();
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.name.setTextSize(fontSize);
			holder.status = (TextView) convertView.findViewById(R.id.status);
			holder.status.setTextSize(statusSize);
			holder.status.setTextColor(prefs.getBoolean("DarkColors", false) ? 0xFFBBBBBB : 0xFF555555);
			holder.counter = (TextView) convertView.findViewById(R.id.msg_counter);
			holder.counter.setTextSize(fontSize);
			holder.messageIcon = (ImageView) convertView.findViewById(R.id.msg);
			holder.messageIcon.setImageBitmap(iconPicker.getMsgBitmap());
			holder.statusIcon = (ImageView) convertView.findViewById(R.id.status_icon);
			holder.statusIcon.setPadding(3, 0, 0, 0);
			holder.statusIcon.setVisibility(View.VISIBLE);
			
			if (prefs.getBoolean("LoadAvatar", false)) {
				holder.avatar = (ImageView) convertView.findViewById(R.id.contactlist_pic);
			}
			
			if (prefs.getBoolean("ShowCaps", false)) {
				holder.caps = (ImageView) convertView.findViewById(R.id.caps);
			}
			convertView.setTag(holder);
		}
		
		ViewHolder holder = (ViewHolder) convertView.getTag();
		holder.name.setText(name);
		if (service.getComposeList().contains(re.getUser())) holder.name.setTextColor(0xFFAA2323);
		else holder.name.setTextColor(prefs.getBoolean("DarkColors", false) ? 0xFFEEEEEE : 0xFF343434);
		
		if (service.getMessagesHash().containsKey(re.getUser())) {
			if (service.getMessagesHash().get(re.getUser()).size() > 0) holder.name.setTypeface(Typeface.DEFAULT_BOLD);
		} else holder.name.setTypeface(Typeface.DEFAULT);
		
		holder.status.setText(status);
        holder.status.setVisibility((prefs.getBoolean("ShowStatuses", false) && status.length() > 0) ? View.VISIBLE : View.GONE);
		
        if (count > 0) {
        	holder.messageIcon.setVisibility(View.VISIBLE);
			holder.counter.setVisibility(View.VISIBLE);
			holder.counter.setText(count+"");
		} else {
			holder.messageIcon.setVisibility(View.GONE);
			holder.counter.setVisibility(View.GONE);
		}
        
        if (holder.caps != null) {
			String node = service.getNode(re.getUser());
			ClientIcons.loadClientIcon(activity, holder.caps, node);
		}
        
        if (holder.avatar != null) {
			Avatars.loadAvatar(activity, re.getUser(), holder.avatar);
		}
        
		if (iconPicker != null) holder.statusIcon.setImageBitmap(iconPicker.getIconByPresence(presence));
		return convertView;
	}
}
