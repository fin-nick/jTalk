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
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
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
	
	public static void AddDialog(final Activity a, final String account, String jid, String name) {
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
						BookmarkManager bm = BookmarkManager.getBookmarkManager(service.getConnection(account));
						bm.addBookmarkedConference(name, group, autoJoin.isChecked(), nick, pass);
					} catch (XMPPException e) {
						Toast.makeText(a, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
					}
					
					Intent i = new Intent(Constants.UPDATE);
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
	
	public static void EditDialog(final Activity a, final String account, final BookmarkedConference bc) {
		if (bc == null) return;
		
		final String jid = bc.getJid();
		
		LayoutInflater inflater = a.getLayoutInflater();
		View layout = inflater.inflate(R.layout.bookmark_dialog, (ViewGroup) a.findViewById(R.id.bookmarks_dialog_linear));
	    
		final EditText groupEdit = (EditText) layout.findViewById(R.id.bookmarks_dialog_jid);
		groupEdit.setText(bc.getJid());
		groupEdit.setEnabled(false);
	    final EditText nameEdit = (EditText) layout.findViewById(R.id.bookmarks_dialog_name);
	    nameEdit.setText(bc.getName());
	    final EditText passEdit = (EditText) layout.findViewById(R.id.bookmarks_dialog_pass);
	    passEdit.setText(bc.getPassword());
	    final EditText nickEdit = (EditText) layout.findViewById(R.id.bookmarks_dialog_nick);
	    nickEdit.setText(bc.getNickname());
	    final CheckBox autoJoin = (CheckBox) layout.findViewById(R.id.auto_join);
	    autoJoin.setChecked(bc.isAutoJoin());
	    
		AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setView(layout);
		builder.setTitle("Edit bookmark");
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				JTalkService service = JTalkService.getInstance();
				String name = nameEdit.getText().toString();
				String nick = nickEdit.getText().toString();
				String pass = passEdit.getText().toString();
				
				if (jid.length() > 0) {
					if (name.length() <= 0) name = jid;
					if (nick.length() <= 0) nick = StringUtils.parseName(service.getConnection(account).getUser());
					
					try {
						BookmarkManager manager = BookmarkManager.getBookmarkManager(JTalkService.getInstance().getConnection(account));
						manager.removeBookmarkedConference(jid);
						manager.addBookmarkedConference(name, jid, autoJoin.isChecked(), nick, pass);
					} catch (XMPPException e) {
						XMPPError error = e.getXMPPError();
						if (error != null) {
							Intent intent = new Intent(Constants.ERROR);
							intent.putExtra("error", "[" + error.getCode() + "] " + error.getMessage());
							a.sendBroadcast(intent);
						}
					}
					
					Intent i = new Intent(Constants.UPDATE);
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
