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

import java.util.ArrayList;
import java.util.Collection;

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.activity.Chat;
import net.ustyugov.jtalk.adapter.BookmarksAdapter;
import net.ustyugov.jtalk.adapter.MainPageAdapter;
import net.ustyugov.jtalk.adapter.MucRosterAdapter;
import net.ustyugov.jtalk.dialog.BookmarksDialogs;
import net.ustyugov.jtalk.dialog.ChangeChatDialog;
import net.ustyugov.jtalk.dialog.MucDialogs;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bookmark.BookmarkManager;
import org.jivesoftware.smackx.bookmark.BookmarkedConference;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;
import com.viewpagerindicator.TitlePageIndicator;

public class Muc extends SherlockActivity implements OnKeyListener, OnItemLongClickListener {
	private static final int CONTEXT_EDIT = 1;
	private static final int CONTEXT_REMOVE = 2;
	private static final int CONTEXT_USERS = 3;
	
	private BookmarkManager bm;
	private Collection<BookmarkedConference> bCollection;
	private Init task;
	private ListView mucList;
	private ListView bookList;
	private ProgressBar progress;
	private BookmarksAdapter bookmarksAdapter;
	private MucRosterAdapter conferencesAdapter;
	private BroadcastReceiver updateReceiver;
	private BroadcastReceiver messageReceiver;
	private BroadcastReceiver errorReceiver;
	private JTalkService service;
	private SharedPreferences prefs;
	private String[] statusArray;

	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        service = JTalkService.getInstance();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
		setTheme(prefs.getBoolean("DarkColors", false) ? R.style.AppThemeDark : R.style.AppThemeLight);
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.paged_activity);
        
		statusArray = getResources().getStringArray(R.array.statusArray);
		
       	LinearLayout muc = (LinearLayout) findViewById(R.id.linear);
       	muc.setBackgroundColor(prefs.getBoolean("DarkColors", false) ? 0xFF000000 : 0xFFFFFFFF);
       	muc.setOnKeyListener(this);
        
        LayoutInflater inflater = LayoutInflater.from(this);
		View mucPage = inflater.inflate(R.layout.muc, null);
		mucPage.setTag(getString(R.string.MUC));
		View bookPage = inflater.inflate(R.layout.list_activity, null);
		bookPage.setTag(getString(R.string.Bookmarks));
		
		ArrayList<View> mPages = new ArrayList<View>();
	    mPages.add(mucPage);
	    mPages.add(bookPage);
	    
	    MainPageAdapter adapter = new MainPageAdapter(mPages);
	    ViewPager mPager = (ViewPager) findViewById(R.id.pager);
	    mPager.setAdapter(adapter);
	    mPager.setCurrentItem(0);
	        
	    TitlePageIndicator mTitleIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
	    mTitleIndicator.setTextColor(0xFF555555);
	    mTitleIndicator.setViewPager(mPager);
	    if (service.getConferencesHash().isEmpty()) {
	    	mTitleIndicator.setCurrentItem(1);
	    } else mTitleIndicator.setCurrentItem(0);
		
		conferencesAdapter = new MucRosterAdapter(this);
        mucList = (ListView) mucPage.findViewById(R.id.mucList);
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
		
		progress = (ProgressBar) bookPage.findViewById(R.id.progress);
        bookList = (ListView) bookPage.findViewById(R.id.list);
        bookList.setBackgroundColor(prefs.getBoolean("DarkColors", false) ? 0xFF000000 : 0xFFFFFFFF);
        bookList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				BookmarkedConference item = (BookmarkedConference) parent.getAdapter().getItem(position);
				String jid  = item.getJid();
				String nick = item.getNickname();
				if (nick == null || nick.length() < 1) nick = StringUtils.parseName(service.getConnection().getUser());
				String pass = item.getPassword();
				service.joinRoom(jid, nick, pass);
			}
		});
        bookList.setDividerHeight(0);
        bookList.setCacheColorHint(0x00000000);
        registerForContextMenu(bookList);
        
        try {
			bm = BookmarkManager.getBookmarkManager(service.getConnection());
			updateBookmarks();
        } catch (XMPPException e) { }
    }
	
	@Override
	public void onResume() {
		super.onResume();
		service.resetTimer();
	    errorReceiver = new BroadcastReceiver() {
	        @Override
	        public void onReceive(Context context, Intent intent) {
	        	Toast.makeText(context, intent.getExtras().getString("error"), Toast.LENGTH_LONG).show();
	        	updateStatus();
	            updateList();
	        }
	    };
	    registerReceiver(errorReceiver, new IntentFilter(Constants.ERROR));
	    
	    updateReceiver = new BroadcastReceiver() {
	        @Override
	        public void onReceive(Context context, Intent intent) {
	        	updateStatus();
	        	updateList();
	        	if (intent.getBooleanExtra("bookmarks", false)) {
  					updateBookmarks();
  				}
	        	if (intent.getBooleanExtra("join", false)) {
	        		String group = intent.getStringExtra("group");
	        		Toast.makeText(context, "Joined to " + group, Toast.LENGTH_LONG).show();
  				}
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

	    updateStatus();
		updateList();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(errorReceiver);
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
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				break;
  	    	case R.id.join:
  	    		MucDialogs.joinDialog(this, null);
  	    		break;
  	    	case R.id.search:
  	    		startActivity(new Intent(this, MucSearch.class));
  	    		break;
  	    	case R.id.chats:
  	    		ChangeChatDialog.show(this);
  	    		break;
  	    	case R.id.add:
  	    		BookmarksDialogs.AddDialog(this, null, null);
  	    		break;
  	    	default:
  	    		return false;
		}
		return true;
	}
	
	@Override
  	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
		if (v == bookList) {
			menu.add(Menu.NONE, CONTEXT_USERS, Menu.NONE, R.string.Users);
			menu.add(Menu.NONE, CONTEXT_EDIT, Menu.NONE, R.string.Edit);
			menu.add(Menu.NONE, CONTEXT_REMOVE, Menu.NONE, R.string.Remove);
		}
		menu.setHeaderTitle(getString(R.string.Actions));
  		super.onCreateContextMenu(menu, v, info);
  	}
	
	@Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	int position = info.position;
    	BookmarkedConference bc = (BookmarkedConference) bookList.getItemAtPosition(position);
    	String jid = bc.getJid();
    	
    	switch(item.getItemId()) {
    		case CONTEXT_USERS:
    			MucDialogs.showUsersDialog(this, bc);
    			break;
            case CONTEXT_EDIT:
            	BookmarksDialogs.EditDialog(this, jid, bm);
            	break;
            case CONTEXT_REMOVE:
            	try {
            		bm.removeBookmarkedConference(jid);
            	} catch (XMPPException e) {	}
            	updateBookmarks();
            	break;
        }
        return true;
    }
	
	private void updateList() {
		conferencesAdapter.update();
		conferencesAdapter.notifyDataSetChanged();
	}
	
	private void updateStatus() {
    	if (service.isAuthenticated() && service.isStarted()) {
   			String status = statusArray[prefs.getInt("currentSelection", 0)];
   			String substatus = prefs.getString("currentStatus", "");
   			setTitle(status);
   			getSupportActionBar().setSubtitle(substatus);
   		} else {
   			setTitle(getString(R.string.NotConnected));
   			getSupportActionBar().setSubtitle(service.getState());
   		}
    }
	
	public boolean onKey(View view, int code, KeyEvent event) {
		if (KeyEvent.KEYCODE_SEARCH == code) {
			Intent sIntent = new Intent(this, MucSearch.class);
	    	startActivity(sIntent);
		}
		return false;
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
		String item = (String) parent.getItemAtPosition(position);
		String group = StringUtils.parseBareAddress(item);
		String nick = StringUtils.parseResource(item);
		
		if (item.contains("/")) MucDialogs.userMenu(this, group, nick);
		else MucDialogs.roomMenu(this, group);
		return true;
	}
	
	private void updateBookmarks() {
		if (task != null && task.getStatus() == AsyncTask.Status.RUNNING) task.cancel(true);
  		task = new Init();
  		task.execute(null, null, null);
	}
	
	private class Init extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			try {
				bCollection = bm.getBookmarkedConferences();
				bookmarksAdapter = new BookmarksAdapter(Muc.this, bCollection);
			} catch (XMPPException e) {	
				Intent intent = new Intent(Constants.ERROR);
				intent.putExtra("error", "Bookmarks: " + e.getLocalizedMessage());
				sendBroadcast(intent);
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v) {
			super.onPostExecute(v);
		    bookList.setAdapter(bookmarksAdapter);
		    bookList.setVisibility(View.VISIBLE);
		    progress.setVisibility(View.GONE);
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			bookList.setVisibility(View.GONE);
			progress.setVisibility(View.VISIBLE);
		}
	}
}