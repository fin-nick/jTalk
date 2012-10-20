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

package net.ustyugov.jtalk;

import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;

import com.jtalk2.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

public class IconPicker {
	private Bitmap online;
	private Bitmap chat;
	private Bitmap away;
	private Bitmap xa;
	private Bitmap dnd;
	private Bitmap offline;
	private Bitmap none;
	private Bitmap muc;
	private Bitmap moderator;
	private Bitmap participant;
	private Bitmap visitor;
	private Bitmap msg;
	
	private int onlineId;
	private	int chatId;
	private int awayId;
	private int xaId;
	private int dndId;
	private int offlineId;
	private int noneId;
	private int mucId;
	private int moderatorId;
	private int participantId;
	private int visitorId;
	private int msgId;
	
	private Resources res;
	
	private SharedPreferences prefs;
	private Context context;
	private String currentPack = "default";
	
	public IconPicker(Context context) {
		this.context = context;
		this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
		loadIconPack();
	}
	
	public void loadIconPack() {
		Resources defres = context.getResources();
		String packName = prefs.getString("IconPack", "default");
		try {
			res = context.getPackageManager().getResourcesForApplication(packName);
			this.currentPack = packName;
		}
		catch(NameNotFoundException nnfe) {
			res = defres;
			this.currentPack = "default";
		}
			
		onlineId = res.getIdentifier(packName + ":drawable/online", null, null);
		chatId = res.getIdentifier(packName + ":drawable/chat", null, null);
		awayId = res.getIdentifier(packName + ":drawable/away", null, null);
		xaId = res.getIdentifier(packName + ":drawable/xa", null, null);
		dndId = res.getIdentifier(packName + ":drawable/dnd", null, null);
		offlineId = res.getIdentifier(packName + ":drawable/offline", null, null);
		noneId = res.getIdentifier(packName + ":drawable/none", null, null);
		mucId = res.getIdentifier(packName + ":drawable/muc", null, null);
		moderatorId = res.getIdentifier(packName + ":drawable/moderator", null, null);
		participantId = res.getIdentifier(packName + ":drawable/participant", null, null);
		visitorId = res.getIdentifier(packName + ":drawable/visitor", null, null);
		msgId = res.getIdentifier(packName + ":drawable/msg", null, null);
			
		online = BitmapFactory.decodeResource(res, onlineId);
		if (online == null) online = BitmapFactory.decodeResource(defres, R.drawable.icon_online);
		chat = BitmapFactory.decodeResource(res, chatId);
		if (chat == null) chat = BitmapFactory.decodeResource(defres, R.drawable.icon_chat);
		away = BitmapFactory.decodeResource(res, awayId);
		if (away == null) away = BitmapFactory.decodeResource(defres, R.drawable.icon_away);
		xa = BitmapFactory.decodeResource(res, xaId);
		if (xa == null) xa = BitmapFactory.decodeResource(defres, R.drawable.icon_xa);
		dnd = BitmapFactory.decodeResource(res, dndId);
		if (dnd == null) dnd = BitmapFactory.decodeResource(defres, R.drawable.icon_dnd);
		offline = BitmapFactory.decodeResource(res, offlineId);
		if (offline == null) offline = BitmapFactory.decodeResource(defres, R.drawable.icon_offline);
		none = BitmapFactory.decodeResource(res, noneId);
		if (none == null) none = BitmapFactory.decodeResource(defres, R.drawable.icon_none);
		msg = BitmapFactory.decodeResource(res, msgId);
		if (msg == null) msg = BitmapFactory.decodeResource(defres, R.drawable.msg);
		muc = BitmapFactory.decodeResource(res, mucId);
		if (muc == null) muc = BitmapFactory.decodeResource(defres, R.drawable.muc); 
		moderator = BitmapFactory.decodeResource(res, moderatorId);
		if (moderator == null) moderator = BitmapFactory.decodeResource(defres, R.drawable.moderator);
		participant = BitmapFactory.decodeResource(res, participantId);
		if (participant == null) participant = BitmapFactory.decodeResource(defres, R.drawable.participant);
		visitor = BitmapFactory.decodeResource(res, visitorId);
		if (visitor == null) visitor = BitmapFactory.decodeResource(defres, R.drawable.visitor);
	}
	
	public String getPackName() { return currentPack; }
	public Bitmap getOnlineBitmap() { return online; }
	public Bitmap getChatBitmap() { return chat; }
	public Bitmap getAwayBitmap() { return away; }
	public Bitmap getXaBitmap() { return xa; }
	public Bitmap getDndBitmap() { return dnd; }
	public Bitmap getOfflineBitmap() { return offline; }
	public Bitmap getMucBitmap() { return muc; }
	public Bitmap getModeratorBitmap() { return moderator; }
	public Bitmap getParticipantBitmap() { return participant; }
	public Bitmap getVisitorBitmap() { return visitor; }
	public Bitmap getNoneBitmap() { return none; }
	public Bitmap getMsgBitmap() { return msg; }
	
	public Bitmap getRoleIcon(String role) {
		if (role.equals("moderator")) return moderator;
		else if (role.equals("visitor")) return visitor;
		else return participant;
	}
	
	public Bitmap getIconByMode(String mode) {
		if (mode.equals("available")) return online;
		else if (mode.equals("chat")) return chat;
		else if (mode.equals("away")) return away;
		else if (mode.equals("xa")) return xa;
		else if (mode.equals("dnd")) return dnd;
		else return offline;
	}
	
	public Bitmap getIconByPresence(Presence presence) {
		if (presence != null) {
			Presence.Type type = presence.getType();
			if (type == Presence.Type.available) {
				Presence.Mode mode = presence.getMode();
				if(mode == Presence.Mode.away) return away;
				else if (mode == Presence.Mode.xa) return xa;
				else if (mode == Presence.Mode.dnd) return dnd;
				else if (mode == Presence.Mode.chat) return chat;
				else return online;
			} else if (type == Presence.Type.error) return none;
			else {
				JTalkService service = JTalkService.getInstance();
				Roster roster = service.getRoster();
				if (roster != null) {
					RosterEntry re = service.getRoster().getEntry(presence.getFrom());
					if (re != null) {
						ItemType it = re.getType();
						if (it.equals(ItemType.none) || it.equals(ItemType.from)) return none;
						else return offline;
					} else return offline;
				} else return offline;
			}
		} else return offline;
	}
	
	public Drawable getDrawableByPresence(Presence presence) {
		if (presence != null) {
			Presence.Type type = presence.getType();
			if (type == Presence.Type.available) {
				Presence.Mode mode = presence.getMode();
				if(mode == Presence.Mode.away) {
					if (currentPack.equals("default")) return context.getResources().getDrawable(R.drawable.icon_away);
					else return res.getDrawable(awayId);
				}
				else if (mode == Presence.Mode.xa) {
					if (currentPack.equals("default")) return context.getResources().getDrawable(R.drawable.icon_xa);
					else return res.getDrawable(xaId);
				}
				else if (mode == Presence.Mode.dnd) {
					if (currentPack.equals("default")) return context.getResources().getDrawable(R.drawable.icon_dnd);
					else return res.getDrawable(dndId);
				}
				else if (mode == Presence.Mode.chat) {
					if (currentPack.equals("default")) return context.getResources().getDrawable(R.drawable.icon_chat);
					else return res.getDrawable(chatId);
				}
				else {
					if (currentPack.equals("default")) return context.getResources().getDrawable(R.drawable.icon_online);
					else return res.getDrawable(onlineId);
				}
			} else {
				if (currentPack.equals("default")) return context.getResources().getDrawable(R.drawable.icon_offline);
				else return res.getDrawable(offlineId);
			}
		} else {
			if (currentPack.equals("default")) return context.getResources().getDrawable(R.drawable.icon_offline);
			else return res.getDrawable(offlineId);
		}
	}
	
	public Drawable getMucDrawable() {
		try {
			if (currentPack.equals("default")) return context.getResources().getDrawable(R.drawable.muc);
			else return res.getDrawable(mucId);
		} catch (Exception e) { return context.getResources().getDrawable(R.drawable.muc); }
	}
}
