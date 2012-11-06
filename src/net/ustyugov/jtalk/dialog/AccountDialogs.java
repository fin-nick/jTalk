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

package net.ustyugov.jtalk.dialog;

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import com.jtalk2.R;

public class AccountDialogs {
	public static void addDialog(final Activity a) {
		LayoutInflater inflater = a.getLayoutInflater();
		View layout = inflater.inflate(R.layout.account_dialog, (ViewGroup) a.findViewById(R.id.add_account_linear));
	    
	    final EditText jidEdit = (EditText) layout.findViewById(R.id.account_jid);
	    final EditText passEdit = (EditText) layout.findViewById(R.id.account_password);
	    final EditText resEdit = (EditText) layout.findViewById(R.id.account_resource);
	    final EditText serEdit = (EditText) layout.findViewById(R.id.account_server);
	    final EditText portEdit = (EditText) layout.findViewById(R.id.account_port);
	    final CheckBox active = (CheckBox) layout.findViewById(R.id.account_active);
	    final CheckBox tls = (CheckBox) layout.findViewById(R.id.account_tls);
	    final CheckBox sasl = (CheckBox) layout.findViewById(R.id.account_sasl);
	    
		AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setView(layout);
		builder.setTitle(a.getString(R.string.Add));
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String jid = jidEdit.getText().toString();
				if (jid == null) jid = "";
				String pass = passEdit.getText().toString();
				if (pass == null) pass = "";
				String res = resEdit.getText().toString();
				if (res == null || res.length() < 1) res = "Android";
				String ser = serEdit.getText().toString();
				if (ser == null) ser = "";
				String port = portEdit.getText().toString();
				if (port == null) port = "5222";
				boolean e = active.isChecked();
				boolean t = tls.isChecked();
				boolean s = sasl.isChecked();
				
				if (jid.length() > 0) {
					ContentValues values = new ContentValues();
	 	            values.put(AccountDbHelper.JID, jid);
	 	            values.put(AccountDbHelper.PASS, pass);
	 	            values.put(AccountDbHelper.RESOURCE, res);
	 	            values.put(AccountDbHelper.SERVER, ser);
	 	            values.put(AccountDbHelper.PORT, port);
	 	            if (e) values.put(AccountDbHelper.ENABLED, "1");
	 	            else values.put(AccountDbHelper.ENABLED, "0");
	 	            if (t) values.put(AccountDbHelper.TLS, "1");
	 	            else values.put(AccountDbHelper.TLS, "0");
	 	            if (s) values.put(AccountDbHelper.SASL, "1");
	 	            else values.put(AccountDbHelper.SASL, "0");
	 	            
	 	            a.getContentResolver().insert(JTalkProvider.ACCOUNT_URI, values);
	 	           
	 	            Intent i = new Intent(Constants.UPDATE);
	             	a.sendBroadcast(i);
				}
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}
	
	public static void editDialog(final Activity a, final int id) {
		String username = "";
		String password = "";
		String resource = "";
		String service = "";
		String e = "";
		String t = "";
		String s = "";
		int port = 5222;
		
		Cursor cursor = a.getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, "_id = '" + id + "'", null, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			username = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID));
			password = cursor.getString(cursor.getColumnIndex(AccountDbHelper.PASS));
			resource = cursor.getString(cursor.getColumnIndex(AccountDbHelper.RESOURCE));
			if (resource == null || resource.length() < 1) resource = "Android";
			service = cursor.getString(cursor.getColumnIndex(AccountDbHelper.SERVER));
				
			try {
				port = Integer.parseInt(cursor.getString(cursor.getColumnIndex(AccountDbHelper.PORT)));
			} catch (NumberFormatException nfc) { }
				
			e = cursor.getString(cursor.getColumnIndex(AccountDbHelper.ENABLED));
			t = cursor.getString(cursor.getColumnIndex(AccountDbHelper.TLS));
			s = cursor.getString(cursor.getColumnIndex(AccountDbHelper.SASL));
		}
		
		LayoutInflater inflater = a.getLayoutInflater();
		View layout = inflater.inflate(R.layout.account_dialog, (ViewGroup) a.findViewById(R.id.add_account_linear));
	    
	    final EditText jidEdit = (EditText) layout.findViewById(R.id.account_jid);
	    jidEdit.setText(username);
	    
	    final EditText passEdit = (EditText) layout.findViewById(R.id.account_password);
	    passEdit.setText(password);
	    
	    final EditText resEdit = (EditText) layout.findViewById(R.id.account_resource);
	    resEdit.setText(resource);
	    
	    final EditText serEdit = (EditText) layout.findViewById(R.id.account_server);
	    serEdit.setText(service);
	    
	    final EditText portEdit = (EditText) layout.findViewById(R.id.account_port);
	    portEdit.setText(port+"");
	    
	    final CheckBox active = (CheckBox) layout.findViewById(R.id.account_active);
	    active.setChecked(e.equals("1") ? true : false);
	    
	    final CheckBox tls = (CheckBox) layout.findViewById(R.id.account_tls);
	    tls.setChecked(t.equals("1") ? true : false);
	    
	    final CheckBox sasl = (CheckBox) layout.findViewById(R.id.account_sasl);
	    sasl.setChecked(s.equals("1") ? true : false);
	    
		AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setView(layout);
		builder.setTitle(a.getString(R.string.Add));
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String jid = jidEdit.getText().toString();
				if (jid == null) jid = "";
				String pass = passEdit.getText().toString();
				if (pass == null) pass = "";
				String res = resEdit.getText().toString();
				if (res == null || res.length() < 1) res = "Android";
				String ser = serEdit.getText().toString();
				if (ser == null) ser = "";
				String port = portEdit.getText().toString();
				if (port == null) port = "5222";
				boolean e = active.isChecked();
				boolean t = tls.isChecked();
				boolean s = sasl.isChecked();
				
				if (jid.length() > 0) {
					ContentValues values = new ContentValues();
	 	            values.put(AccountDbHelper.JID, jid);
	 	            values.put(AccountDbHelper.PASS, pass);
	 	            values.put(AccountDbHelper.RESOURCE, res);
	 	            values.put(AccountDbHelper.SERVER, ser);
	 	            values.put(AccountDbHelper.PORT, port);
	 	            if (e) values.put(AccountDbHelper.ENABLED, "1");
	 	            else values.put(AccountDbHelper.ENABLED, "0");
	 	            if (t) values.put(AccountDbHelper.TLS, "1");
	 	            else values.put(AccountDbHelper.TLS, "0");
	 	            if (s) values.put(AccountDbHelper.SASL, "1");
	 	            else values.put(AccountDbHelper.SASL, "0");
	 	            
	 	            a.getContentResolver().update(JTalkProvider.ACCOUNT_URI, values, "_id = '" + id + "'", null);
	 	           
	 	            Intent i = new Intent(Constants.UPDATE);
	             	a.sendBroadcast(i);
				}
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}
}
