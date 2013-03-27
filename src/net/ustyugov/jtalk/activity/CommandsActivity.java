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
import net.ustyugov.jtalk.adapter.CommandsAdapter;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smackx.packet.DiscoverItems.Item;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;

public class CommandsActivity extends SherlockActivity implements OnItemClickListener {
	private JTalkService service;
	private String jid;
	private String account;
	private ProgressBar progress;
	private ListView list;
	private CommandsAdapter adapter;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
		jid = getIntent().getStringExtra("jid");
		account = getIntent().getStringExtra("account");
		service = JTalkService.getInstance();

		setContentView(R.layout.list_activity);
		setTitle(R.string.ExecuteCommand);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		LinearLayout linear = (LinearLayout) findViewById(R.id.linear);
    	linear.setBackgroundColor(Colors.BACKGROUND);
    	
		progress = (ProgressBar) findViewById(R.id.progress);
        
        list = (ListView) findViewById(R.id.list);
        list.setOnItemClickListener(this);
        list.setDividerHeight(0);
        list.setCacheColorHint(0x00000000);
        
        new Init().execute(null, null, null);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		service.resetTimer();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Item item = (Item) parent.getItemAtPosition(position);

		Intent intent = new Intent(this, DataFormActivity.class);
        intent.putExtra("account", account);
		intent.putExtra("jid", jid);
		intent.putExtra("node", item.getNode());
		intent.putExtra("com", true);
		startActivity(intent);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    	case android.R.id.home:
	    		finish();
	    		break;
	    }
	    return true;
	}
	
	private class Init extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
            adapter = new CommandsAdapter(CommandsActivity.this, account, jid);
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
