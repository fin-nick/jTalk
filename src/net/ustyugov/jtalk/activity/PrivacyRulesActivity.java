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
import java.util.List;

import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.adapter.PrivacyRulesAdapter;
import net.ustyugov.jtalk.dialog.PrivacyDialogs;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.PrivacyList;
import org.jivesoftware.smack.PrivacyListManager;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.PrivacyItem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;

public class PrivacyRulesActivity extends SherlockActivity {
	private static final int MENU_SAVE = 1;
	private static final int MENU_ADD = 2;
	private static final int CONTEXT_EDIT = 3;
	private static final int CONTEXT_REMOVE = 4;
	
	private String account;
	private JTalkService service;
	private PrivacyListManager plm;
	private ProgressBar progress;
	private TextView name;
	private ListView list;
	private PrivacyRulesAdapter adapter;
	private String listName;
	private List<PrivacyItem> rules = new ArrayList<PrivacyItem>();
	private BroadcastReceiver receiver;
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		account = getIntent().getStringExtra("account");
		service = JTalkService.getInstance();
		listName = getIntent().getStringExtra("list");

        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
		setContentView(R.layout.rules_activity);
		setTitle("Rules");
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		LinearLayout linear = (LinearLayout) findViewById(R.id.linear);
    	linear.setBackgroundColor(Colors.BACKGROUND);
    	
    	name = (TextView) findViewById(R.id.name);
    	if (listName == null) name.setEnabled(true);
    	else {
    		name.setText(listName);
    		name.setEnabled(false);
    	}
    	
    	progress = (ProgressBar) findViewById(R.id.progress);
    	
        list = (ListView) findViewById(R.id.list);
        list.setDividerHeight(0);
        list.setCacheColorHint(0x00000000);
        registerForContextMenu(list);
		init();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int position = intent.getIntExtra("position", -1);
				String order = intent.getStringExtra("order");
				String type = intent.getStringExtra("type");
				String value = intent.getStringExtra("value");
				boolean allow = intent.getBooleanExtra("allow", true);
				
				PrivacyItem item = new PrivacyItem(type, allow, Integer.parseInt(order));
				item.setValue(value);
				item.setFilterIQ(intent.getBooleanExtra("iq", false));
				item.setFilterMessage(intent.getBooleanExtra("msg", false));
				item.setFilterPresence_in(intent.getBooleanExtra("presence_in", false));
				item.setFilterPresence_out(intent.getBooleanExtra("presence_out", false));
				
				if (position < 0) rules.add(item);
				else rules.set(position, item);
				new Init().execute();
			}
		};
		
		registerReceiver(receiver, new IntentFilter("com.jtalk.privacy.add"));
	}
	
	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(receiver);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_ADD, Menu.NONE, R.string.Add);
		menu.add(Menu.NONE, MENU_SAVE, Menu.NONE, R.string.save);
        return super.onCreateOptionsMenu(menu);
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case android.R.id.home:
    			finish();
    			break;
    		case MENU_ADD:
    			PrivacyDialogs.addListDialog(this, account, null, -1);
	     		break;
    		case MENU_SAVE:
    			try {
    				if (listName != null && listName.length() > 0) plm.updatePrivacyList(listName, rules);
    				else plm.createPrivacyList(name.getText().toString(), rules);
    			} catch (XMPPException e) {
    				e.printStackTrace();
    			}
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
	 public boolean onContextItemSelected(android.view.MenuItem item) {
		 AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	     int position = info.position;
	     switch(item.getItemId()) {
	     	case CONTEXT_EDIT:
	     		PrivacyItem pi = rules.get(position);
	     		PrivacyDialogs.addListDialog(this, account, pi, position);
	            break;
	        case CONTEXT_REMOVE:
	        	rules.remove(position);
	        	new Init().execute();
	           	break;
	     }
	     return true;
	    }

	private void init() {
		plm = PrivacyListManager.getInstanceFor(service.getConnection(account));
		if (listName != null) {
			try {
				PrivacyList list = plm.getPrivacyList(listName);
				rules = list.getItems();
			} catch (XMPPException e) {
				e.printStackTrace();
			}
		}
  		new Init().execute(null, null, null);
	}
	
	private class Init extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			adapter = new PrivacyRulesAdapter(PrivacyRulesActivity.this, rules);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v) {
			super.onPostExecute(v);
			list.setAdapter(adapter);
		    list.setVisibility(View.VISIBLE);
		    progress.setVisibility(View.GONE);
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			list.setVisibility(View.GONE);
			progress.setVisibility(View.VISIBLE);
		}
	}
}
