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

import net.ustyugov.jtalk.activity.DataFormActivity;
import net.ustyugov.jtalk.activity.RosterActivity;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;

import com.jtalk2.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

public class Notify {
	private static final int NOTIFICATION              = 1;
	private static final int NOTIFICATION_FILE 		   = 2;
	private static final int NOTIFICATION_IN_FILE 	   = 3;
	private static final int NOTIFICATION_FILE_REQUEST = 4;
	private static final int NOTIFICATION_SUBSCRIBTION = 5;
	private static final int NOTIFICATION_CAPTCHA      = 6;
	
	public static boolean newMessages = false;
	public enum Type {Chat, Conference, Direct}
	
    public static void updateNotify() {
    	JTalkService service = JTalkService.getInstance();
    	if (service.getMessagesList().isEmpty()) {
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
            i.setAction(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, 0);
            
            Notification notification = new Notification(icon, statusArray[pos] + " (" + text + ")", System.currentTimeMillis());
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.setLatestEventInfo(service, statusArray[pos], text, contentIntent);

            NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
            mng.notify(NOTIFICATION, notification);
    	} else {
    		String currentJid = JTalkService.getInstance().getCurrentJid();
        	if (currentJid.equals("me")) {
            	Intent i = new Intent(service, RosterActivity.class);
            	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            	i.putExtra("msg", true);
                PendingIntent pi = PendingIntent.getActivity(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
                
                Notification n = new Notification(R.drawable.msg, service.getString(R.string.UnreadMessage), System.currentTimeMillis());
                n.flags |= Notification.FLAG_ONGOING_EVENT;
                n.flags |= Notification.FLAG_SHOW_LIGHTS;
                n.setLatestEventInfo(service, service.getString(R.string.app_name), service.getString(R.string.UnreadMessage), pi);
                
                NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
                mng.notify(NOTIFICATION, n);
        	}
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
   		
//        String str = service.getString(R.string.Disconnect);
//        str = str + " (" + state + ")";
        
        Notification notification = new Notification(R.drawable.stat_offline, state, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.setLatestEventInfo(service, "jTalk", state, contentIntent);

        NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION, notification);
    }
    
    public static void cancelAll(Context context) {
    	newMessages = false;
    	NotificationManager mng = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    	mng.cancelAll();
    }
    
    public static void messageNotify(Context c, String from, Type type, String text) {
    	newMessages = true;
    	String nick = from;
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    	boolean include = prefs.getBoolean("MessageInNotification", false);
    	if (include) {
    		int count = Integer.parseInt(prefs.getString("MessageInNotificationCount", "64"));
    		if (count > 0 && count < text.length()) text = text.substring(0, count);
    	}
    	String vibration = prefs.getString("vibrationMode", "1");
    	Vibrator vibrator = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
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
    	
    		Roster roster = JTalkService.getInstance().getRoster();
    		if (roster != null) {
    			RosterEntry re = roster.getEntry(from);
    			if (re != null && re.getName() != null) nick = re.getName();
    		}
    		
        	Uri sound_file = Uri.parse(soundPath);
        	
        	Intent i = new Intent(c, RosterActivity.class);
        	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	i.putExtra("msg", true);
            PendingIntent pi = PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            
            Notification n;
            if (include) n = new Notification(R.drawable.stat_msg, c.getString(R.string.NewMessageFrom) + " " + nick + ": " + text, System.currentTimeMillis());
            else n = new Notification(R.drawable.stat_msg, c.getString(R.string.NewMessageFrom) + " " + nick, System.currentTimeMillis());
            n.flags |= Notification.FLAG_ONGOING_EVENT;
            n.flags |= Notification.FLAG_SHOW_LIGHTS;
            n.ledARGB = 0xFF00FF00;
            n.ledOnMS = 2000;
            n.ledOffMS = 3000;
            n.setLatestEventInfo(c, c.getString(R.string.app_name), c.getString(R.string.UnreadMessage), pi);
            
            if (sound) n.sound = sound_file;
          
            NotificationManager mng = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            mng.notify(NOTIFICATION, n);
    	}
    }
    
    public static void subscribtionRequest(Context context, String from) {
    	newMessages = false;
    	Intent i = new Intent(context, RosterActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, i, 0);
  
        String str = "Request subscribtion from " + from; 
        
        Notification notification = new Notification(R.drawable.noface, str, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(context, "JTalk", str, contentIntent);
    	
    	NotificationManager mng = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION_SUBSCRIBTION, notification);
    }
    
    public static void fileProgress(String filename, Status status) {
    	JTalkService service = JTalkService.getInstance();
    	int drawable = android.R.drawable.stat_sys_warning;
    	String str = "";
    	int flag = Notification.FLAG_AUTO_CANCEL;
    	
    	Intent i = new Intent(service, RosterActivity.class);
    	i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, 0);
        
        if (status == Status.complete) {
        	drawable = android.R.drawable.stat_sys_upload_done;
        	str = service.getString(R.string.Completed);
        } else if (status == Status.cancelled) {
        	drawable = android.R.drawable.stat_sys_warning;
        	str = service.getString(R.string.Canceled);
        } else if (status == Status.refused) {
        	drawable = android.R.drawable.stat_sys_warning;
        	str = service.getString(R.string.Refused);
        } else if (status == Status.negotiating_transfer) {
//        	flag = Notification.FLAG_ONGOING_EVENT;
        	drawable = android.R.drawable.stat_sys_upload_done;
        	str = service.getString(R.string.Waiting);
        } else if (status == Status.in_progress) {
        	flag = Notification.FLAG_ONGOING_EVENT;
        	drawable = android.R.drawable.stat_sys_upload;
        	str = service.getString(R.string.Sending);
        } else if (status == Status.error) {
        	drawable = android.R.drawable.stat_sys_warning;
        	str = service.getString(R.string.Error);
        } else {
        	return;
        }
        
        Notification notification = new Notification(drawable, str, System.currentTimeMillis());
        notification.flags |= flag;
        notification.setLatestEventInfo(service, filename, str, contentIntent);
    	
    	NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION_FILE, notification);
    }
    
    public static void incomingFile() {
    	JTalkService service = JTalkService.getInstance();
    	Intent i = new Intent(service, RosterActivity.class);
    	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	i.putExtra("file", true);
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
  
        Notification notification = new Notification(android.R.drawable.stat_sys_warning, service.getString(R.string.AcceptFile), System.currentTimeMillis());
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.setLatestEventInfo(service, "jTalk", service.getString(R.string.AcceptFile), contentIntent);
    	
    	NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION_FILE_REQUEST, notification);
    }
    
    public static void incomingFileProgress(String filename, Status status) {
    	JTalkService service = JTalkService.getInstance();
    	int drawable = android.R.drawable.stat_sys_warning;
    	String str = "";
    	int flag = Notification.FLAG_AUTO_CANCEL;
    	
    	Intent i = new Intent(service, RosterActivity.class);
    	i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        if (status == Status.complete) {
        	drawable = android.R.drawable.stat_sys_download_done;
        	str = service.getString(R.string.Completed);
        } else if (status == Status.cancelled) {
        	drawable = android.R.drawable.stat_sys_warning;
        	str = service.getString(R.string.Canceled);
        } else if (status == Status.refused) {
        	drawable = android.R.drawable.stat_sys_warning;
        	str = service.getString(R.string.Refused);
        } else if (status == Status.negotiating_transfer) {
        	flag = Notification.FLAG_ONGOING_EVENT;
        	drawable = android.R.drawable.stat_sys_download_done;
        	str = service.getString(R.string.Waiting);
        } else if (status == Status.in_progress) {
        	flag = Notification.FLAG_ONGOING_EVENT;
        	drawable = android.R.drawable.stat_sys_download;
        	str = service.getString(R.string.Downloading);
        } else if (status == Status.error) {
        	drawable = android.R.drawable.stat_sys_warning;
        	str = service.getString(R.string.Error);
        } else {
        	return;
        }
        
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, 0);
        
        Notification notification = new Notification(drawable, str, System.currentTimeMillis());
        notification.flags |= flag;
        notification.setLatestEventInfo(service, filename, str, contentIntent);
    	
    	NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION_IN_FILE, notification);
    }
    
    public static void cancelFileRequest() {
    	NotificationManager mng = (NotificationManager) JTalkService.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
    	mng.cancel(NOTIFICATION_FILE_REQUEST);
    }
    
    public static void captchaNotify(MessageItem message) {
    	JTalkService service = JTalkService.getInstance();
    	service.addDataForm(message.getId(), message.getForm());
    	
    	Intent intent = new Intent(service, DataFormActivity.class);
    	intent.putExtra("id", message.getId());
    	intent.putExtra("cap", true);
        intent.putExtra("jid", message.getName());
        intent.putExtra("bob", message.getBob().getData());
        intent.putExtra("cid", message.getBob().getCid());
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, intent, 0);
   		
        String str = "Captcha";
        
        Notification notification = new Notification(R.drawable.muc, str, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(service, "jTalk", str, contentIntent);

        NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION_CAPTCHA, notification);
    }
}
