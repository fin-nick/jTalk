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
import java.text.SimpleDateFormat;
import java.util.*;

import com.google.android.gms.location.LocationClient;
import net.ustyugov.jtalk.*;
import net.ustyugov.jtalk.activity.RosterActivity;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.listener.*;

import net.ustyugov.jtalk.receivers.ChangeConnectionReceiver;
import net.ustyugov.jtalk.receivers.ScreenStateReceiver;
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
import org.jivesoftware.smackx.packet.*;
import org.jivesoftware.smackx.provider.*;
import org.jivesoftware.smackx.search.UserSearch;
import org.xbill.DNS.Credibility;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Type;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.jtalk2.R;
 
public class JTalkService extends Service {
    private boolean started = false;
    private boolean connecting = false;
	private static JTalkService js = new JTalkService();
    private List<String> collapsedGroups = new ArrayList<String>();
    private List<String> composeList = new ArrayList<String>();
    private Hashtable<String, List<String>> activeChats = new Hashtable<String, List<String>>();
    private Hashtable<String, Integer> msgCounter = new Hashtable<String, Integer>();
    private List<MessageItem> unreadMessages = new ArrayList<MessageItem>();
    private Hashtable<String, List<String>> mucHighlightsList = new Hashtable<String, List<String>>();
    private Hashtable<String, String> textHash = new Hashtable<String, String>();
    private Hashtable<String, String> stateHash = new Hashtable<String, String>();
    private Hashtable<String, Hashtable<String, String>> resourceHash = new Hashtable<String, Hashtable<String, String>>();
    private Hashtable<String, Integer> positionHash = new Hashtable<String, Integer>();
    private Hashtable<String, Conference> joinedConferences = new Hashtable<String, Conference>();
    private Hashtable<String, Hashtable<String, MultiUserChat>> conferences = new Hashtable<String, Hashtable<String, MultiUserChat>>();
    private Hashtable<String, Bitmap> avatarsHash = new Hashtable<String, Bitmap>();
    private Hashtable<String, Hashtable<String, Integer>> messagesCount = new Hashtable<String, Hashtable<String, Integer>>();
    private Hashtable<String, DataForm> formHash = new Hashtable<String, DataForm>();
    private Hashtable<String, XMPPConnection> connections = new Hashtable<String, XMPPConnection>();
    private Hashtable<String, VCard> vcards = new Hashtable<String, VCard>();
    private Hashtable<String, ConListener> conListeners = new Hashtable<String, ConListener>();
    private Hashtable<String, ConnectionTask> connectionTasks = new Hashtable<String, ConnectionTask>();
    private Hashtable<String, LocationExtension> locations = new Hashtable<String, LocationExtension>();
    private Hashtable<String, TunesExtension> tunes = new Hashtable<String, TunesExtension>();
    private Hashtable<String, Timer> pingTimers = new Hashtable<String, Timer>();
    private String currentJid = "me";
    private String sidebarMode = "users";
    private String globalState = "";
    private SharedPreferences prefs;
//    private BroadcastReceiver updateReceiver;
    private ScreenStateReceiver screenStateReceiver;
    private ChangeConnectionReceiver connectionReceiver;
    private FileTransferManager fileTransferManager;
    private List<FileTransferRequest> incomingRequests = new ArrayList<FileTransferRequest>();
    private Timer autoStatusTimer = new Timer();
    private boolean autoStatus = false;
    private Presence oldPresence;

    private WifiManager.WifiLock wifiLock;
    
    private LocationClient locationClient;
    private Location location;

    private IconPicker iconPicker;

    private Hashtable<String, Hashtable<String, List<MessageItem>>> messages = new Hashtable<String, Hashtable<String, List<MessageItem>>>();

    public static JTalkService getInstance() { return js; }

    public void addLocation(String jid, LocationExtension geoloc) {
        if (geoloc != null) locations.put(jid, geoloc);
    }

    public LocationExtension getLocation(String jid) {
        if (locations.containsKey(jid)) return locations.get(jid);
        else return null;
    }

    public LocationClient getLocationClient() {
        return locationClient;
    }

    public void addTunes(String jid, TunesExtension tune) {
        if (tune != null) {
            tunes.put(jid, tune);
        }
    }

    public TunesExtension getTunes(String jid) {
        if (tunes.containsKey(jid)) return tunes.get(jid);
        else return null;
    }

    public List<MessageItem> getMessageList(String account, String jid) {
        Hashtable<String, List<MessageItem>> hash = new Hashtable<String, List<MessageItem>>();
        if (messages.containsKey(account)) hash = messages.get(account);

        if (!hash.containsKey(jid)) return new ArrayList<MessageItem>();
        else return hash.get(jid);
    }

    public void setMessageList(String account, String jid, List<MessageItem> list) {
        Hashtable<String, List<MessageItem>> hash = new Hashtable<String, List<MessageItem>>();
        if (messages.containsKey(account)) hash = messages.get(account);

        hash.put(jid, list);
        messages.put(account, hash);
    }
    
    private void removeConnectionListener(String account) {
    	if (conListeners.containsKey(account)) {
    		ConListener listener = conListeners.remove(account);
    		XMPPConnection connection = getConnection(account);
    		if (connection != null) connection.removeConnectionListener(listener);
    	}
    }
    
    private void addConnectionListener(String account, XMPPConnection connection) {
        if (!conListeners.containsKey(account)) {
            ConListener listener = new ConListener(this, account);
            connection.addConnectionListener(listener);
            conListeners.put(account, listener);
        }
    }

    public ConListener getConnectionListener(String account) {
        if (conListeners.containsKey(account)) return conListeners.get(account);
        else return null;
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
    	if (account != null && connections.containsKey(account)) return connections.get(account);
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

    public void removeMessagesCountForJid(String account, String jid) {
        if (messagesCount.containsKey(account)) {
            Hashtable<String, Integer> hash = messagesCount.get(account);
            Enumeration<String> keys = hash.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                if (key.startsWith(jid)) {
                    hash.remove(key);
                }
            }
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
    public Presence getOldPresence() { return oldPresence; }
    public FileTransferManager getFileTransferManager() { return fileTransferManager; }
    public List<FileTransferRequest> getIncomingRequests() { return incomingRequests; }
    public void setCurrentJid(String jid) { this.currentJid = jid; }
    public String getCurrentJid() { return currentJid; }
    public String getGlobalState() { return globalState; }
    public void setGlobalState(String s) { globalState = s; }
    public Roster getRoster(String account) {
    	if (connections != null && account!= null && connections.containsKey(account)) {
            XMPPConnection connection = connections.get(account);
            if (connection != null) return connection.getRoster();
            else return null;
        }
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
        if (!getConferencesHash(account).containsKey(jid) && !getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid)))
            jid = StringUtils.parseBareAddress(jid);

        for (MessageItem message : unreadMessages) {
            if (message.getAccount().equals(account)) {
                String j = StringUtils.parseBareAddress(message.getJid());
                if (j.equals(jid)) return;
            }
        }

        unreadMessages.add(item);
    }

    public MessageItem getUnreadMessage() {
        if (!unreadMessages.isEmpty()) return unreadMessages.remove(0);
        else return null;
    }

    public void removeUnreadMesage(String account, String jid) {
        if (!getConferencesHash(account).containsKey(jid) && !getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid)))
            jid = StringUtils.parseBareAddress(jid);

        for (MessageItem i : unreadMessages) {
            if (i.getAccount().equals(account)) {
                String j = i.getJid();
                if (j.startsWith(jid)) {
                    unreadMessages.remove(i);
                    return;
                }
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

    public void addActiveChat(String account, String jid) {
        if (activeChats.containsKey(account)) {
            List<String> chats = activeChats.get(account);
            if (!chats.contains(jid)) chats.add(jid);
        }
        else {
            List<String> chats = new ArrayList<String>();
            chats.add(jid);
            activeChats.put(account, chats);
        }
    }

    public void removeActiveChat(String account, String jid) {
        if (activeChats.containsKey(account)) {
            List<String> chats = activeChats.remove(account);
            while(chats.contains(jid)) chats.remove(jid);
            activeChats.put(account, chats);
        }
    }

    public List<String> getActiveChats(String account) {
        if (activeChats.containsKey(account)) return activeChats.get(account);
        else return new ArrayList<String>();
    }

    public List<String> getPrivateMessages(String account) {
        List<String> list = new ArrayList<String>();
        for (String jid : getActiveChats(account)) {
            if (getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid)) && !getConferencesHash(account).containsKey(jid)) {
                list.add(jid);
            }
        }
        return list;
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
    public void setText(String jid, String text) {
        if (text != null) textHash.put(jid, text);
    }

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
    
    public void resetTimer() {
    	if (prefs != null) {
            if (prefs.getBoolean("AutoStatusOnDisplay", false)) return;

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

        location = new Location();
        locationClient = new LocationClient(this, location, location);

//        updateReceiver = new BroadcastReceiver() {
//			@Override
//			public void onReceive(Context arg0, Intent arg1) {
//				updateWidget();
//			}
//        };
//        registerReceiver(updateReceiver, new IntentFilter(Constants.UPDATE));
        
        connectionReceiver = new ChangeConnectionReceiver();
        registerReceiver(connectionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        screenStateReceiver = new ScreenStateReceiver();
        registerReceiver(new ScreenStateReceiver(), new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(new ScreenStateReceiver(), new IntentFilter(Intent.ACTION_SCREEN_OFF));
        
        Intent i = new Intent(this, RosterActivity.class);
   		i.setAction(Intent.ACTION_MAIN);
   		i.addCategory(Intent.CATEGORY_LAUNCHER);
   		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
   		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);
   		
   		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.stat_offline);
        mBuilder.setContentTitle(getString(R.string.app_name));
        mBuilder.setContentIntent(contentIntent);

		startForeground(Notify.NOTIFICATION, mBuilder.build());

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "jTalk");

        started = true;

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
    	try {
//    		unregisterReceiver(updateReceiver);
    		unregisterReceiver(connectionReceiver);
            unregisterReceiver(screenStateReceiver);
    	} catch(Exception ignored) { }

        Notify.cancelAll(this);
        stopForeground(true);
        disconnect();
        clearAll();
//        updateWidget();
        started = false;
    }

    public void disconnect() {
        if (!started) return;
        if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();
    	if (locationClient.isConnected()) locationClient.disconnect();
    	Collection<XMPPConnection> con = getAllConnections();
		for (XMPPConnection connection: con) {
			String account = StringUtils.parseBareAddress(connection.getUser());
	    	if (isAuthenticated(account)) {
                if (connectionTasks.containsKey(account)) { connectionTasks.remove(account).cancel(true); }
                if (pingTimers.containsKey(account)) {
                    Timer timer = pingTimers.remove(account);
                    timer.cancel();
                    timer.purge();
                }
	    		removeConnectionListener(account);
				Presence presence = new Presence(Presence.Type.unavailable, "", 0, null);
				connection.disconnect(presence);
	    	} else if (connection.isConnected()) connection.disconnect();
            setState(account, getString(R.string.Disconnect));
            connections.remove(connection);
		}
    }
    
    public void disconnect(String account) {
        if (!started) return;
        Log.e("Disconnect", account);
    	if (connections.containsKey(account)) {
            if (connectionTasks.containsKey(account)) {
                connectionTasks.remove(account).cancel(true);
            }

            if (pingTimers.containsKey(account)) {
                Timer timer = pingTimers.remove(account);
                timer.cancel();
                timer.purge();
            }

            try {
                removeConnectionListener(account);
                Presence presence = new Presence(Presence.Type.unavailable, "", 0, null);
                XMPPConnection connection = connections.remove(account);
                connection.disconnect(presence);
            } catch (Exception ignored) { }

            setState(account, getString(R.string.Disconnect));
    	}
    	sendBroadcast(new Intent(Constants.UPDATE));
    }
    
    public void reconnect() {
        if (!started) return;
    	globalState = getResources().getString(R.string.Reconnecting) + "...";
    	Intent i = new Intent(Constants.UPDATE);
    	sendBroadcast(i);
    	new Thread() {
    		@Override
    		public void run() {
    			disconnect();
    			connect();
    		}
    	}.start();
    }
    
    public void reconnect(final String account) {
        if (!started) return;
        setState(account, getResources().getString(R.string.Reconnecting) + "...");
    	Intent i = new Intent(Constants.UPDATE);
    	sendBroadcast(i);
    	new Thread() {
    		@Override
    		public void run() {
//    			disconnect(account);
                try {
                    Thread.sleep(8000);
                } catch (Exception ignored) { }
    			connect(account);
    		}
    	}.start();
    }
    
    public void connect() {
        if (!started) return;
    	if (prefs.getBoolean("WifiLock", false)) wifiLock.acquire();
    	
//		String text  = prefs.getString("currentStatus", "");
		String mode  = prefs.getString("currentMode", "available");
		
		if (mode.equals("online")) {
			mode = "available";
			setPreference("currentSelection", 0);
		}
		
		if (!mode.equals("unavailable")) {
//			globalState = getString(R.string.Connecting) + "...";
	    	Intent i = new Intent(Constants.UPDATE);
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
                    String sasl = cursor.getString(cursor.getColumnIndex(AccountDbHelper.SASL));
					String port = cursor.getString(cursor.getColumnIndex(AccountDbHelper.PORT));

                    ConnectionTask task = new ConnectionTask();
                    if (connectionTasks.containsKey(username)) task = connectionTasks.get(username);
                    if (task.getStatus() != AsyncTask.Status.RUNNING && task.getStatus() != AsyncTask.Status.FINISHED) {
                        task.execute(username, password, resource, service, tls, sasl, port);
                        connectionTasks.put(username, task);
                    }
				} while(cursor.moveToNext());
				cursor.close();
			}
		} else {
//			globalState = text;
			Intent i = new Intent(Constants.UPDATE);
	    	sendBroadcast(i);
		}
    }
    
    public void connect(String account) {
        if (!started) return;
    	Cursor cursor = getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.JID + " = '" + account + "'", null, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			
			String username = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID)).trim();
			String password = cursor.getString(cursor.getColumnIndex(AccountDbHelper.PASS)).trim();
			String resource = cursor.getString(cursor.getColumnIndex(AccountDbHelper.RESOURCE)).trim();
			String service = cursor.getString(cursor.getColumnIndex(AccountDbHelper.SERVER));
			String tls = cursor.getString(cursor.getColumnIndex(AccountDbHelper.TLS));
            String sasl = cursor.getString(cursor.getColumnIndex(AccountDbHelper.SASL));
			String port = cursor.getString(cursor.getColumnIndex(AccountDbHelper.PORT));

            ConnectionTask task = new ConnectionTask();
            task.execute(username, password, resource, service, tls, sasl, port);
            connectionTasks.put(username, task);
			cursor.close();
		}
    }

    public void joinRoom(final String account, final String group, final String nick, final String password) {
        if (connections.containsKey(account)) {
            final XMPPConnection connection = connections.get(account);
            if (!connection.isAuthenticated()) return;

            new Thread() {
                @Override
                public void run() {
                    boolean reconnecting = false;
                    if (getConferencesHash(account).containsKey(group)) {
                        getConferencesHash(account).remove(group);
                        reconnecting = true;
                    }

                    MultiUserChat muc = new MultiUserChat(connection, group);
                    getConferencesHash(account).put(group, muc);

                    Presence presence = new Presence(Presence.Type.available);
                    presence.setStatus(prefs.getString("currentStatus", ""));
                    presence.setMode(Presence.Mode.valueOf(prefs.getString("currentMode", "available")));

                    DiscussionHistory h = new DiscussionHistory();
                    if (!reconnecting) {
                        try {
                            h.setMaxStanzas(Integer.parseInt(prefs.getString("MucHistorySize", "10")));
                        } catch (NumberFormatException nfe) {
                            h.setMaxStanzas(10);
                        }
                    } else h.setMaxStanzas(0);


                    try {
                        writeMucMessage(account, group, nick, getString(R.string.YouJoin));

                        muc.addParticipantListener(new PacketListener() {
                            @Override
                            public void processPacket(Packet packet) {
                                Presence p = (Presence) packet;
                                if (p.isAvailable()) sendBroadcast(new Intent(Constants.PRESENCE_CHANGED));
                            }
                        });
                        muc.join(nick, password, h, 10000, presence);
                    } catch (Exception e) {
                        Intent eIntent = new Intent(Constants.ERROR);
                        eIntent.putExtra("error", "Error: " + e.getLocalizedMessage());
                        sendBroadcast(eIntent);
                        return;
                    }

                    if (muc.isJoined()) {
                        Intent updateIntent = new Intent(Constants.PRESENCE_CHANGED);
                        updateIntent.putExtra("join", true);
                        updateIntent.putExtra("group", group);
                        sendBroadcast(updateIntent);

                        try {
                            Thread.sleep(10000);
                        } catch (Exception ignored) { }
                        Conference conf = new Conference(group, nick, password);
                        joinedConferences.put(group, conf);
                        muc.addParticipantStatusListener(new MucParticipantStatusListener(account, group));

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
				if (muc.isJoined()) {
                    writeMucMessage(account, group, muc.getNickname(), getString(R.string.YouLeave));
                    muc.leave();
                }
			} catch (IllegalStateException ignored) { }
			getConferencesHash(account).remove(group);
            setMessageList(account, group, new ArrayList<MessageItem>());
	    }
	    while (joinedConferences.containsKey(group)) joinedConferences.remove(group);
	    Intent updateIntent = new Intent(Constants.PRESENCE_CHANGED);
		sendBroadcast(updateIntent);
	}
	
    public void leaveAllRooms(String account) {
        Hashtable<String, MultiUserChat> hash = conferences.get(account);
        if (!hash.isEmpty()) {
            Enumeration<String> groups = hash.keys();
            while(groups.hasMoreElements()) {
                String group = groups.nextElement();
                MultiUserChat muc = hash.get(group);
                writeMucMessage(account, group, muc.getNickname(), getString(R.string.YouLeave));
                try {
                    if (muc.isJoined()) { muc.leave(); }
                } catch (IllegalStateException ignored) { }
            }
        }
        Intent updateIntent = new Intent(Constants.PRESENCE_CHANGED);
        sendBroadcast(updateIntent);
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
  	  		
  	  		ReceiptExtension receipt = new ReceiptExtension(Receipt.request, "");
  	  		msg.addExtension(receipt);
  	  		
  	  		Date date = new java.util.Date();
            String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date);
  	        
  	  		MessageItem msgItem = new MessageItem(account, user);
  	  		msgItem.setTime(time);
  	  		msgItem.setName(getResources().getString(R.string.Me));
  	  		msgItem.setId(mil);
  	  		msgItem.setBody(message);
  	  		msgItem.setReceived(false);
  	  		
  	  		MessageLog.writeMessage(account, user, msgItem);

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
          ReceiptExtension extension = new ReceiptExtension(Receipt.received, id);
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
  	  						if (!isAuthenticated()) Notify.offlineNotify(JTalkService.this, state);
  	  					}
  	  				} else {
  	  					if (mode.equals("unavailable")) {
  	  						if (!isAuthenticated()) Notify.offlineNotify(JTalkService.this, state);
  	  					}
  	  				}
  	  				
  					Intent i = new Intent(Constants.UPDATE);
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
  	  						if (!isAuthenticated()) Notify.offlineNotify(JTalkService.this, state);
  	  					}
  	  				} else {
  	  					if (mode.equals("unavailable")) {
  	  						if (isAuthenticated()) Notify.offlineNotify(JTalkService.this, state);
  	  					}
  	  				}
                    setState(account, state);
  	  				
  					Intent i = new Intent(Constants.UPDATE);
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

    public void sendLocation() {
        sendLocation(locationClient.getLastLocation());
    }

    public void sendLocation(final android.location.Location location) {
        if (location != null) {
            Collection<XMPPConnection> collection = connections.values();
            for (XMPPConnection connection : collection) {
                if (connection.isAuthenticated()) {
                    IQ iq = new IQ() {
                        public String getChildElementXML() {
                            double lat = location.getLatitude();
                            double lon = location.getLongitude();

                            StringBuilder sb = new StringBuilder();
                            sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");
                            sb.append("<publish node='http://jabber.org/protocol/geoloc'>");
                            sb.append("<item><geoloc xmlns='http://jabber.org/protocol/geoloc'>");
                            sb.append("<lat>" + lat + "</lat>");
                            sb.append("<lon>" + lon + "</lon>");
                            sb.append("</geoloc></item></publish></pubsub>");
                            return sb.toString();
                        }
                    };

                    iq.setPacketID(System.currentTimeMillis()+"");
                    iq.setType(IQ.Type.SET);
                    iq.setTo(connection.getHost());
                    connection.sendPacket(iq);
                }
            }
        }
    }

    public void sendEmptyLocation() {
        Collection<XMPPConnection> collection = connections.values();
        for (XMPPConnection connection : collection) {
            if (connection.isAuthenticated()) {
                IQ iq = new IQ() {
                    public String getChildElementXML() {
                        StringBuilder sb = new StringBuilder();
                        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");
                        sb.append("<publish node='http://jabber.org/protocol/geoloc'>");
                        sb.append("<item><geoloc xmlns='http://jabber.org/protocol/geoloc'>");
                        sb.append("</geoloc></item></publish></pubsub>");
                        return sb.toString();
                    }
                };

                iq.setPacketID(System.currentTimeMillis()+"");
                iq.setType(IQ.Type.SET);
                iq.setTo(connection.getHost());
                connection.sendPacket(iq);
            }
        }
    }

    public void sendTunes(final String artist, final String title, final String source) {
        Collection<XMPPConnection> collection = connections.values();
        for (XMPPConnection connection : collection) {
            if (connection.isAuthenticated()) {
                IQ iq = new IQ() {
                    public String getChildElementXML() {
                        StringBuilder sb = new StringBuilder();
                        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");
                        sb.append("<publish node='http://jabber.org/protocol/tune'>");
                        sb.append("<item><tune xmlns='http://jabber.org/protocol/tune'>");
                        if (artist != null) sb.append("<artist>" + StringUtils.escapeForXML(artist) + "</artist>");
                        if (title != null) sb.append("<title>" + StringUtils.escapeForXML(title) + "</title>");
                        if (source != null) sb.append("<source>" + StringUtils.escapeForXML(source) + "</source>");
                        sb.append("</tune></item></publish></pubsub>");
                        return sb.toString();
                    }
                };

                iq.setPacketID(System.currentTimeMillis()+"");
                iq.setType(IQ.Type.SET);
                iq.setTo(connection.getHost());
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
  	
  	public void setPreference(String name, Object value) {
  		if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(this);
  		SharedPreferences.Editor editor = prefs.edit();
  		if(value instanceof String) editor.putString(name, String.valueOf(value));
  		else if(value instanceof Integer) editor.putInt(name, Integer.parseInt(String.valueOf(value)));
  		else if(value instanceof Boolean) editor.putBoolean(name, (Boolean)value);
  		editor.commit();
  	}

    private void writeMucMessage(String account, String group, String nick, String message) {
        String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date());

        MessageItem item = new MessageItem(account, group + "/" + nick);
        item.setBody(message);
        item.setType(MessageItem.Type.status);
        item.setName(StringUtils.parseName(group));
        item.setTime(time);

        MessageLog.writeMucMessage(account, group, nick, item);
    }

  	private void clearAll() {
        activeChats.clear();
        msgCounter.clear();
  		autoStatusTimer.cancel();
        collapsedGroups.clear();
        composeList.clear();
        unreadMessages.clear();
        conferences.clear();
        joinedConferences.clear();
        avatarsHash.clear();
        vcards.clear();
        textHash.clear();
        messagesCount.clear();
  	}

    public void configure() {
        ProviderManager pm = ProviderManager.getInstance();

        // PEP
        PEPProvider pep = new PEPProvider();
        pep.registerPEPParserExtension("http://jabber.org/protocol/tune", new TunesProvider());
        pep.registerPEPParserExtension("http://jabber.org/protocol/geoloc", new LocationProvider());
        pm.addExtensionProvider("event", "http://jabber.org/protocol/pubsub#event", pep);

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
        Intent intent = new Intent(Constants.UPDATE);

        @Override
        protected String doInBackground(String... args) {
            if (connecting) return null;
            else connecting = true;

            String username = args[0];
            String password = args[1];
            String resource = args[2];
            String service = args[3];
            String tls = args[4];
            String sasl = args[5];
            int port = 5222;
            try {
                port = Integer.parseInt(args[6]);
            } catch (NumberFormatException ignored) { }

            if (username == null || username.indexOf("@") < 1) {
                setState(username, getString(R.string.ConnectionError));
                sendBroadcast(intent);
                return null;
            } else {
                Notify.connectingNotify(username);

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
                cc.setSASLAuthenticationEnabled(sasl.equals("1"));
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
                connection.addFeature("http://jabber.org/protocol/chatstates");
                connection.addFeature("http://jabber.org/protocol/geoloc");
                connection.addFeature("http://jabber.org/protocol/tune");
                connection.addFeature("http://jabber.org/protocol/pubsub#event");
                connection.addFeature("jabber:iq:version");
                connection.addFeature("urn:xmpp:receipts");
                connection.addFeature("urn:xmpp:time");
                connection.addFeature("urn:xmpp:message-correct:0");

                try {
                    if (!connection.isConnected()) connection.connect();
                } catch (XMPPException xe) {
                    String error = "Error connecting to " + connection.getServiceName();
                    setState(username, error);
                    sendBroadcast(intent);
                    if (!isAuthenticated()) Notify.offlineNotify(JTalkService.this, error);
                    return null;
                }

                try {
                    if (connection.isConnected() && !connection.isAuthenticated()) {
                        connection.login(user, password, resource);

                        connection.addPacketListener(new MsgListener(JTalkService.this, connection, username), new PacketTypeFilter(Message.class));
                        addConnectionListener(username, connection);

                        Roster roster = connection.getRoster();
                        roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
                        roster.addRosterListener(new RstListener(username));

                        connections.put(username, connection);
                        return username;
                    }
                } catch (XMPPException e) {
                    XMPPError error = e.getXMPPError();
                    if (error != null) setState(username, "[" + error.getCode() + "]: " + error.getMessage());
                    else setState(username, e.getLocalizedMessage());
                    sendBroadcast(intent);
                    if (!isAuthenticated()) Notify.offlineNotify(JTalkService.this, "");
                    return null;
                }
            }
            return null;
        }

        @Override
        public void onPostExecute(final String username) {
            connecting = false;
            if (username != null) {
                final XMPPConnection connection = connections.get(username);
                if (!connection.isAuthenticated()) return;

                int priority = prefs.getInt("currentPriority", 0);
                String status  = prefs.getString("currentStatus", "");
                String mode  = prefs.getString("currentMode", "available");
                sendPresence(username, status, mode, priority);
                setState(username, status);

                new PrivacyListManager(connection);
                new ServiceDiscoveryManager(connection);
                new AdHocCommandManager(connection);
                fileTransferManager = new FileTransferManager(connection);
                fileTransferManager.addFileTransferListener(new IncomingFileListener());

                try {
                    MultiUserChat.addInvitationListener(connection, new InviteListener(username));
                } catch (Exception ignored) { }

                // Join to rooms if reconnected or autojoin is enabled
                new Thread() {
                    public void run() {
                        try {
                            Thread.sleep(5000);
                        } catch (Exception ignored) { }

                        if (!getConferencesHash(username).isEmpty()) {
                            Collection<Conference> coll = joinedConferences.values();
                            for (Conference conf : coll) {
                                joinRoom(username, conf.getName(), conf.getNick(), conf.getPassword());
                            }
                        } else {
                            try {
                                BookmarkManager bm = BookmarkManager.getBookmarkManager(connection);
                                Collection<BookmarkedConference> bookmarks = bm.getBookmarkedConferences();
                                for(BookmarkedConference bc : bookmarks) {
                                    String nick = bc.getNickname();
                                    if (nick == null || nick.length() < 1) nick = StringUtils.parseName(username);
                                    if (bc.isAutoJoin()) joinRoom(username, bc.getJid(), bc.getNickname(), bc.getPassword());
                                }
                            } catch (XMPPException ignored) { }
                        }
                    }
                }.start();

                if (prefs.getBoolean("Locations", false)) {
                    if (!locationClient.isConnected() && !locationClient.isConnecting()) {
                        locationClient.connect();
                    }
                } else {
                    sendEmptyLocation();
                }

                if (prefs.getBoolean("Ping", false)) {
                    int timeout = 60000;
                    try {
                        timeout = Integer.parseInt(prefs.getString("PingTimeout", 60+"")) * 1000;
                    } catch (NumberFormatException ignored) { }
                    Timer pingTimer = new Timer();
                    pingTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            new PingTask(username).execute();
                        }
                    }, timeout, timeout * 2);
                    pingTimers.put(username, pingTimer);
                }

                try {
                    OfflineMessageManager omm = new OfflineMessageManager(connection);
                    omm.deleteMessages();
                } catch (Exception ignored) {	}

                Notify.updateNotify();
                new IgnoreList(connection).createIgnoreList();
                resetTimer();

                if (connectionTasks.containsKey(username)) connectionTasks.remove(username);
                sendBroadcast(new Intent(Constants.UPDATE));
            }
        }
    }
}
