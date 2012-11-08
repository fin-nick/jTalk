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

import org.jivesoftware.smack.packet.Presence;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

import com.jtalk2.R;

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
	private String currentPack;
	
	public IconPicker(Context context) {
		this.context = context;
		this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
		loadIconPack();
	}
	
	public void loadIconPack() {
		String packName = prefs.getString("IconPack", context.getPackageName());
		try {
			res = context.getPackageManager().getResourcesForApplication(packName);
			currentPack = packName;
		}
		catch(Exception e) {
			res = context.getResources();
			currentPack = context.getPackageName();
		}
		
		onlineId = res.getIdentifier("icon_online", "drawable", packName);
		chatId = res.getIdentifier("icon_chat", "drawable", packName);
		awayId = res.getIdentifier("icon_away", "drawable", packName);
		xaId = res.getIdentifier("icon_xa", "drawable", packName);
		dndId = res.getIdentifier("icon_dnd", "drawable", packName);
		offlineId = res.getIdentifier("icon_offline", "drawable", packName);
		noneId = res.getIdentifier("icon_none", "drawable", packName);
		mucId = res.getIdentifier("icon_muc", "drawable", packName);
		msgId = res.getIdentifier("icon_msg", "drawable", packName);
		moderatorId = res.getIdentifier("icon_moderator", "drawable", packName);
		visitorId = res.getIdentifier("icon_visitor", "drawable", packName);
		participantId = res.getIdentifier("icon_participant", "drawable", packName);
		
		if (onlineId == 0) onlineId = R.drawable.icon_online;
		if (chatId == 0) chatId = R.drawable.icon_chat;
		if (awayId == 0) awayId = R.drawable.icon_away;
		if (xaId == 0) xaId = R.drawable.icon_xa;
		if (dndId == 0) dndId = R.drawable.icon_dnd;
		if (offlineId == 0) offlineId = R.drawable.icon_offline;
		if (noneId == 0) noneId = R.drawable.icon_none;
		if (mucId == 0) mucId = R.drawable.icon_muc;
		if (moderatorId == 0) moderatorId = R.drawable.icon_moderator;
		if (participantId == 0) participantId = R.drawable.icon_participant;
		if (visitorId == 0) visitorId = R.drawable.icon_visitor;
		if (msgId == 0) msgId = R.drawable.icon_msg;
			
		online = BitmapFactory.decodeResource(res, onlineId);
		chat = BitmapFactory.decodeResource(res, chatId);
		away = BitmapFactory.decodeResource(res, awayId);
		xa = BitmapFactory.decodeResource(res, xaId);
		dnd = BitmapFactory.decodeResource(res, dndId);
		offline = BitmapFactory.decodeResource(res, offlineId);
		none = BitmapFactory.decodeResource(res, noneId);
		msg = BitmapFactory.decodeResource(res, msgId);
		muc = BitmapFactory.decodeResource(res, mucId);
		moderator = BitmapFactory.decodeResource(res, moderatorId);
		participant = BitmapFactory.decodeResource(res, participantId);
		visitor = BitmapFactory.decodeResource(res, visitorId);
		
		if (online == null) online = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_online);
		if (chat == null) chat = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_chat);
		if (away == null) away = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_away);
		if (xa == null) xa = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_xa);
		if (dnd == null) dnd = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_dnd);
		if (offline == null) offline = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_offline);
		if (none == null) none = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_none);
		if (msg == null) msg = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_msg);
		if (muc == null) muc = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_muc);
		if (moderator == null) moderator = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_moderator);
		if (participant == null) participant = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_participant);
		if (visitor == null) visitor = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_visitor);
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
			else return offline;
		} else return offline;
	}
	
	public Drawable getDrawableByPresence(Presence presence) {
		if (presence != null) {
			Presence.Type type = presence.getType();
			if (type == Presence.Type.available) {
				Presence.Mode mode = presence.getMode();
				if(mode == Presence.Mode.away) {
					try {
						return res.getDrawable(awayId);
					} catch (Exception e) {
						return context.getResources().getDrawable(R.drawable.icon_away);
					}
				}
				else if (mode == Presence.Mode.xa) {
					try {
						return res.getDrawable(xaId);
					} catch (Exception e) {
						return context.getResources().getDrawable(R.drawable.icon_xa);
					}
				}
				else if (mode == Presence.Mode.dnd) {
					try {
						return res.getDrawable(dndId);
					} catch (Exception e) {
						return context.getResources().getDrawable(R.drawable.icon_dnd);
					}
				}
				else if (mode == Presence.Mode.chat) {
					try {
						return res.getDrawable(chatId);
					} catch (Exception e) {
						return context.getResources().getDrawable(R.drawable.icon_chat);
					}
				}
				else {
					try {
						return res.getDrawable(onlineId);
					} catch (Exception e) {
						return context.getResources().getDrawable(R.drawable.icon_online);
					}
				}
			} else {
				try {
					return res.getDrawable(offlineId);
				} catch (Exception e) {
					return context.getResources().getDrawable(R.drawable.icon_offline);
				}
			}
		} else {
			try {
				return res.getDrawable(offlineId);
			} catch (Exception e) {
				return context.getResources().getDrawable(R.drawable.icon_offline);
			}
		}
	}
	
	public Drawable getMucDrawable() {
		try {
			return res.getDrawable(mucId);
		} catch (Exception e) { return context.getResources().getDrawable(R.drawable.icon_muc); }
	}
}
