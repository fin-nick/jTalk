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

package net.ustyugov.jtalk.listener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.MessageItem;
import net.ustyugov.jtalk.MessageLog;
import net.ustyugov.jtalk.Notify;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.db.MessageDbHelper;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.packet.BobExtension;
import org.jivesoftware.smackx.packet.CaptchaExtension;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.packet.MultipleAddresses;
import org.jivesoftware.smackx.packet.ReplaceExtension;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

import com.jtalk2.R;

public class MsgListener implements PacketListener {
	private XMPPConnection connection;
	private String account;
	private Context context;
	private SharedPreferences prefs;
	private JTalkService service;
	
    public MsgListener(Context c, XMPPConnection connection, String account) {
    	this.context = c;
    	this.connection = connection;
    	this.account = account;
    	this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	this.service = JTalkService.getInstance();
    	return;
    }

	public void processPacket(Packet packet) {
		Message msg = (Message) packet;
		String from = msg.getFrom();
		String id = msg.getPacketID();
		String user = StringUtils.parseBareAddress(from).toLowerCase();
		String type = msg.getType().name();
		String body = msg.getBody();
		
		MultipleAddresses ma = (MultipleAddresses) msg.getExtension("addresses", "http://jabber.org/protocol/address");
		if (ma != null) {
			List<MultipleAddresses.Address> list = ma.getAddressesOfType(MultipleAddresses.OFROM);
			if (!list.isEmpty()) {
				String jid = list.get(0).getJid();
				user = StringUtils.parseBareAddress(jid);
			}
		}
		
		PacketExtension stateExt = msg.getExtension("http://jabber.org/protocol/chatstates");
		if (stateExt != null && !type.equals("error") && !service.getConferencesHash(account).containsKey(user)) {
			String state = stateExt.getElementName();
			if (state.equals(ChatState.composing.name())) {
				updateComposeList(user, true, true);
			} else {
				if (body != null && body.length() > 0) updateComposeList(user, false, false);
				else updateComposeList(user, false, true);
			}
		}
		
		PacketExtension receiptExt = msg.getExtension("urn:xmpp:receipts");
		if (receiptExt != null && !type.equals("error")) {
			String receipt = receiptExt.getElementName();
			if (receipt.equals("request")) {
				service.sendReceivedPacket(connection, user, id);
			} else if (receipt.equals("received")) {
				if (service.getMessagesHash(account).containsKey(user)) {
					List<MessageItem> l = service.getMessagesHash(account).get(user);
					for (final MessageItem i : l) {
						if (i.getId().equals(id)) i.setReceived(true);
						final String u = user;
						final String pid = id;
						new Thread() {
							public void run() {
								try {
					 	            ContentValues values = new ContentValues();
					 	            values.put(MessageDbHelper.TYPE, i.getType().name());
					 	            values.put(MessageDbHelper.JID, u);
					 	            values.put(MessageDbHelper.ID, pid);
					 	            values.put(MessageDbHelper.STAMP, i.getTime());
					 	            values.put(MessageDbHelper.NICK, i.getName());
					 	            values.put(MessageDbHelper.BODY, i.getBody());
					 	            values.put(MessageDbHelper.COLLAPSED, i.isCollapsed() ? "true" : "false");
					 	            values.put(MessageDbHelper.RECEIVED, "true");
					 	            values.put(MessageDbHelper.FORM, "NULL");
					 	            values.put(MessageDbHelper.BOB, "NULL");
					            	service.getContentResolver().update(JTalkProvider.CONTENT_URI, values, MessageDbHelper.ID + " = '" + pid + "'", null);
					            } catch (Exception sqle) { }
							}
						}.start();
					}
					Intent i = new Intent(Constants.RECEIVED);
					context.sendBroadcast(i);
				}
			}
		}
		
		if (body.length() > 0) {
	        if (type.equals("groupchat")) { // Group Chat Message
	        	Date date = new java.util.Date();
				String time = DateFormat.getTimeFormat(context).format(date);
				DelayInformation delayExt = (DelayInformation) msg.getExtension("jabber:x:delay");
				if (delayExt != null) time = delayExt.getStamp().toLocaleString();
		       
	        	String nick  = StringUtils.parseResource(from);
	        	String group = StringUtils.parseBareAddress(from);
	        	
	        	String mynick = context.getResources().getString(R.string.Me);
	        	if (service.getConferencesHash(account).containsKey(group)) mynick = service.getConferencesHash(account).get(group).getNickname();
	        	
	        	if (!service.getCurrentJid().equals(group)) {
                	service.addMessagesCount(account, group);
                }
	        	
	            if (body.contains(mynick)) {
	            	if (!service.getCurrentJid().equals(group)) service.addHighlight(account, group);
	            	if (!service.getMessagesList(account).contains(group)) service.getMessagesList(account).add(group);
	            	Notify.messageNotify(account, group, Notify.Type.Direct, body);
	            }
	            else Notify.messageNotify(account, group, Notify.Type.Conference, body);
	            
	            if (nick != null && nick.length() > 0) {
	            	MessageItem item = new MessageItem();
					item.setBody(body);
					item.setId(id);
					item.setTime(time);
					item.setReceived(false);
		            item.setName(nick);
		            
	            	MessageLog.writeMucMessage(group, nick, item);
	            	
	                if (service.getMucMessagesHash(account).containsKey(group)) {
	                   	List<MessageItem> list = service.getMucMessagesHash(account).get(group);
	                   	list.add(item);
	                   	if (list.size() > Constants.MAX_MUC_MESSAGES) list.remove(0);
	                } else {
	                	List<MessageItem> list = new ArrayList<MessageItem>();
	                	list.add(item);
	                	service.getMucMessagesHash(account).put(group, list);
	                }
	                    
	                Intent intent = new Intent(Constants.NEW_MUC_MESSAGE);
	                intent.putExtra("jid", group);
	                context.sendBroadcast(intent);
	            }
	        } else if (type.equals("chat") || type.equals("normal") || type.equals("headline")) {
	        	ReplaceExtension replace = (ReplaceExtension) msg.getExtension("urn:xmpp:message-correct:0");
	    		if (replace != null) {
	    			String rid = replace.getId();
	    			MessageLog.editMessage(account, user, rid, body);
	    		} else {
	    			String action = Constants.NEW_MESSAGE;
		        	String name = null;
		        	String group = null;
		        	
		        	// from room 
		        	if (service.getConferencesHash(account).containsKey(user)) {
		        		group = StringUtils.parseBareAddress(from);
		        		name = StringUtils.parseResource(from);
		        		action = Constants.NEW_MUC_MESSAGE;
		        		
		        		if (name == null || name.length() <= 0) {
		        			MessageItem mucMsg = new MessageItem();
		    				mucMsg.setBody(body);
		    				mucMsg.setId(id);
		    				mucMsg.setTime("");
		    	            mucMsg.setName(group);
		    	            
		    	            CaptchaExtension captcha = (CaptchaExtension) msg.getExtension("captcha", "urn:xmpp:captcha");
			            	if (captcha != null) {
			            		BobExtension bob = (BobExtension) msg.getExtension("data","urn:xmpp:bob");
			            		mucMsg.setBob(bob);
			            		mucMsg.setCaptcha(true);
			            		mucMsg.setForm(captcha.getForm());
			            		
			            		Notify.captchaNotify(account, mucMsg);
			            	}
		    	            
		        			if (service.getMucMessagesHash(account).containsKey(group)) {
		                       	List<MessageItem> list = service.getMucMessagesHash(account).get(group);
		                       	list.add(mucMsg);
		                    } else {
		                    	List<MessageItem> list = new ArrayList<MessageItem>();
		                    	list.add(mucMsg);
		                    	service.getMucMessagesHash(account).put(group, list);
		                    }

		                    if (!service.getCurrentJid().equals(group)) {
		                    	if (!service.getMessagesList(account).contains(group)) service.getMessagesList(account).add(group);
		                    }
		                        
		                    Intent intent = new Intent(Constants.NEW_MUC_MESSAGE);
		                    intent.putExtra("jid", group);
		                    context.sendBroadcast(intent);
		                    
		                    // TODO
		                    Date date = new java.util.Date();
		    	            date.setTime(Long.parseLong(System.currentTimeMillis()+""));
		    	            String time = DateFormat.getTimeFormat(context).format(date);
		    	            
		                    MessageItem item = new MessageItem();
		    				item.setBody(body);
		    				item.setId(id);
		    				item.setTime(time);
		    				item.setReceived(false);
		    	            item.setName(name);
		    	            
		    	            if (prefs.getBoolean("CollapseBigMessages", false) && body.length() > 196) {
		    	            	item.setCollapsed(true);
		    	            }
		    	            
		    	            MessageLog.writeMessage(group, item);
		                    return;
		        		}
		        	} else { // from user
		        		Roster roster = service.getRoster(account);
		        		if (roster != null) {
		        			RosterEntry entry = roster.getEntry(user);
		        			if (entry != null) name = entry.getName();
		        		}
		        	}
		        	
		            if (name == null || name.equals("")) name = user;
		            Date date = new java.util.Date();
		            date.setTime(Long.parseLong(System.currentTimeMillis()+""));
		            String time = DateFormat.getTimeFormat(context).format(date);
		            
		            DelayInformation delayExt = (DelayInformation) msg.getExtension("jabber:x:delay");
					if (delayExt != null) time = delayExt.getStamp().toLocaleString();
					
		            MessageItem item = new MessageItem();
		            item.setSubject(msg.getSubject());
					item.setBody(body);
					item.setId(id);
					item.setTime(time);
		            item.setName(name);
		            
		            if (prefs.getBoolean("CollapseBigMessages", false) && body.length() > 196) {
		            	item.setCollapsed(true);
		            }
		            
		            if (group != null && group.length() > 0) user = group + "/" + name; 
		        	
		            MessageLog.writeMessage(user, item);
		            
		            if (service.getMessagesHash(account).containsKey(user)) {
		            	List<MessageItem> list = service.getMessagesHash(account).get(user); 
		           		list.add(item);
		            } else {
		        		List<MessageItem> list = new ArrayList<MessageItem>();
		        		list.add(item);
		        		service.getMessagesHash(account).put(user, list);
		        	}
		            
		            if (!service.getCurrentJid().equals(user)) {
		            	if (!service.getMessagesList(account).contains(user)) service.getMessagesList(account).add(user);
		            	service.addMessagesCount(account, user);
		            }
		            
		            updateComposeList(user, false, false);
		            
		            Intent intent = new Intent(action);
		            intent.putExtra("jid", user);
		            context.sendBroadcast(intent);
		            
		            Notify.messageNotify(account, user, Notify.Type.Chat, body);
	    		}
	        }
		}
	}
	
	private void updateComposeList(String jid, boolean add, boolean send) {
		if (add) {
			service.getComposeList().add(jid);
		} else {
			while (service.getComposeList().contains(jid)) service.getComposeList().remove(jid);
		}
		
		if (send) {
			Intent i = new Intent(Constants.UPDATE);
			context.sendBroadcast(i);
		}
	}
}
