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

import java.util.Iterator;
import org.jivesoftware.smackx.FormField;
import com.jtalk2.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

public class OptionsSpinnerAdapter extends ArrayAdapter<FormField.Option> {
	private Context context;

	public OptionsSpinnerAdapter(Context context, Iterator<FormField.Option> options) {
		super(context, R.id.name);
		this.context = context;
		while (options.hasNext()) add(options.next());
	}
	
	private String getName(int position) {
		FormField.Option option = getItem(position);
        if (option.getLabel() == null || option.getLabel().equals("")) return option.getValue();
        else return option.getLabel();
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        TextView tv = (TextView) vi.inflate(android.R.layout.simple_spinner_item, null);
		tv.setText(getName(position));
        return tv;
    }
	
	@Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        CheckedTextView tv = (CheckedTextView) vi.inflate(android.R.layout.simple_spinner_dropdown_item, null);
		tv.setText(getName(position));
        return tv;
    }
}
