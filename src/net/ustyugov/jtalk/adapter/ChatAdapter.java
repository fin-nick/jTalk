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

package net.ustyugov.jtalk.adapter;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.MessageItem;
import net.ustyugov.jtalk.Smiles;
import net.ustyugov.jtalk.dialog.JuickMessageMenuDialog;
import net.ustyugov.jtalk.listener.TextLinkClickListener;
import net.ustyugov.jtalk.view.MyTextView;

import com.jtalk2.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class ChatAdapter extends ArrayAdapter<MessageItem> implements TextLinkClickListener {
	private int searchCurrentPosition = 0;
	private String searchString = "";
	private Hashtable<Integer, List<Integer>> searchHash = new Hashtable<Integer, List<Integer>>();
	
	private SharedPreferences prefs;
	private Context context;
	private Smiles smiles;
	private String jid;
	private boolean firstClick = false;
	private boolean showtime;
	private Timer doubleClickTimer = new Timer();
	
	private int linkColor;
	private int textColor;
	private int inColor;
	private int outColor;
	
	public ChatAdapter(Context context, Smiles smiles) {
        super(context, R.id.chat1);
        this.context = context;
        this.smiles  = smiles;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.showtime = prefs.getBoolean("ShowTime", false);
        applyColors();
    }
	
	public void update(String jid, List<MessageItem> list) {
		this.jid = jid;
		clear();
		if (!prefs.getBoolean("ShowStatus", false)) {
        	for (int i = 0; i < list.size(); i++) {
        		MessageItem item = list.get(i);
            	if (item.getType() != MessageItem.Type.message) {
            		list.remove(item);
            	} else add(item);
            }
        } else {
        	for (int i = 0; i < list.size(); i++) {
        		MessageItem item = list.get(i);
            	add(item);
            }
        }
	}
	
	public String getJid() { return this.jid; }
	
	public int nextSearch() {
		List<Integer> list = new ArrayList<Integer>();
		Enumeration<Integer> keys = searchHash.keys();
		while(keys.hasMoreElements()) {
			list.add(keys.nextElement());
		}
		
		int idx = list.indexOf(searchCurrentPosition);
		if (idx < list.size() - 1) {
			idx++;
			searchCurrentPosition = list.get(idx);
		}
		notifyDataSetChanged();
		return searchCurrentPosition;
	}
	
	public int prevSearch() {
		List<Integer> list = new ArrayList<Integer>();
		Enumeration<Integer> keys = searchHash.keys();
		while(keys.hasMoreElements()) {
			list.add(keys.nextElement());
		}
		
		int idx = list.indexOf(searchCurrentPosition);
		if (idx > 0) {
			idx--;
			searchCurrentPosition = list.get(idx);
		}
		notifyDataSetChanged();
		return searchCurrentPosition;
	}
	
	public void search(String search) {
		this.searchString = search;
		searchHash.clear();
		if (search.length() > 0) {
			int count = getCount();
			for (int i = 0; i < count; i++) {
				MessageItem item = getItem(i);
				String name = item.getName();
				String body = item.getBody();
				MessageItem.Type type = item.getType();
				String time = "(" + item.getTime() + ")";
				if (type == MessageItem.Type.status) {
					if (showtime) body = time + "  " + body;
				} else {
					if (showtime) body = time + " " + name + ": " + body;
		        	else body = name + ": " + body;
				}
				
				List<Integer> list = new ArrayList<Integer>();
	        	int from = 0;
	   	        int start;
	   	        while ((start = body.toLowerCase().indexOf(search.toLowerCase(), from)) != -1) {
	   	            from = start + search.length();
	   	            list.add(start);
	   	        }
	   	        if (list.size() > 0) {
	   	        	searchHash.put(i, list);
	   	        	searchCurrentPosition = i;
	   	        }
			}
		} else {
			searchCurrentPosition = -1;
		}
		notifyDataSetChanged();
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		boolean enableCollapse = prefs.getBoolean("EnableCollapseMessages", true);
		int fontSize = Integer.parseInt(context.getResources().getString(R.string.DefaultFontSize));
		try {
			fontSize = Integer.parseInt(prefs.getString("FontSize", context.getResources().getString(R.string.DefaultFontSize)));
		} catch (NumberFormatException e) {	}
		
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.chat_item, null, false);
        }

        final MessageItem item = getItem(position);
        String subj = "";
        String body = item.getBody();
        String name = item.getName();
        MessageItem.Type type = item.getType();
        String nick = item.getName();
        final boolean collapsed = item.isCollapsed();
        boolean received = item.isReceived();
        String t = "(" + item.getTime() + ")";
        if (item.getSubject().length() > 0) subj = "\n" + context.getString(R.string.Subject) + ": " + item.getSubject() + "\n";
        body = subj + body;
        
//        String id = item.getId();
//        Cursor cursor = context.getContentResolver().query(JTalkProvider.CONTENT_URI, null, "jid = '" + jid + "' AND id = '" + id + "'", null, MessageDbHelper._ID);
//        if (cursor != null && cursor.getCount() > 0) {
//        	cursor.moveToLast();
//        	received = Boolean.valueOf(cursor.getString(cursor.getColumnIndex(MessageDbHelper.RECEIVED)));
//        }
        
        String message;
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        if (type == MessageItem.Type.status) {
        	if (showtime) message = t + "  " + body;
        	else message = body;
        	ssb.append(message);
        	ssb.setSpan(new ForegroundColorSpan(0xFF239923), 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
        	int colorLength = name.length();
        	int boldLength = colorLength;
        	
        	if (showtime) {
        		message = t + " " + name + ": " + body; 
        		colorLength = name.length() + t.length() + 1;
        		boldLength = name.length() + t.length() + subj.length() + 2;
        	}
        	else message = name + ": " + body;
        	ssb.append(message);
        	ssb.setSpan(new ForegroundColorSpan(textColor), 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        	ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, boldLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (!nick.equals(context.getResources().getString(R.string.Me)))
            	ssb.setSpan(new ForegroundColorSpan(inColor), 0, colorLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            else {
            	if (received) ssb.setSpan(new ForegroundColorSpan(outColor), 0, colorLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            	else ssb.setSpan(new ForegroundColorSpan(textColor), 0, colorLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            
            if (item.isEdited()) ssb.setSpan(new ForegroundColorSpan(inColor), colorLength + 1, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
     // Search...
        if (searchHash.containsKey(position)) {
        	List<Integer> list = searchHash.get(position);
        	for (int start : list) {
        		int backcolor = Constants.SEARCH_BACKGROUND;
        		if (searchCurrentPosition == position ) backcolor = Constants.SEARCH_ACTIVE_BACKGROUND;
        		ssb.setSpan(new BackgroundColorSpan(backcolor), start, start + searchString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
   	            ssb.setSpan(new ForegroundColorSpan(Constants.SEARCH_FOREGROUND), start, start + searchString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        	}
        }

        LinearLayout linear = (LinearLayout) convertView.findViewById(R.id.chat_item);
        linear.setMinimumHeight(Integer.parseInt(prefs.getString("SmilesSize", "24")));
        
        final ImageView expand = (ImageView) convertView.findViewById(R.id.expand);
        final MyTextView textView = (MyTextView) convertView.findViewById(R.id.chat1);
        textView.setOnTextLinkClickListener(this);
        textView.setLinkTextColor(linkColor);
        if (enableCollapse) {
        	textView.setOnTouchListener(new OnTouchListener() {
            	View oldView = null;
    			public boolean onTouch(View view, MotionEvent event) {
    				switch (event.getAction()) {
    	    			case MotionEvent.ACTION_DOWN:
    	    				if (!firstClick) {
    	    					oldView = view;
    	    					firstClick = true;
    	    					doubleClickTimer.purge();
    	    					doubleClickTimer.cancel();
    	    					doubleClickTimer = new Timer();
    	    					doubleClickTimer.schedule(new TimerTask(){
    	    						@Override
    	    						public void run() {
    	    							firstClick = false;
    	    						}
    	    					}, 500);
    	    				} else {
    	    					firstClick = false;
    	    					if (oldView != null && oldView.equals(view)) {
    		    					if (item.isCollapsed()) {
    		    						item.setCollapsed(false);
    		    						textView.setSingleLine(false);
    		    						expand.setVisibility(View.GONE);
    		    					} else {
    		    						item.setCollapsed(true);
    		    						textView.setSingleLine(true);
    		    						expand.setVisibility(View.VISIBLE);
    		    					}
    	    					}
    	    				}
    	    				break;
    	    			default:
    	    				break;
    				}
    				return false;			
    			}
            });
        }
        
        if (collapsed && enableCollapse) {
        	textView.setSingleLine(true);
        	expand.setVisibility(View.VISIBLE);
        } else {
        	textView.setSingleLine(false);
        	expand.setVisibility(View.GONE);
        }
        
        if (prefs.getBoolean("ShowSmiles", true)) {
        	int startPosition = message.length() - body.length();
        	ssb = smiles.parseSmiles(ssb, startPosition);
        }
        
        if (jid.equals(Constants.JUICK) || jid.equals(Constants.JUBO)) textView.setTextWithLinks(ssb, MyTextView.Mode.juick);
        else if (jid.equals(Constants.PSTO)) textView.setTextWithLinks(ssb, MyTextView.Mode.psto);
        else textView.setTextWithLinks(ssb);
        
        MovementMethod m = textView.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            if (textView.getLinksClickable()) {
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
        
        textView.setTextSize(fontSize);

        if (item.isSelected()) convertView.setBackgroundColor(prefs.getBoolean("DarkColors", false) ? 0x77525252 : 0xFFDDDDDD);
        else convertView.setBackgroundColor(0X00000000);
        return convertView;
    }
	
	public void onTextLinkClick(View textView, String s) {
		if (s.length() > 1) {
			if (s.substring(0, 1).equals("@") || s.substring(0, 1).equals("#")) {
				new JuickMessageMenuDialog(context, s).show();
			} else {
				int idx = s.indexOf(":");
				if (idx > 0) {
					String scheme = s.substring(0, idx).toLowerCase();
					String path = s.substring(idx);
					Uri uri = Uri.parse(scheme + path);
					if (uri != null) {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(uri);
						context.startActivity(intent);
					}
				}
			}
		}
	}
	
	private void applyColors() {
		if (prefs.getBoolean("DarkColors", false)) {
        	textColor = 0xFFEEEEEE;
        	linkColor = 0xFF5180b7;
        	inColor = 0xFFAA2323;
        	outColor = 0xFF5180b7;
        }
		else {
			textColor = 0xFF232323;
			linkColor = 0xFF2323AA;
		    inColor = 0xFFAA2323;
		    outColor = 0xFF2323AA;
		}
	}
}
