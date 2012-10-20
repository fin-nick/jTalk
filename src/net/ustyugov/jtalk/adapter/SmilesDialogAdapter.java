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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import com.jtalk2.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

public class SmilesDialogAdapter extends ArrayAdapter<String>{
	private Context context;
	private Hashtable<String,Bitmap> hash;
	
	public SmilesDialogAdapter(Context context, Hashtable<String,Bitmap> hash, Hashtable<String, List<String>> table) {
		super(context, R.layout.selector);
		this.context = context;
		this.hash = hash;
        
		Enumeration<String> keys = table.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			add(key);
		}
    }
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		String key = getItem(position);
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.smile, null);
        }
        
		ImageView icon = (ImageView) v.findViewById(R.id.smile);
		icon.setImageBitmap(hash.get(key));
        return v;
    }

}
