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

import java.util.List;

import android.graphics.BitmapFactory;
import net.ustyugov.jtalk.activity.Chat;
import net.ustyugov.jtalk.activity.DataFormActivity;
import net.ustyugov.jtalk.activity.RosterActivity;
import net.ustyugov.jtalk.activity.filetransfer.ReceiveFileActivity;
import net.ustyugov.jtalk.activity.muc.Invite;
import net.ustyugov.jtalk.service.JTalkService;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import com.jtalk2.R;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

public class Notify {
	private static final int NOTIFICATION = 1;
	private static final int NOTIFICATION_FILE = 2;
	private static final int NOTIFICATION_IN_FILE = 3;
	private static final int NOTIFICATION_FILE_REQUEST = 4;
//	private static final int NOTIFICATION_SUBSCRIBTION = 5;
	private static final int NOTIFICATION_CAPTCHA = 6;
	private static final int NOTIFICATION_INVITE = 7;
	
	public static boolean newMessages = false;
	public enum Type {Chat, Conference, Direct}
	
    public static void updateNotify() {
    	JTalkService service = JTalkService.getInstance();
    	if (service.getUnreadMessages().isEmpty()) {
    		newMessages = false;
        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
        	String mode = prefs.getString("currentMode", "available");
        	int pos = prefs.getInt("currentSelection", 0);
            String text = prefs.getString("currentStatus", null);
            String[] statusArray = service.getResources().getStringArray(R.array.statusArray);

            int icon = R.drawable.stat_online;
            if (mode.equals("available")) {
            	icon = R.drawable.stat_online;
            }
            else if (mode.equals("chat")) {
            	icon = R.drawable.stat_chat;
            }
            else if (mode.equals("away")) {
            	icon = R.drawable.stat_away;
            }
            else if (mode.equals("xa")) {
            	icon = R.drawable.stat_xaway;
            }
            else if (mode.equals("dnd")) {
            	icon = R.drawable.stat_dnd;
            }

            Intent i = new Intent(service, RosterActivity.class);
            i.setAction(Constants.UPDATE);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.putExtra("status", false);
            PendingIntent piRoster = PendingIntent.getActivity(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent i2 = new Intent(service, RosterActivity.class);
            i2.setAction(Constants.PRESENCE_CHANGED);
            i2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i2.putExtra("status", true);
            PendingIntent piStatus = PendingIntent.getActivity(service, 0, i2, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service);
            mBuilder.setLargeIcon(BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_launcher));
            mBuilder.setSmallIcon(icon);
            mBuilder.setContentTitle(statusArray[pos]);
            mBuilder.setContentText(text);
            mBuilder.setContentIntent(piRoster);
            mBuilder.addAction(R.drawable.ic_action_refresh, service.getString(R.string.Status), piStatus);
            mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);

            NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
            mng.notify(NOTIFICATION, mBuilder.build());
    	} else {
            List<MessageItem> list = service.getUnreadMessages();
            MessageItem messageItem = list.get(0);

           	Intent i = new Intent(service, Chat.class);
           	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.putExtra("jid", messageItem.getJid());
            i.putExtra("account", messageItem.getAccount());
            PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service);
            mBuilder.setLargeIcon(BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_launcher));
            mBuilder.setSmallIcon(R.drawable.stat_msg);
            mBuilder.setLights(0xFF00FF00, 2000, 3000);
            mBuilder.setContentTitle(service.getString(R.string.app_name));
            mBuilder.setContentText(service.getString(R.string.UnreadMessage));
            mBuilder.setNumber(service.getUnreadMessages().size());
            mBuilder.setContentIntent(contentIntent);
            mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle(service.getString(R.string.UnreadMessage));

            for (MessageItem item : list) {
                String account = item.getAccount();
                String jid = item.getJid();
                String n = StringUtils.parseBareAddress(jid);
                if (service.getConferencesHash(account).containsKey(jid)) {
                    n = StringUtils.parseName(jid);
                } else if (service.getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid))) {
                    n = StringUtils.parseResource(jid);
                } else {
                    jid = StringUtils.parseBareAddress(jid);
                    Roster roster = JTalkService.getInstance().getRoster(account);
                    if (roster != null) {
                        RosterEntry re = roster.getEntry(n);
                        if (re != null && re.getName() != null) n = re.getName();
                    }
                }
                if (n != null && n.length() > 0) {
                    inboxStyle.addLine(n + ": (" + service.getMessagesCount(account, jid) + ") " + item.getBody());
                }
            }

            mBuilder.setStyle(inboxStyle);

            NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
            mng.notify(NOTIFICATION, mBuilder.build());
    	}
    }
    
    public static void offlineNotify(String state) {
    	newMessages = false;
    	JTalkService service = JTalkService.getInstance();
        Intent i = new Intent(service, RosterActivity.class);
        i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, 0);
   		
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service);
        mBuilder.setLargeIcon(BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_launcher));
        mBuilder.setSmallIcon(R.drawable.stat_offline);
        mBuilder.setContentTitle(service.getString(R.string.app_name));
        mBuilder.setContentText(state);
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION, mBuilder.build());
    }
    
    public static void cancelAll(Context context) {
    	newMessages = false;
    	NotificationManager mng = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    	mng.cancelAll();
    }
    
    public static void messageNotify(String account, String from, Type type, String text) {
    	newMessages = true;
    	JTalkService service = JTalkService.getInstance();
    	String nick = from;
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
        String ticker = "";
    	boolean include = prefs.getBoolean("MessageInNotification", false);
    	if (include) {
    		int count = Integer.parseInt(prefs.getString("MessageInNotificationCount", "64"));
    		if (count > 0 && count < text.length()) text = text.substring(0, count);
    	}
    	String vibration = prefs.getString("vibrationMode", "1");
    	Vibrator vibrator = (Vibrator) service.getSystemService(Context.VIBRATOR_SERVICE);
    	boolean vibro = false;
    	boolean sound = true;
    	String soundPath = "";

    	if (type == Type.Conference) {
    		String currentJid = JTalkService.getInstance().getCurrentJid();
        	if (!currentJid.equals(from) || currentJid.equals("me")) {
        		if (vibration.equals("1") || vibration.equals("4")) vibrator.vibrate(200);
        		new SoundTask().execute("");
        	}
    		return;
    	} else if (type == Type.Direct) {
    		if (vibration.equals("1") || vibration.equals("3") || vibration.equals("4")) vibro = true;
    		soundPath = prefs.getString("ringtone_direct", "");
    	} else {
    		if (vibration.equals("1") || vibration.equals("2") || vibration.equals("3")) vibro = true;
    		soundPath = prefs.getString("ringtone", "");
    	}
    	
    	if (soundPath.equals("")) sound = false;
    	
    	String currentJid = JTalkService.getInstance().getCurrentJid();
    	if (!currentJid.equals(from) || currentJid.equals("me")) {
    		if (vibro) vibrator.vibrate(200);
    	
    		if (service.getConferencesHash(account).containsKey(from)) {
    			nick = StringUtils.parseName(from);
    		} else if (service.getConferencesHash(account).containsKey(StringUtils.parseBareAddress(from))) {
    			nick = StringUtils.parseResource(from);
    		} else {
    			Roster roster = JTalkService.getInstance().getRoster(account);
        		if (roster != null) {
        			RosterEntry re = roster.getEntry(from);
        			if (re != null && re.getName() != null) nick = re.getName();
        		}
    		}

            if (include) {
                ticker = service.getString(R.string.NewMessageFrom) + " " + nick + ": " + text;
            } else ticker = service.getString(R.string.NewMessageFrom) + " " + nick;
    		
        	Uri sound_file = Uri.parse(soundPath);
        	
        	Intent i = new Intent(service, Chat.class);
        	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.putExtra("jid", from);
        	i.putExtra("account", account);
            PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service);
//            mBuilder.setLargeIcon(BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_launcher));
            mBuilder.setSmallIcon(R.drawable.stat_msg);
            mBuilder.setLights(0xFF00FF00, 2000, 3000);
            mBuilder.setContentTitle(service.getString(R.string.UnreadMessage));
            mBuilder.setContentText(ticker);
            mBuilder.setNumber(service.getUnreadMessages().size());
            mBuilder.setContentIntent(contentIntent);
            mBuilder.setTicker(ticker);
            mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
            if (sound) mBuilder.setSound(sound_file);
            
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle(service.getString(R.string.UnreadMessage));

            List<MessageItem> list = service.getUnreadMessages();
            for (MessageItem item : list) {
                String acc = item.getAccount();
                String jid = item.getJid();
                String n = StringUtils.parseBareAddress(jid);
                if (service.getConferencesHash(acc).containsKey(jid)) {
                    n = StringUtils.parseName(jid);
                } else if (service.getConferencesHash(acc).containsKey(StringUtils.parseBareAddress(jid))) {
                    n = StringUtils.parseResource(jid);
                } else {
                    jid = StringUtils.parseBareAddress(jid);
                    Roster roster = JTalkService.getInstance().getRoster(acc);
                    if (roster != null) {
                        RosterEntry re = roster.getEntry(n);
                        if (re != null && re.getName() != null) n = re.getName();
                    }
                }
                if (n != null && n.length() > 0) inboxStyle.addLine(n + ": (" + service.getMessagesCount(acc, jid) + ") " + item.getBody());
            }

            mBuilder.setStyle(inboxStyle);
            
            NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
            mng.notify(NOTIFICATION, mBuilder.build());
    	}
    }
    
//    public static void subscribtionRequest(Context context, String from) {
//    	newMessages = false;
//    	Intent i = new Intent(context, RosterActivity.class);
//        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, i, 0);
//  
//        String str = "Request subscribtion from " + from; 
//        
//        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
//        mBuilder.setOngoing(false);
//        mBuilder.setSmallIcon(R.drawable.noface);
//        mBuilder.setLights(0xFF0000FF, 2000, 3000);
//        mBuilder.setContentTitle(context.getString(R.string.app_name));
//        mBuilder.setContentText(str);
//        mBuilder.setContentIntent(contentIntent);
//        mBuilder.setTicker(str);
//        
//    	NotificationManager mng = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        mng.notify(NOTIFICATION_SUBSCRIBTION, mBuilder.build());
//    }
    
    public static void fileProgress(String filename, Status status) {
    	JTalkService service = JTalkService.getInstance();
    	
    	Intent i = new Intent(service, RosterActivity.class);
    	i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, 0);
        
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service);
        mBuilder.setContentTitle(filename);
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setOngoing(false);
        
        if (status == Status.complete) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        	mBuilder.setTicker(service.getString(R.string.Completed));
        	mBuilder.setContentText(service.getString(R.string.Completed));
        	mBuilder.setAutoCancel(true);
        } else if (status == Status.cancelled) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
        	mBuilder.setTicker(service.getString(R.string.Canceled));
        	mBuilder.setContentText(service.getString(R.string.Canceled));
        	mBuilder.setAutoCancel(true);
        } else if (status == Status.refused) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
        	mBuilder.setTicker(service.getString(R.string.Canceled));
        	mBuilder.setContentText(service.getString(R.string.Canceled));
        	mBuilder.setAutoCancel(true);
        } else if (status == Status.negotiating_transfer) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        	mBuilder.setTicker(service.getString(R.string.Waiting));
        	mBuilder.setContentText(service.getString(R.string.Waiting));
        } else if (status == Status.in_progress) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
        	mBuilder.setTicker(service.getString(R.string.Downloading));
        	mBuilder.setContentText(service.getString(R.string.Downloading));
        } else if (status == Status.error) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
        	mBuilder.setTicker(service.getString(R.string.Error));
        	mBuilder.setContentText(service.getString(R.string.Error));
        	mBuilder.setAutoCancel(true);
        } else {
        	return;
        }
        
    	NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION_FILE, mBuilder.build());
    }
    
    public static void incomingFile() {
    	JTalkService service = JTalkService.getInstance();
    	Intent i = new Intent(service, ReceiveFileActivity.class);
    	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	i.putExtra("file", true);
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
  
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service);
        mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
        mBuilder.setLights(0xFF0000FF, 2000, 3000);
        mBuilder.setContentTitle(service.getString(R.string.app_name));
        mBuilder.setContentText(service.getString(R.string.AcceptFile));
        mBuilder.setContentIntent(contentIntent);
        
    	NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION_FILE_REQUEST, mBuilder.build());
    }
    
    public static void inviteNotify(String account, String room, String from, String reason, String password) {
    	JTalkService service = JTalkService.getInstance();
    	
    	Intent i = new Intent(service, Invite.class);
    	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	i.putExtra("account", account);
        i.putExtra("room", room);
        i.putExtra("from", from);
        i.putExtra("reason", reason);
        i.putExtra("password", password);
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service);
        mBuilder.setSmallIcon(R.drawable.icon_muc);
        mBuilder.setLights(0xFF0000FF, 2000, 3000);
        mBuilder.setAutoCancel(true);
        mBuilder.setTicker(service.getString(R.string.InviteTo) + " " + room);
        mBuilder.setContentTitle(service.getString(R.string.InviteTo));
        mBuilder.setContentText(room);
        mBuilder.setContentIntent(contentIntent);
        
    	NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION_INVITE, mBuilder.build());
    }
    
    public static void incomingFileProgress(String filename, Status status) {
    	JTalkService service = JTalkService.getInstance();
    	
    	Intent i = new Intent(service, RosterActivity.class);
    	i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, 0);
        
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service);
        mBuilder.setContentTitle(filename);
        mBuilder.setContentIntent(contentIntent);
        
        if (status == Status.complete) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        	mBuilder.setTicker(service.getString(R.string.Completed));
        	mBuilder.setContentText(service.getString(R.string.Completed));
        	mBuilder.setAutoCancel(true);
        	mBuilder.setOngoing(false);
        } else if (status == Status.cancelled) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
        	mBuilder.setTicker(service.getString(R.string.Canceled));
        	mBuilder.setContentText(service.getString(R.string.Canceled));
        	mBuilder.setAutoCancel(true);
        	mBuilder.setOngoing(false);
        } else if (status == Status.refused) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
        	mBuilder.setTicker(service.getString(R.string.Canceled));
        	mBuilder.setContentText(service.getString(R.string.Canceled));
        	mBuilder.setAutoCancel(true);
        	mBuilder.setOngoing(false);
        } else if (status == Status.negotiating_transfer) {
        	mBuilder.setOngoing(true);
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        	mBuilder.setTicker(service.getString(R.string.Waiting));
        	mBuilder.setContentText(service.getString(R.string.Waiting));
        } else if (status == Status.in_progress) {
        	mBuilder.setOngoing(true);
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
        	mBuilder.setTicker(service.getString(R.string.Downloading));
        	mBuilder.setContentText(service.getString(R.string.Downloading));
        } else if (status == Status.error) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
        	mBuilder.setTicker(service.getString(R.string.Error));
        	mBuilder.setContentText(service.getString(R.string.Error));
        	mBuilder.setAutoCancel(true);
        	mBuilder.setOngoing(false);
        } else {
        	return;
        }
        
    	NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION_IN_FILE, mBuilder.build());
    }
    
    public static void cancelFileRequest() {
    	NotificationManager mng = (NotificationManager) JTalkService.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
    	mng.cancel(NOTIFICATION_FILE_REQUEST);
    }
    
    public static void captchaNotify(String account, MessageItem message) {
    	JTalkService service = JTalkService.getInstance();
    	service.addDataForm(message.getId(), message.getForm());
    	
    	Intent intent = new Intent(service, DataFormActivity.class);
        intent.putExtra("account", account);
    	intent.putExtra("id", message.getId());
    	intent.putExtra("cap", true);
        intent.putExtra("jid", message.getName());
        intent.putExtra("bob", message.getBob().getData());
        intent.putExtra("cid", message.getBob().getCid());
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, intent, 0);
   		
        String str = "Captcha";
        
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service);
        mBuilder.setAutoCancel(true);
        mBuilder.setOngoing(false);
        mBuilder.setSmallIcon(R.drawable.icon_muc);
        mBuilder.setLights(0xFF0000FF, 2000, 3000);
        mBuilder.setContentTitle(str);
        mBuilder.setContentText(message.getBody());
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setTicker(str);
        
        NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION_CAPTCHA, mBuilder.build());
    }
}
