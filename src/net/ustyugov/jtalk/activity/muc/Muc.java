package net.ustyugov.jtalk.activity.muc;

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.activity.Chat;
import net.ustyugov.jtalk.adapter.MucRosterAdapter;
import net.ustyugov.jtalk.dialog.ChangeChatDialog;
import net.ustyugov.jtalk.dialog.MucDialogs;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.util.StringUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;

public class Muc extends SherlockActivity implements OnKeyListener, OnItemClickListener, OnItemLongClickListener {
	private ListView mucList;
	private MucRosterAdapter conferencesAdapter;
	private BroadcastReceiver updateReceiver;
	private BroadcastReceiver messageReceiver;
	private JTalkService service;
	private String account;

	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        service = JTalkService.getInstance();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme(prefs.getBoolean("DarkColors", false) ? R.style.AppThemeDark : R.style.AppThemeLight);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setTitle(R.string.MUC);
		setContentView(R.layout.muc);
        
       	LinearLayout muc = (LinearLayout) findViewById(R.id.muc_linear);
       	muc.setBackgroundColor(prefs.getBoolean("DarkColors", false) ? 0xFF000000 : 0xFFFFFFFF);
       	muc.setOnKeyListener(this);
        
       	account = getIntent().getStringExtra("account");
       	
       	conferencesAdapter = new MucRosterAdapter(this, null);
        mucList = (ListView) findViewById(R.id.mucList);
        mucList.setDividerHeight(0);
        mucList.setCacheColorHint(0x00000000);
		mucList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				String item = (String) parent.getItemAtPosition(position);
				if (item.contains("/")) {
					String group = StringUtils.parseBareAddress(item);
					String nick = StringUtils.parseResource(item);
					
					Intent i = new Intent(Muc.this, Chat.class);
//					i.putExtra("account", account);
			        i.putExtra("jid", group + "/" + nick);
			        i.putExtra("username", nick);
			        startActivity(i);
				} else {
					if (service.getCollapsedGroups().contains(item)) 
						while (service.getCollapsedGroups().contains(item)) service.getCollapsedGroups().remove(item);
					else service.getCollapsedGroups().add(item);
					updateList();
				}
			}
		});
		mucList.setOnItemLongClickListener(this);
		mucList.setAdapter(conferencesAdapter);
    }
	
	@Override
	public void onResume() {
		super.onResume();
		service.resetTimer();
	    
	    updateReceiver = new BroadcastReceiver() {
	        @Override
	        public void onReceive(Context context, Intent intent) {
	        	updateList();
	        }
	    };
	    registerReceiver(updateReceiver, new IntentFilter(Constants.PRESENCE_CHANGED));
	    registerReceiver(updateReceiver, new IntentFilter(Constants.UPDATE));
	    
	    messageReceiver = new BroadcastReceiver() {
	        @Override
	        public void onReceive(Context context, Intent intent) {
	        	updateList();
	        }
	    };
	    registerReceiver(messageReceiver, new IntentFilter(Constants.NEW_MUC_MESSAGE));
		updateList();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(messageReceiver);
		unregisterReceiver(updateReceiver);
	}
    
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.muc, menu);
        return super.onCreateOptionsMenu(menu);
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
//			case android.R.id.home:
//				finish();
//				break;
//  	    	case R.id.join:
//  	    		MucDialogs.joinDialog(this, account, null);
//  	    		break;
//  	    	case R.id.bookmarks:
//  	    		startActivity(new Intent(this, Bookmarks.class));
//  	    		break;
//  	    	case R.id.search:
//  	    		startActivity(new Intent(this, MucSearch.class));
//  	    		break;
//  	    	case R.id.chats:
//  	    		ChangeChatDialog.show(this);
//  	    	default:
//  	    		return false;
//		}
		return true;
	}
	
	private void updateList() {
		conferencesAdapter.update();
		conferencesAdapter.notifyDataSetChanged();
	}
	
	public boolean onKey(View view, int code, KeyEvent event) {
		if (KeyEvent.KEYCODE_SEARCH == code) {
			Intent sIntent = new Intent(this, MucSearch.class);
	    	startActivity(sIntent);
		}
		return false;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		String item = (String) parent.getItemAtPosition(position);
		if (item.contains("/")) {
			String group = StringUtils.parseBareAddress(item);
			String nick = StringUtils.parseResource(item);
			
			Intent i = new Intent(Muc.this, Chat.class);
	        i.putExtra("jid", group + "/" + nick);
	        i.putExtra("username", nick);
	        startActivity(i);
		} else {
			if (service.getCollapsedGroups().contains(item)) 
				while (service.getCollapsedGroups().contains(item)) service.getCollapsedGroups().remove(item);
			else service.getCollapsedGroups().add(item);
			updateList();
		}
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
		String item = (String) parent.getItemAtPosition(position);
		String group = StringUtils.parseBareAddress(item);
		String nick = StringUtils.parseResource(item);
		
//		if (item.contains("/")) MucDialogs.userMenu(this, group, nick);
//		else MucDialogs.roomMenu(this, group);
		return true;
	}
}