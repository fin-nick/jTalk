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
import net.ustyugov.jtalk.IgnoreList;
import net.ustyugov.jtalk.RosterItem;
import net.ustyugov.jtalk.activity.CommandsActivity;
import net.ustyugov.jtalk.activity.PrivacyListsActivity;
import net.ustyugov.jtalk.activity.filetransfer.SendFileActivity;
import net.ustyugov.jtalk.activity.vcard.SetVcardActivity;
import net.ustyugov.jtalk.activity.vcard.VCardActivity;
import net.ustyugov.jtalk.adapter.ResourceAdapter;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
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
	
	public static void changeStatusDialog(final Activity a, String acc, final String to) {
		final JTalkService service = JTalkService.getInstance();
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(a);
		String[] statusArray = a.getResources().getStringArray(R.array.statusArray);
		final int selection = prefs.getInt("currentSelection", Constants.STATUS_ONLINE);
		
		LayoutInflater inflater = a.getLayoutInflater();
		View layout = inflater.inflate(R.layout.set_status_dialog, (ViewGroup) a.findViewById(R.id.set_status_linear));
	    
		final Spinner accountsSpinner = (Spinner) layout.findViewById(R.id.accounts);
		List<String> accounts = new ArrayList<String>();
		if (to == null || to.length() < 1) {
			accounts.add("All");
            Cursor cursor = a.getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '" + 1 + "'", null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    String jid = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID));
                    accounts.add(jid);
                } while (cursor.moveToNext());
                cursor.close();
            }
		} else {
			accounts.add(acc);
			accountsSpinner.setVisibility(View.GONE);
		}
		
		ArrayAdapter<String> accountsAdapter = new ArrayAdapter<String>(a, android.R.layout.simple_spinner_item, accounts);
	    accountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		accountsSpinner.setAdapter(accountsAdapter);
		
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
				String mode = getMode(index);
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
				String mode = getMode(pos);
				String text = statusMsg.getText().toString();
				String account;
				account = (String) accountsSpinner.getSelectedItem();
				if (account != null && !account.equals("All")) {
					if (service.isAuthenticated(account)) {
						if (to == null) {
		           			service.sendPresence(account, text, mode, priority);
		           		} else {
		           			service.sendPresenceTo(account, to, text, mode, priority);
		           		}
					} else {
		           		service.connect(account);
					}
				} else {
					service.setPreference("currentPriority", priority);
	        		service.setPreference("currentSelection", pos);
	        		service.setPreference("currentMode", mode);
	        		service.setPreference("currentStatus", text);
	        		service.setPreference("lastStatus"+mode, text);
	        		
					Cursor cursor = service.getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '" + 1 + "'", null, null);
					if (cursor != null && cursor.getCount() > 0) {
						cursor.moveToFirst();
						do {
							String acc = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID)).trim();
							if (service.isAuthenticated(acc)) {
								service.sendPresence(acc, text, mode, priority);
							} else {
				           		service.connect(acc);
							}
						} while(cursor.moveToNext());
						cursor.close();
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
		View layout = inflater.inflate(R.layout.add_contact_dialog, (ViewGroup) a.findViewById(R.id.linear));
	    
		List<String> accounts = new ArrayList<String>();
		for(XMPPConnection connection : service.getAllConnections()) {
			accounts.add(StringUtils.parseBareAddress(connection.getUser()));
		}
		
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(a, android.R.layout.simple_spinner_item, accounts);
	    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		final Spinner spinner = (Spinner) layout.findViewById(R.id.accounts);
		spinner.setAdapter(arrayAdapter);
		
	    final EditText jidEdit = (EditText) layout.findViewById(R.id.jid_entry);
	    if (jid != null) jidEdit.setText(jid);
	    final EditText nameEdit = (EditText) layout.findViewById(R.id.name_entry);
	    final AutoCompleteTextView groupEdit = (AutoCompleteTextView) layout.findViewById(R.id.group_entry);
	    
		AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setView(layout);
		builder.setTitle(a.getString(R.string.Add));
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String account = (String) spinner.getSelectedItem();
				String group = groupEdit.getText().toString();
				String name = nameEdit.getText().toString();
				String jid = jidEdit.getText().toString();
				
				if (jid.length() > 0) {
					if (name.length() <= 0) name = jid;
					if (group.length() <= 0) group = null;
					if (service.isAuthenticated()) {
						service.addContact(account, jid, name, group);
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
	
	public static void editDialog(Activity a, String account, final String jid, String name, String group) {
		final JTalkService service = JTalkService.getInstance();
		
		LayoutInflater inflater = a.getLayoutInflater();
		View layout = inflater.inflate(R.layout.add_contact_dialog, (ViewGroup) a.findViewById(R.id.linear));
	    
		List<String> connections = new ArrayList<String>();
		for(XMPPConnection connection : service.getAllConnections()) {
			connections.add(StringUtils.parseBareAddress(connection.getUser()));
		}
		
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(a, android.R.layout.simple_spinner_item, connections);
	    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		final Spinner spinner = (Spinner) layout.findViewById(R.id.accounts);
		spinner.setAdapter(arrayAdapter);
		spinner.setSelection(arrayAdapter.getPosition(account));
		spinner.setEnabled(false);
		
		EditText jidEdit = (EditText) layout.findViewById(R.id.jid_entry);
		jidEdit.setText(jid);
		
	    final EditText nameEdit = (EditText) layout.findViewById(R.id.name_entry);
	    nameEdit.setText(name);
	    
	    List<String> groups = new ArrayList<String>();
	    Collection<RosterGroup> col = service.getRoster(account).getGroups();
	    for(RosterGroup rg : col) {
	    	groups.add(rg.getName());
	    }
	    
	    ArrayAdapter<String> adapter = new ArrayAdapter<String>(a, android.R.layout.simple_list_item_1, groups);
	    
	    final AutoCompleteTextView groupEdit = (AutoCompleteTextView) layout.findViewById(R.id.group_entry);
	    groupEdit.setText(group);
	    groupEdit.setAdapter(adapter);
	    
	    
		AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setView(layout);
		builder.setTitle(a.getString(R.string.Edit));
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String account = (String) spinner.getSelectedItem();
				String group = groupEdit.getText().toString();
				String name = nameEdit.getText().toString();
				
				if (name.length() <= 0) name = jid;
				if (group.length() <= 0) group = null;
				if (service.isAuthenticated(account)) {
					JTalkService.getInstance().addContact(account, jid, name, group);
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
	
	public static void renameGroupDialog(final Activity activity, final String account, String group) {
		final RosterGroup rg = JTalkService.getInstance().getRoster(account).getGroup(group);
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
							Intent i = new Intent(Constants.UPDATE);
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
	
	public static void subscribtionDialog(Activity activity, final String account, final String jid) {
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
        		if (service.isAuthenticated()) {
        			service.sendPacket(account, presence);
        		}
        	}
        });
        builder.create().show();
	}
	
	public static void resourceDialog(final Activity activity, final String account, final String jid) {
		JTalkService service = JTalkService.getInstance();
		final List<String> list = new ArrayList<String>();
		
		int slash = jid.lastIndexOf("/");
		if (slash == -1) {
			Iterator<Presence> it =  service.getRoster(account).getPresences(jid);
			while (it.hasNext()) {
				Presence p = it.next();
				if (p.isAvailable()) list.add(StringUtils.parseResource(p.getFrom()));
			}
			
			if (!list.isEmpty()) {
				ResourceAdapter adapter = new ResourceAdapter(activity, account, jid, list);

		        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		        builder.setTitle(activity.getString(R.string.SelectResource));
		        builder.setAdapter(adapter, new OnClickListener() {
		        	@Override
		        	public void onClick(DialogInterface dialog, int which) {
                        String fullJid = jid;
	        			String resource = list.get(which);
                        if (resource != null && resource.length() > 0) fullJid = jid + "/" + resource;
	        			Intent intent = new Intent(activity, CommandsActivity.class);
	        			intent.putExtra("account", account);
	        			intent.putExtra("jid", fullJid);
	        	        activity.startActivity(intent);
		        	}
		        });
		        builder.create().show();
			}
		} else {
            Intent intent = new Intent(activity, CommandsActivity.class);
            intent.putExtra("account", account);
            intent.putExtra("jid", jid);
            activity.startActivity(intent);
        }
	}
	
	public static void ContactMenuDialog(final Activity activity, final RosterItem item) {
    	final JTalkService service = JTalkService.getInstance();
    	final String account = item.getAccount();
    	final RosterEntry entry = item.getEntry();
    	
    	CharSequence[] items;
    	if (service.getActiveChats(account).contains(entry.getUser())) {
    		items = new CharSequence[10];
    		items[9] = activity.getString(R.string.Close);
    	}
    	else items = new CharSequence[9];
        items[0] = activity.getString(R.string.Info);
        items[1] = activity.getString(R.string.Edit);
        items[2] = activity.getString(R.string.SendStatus);
        items[3] = activity.getString(R.string.SendFile);
        items[4] = activity.getString(R.string.Subscribtion);
        items[5] = activity.getString(R.string.AddInIgnoreList);
        items[6] = activity.getString(R.string.ExecuteCommand);
        items[7] = activity.getString(R.string.DeleteHistory);
        items[8] = activity.getString(R.string.Remove);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(entry.getUser());
        builder.setItems(items, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String jid = entry.getUser();
		    	String name = entry.getName();
		    	String group = activity.getResources().getString(R.string.Nogroup);
		    	if (!entry.getGroups().isEmpty()) {
		    		Iterator<RosterGroup> it = entry.getGroups().iterator();
		    		if (it.hasNext()) group = it.next().getName();
		    	}
		    	
		    	switch (which) {
	        	case 0:
	        		Intent infoIntent = new Intent(activity, VCardActivity.class);
	        		infoIntent.putExtra("jid", jid);
	        		infoIntent.putExtra("account", account);
	        		activity.startActivity(infoIntent);
	        		break;
	        	case 1:
		         	editDialog(activity, account, jid, name, group);
		         	break;
	        	case 2:
	        		changeStatusDialog(activity, account, jid);
	        		break;
	        	case 3:
		        	 Intent intent = new Intent(activity, SendFileActivity.class);
		        	 intent.putExtra("account", account);
		        	 intent.putExtra("jid", jid);
		        	 activity.startActivity(intent);
		 	        break;
	        	case 4:
		        	 subscribtionDialog(activity, account, jid);
		        	 break;
	        	case 5:
		        	 new IgnoreList(account).updateIgnoreList(jid);
		        	 break;
	        	case 6:
		        	 RosterDialogs.resourceDialog(activity, account, jid);
		        	 break;
	        	case 7:
	        		activity.getContentResolver().delete(JTalkProvider.CONTENT_URI, "jid = '" + jid + "'", null);
	  	    		service.removeActiveChat(account, jid);
	  	    		service.sendBroadcast(new Intent(Constants.UPDATE));
	  	    		break;
	        	case 8:
				    service.removeContact(account, jid);
				    Intent i = new Intent(Constants.UPDATE);
		         	activity.sendBroadcast(i);
		 	        break;
	        	case 9:
	        		service.setChatState(account, jid, ChatState.gone);
		        	service.removeActiveChat(account, jid);
					if (service.getCurrentJid().equals(jid)) service.sendBroadcast(new Intent(Constants.FINISH));
					else service.sendBroadcast(new Intent(Constants.UPDATE));
		        	break;
		    	}
			}
        });
        builder.create().show();
    }

    public static void PrivateMenuDialog(final Activity activity, final RosterItem item) {
        final JTalkService service = JTalkService.getInstance();
        final String account = item.getAccount();
        final RosterEntry entry = item.getEntry();

        CharSequence[] items;
        if (service.getActiveChats(account).contains(entry.getUser())) {
            items = new CharSequence[6];
            items[5] = activity.getString(R.string.Close);
        }
        else items = new CharSequence[5];
        items[0] = activity.getString(R.string.Info);
        items[1] = activity.getString(R.string.SendFile);
        items[2] = activity.getString(R.string.AddInIgnoreList);
        items[3] = activity.getString(R.string.ExecuteCommand);
        items[4] = activity.getString(R.string.DeleteHistory);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(entry.getUser());
        builder.setItems(items, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String jid = entry.getUser();
                String name = entry.getName();

                switch (which) {
                    case 0:
                        Intent infoIntent = new Intent(activity, VCardActivity.class);
                        infoIntent.putExtra("jid", jid);
                        infoIntent.putExtra("account", account);
                        activity.startActivity(infoIntent);
                        break;
                    case 1:
                        Intent intent = new Intent(activity, SendFileActivity.class);
                        intent.putExtra("account", account);
                        intent.putExtra("jid", jid);
                        activity.startActivity(intent);
                        break;
                    case 2:
                        new IgnoreList(account).updateIgnoreList(jid);
                        break;
                    case 3:
                        Intent com = new Intent(activity, CommandsActivity.class);
                        com.putExtra("account", account);
                        com.putExtra("jid", jid + "/" + name);
                        activity.startActivity(com);
                        break;
                    case 4:
                        activity.getContentResolver().delete(JTalkProvider.CONTENT_URI, "jid = '" + jid + "'", null);
                        service.removeActiveChat(account, jid);
                        service.sendBroadcast(new Intent(Constants.UPDATE));
                        break;
                    case 5:
                        service.setChatState(account, jid, ChatState.gone);
                        service.removeActiveChat(account, jid);
                        if (service.getCurrentJid().equals(jid)) service.sendBroadcast(new Intent(Constants.FINISH));
                        else service.sendBroadcast(new Intent(Constants.UPDATE));
                        break;
                }
            }
        });
        builder.create().show();
    }
	
	public static void MucContactMenuDialog(final Activity activity, final String account, final String group, final String nick) {
    	final JTalkService service = JTalkService.getInstance();
		final MultiUserChat muc = service.getConferencesHash(account).get(group);
		
		CharSequence[] items = new CharSequence[3];
		items[0] = activity.getString(R.string.Info);
		items[1] = activity.getString(R.string.ExecuteCommand);
		items[2] = activity.getString(R.string.Actions);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.Actions);
        builder.setItems(items, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
		        switch(which) {
		        	case 0:
		        		Intent infoIntent = new Intent(activity, VCardActivity.class);
		        		infoIntent.putExtra("jid", group + "/" + nick);
		        		activity.startActivity(infoIntent);
		        		break;
		        	case 1:
		        		Intent intent = new Intent(activity, CommandsActivity.class);
		    			intent.putExtra("jid", group + "/" + nick);
		    	        activity.startActivity(intent);
		        		break;
		        	case 2:
		        		new MucAdminMenu(activity, muc, nick).show();
		        		break;
		        }
			}
        });
        builder.create().show();
    }
	
	public static void SelfContactMenuDialog(final Activity activity, final RosterItem item) {
    	final JTalkService service = JTalkService.getInstance();
    	final String account = item.getAccount();
    	final RosterEntry entry = item.getEntry();
    	
    	CharSequence[] items;
    	if (service.getActiveChats(account).contains(entry.getUser())) {
    		items = new CharSequence[5];
    		items[4] = activity.getString(R.string.Close);
    	}
    	else items = new CharSequence[4];
        items[0] = activity.getString(R.string.Info);
        items[1] = activity.getString(R.string.SendFile);
        items[2] = activity.getString(R.string.ExecuteCommand);
        items[3] = activity.getString(R.string.DeleteHistory);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(entry.getUser());
        builder.setItems(items, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String jid = entry.getUser();
		    	switch (which) {
	        	case 0:
	        		Intent infoIntent = new Intent(activity, VCardActivity.class);
	        		infoIntent.putExtra("jid", StringUtils.parseBareAddress(jid));
	        		infoIntent.putExtra("account", account);
	        		activity.startActivity(infoIntent);
	        		break;
	        	case 1:
	        		Intent intent = new Intent(activity, SendFileActivity.class);
	        		intent.putExtra("account", account);
	        		intent.putExtra("jid", jid);
	        		activity.startActivity(intent);
	        		break;
	        	case 2:
                    if (!jid.contains("/")) RosterDialogs.resourceDialog(activity, account, jid);
                    else {
                        Intent comIntent = new Intent(activity, CommandsActivity.class);
                        comIntent.putExtra("account", account);
                        comIntent.putExtra("jid", jid);
                        activity.startActivity(comIntent);
                    }
	        		break;
	        	case 3:
	        		activity.getContentResolver().delete(JTalkProvider.CONTENT_URI, "jid = '" + jid + "'", null);
                    service.removeActiveChat(account, jid);
	  	    		service.sendBroadcast(new Intent(Constants.UPDATE));
		 	        break;
	        	case 4:
	        		service.setChatState(account, jid, ChatState.gone);
                    service.removeActiveChat(account, jid);
					if (service.getCurrentJid().equals(jid)) service.sendBroadcast(new Intent(Constants.FINISH));
					else service.sendBroadcast(new Intent(Constants.UPDATE));
					break;
		    	}
			}
        });
        builder.create().show();
    }
	
	public static void AccountMenuDialog(final Activity activity, final RosterItem item) {
    	final String account = item.getAccount();
    	
    	CharSequence[] items = new CharSequence[2];
        items[0] = activity.getString(R.string.vcard);
        items[1] = activity.getString(R.string.PrivacyLists);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(account);
        builder.setItems(items, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
		    	switch (which) {
	        	case 0:
	        		if (JTalkService.getInstance().isAuthenticated(account)) activity.startActivity(new Intent(activity, SetVcardActivity.class).putExtra("account", account));
	        		break;
	        	case 1:
                    if (JTalkService.getInstance().isAuthenticated(account)) activity.startActivity(new Intent(activity, PrivacyListsActivity.class).putExtra("account", account));
		 	        break;
		    	}
			}
        });
        builder.create().show();
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
