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
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.adapter.AccountsAdapter;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.dialog.AccountDialogs;
import net.ustyugov.jtalk.service.JTalkService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;

public class Accounts extends SherlockActivity {
	private static final int CONTEXT_EDIT = 1;
	private static final int CONTEXT_REMOVE = 2;
	
	private ListView list;
	private AccountsAdapter adapter;
    private BroadcastReceiver refreshReceiver;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		setTheme(prefs.getBoolean("DarkColors", false) ? R.style.AppThemeDark : R.style.AppThemeLight);
        setContentView(R.layout.accounts);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setTitle(R.string.Accounts);
        
        LinearLayout linear = (LinearLayout) findViewById(R.id.accounts_linear);
        linear.setBackgroundColor(prefs.getBoolean("DarkColors", false) ? 0xFF000000 : 0xFFFFFFFF);

        adapter = new AccountsAdapter(this);
		
		list = (ListView) findViewById(R.id.accounts_List);
        list.setBackgroundColor(prefs.getBoolean("DarkColors", false) ? 0xFF000000 : 0xFFFFFFFF);
        list.setDividerHeight(0);
        list.setCacheColorHint(0x00000000);
        list.setAdapter(adapter);
        registerForContextMenu(list);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		update();
		
		refreshReceiver = new BroadcastReceiver() {
  			@Override
  			public void onReceive(Context context, Intent intent) {
				update();
  			}
  		};
  		registerReceiver(refreshReceiver, new IntentFilter(Constants.PRESENCE_CHANGED));
  		registerReceiver(refreshReceiver, new IntentFilter(Constants.UPDATE));
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
		menu.add(Menu.NONE, CONTEXT_EDIT, Menu.NONE, R.string.Edit);
		menu.add(Menu.NONE, CONTEXT_REMOVE, Menu.NONE, R.string.Remove);
		menu.setHeaderTitle(getString(R.string.Actions));
	 	super.onCreateContextMenu(menu, v, info);
	 }
	 
	 @Override
	 public boolean onContextItemSelected(android.view.MenuItem menuitem) {
		 AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuitem.getMenuInfo();
	     int position = info.position;
	     Account account = (Account) list.getItemAtPosition(position);
	     int id = account.getId();
	     
	     switch(menuitem.getItemId()) {
	     	case CONTEXT_EDIT:
	     		AccountDialogs.editDialog(this, id);
	            break;
	        case CONTEXT_REMOVE:
	        	JTalkService service = JTalkService.getInstance();
	        	if (service.isAuthenticated(account.getJid())) service.disconnect(account.getJid());
	        	getContentResolver().delete(JTalkProvider.ACCOUNT_URI, "_id = '" + id + "'", null);
	        	update();
	           	break;
	     }
	     return true;
	}
	 
	private void update() {
		adapter.update();
		adapter.notifyDataSetChanged();
	}
}
