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
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smackx.muc.HostedRoom;

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

public class MucSearchAdapter extends ArrayAdapter<HostedRoom>{
	private Context context;
	
	static class ViewHolder {
		protected TextView name;
		protected TextView jid;
		protected ImageView icon;
	}

	public MucSearchAdapter(Context context, Collection<HostedRoom> rooms) {
		super(context, R.id.name);
		this.context = context;
		for (HostedRoom room : rooms) {
			if (room.getJid().contains("@")) add(room);
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		IconPicker ip = JTalkService.getInstance().getIconPicker();
		int fontSize;
		try {
			fontSize = Integer.parseInt(prefs.getString("RosterSize", context.getResources().getString(R.string.DefaultFontSize)));
		} catch (NumberFormatException e) {
			fontSize = Integer.parseInt(context.getResources().getString(R.string.DefaultFontSize));
		}
		
		String name = getItem(position).getName();
        String jid  = getItem(position).getJid();
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.entry, null, false);
            
            ViewHolder holder = new ViewHolder();
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.name.setTextSize(fontSize);
			holder.name.setTextColor(Colors.PRIMARY_TEXT);
			holder.jid = (TextView) convertView.findViewById(R.id.status);
			holder.jid.setVisibility(View.VISIBLE);
			holder.jid.setTextSize(fontSize - 4);
			holder.jid.setTextColor(Colors.SECONDARY_TEXT);
            holder.icon = (ImageView) convertView.findViewById(R.id.status_icon);
    		holder.icon.setVisibility(View.VISIBLE);
    		holder.icon.setImageBitmap(ip.getMucBitmap());
    		convertView.setTag(holder);
        }

		ViewHolder holder = (ViewHolder) convertView.getTag();
		holder.name.setText(name);
		holder.jid.setText(jid);
		return convertView;
    }
}
