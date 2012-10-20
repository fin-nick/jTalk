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

import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smackx.packet.DiscoverItems.Item;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jtalk2.R;

public class CommandsAdapter extends ArrayAdapter<Item> {
	private JTalkService service;
	
	public CommandsAdapter(Context context) {
		super(context, R.id.item);
        this.service = JTalkService.getInstance();
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
        View v = convertView;
        Item item = getItem(position);
        String name = item.getName();
        if (name == null || name.length() < 1) name = item.getNode();
        
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.selector, null);
        }
        
        TextView label = (TextView) v.findViewById(R.id.item);
        label.setTextColor(prefs.getBoolean("DarkColors", false) ? 0xFFFFFFFF : 0xFF000000);
        label.setText(name);
        
        ImageView icon = (ImageView)v.findViewById(R.id.status);
        icon.setVisibility(View.GONE);
        
        ImageView close = (ImageView) v.findViewById(R.id.close);
		close.setVisibility(View.GONE);
        return v;
    }
}
