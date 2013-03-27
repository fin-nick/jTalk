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

package net.ustyugov.jtalk.activity.muc;

import java.util.Collection;

import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.adapter.MucSearchAdapter;
import net.ustyugov.jtalk.dialog.BookmarksDialogs;
import net.ustyugov.jtalk.dialog.MucDialogs;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;

public class MucSearch extends SherlockActivity implements OnClickListener, OnItemClickListener, OnItemLongClickListener {
	private JTalkService service;
	private String account;
	private ImageButton searchButton;
	private EditText searchInput;
	private SharedPreferences prefs;
	private ProgressBar progress;
	private ListView list;
	private MucSearchAdapter adapter;
	private GetRooms task;
	
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
        setContentView(R.layout.muc_search);
        setTitle(android.R.string.search_go);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        	
        LinearLayout linear = (LinearLayout) findViewById(R.id.muc_search);
        linear.setBackgroundColor(Colors.BACKGROUND);
        
        searchButton = (ImageButton) findViewById(R.id.search_button);
        searchButton.setOnClickListener(this);
        
        searchInput = (EditText) findViewById(R.id.search_input);
        progress = (ProgressBar) findViewById(R.id.progress);
        
        list = (ListView) findViewById(R.id.search_list);
        list.setDividerHeight(0);
        list.setCacheColorHint(0x00000000);
        list.setOnItemClickListener(this);
        list.setOnItemLongClickListener(this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		service = JTalkService.getInstance();
		String server = prefs.getString("lastGroup", "");
		if (server.indexOf("@") > 0 ) server = server.substring(server.indexOf("@") + 1);
		if (server.length() > 0) searchInput.setText(server);
	}
	
	@Override
	public void onClick(View arg0) {
		if (task != null && task.getStatus() == AsyncTask.Status.RUNNING) task.cancel(true);
		task = new GetRooms();
		task.execute(null, null, null);
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
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		HostedRoom item = (HostedRoom) parent.getItemAtPosition(position);
		String jid = item.getJid();
		MucDialogs.joinDialog(this, account, jid, null);
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		HostedRoom item = (HostedRoom) parent.getItemAtPosition(position);
		BookmarksDialogs.AddDialog(this, account, item.getJid(), item.getName());
        return true;
    }
	
	private class GetRooms extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			try {
				String server = searchInput.getText().toString();
				Collection<HostedRoom> rooms = MultiUserChat.getHostedRooms(service.getConnection(account), server);
				if (!rooms.isEmpty()) adapter = new MucSearchAdapter(MucSearch.this, rooms);
			} catch (XMPPException e) { }
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v) {
			super.onPostExecute(v);
		    list.refreshDrawableState();
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
