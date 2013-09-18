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

import java.util.Collection;

import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.RosterItem;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jtalk2.R;
import org.jivesoftware.smack.util.StringUtils;

public class OnlineUsersAdapter extends ArrayAdapter<RosterItem> {
	private JTalkService service;
	
	public OnlineUsersAdapter(Context context) {
		super(context, R.id.item);
        this.service = JTalkService.getInstance();
        update();
	}
	
	public void update() {
		clear();
		for (XMPPConnection connection : service.getAllConnections()) {
			if (connection.getUser() != null) {
				String account = StringUtils.parseBareAddress(connection.getUser());
				Roster roster = service.getRoster(account);
				if (roster != null) {
					Collection<RosterEntry> users = roster.getEntries();
					for (RosterEntry entry : users) {
						if (roster.getPresence(entry.getUser()).isAvailable()) {
							RosterItem item = new RosterItem(account, RosterItem.Type.entry, entry);
							add(item);
						}
					}
				}
			}
		}
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		IconPicker ip = service.getIconPicker();
        View v = convertView;
        RosterItem item = getItem(position);
        RosterEntry entry = item.getEntry();
        
        String name = entry.getName();
        if (name == null || name.length() < 1) name = entry.getUser();
        
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.selector, null);
        }
        
        TextView label = (TextView) v.findViewById(R.id.item);
        label.setText(name);
        if (Build.VERSION.SDK_INT >= 11) {
        	label.setTextColor(Colors.PRIMARY_TEXT);
        } else label.setTextColor(0xFF232323);
        
		Presence presence = service.getRoster(item.getAccount()).getPresence(entry.getUser());
      	ImageView icon = (ImageView)v.findViewById(R.id.status);
       	icon.setImageBitmap(ip.getIconByPresence(presence));
        return v;
    }

}
