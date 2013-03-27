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

import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.adapter.AffiliateAdapter;
import net.ustyugov.jtalk.adapter.MainPageAdapter;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.muc.Affiliate;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.MUCAdmin;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;
import com.viewpagerindicator.TitlePageIndicator;

public class MucUsers extends SherlockActivity {
	private static final int CONTEXT_EDIT = 2;
	private static final int CONTEXT_REMOVE = 3;
	
	private String account;
	private MultiUserChat muc;
	private ListView ownersList;
	private ListView adminsList;
	private ListView membersList;
	private ListView outcastsList;
	
	private boolean newJid = false;
	private String editedJid;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		String group = getIntent().getStringExtra("group");
		account = getIntent().getStringExtra("account");
		muc = JTalkService.getInstance().getConferencesHash(account).get(group);

        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
		setContentView(R.layout.paged_activity);
		setTitle(R.string.Users);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
       	LinearLayout chat_linear = (LinearLayout) findViewById(R.id.linear);
       	chat_linear.setBackgroundColor(Colors.BACKGROUND);
		
       	account = getIntent().getExtras().getString("account");
       	
		LayoutInflater inflater = LayoutInflater.from(MucUsers.this);
		View ownersPage = inflater.inflate(R.layout.muc_users_page, null);
		View adminsPage = inflater.inflate(R.layout.muc_users_page, null);
		View membersPage = inflater.inflate(R.layout.muc_users_page, null);
		View outcastsPage = inflater.inflate(R.layout.muc_users_page, null);
		
		ownersPage.setTag("Owners");
		adminsPage.setTag("Admins");
		membersPage.setTag("Members");
		outcastsPage.setTag("Outcast");
		
		ownersList = (ListView) ownersPage.findViewById(R.id.list);
	    adminsList = (ListView) adminsPage.findViewById(R.id.list);
	    membersList = (ListView) membersPage.findViewById(R.id.list);
	    outcastsList = (ListView) outcastsPage.findViewById(R.id.list);
	    
	    ownersList.setAdapter(new AffiliateAdapter(this));
	    adminsList.setAdapter(new AffiliateAdapter(this));
	    membersList.setAdapter(new AffiliateAdapter(this));
	    outcastsList.setAdapter(new AffiliateAdapter(this));
	    
	    registerForContextMenu(ownersList);
	    registerForContextMenu(adminsList);
	    registerForContextMenu(membersList);
	    registerForContextMenu(outcastsList);

	    ArrayList<View> mPages = new ArrayList<View>();
	    mPages.add(ownersPage);
	    mPages.add(adminsPage);
	    mPages.add(membersPage);
	    mPages.add(outcastsPage);
	        
	    MainPageAdapter adapter = new MainPageAdapter(mPages);
	    ViewPager mPager = (ViewPager) findViewById(R.id.pager);
	    mPager.setAdapter(adapter);
	    mPager.setCurrentItem(0);
	        
	    TitlePageIndicator mTitleIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
	    mTitleIndicator.setTextColor(0xFF555555);
	    mTitleIndicator.setViewPager(mPager);
	    mTitleIndicator.setCurrentItem(0);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		update();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.muc_users, menu);
        return super.onCreateOptionsMenu(menu);
    }
   
   @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case android.R.id.home:
    			finish();
    			break;
    		case R.id.add:
    			newJid = true;
    			editedJid = "";
    			showDialog();
    			break;
    	}
    	return true;
   }
   
   @Override
   public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
		menu.setHeaderTitle(R.string.Actions);
		menu.add(Menu.NONE, CONTEXT_EDIT, Menu.NONE, R.string.Edit);
		menu.add(Menu.NONE, CONTEXT_REMOVE, Menu.NONE, R.string.Remove);
		super.onCreateContextMenu(menu, v, info);
 	}
	
	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		TextView v = (TextView) info.targetView.findViewById(R.id.item);
		String jid = v.getText().toString();
		
		switch(item.getItemId()) {
			case CONTEXT_REMOVE:
				MUCAdmin.Item i = new MUCAdmin.Item("none", null);
				i.setJid(jid);
				
				MUCAdmin admin = new MUCAdmin();
				admin.setType(IQ.Type.SET);
				admin.setTo(muc.getRoom());
				admin.addItem(i);
				
				JTalkService.getInstance().getConnection(account).sendPacket(admin);
				update();
				break;
			case CONTEXT_EDIT:
				newJid = false;
				editedJid = jid;
				showDialog();
				break;
		}
		return true;
	}
   
   private void showDialog() {
	   LayoutInflater inflater = getLayoutInflater();
	   View layout = inflater.inflate(R.layout.set_affiliate_dialog, (ViewGroup) findViewById(R.id.set_affiliate_linear));
	   
	   final EditText jidEdit = (EditText) layout.findViewById(R.id.jid);
	   jidEdit.setEnabled(newJid);
	   jidEdit.setText(editedJid);
	    
	    String[] affiliations = new String[4];
	    affiliations[0] = "owner";
	    affiliations[1] = "admin";
	    affiliations[2] = "member";
	    affiliations[3] = "outcast";
	    
	    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, affiliations);
	    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    
	    final Spinner spinner = (Spinner) layout.findViewById(R.id.spinner);
	    spinner.setAdapter(arrayAdapter);
			    
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		builder.setTitle(getString(R.string.Edit));
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String aff = (String) spinner.getSelectedItem();
				
				MUCAdmin.Item i = new MUCAdmin.Item(aff, null);
				i.setJid(jidEdit.getText().toString());
						
				MUCAdmin admin = new MUCAdmin();
				admin.setType(IQ.Type.SET);
				admin.setTo(muc.getRoom());
				admin.addItem(i);
						
				JTalkService.getInstance().getConnection(account).sendPacket(admin);
				update();
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
   }
   
   private void update() {
	   new Thread() {
		   public void run() {
			   MucUsers.this.runOnUiThread(new Runnable() {
				  public void run() {
					  if (muc != null) {
						   try {
							   Collection<Affiliate> ownersColl = muc.getOwners();
							   ((AffiliateAdapter) ownersList.getAdapter()).update(ownersColl);
							   
							   Collection<Affiliate> adminsColl = muc.getAdmins();
							   ((AffiliateAdapter) adminsList.getAdapter()).update(adminsColl);
							   
							   Collection<Affiliate> membersColl = muc.getMembers();
							   ((AffiliateAdapter) membersList.getAdapter()).update(membersColl);
							   
							   Collection<Affiliate> outcastsColl = muc.getOutcasts();
							   ((AffiliateAdapter) outcastsList.getAdapter()).update(outcastsColl);
							   
						   } catch(XMPPException e) { 
							   Log.i("EXCEPTION", e.getLocalizedMessage());
						   }
					   }
				  }
			   });
		   }
	   }.start();
   }
}
