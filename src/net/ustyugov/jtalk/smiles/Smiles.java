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

package net.ustyugov.jtalk.smiles;

import java.io.*;
import java.util.*;

import android.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.adapter.SmilesDialogAdapter;

import net.ustyugov.jtalk.smiles.MyImageSpan;
import net.ustyugov.jtalk.smiles.SmileDrawable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;

public class Smiles implements DialogInterface.OnClickListener {
    private SharedPreferences prefs;
	private Hashtable<String, List<String>> table;
	private Hashtable<String, String> smiles = new Hashtable<String, String>();
	private String path;
	private Activity activity;
	private SmilesDialogAdapter adapter;
	private int columns = 3;

	public Smiles(Activity activity) {
		prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		String pack = prefs.getString("SmilesPack", "default");
		table = new Hashtable<String, List<String>>();
		path = Constants.PATH_SMILES + pack;
		this.activity = activity;

		try {
		    columns = Integer.parseInt(prefs.getString("SmilesColumns", 3+""));
		} catch (NumberFormatException ignored) {	}

        File file = new File(path + "/icondef.xml");
        if (file.exists()) createPsiSmiles(); else createSmiles();
	}

    private void createSmiles() {
        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new FileReader(path + "/table.xml"));

            boolean end = false;
            while(!end) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("smile")) {
                        List<String> tmpList = new ArrayList<String>();
                        String file = parser.getAttributeValue("", "file");
                        do {
                            eventType = parser.next();
                            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("value")) {
                                String content = "";
                                int parserDepth = parser.getDepth();
                                while (!(parser.next() == XmlPullParser.END_TAG && parser.getDepth() == parserDepth)) {
                                    content += parser.getText();
                                }
                                tmpList.add(content);
                            }
                        }
                        while (eventType != XmlPullParser.END_TAG);
                        table.put(file, tmpList);
                    }
                } else if (eventType == XmlPullParser.END_DOCUMENT) {
                    end = true;
                }
            }
            Enumeration<String> keys = table.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                smiles.put(key, path + "/" + key);
            }
        } catch(Exception e) { }
    }

    private void createPsiSmiles() {
        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new FileReader(path + "/icondef.xml"));

            boolean end = false;
            while(!end) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("icon")) {
                        String file = "";
                        List<String> tmpList = new ArrayList<String>();
                        do {
                            eventType = parser.next();
                            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("text")) {
                                String content = "";
                                int parserDepth = parser.getDepth();
                                while (!(parser.next() == XmlPullParser.END_TAG && parser.getDepth() == parserDepth)) {
                                    content += parser.getText();
                                }
                                tmpList.add(content);
                            } else if (eventType == XmlPullParser.START_TAG && parser.getName().equals("object")) {
                                String mime = parser.getAttributeValue("", "mime");
                                int parserDepth = parser.getDepth();
                                while (!(parser.next() == XmlPullParser.END_TAG && parser.getDepth() == parserDepth)) {
                                    if (mime.startsWith("image/")) file = parser.getText();
                                }
                            }
                        }
                        while (eventType != XmlPullParser.END_TAG);
                        if (file != null && file.length() > 0) table.put(file, tmpList);
                    }
                } else if (eventType == XmlPullParser.END_DOCUMENT) {
                    end = true;
                }
            }

            Enumeration<String> keys = table.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                smiles.put(key, path + "/" + key);
            }
        } catch(Exception e) { }
    }

	public SpannableStringBuilder parseSmiles(final TextView textView, SpannableStringBuilder ssb, int startPosition) {
		String message = ssb.toString();

		Enumeration<String> keys = table.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			List<String> list = table.get(key);
			String smilePath = smiles.get(key);
			for (String s : list) {
				int start = message.indexOf(s, startPosition);
	       		while(start != -1) {
	            	ssb.setSpan(new MyImageSpan(new SmileDrawable(smilePath, new SmileDrawable.UpdateListener() {
                        @Override
                        public void update() {
                            textView.postInvalidate();
                        }
                    })), start, start + s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	                start = message.indexOf(s, start + 1);
	            }
			}
		}
		return ssb;
	}
	
	public void showDialog() {
		adapter = new SmilesDialogAdapter(activity, smiles, table);

		GridView view = new GridView(activity);
		view.setNumColumns(columns);
		view.setAdapter(adapter);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setView(view);
        final AlertDialog dialog = builder.create();

		view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String key = (String) parent.getItemAtPosition(position);
				String smile = table.get(key).get(0);

				Intent intent = new Intent(Constants.PASTE_TEXT);
				intent.putExtra("text", smile);
				activity.sendBroadcast(intent);
				dialog.dismiss();
			}
		});
		dialog.show();
	}
	
	public void onClick(DialogInterface dialog, int which) {
		String key = adapter.getItem(which);
		String smile = table.get(key).get(0);
		
		Intent intent = new Intent(Constants.PASTE_TEXT);
		intent.putExtra("text", smile);
		activity.sendBroadcast(intent);
	}
}