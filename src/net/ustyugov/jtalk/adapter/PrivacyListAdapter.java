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
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.PrivacyList;
import org.jivesoftware.smack.PrivacyListManager;
import org.jivesoftware.smack.XMPPException;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jtalk2.R;

public class PrivacyListAdapter extends ArrayAdapter<PrivacyList> {
	private JTalkService service;
	private Context context;
	private String account;
	
	static class ViewHolder {
		protected TextView name;
		protected ImageView icon;
		protected ImageView close;
	}
	
	public PrivacyListAdapter(Context context, String account) {
		super(context, R.id.item);
		this.context = context;
        this.service = JTalkService.getInstance();
        this.account = account;
        update();
	}
	
	public void update() {
		PrivacyListManager plm = PrivacyListManager.getInstanceFor(service.getConnection(account));
		clear();
		try {
			PrivacyList[] array = plm.getPrivacyLists();
			for(int i = 0; i < array.length; i++) {
				PrivacyList pl = array[i];
				add(pl);
			}
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		IconPicker ip = service.getIconPicker();
		PrivacyList list = getItem(position);
      
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.selector, null);
            
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.item);
            holder.name.setTextColor(Colors.PRIMARY_TEXT);
            holder.icon = (ImageView) convertView.findViewById(R.id.status);
            convertView.setTag(holder);
        } else {
        	holder = (ViewHolder) convertView.getTag();
        }
        
        if (list.isDefaultList()) {
        	holder.icon.setImageBitmap(ip.getChatBitmap());
        	holder.name.setText(list.toString() + " (default)");
        }
        else if (list.isActiveList()) {
        	holder.icon.setImageBitmap(ip.getOnlineBitmap());
        	holder.name.setText(list.toString() + " (active)");
        }
        else {
        	holder.icon.setImageBitmap(ip.getOfflineBitmap());
        	holder.name.setText(list.toString());
        }
       	
        return convertView;
    }
}
