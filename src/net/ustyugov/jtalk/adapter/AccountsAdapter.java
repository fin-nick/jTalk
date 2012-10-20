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

import net.ustyugov.jtalk.Account;
import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.util.StringUtils;

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

public class AccountsAdapter extends ArrayAdapter<Account> {
	private JTalkService service;
	private Context context;
	
	public AccountsAdapter(Context context) {
		super(context, R.id.item);
		this.context = context;
        this.service = JTalkService.getInstance();
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		IconPicker ip = service.getIconPicker();
        View v = convertView;
        Account account = getItem(position);
        final String jid = account.getJid();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.selector, null);
        }
        
        TextView label = (TextView) v.findViewById(R.id.item);
        label.setText(jid);
        label.setTextColor(prefs.getBoolean("DarkColors", false) ? 0xFFFFFFFF : 0xFF000000);
        
      	ImageView icon = (ImageView)v.findViewById(R.id.status);
      	icon.setImageResource(R.drawable.icon_offline);
      	if (service != null && service.isAuthenticated()) {
      		if (jid.equals(StringUtils.parseBareAddress(service.getConnection().getUser()))) {
      			String mode = prefs.getString("currentMode", "available");
      			icon.setImageBitmap(ip.getIconByMode(mode));
      		}
      	}
       	
        ImageView close = (ImageView) v.findViewById(R.id.close);
		close.setVisibility(View.GONE);
        return v;
    }
}
