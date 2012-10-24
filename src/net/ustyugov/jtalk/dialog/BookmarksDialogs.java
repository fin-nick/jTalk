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

import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bookmark.BookmarkManager;
import org.jivesoftware.smackx.bookmark.BookmarkedConference;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.jtalk2.R;

public class BookmarksDialogs {
	
	public static void AddDialog(final Activity a, String jid, String name) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(a);
		final JTalkService service = JTalkService.getInstance();
		
		LayoutInflater inflater = a.getLayoutInflater();
		View layout = inflater.inflate(R.layout.bookmark_dialog, (ViewGroup) a.findViewById(R.id.bookmarks_dialog_linear));
	    
	    final EditText nameEdit = (EditText) layout.findViewById(R.id.bookmarks_dialog_name);
	    if (name != null) nameEdit.setText(name);
	    
	    final EditText passEdit = (EditText) layout.findViewById(R.id.bookmarks_dialog_pass);
	    
	    final EditText groupEdit = (EditText) layout.findViewById(R.id.bookmarks_dialog_jid);
	    if (jid != null) groupEdit.setText(jid);
	    else groupEdit.setText(prefs.getString("lastGroup", ""));
	    
	    final EditText nickEdit = (EditText) layout.findViewById(R.id.bookmarks_dialog_nick);
	    nickEdit.setText(prefs.getString("lastNick", ""));
	    
	    final CheckBox autoJoin = (CheckBox) layout.findViewById(R.id.auto_join);
	    autoJoin.setChecked(false);
	    
		AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setView(layout);
		builder.setTitle("Add bookmark");
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String name = nameEdit.getText().toString();
				String group = groupEdit.getText().toString();
				String nick = nickEdit.getText().toString();
				String pass = passEdit.getText().toString();
				
				if (group.length() > 0) {
					if (name.length() <= 0) name = group;
					if (nick.length() <= 0) nick = StringUtils.parseName(prefs.getString("JID", ""));
					try {
						BookmarkManager bm = BookmarkManager.getBookmarkManager(service.getConnection());
						bm.addBookmarkedConference(name, group, autoJoin.isChecked(), nick, pass);
					} catch (XMPPException e) {
						Toast.makeText(a, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
					}
					
					Intent i = new Intent(net.ustyugov.jtalk.Constants.UPDATE);
					i.putExtra("bookmarks", true);
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
	
	public static void EditDialog(final Activity a, final String group, final BookmarkManager bm) {
		BookmarkedConference item = null;
		try {
			for(BookmarkedConference bc : bm.getBookmarkedConferences()) {
				if (bc.getJid().equals(group)) {
					item = bc;
				}
			}
		} catch (XMPPException e) {	}
		
		if (item == null) return;
		
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(a);
		
		LayoutInflater inflater = a.getLayoutInflater();
		View layout = inflater.inflate(R.layout.bookmark_dialog, (ViewGroup) a.findViewById(R.id.bookmarks_dialog_linear));
	    
		final EditText groupEdit = (EditText) layout.findViewById(R.id.bookmarks_dialog_jid);
		groupEdit.setText(item.getJid());
		groupEdit.setEnabled(false);
	    final EditText nameEdit = (EditText) layout.findViewById(R.id.bookmarks_dialog_name);
	    nameEdit.setText(item.getName());
	    final EditText passEdit = (EditText) layout.findViewById(R.id.bookmarks_dialog_pass);
	    passEdit.setText(item.getPassword());
	    final EditText nickEdit = (EditText) layout.findViewById(R.id.bookmarks_dialog_nick);
	    nickEdit.setText(item.getNickname());
	    final CheckBox autoJoin = (CheckBox) layout.findViewById(R.id.auto_join);
	    autoJoin.setChecked(item.isAutoJoin());
	    
		AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setView(layout);
		builder.setTitle("Edit bookmark");
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String name = nameEdit.getText().toString();
				String nick = nickEdit.getText().toString();
				String pass = passEdit.getText().toString();
				
				if (group.length() > 0) {
					if (name.length() <= 0) name = group;
					if (nick.length() <= 0) nick = StringUtils.parseName(prefs.getString("JID", ""));
					try {
						bm.removeBookmarkedConference(group);
						bm.addBookmarkedConference(name, group, autoJoin.isChecked(), nick, pass);
					} catch (XMPPException e) {
						Toast.makeText(a, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
					}
					
					Intent i = new Intent(net.ustyugov.jtalk.Constants.UPDATE);
					i.putExtra("bookmarks", true);
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
