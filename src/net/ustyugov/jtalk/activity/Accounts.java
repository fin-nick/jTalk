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

import net.ustyugov.jtalk.Account;
import net.ustyugov.jtalk.activity.vcard.SetVcardActivity;
import net.ustyugov.jtalk.adapter.AccountsAdapter;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.dialog.AccountDialogs;
import net.ustyugov.jtalk.dialog.RosterDialogs;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.StringUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;

public class Accounts extends SherlockActivity implements OnItemClickListener {
	private static final int CONTEXT_CONNECT = 1;
	private static final int CONTEXT_EDIT = 2;
	private static final int CONTEXT_VCARD = 3;
	private static final int CONTEXT_COMMAND = 4;
	private static final int CONTEXT_PRIVACY = 5;
	private static final int CONTEXT_REMOVE = 6;
	
	private ListView list;
	private AccountsAdapter adapter;
	private SharedPreferences prefs;
	private BroadcastReceiver refreshReceiver;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		setTheme(prefs.getBoolean("DarkColors", false) ? R.style.AppThemeDark : R.style.AppThemeLight);
        setContentView(R.layout.accounts);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setTitle(R.string.Accounts);
        
        LinearLayout linear = (LinearLayout) findViewById(R.id.accounts_linear);
        linear.setBackgroundColor(prefs.getBoolean("DarkColors", false) ? 0xFF000000 : 0xFFFFFFFF);

		list = (ListView) findViewById(R.id.accounts_List);
        list.setBackgroundColor(prefs.getBoolean("DarkColors", false) ? 0xFF000000 : 0xFFFFFFFF);
        list.setOnItemClickListener(this);
        list.setDividerHeight(0);
        list.setCacheColorHint(0x00000000);
        registerForContextMenu(list);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		init();
		
		refreshReceiver = new BroadcastReceiver() {
  			@Override
  			public void onReceive(Context context, Intent intent) {
				init();
  			}
  		};
  		registerReceiver(refreshReceiver, new IntentFilter(net.ustyugov.jtalk.Constants.UPDATE));
	}
	
	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(refreshReceiver);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.accounts, menu);
        return super.onCreateOptionsMenu(menu);
    }
	
	 @Override
	 public boolean onOptionsItemSelected(MenuItem item) {
	   	switch (item.getItemId()) {
	   		case android.R.id.home:
	   			finish();
	   			break;
	     	case R.id.add:
	     		AccountDialogs.addDialog(this);
	     		onResume();
	     		break;
	     	default:
	     		return false;
	    	}
	    	return true;
	 }
	 
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
		JTalkService service = JTalkService.getInstance();
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) info;
		Account acc = (Account) list.getItemAtPosition(acmi.position);
		String jid = acc.getJid();
		XMPPConnection connection = service.getConnection();
		String user = "_";
		if (service != null && connection != null) {
			if (connection.getUser() != null) user = StringUtils.parseBareAddress(connection.getUser());
		}
		
		menu.add(Menu.NONE, CONTEXT_CONNECT, Menu.NONE, R.string.Connect);
		menu.add(Menu.NONE, CONTEXT_EDIT, Menu.NONE, R.string.Edit);
		menu.add(Menu.NONE, CONTEXT_VCARD, Menu.NONE, R.string.vcard).setEnabled(jid.equals(user));
		menu.add(Menu.NONE, CONTEXT_COMMAND, Menu.NONE, R.string.ExecuteCommand).setEnabled(jid.equals(user));
		menu.add(Menu.NONE, CONTEXT_PRIVACY, Menu.NONE, R.string.PrivacyLists).setEnabled(jid.equals(user));
		menu.add(Menu.NONE, CONTEXT_REMOVE, Menu.NONE, R.string.Remove);
		menu.setHeaderTitle(getString(R.string.Actions));
	 	super.onCreateContextMenu(menu, v, info);
	 }
	 
	 @Override
	 public boolean onContextItemSelected(android.view.MenuItem item) {
		 AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	     int position = info.position;
	     Account acc = (Account) list.getItemAtPosition(position);
	     String jid = acc.getJid();
	     int id = acc.getId();
	     
	     switch(item.getItemId()) {
	     	case CONTEXT_CONNECT:
	     		click(acc);
	            break;
	     	case CONTEXT_EDIT:
	     		AccountDialogs.editDialog(this, id);
	            break;
	     	case CONTEXT_VCARD:
  	    		startActivity(new Intent(this, SetVcardActivity.class));
	            break;
	        case CONTEXT_REMOVE:
	        	getContentResolver().delete(JTalkProvider.ACCOUNT_URI, "_id = '" + id + "'", null);
	           	break;
	        case CONTEXT_COMMAND:
	        	RosterDialogs.resourceDialog(this, jid);
	        	break;
	        case CONTEXT_PRIVACY:
	        	startActivity(new Intent(this, PrivacyListsActivity.class));
	        	break;
	     }
	     return true;
	}
	 
	@Override
	public void onItemClick(final AdapterView<?> parent, View view, final int position, long i) {
		new Thread() {
			public void run() {
				Accounts.this.runOnUiThread(new Runnable() {
					public void run() {
						click((Account) parent.getItemAtPosition(position));
					}
				});
			}
		}.start();
	 }
	
	private void init() {
		adapter = new AccountsAdapter(this);
		Cursor cursor = getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, null, null, AccountDbHelper._ID);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			do {
				int id = cursor.getInt(cursor.getColumnIndex(AccountDbHelper._ID));
				String jid = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID));
				
				adapter.add(new Account(id, jid));
			} while (cursor.moveToNext());
			cursor.close();
		}
		list.setAdapter(adapter);
	}
	
	private void click(final Account acc) {
		finish();
		new Thread() {
			public void run() {
				Accounts.this.runOnUiThread(new Runnable() {
					public void run() {
						String jid = acc.getJid();
						int id = acc.getId();
						SharedPreferences.Editor editor = prefs.edit();
				  		editor.putInt("Account", id);
				  		editor.commit();
						
						JTalkService service = JTalkService.getInstance();
						if (service != null && service.isStarted()) {
							if (service.isAuthenticated()) {
								String user = StringUtils.parseBareAddress(service.getConnection().getUser());
								if (!user.equals(jid)) {
									service.reconnect();
								}
							} else {
								service.reconnect();
							}
						} else {
							startService(new Intent(Accounts.this, JTalkService.class));
						}
					}
				});
			}
		}.start();
	}
}
