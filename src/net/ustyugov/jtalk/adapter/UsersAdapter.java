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
import net.ustyugov.jtalk.RosterItem;
import net.ustyugov.jtalk.SortList;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jtalk2.R;

public class UsersAdapter extends ArrayAdapter<RosterItem> {
	private JTalkService service;
	private String group;
    private String account;
	
	public UsersAdapter(Context context, String account, String group) {
		super(context, R.id.item);
        this.account = account;
		this.group = group;
        this.service = JTalkService.getInstance();
        update();
	}
	
	public void update() {
		clear();
        List<String> users = new ArrayList<String>();
        Iterator<Presence> it = service.getRoster(account).getPresences(group);
        while (it.hasNext()) {
            Presence p = it.next();
            users.add(StringUtils.parseResource(p.getFrom()));
        }

        users = SortList.sortParticipantsInChat(account, group, users);
        for (String user: users) {
            RosterItem item = new RosterItem(account, RosterItem.Type.group, null);
            item.setName(user);
        }
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		IconPicker ip = service.getIconPicker();
        View v = convertView;
        RosterItem item = getItem(position);
        String nick = item.getName();
        String account = item.getAccount();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
        
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.selector, null);
        }
        
        TextView label = (TextView) v.findViewById(R.id.item);
        label.setText(nick);
        if (Build.VERSION.SDK_INT >= 11) {
        	label.setTextColor(prefs.getBoolean("DarkColors", false) ? 0xFFFFFFFF : 0xFF000000);
        } else label.setTextColor(0xFF232323);
        
		Presence presence = service.getRoster(account).getPresenceResource(group + "/" + nick);
      	ImageView icon = (ImageView)v.findViewById(R.id.status);
       	icon.setImageBitmap(ip.getIconByPresence(presence));
       	
        ImageView close = (ImageView) v.findViewById(R.id.close);
		close.setVisibility(View.GONE);
        return v;
    }

}
