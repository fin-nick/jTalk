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

import java.util.Collection;

import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.packet.PrivacyItem;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import com.jtalk2.R;

public class PrivacyDialogs {
	
	public static void addListDialog(final Activity activity, final String account, PrivacyItem item, final int position) {
		final JTalkService service = JTalkService.getInstance();
		LayoutInflater inflater = activity.getLayoutInflater();
		View layout = inflater.inflate(R.layout.privacy_dialog, (ViewGroup) activity.findViewById(R.id.privacy_linear));
			
		final EditText order = (EditText) layout.findViewById(R.id.order);
		final EditText edit = (EditText) layout.findViewById(R.id.value_edit);
		final Spinner actionSpinner = (Spinner) layout.findViewById(R.id.action_spinner);
		final Spinner typeSpinner = (Spinner) layout.findViewById(R.id.type_spinner);
		final Spinner valueSpinner = (Spinner) layout.findViewById(R.id.value_spinner);
		final CheckBox checkIq = (CheckBox) layout.findViewById(R.id.check_iq);
		final CheckBox checkMsg = (CheckBox) layout.findViewById(R.id.check_msg);
		final CheckBox checkPI = (CheckBox) layout.findViewById(R.id.check_presence_in);
		final CheckBox checkPO = (CheckBox) layout.findViewById(R.id.check_presence_out);
		
		typeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
				String type = (String) parent.getItemAtPosition(position);
				if (type.equals("jid")) {
					edit.setVisibility(View.VISIBLE);
					valueSpinner.setVisibility(View.GONE);
				} else {
					edit.setVisibility(View.GONE);
					valueSpinner.setVisibility(View.VISIBLE);
					if (type.equals("group")) {
						Collection<RosterGroup> coll = service.getRoster(account).getGroups();
						String[] array = new String[coll.size()];
						int i = 0;
						for (RosterGroup group : coll) {
							array[i] = group.getName();
							i++;
						}
						
						ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, array);
					    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						
						valueSpinner.setAdapter(arrayAdapter);
						valueSpinner.setSelection(0);
					} else {
						String[] array = new String[4];
						array[0] = "none";
						array[1] = "from";
						array[2] = "to";
						array[3] = "both";
						ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, array);
					    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						valueSpinner.setAdapter(arrayAdapter);
						valueSpinner.setSelection(0);
					}
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		if (item != null) {
			order.setText(item.getOrder()+"");
			if (item.isAllow()) actionSpinner.setSelection(0); else actionSpinner.setSelection(1);
			if (item.getType() == PrivacyItem.Type.jid) {
				typeSpinner.setSelection(0);
				edit.setText(item.getValue());
			}
			else if (item.getType() == PrivacyItem.Type.subscription) {
				typeSpinner.setSelection(2);
				if (item.getValue().equals("none")) valueSpinner.setSelection(0);
				else if (item.getValue().equals("from")) valueSpinner.setSelection(1);
				else if (item.getValue().equals("to")) valueSpinner.setSelection(2);
				else valueSpinner.setSelection(3);
			} else {
				typeSpinner.setSelection(1);
//				String group = item.getValue();
//				valueSpinner.setSelection(valueSpinner.getAdapter().
			}
			checkIq.setChecked(item.isFilterIQ());
			checkMsg.setChecked(item.isFilterMessage());
			checkPI.setChecked(item.isFilterPresence_in());
			checkPO.setChecked(item.isFilterPresence_out());
		}
		    
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setView(layout);
		builder.setTitle(activity.getString(R.string.Add));
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String o = order.getText().toString();
				if (o == null || o.length() < 1) o = "0";
				
				String allow = (String) actionSpinner.getSelectedItem();
				String type = (String) typeSpinner.getSelectedItem();
				
				String value;
				if (type.equals("jid")) value = edit.getText().toString();
				else value = (String) valueSpinner.getSelectedItem();
				
				Intent intent = new Intent("com.jtalk.privacy.add");
				intent.putExtra("position", position);
				intent.putExtra("order", o);
				intent.putExtra("type", type);
				intent.putExtra("value", value);
				if (allow.equals("allow")) intent.putExtra("allow", true); else intent.putExtra("allow", false);
				intent.putExtra("iq", checkIq.isChecked());
				intent.putExtra("msg", checkMsg.isChecked());
				intent.putExtra("presence_in", checkPI.isChecked());
				intent.putExtra("presence_out", checkPO.isChecked());
				
				activity.sendBroadcast(intent);
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
