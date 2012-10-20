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

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import com.jtalk2.R;

public class MucAdminMenu implements OnClickListener {
	private Activity activity;
	private MultiUserChat muc;
	private String nick;
	private String group;
	private CharSequence[] items = null;
	
	public MucAdminMenu(Activity activity, MultiUserChat muc, String nick) {
		this.activity = activity;
		this.muc = muc;
		this.nick = nick;
		this.group = muc.getRoom();
	}
	
	public void show() {
		items = new CharSequence[12];
		items[0] = activity.getString(R.string.GrantVoice);
		items[1] = activity.getString(R.string.GrantMember);
		items[2] = activity.getString(R.string.GrantModer);
		items[3] = activity.getString(R.string.GrantAdmin);
		items[4] = activity.getString(R.string.GrantOwner);
		items[5] = activity.getString(R.string.RevokeVoice);
		items[6] = activity.getString(R.string.RevokeMember);
		items[7] = activity.getString(R.string.RevokeModer);
		items[8] = activity.getString(R.string.RevokeAdmin);
		items[9] = activity.getString(R.string.RevokeOwner);
		items[10] = activity.getString(R.string.Kick);
		items[11] = activity.getString(R.string.Ban);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.Actions);
        builder.setItems(items, this);
        builder.create().show();
	}
	
	public void onClick(DialogInterface dialog, int which) { 
		switch (which) {
			case 0:
				try {
					muc.grantVoice(nick);
				} catch (XMPPException e) {	}
				break;
			case 1:
				try {
					String jid = muc.getOccupant(group + "/" + nick).getJid();
					if (jid != null) muc.grantMembership(jid);
				} catch(XMPPException e) { }
				break;
			case 2:
				try {
					muc.grantModerator(nick);
				} catch (XMPPException e) {	}
				break;
			case 3:
				try {
					String jid = muc.getOccupant(group + "/" + nick).getJid();
					if (jid != null) muc.grantAdmin(jid);
				} catch (XMPPException e) {	}
				break;
			case 4:
				try {
					String jid = muc.getOccupant(group + "/" + nick).getJid();
					if (jid != null) muc.grantOwnership(jid);
				} catch (XMPPException e) {	}
				break;
			case 5:
				try {
					muc.revokeVoice(nick);
				} catch (XMPPException e) {	}
				break;
			case 6:
				try {
					String jid = muc.getOccupant(group + "/" + nick).getJid();
					if (jid != null) muc.revokeMembership(jid);
				} catch (XMPPException e) {	}
				break;
			case 7:
				try {
					muc.revokeModerator(nick);
				} catch (XMPPException e) {	}
				break;
			case 8:
				try {
					String jid = muc.getOccupant(group + "/" + nick).getJid();
					if (jid != null) muc.revokeAdmin(jid);
				} catch (XMPPException e) {	}
				break;
			case 9:
				try {
					String jid = muc.getOccupant(group + "/" + nick).getJid();
					if (jid != null) muc.revokeOwnership(jid);
				} catch (XMPPException e) {	}
				break;
			case 10:
				MucDialogs.kickDialog(activity, muc, nick);
				break;
			case 11:
				String jid = muc.getOccupant(group + "/" + nick).getJid();
				if (jid != null) MucDialogs.banDialog(activity, muc, jid);
				break;
		}
	}
}
