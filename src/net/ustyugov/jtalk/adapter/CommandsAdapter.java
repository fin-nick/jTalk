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
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smackx.commands.AdHocCommandManager;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jtalk2.R;

import java.util.Iterator;

public class CommandsAdapter extends ArrayAdapter<Item> {
    private JTalkService service;

	public CommandsAdapter(Context context, String account, String jid) {
		super(context, R.id.item);
        service = JTalkService.getInstance();
        try {
            AdHocCommandManager manager = AdHocCommandManager.getAddHocCommandsManager(service.getConnection(account));
            if (manager != null) {

                DiscoverItems items = manager.discoverCommands(jid);
                Iterator<Item> it = items.getItems();
                while(it.hasNext()){
                    Item item = it.next();
                    add(item);
                }
            }
        } catch (Exception ignored) { }
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
        View v = convertView;
        Item item = getItem(position);
        String name = item.getName();
        if (name == null || name.length() < 1) name = item.getNode();
        
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.selector, null);
        }
        
        TextView label = (TextView) v.findViewById(R.id.item);
        label.setTextColor(Colors.PRIMARY_TEXT);
        label.setText(name);
        
        ImageView icon = (ImageView)v.findViewById(R.id.status);
        icon.setImageResource(R.drawable.icon_online);
        icon.setVisibility(View.INVISIBLE);
        return v;
    }
}
