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

import java.util.*;

import net.ustyugov.jtalk.*;
import net.ustyugov.jtalk.Holders.GroupHolder;
import net.ustyugov.jtalk.Holders.ItemHolder;
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
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jtalk2.R;
import org.jivesoftware.smackx.packet.MUCUser;

public class MucRosterAdapter extends ArrayAdapter<RosterItem> {
	private JTalkService service;
	private Activity activity;
	private String account;
	private SharedPreferences prefs;
	private IconPicker iconPicker;
	private int fontSize, statusSize;
	
	public MucRosterAdapter(Activity activity, String account) {
        super(activity, R.id.name);
        this.account = account;
        this.activity = activity;
        this.service = JTalkService.getInstance();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        this.iconPicker = service.getIconPicker();
		this.fontSize = Integer.parseInt(activity.getResources().getString(R.string.DefaultFontSize));
		try {
			this.fontSize = Integer.parseInt(prefs.getString("RosterSize", activity.getResources().getString(R.string.DefaultFontSize)));
		} catch (NumberFormatException ignored) { }
		this.statusSize = fontSize - 4;
    }
	
	public void update() {
		clear();

        // Add conferences
        if (!service.getConferencesHash(account).isEmpty()) {
            Enumeration<String> groups = service.getConferencesHash(account).keys();
            while (groups.hasMoreElements()) {
                String group = groups.nextElement();
                RosterItem groupItem = new RosterItem(account, RosterItem.Type.group, null);
                groupItem.setName(group);
                add(groupItem);

                if (!service.getCollapsedGroups().contains(group)) {
                    RosterItem muc = new RosterItem(account, RosterItem.Type.muc, null);
                    muc.setName(group);
                    add(muc);

                    List<String> users = new ArrayList<String>();
                    Roster roster = service.getRoster(account);
                    XMPPConnection connection = service.getConnection(account);
                    Iterator<Presence> it = roster.getPresences(group);
                    while (it.hasNext()) {
                        Presence p = it.next();
                        users.add(StringUtils.parseResource(p.getFrom()));
                    }

                    if (prefs.getBoolean("SortByStatuses", true)) users = SortList.sortParticipantsInChat(account, group, users);
                    else Collections.sort(users, new SortList.StringComparator());

                    for (String user: users) {
                        RosterEntry entry = new RosterEntry(group + "/" + user, user, RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem item = new RosterItem(account, RosterItem.Type.entry, entry);
                        item.setName(user);
                        add(item);
                    }
                } else groupItem.setCollapsed(true);
            }
        }
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final RosterItem item = getItem(position);
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
        } else if (item.isEntry()) {
            RosterEntry re = item.getEntry();
            String jid = re.getUser();
            String name = re.getName();
            String role = "";
            if (name == null || name.length() <= 0 ) name = jid;

            Presence presence = service.getPresence(account, jid);
            String status = service.getStatus(account, jid);
            if (service.getComposeList().contains(jid)) status = service.getString(R.string.Composes);

            MUCUser mucUser = (MUCUser) presence.getExtension("x", "http://jabber.org/protocol/muc#user");
            if (mucUser != null) {
                role = mucUser.getItem().getRole();
            }

            int count = service.getMessagesCount(account, jid);

            ItemHolder holder;
            if (convertView == null || convertView.findViewById(R.id.entry_layout) == null) {
                LayoutInflater inflater = activity.getLayoutInflater();
                convertView = inflater.inflate(R.layout.entry, null, false);
                holder = new ItemHolder();
                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.name.setTextColor(Colors.PRIMARY_TEXT);
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

            holder.name.setText(name);
            if (role.equals("moderator")) holder.name.setTextColor(Colors.ROLE_MODERATOR);
            else if (role.equals("visitor")) holder.name.setTextColor(Colors.ROLE_VISITOR);
            else holder.name.setTextColor(Colors.PRIMARY_TEXT);

            if (service.getActiveChats(account).contains(jid)) {
                holder.name.setTypeface(Typeface.DEFAULT_BOLD);
            } else holder.name.setTypeface(Typeface.DEFAULT);

            if (prefs.getBoolean("ShowStatuses", false)) {
                holder.status.setVisibility(status.length() > 0 ? View.VISIBLE : View.GONE);
                holder.status.setText(status);
            } else holder.status.setVisibility(View.GONE);

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
                Avatars.loadAvatar(activity, jid.replaceAll("/", "%"), holder.avatar);
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

            String subject = "";
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

            if (iconPicker != null) {
                if (joined) holder.statusIcon.setImageBitmap(iconPicker.getMucBitmap());
                else holder.statusIcon.setImageBitmap(iconPicker.getNoneBitmap());
            }
            return convertView;
        } else return null;
	}
}
