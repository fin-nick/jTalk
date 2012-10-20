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

package net.ustyugov.jtalk.activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.Notify;
import net.ustyugov.jtalk.RosterItem;
import net.ustyugov.jtalk.SortList;
import net.ustyugov.jtalk.activity.muc.Muc;
import net.ustyugov.jtalk.adapter.NoGroupsAdapter;
import net.ustyugov.jtalk.adapter.RosterAdapter;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.dialog.ChangeChatDialog;
import net.ustyugov.jtalk.dialog.ContactMenuDialogs;
import net.ustyugov.jtalk.dialog.IncomingFileDialog;
import net.ustyugov.jtalk.dialog.InviteDialog;
import net.ustyugov.jtalk.dialog.RosterDialogs;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;

public class RosterActivity extends SherlockActivity implements OnItemClickListener, OnItemLongClickListener {
    private static final int ACTIVITY_PREFERENCES = 10;
	private BroadcastReceiver updateReceiver;
    private BroadcastReceiver errorReceiver;
    private BroadcastReceiver stateReceiver;
    
    private Menu menu = null;

    private JTalkService service;
    private SharedPreferences prefs;

    private GridView gridView;
    private NoGroupsAdapter simpleAdapter;
    private RosterAdapter rosterAdapter;
    private String[] statusArray;
    private int columns = 1;
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        service = JTalkService.getInstance();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme(prefs.getBoolean("DarkColors", false) ? R.style.AppThemeDark : R.style.AppThemeLight);
        
		setContentView(R.layout.roster);
        
        LinearLayout roster = (LinearLayout) findViewById(R.id.roster_linear);
    	roster.setBackgroundColor(prefs.getBoolean("DarkColors", false) ? 0xFF000000 : 0xFFFFFFFF);
    	
    	getSupportActionBar().setHomeButtonEnabled(true);
        
        statusArray = getResources().getStringArray(R.array.statusArray);
        rosterAdapter = new RosterAdapter(this);
        simpleAdapter = new NoGroupsAdapter(this);
        
        
        try {
			columns = Integer.parseInt(prefs.getString("RosterColumns", 1+""));
		} catch (NumberFormatException e) {	}
        
        gridView = (GridView) findViewById(R.id.users);
        if (!prefs.getBoolean("ShowGroups", true)) {
        	gridView.setNumColumns(columns);
        } else gridView.setNumColumns(1);
		gridView.setCacheColorHint(0x00000000);
        gridView.setOnItemClickListener(this);
        gridView.setOnItemLongClickListener(this);
        gridView.setAdapter(rosterAdapter);
  		
       	if (getIntent().getBooleanExtra("file", false)) {
           	if (service.getIncomingRequests().size() > 0) {
           		FileTransferRequest request = service.getIncomingRequests().remove(0);
           		new IncomingFileDialog(this, request).show();
           		if(service.getIncomingRequests().isEmpty()) Notify.cancelFileRequest();
           	}
       	}
       	
       	if (getIntent().getBooleanExtra("invite", false)) {
       		String room = getIntent().getStringExtra("room");
       		String from = getIntent().getStringExtra("from");
       		String reason = getIntent().getStringExtra("reason");
       		String password = getIntent().getStringExtra("password");
       		
       		new InviteDialog(this, room, from, reason, password).show();
       	}
       	
       	if (getIntent().getBooleanExtra("msg", false)) {
           	if (service.getMessagesList().size() >= 1) {
           		String jid = service.getMessagesList().get(0);
           		String username = null;
           		
           		if (service.getConferencesHash().containsKey(StringUtils.parseBareAddress(jid)) && jid.contains("/")) {
    				username = StringUtils.parseResource(jid);
    			} else {
    				RosterEntry re = service.getRoster().getEntry(jid);
    				if (re != null) {
    					username = re.getName();
    					if (username == null || username.equals("")) username = jid;
    				} else {
    					username = jid;
    				}
    			}
           		
        		Intent intent = new Intent(this, Chat.class);
        		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        		intent.putExtra("jid", jid);
        		if (username != null) intent.putExtra("username", username);
        		startActivity(intent);
           	}
       	}
       	
       	int activeAccount = prefs.getInt("Account", 0);
        if (activeAccount < 1) {
        	Intent preferences = new Intent(this, Accounts.class);
        	startActivity(preferences);
        } else {
        	startService(new Intent(this, JTalkService.class));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        errorReceiver = new BroadcastReceiver() {
    		@Override
    		public void onReceive(Context context, Intent intent) {
    			service = JTalkService.getInstance();
    			String error = intent.getStringExtra("error");
    			Toast.makeText(context, error, Toast.LENGTH_LONG).show();
    		}
    	};
    	
        updateReceiver = new BroadcastReceiver() {
  			@Override
  			public void onReceive(Context context, Intent intent) {
  				service = JTalkService.getInstance();
  				updateList();
  			}
  		};
  		
  		stateReceiver = new BroadcastReceiver() {
  			@Override
  			public void onReceive(Context context, Intent intent) {
  				service = JTalkService.getInstance();
  				updateMenu();
  				updateStatus();
  			}
  		};
        
        service = JTalkService.getInstance();
        service.setCurrentJid("me");
  		
  		registerReceiver(errorReceiver, new IntentFilter(Constants.ERROR));
      	registerReceiver(updateReceiver, new IntentFilter(Constants.UPDATE));
      	registerReceiver(stateReceiver, new IntentFilter(Constants.CONNECTION_STATE));
      	registerReceiver(updateReceiver, new IntentFilter(Constants.NEW_MESSAGE));
      	
        if (service != null && service.isStarted()) service.resetTimer();
        updateList();
        updateMenu();
        updateStatus();
    }

    @Override
    public void onPause() {
    	super.onPause();
    	unregisterReceiver(errorReceiver);
	    unregisterReceiver(updateReceiver);
	    unregisterReceiver(stateReceiver);
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_PREFERENCES) {
            if (resultCode == RESULT_OK) {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
                
                LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                if (prefs.getBoolean("Locations", false)) {
                	Location gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                	if (gps == null) service.sendLocation(lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
                	else service.sendLocation(gps);
                	
                	lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, Constants.LOCATION_MIN_TIME, Constants.LOCATION_MIN_DIST, service.getLocationListener());
      	    		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.LOCATION_MIN_TIME, Constants.LOCATION_MIN_DIST, service.getLocationListener());
                } else {
                	lm.removeUpdates(service.getLocationListener());
                	service.sendLocation(null);
                }
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.roster, menu);
        this.menu = menu;
        updateMenu();
        return true;
    }
    
    private void updateMenu() {
    	if (menu != null) {
    		menu.findItem(R.id.add).setEnabled(service.isAuthenticated());
            menu.findItem(R.id.muc).setEnabled(service.isAuthenticated());
            menu.findItem(R.id.disco).setEnabled(service.isAuthenticated());
            if (service.getMessagesHash().size() > 0 || service.getConferencesHash().size() > 0) menu.findItem(R.id.chats).setEnabled(true);
            else menu.findItem(R.id.chats).setEnabled(false);
            super.onCreateOptionsMenu(menu);
    	}
    }
  
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.status:
    			RosterDialogs.changeStatusDialog(this, null);
    			break;
    		case android.R.id.home:
    			RosterDialogs.changeStatusDialog(this, null);
    			break;
  	    	case R.id.add:
  	    		RosterDialogs.addDialog(this, null);
  	    		break;
  	    	case R.id.muc:
  	    		Intent mucIntent = new Intent(this, Muc.class);
  	    		startActivity(mucIntent);
  	    		break;
  	    	case R.id.chats:
  	    		ChangeChatDialog.show(this);
  	    		break;
  	    	case R.id.accounts:
  	    		Intent aIntent = new Intent(this, Accounts.class);
  	    		startActivity(aIntent);
  	    		break;
  	    	case R.id.prefs:
  	    		startActivityForResult(new Intent(this, Preferences.class), ACTIVITY_PREFERENCES);
  	    		break;
  	    	case R.id.disco:
  	    		startActivity(new Intent(this, ServiceDiscovery.class));
  	    		break;
  	    	case R.id.info:
  	    		String message = "jTalk " + getString(R.string.version) + " (" + getString(R.string.build) + ")";
  	    	
				View layout = getLayoutInflater().inflate(R.layout.about_dialog, (ViewGroup) findViewById(R.id.about_dialog_linear));
				TextView text = (TextView) layout.findViewById(R.id.text);
				text.setText(message);
  	    		
  	    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
  	    		builder.setIcon(R.drawable.ic_launcher);
  	    		builder.setTitle(R.string.Info);
  	    		builder.setView(layout);
  	    		builder.setPositiveButton("OK", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int arg1) {
						dialog.dismiss();
					}
  	    		});
  	    		builder.create().show();
  	    		break;
  	    	case R.id.exit:
  	    		if (prefs.getBoolean("DeleteHistory", false)) {
  	    			getContentResolver().delete(JTalkProvider.CONTENT_URI, null, null);
  	    		}
  	    		finish();
  	    		Notify.cancelAll(this);
  	    		service.disconnect(true);
  	    		break;
  	    	default:
  	    		return false;
    	}
    	return true;
    }
  
    private void updateList() {
    	new Thread() {
    		public void run() {
    			RosterActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RosterActivity.this);
						boolean hideOffline = prefs.getBoolean("hideOffline", false);
						List<RosterItem> rosterList = new ArrayList<RosterItem>();
						if (service != null && service.getRoster() != null && service.isAuthenticated()) {
							Roster roster = service.getRoster();
							if (prefs.getBoolean("ShowGroups", true)) {
								Collection<RosterGroup> groups = roster.getGroups();
								for (RosterGroup group: groups) {
									List<String> list = new ArrayList<String>();
									Collection<RosterEntry> entrys = group.getEntries();
									for (RosterEntry re: entrys) {
										String jid = re.getUser();
										Presence.Type presenceType = service.getType(jid);
										if (hideOffline) {
					   		  				if (presenceType != Presence.Type.unavailable) list.add(jid);
					   		  			} else {
					   		  				list.add(jid);
					   		  			}
									}
									if (list.size() > 0) {
										String name = group.getName();
										RosterItem item = new RosterItem();
										item.setGroup(true);
										item.setName(name);
										rosterList.add(item);
										if (service.getCollapsedGroups().contains(name)) item.setCollapsed(true);
										else {
											list = SortList.sortSimpleContacts(list);
											for (String jid: list) {
												RosterEntry re = roster.getEntry(jid);
												RosterItem i = new RosterItem();
												i.setGroup(false);
												i.setEntry(re);
												rosterList.add(i);
											}
										}
									}
								}
								
								List<String> list = new ArrayList<String>();
								Collection<RosterEntry> entrys = roster.getUnfiledEntries();
								for (RosterEntry re: entrys) {
									String jid = re.getUser();
									Presence.Type presenceType = service.getType(jid);
									if (hideOffline) {
				   		  				if (presenceType != Presence.Type.unavailable) list.add(jid);
				   		  			} else {
				   		  				list.add(jid);
				   		  			}
								}
								
								if (list.size() > 0) {
									String name = getString(R.string.Nogroup);
									RosterItem item = new RosterItem();
									item.setGroup(true);
									item.setName(name);
									rosterList.add(item);
									if (service.getCollapsedGroups().contains(name)) item.setCollapsed(true);
									else {
										list = SortList.sortSimpleContacts(list);
										for (String jid: list) {
											RosterEntry re = roster.getEntry(jid);
											RosterItem i = new RosterItem();
											i.setGroup(false);
											i.setEntry(re);
											rosterList.add(i);
										}
									}
								}
								
								if (gridView.getAdapter() instanceof NoGroupsAdapter) gridView.setAdapter(rosterAdapter);
								rosterAdapter.update();
						    	rosterAdapter.notifyDataSetChanged();
							} else {
								List<String> list = new ArrayList<String>();
				   				Iterator<RosterEntry> it = roster.getEntries().iterator();
				   				
				   				while(it.hasNext()) {
				   					String jid = it.next().getUser();
				   					Presence.Type presenceType = service.getType(jid);
				   		  			if (hideOffline) {
				   		  				if (presenceType != Presence.Type.unavailable) list.add(jid);
				   		  			} else {
				   		  				list.add(jid);
				   		  			}
				   				}
				   				list = SortList.sortSimpleContacts(list);
				   				
				   				if (gridView.getAdapter() instanceof RosterAdapter) gridView.setAdapter(simpleAdapter);
				   				simpleAdapter.update(list);
				   				simpleAdapter.notifyDataSetChanged();
							}
					}
					}
    			});
    		}
    	}.start();
    }
    
    private void updateStatus() {
    	if (service.isAuthenticated() && service.isStarted()) {
   			String status = statusArray[prefs.getInt("currentSelection", 0)];
   			String substatus = prefs.getString("currentStatus", "");
   			getSupportActionBar().setTitle(status);
   			getSupportActionBar().setSubtitle(substatus);
   		} else {
   			getSupportActionBar().setTitle(getString(R.string.NotConnected));
   			getSupportActionBar().setSubtitle(service.getState());
   		}
    }
    
	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		if (parent.getAdapter() instanceof NoGroupsAdapter) {
			RosterEntry entry = (RosterEntry) parent.getAdapter().getItem(position);
			Intent i = new Intent(this, Chat.class);
	        i.putExtra("jid", entry.getUser());
	        startActivity(i);
		} else {
			RosterItem item = (RosterItem) parent.getAdapter().getItem(position);
			
			if (item.isGroup()) {
				if (item.isCollapsed()) 
					while (service.getCollapsedGroups().contains(item.getName())) service.getCollapsedGroups().remove(item.getName());
				else service.getCollapsedGroups().add(item.getName());
				updateList();
			} else {
				RosterEntry re = item.getEntry();
				String jid = re.getUser();
				
				Intent i = new Intent(this, Chat.class);
		        i.putExtra("jid", jid);
		        startActivity(i);
			}
		}
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		if (parent.getAdapter() instanceof NoGroupsAdapter) {
			RosterEntry entry = (RosterEntry) parent.getAdapter().getItem(position);
			ContactMenuDialogs.ContactMenu(this, entry);
		} else {
			RosterItem entry = (RosterItem) parent.getAdapter().getItem(position);
			if (entry.isGroup()) {
				if (!entry.getName().equals(getString(R.string.Nogroup))) RosterDialogs.renameGroupDialog(this, entry.getName());
			}
			else ContactMenuDialogs.ContactMenu(this, entry.getEntry());
		}
		return true;
	}
}
