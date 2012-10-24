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
import java.util.Iterator;
import java.util.List;

import net.ustyugov.jtalk.DiscoItem;
import net.ustyugov.jtalk.activity.vcard.VCardActivity;
import net.ustyugov.jtalk.adapter.DiscoveryAdapter;
import net.ustyugov.jtalk.dialog.MucDialogs;
import net.ustyugov.jtalk.dialog.RosterDialogs;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Feature;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;

public class ServiceDiscovery extends SherlockActivity implements OnClickListener, OnItemClickListener {
	private static final int CONTEXT_REG  = 1;
	private static final int CONTEXT_JOIN = 2;
	private static final int CONTEXT_INFO = 3;
	private static final int CONTEXT_ADD  = 4;
	
	private ParseIdentity task;
	private ListView list;
	private EditText input;
	private ImageButton button;
	private ProgressBar progress;
	private JTalkService service;
	private DiscoveryAdapter adapter;
	private ServiceDiscoveryManager discoManager;
	private DiscoItem discoItem;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		setTheme(prefs.getBoolean("DarkColors", false) ? R.style.AppThemeDark : R.style.AppThemeLight);
		setContentView(R.layout.discovery);
		setTitle(R.string.ServiceDiscovery);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
        LinearLayout linear = (LinearLayout) findViewById(R.id.discovery_linear);
        linear.setBackgroundColor(prefs.getBoolean("DarkColors", false) ? 0xFF000000 : 0xFFFFFFFF);
		
		service = JTalkService.getInstance();
		discoManager = ServiceDiscoveryManager.getInstanceFor(service.getConnection());
		
		progress = (ProgressBar) findViewById(R.id.progress);
		
		String host = (prefs.getString("Server", "")).trim();
		if (host == null || host.length() < 3) host = service.getConnection().getHost();
		
		input = (EditText) findViewById(R.id.input);
		input.setText(host);
		input.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					onClick(button);
					return true;
				} else return false;
			}
		});
		
		button = (ImageButton) findViewById(R.id.button);
        button.setOnClickListener(this);
		
		list = (ListView) findViewById(R.id.list);
        list.setDividerHeight(0);
        list.setCacheColorHint(0x00000000);
        list.setOnItemClickListener(this);
        
        registerForContextMenu(list);
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	public void onClick(View v) {
		if(v.equals(button)) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(input.getWindowToken(), 0, null);
			
			if (task != null && task.getStatus() == AsyncTask.Status.RUNNING) task.cancel(true);
			task = new ParseIdentity();
			task.execute(null, null, null);
		}
	}
	
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		DiscoItem item = (DiscoItem) parent.getItemAtPosition(position);
		String jid = item.getJid();
		input.setText(jid);
		String node = item.getNode();
		new ParseIdentity().execute(node, null, null);
	}
	
	@Override
  	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
		AdapterContextMenuInfo cm = (AdapterContextMenuInfo) info;
		DiscoItem item = (DiscoItem) list.getAdapter().getItem(cm.position);
		Log.i("CONTEXTMENU", "Position: " + cm.position + "Jid: " + item.getJid());
		
		menu.setHeaderTitle(R.string.Actions);
		if (item.isRegister()) menu.add(Menu.NONE, CONTEXT_REG, Menu.NONE, R.string.Registration);
		if (item.isMUC()) menu.add(Menu.NONE, CONTEXT_JOIN, Menu.NONE, R.string.Join);
		menu.add(Menu.NONE, CONTEXT_ADD, Menu.NONE, R.string.AddInRoster);
		if (item.isVCard()) menu.add(Menu.NONE, CONTEXT_INFO, Menu.NONE, R.string.Info);
		
		super.onCreateContextMenu(menu, v, info);
	}
	
	@Override
	public boolean onContextItemSelected(android.view.MenuItem menu) {
		AdapterContextMenuInfo cm = (AdapterContextMenuInfo) menu.getMenuInfo();
		DiscoItem item = (DiscoItem) list.getItemAtPosition(cm.position);
		String jid = item.getJid();
		
		switch(menu.getItemId()) {
			case CONTEXT_ADD:
				RosterDialogs.addDialog(this, jid);
				break;
			case CONTEXT_REG:
				if (discoItem != null && discoItem.isRegister()) {
					Intent i = new Intent(this, DataFormActivity.class);
			        i.putExtra("reg", true);
			        i.putExtra("jid", jid);
					startActivity(i);
				}
				break;
			case CONTEXT_JOIN:
				if (discoItem != null && discoItem.isMUC()) MucDialogs.joinDialog(this, jid, null);
				break;
			case CONTEXT_INFO:
				if (discoItem != null && discoItem.isVCard()) {
					Intent infoIntent = new Intent(this, VCardActivity.class);
	        		infoIntent.putExtra("jid", jid);
	        		startActivity(infoIntent);
				}
				break;
		}
		return true;
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.discovery, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		String jid = null;
		if (discoItem != null) jid = discoItem.getJid();
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				break;
  	    	case R.id.reg:
  	    		if (jid != null) {
  	    			Intent i = new Intent(this, DataFormActivity.class);
  					i.putExtra("reg", true);
  			        i.putExtra("jid", jid);
  					startActivity(i);
  	    		}
  	    		break;
  	    	case R.id.join:
  	    		if (jid != null) MucDialogs.joinDialog(this, jid, null);
  	    		break;
  	    	case R.id.add:
  	    		if (jid != null) RosterDialogs.addDialog(this, jid);
  	    		break;
  	    	case R.id.info:
  	    		if (jid != null) {
  	    			Intent infoIntent = new Intent(this, VCardActivity.class);
  	        		infoIntent.putExtra("jid", jid);
  	        		startActivity(infoIntent);
  	    		}
  	    	default:
  	    		return false;
		}
		return true;
	}
	
	private class ParseIdentity extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			String node = params[0];
			String server = input.getText().toString();
			discoItem = new DiscoItem();
			discoItem.setJid(server);
			List<DiscoItem> items = new ArrayList<DiscoItem>();
			
			DiscoverItems discoItems;
			try {
				if (node != null && node.length() > 0) discoItems = discoManager.discoverItems(server, node);
				else discoItems = discoManager.discoverItems(server);
				
				DiscoverInfo dci = discoManager.discoverInfo(server);
				Iterator<Feature> itf = dci.getFeatures();
				while (itf.hasNext()) {
					Feature f = itf.next();
					if (f.getVar().equals("jabber:iq:register")) discoItem.setRegister(true);
					else if (f.getVar().equals("vcard-temp")) discoItem.setVCard(true);
					else if (f.getVar().equals("http://jabber.org/protocol/muc")) discoItem.setMUC(true);
				}
			    
			    Iterator<DiscoverItems.Item> i = discoItems.getItems();
			    while (i.hasNext()) {
			    	DiscoverItems.Item item = i.next();
			    	String j = item.getEntityID();
			    	String n = item.getNode();
			    	String name = item.getName();
			    	if (name == null || name.length() <= 0) name = j;
			    	
			    	DiscoItem di = new DiscoItem();
					di.setJid(j);
					di.setNode(n);
					di.setName(name);
					
			    	try {
			    		DiscoverInfo info = discoManager.discoverInfo(j);
			    		if (info.containsFeature("jabber:iq:register")) di.setRegister(true);
			    		if (info.containsFeature("vcard-temp")) di.setVCard(true);
			    		if (info.containsFeature("http://jabber.org/protocol/muc")) di.setMUC(true);
						Iterator<Identity> it = info.getIdentities();
						if (it.hasNext()) {
							Identity identity = it.next();
							String type = identity.getType();
							String category = identity.getCategory();
							
							String iname = identity.getName();
							if (iname != null && iname.length() > 0) di.setName(iname);
							
							di.setType(type);
							di.setCategory(category);
					    }
			    	} catch (XMPPException ex) { }
			    	
			    	items.add(di);
			    }
			} catch (Exception e) { 
//				Toast.makeText(ServiceDiscovery.this, e.getLocalizedMessage(), Toast.LENGTH_LONG);
			}
			
			adapter = new DiscoveryAdapter(ServiceDiscovery.this, items);
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
