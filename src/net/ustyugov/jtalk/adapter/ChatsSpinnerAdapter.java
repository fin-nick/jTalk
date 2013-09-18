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

import net.ustyugov.jtalk.*;
import net.ustyugov.jtalk.Holders.ItemHolder;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
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

public class ChatsSpinnerAdapter extends ArrayAdapter<RosterItem> implements SpinnerAdapter {
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
		Cursor cursor = service.getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '" + 1 + "'", null, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			do {
				String account = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID)).trim();
				XMPPConnection connection = service.getConnection(account);

                for (String jid : service.getActiveChats(account)) {
                    if (!service.getConferencesHash(account).containsKey(jid)) {
                        RosterEntry entry = null;
                        Roster roster = service.getRoster(account);
                        if (roster != null) entry = roster.getEntry(jid);
                        if (entry == null) entry = new RosterEntry(jid, jid, RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem item = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(item);
                    }
                }

				Enumeration<String> groupEnum = service.getConferencesHash(account).keys();
				while(groupEnum.hasMoreElements()) {
					String name = groupEnum.nextElement();
					RosterItem item = new RosterItem(account, RosterItem.Type.muc, null);
					item.setName(name);
					add(item);
				}
			} while (cursor.moveToNext());
			cursor.close();
		}
	}
	
	public int getPosition(String account, String jid) {
		for (int i = 0; i < getCount(); i++) {
			RosterItem item = getItem(i);
			if (item.isEntry()) {
				if (item.getAccount().equals(account) && item.getEntry().getUser().equals(jid)) return i;
			} else if (item.isMuc()) {
				if (item.getAccount().equals(account) && item.getName().equals(jid)) return i;
			}
		}
		return 0;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        RosterItem item = getItem(position);
        String account = item.getAccount();
        String jid = service.getCurrentJid();
		
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.sherlock_spinner_item, null);
        }
        
        String name = jid;
        if (service.getConferencesHash(account).containsKey(jid)) {
        	name = StringUtils.parseName(jid);
        } else if (service.getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid))) {
        	name = StringUtils.parseResource(jid);
        } else {
        	RosterEntry re = item.getEntry();
            if (re != null) name = re.getName();
            if (name == null || name.equals("")) name = jid;
        }
        
        TextView label = (TextView) v.findViewById(android.R.id.text1);
        label.setText(name);
        if (service.getComposeList().contains(jid)) {
    		label.setTextColor(Colors.HIGHLIGHT_TEXT);
    	} else label.setTextColor(Colors.PRIMARY_TEXT);
        
        return v;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		IconPicker iconPicker = service.getIconPicker();
		int fontSize = Integer.parseInt(service.getResources().getString(R.string.DefaultFontSize));
		try {
			fontSize = Integer.parseInt(prefs.getString("RosterSize", service.getResources().getString(R.string.DefaultFontSize)));
		} catch (NumberFormatException e) { }
		int statusSize = fontSize - 4;
		
		RosterItem item = getItem(position);
		String account = item.getAccount();
		
		ItemHolder holder;
		if (convertView == null) {
			LayoutInflater inflater = activity.getLayoutInflater();
			convertView = inflater.inflate(R.layout.entry, null, false);
			holder = new ItemHolder();
			
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.name.setTextSize(fontSize);
			holder.status = (TextView) convertView.findViewById(R.id.status);
			holder.status.setTextSize(statusSize);
			holder.status.setTextColor(Colors.SECONDARY_TEXT);
			
			holder.counter = (TextView) convertView.findViewById(R.id.msg_counter);
			holder.counter.setTextSize(fontSize);
			holder.messageIcon = (ImageView) convertView.findViewById(R.id.msg);
			holder.messageIcon.setImageBitmap(iconPicker.getMsgBitmap());
			holder.statusIcon = (ImageView) convertView.findViewById(R.id.status_icon);
			holder.statusIcon.setVisibility(View.VISIBLE);
			holder.avatar = (ImageView) convertView.findViewById(R.id.contactlist_pic);
			holder.caps = (ImageView) convertView.findViewById(R.id.caps);
			convertView.setTag(holder);
		} else {
			holder = (ItemHolder) convertView.getTag();
		}
		
		if (item.isEntry()) {
			String jid = item.getEntry().getUser();
			String status = "";
			String name = jid;
			
			if (service.getComposeList().contains(jid)) status = service.getString(R.string.Composes);
			else status = service.getStatus(account, jid);
			
			if (service.getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid))) {
	        	name = StringUtils.parseResource(jid);
	        } else {
	        	RosterEntry re = item.getEntry();
	            if (re != null) name = re.getName();
	            if (name == null || name.equals("")) name = jid;
	        }
			
			Presence presence = service.getPresence(account, jid);
			int count = service.getMessagesCount(account, jid);
			
			if (service.getComposeList().contains(jid)) holder.name.setTextColor(Colors.HIGHLIGHT_TEXT);
			else holder.name.setTextColor(Colors.PRIMARY_TEXT);
	        holder.name.setText(name);
	        holder.name.setTypeface(Typeface.DEFAULT_BOLD);
	        
	        if (prefs.getBoolean("ShowStatuses", false)) {
	        	if (status != null && status.length() > 0) {
	        		holder.status.setVisibility(View.VISIBLE);
	            	holder.status.setText(status);
	        	} else {
	        		holder.status.setVisibility(View.GONE);
	        	}
	        }
			
	        if (count > 0) {
	        	holder.messageIcon.setVisibility(View.VISIBLE);
				holder.counter.setVisibility(View.VISIBLE);
				holder.counter.setText(count+"");
			} else {
				holder.messageIcon.setVisibility(View.GONE);
				holder.counter.setVisibility(View.GONE);
			}
	        
	        if (prefs.getBoolean("ShowCaps", false)) {
        		String node = service.getNode(account, jid);
    			ClientIcons.loadClientIcon(activity, holder.caps, node);
	        } else holder.caps.setVisibility(View.GONE);
	        
	        if (prefs.getBoolean("LoadAvatar", false)) Avatars.loadAvatar(activity, jid, holder.avatar);
			holder.statusIcon.setImageBitmap(iconPicker.getIconByPresence(presence));
			return convertView;
		} else if (item.isMuc()) {
			String name = item.getName();
			String subject = null;
			boolean joined = false;
			int count = service.getMessagesCount(account, name);
			
			if (service.getConferencesHash(account).containsKey(name)) {
				MultiUserChat muc = service.getConferencesHash(account).get(name);
				subject = muc.getSubject();
				joined = muc.isJoined();
			}
			
			if (service.isHighlight(account, name)) holder.name.setTextColor(Colors.HIGHLIGHT_TEXT);
			else holder.name.setTextColor(Colors.PRIMARY_TEXT);
	        holder.name.setText(StringUtils.parseName(name));
	        holder.name.setTypeface(Typeface.DEFAULT_BOLD);
	        
	        if (prefs.getBoolean("ShowStatuses", false)) {
	        	if (subject != null && subject.length() > 0) {
	        		holder.status.setVisibility(View.VISIBLE);
	            	holder.status.setText(subject);
	        	} else {
	        		holder.status.setVisibility(View.GONE);
	        	}
	        }
			
	        if (count > 0) {
	        	holder.messageIcon.setVisibility(View.VISIBLE);
				holder.counter.setVisibility(View.VISIBLE);
				holder.counter.setText(count+"");
			} else {
				holder.messageIcon.setVisibility(View.GONE);
				holder.counter.setVisibility(View.GONE);
			}
	        
	        holder.caps.setVisibility(View.GONE);
	        holder.avatar.setVisibility(View.GONE);
			if (joined) holder.statusIcon.setImageBitmap(iconPicker.getMucBitmap());
			else holder.statusIcon.setImageBitmap(iconPicker.getOfflineBitmap());
			return convertView;
		}
		return null;
	}
}
