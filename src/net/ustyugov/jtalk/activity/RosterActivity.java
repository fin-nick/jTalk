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

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.MessageItem;
import net.ustyugov.jtalk.Notify;
import net.ustyugov.jtalk.RosterItem;
import net.ustyugov.jtalk.activity.muc.Bookmarks;
import net.ustyugov.jtalk.adapter.NoGroupsAdapter;
import net.ustyugov.jtalk.adapter.RosterAdapter;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.dialog.ChangeChatDialog;
import net.ustyugov.jtalk.dialog.IncomingFileDialog;
import net.ustyugov.jtalk.dialog.InviteDialog;
import net.ustyugov.jtalk.dialog.MucDialogs;
import net.ustyugov.jtalk.dialog.RosterDialogs;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;

public class RosterActivity extends SherlockActivity implements OnItemClickListener, OnItemLongClickListener {
    private static final int ACTIVITY_PREFERENCES = 10;
    final static int UPDATE_INTERVAL = 500;
    static long lastUpdateReceived;
    
	private BroadcastReceiver updateReceiver;
    private BroadcastReceiver errorReceiver;

    private Menu menu = null;

    private JTalkService service;
    private SharedPreferences prefs;

    private GridView gridView;
    private NoGroupsAdapter simpleAdapter;
    private RosterAdapter rosterAdapter;
    private String[] statusArray;
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        startService(new Intent(this, JTalkService.class));
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
        
        gridView = (GridView) findViewById(R.id.users);
        gridView.setNumColumns(1);
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
       		String account = getIntent().getStringExtra("account");
       		String room = getIntent().getStringExtra("room");
       		String from = getIntent().getStringExtra("from");
       		String reason = getIntent().getStringExtra("reason");
       		String password = getIntent().getStringExtra("password");
       		
       		new InviteDialog(this, account, room, from, reason, password).show();
       	}

        if (getIntent().getBooleanExtra("msg", false)) {
            MessageItem item = service.getUnreadMessage();
            if (item != null) {
                String jid = item.getJid();
                String account = item.getAccount();

                Intent intent = new Intent(this, Chat.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("account", account);
                intent.putExtra("jid", jid);
                startActivity(intent);
            }
        }
       	Cursor cursor = getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '" + 1 + "'", null, null);
		if (cursor == null || cursor.getCount() < 1) startActivity(new Intent(this, Accounts.class));
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
                updateMenu();
                updateStatus();

                long now = System.currentTimeMillis();
                if ((now - lastUpdateReceived) < RosterActivity.UPDATE_INTERVAL) return;
                lastUpdateReceived = now;
                updateList();
            }
        };
  		
        service = JTalkService.getInstance();
        service.setCurrentJid("me");
  		
  		registerReceiver(errorReceiver, new IntentFilter(Constants.ERROR));
      	registerReceiver(updateReceiver, new IntentFilter(Constants.UPDATE));
      	registerReceiver(updateReceiver, new IntentFilter(Constants.CONNECTION_STATE));
      	registerReceiver(updateReceiver, new IntentFilter(Constants.NEW_MESSAGE));
      	
        if (service != null) service.resetTimer();
        updateList();
        updateMenu();
        updateStatus();
    }

    @Override
    public void onPause() {
    	super.onPause();
    	unregisterReceiver(errorReceiver);
	    unregisterReceiver(updateReceiver);
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
            menu.findItem(R.id.offline).setTitle(prefs.getBoolean("hideOffline", false) ? R.string.ShowOfflineContacts : R.string.HideOfflineContacts);
            if (!service.getMessages().isEmpty() || !service.getConferences().isEmpty()) menu.findItem(R.id.chats).setEnabled(true);
            else menu.findItem(R.id.chats).setEnabled(false);
            super.onCreateOptionsMenu(menu);
    	}
    }
  
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.offline:
    			if (prefs.getBoolean("hideOffline", false)) service.setPreference(this, "hideOffline", false);
    			else service.setPreference(this, "hideOffline", true);
    			updateMenu();
    			updateList();
    			break;
    		case R.id.status:
    			RosterDialogs.changeStatusDialog(this, null, null);
    			break;
    		case android.R.id.home:
    			RosterDialogs.changeStatusDialog(this, null, null);
    			break;
  	    	case R.id.add:
  	    		RosterDialogs.addDialog(this, null);
  	    		break;
  	    	case R.id.muc:
  	    		Intent mucIntent = new Intent(this, Bookmarks.class);
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
                        if (prefs.getBoolean("ShowGroups", true)) {
                            if (gridView.getAdapter() instanceof NoGroupsAdapter) gridView.setAdapter(rosterAdapter);
                            rosterAdapter.update();
                            rosterAdapter.notifyDataSetChanged();
                        } else {
                            if (gridView.getAdapter() instanceof RosterAdapter) gridView.setAdapter(simpleAdapter);
                            simpleAdapter.update();
                            simpleAdapter.notifyDataSetChanged();
                        }
					}
                });
    		}
    	}.start();
    }
    
    private void updateStatus() {
    	if (service.isAuthenticated()) {
   			String status = statusArray[prefs.getInt("currentSelection", 0)];
   			String substatus = prefs.getString("currentStatus", "");
   			getSupportActionBar().setTitle(status);
   			getSupportActionBar().setSubtitle(substatus);
   		} else {
   			getSupportActionBar().setTitle(getString(R.string.NotConnected));
   			getSupportActionBar().setSubtitle(service.getGlobalState());
   		}
    }
    
	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		RosterItem item = (RosterItem) parent.getItemAtPosition(position);
		String name = item.getName();
		String account = item.getAccount();
		
		if (item.isGroup() || item.isAccount()) {
			if (item.isCollapsed()) {
				while (service.getCollapsedGroups().contains(name)) service.getCollapsedGroups().remove(name);
				item.setCollapsed(false);
			} else {
				service.getCollapsedGroups().add(name);
				item.setCollapsed(true);
			}
			updateList();
		} else if (item.isEntry() || item.isSelf()) {
			RosterEntry re = item.getEntry();
			String jid = re.getUser();
			Intent i = new Intent(this, Chat.class);
			i.putExtra("account", account);
	        i.putExtra("jid", jid);
	        startActivity(i);
		} else if (item.isMuc()) {
			Intent i = new Intent(this, Chat.class);
			i.putExtra("account", account);
	        i.putExtra("jid", item.getName());
	        startActivity(i);
		}
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		RosterItem item = (RosterItem) parent.getItemAtPosition(position);
		if (item.isGroup()) {
			String name = item.getName();
			if (!name.equals(getString(R.string.Nogroup)) && !name.equals(getString(R.string.SelfGroup)) && !name.equals(getString(R.string.MUC)) && !name.equals(getString(R.string.Privates))) RosterDialogs.renameGroupDialog(this, item.getAccount(), item.getName());
		} else if (item.isAccount()) {
			RosterDialogs.AccountMenuDialog(this, item);
		} else if (item.isEntry()) {
            String j = item.getEntry().getUser();
			if (!service.getPrivateMessages(item.getAccount()).contains(j)) RosterDialogs.ContactMenuDialog(this, item);
            else RosterDialogs.PrivateMenuDialog(this, item);
		} else if (item.isSelf()) {
			RosterDialogs.SelfContactMenuDialog(this, item);
		} else if (item.isMuc()) {
			MucDialogs.roomMenu(this, item.getAccount(), item.getName());
		}
		return true;
	}
}
