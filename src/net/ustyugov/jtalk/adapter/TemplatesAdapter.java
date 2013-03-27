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

import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.Template;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.db.TemplatesDbHelper;
import net.ustyugov.jtalk.service.JTalkService;

import com.jtalk2.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TemplatesAdapter extends ArrayAdapter<Template> {
	Context context;
	
	static class ViewHolder {
		protected ImageView icon;
		protected TextView label;
		protected TextView jid;
	}
	
	public TemplatesAdapter(Context context) {
		super(context, R.id.name);
		this.context = context;
		Cursor cursor = context.getContentResolver().query(JTalkProvider.TEMPLATES_URI, null, null, null, TemplatesDbHelper._ID);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			do {
				int id = cursor.getInt(cursor.getColumnIndex(TemplatesDbHelper._ID));
				String message = cursor.getString(cursor.getColumnIndex(TemplatesDbHelper.TEXT));
				
				add(new Template(id, message));
			} while (cursor.moveToNext());
			cursor.close();
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
		
		ViewHolder holder;
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.entry, null);
            
            holder = new ViewHolder();
            holder.icon = (ImageView) convertView.findViewById(R.id.status_icon);
            holder.icon.setVisibility(View.VISIBLE);
        	holder.icon.setImageBitmap(ip.getMsgBitmap());
        	
        	holder.label = (TextView) convertView.findViewById(R.id.name);
        	holder.label.setTextColor(Colors.PRIMARY_TEXT);
            holder.label.setTextSize(fontSize);
        	
            holder.jid = (TextView) convertView.findViewById(R.id.status);
            holder.jid.setVisibility(View.GONE);
            convertView.setTag(holder);
        } else {
        	holder = (ViewHolder) convertView.getTag();
        }

        Template template = getItem(position);
        holder.label.setText(template.getText());
        return convertView;
    }
}
