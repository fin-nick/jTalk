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

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.widget.*;
import net.ustyugov.jtalk.Account;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.Notify;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.util.StringUtils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jtalk2.R;

public class AccountsAdapter extends ArrayAdapter<Account> {
	private JTalkService service;
	private Activity activity;
    private int fontSize;
	
	public AccountsAdapter(Activity activity) {
		super(activity, R.id.item);
		this.activity = activity;
        this.service = JTalkService.getInstance();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        this.fontSize = Integer.parseInt(activity.getResources().getString(R.string.DefaultFontSize));
		try {
			this.fontSize = Integer.parseInt(prefs.getString("RosterSize", activity.getResources().getString(R.string.DefaultFontSize)));
		} catch (NumberFormatException ignored) { }
	}
	
	public void update() {
		clear();
		
		Cursor cursor = activity.getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, null, null, AccountDbHelper._ID);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			do {
				int id = cursor.getInt(cursor.getColumnIndex(AccountDbHelper._ID));
				String jid = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID));
                String enabled = cursor.getString(cursor.getColumnIndex(AccountDbHelper.ENABLED));
				
				Account account = new Account(id, jid);
                account.setEnabled(enabled.equals("1"));
				add(account);
			} while (cursor.moveToNext());
			cursor.close();
		}
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		IconPicker ip = service.getIconPicker();
		View v = convertView;
		final Account account = getItem(position);
		final String jid = account.getJid();
        final int id = account.getId();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.selector, null);
		}
    
		TextView label = (TextView) v.findViewById(R.id.item);
		label.setText(jid);
		label.setTextSize(fontSize);
        label.setTextColor(Colors.PRIMARY_TEXT);

        ToggleButton toggle = (ToggleButton) v.findViewById(R.id.toggle);
        toggle.setChecked(account.isEnabled());
        toggle.setVisibility(View.VISIBLE);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ToggleButton toggle = (ToggleButton) view;
                JTalkService service = JTalkService.getInstance();
                ContentValues values = new ContentValues();
                if (toggle.isChecked()) {
                    values.put(AccountDbHelper.ENABLED, "1");
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setMessage("Connect?");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            JTalkService.getInstance().connect(jid);
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                } else {
                    values.put(AccountDbHelper.ENABLED, "0");
                    if (service.isAuthenticated(jid)) {
                        service.disconnect(jid);
                        if (service.isAuthenticated()) Notify.updateNotify();
                        else Notify.offlineNotify(service.getGlobalState());
                    }
                }
                activity.getContentResolver().update(JTalkProvider.ACCOUNT_URI, values, "_id = '" + id + "'", null);
            }
        });

		ImageView icon = (ImageView)v.findViewById(R.id.status);
		icon.setImageResource(R.drawable.icon_offline);
		if (service != null && service.isAuthenticated(account.getJid())) {
			if (jid.equals(StringUtils.parseBareAddress(service.getConnection(account.getJid()).getUser()))) {
				String mode = prefs.getString("currentMode", "available");
				icon.setImageBitmap(ip.getIconByMode(mode));
			}
		}
		return v;
	}
}
