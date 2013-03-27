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
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.RosterItem;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.bookmark.BookmarkManager;
import org.jivesoftware.smackx.bookmark.BookmarkedConference;

import com.jtalk2.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class BookmarksAdapter extends ArrayAdapter<RosterItem> {
	Context context;
	
	static class ViewHolder {
		protected ImageView icon;
		protected TextView label;
		protected TextView jid;
	}
	
	public BookmarksAdapter(Context context, String account) {
		super(context, R.id.name);
		this.context = context;
		
		try {
			BookmarkManager bm = BookmarkManager.getBookmarkManager(JTalkService.getInstance().getConnection(account));
			Collection<BookmarkedConference> collection = bm.getBookmarkedConferences();
			for (BookmarkedConference bc : collection) {
				RosterItem item = new RosterItem(account, RosterItem.Type.muc, null);
				item.setObject(bc);
				add(item);
			}
		} catch (XMPPException e) {
			XMPPError error = e.getXMPPError();
			if (error != null) {
				Intent intent = new Intent(Constants.ERROR);
				intent.putExtra("error", "[" + error.getCode() + "] " + error.getMessage());
				context.sendBroadcast(intent);
			}
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
        	holder.icon.setImageBitmap(ip.getMucBitmap());
        	
        	holder.label = (TextView) convertView.findViewById(R.id.name);
        	holder.label.setTextColor(Colors.PRIMARY_TEXT);
            holder.label.setTextSize(fontSize);
        	
            holder.jid = (TextView) convertView.findViewById(R.id.status);
            holder.jid.setVisibility(View.VISIBLE);
            holder.jid.setTextColor(Colors.SECONDARY_TEXT);
            holder.jid.setTextSize(fontSize - 4);
            convertView.setTag(holder);
        } else {
        	holder = (ViewHolder) convertView.getTag();
        }

        BookmarkedConference item = (BookmarkedConference) getItem(position).getObject();
        String name = item.getName();
        String jid  = item.getJid();
        String nick = item.getNickname();

        holder.label.setText(name);
        holder.jid.setText(nick + " in " + jid);
        return convertView;
    }
}
