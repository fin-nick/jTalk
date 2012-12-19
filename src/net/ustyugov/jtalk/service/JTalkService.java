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

package net.ustyugov.jtalk.service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;

import net.ustyugov.jtalk.*;
import net.ustyugov.jtalk.activity.RosterActivity;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.listener.ConListener;
import net.ustyugov.jtalk.listener.IncomingFileListener;
import net.ustyugov.jtalk.listener.InviteListener;
import net.ustyugov.jtalk.listener.LocationChangedListener;
import net.ustyugov.jtalk.listener.MsgListener;
import net.ustyugov.jtalk.listener.MucParticipantStatusListener;
import net.ustyugov.jtalk.listener.RstListener;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.PrivacyListManager;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.OfflineMessageManager;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.Receipt;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.bookmark.BookmarkManager;
import org.jivesoftware.smackx.bookmark.BookmarkedConference;
import org.jivesoftware.smackx.commands.AdHocCommandManager;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.BobExtension;
import org.jivesoftware.smackx.packet.CapsExtension;
import org.jivesoftware.smackx.packet.CaptchaExtension;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.MultipleAddresses;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.ReceiptExtension;
import org.jivesoftware.smackx.packet.ReplaceExtension;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.BytestreamsProvider;
import org.jivesoftware.smackx.provider.CapsExtensionProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.IBBProviders;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.VersionProvider;
import org.jivesoftware.smackx.search.UserSearch;
import org.xbill.DNS.Credibility;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;
import android.util.Log;

import com.jtalk2.R;
 
public class JTalkService extends Service {
	private static JTalkService js = new JTalkService();
    private static List<String> collapsedGroups = new ArrayList<String>();
    private static List<String> composeList = new ArrayList<String>();
    private static List<MessageItem> unreadMessages = new ArrayList<MessageItem>();
    private static Hashtable<String, List<String>> mucHighlightsList = new Hashtable<String, List<String>>();
    private static Hashtable<String, String> textHash = new Hashtable<String, String>();
    private static Hashtable<String, String> stateHash = new Hashtable<String, String>();
    private static Hashtable<String, Hashtable<String, String>> resourceHash = new Hashtable<String, Hashtable<String, String>>();
    private static Hashtable<String, Integer> positionHash = new Hashtable<String, Integer>();
    private static Hashtable<String, Conference> joinedConferences = new Hashtable<String, Conference>();
    private static Hashtable<String, Hashtable<String, MultiUserChat>> conferences = new Hashtable<String, Hashtable<String, MultiUserChat>>();
    private static Hashtable<String, Bitmap> avatarsHash = new Hashtable<String, Bitmap>();
    private static Hashtable<String, Hashtable<String, List<MessageItem>>> messages = new Hashtable<String, Hashtable<String, List<MessageItem>>>();
    private static Hashtable<String, Hashtable<String, List<MessageItem>>> mucMessages = new Hashtable<String, Hashtable<String, List<MessageItem>>>();
    private static Hashtable<String, Hashtable<String, Integer>> messagesCount = new Hashtable<String, Hashtable<String, Integer>>();
    private static Hashtable<String, DataForm> formHash = new Hashtable<String, DataForm>(); 
    private static Hashtable<String, XMPPConnection> connections = new Hashtable<String, XMPPConnection>();
    private static Hashtable<String, VCard> vcards = new Hashtable<String, VCard>();
    private static Hashtable<String, ConListener> conListeners = new Hashtable<String, ConListener>();
    private Hashtable<String, ConnectionTask> connectionTasks = new Hashtable<String, ConnectionTask>();
    private String currentJid = "me";
    private String sidebarMode = "users";
    private String globalState = "";
    private SharedPreferences prefs;
    private BroadcastReceiver updateReceiver;
    private ChangeConnectionReceiver connectionReceiver;
    private FileTransferManager fileTransferManager;
    private List<FileTransferRequest> incomingRequests = new ArrayList<FileTransferRequest>();
    private Timer autoStatusTimer = new Timer();
    private boolean autoStatus = false;
    private Presence oldPresence;

    private WifiManager.WifiLock wifiLock;
    
    private LocationManager locationManager;
    private LocationChangedListener locationListener = new LocationChangedListener();

    private IconPicker iconPicker;

    public static JTalkService getInstance() { return js; }
    
    private void removeConnectionListener(String account) {
    	if (conListeners.containsKey(account)) {
    		ConListener listener = conListeners.remove(account);
    		XMPPConnection connection = getConnection(account);
    		if (connection != null) connection.removeConnectionListener(listener);
    	}
    }
    
    private void addConnectionListener(String account, XMPPConnection connection) {
    	ConListener listener = new ConListener(this, account);
    	connection.addConnectionListener(listener);
    	conListeners.put(account, listener);
    }
    
    public Collection<XMPPConnection> getAllConnections() {
    	return connections.values();
    }

    public void setState(String account, String state) {
        if (state == null) state = "null";
        if (account != null) stateHash.put(account, state);
    }

    public String getState(String account) {
        if (stateHash.containsKey(account)) return stateHash.get(account);
        else {
            if (isAuthenticated(account)) {
                return getStatus(account, account);
            } else return getString(R.string.Disconnect);
        }
    }
    
    public XMPPConnection getConnection(String account) {
    	if (connections.containsKey(account)) return connections.get(account);
    	else return null;
    }
    
    public int getMessagesCount() {
    	int result = 0;
    	for (Hashtable<String, Integer> hash : messagesCount.values()) {
    		for (Integer i : hash.values()) {
    			result = result + i;
    		}
    	}
    	return result;
    }
    
    public int getMessagesCount(String account, String jid) {
    	if (messagesCount.containsKey(account)) {
    		Hashtable<String, Integer> hash = messagesCount.get(account);
    		if (hash.containsKey(jid)) return hash.get(jid);
    	}
    	return 0;
    }
    
    public void addMessagesCount(String account, String jid) {
    	Hashtable<String, Integer> hash = new Hashtable<String, Integer>();
    	if (messagesCount.containsKey(account)) {
    		hash = messagesCount.get(account);
    		hash.put(jid, getMessagesCount(account, jid) + 1);
    	} else {
    		hash.put(jid, 1);
    	}
    	messagesCount.put(account, hash);
//    	updateWidget();
    }
    
    public void removeMessagesCount(String account, String jid) {
    	if (messagesCount.containsKey(account)) {
    		Hashtable<String, Integer> hash = messagesCount.get(account);
    		if (hash.containsKey(jid)) hash.remove(jid);
    	}
//    	updateWidget();
    }
    
    public void addDataForm(String id, DataForm form) {
    	formHash.put(id, form);
    }
    
    public DataForm getDataForm(String id) {
    	if (formHash.containsKey(id)) return formHash.remove(id);
    	else return null;
    }
    
    public void addLastPosition(String jid, int position) {
    	positionHash.put(jid, position);
    }
    
    public int getLastPosition(String jid) {
    	if (positionHash.containsKey(jid)) return positionHash.remove(jid);
    	else return -1;
    }
    
    public IconPicker getIconPicker() { return iconPicker; }
    public String getSidebarMode() { return sidebarMode; }
    public void setSidebarMode(String mode) { this.sidebarMode = mode; }
    public void setAutoStatus(boolean auto) { this.autoStatus = auto; }
    public boolean getAutoStatus() { return autoStatus; }
    public void setOldPresence(Presence presence) { this.oldPresence = presence; }
    public FileTransferManager getFileTransferManager() { return fileTransferManager; }
    public List<FileTransferRequest> getIncomingRequests() { return incomingRequests; }
    public void setCurrentJid(String jid) { this.currentJid = jid; }
    public String getCurrentJid() { return currentJid; }
    public String getGlobalState() { return globalState; }
    public void setGlobalState(String s) { globalState = s; }
    public Roster getRoster(String account) {
    	if (connections.containsKey(account)) return connections.get(account).getRoster();
    	else return null;
    }
    public List<String> getCollapsedGroups() { return collapsedGroups; }
    public List<String> getComposeList() { return composeList; }
    
    public Hashtable<String, Hashtable<String, MultiUserChat>> getConferences() {return conferences;}
    public Hashtable<String, MultiUserChat> getConferencesHash(String account) { 
    	if (conferences.containsKey(account)) return conferences.get(account); 
    	else {
    		conferences.put(account, new Hashtable<String, MultiUserChat>());
    		return conferences.get(account);
    	}
    }
    public Hashtable<String, Conference> getJoinedConferences() { return joinedConferences; }
    public Hashtable<String, Bitmap> getAvatarsHash() { return avatarsHash; }

    public void addUnreadMessage(MessageItem item) {
        String account = item.getAccount();
        String jid = item.getJid();
        for (MessageItem i : unreadMessages) {
            if (i.getAccount().equals(account) && i.getJid().equals(jid)) return;
        }
        unreadMessages.add(item);
    }

    public MessageItem getUnreadMessage() {
        if (!unreadMessages.isEmpty()) return unreadMessages.remove(0);
        else return null;
    }

    public void removeUnreadMesage(String account, String jid) {
        for (MessageItem i : unreadMessages) {
            String a = i.getAccount();
            String j = i.getJid();
            if (!getConferencesHash(account).containsKey(j) && !getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid)))
                j = StringUtils.parseBareAddress(j);
            if (a.equals(account) && j.equals(jid)) {
                unreadMessages.remove(i);
                return;
            }
        }
    }

    public List<MessageItem> getUnreadMessages() {
        return unreadMessages;
    }
    
    public boolean isHighlight(String account, String jid) { 
    	if (!mucHighlightsList.containsKey(account)) return false;
    	List<String> list = mucHighlightsList.get(account);
        return list.contains(jid);
    }
    
    public void removeHighlight(String account, String jid) { 
    	if (!mucHighlightsList.containsKey(account)) return;
    	List<String> list = mucHighlightsList.get(account);
    	if (list.contains(jid)) list.remove(jid);
    	mucHighlightsList.put(account, list);
    }
    
    public void addHighlight(String account, String jid) {
    	List<String> list = new ArrayList<String>();
    	if (mucHighlightsList.containsKey(account)) {
    		list = mucHighlightsList.get(account);
    		if (!list.contains(jid)) list.add(jid);
    	} else {
    		list.add(jid);
    	}
    	mucHighlightsList.put(account, list);
    }
    
    public Hashtable<String, Hashtable<String, List<MessageItem>>> getMessages() { return messages; }
    public Hashtable<String, List<MessageItem>> getMessagesHash(String account) { 
    	if (messages.containsKey(account)) return messages.get(account);
    	else {
    		messages.put(account, new Hashtable<String, List<MessageItem>>());
    		return messages.get(account);
    	}
    }

    public List<String> getPrivateMessages(String account) {
        List<String> list = new ArrayList<String>();
        Enumeration<String> chatEnum = getMessagesHash(account).keys();
        while (chatEnum.hasMoreElements()) {
            String jid = chatEnum.nextElement();
            if (getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid))) {
                list.add(jid);
            }
        }
        return list;
    }
    
    public Hashtable<String, List<MessageItem>> getMucMessagesHash(String account) { 
    	if (mucMessages.containsKey(account)) return mucMessages.get(account);
    	else {
    		mucMessages.put(account, new Hashtable<String, List<MessageItem>>());
    		return mucMessages.get(account);
    	}
    }
    
    public void setResource(String account, String jid, String resource) {
    	Hashtable<String,String> hash = new Hashtable<String, String>();
    	hash.put(jid, resource);
    	resourceHash.put(account, hash);
    }
    public String getResource(String account, String jid) {
    	if (resourceHash.containsKey(account)) {
    		Hashtable<String,String> hash = resourceHash.get(account);
    		if (hash.containsKey(jid)) return hash.get(jid);
    	}
    	return "";
    }
    public void setText(String jid, String text) { textHash.put(jid, text); }
    public String getText(String jid) {
    	if (textHash.containsKey(jid)) return textHash.get(jid);
    	else return "";
    }
    public VCard getVCard(String account) { 
    	if (vcards.containsKey(account)) return vcards.get(account);
    	else return null; 
    }
    
    public void setVCard(final String account, VCard vcard) { 
    	if (vcard != null) {
    		vcards.put(account, vcard);
        	final byte[] buffer = vcard.getAvatar();
    		if (buffer != null) {
    			new Thread() {
    				public void run() {
    					try {
    						File f = new File(Constants.PATH);
    						f.mkdirs();
    						FileOutputStream fos = new FileOutputStream(Constants.PATH + "/" + account);
    						fos.write(buffer);
    						fos.close();
    					} catch (Throwable ignored) { }
    				}
    			}.start();
    		}
    		Intent intent = new Intent(Constants.UPDATE);
    		sendBroadcast(intent);
    	}
    }
    
    public Presence getPresence(String account, String user) {
    	Presence unavailable = new Presence(Presence.Type.unavailable);
    	if (connections.containsKey(account)) {
        	if (!connections.get(account).isAuthenticated()) return unavailable;
        	
    		if (StringUtils.parseResource(user).length() > 0) {
    			String bareJid = StringUtils.parseBareAddress(user);
    			if (getConferencesHash(account).containsKey(bareJid)) {
    				Presence p = getConferencesHash(account).get(bareJid).getOccupantPresence(user);
    				if (p != null) return p;
    				else return unavailable;
    			} else {
    				Presence p = getRoster(account).getPresenceResource(user);
    				if (p != null) return p;
    				else return unavailable;
    			}
    		} else {
    	    	Iterator<Presence> it = getRoster(account).getPresences(user);
    	    	if(it.hasNext()) {
    	    		return it.next();
    	    	} else {
    	    		return unavailable;
    	    	}
    		}
    	}
		return unavailable;
    }
    
    public Presence.Type getType(String account, String user) {
    	if (connections.containsKey(account)) {
    		if (StringUtils.parseResource(user).length() > 0) {
    			String g = StringUtils.parseBareAddress(user);
    			if (getConferencesHash(account).containsKey(g)) {
    				Presence p = getConferencesHash(account).get(g).getOccupantPresence(user);
    				if (p != null) return p.getType();
    				else return Presence.Type.unavailable;
    			} else {
    				Presence p = getRoster(account).getPresenceResource(user);
    				if (p != null) return p.getType();
    				else return Presence.Type.unavailable;
    			}
    		} else {
    	    	Iterator<Presence> it = getRoster(account).getPresences(user);
    	    	if(it.hasNext()) {
    	    		return it.next().getType();
    	    	} else {
    	    		return Presence.Type.unavailable;
    	    	}
    		}
    	}
		return Presence.Type.unavailable;
    }
    
    public Presence.Mode getMode(String account, String user) {
    	if (connections.containsKey(account)) {
    		if (StringUtils.parseResource(user).length() > 0) {
    			String g = StringUtils.parseBareAddress(user);
    			if (getConferencesHash(account).containsKey(g)) {
    				Presence p = getConferencesHash(account).get(g).getOccupantPresence(user);
    				if (p != null) {
    					Presence.Mode m = p.getMode();
    					if (m == null) return Presence.Mode.available;
    					else return m;
    				}
    				else return Presence.Mode.available;
    			} else {
    				Presence p = getRoster(account).getPresenceResource(user);
    				if (p != null) {
    					Presence.Mode m = p.getMode();
    					if (m == null) return Presence.Mode.available;
    					else return m;
    				}
    				else return Presence.Mode.available;
    			}
    		} 
        	
        	Iterator<Presence> it = getRoster(account).getPresences(user);
        	if(it.hasNext()) {
        		Presence presence = it.next();
        		if (presence.getType() != Presence.Type.unavailable) return presence.getMode();
        		else return Presence.Mode.available;
        	}
    	}
    	return Presence.Mode.available;
    }
    
    public String getStatus(String account, String user) {
    	if (connections.containsKey(account)) {
    		if (StringUtils.parseResource(user).length() > 0) {
    			String g = StringUtils.parseBareAddress(user);
    			if (getConferencesHash(account).containsKey(g)) {
    				Presence p = getConferencesHash(account).get(g).getOccupantPresence(user);
    				if (p != null) {
    					String s = p.getStatus();
    					if (s == null) return "";
    					else return s;
    				}
    				else return "";
    			} else {
                    Roster roster = getRoster(account);
                    if (roster != null) {
                        Presence p = getRoster(account).getPresenceResource(user);
                        if (p != null) {
                            String s = p.getStatus();
                            if (s == null) return "";
                            else return s;
                        }
                        else return "";
                    } else return "";
    			}
    		}
        	
        	Roster roster = getRoster(account);
        	if (roster != null) {
        		Iterator<Presence> it = roster.getPresences(user);
            	while(it.hasNext()) {
            		Presence presence = it.next();
            		if (presence.getType() != Presence.Type.unavailable)
            			if (presence.getStatus() == null) return "";
            			else return presence.getStatus();
            	}
        	}
    	}
    	return "";
    }
    
    public String getNode(String account, String user) {
    	if (connections.containsKey(account)) {
    		if (StringUtils.parseResource(user).length() > 0) {
    			String g = StringUtils.parseBareAddress(user);
    			if (getConferencesHash(account).containsKey(g)) {
    				Presence p = getConferencesHash(account).get(g).getOccupantPresence(user);
    				if (p != null) {
    					CapsExtension caps = (CapsExtension) p.getExtension(CapsExtension.NODE_NAME, CapsExtension.XMLNS);
    					if (caps != null) return caps.getNode();
    					else return null;
    				} else return null;
    			} else {
    				Presence p = getRoster(account).getPresenceResource(user);
    				if (p != null) {
    					CapsExtension caps = (CapsExtension) p.getExtension(CapsExtension.NODE_NAME, CapsExtension.XMLNS);
    					if (caps != null) return caps.getNode();
    					else return null;
    				}
    			}
    		} 
        	
        	List<String> list = new ArrayList<String>();
        	Iterator<Presence> it = getRoster(account).getPresences(user);
        	while(it.hasNext()) {
        		Presence presence = it.next();
        		if (presence.getType() != Presence.Type.unavailable) {
        			CapsExtension caps = (CapsExtension) presence.getExtension(CapsExtension.NODE_NAME, CapsExtension.XMLNS);
    				if (caps != null) list.add(caps.getNode());
        		}
        	}
        	
        	if (!list.isEmpty()) return list.get(0);
        	else return null;
    	}
    	return null;
    }
    
    public LocationChangedListener getLocationListener() { return locationListener; }
    
    public void resetTimer() {
    	if (prefs != null) {
    		autoStatusTimer.purge();
            autoStatusTimer.cancel();
            if (autoStatus) {
            	autoStatus = false;
            	
            	Enumeration<String> e = connections.keys();
            	while(e.hasMoreElements()) {
            		sendPresence(e.nextElement(), oldPresence.getStatus(), oldPresence.getMode().name(), oldPresence.getPriority());
            	}
            }
            autoStatusTimer = new Timer();
            int delayAway = 10;
            int delayXa = 20;
            try {
            	delayAway = Integer.parseInt(prefs.getString("AutoStatusAway", "10"));
                delayXa = Integer.parseInt(prefs.getString("AutoStatusXa", "20"));
            } catch(NumberFormatException ignored) { }
            if (delayAway < 1) delayAway = 1;
            if (delayXa < delayAway) delayXa = delayAway + 1;
            autoStatusTimer.schedule(new AutoAwayStatus(), delayAway * 60000);
            autoStatusTimer.schedule(new AutoXaStatus(), delayXa * 60000);
    	}
    }
    
//    public void updateWidget() {
//		int	count = 0;
//		
//		String[] statusArray = getResources().getStringArray(R.array.statusArray);	
//		String status = getString(R.string.NotConnected);
//		if (isAuthenticated()) {
//			status = statusArray[prefs.getInt("currentSelection", 0)];
//			count = getMessagesCount();
//		}
//		
//		ContentValues values = new ContentValues();
//        values.put(WidgetDbHelper.MODE, status);
//        values.put(WidgetDbHelper.COUNTER, count + "");
//        getContentResolver().update(JTalkProvider.WIDGET_URI, values, null, null);
//        
//        sendBroadcast(new Intent(Constants.WIDGET_UPDATE));
//    }
    
    @Override
    public void onCreate() {
    	configure();
    	js = this;
    	prefs = PreferenceManager.getDefaultSharedPreferences(this);
        iconPicker = new IconPicker(this);
        
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        updateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
//				updateWidget();
			}
        };
        registerReceiver(updateReceiver, new IntentFilter(Constants.UPDATE));
        
        connectionReceiver = new ChangeConnectionReceiver();
        registerReceiver(connectionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        
        Intent i = new Intent(this, RosterActivity.class);
   		i.setAction(Intent.ACTION_MAIN);
   		i.addCategory(Intent.CATEGORY_LAUNCHER);
   		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
   		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);
   		
   		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.stat_offline);
        mBuilder.setContentTitle(getString(R.string.app_name));
        mBuilder.setContentIntent(contentIntent);
    		
		startForeground(1, mBuilder.build());

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiLock = wifiManager.createWifiLock("jTalk");

        Cursor cursor = getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '" + 1 + "'", null, null);
        if (cursor != null && cursor.getCount() > 0) {
            connect();
        }
    }

    @Override
	public IBinder onBind(Intent intent) {
	    return null;
	}

    @Override
    public void onDestroy() {
    	stopForeground(true);

    	try {
    		unregisterReceiver(updateReceiver);
    		unregisterReceiver(connectionReceiver);
    	} catch(Exception ignored) { }
        
        Notify.cancelAll(this);
        clearAll();
//        updateWidget();
    }

    public void disconnect(final boolean exit) {
    	if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();
    	if (locationManager != null && locationListener != null) locationManager.removeUpdates(locationListener);
    	leaveAllRooms();
    	
    	Collection<XMPPConnection> con = getAllConnections();
		for (XMPPConnection connection: con) {
			String account = StringUtils.parseBareAddress(connection.getUser());
	    	if (isAuthenticated(account)) {
                if (connectionTasks.containsKey(account)) {
                    connectionTasks.remove(account).cancel(true);
                }

	    		removeConnectionListener(account);
				Presence presence = new Presence(Presence.Type.unavailable, "", 0, null);
				connection.disconnect(presence);
	    	} else if (connection.isConnected()) connection.disconnect();
            setState(account, getString(R.string.Disconnect));
            connections.remove(connection);
	    	if (exit) stopSelf();
		}
    }
    
    public void disconnect(String account) {
    	if (connections.containsKey(account)) {
            if (connectionTasks.containsKey(account)) {
                connectionTasks.remove(account).cancel(true);
            }

    		XMPPConnection connection = connections.remove(account);
    		removeConnectionListener(account);
    		connection.disconnect();
    	}
    	sendBroadcast(new Intent(Constants.UPDATE));
    }
    
    public void reconnect() {
    	globalState = getResources().getString(R.string.Reconnecting) + "...";
    	Intent i = new Intent(Constants.CONNECTION_STATE);
    	sendBroadcast(i);
    	new Thread() {
    		@Override
    		public void run() {
    			disconnect(false);
    			connect();
    		}
    	}.start();
    }
    
    public void reconnect(final String account) {
        setState(account, getResources().getString(R.string.Reconnecting) + "...");
    	Intent i = new Intent(Constants.CONNECTION_STATE);
    	sendBroadcast(i);
    	new Thread() {
    		@Override
    		public void run() {
    			disconnect(account);
    			connect(account);
    		}
    	}.start();
    }
    
    public void connect() {
    	if (prefs.getBoolean("WifiLock", false)) wifiLock.acquire();
    	
//		String text  = prefs.getString("currentStatus", "");
		String mode  = prefs.getString("currentMode", "available");
		
		if (mode.equals("online")) {
			mode = "available";
			setPreference(this, "currentSelection", 0);
		}
		
		if (!mode.equals("unavailable")) {
//			globalState = getString(R.string.Connecting) + "...";
	    	Intent i = new Intent(Constants.CONNECTION_STATE);
	    	sendBroadcast(i);
	    	
	    	Cursor cursor = getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '" + 1 + "'", null, null);
			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();
				do {
					String username = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID)).trim();
					String password = cursor.getString(cursor.getColumnIndex(AccountDbHelper.PASS)).trim();
					String resource = cursor.getString(cursor.getColumnIndex(AccountDbHelper.RESOURCE)).trim();
					String service = cursor.getString(cursor.getColumnIndex(AccountDbHelper.SERVER));
					String tls = cursor.getString(cursor.getColumnIndex(AccountDbHelper.TLS));
					String port = cursor.getString(cursor.getColumnIndex(AccountDbHelper.PORT));

                    ConnectionTask task = new ConnectionTask();
                    if (connectionTasks.containsKey(username)) task = connectionTasks.get(username);
                    if (task.getStatus() != AsyncTask.Status.RUNNING) {
                        task.execute(username, password, resource, service, tls, port);
                        connectionTasks.put(username, task);
                    }
				} while(cursor.moveToNext());
				cursor.close();
			}
		} else {
//			globalState = text;
			Intent i = new Intent(Constants.CONNECTION_STATE);
	    	sendBroadcast(i);
		}
    }
    
    public void connect(String account) {
    	Cursor cursor = getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.JID + " = '" + account + "'", null, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			
			String username = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID)).trim();
			String password = cursor.getString(cursor.getColumnIndex(AccountDbHelper.PASS)).trim();
			String resource = cursor.getString(cursor.getColumnIndex(AccountDbHelper.RESOURCE)).trim();
			String service = cursor.getString(cursor.getColumnIndex(AccountDbHelper.SERVER));
			String tls = cursor.getString(cursor.getColumnIndex(AccountDbHelper.TLS));
			String port = cursor.getString(cursor.getColumnIndex(AccountDbHelper.PORT));

            ConnectionTask task = new ConnectionTask();
            task.execute(username, password, resource, service, tls, port);
            connectionTasks.put(username, task);
			cursor.close();
		}
    }

	public void joinRoom(final String account, final String group, final String nick, final String password) {	
		if (connections.containsKey(account)) {
  			final XMPPConnection connection = connections.get(account);
  			
  			new Thread() {
  				@Override
  				public void run() {
  					if (getConferencesHash(account).containsKey(group)) getConferencesHash(account).remove(group);
  					
  					MultiUserChat muc = new MultiUserChat(connection, group);
  					getConferencesHash(account).put(group, muc);
  					
  					Presence presence = new Presence(Presence.Type.available);
  					presence.setStatus(prefs.getString("currentStatus", ""));
  					presence.setMode(Presence.Mode.valueOf(prefs.getString("currentMode", "available")));
  					
  					try { 
  						DiscussionHistory h = new DiscussionHistory();
  						h.setMaxStanzas(10);
  						muc.addParticipantStatusListener(new MucParticipantStatusListener(account, group));
  						muc.addParticipantListener(new PacketListener() {
  							@Override
  							public void processPacket(Packet packet) {
  								Presence p = (Presence) packet;
  						        if (p.isAvailable()) sendBroadcast(new Intent(Constants.PRESENCE_CHANGED));
  							}
  						});
  						muc.join(nick, password, h, 5000, presence);
  					} catch (Exception e) {
  				  		Intent eIntent = new Intent(Constants.ERROR);
  				  		eIntent.putExtra("error", "Error: " + e.getLocalizedMessage());
  				  		sendBroadcast(eIntent);
  				  		return;
  				  	}
  				  		
  					if (muc.isJoined()) {
  						Conference conf = new Conference(group, nick, password);
  						if (!joinedConferences.contains(conf)) joinedConferences.put(group, conf);
  						Intent updateIntent = new Intent(Constants.PRESENCE_CHANGED);
  						updateIntent.putExtra("join", true);
  						updateIntent.putExtra("group", group);
  						sendBroadcast(updateIntent);
  						
  						if (prefs.getBoolean("LoadAllAvatars", false)) {
  							Avatars.loadAllAvatars(connection, group);
  						}
  					}
  				}
  			}.start();
		}
    }
	
	public void leaveRoom(String account, String group) {
		if (getConferencesHash(account).containsKey(group)) {
			try {
				MultiUserChat muc = getConferencesHash(account).get(group);
				if (muc.isJoined()) muc.leave();
			} catch (IllegalStateException ignored) { }
			getConferencesHash(account).remove(group);
	    }
	    if (mucMessages.containsKey(account)) mucMessages.get(account).remove(group);
	    if (joinedConferences.containsKey(group)) joinedConferences.remove(group);
	    Intent updateIntent = new Intent(Constants.PRESENCE_CHANGED);
		sendBroadcast(updateIntent);
	}
	
	public void leaveAllRooms() {
		for (Hashtable<String, MultiUserChat> hash : conferences.values()) {
			if (!hash.isEmpty()) {
				Collection<MultiUserChat> coll = hash.values();
				for(MultiUserChat muc : coll) {
					try {
						if (muc.isJoined()) muc.leave();
					} catch (IllegalStateException ignored) { }
				}
				mucMessages.clear();
//				joinedConferences.clear();
//				conferencesHash.clear();
			}
		}
	}
	
    public void addContact(String account, String jid, String name, String group) {
	    try {
		    final String[] groups = { group };
		    getRoster(account).createEntry(jid, name, groups);
	    } catch (XMPPException ignored) {    }
    }
  
    public void removeContact(String account, String jid) {
    	try {
    		Roster roster = getRoster(account);
    		if (roster != null) {
    			RosterEntry entry = roster.getEntry(jid);
        		roster.removeEntry(entry);
    		}
    		
//    		getContentResolver().delete(JTalkProvider.CONTENT_URI, "jid = '" + jid + "'", null);
//	    		if (getMessagesHash(account).containsKey(jid)) {
//	    			getMessagesHash(account).remove(jid);
//	    		}
    	} catch (XMPPException ignored) {  }
    }

    public boolean isAuthenticated() {
    	for(XMPPConnection connection : connections.values()) {
    		if (connection.isAuthenticated()) return true;
    	}
    	return false;
    }
    
  	public boolean isAuthenticated(String account) {
  		if (account != null && connections.containsKey(account)) {
  			XMPPConnection connection = connections.get(account);
              return connection.getUser() != null && connection.isAuthenticated();
  		} else return false;
  	}

  	public void sendMessage(String account, String user, String message) {
  		String resource = StringUtils.parseResource(user);
  		if (resource.length() > 0) sendMessage(account, StringUtils.parseBareAddress(user), message, resource);
  		else sendMessage(account, user, message, null);
  		
  	}
  	
  	private void sendMessage(String account, String user, String message, String resource) {
  		if (connections.containsKey(account)) {
  			final XMPPConnection connection = connections.get(account);
  			
  			String mil = System.currentTimeMillis()+"";
  	  		
  	  		final Message msg;
  	  		if (resource != null && resource.length() > 0) {
                    msg = new Message(user + "/" + resource, Message.Type.chat);
                    if (getConferencesHash(account).containsKey(user)) user = user + "/" + resource;
                }
  	  		else msg = new Message(user, Message.Type.chat);
  	  		msg.setPacketID(mil);
  	  		msg.setBody(message);
  	  		
  	  		ReceiptExtension receipt = new ReceiptExtension(Receipt.request);
  	  		msg.addExtension(receipt);
  	  		
  	  		Date date = new java.util.Date();
  	        date.setTime(Long.parseLong(mil));
  	        String time = DateFormat.getTimeFormat(this).format(date);
  	        
  	  		MessageItem msgItem = new MessageItem(account, user);
  	  		msgItem.setTime(time);
  	  		msgItem.setName(getResources().getString(R.string.Me));
  	  		msgItem.setId(mil);
  	  		msgItem.setBody(message);
  	  		msgItem.setReceived(false);
  	  		
  	  		MessageLog.writeMessage(user, msgItem);
  	  		
  	  		if (getMessagesHash(account).containsKey(user)) {
  				List<MessageItem> list = getMessagesHash(account).get(user);
  				list.add(msgItem);
  			} else {
  				List<MessageItem> list = new ArrayList<MessageItem>();
  				list.add(msgItem);
  				getMessagesHash(account).put(user, list);
  			}
  	  		
  	  		new Thread() {
  	  			@Override
  	  			public void run() {
  	  				if(connection != null && connection.getUser() != null) {
  	  					connection.sendPacket(msg);
  	  				}
  	  			}
  	  		}.start();
  		}
  	}
  	
  	public void editMessage(String account, final String user, final String id, final String message) {
  		if (connections.containsKey(account)) {
  			XMPPConnection connection = connections.get(account);
  			String mil = System.currentTimeMillis()+"";
  	  		
  	  		final Message msg = new Message(user, Message.Type.chat);
  	  		msg.setPacketID(mil);
  	  		msg.setBody(message);
  	  		
  	  		ReplaceExtension replace = new ReplaceExtension(id);
  	  		msg.addExtension(replace);
  	  		
  	  		if(connection != null && connection.getUser() != null) {
  					connection.sendPacket(msg);
  					MessageLog.editMessage(account, user, id, message);
  			}
  		}
  	}
  	
  	public void sendPacket(String from, final Packet packet) {
  		if (connections.containsKey(from)) {
  			final XMPPConnection connection = connections.get(from);
  			
  			new Thread() {
  	  			@Override
  	  			public void run() {
  	  				if(connection != null && connection.getUser() != null) {
  	  					connection.sendPacket(packet);
  	  				}
  	  			}
  	  		}.start();
  		}
  	}
  	
  	public void sendReceivedPacket(final XMPPConnection connection, String user, String id) {
  		ReceiptExtension extension = new ReceiptExtension(Receipt.received);
	  		final Message msg = new Message(user);
	  		msg.setPacketID(id);
	  		msg.addExtension(extension);
	  		new Thread() {
	  			@Override
	  			public void run() {
	  				if(connection != null && connection.getUser() != null) {
	  					connection.sendPacket(msg);
	  				}
	  			}
	  		}.start();
  	}

  	public void setChatState(String account, String user, ChatState state) {
  		if (connections.containsKey(account)) {
  			final XMPPConnection connection = connections.get(account);
  			
  			if (user != null && getType(account, user) != Presence.Type.unavailable) {
  	  			ChatStateExtension extension = new ChatStateExtension(state);
  	  	  		final Message msg = new Message(user, Message.Type.chat);
  	  	        msg.addExtension(extension);
  	  	        new Thread() {
  	  	  			@Override
  	  	  			public void run() {
  	  	  				if(connection != null && connection.getUser() != null) {
  	  	  					connection.sendPacket(msg);
  	  	  				}
  	  	  			}
  	  	  		}.start();
  	  		}
  		}
  	}
  	
  	// Global status
  	public void sendPresence(final String state, final String mode, final int priority) {
  		for (final XMPPConnection connection : connections.values()) {
  			new Thread() {
  	  			public void run() {
//  	  				setPreference(JTalkService.this, "currentPriority", priority);
//  	       			setPreference(JTalkService.this, "currentMode", mode);
//  	       			setPreference(JTalkService.this, "currentStatus", state);
//  					setPreference(JTalkService.this, "currentSelection", getPosition(mode));
//  					setPreference(JTalkService.this, "lastStatus"+mode, state);
  						
  	  				if (connection.isAuthenticated()) {
  	  					String account = StringUtils.parseBareAddress(connection.getUser());
  	  					if (!mode.equals("unavailable")) {
  	  						Presence presence = new Presence(Presence.Type.available);
  	  	  					if (state != null) presence.setStatus(state);
  	  	  					presence.setMode(Presence.Mode.valueOf(mode));
  	  	  					presence.setPriority(priority);
  	  	  					connection.sendPacket(presence);
  	  	  					
  	  	  					for (Hashtable<String, MultiUserChat> hash : conferences.values()) {
  	  	  						Enumeration<String> e = hash.keys();
  	  	  						while(e.hasMoreElements()) {
  	  	  							try {
  	  	  								MultiUserChat muc = hash.get(e.nextElement());
  	  	  								if (muc.isJoined())	muc.changeAvailabilityStatus(state, Presence.Mode.valueOf(mode));
  	  	  							} catch (IllegalStateException ignored) { }
  	  	  						}
  	  	  					}
  	  	  					Notify.updateNotify();
  	  					} else {
  	  						removeConnectionListener(account);
  	  						Presence presence = new Presence(Presence.Type.unavailable, state, priority, null);
  	  						connection.disconnect(presence);
  	  						if (!isAuthenticated()) Notify.offlineNotify(state);
  	  					}
  	  				} else {
  	  					if (mode.equals("unavailable")) {
  	  						if (!isAuthenticated()) Notify.offlineNotify(state);
  	  					}
  	  				}
  	  				
  					Intent i = new Intent(Constants.CONNECTION_STATE);
  		            sendBroadcast(i);
  		            	
  		            i = new Intent(Constants.UPDATE);
  		            sendBroadcast(i);
  	  			}
  	  		}.start();
  		}
  	}
  	
  	public void sendPresence(final String account, final String state, final String mode, final int priority) {
  		if (connections.containsKey(account)) {
  			final XMPPConnection connection = connections.get(account);
  			
  			new Thread() {
  	  			public void run() {
  	  				if (connection.isAuthenticated()) {
  	  					String account = StringUtils.parseBareAddress(connection.getUser());
  	  					if (!mode.equals("unavailable")) {
  	  						Presence presence = new Presence(Presence.Type.available);
  	  	  					if (state != null) presence.setStatus(state);
  	  	  					presence.setMode(Presence.Mode.valueOf(mode));
  	  	  					presence.setPriority(priority);
  	  	  					connection.sendPacket(presence);
  	  	  					
  	  	  					Enumeration<String> e = getConferencesHash(account).keys();
  	  	  					while(e.hasMoreElements()) {
  	  	  						try {
  		  							MultiUserChat muc = getConferencesHash(account).get(e.nextElement());
  		  							if (muc.isJoined())	muc.changeAvailabilityStatus(state, Presence.Mode.valueOf(mode));
  		  						} catch (IllegalStateException ignored) { }
  	  	  					}
  	  	  					
  	  	  					Notify.updateNotify();
  	  					} else {
  	  						removeConnectionListener(account);
  	  						Presence presence = new Presence(Presence.Type.unavailable, state, priority, null);
  	  						connection.disconnect(presence);
  	  						if (!isAuthenticated()) Notify.offlineNotify(state);
  	  					}
  	  				} else {
  	  					if (mode.equals("unavailable")) {
  	  						if (isAuthenticated()) Notify.offlineNotify(state);
  	  					}
  	  				}
                    setState(account, state);
  	  				
  					Intent i = new Intent(Constants.CONNECTION_STATE);
  		            sendBroadcast(i);
  		            	
  		            i = new Intent(Constants.UPDATE);
  		            sendBroadcast(i);
  	  			}
  	  		}.start();
  		}
  	}
  	
  	public void sendPresenceTo(String account, final String to, final String state, final String mode, final int priority) {
  		if (connections.containsKey(account)) {
  			final XMPPConnection connection = connections.get(account);
  			
  			new Thread() {
  	  			public void run() {
  	  				if (connection.getUser() != null) {
  	  					Presence presence;
  	  					
  	  					if (!mode.equals("unavailable")) {
  	  						presence = new Presence(Presence.Type.available);
  	  						presence.setMode(Presence.Mode.valueOf(mode));
  	  					}
  	  					else presence = new Presence(Presence.Type.unavailable);
  	  	  				if (state != null) presence.setStatus(state);
  	  	  				presence.setTo(to);
  	  	  				presence.setPriority(priority);
  	  	  				connection.sendPacket(presence);
  	  				}
  	  			}
  	  		}.start();
  		}
  	}
  	
  	public void sendLocation(final Location location) {
  		for (final XMPPConnection connection : connections.values()) {
  			if (connection.isAuthenticated()) {
  				IQ iq = new IQ() {
  					public String getChildElementXML() {
  						StringBuilder sb = new StringBuilder();
  						sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");
  						sb.append("<publish node='http://jabber.org/protocol/geoloc'>");
  						sb.append("<item><geoloc xmlns='http://jabber.org/protocol/geoloc'>");
  						if (location != null) {
  							final float accuracy = location.getAccuracy();
  							final double lat = location.getLatitude();
  							final double lon = location.getLongitude();
  							
  							Bundle extras = location.getExtras();
  							for (String s : extras.keySet()) {
  								Log.i("Location", s);
  							}

  							sb.append("<accuracy>").append(accuracy).append("</accuracy>");
  							sb.append("<lat>").append(lat).append("</lat>");
  							sb.append("<lon>").append(lon).append("</lon>");
  						}
  						sb.append("</geoloc></item></publish></pubsub>");
  		   				return sb.toString();
  		            }
  				};
  				iq.setPacketID(System.currentTimeMillis()+"");
  				iq.setType(IQ.Type.SET);
  				connection.sendPacket(iq);
  			}
  		}
	}

  	public int getPosition(String mode) {
  		int pos;
  		if (mode.equals("available"))
  			pos = 0;
  		else if (mode.equals("away"))
  			pos = 1;
  		else if (mode.equals("xa"))
  			pos = 2;
  		else if (mode.equals("dnd"))
  			pos = 3;
  		else if (mode.equals("chat"))
  			pos = 4;
  		else 
  			pos = 0;
  		return pos;
  	}
  	
  	public void setPreference(Context context, String name, Object value) {
  		if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(context);
  		SharedPreferences.Editor editor = prefs.edit();
  		if(value instanceof String) editor.putString(name, String.valueOf(value));
  		else if(value instanceof Integer) editor.putInt(name, Integer.parseInt(String.valueOf(value)));
  		else if(value instanceof Boolean) editor.putBoolean(name, (Boolean)value);
  		editor.commit();
  	}
  	
  	private void clearAll() {
  		autoStatusTimer.cancel();
        collapsedGroups.clear();
        composeList.clear();
        unreadMessages.clear();
        messages.clear();
        mucMessages.clear();
        conferences.clear();
        joinedConferences.clear();
        avatarsHash.clear();
        vcards.clear();
        textHash.clear();
        messagesCount.clear();
  	}
  	
  	public void configure() {
  		ProviderManager pm = ProviderManager.getInstance();
  		
  		pm.addIQProvider("query","jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());
  		pm.addIQProvider("query", "jabber:iq:version", new VersionProvider());
        
         //  Roster Exchange
         pm.addExtensionProvider("x","jabber:x:roster", new RosterExchangeProvider());
  
         //  Caps
         pm.addExtensionProvider("c", CapsExtension.XMLNS, new CapsExtensionProvider());
         
         // Messages Receipts
   		 pm.addExtensionProvider("request","urn:xmpp:receipts", new ReceiptExtension.Provider());
   		 pm.addExtensionProvider("received","urn:xmpp:receipts", new ReceiptExtension.Provider());
   		 
   		 // Last Message Correction
   		 pm.addExtensionProvider("replace", "urn:xmpp:message-correct:0", new ReplaceExtension.Provider());
   		
   		 // Captcha
   		 pm.addExtensionProvider("captcha", "urn:xmpp:captcha", new CaptchaExtension.Provider());
   		 pm.addExtensionProvider("data", "urn:xmpp:bob", new BobExtension.Provider());
   		 
         //  Chat State
         pm.addExtensionProvider("active","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
         pm.addExtensionProvider("composing","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
         pm.addExtensionProvider("paused","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
         pm.addExtensionProvider("inactive","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
         pm.addExtensionProvider("gone","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
  
         //  Group Chat Invitations
         pm.addExtensionProvider("x","jabber:x:conference", new GroupChatInvitation.Provider());
  
         //  Service Discovery # Items    
         pm.addIQProvider("query","http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());
         pm.addIQProvider("query","http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());
  
         //  Data Forms
         pm.addExtensionProvider("x","jabber:x:data", new DataFormProvider());
  
         //  MUC User
         pm.addExtensionProvider("x","http://jabber.org/protocol/muc#user", new MUCUserProvider());
  
         //  MUC Admin    
         pm.addIQProvider("query","http://jabber.org/protocol/muc#admin", new MUCAdminProvider());
  
         //  MUC Owner    
         pm.addIQProvider("query","http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());
  
         //  Delayed Delivery
         pm.addExtensionProvider("x","jabber:x:delay", new DelayInformationProvider());
  
         //  VCard
         pm.addIQProvider("vCard","vcard-temp", new VCardProvider());
  
         //  Offline Message Requests
         pm.addIQProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());
  
         //  Offline Message Indicator
         pm.addExtensionProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());
  
         //  Last Activity
         pm.addIQProvider("query","jabber:iq:last", new LastActivity.Provider());
  
         //  User Search
         pm.addIQProvider("query","jabber:iq:search", new UserSearch.Provider());
  
         //  SharedGroupsInfo
         pm.addIQProvider("sharedgroup","http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());
  
         //  JEP-33: Extended Stanza Addressing
         pm.addExtensionProvider("addresses","http://jabber.org/protocol/address", new MultipleAddresses.Provider());
  
         //   FileTransfer
         pm.addIQProvider("si","http://jabber.org/protocol/si", new StreamInitiationProvider());
         pm.addIQProvider("query","http://jabber.org/protocol/bytestreams", new BytestreamsProvider());
         pm.addIQProvider("open","http://jabber.org/protocol/ibb", new IBBProviders.Open());
         pm.addIQProvider("close","http://jabber.org/protocol/ibb", new IBBProviders.Close());
         pm.addExtensionProvider("data","http://jabber.org/protocol/ibb", new IBBProviders.Data());
  
         //  Privacy
         pm.addIQProvider("query","jabber:iq:privacy", new PrivacyProvider());
         pm.addIQProvider("command", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider());
         pm.addExtensionProvider("malformed-action", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.MalformedActionError());
         pm.addExtensionProvider("bad-locale", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadLocaleError());
         pm.addExtensionProvider("bad-payload", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadPayloadError());
         pm.addExtensionProvider("bad-sessionid", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadSessionIDError());
         pm.addExtensionProvider("session-expired", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.SessionExpiredError());
  	}

    public class ConnectionTask extends AsyncTask<String, Integer, String> {
        Intent intent = new Intent(Constants.CONNECTION_STATE);

        @Override
        protected void onPreExecute() {
            //  			state = getString(R.string.Connecting) + "...";
            //            sendBroadcast(intent);
            //            Notify.offlineNotify(getString(R.string.Connecting));
        }

        @Override
        protected String doInBackground(String... args) {
            String username = args[0];
            String password = args[1];
            String resource = args[2];
            String service = args[3];
            String tls = args[4];
            int port = 5222;
            try {
                port = Integer.parseInt(args[5]);
            } catch (NumberFormatException ignored) { }

            if (username == null || username.indexOf("@") < 1) {
                setState(username, getString(R.string.ConnectionError));
                sendBroadcast(intent);
                return null;
            } else {
                setState(username, getString(R.string.Connecting));
                sendBroadcast(new Intent(Constants.UPDATE));

                SmackConfiguration.setPacketReplyTimeout(60000);
                SmackConfiguration.setKeepAliveInterval(60000);

                String host = StringUtils.parseServer(username);
                String user = StringUtils.parseName(username);

                if (service == null || service.length() < 4) {
                    try {
                        Lookup lookup = new Lookup("_xmpp-client._tcp." + host, Type.SRV);
                        lookup.setCredibility(Credibility.ANY);
                        Record[] records = lookup.run();
                        if(lookup.getResult() == Lookup.SUCCESSFUL) {
                            if (records.length > 0) {
                                SRVRecord record = (SRVRecord) records[0];
                                service = record.getTarget().toString();
                                service = service.substring(0, service.length()-1);
                                port = record.getPort();
                            }
                        }
                    } catch(Exception ignored) { }
                }

                if (service == null || service.length() <= 3) service = host;

                ConnectionConfiguration cc = new ConnectionConfiguration(service, port, host);
                cc.setCapsNode("http://jtalk.ustyugov.net/caps");
                cc.setSelfSignedCertificateEnabled(true);
                cc.setReconnectionAllowed(false);
                cc.setRosterLoadedAtLogin(true);
                cc.setSendPresence(false);
                cc.setSASLAuthenticationEnabled(true);
                cc.setSecurityMode(tls.equals("0") ? SecurityMode.disabled : SecurityMode.enabled);

                if (service.equals("talk.google.com") || host.equals("gmail.com")) cc.setSASLAuthenticationEnabled(false);
                else if (service.equals("vkmessenger.com")) cc.setSecurityMode(SecurityMode.disabled);

                XMPPConnection connection = new XMPPConnection(cc);
                connection.setSoftName(getString(R.string.app_name));
                connection.setSoftVersion(getString(R.string.version) + " (" + getString(R.string.build) + ")");
                connection.addFeature("http://jabber.org/protocol/disco#info");
                connection.addFeature("http://jabber.org/protocol/muc");
                connection.addFeature("http://jabber.org/protocol/chatstates");
                connection.addFeature("http://jabber.org/protocol/bytestreams");
                connection.addFeature("jabber:iq:version");
                connection.addFeature("urn:xmpp:receipts");
                connection.addFeature("urn:xmpp:time");
                connection.addFeature("urn:xmpp:message-correct:0");

                try {
                    if (!connection.isConnected()) connection.connect();
                } catch (XMPPException xe) {
                    setState(username, "Error connecting to " + connection.getServiceName());
                    sendBroadcast(intent);
                    if (!isAuthenticated()) Notify.offlineNotify("");
                    return null;
                }

                if (connection != null && connection.isConnected() && !connection.isAuthenticated()) {
                    connection.addPacketListener(new MsgListener(JTalkService.this, connection, username), new PacketTypeFilter(Message.class));
                    addConnectionListener(username, connection);

                    try {
                        connection.login(user, password, resource);
                    } catch (XMPPException e) {
                        XMPPError error = e.getXMPPError();
                        if (error != null) setState(username, "[" + error.getCode() + "]: " + error.getMessage());
                        else setState(username, e.getLocalizedMessage());
                        sendBroadcast(intent);
                        if (!isAuthenticated()) Notify.offlineNotify("");
                        return null;
                    }

                    Roster roster = connection.getRoster();
                    roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
                    roster.addRosterListener(new RstListener(username));

                    connections.put(username, connection);
                    return username;
                }
            }
            return null;
        }

        @Override
        public void onPostExecute(String username) {
            if (username != null) {
                XMPPConnection connection = connections.get(username);

                int priority = prefs.getInt("currentPriority", 0);
                String status  = prefs.getString("currentStatus", "");
                String mode  = prefs.getString("currentMode", "available");
                sendPresence(username, status, mode, priority);

                new PrivacyListManager(connection);
                new ServiceDiscoveryManager(connection);
                new AdHocCommandManager(connection);
                fileTransferManager = new FileTransferManager(connection);
                fileTransferManager.addFileTransferListener(new IncomingFileListener());

                VCard vCard = new VCard();
                try {
                    vCard.load(connection, username);
                } catch (XMPPException ignored) { }
                setVCard(username, vCard);

                try {
                    MultiUserChat.addInvitationListener(connection, new InviteListener(username));
                } catch (Exception ignored) { }

                if (!getConferencesHash(username).isEmpty()) {
                    Collection<Conference> coll = joinedConferences.values();
                    for (Conference conf : coll) {
                        joinRoom(username, conf.getName(), conf.getNick(), conf.getPassword());
                    }
                } else {
                    if (prefs.getBoolean("AutoJoin", false)) {
                        try {
                            BookmarkManager bm = BookmarkManager.getBookmarkManager(connection);
                            for(BookmarkedConference bc : bm.getBookmarkedConferences()) {
                                String nick = bc.getNickname();
                                if (nick == null || nick.length() < 1) nick = StringUtils.parseName(username);
                                if (bc.isAutoJoin()) joinRoom(username, bc.getJid(), bc.getNickname(), bc.getPassword());
                            }
                        } catch (XMPPException ignored) { }
                    }
                }

                if (prefs.getBoolean("Locations", false)) {
                    try {
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, Constants.LOCATION_MIN_TIME, Constants.LOCATION_MIN_DIST, locationListener);
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.LOCATION_MIN_TIME, Constants.LOCATION_MIN_DIST, locationListener);
                    } catch (Exception e) {
                        Log.e("LOCATION", e.getLocalizedMessage());
                    }
                } else {
                    sendLocation(null);
                }

                try {
                    OfflineMessageManager omm = new OfflineMessageManager(connection);
                    omm.deleteMessages();
                } catch (Exception ignored) {	}

                if (connectionTasks.containsKey(username)) connectionTasks.remove(username);
                setState(username, status);
                Notify.updateNotify();
                new IgnoreList(connection).createIgnoreList();
                resetTimer();

                sendBroadcast(new Intent(Constants.CONNECTION_STATE));
                Intent updateIn = new Intent(Constants.UPDATE);
                sendBroadcast(updateIn);
            }
        }
    }
}
