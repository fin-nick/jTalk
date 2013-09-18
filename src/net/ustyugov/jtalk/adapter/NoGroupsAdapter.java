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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import net.ustyugov.jtalk.*;
import net.ustyugov.jtalk.Holders.GroupHolder;
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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jtalk2.R;

public class NoGroupsAdapter extends ArrayAdapter<RosterItem> {
	private JTalkService service;
	private Activity activity;
	private IconPicker iconPicker;
	private SharedPreferences prefs;
	private int fontSize, statusSize;

	public NoGroupsAdapter(Activity activity) {
        super(activity, R.id.name);
        this.activity = activity;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		this.fontSize = Integer.parseInt(activity.getResources().getString(R.string.DefaultFontSize));
		try {
			this.fontSize = Integer.parseInt(prefs.getString("RosterSize", activity.getResources().getString(R.string.DefaultFontSize)));
		} catch (NumberFormatException ignored) { }
		this.statusSize = fontSize - 4;
    }
	
	public void update() {
		this.service = JTalkService.getInstance();
		this.iconPicker = service.getIconPicker();
		boolean hideOffline = prefs.getBoolean("hideOffline", false);
		clear();
		
		Cursor cursor = service.getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '" + 1 + "'", null, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			do {
				String account = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID)).trim();
				XMPPConnection connection = service.getConnection(account);

                RosterItem item = new RosterItem(account, RosterItem.Type.account, null);
                item.setName(account);
                if (cursor.getCount() > 1) add(item);
                else while (service.getCollapsedGroups().contains(account)) service.getCollapsedGroups().remove(account);

                Roster roster = service.getRoster(account);
                if (roster != null && connection != null && connection.isAuthenticated() && !service.getCollapsedGroups().contains(account)) {
                    // add self contact
                    Iterator<Presence> it = roster.getPresences(account);
                    if (prefs.getBoolean("SelfContact", true) && it.hasNext() && it.next().isAvailable()) {
                        RosterEntry entry = new RosterEntry(account, account, RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem self = new RosterItem(account, RosterItem.Type.self, entry);
                        add(self);
                    }

                    // add conferences and privates
                    if (!service.getConferencesHash(account).isEmpty()) {
                        if (prefs.getBoolean("ShowMucGroup", false)) {
                            RosterItem mucGroup = new RosterItem(account, RosterItem.Type.group, null);
                            mucGroup.setName(service.getString(R.string.MUC));
                            add(mucGroup);
                            if (service.getCollapsedGroups().contains(service.getString(R.string.MUC))) mucGroup.setCollapsed(true);
                        } else {
                            while (service.getCollapsedGroups().contains(service.getString(R.string.MUC))) service.getCollapsedGroups().remove(service.getString(R.string.MUC));
                        }

                        if (!service.getCollapsedGroups().contains(service.getString(R.string.MUC))) {
                            Enumeration<String> groupEnum = service.getConferencesHash(account).keys();
                            while(groupEnum.hasMoreElements()) {
                                RosterItem muc = new RosterItem(account, RosterItem.Type.muc, null);
                                muc.setName(groupEnum.nextElement());
                                add(muc);
                            }

                            List<String> privates = service.getPrivateMessages(account);
                            for (String jid : privates) {
                                RosterEntry e = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                                RosterItem i = new RosterItem(account, RosterItem.Type.entry, e);
                                add(i);
                            }
                        }
                    }

                    List<String> list = new ArrayList<String>();

                    for (RosterEntry rosterEntry : roster.getEntries()) {
                        String jid = rosterEntry.getUser();
                        String name = rosterEntry.getName();
                        if (name == null) name = jid;
                        Presence.Type presenceType = service.getType(account, jid);
                        if (hideOffline) {
                            if (presenceType != Presence.Type.unavailable) list.add(jid);
                        } else {
                            list.add(jid);
                        }
                    }

                    if (prefs.getBoolean("SortByStatuses", true)) list = SortList.sortSimpleContacts(account, list);

                    for (String jid: list) {
                        RosterEntry re = roster.getEntry(jid);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, re);
                        add(i);
                    }
                } else item.setCollapsed(true);
			} while (cursor.moveToNext());
			cursor.close();
		}
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		RosterItem item = getItem(position);
        String account = item.getAccount();
        if (item.isGroup()) {
            GroupHolder holder;
            if (convertView == null || convertView.findViewById(R.id.group_layout) == null) {
                LayoutInflater inflater = activity.getLayoutInflater();
                convertView = inflater.inflate(R.layout.group, null, false);

                holder = new GroupHolder();
                holder.messageIcon = (ImageView) convertView.findViewById(R.id.msg);
                holder.messageIcon.setVisibility(View.INVISIBLE);
                holder.text = (TextView) convertView.findViewById(R.id.name);
                holder.text.setTextSize(fontSize);
                holder.text.setTextColor(Colors.PRIMARY_TEXT);
                holder.state = (ImageView) convertView.findViewById(R.id.state);
                convertView.setTag(holder);
                convertView.setBackgroundColor(Colors.GROUP_BACKGROUND);
            } else {
                holder = (GroupHolder) convertView.getTag();
            }
            holder.text.setText(item.getName());
            holder.messageIcon.setImageResource(R.drawable.icon_msg);
            holder.messageIcon.setVisibility(View.INVISIBLE);
            holder.state.setImageResource(item.isCollapsed() ? R.drawable.close : R.drawable.open);
            convertView.setBackgroundColor(Colors.GROUP_BACKGROUND);
            return convertView;
        } else if (item.isAccount()) {
            Holders.AccountHolder holder;
            if (convertView == null || convertView.findViewById(R.id.account_layout) == null) {
                LayoutInflater inflater = activity.getLayoutInflater();
                convertView = inflater.inflate(R.layout.account, null, false);

                holder = new Holders.AccountHolder();
                holder.avatar = (ImageView) convertView.findViewById(R.id.avatar);
                holder.jid = (TextView) convertView.findViewById(R.id.jid);
                holder.jid.setTextSize(fontSize);
                holder.jid.setTextColor(Colors.PRIMARY_TEXT);
                holder.status = (TextView) convertView.findViewById(R.id.status);
                holder.status.setTextSize(statusSize);
                holder.status.setTextColor(Colors.SECONDARY_TEXT);
                holder.state = (ImageView) convertView.findViewById(R.id.state);
                convertView.setTag(holder);
                convertView.setBackgroundColor(Colors.ACCOUNT_BACKGROUND);
            } else {
                holder = (Holders.AccountHolder) convertView.getTag();
            }
            holder.jid.setText(account);
            String status = service.getState(account);
            holder.status.setText(status);
            holder.state.setVisibility(status.length() > 0 ? View.VISIBLE : View.GONE);
            holder.state.setImageResource(item.isCollapsed() ? R.drawable.close : R.drawable.open);
            holder.avatar.setVisibility(View.INVISIBLE);
            Avatars.loadAvatar(activity, account, holder.avatar);
            return convertView;
		} else if (item.isEntry() || item.isSelf()) {
			String name = item.getName();
			String jid = item.getEntry().getUser();
			if (name == null || name.length() <= 0 ) name = jid;
			if (item.isSelf()) name += " (self)";
			
			Presence presence = service.getPresence(item.getAccount(), jid);
			String status = service.getStatus(account, jid);
			
			int count = service.getMessagesCount(account, jid);
			
			if(convertView == null || convertView.findViewById(R.id.entry_layout) == null) {
				LayoutInflater inflater = activity.getLayoutInflater();
				convertView = inflater.inflate(R.layout.entry, null, false);
				
				ItemHolder holder = new ItemHolder();
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
				holder.statusIcon.setPadding(3, 3, 0, 0);
				holder.statusIcon.setVisibility(View.VISIBLE);
				holder.avatar = (ImageView) convertView.findViewById(R.id.contactlist_pic);
				holder.caps = (ImageView) convertView.findViewById(R.id.caps);
				convertView.setTag(holder);
			}
			
			ItemHolder holder = (ItemHolder) convertView.getTag();
			holder.name.setText(name);
			if (service.getComposeList().contains(jid)) holder.name.setTextColor(Colors.HIGHLIGHT_TEXT);
			else holder.name.setTextColor(Colors.PRIMARY_TEXT);
			
			if (service.getActiveChats(account).contains(jid)) {
				holder.name.setTypeface(Typeface.DEFAULT_BOLD);
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
	        
	        if (prefs.getBoolean("ShowCaps", false)) {
				String node = service.getNode(account, jid);
				ClientIcons.loadClientIcon(activity, holder.caps, node);
			}
	        
	        if (prefs.getBoolean("LoadAvatar", false)) {
				Avatars.loadAvatar(activity, jid, holder.avatar);
			}

            if (prefs.getBoolean("ColorLines", false)) {
                if ((position % 2) != 0) convertView.setBackgroundColor(Colors.ENTRY_BACKGROUND);
                else convertView.setBackgroundColor(0x00000000);
            }
	        
			if (iconPicker != null) holder.statusIcon.setImageBitmap(iconPicker.getIconByPresence(presence));
			return convertView;
		} else if (item.isMuc()) {
			String name = item.getName();
			int count = service.getMessagesCount(account, name);
			
			if(convertView == null || convertView.findViewById(R.id.entry_layout) == null) {
				LayoutInflater inflater = activity.getLayoutInflater();
				convertView = inflater.inflate(R.layout.entry, null, false);
				
				ItemHolder holder = new ItemHolder();
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
				holder.statusIcon.setPadding(3, 3, 0, 0);
				holder.statusIcon.setVisibility(View.VISIBLE);
				holder.avatar = (ImageView) convertView.findViewById(R.id.contactlist_pic);
				holder.avatar.setVisibility(View.GONE);
				holder.caps = (ImageView) convertView.findViewById(R.id.caps);
				holder.caps.setVisibility(View.GONE);
				convertView.setTag(holder);
			}
			
			String subject = null;
			boolean joined = false;
			if (service.getConferencesHash(account).containsKey(name)) {
				MultiUserChat muc = service.getConferencesHash(account).get(name);
				subject = muc.getSubject();
				joined = muc.isJoined();
			}
			if (subject == null) subject = "";
			
			ItemHolder holder = (ItemHolder) convertView.getTag();
			holder.name.setTypeface(Typeface.DEFAULT);
			holder.name.setText(StringUtils.parseName(name));
			if (service.isHighlight(account, name)) holder.name.setTextColor(Colors.HIGHLIGHT_TEXT);
			else holder.name.setTextColor(Colors.PRIMARY_TEXT);
			
			holder.status.setText(subject);
	        holder.status.setVisibility((prefs.getBoolean("ShowStatuses", false) && subject.length() > 0) ? View.VISIBLE : View.GONE);
			
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

            if (prefs.getBoolean("ColorLines", false)) {
                if ((position % 2) != 0) convertView.setBackgroundColor(Colors.ENTRY_BACKGROUND);
                else convertView.setBackgroundColor(0x00000000);
            }

			if (iconPicker != null) {
				if (joined) holder.statusIcon.setImageBitmap(iconPicker.getMucBitmap());
				else holder.statusIcon.setImageBitmap(iconPicker.getNoneBitmap());
			}
			return convertView;
		} else return null;
	}
}
