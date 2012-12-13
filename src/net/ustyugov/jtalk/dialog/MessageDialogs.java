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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.MessageItem;
import net.ustyugov.jtalk.service.JTalkService;

import com.jtalk2.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import org.jivesoftware.smack.util.StringUtils;

public class MessageDialogs {
	private static Pattern pattern = Pattern.compile("(ht|f)tps?://[a-z0-9\\-\\.]+[a-z]{2,}/?[^\\s\\n]*", Pattern.CASE_INSENSITIVE);
	
	public static void QuoteDialog(final Activity activity, final String jid, final MessageItem message) {
		final String body = message.getBody();
		final List<String> urls = new ArrayList<String>();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    	final boolean showtime = prefs.getBoolean("ShowTime", false);
		
		String time = message.getTime();
        String name = message.getName();
        String t = "(" + time + ")";
        if (showtime) name = t + " " + name;
        final String text = name + ": " + body;
        
        Matcher m = pattern.matcher(body);
		while (m.find()) {
			int start = m.start();
			int end = m.end();
			urls.add(body.substring(start, end));
		}
		
		CharSequence[] items = new CharSequence[urls.size() + 3];

        items[0] = activity.getResources().getString(R.string.QuoteSelectedMessages);
        items[1] = activity.getResources().getString(R.string.QuoteMessage);
        items[2] = activity.getResources().getString(R.string.QuoteTextMessage);
        for (int i = 0; i < urls.size(); i++) {
        	items[i+3] = activity.getResources().getString(R.string.Quote) + " " + urls.get(i);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.Quote);
        builder.setItems(items, new OnClickListener() {
        	@Override
        	public void onClick(DialogInterface dialog, int which) {
        		Intent intent = new Intent(Constants.PASTE_TEXT);
        		switch(which) {
                    case 0:
                        String str = "";
                        JTalkService service = JTalkService.getInstance();
                        List<MessageItem> msgList = new ArrayList<MessageItem>();

                        String account = message.getAccount();
                        if (service.getConferencesHash(account).containsKey(jid)) {
                            if (service.getMucMessagesHash(account).containsKey(jid)) {
                                msgList = service.getMucMessagesHash(account).get(jid);
                            }
                        } else {
                            if (service.getMessagesHash(account).containsKey(jid)) {
                                msgList = service.getMessagesHash(account).get(jid);
                            }
                        }
                        for (MessageItem item : msgList) {
                            if (item.isSelected()) {
                                String time = item.getTime();
                                String name = item.getName();
                                String body = item.getBody();
                                String t = "(" + time + ")";
                                if (showtime) str += "> " + t + " " + name + ": " + body + "\n\n";
                                else str += "> " + name + ": " + body + "\n\n";
                            }
                        }
                        intent.putExtra("text", str);
                        break;
                    case 1:
                        intent.putExtra("text", "> " + text + "\n");
                        break;
                    case 2:
                        intent.putExtra("text", "> " + body + "\n");
                        break;
        		}
        		if (which >= 3) {
        			which = which - 3;
        			intent.putExtra("text", urls.get(which));
        		} 
        		activity.sendBroadcast(intent);
        	}
        });
        builder.create().show();
	}

	public static void CopyDialog(final Activity activity, final String jid, final MessageItem message) {
		final String body = message.getBody();
		final List<String> urls = new ArrayList<String>();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    	final boolean showtime = prefs.getBoolean("ShowTime", false);
		
		String time = message.getTime();
        String name = message.getName();
        String t = "(" + time + ")";
        if (showtime) name = t + " " + name;
        final String text = name + ": " + body;;
        
        Matcher m = pattern.matcher(body);
		while (m.find()) {
			int start = m.start();
			int end = m.end();
			urls.add(body.substring(start, end));
		}
		
		CharSequence[] items = new CharSequence[urls.size() + 3];

        items[0] = activity.getResources().getString(R.string.CopySelectedMessages);
        items[1] = activity.getResources().getString(R.string.CopyMessage);
        items[2] = activity.getResources().getString(R.string.CopyTextMessage);
        for (int i = 0; i < urls.size(); i++) {
        	items[i+3] = activity.getResources().getString(R.string.Copy) + " " + urls.get(i);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.Quote);
        builder.setItems(items, new OnClickListener() {
        	@Override
        	public void onClick(DialogInterface dialog, int which) {
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                switch(which) {
                    case 0:
                        String str = "";
                        JTalkService service = JTalkService.getInstance();
                        List<MessageItem> msgList = new ArrayList<MessageItem>();
                        String account = message.getAccount();
                        if (service.getConferencesHash(account).containsKey(jid)) {
                            if (service.getMucMessagesHash(account).containsKey(jid)) {
                                msgList = service.getMucMessagesHash(account).get(jid);
                            }
                        } else {
                            if (service.getMessagesHash(account).containsKey(jid)) {
                                msgList = service.getMessagesHash(account).get(jid);
                            }
                        }
                        for (MessageItem item : msgList) {
                            if (item.isSelected()) {
                                String time = item.getTime();
                                String name = item.getName();
                                String body = item.getBody();
                                String t = "(" + time + ")";
                                if (showtime) str += "> " + t + " " + name + ": " + body + "\n\n";
                                else str += "> " + name + ": " + body + "\n\n";
                            }
                        }
                        if (str.length() > 0) clipboard.setText(str);
                        break;
                    case 1:
                        clipboard.setText(text);
                        break;
                    case 2:
                        clipboard.setText(body);
                        break;
                }
                if (which >= 3) {
                    which = which - 3;
                    clipboard.setText(urls.get(which));
                }
            }
        });
        builder.create().show();
	}
	
	public static void SelectTextDialog(Activity activity, MessageItem message) {
		String text;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    	boolean showtime = prefs.getBoolean("ShowTime", false);
		
		String body = message.getBody();
		String time = message.getTime();
        String name = message.getName();
        String t = "(" + time + ")";
        if (showtime) name = t + " " + name;
        text = name + ": " + body;
        
        LayoutInflater inflater = activity.getLayoutInflater();
		View layout = inflater.inflate(R.layout.select_text_dialog, (ViewGroup) activity.findViewById(R.id.select_text_dialog_linear));
		
		final EditText edit = (EditText) layout.findViewById(R.id.text);
	    edit.setText(text);
	    edit.selectAll();
	    
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setView(layout);
		builder.create().show();
	}
	
	public static void EditMessageDialog(Activity activity, final String account, MessageItem message, final String jid) {
		final String id = message.getId();
		String text = message.getBody();
		
		LayoutInflater inflater = activity.getLayoutInflater();
		View layout = inflater.inflate(R.layout.select_text_dialog, (ViewGroup) activity.findViewById(R.id.select_text_dialog_linear));
		
		final EditText edit = (EditText) layout.findViewById(R.id.text);
	    edit.setText(text);
	    edit.selectAll();
	    
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setView(layout);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				JTalkService service = JTalkService.getInstance();
				if (service != null && service.isAuthenticated()) {
					service.editMessage(account, jid, id, edit.getText().toString());
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
