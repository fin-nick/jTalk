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
import net.ustyugov.jtalk.DiscoItem;

import com.jtalk2.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class DiscoveryAdapter extends ArrayAdapter<DiscoItem>{
	private Context context;

	public DiscoveryAdapter(Context context, List<DiscoItem> items) {
		super(context, R.id.name);
		this.context = context;
		
		for (DiscoItem item : items) {
			add(item);
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		int fontSize;
		try {
			fontSize = Integer.parseInt(prefs.getString("RosterSize", context.getResources().getString(R.string.DefaultFontSize)));
		} catch (NumberFormatException e) {
			fontSize = Integer.parseInt(context.getResources().getString(R.string.DefaultFontSize));
		}
		
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.entry, null, false);
        }

        DiscoItem item = getItem(position);
        String jid  = item.getJid();
        String node = item.getNode();
        String name = item.getName();
        String cat  = item.getCategory();
        String type = item.getType();
        
        ImageView icon = (ImageView) v.findViewById(R.id.status_icon);
        icon.setImageResource(R.drawable.icon_online);
        icon.setVisibility(View.VISIBLE);
        if (cat != null) {
        	if (cat.equals("conference")) {
            	icon.setImageResource(R.drawable.icon_muc);
            	if (type != null && type.equals("irc")) icon.setImageResource(R.drawable.irc);
            }
            else if (cat.equals("client")) icon.setImageResource(R.drawable.noface);
            else if (cat.equals("gateway")) {
            	if (type != null) {
            		if (type.equals("msn")) icon.setImageResource(R.drawable.msn);
                	else if (type.equals("icq")) icon.setImageResource(R.drawable.icq);
                	else if (type.equals("gtalk")) icon.setImageResource(R.drawable.gtalk);
            	}
            } else if (cat.equals("error")) icon.setImageResource(R.drawable.icon_none);
        }
        
        TextView label = (TextView) v.findViewById(R.id.name);
		label.setTextSize(fontSize);
		label.setText(name);
		
		TextView id = (TextView) v.findViewById(R.id.status);
		id.setVisibility(View.VISIBLE);
		id.setTextSize(fontSize - 4);
		id.setText(jid);
		if (node != null) id.setText(id.getText() + " (" + node + ")");

        label.setTextColor(Colors.PRIMARY_TEXT);
        id.setTextColor(Colors.SECONDARY_TEXT);
		return v;
    }
}
