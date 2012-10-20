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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.activity.CommandsActivity;
import net.ustyugov.jtalk.adapter.ResourceAdapter;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;

import com.jtalk2.R;

public class RosterDialogs {
	
	public static void changeStatusDialog(Activity a, final String to) {
		final JTalkService service = JTalkService.getInstance();
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(a);
		String[] statusArray = a.getResources().getStringArray(R.array.statusArray);
		final int selection = prefs.getInt("currentSelection", Constants.STATUS_ONLINE);
		
		LayoutInflater inflater = a.getLayoutInflater();
		View layout = inflater.inflate(R.layout.set_status_dialog, (ViewGroup) a.findViewById(R.id.set_status_linear));
	    
	    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(a, android.R.layout.simple_spinner_item, statusArray);
	    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    
	    final EditText statusMsg = (EditText) layout.findViewById(R.id.msg);
	    statusMsg.setText(prefs.getString("currentStatus", ""));
	    
	    final Spinner spinner = (Spinner) layout.findViewById(R.id.statusSwitcher);
	    spinner.setAdapter(arrayAdapter);
	    spinner.setSelection(selection);
	    spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int index, long id) {
				String mode = (String) getMode(index);
				statusMsg.setText(prefs.getString("lastStatus"+mode, ""));
			}
			public void onNothingSelected(AdapterView<?> arg0) { }
	    });
	    
	    final EditText priorityText = (EditText) layout.findViewById(R.id.priority);
	    priorityText.setText(prefs.getInt("currentPriority", 0)+"");
		
		AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setView(layout);
		builder.setTitle(a.getString(R.string.Status));
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				int priority;
				try {
					priority = Integer.parseInt(priorityText.getText().toString());
				} catch (NumberFormatException e) {
					priority = 0;
				}
				int pos = spinner.getSelectedItemPosition();
				String mode = (String) getMode(pos);
				String text = statusMsg.getText().toString();
				
				if (service.isStarted()) {
					if(service.isAuthenticated()) {
		           		if (to == null) {
		           			service.sendPresence(text, mode, priority);
		           		} else {
		           			service.sendPresenceTo(to, text, mode, priority);
		           		}
		           	} else {
		           		service.setPreference("currentPriority", priority);
		        		service.setPreference("currentSelection", pos);
		        		service.setPreference("currentMode", mode);
		        		service.setPreference("currentStatus", text);
		        		service.setPreference("lastStatus"+mode, text);
		           		service.connect();
		           	}
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
	
	public static void addDialog(Activity a, String jid) {
		final JTalkService service = JTalkService.getInstance();
		
		LayoutInflater inflater = a.getLayoutInflater();
		View layout = inflater.inflate(R.layout.add_contact_dialog, (ViewGroup) a.findViewById(R.id.add_contact_linear));
	    
	    final EditText jidEdit = (EditText) layout.findViewById(R.id.add_dialog_jid_entry);
	    if (jid != null) jidEdit.setText(jid);
	    final EditText nameEdit = (EditText) layout.findViewById(R.id.add_dialog_name_entry);
	    final AutoCompleteTextView groupEdit = (AutoCompleteTextView) layout.findViewById(R.id.add_dialog_group_entry);
	    
		AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setView(layout);
		builder.setTitle(a.getString(R.string.Add));
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String group = groupEdit.getText().toString();
				String name = nameEdit.getText().toString();
				String jid = jidEdit.getText().toString();
				
				if (jid.length() > 0) {
					if (name.length() <= 0) name = jid;
					if (group.length() <=0) group = null;
					if (service != null && service.isAuthenticated()) {
						service.addContact(jid, name, group);
					}
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
	
	public static void editDialog(Activity a, final String jid, String name, String group) {
		final JTalkService service = JTalkService.getInstance();
		
		LayoutInflater inflater = a.getLayoutInflater();
		View layout = inflater.inflate(R.layout.edit_contact_dialog, (ViewGroup) a.findViewById(R.id.edit_contact_linear));
	    
		EditText jidEdit = (EditText) layout.findViewById(R.id.edit_dialog_jid_entry);
		jidEdit.setText(jid);
		
	    final EditText nameEdit = (EditText) layout.findViewById(R.id.edit_dialog_name_entry);
	    nameEdit.setText(name);
	    
	    List<String> groups = new ArrayList<String>();
	    Collection<RosterGroup> col = service.getRoster().getGroups();
	    for(RosterGroup rg : col) {
	    	groups.add(rg.getName());
	    }
	    
	    String[] array = new String[groups.size()];
	    for (int i = 0; i < groups.size(); i++) array[i] = groups.get(i);
	    
	    ArrayAdapter<String> adapter = new ArrayAdapter<String>(a, android.R.layout.simple_list_item_1, array);
	    
	    final AutoCompleteTextView groupEdit = (AutoCompleteTextView) layout.findViewById(R.id.edit_dialog_group_entry);
	    groupEdit.setText(group);
	    groupEdit.setAdapter(adapter);
	    
	    
		AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setView(layout);
		builder.setTitle(a.getString(R.string.Add));
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String group = groupEdit.getText().toString();
				String name = nameEdit.getText().toString();
				
				if (name.length() <= 0) name = jid;
				if (group.length() <=0) group = null;
				if (service != null && service.isAuthenticated()) {
					JTalkService.getInstance().addContact(jid, name, group);
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
	
	public static void renameGroupDialog(final Activity activity, String group) {
		final RosterGroup rg = JTalkService.getInstance().getRoster().getGroup(group);
		if (rg != null) {
			final String oldname = rg.getName();
			
			LayoutInflater inflater = activity.getLayoutInflater();
			View layout = inflater.inflate(R.layout.set_nick_dialog, (ViewGroup) activity.findViewById(R.id.set_nick_linear));
			
			final EditText groupEdit = (EditText) layout.findViewById(R.id.nick_edit);
		    groupEdit.setText(oldname);
		    
		    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setView(layout);
			builder.setTitle(activity.getString(R.string.Rename));
			builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String newname = groupEdit.getText().toString();
					if(newname.length() > 0 && !newname.equals(oldname)) {
							rg.setName(newname);
							Intent i = new Intent(net.ustyugov.jtalk.Constants.UPDATE);
							activity.sendBroadcast(i);
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
	
	public static void subscribtionDialog(Activity activity, final String jid) {
		final JTalkService service = JTalkService.getInstance();
		
		CharSequence[] items = new CharSequence[3];
        items[0] = service.getResources().getString(R.string.Request);
        items[1] = service.getResources().getString(R.string.Allow);
        items[2] = service.getResources().getString(R.string.Remove);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.Subscribtion);
        builder.setItems(items, new OnClickListener() {
        	@Override
        	public void onClick(DialogInterface dialog, int which) {
        		Presence presence = new Presence(Presence.Type.subscribe);
        		presence.setTo(jid);
        		switch(which) {
        			case 0:
        				presence.setType(Presence.Type.subscribe);
        				break;
        			case 1:
        				presence.setType(Presence.Type.subscribed);
        				break;
        			case 2:
        				presence.setType(Presence.Type.unsubscribed);
        				break;
        		}
        		if (service != null && service.isAuthenticated()) {
        			service.sendPacket(presence);
        		}
        	}
        });
        builder.create().show();
	}
	
	public static void resourceDialog(final Activity activity, final String jid) {
		JTalkService service = JTalkService.getInstance();
		final List<String> list = new ArrayList<String>();
		
		int slash = jid.lastIndexOf("/");
		if (slash == -1) {
			Iterator<Presence> it =  service.getRoster().getPresences(jid);
			while (it.hasNext()) {
				Presence p = it.next();
				if (p.isAvailable()) list.add(StringUtils.parseResource(p.getFrom()));
			}
			
			if (!list.isEmpty()) {
				ResourceAdapter adapter = new ResourceAdapter(activity, jid, list);

		        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		        builder.setTitle(activity.getString(R.string.SelectResource));
		        builder.setAdapter(adapter, new OnClickListener() {
		        	@Override
		        	public void onClick(DialogInterface dialog, int which) {
	        			String resource = list.get(which);
	        			Intent intent = new Intent(activity, CommandsActivity.class);
	        			intent.putExtra("jid", jid + "/" + resource);
	        	        activity.startActivity(intent);
		        	}
		        });
		        builder.create().show();
			}
		}
	}
	
	private static String getMode(int position) {
  		String mode = null;

  		switch(position) {
  		case Constants.STATUS_ONLINE:
  			mode = "available";
  			break;
  		case Constants.STATUS_AWAY:
  			mode = "away";
  			break;
  		case Constants.STATUS_E_AWAY:
  			mode = "xa";
  			break;
  		case Constants.STATUS_DND:
  			mode = "dnd";
  			break;
  		case Constants.STATUS_FREE:
  			mode = "chat";
  			break;
  		case Constants.STATUS_OFFLINE:
  			mode = "unavailable";
  			break;
  		}
  		return mode;
  	}
}
