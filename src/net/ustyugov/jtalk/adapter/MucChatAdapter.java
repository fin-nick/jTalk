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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.MessageItem;
import net.ustyugov.jtalk.Smiles;
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
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class MucChatAdapter extends ArrayAdapter<MessageItem> implements TextLinkClickListener {
    private int searchCurrentPosition = 0;
    private String searchString = "";
    private Hashtable<Integer, List<Integer>> searchHash = new Hashtable<Integer, List<Integer>>();

    private Context context;
    private Smiles smiles;
    private String nick;
    private boolean firstClick = false;
    private boolean showtime = false;
    private Timer doubleClickTimer = new Timer();

    private SharedPreferences prefs;
    private int linkColor;
    private int textColor;
    private int inColor;
    private int outColor;

    public MucChatAdapter(Context context, String group, String nick, Smiles smiles) {
        super(context, R.id.chat1);
        this.context = context;
        this.smiles = smiles;
        this.nick	 = nick;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.showtime = prefs.getBoolean("ShowTime", false);
        applyColors();
    }

    public void update(List<MessageItem> list) {
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
                String n    = item.getName();
                String body = item.getBody();
                MessageItem.Type type = item.getType();
                String time = item.getTime();
                String t = null;
                if (time != null) {
                    t = "(" + time + ")";
                    if (showtime && t.length() > 2) name = t + " " + name;
                }

                String message;
                if (type == MessageItem.Type.status) message = name + " " + body;
                else {
                    if (body.length() > 4 && body.substring(0, 3).equals("/me")) {
                        if (showtime && t.length() > 2) message = t + " * " + n + " " + body.substring(3);
                        else message = " * " + n + " " + body.substring(3);
                    } else {
                        message = name + ": " + body;
                    }
                }

                List<Integer> list = new ArrayList<Integer>();
                int from = 0;
                int start;
                while ((start = message.toLowerCase().indexOf(search.toLowerCase(), from)) != -1) {
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
    public View getView(int position, View convertView, ViewGroup parent) {
        boolean enableCollapse = prefs.getBoolean("EnableCollapseMessages", true);
        int fontSize = Integer.parseInt(context.getResources().getString(R.string.DefaultFontSize));
        try {
            fontSize = Integer.parseInt(prefs.getString("FontSize", context.getResources().getString(R.string.DefaultFontSize)));
        } catch (NumberFormatException e) {	}

        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.chat_item, null);
        }

        final MessageItem item = getItem(position);
        String time = item.getTime();
        String body = item.getBody();
        String name = item.getName();
        String n    = item.getName();
        MessageItem.Type type = item.getType();
        final boolean collapsed = item.isCollapsed();
        String t = null;
        if (time != null) {
            t = "(" + time + ")";
            if (showtime && t.length() > 2) name = t + " " + name;
        }

        String message;
        if (type == MessageItem.Type.status) message = name + " " + body;
        else {
            if (body.length() > 4 && body.substring(0, 3).equals("/me")) {
                if (showtime && t.length() > 2) message = t + " * " + n + " " + body.substring(3);
                else message = " * " + n + " " + body.substring(3);
            } else {
                message = name + ": " + body;
            }
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append(message);
        if (type == MessageItem.Type.status) {
            ssb.setSpan(new ForegroundColorSpan(0xFF239923), 0, message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            if (showtime && t.length() > 2) {
                ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, t.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (n.equals(nick)) {
                int idx = message.indexOf(n);
                ssb.setSpan(new ForegroundColorSpan(outColor), 0, message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), idx, idx + n.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                int idx = message.indexOf(n);
                ssb.setSpan(new ForegroundColorSpan(inColor), idx, idx + n.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), idx, idx + n.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (nick != null) {
                int pos = 0;
                Pattern nickPattern = Pattern.compile(nick, Pattern.CASE_INSENSITIVE);
                Matcher nickMatcher = nickPattern.matcher(message);
                while (nickMatcher.find(pos)) {
                    ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0 + nickMatcher.start(), 0 + nickMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb.setSpan(new ForegroundColorSpan(outColor), 0 + nickMatcher.start(), 0 + nickMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    pos = nickMatcher.end();
                }
            }
        }

        // Search
        if (searchHash.containsKey(position)) {
            List<Integer> list = searchHash.get(position);
            for (int start : list) {
                int backcolor = Constants.SEARCH_BACKGROUND;
                if (searchCurrentPosition == position ) backcolor = Constants.SEARCH_ACTIVE_BACKGROUND;
                ssb.setSpan(new BackgroundColorSpan(backcolor), start, start + searchString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new ForegroundColorSpan(Constants.SEARCH_FOREGROUND), start, start + searchString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        if (prefs.getBoolean("ShowSmiles", true)) {
            int startPosition = message.length() - body.length();
            ssb = smiles.parseSmiles(ssb, startPosition);
        }

        LinearLayout linear = (LinearLayout) v.findViewById(R.id.chat_item);
        linear.setMinimumHeight(Integer.parseInt(prefs.getString("SmilesSize", "24")));

        final ImageView expand = (ImageView) v.findViewById(R.id.expand);
        final MyTextView t1 = (MyTextView) v.findViewById(R.id.chat1);
        t1.setTextSize(fontSize);
        t1.setOnTextLinkClickListener(this);
        t1.setTextColor(textColor);
        t1.setLinkTextColor(linkColor);
        t1.setTextWithLinks(ssb, n);
        if (enableCollapse) {
            t1.setOnTouchListener(new OnTouchListener() {
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
                                        t1.setSingleLine(false);
                                        expand.setVisibility(View.GONE);
                                    } else {
                                        item.setCollapsed(true);
                                        t1.setSingleLine(true);
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
            t1.setSingleLine(true);
            expand.setVisibility(View.VISIBLE);
        } else {
            t1.setSingleLine(false);
            expand.setVisibility(View.GONE);
        }

        MovementMethod m = t1.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            if (t1.getLinksClickable()) {
                t1.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }

        if (item.isSelected()) v.setBackgroundColor(prefs.getBoolean("DarkColors", false) ? 0x77525252 : 0xFFDDDDDD);
        else v.setBackgroundColor(0X00000000);
        return v;
    }

    public void onTextLinkClick(View textView, String s) {
        if (s.length() > 1) {
            int idx = s.indexOf(":");
            if (idx > 0) {
                Uri uri = Uri.parse(s);
                if (uri != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(uri);
                    context.startActivity(intent);
                }
            } else {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                String separator = prefs.getString("nickSeparator", ", ");

                Intent intent = new Intent(Constants.PASTE_TEXT);
                intent.putExtra("text", s + separator);
                context.sendBroadcast(intent);
            }
        }
    }

    private void applyColors() {
        if (prefs.getBoolean("DarkColors", false)) {
            textColor = 0xFFEEEEEE;
            linkColor = 0xFF5180b7;
            outColor = 0xFFAA2323;
            inColor = 0xFF5180b7;
        }
        else {
            textColor = 0xFF232323;
            linkColor = 0xFF2323AA;
            outColor = 0xFFAA2323;
            inColor = 0xFF2323AA;
        }
    }
}
