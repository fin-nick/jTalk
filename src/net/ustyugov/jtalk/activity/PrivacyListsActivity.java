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

import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.adapter.PrivacyListAdapter;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.PrivacyList;
import org.jivesoftware.smack.PrivacyListManager;
import org.jivesoftware.smack.XMPPException;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;

public class PrivacyListsActivity extends SherlockActivity implements OnItemClickListener {
	private static final int CONTEXT_ACTIVATE = 1;
	private static final int CONTEXT_DEFAULT = 2;
	private static final int CONTEXT_EDIT = 3;
	private static final int CONTEXT_REMOVE = 4;
	
	private String account;
	private JTalkService service;
	private PrivacyListManager plm;
	private ProgressBar progress;
	private ListView list;
	private PrivacyListAdapter adapter;
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		account = getIntent().getStringExtra("account");
		service = JTalkService.getInstance();

        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
		setContentView(R.layout.list_activity);
		setTitle("Lists of privacy");
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		LinearLayout linear = (LinearLayout) findViewById(R.id.linear);
    	linear.setBackgroundColor(Colors.BACKGROUND);
    	
    	progress = (ProgressBar) findViewById(R.id.progress);
        list = (ListView) findViewById(R.id.list);
        list.setOnItemClickListener(this);
        list.setDividerHeight(0);
        list.setCacheColorHint(0x00000000);
        registerForContextMenu(list);
	}
	
	private void init() {
		plm = PrivacyListManager.getInstanceFor(service.getConnection(account));
  		new Init().execute(null, null, null);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		service.resetTimer();
		init();
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
    			Intent intent = new Intent(this, PrivacyRulesActivity.class).putExtra("account", account);
    			startActivity(intent);
	     		break;
	     	default:
	     		return false;
    	}
    	return true;
    }
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
		menu.add(Menu.NONE, CONTEXT_ACTIVATE, Menu.NONE, "Activate");
		menu.add(Menu.NONE, CONTEXT_DEFAULT, Menu.NONE, "Set default");
		menu.add(Menu.NONE, CONTEXT_EDIT, Menu.NONE, R.string.Edit);
		menu.add(Menu.NONE, CONTEXT_REMOVE, Menu.NONE, R.string.Remove);
		menu.setHeaderTitle(getString(R.string.Actions));
	 	super.onCreateContextMenu(menu, v, info);
	 }
	
	@Override
	 public boolean onContextItemSelected(android.view.MenuItem item) {
		 AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	     int position = info.position;
	     PrivacyList pl = (PrivacyList) list.getItemAtPosition(position);
	     
	     switch(item.getItemId()) {
	     	case CONTEXT_ACTIVATE:
	     		try {
					plm.setActiveListName(pl.toString());
					init();
				} catch (XMPPException e) {
					e.printStackTrace();
				}
	            break;
	     	case CONTEXT_EDIT:
	     		Intent intent = new Intent(this, PrivacyRulesActivity.class);
	    		intent.putExtra("list", pl.toString());
	    		intent.putExtra("account", account);
	    		startActivity(intent);
	            break;
	     	case CONTEXT_DEFAULT:
	     		try {
					plm.setDefaultListName(pl.toString());
					init();
				} catch (XMPPException e) {
					e.printStackTrace();
				}
	            break;
	        case CONTEXT_REMOVE:
	        	try {
					plm.deletePrivacyList(pl.toString());
					init();
				} catch (XMPPException e) {
					e.printStackTrace();
				}
	           	break;
	     }
	     return true;
	    }
	
	@Override
	public void onItemClick(final AdapterView<?> parent, View view, final int position, long i) {
		try {
			PrivacyList pl = (PrivacyList) parent.getItemAtPosition(position);
			plm.setActiveListName(pl.toString());
			init();
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}
	
	private class Init extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			if (service.isAuthenticated(account)) adapter = new PrivacyListAdapter(PrivacyListsActivity.this, account);
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
