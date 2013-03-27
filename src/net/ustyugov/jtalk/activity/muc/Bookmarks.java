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

import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.RosterItem;
import net.ustyugov.jtalk.adapter.BookmarksAdapter;
import net.ustyugov.jtalk.adapter.MainPageAdapter;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.dialog.BookmarksDialogs;
import net.ustyugov.jtalk.dialog.ChangeChatDialog;
import net.ustyugov.jtalk.dialog.MucDialogs;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bookmark.BookmarkManager;
import org.jivesoftware.smackx.bookmark.BookmarkedConference;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
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

public class Bookmarks extends SherlockActivity {
	private ViewPager mPager;
	private ArrayList<View> mPages = new ArrayList<View>();
	private BroadcastReceiver updateReceiver;
	private BroadcastReceiver errorReceiver;
	private JTalkService service;

	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        service = JTalkService.getInstance();
        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
		setTitle(R.string.Bookmarks);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.paged_activity);
        
       	LinearLayout linear = (LinearLayout) findViewById(R.id.linear);
       	linear.setBackgroundColor(Colors.BACKGROUND);
        
        LayoutInflater inflater = LayoutInflater.from(this);
        MainPageAdapter adapter = new MainPageAdapter(mPages);
        
        Cursor cursor = service.getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '" + 1 + "'", null, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			do {
				final String account = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID)).trim();
				
				View bookPage = inflater.inflate(R.layout.list_activity, null);
				bookPage.setTag(account);
				mPages.add(bookPage);
				
		        ListView list = (ListView) bookPage.findViewById(R.id.list);
		        list.setDividerHeight(0);
		        list.setCacheColorHint(0x00000000);
		        list.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
						RosterItem item = (RosterItem) parent.getItemAtPosition(position);
						BookmarkedConference bc = (BookmarkedConference) item.getObject();
						String account = item.getAccount();
						String jid  = bc.getJid();
						String nick = bc.getNickname();
						String pass = bc.getPassword();
						if (nick == null || nick.length() < 1) nick = StringUtils.parseName(service.getConnection(account).getUser());
						if (!service.getJoinedConferences().containsKey(jid)) service.joinRoom(account, jid, nick, pass);
					}
				});
		        
		        list.setOnItemLongClickListener(new OnItemLongClickListener() {
		        	@Override
		        	public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
		        		final RosterItem item = (RosterItem) parent.getItemAtPosition(position);
		        		CharSequence[] items = new CharSequence[3];
		                items[0] = getString(R.string.Users);
		                items[1] = getString(R.string.Edit);
		                items[2] = getString(R.string.Remove);

		                AlertDialog.Builder builder = new AlertDialog.Builder(Bookmarks.this);
		                builder.setTitle(R.string.Actions);
		                builder.setItems(items, new OnClickListener() {
		        			@Override
		        			public void onClick(DialogInterface dialog, int which) {
		        		    	switch (which) {
		        	        	case 0:
		        	        		MucDialogs.showUsersDialog(Bookmarks.this, account, (BookmarkedConference) item.getObject());
		        	        		break;
		        	        	case 1:
		        	        		BookmarksDialogs.EditDialog(Bookmarks.this, account, (BookmarkedConference) item.getObject());
		        	        		break;
		        	        	case 2:
		        	        		try {
                                        BookmarkedConference bc = (BookmarkedConference) item.getObject();
		        	        			BookmarkManager bm = BookmarkManager.getBookmarkManager(service.getConnection(account));
		        	            		bm.removeBookmarkedConference(bc.getJid());
		        	            	} catch (Exception e) {	Log.e("Remove", e.getLocalizedMessage()); }
		        	            	updateBookmarks();
		        		 	        break;
		        		    	}
		        			}
		                });
		                builder.create().show();
		        		return true;
		        	}
		        });
		        
			} while (cursor.moveToNext());
			cursor.close();
		}
        
	    mPager = (ViewPager) findViewById(R.id.pager);
	    mPager.setAdapter(adapter);
	    mPager.setCurrentItem(0);
	        
	    TitlePageIndicator mTitleIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
	    mTitleIndicator.setTextColor(0xFF555555);
	    mTitleIndicator.setViewPager(mPager);
    }
	
	@Override
	public void onResume() {
		super.onResume();
		service.resetTimer();
	    errorReceiver = new BroadcastReceiver() {
	        @Override
	        public void onReceive(Context context, Intent intent) {
	        	Toast.makeText(context, intent.getExtras().getString("error"), Toast.LENGTH_LONG).show();
	        }
	    };
	    registerReceiver(errorReceiver, new IntentFilter(Constants.ERROR));
	    
	    updateReceiver = new BroadcastReceiver() {
	        @Override
	        public void onReceive(Context context, Intent intent) {
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
	    
	    updateBookmarks();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(errorReceiver);
		unregisterReceiver(updateReceiver);
	}
    
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.bookmarks, menu);
        return super.onCreateOptionsMenu(menu);
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				break;
  	    	case R.id.join:
                String account = (String) mPages.get(mPager.getCurrentItem()).getTag();
  	    		MucDialogs.joinDialog(this, account, null, null);
  	    		break;
  	    	case R.id.chats:
  	    		ChangeChatDialog.show(this);
  	    		break;
  	    	case R.id.add:
                String acc = (String) mPages.get(mPager.getCurrentItem()).getTag();
  	    		BookmarksDialogs.AddDialog(this, acc, null, null);
  	    		break;
  	    	default:
  	    		return false;
		}
		return true;
	}
	
	private void updateBookmarks() {
		for (View view : mPages) {
			ProgressBar progress = (ProgressBar) view.findViewById(R.id.progress);
	        ListView list = (ListView) view.findViewById(R.id.list);
	        String account = (String) view.getTag();
			new Init(account, list, progress).execute();
		}
	}
	
	private class Init extends AsyncTask<String, Void, Void> {
		String account;
		BookmarksAdapter adapter;
		ListView list;
		ProgressBar progress;
		
		public Init(String account, ListView list, ProgressBar progress) {
			this.account = account;
			this.list = list;
			this.progress = progress;
		}
		
		@Override
		protected Void doInBackground(String... params) {
			adapter = new BookmarksAdapter(Bookmarks.this, account);
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