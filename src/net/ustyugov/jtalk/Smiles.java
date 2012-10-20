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

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import net.ustyugov.jtalk.adapter.SmilesDialogAdapter;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import com.jtalk2.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class Smiles implements DialogInterface.OnClickListener {
	// Smiles
	private static final String[] SMILE = {":-)", ":)", "=)"};
	private static final String[] SAD = {":-(", ":(", "=("};
	private static final String[] WINK = {";-)", ";)"};
	private static final String[] LAUGH = {":-D", ":D"};
	private static final String[] TEASE = {":-P", ":P", ":-p", ":p"};
	private static final String[] SERIOUS = {":-|", ":|", "=|"};
	private static final String[] AMAZE = {":-O", ":-o", ":o", ":O"};
	private static final String[] OO = {"O_o", "o_O", "O_O"};
	
	private Hashtable<String, List<String>> table;
	private Hashtable<String, Bitmap> smiles = new Hashtable<String, Bitmap>();
	private String path;
	private Activity activity;
	private boolean standart = false;
	private SmilesDialogAdapter adapter;
	private int columns = 3;
	
	public Smiles(Activity activity) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		String pack = prefs.getString("SmilesPack", "default");
		this.table = new Hashtable<String, List<String>>();
		this.path = Constants.PATH_SMILES + pack;
		this.activity = activity;

		try {
			this.columns = Integer.parseInt(prefs.getString("SmilesColumns", 3+""));
		} catch (NumberFormatException e) {	}
		
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		float scaleWidth = metrics.scaledDensity;
		float scaleHeight = metrics.scaledDensity;
		
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		
		int size = 24;
		try {
			size = Integer.parseInt(prefs.getString("SmilesSize", size+""));
		} catch (NumberFormatException e) {	}
		
		if (!pack.equals("default")) {
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
				standart = false;
				Enumeration<String> keys = table.keys();
				while (keys.hasMoreElements()) {
					String key = keys.nextElement();
					Bitmap smile = BitmapFactory.decodeFile(path + "/" + key);
					
					int h = smile.getHeight();
					int w = smile.getWidth();
					double k = (double)h/(double)size;
					int ws = (int) (w/k);
					
					smile = Bitmap.createScaledBitmap(smile, ws, size, true);
					
					Bitmap ss = Bitmap.createBitmap(smile, 0, 0, smile.getWidth(), smile.getHeight(), matrix, true);
					ss.setDensity(metrics.densityDpi);

					smiles.put(key, ss);
				}
				
			} catch(Exception e) {
				Log.i("Smiles", e.getLocalizedMessage() + " ");
				standart = true;
			} 
		} else standart = true;
		
		if (standart) {
			table.clear();
			smiles.clear();
			
			List<String> tmp = new ArrayList<String>();
			for (String str : SMILE) {
				tmp.add(str);
			}
			table.put("smile", tmp);
			Bitmap smile = BitmapFactory.decodeResource(activity.getResources(), R.drawable.emotion_smile);
			smile = Bitmap.createScaledBitmap(smile, size, size, true);
			Bitmap ss = Bitmap.createBitmap(smile, 0, 0, smile.getWidth(), smile.getHeight(), matrix, true);
			ss.setDensity(metrics.densityDpi);
			smiles.put("smile", ss);
			
			tmp = new ArrayList<String>();
			for (String str : SAD) {
				tmp.add(str);
			}
			smile = BitmapFactory.decodeResource(activity.getResources(), R.drawable.emotion_sad);
			smile = Bitmap.createScaledBitmap(smile, size, size, true);
			ss = Bitmap.createBitmap(smile, 0, 0, smile.getWidth(), smile.getHeight(), matrix, true);
			ss.setDensity(metrics.densityDpi);
			smiles.put("sad", ss);
			table.put("sad", tmp);
			
			tmp = new ArrayList<String>();
			for (String str : OO) {
				tmp.add(str);
			}
			smile = BitmapFactory.decodeResource(activity.getResources(), R.drawable.emotion_oo);
			smile = Bitmap.createScaledBitmap(smile, size, size, true);
			ss = Bitmap.createBitmap(smile, 0, 0, smile.getWidth(), smile.getHeight(), matrix, true);
			ss.setDensity(metrics.densityDpi);
			smiles.put("oo", ss);
			table.put("oo", tmp);
			
			tmp = new ArrayList<String>();
			for (String str : WINK) {
				tmp.add(str);
			}
			smile = BitmapFactory.decodeResource(activity.getResources(), R.drawable.emotion_wink);
			smile = Bitmap.createScaledBitmap(smile, size, size, true);
			ss = Bitmap.createBitmap(smile, 0, 0, smile.getWidth(), smile.getHeight(), matrix, true);
			ss.setDensity(metrics.densityDpi);
			smiles.put("wink", ss);
			table.put("wink", tmp);
			
			tmp = new ArrayList<String>();
			for (String str : LAUGH) {
				tmp.add(str);
			}
			smile = BitmapFactory.decodeResource(activity.getResources(), R.drawable.emotion_grin);
			smile = Bitmap.createScaledBitmap(smile, size, size, true);
			ss = Bitmap.createBitmap(smile, 0, 0, smile.getWidth(), smile.getHeight(), matrix, true);
			ss.setDensity(metrics.densityDpi);
			smiles.put("laugh", ss);
			table.put("laugh", tmp);
			
			tmp = new ArrayList<String>();
			for (String str : TEASE) {
				tmp.add(str);
			}
			smile = BitmapFactory.decodeResource(activity.getResources(), R.drawable.emotion_tease);
			smile = Bitmap.createScaledBitmap(smile, size, size, true);
			ss = Bitmap.createBitmap(smile, 0, 0, smile.getWidth(), smile.getHeight(), matrix, true);
			ss.setDensity(metrics.densityDpi);
			smiles.put("tease", ss);
			table.put("tease", tmp);
			
			tmp = new ArrayList<String>();
			for (String str : SERIOUS) {
				tmp.add(str);
			}
			smile = BitmapFactory.decodeResource(activity.getResources(), R.drawable.emotion_serious);
			smile = Bitmap.createScaledBitmap(smile, size, size, true);
			ss = Bitmap.createBitmap(smile, 0, 0, smile.getWidth(), smile.getHeight(), matrix, true);
			ss.setDensity(metrics.densityDpi);
			smiles.put("serious", ss);
			table.put("serious", tmp);
			
			tmp = new ArrayList<String>();
			for (String str : AMAZE) {
				tmp.add(str);
			}
			smile = BitmapFactory.decodeResource(activity.getResources(), R.drawable.emotion_shock);
			smile = Bitmap.createScaledBitmap(smile, size, size, true);
			ss = Bitmap.createBitmap(smile, 0, 0, smile.getWidth(), smile.getHeight(), matrix, true);
			ss.setDensity(metrics.densityDpi);
			smiles.put("amaze", ss);
			table.put("amaze", tmp);
		}
	}
		
	public SpannableStringBuilder parseSmiles(SpannableStringBuilder ssb, int startPosition) {
		String message = ssb.toString();
		
		Enumeration<String> keys = table.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			List<String> list = table.get(key);
			Bitmap smile = smiles.get(key);
			for (String s : list) {
				int start = message.indexOf(s, startPosition);
	       		while(start != -1) {
	            	ssb.setSpan(new ImageSpan(activity, smile, ImageSpan.ALIGN_BASELINE), start, start + s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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
		
		view.setOnItemClickListener(new OnItemClickListener() {
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