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

import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.packet.Presence;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jtalk2.R;

public class ResourceAdapter extends ArrayAdapter<String> {
	private JTalkService service;
	private String jid;
	private String account;
	
	public ResourceAdapter(Context context, String account, String jid, List<String> list) {
		super(context, R.layout.selector);
		this.jid = jid;
		this.account = account;
        this.service = JTalkService.getInstance();
        
        for(int i = 0; i < list.size(); i++) {
			add(list.get(i));
		}
    }
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		IconPicker ip = service.getIconPicker();
        View v = convertView;
        String resource = getItem(position);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
        
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.selector, null);
        }
        
		Presence presence = service.getRoster(account).getPresenceResource(jid + "/" + resource);
        	
      	ImageView icon = (ImageView)v.findViewById(R.id.status);
       	icon.setImageBitmap(ip.getIconByPresence(presence));
        
       	TextView label = (TextView) v.findViewById(R.id.item);
       	if (Build.VERSION.SDK_INT >= 11) {
        	label.setTextColor(Colors.PRIMARY_TEXT);
        } else label.setTextColor(0xFF000000);
        label.setText(resource);
        return v;
    }
}
