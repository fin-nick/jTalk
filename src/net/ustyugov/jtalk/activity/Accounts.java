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

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.*;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import net.ustyugov.jtalk.Account;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.Notify;
import net.ustyugov.jtalk.activity.vcard.SetVcardActivity;
import net.ustyugov.jtalk.adapter.AccountsAdapter;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.service.JTalkService;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;

public class Accounts extends SherlockActivity {
    private static final int REQUEST_ACCOUNT = 8;
    private static final int CONTEXT_VCARD = 1;
    private static final int CONTEXT_PRIVACY = 2;
    private static final int CONTEXT_EDIT = 3;
    private static final int CONTEXT_REMOVE = 4;
	
	private ListView list;
	private AccountsAdapter adapter;
    private BroadcastReceiver refreshReceiver;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
        setContentView(R.layout.accounts);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setTitle(R.string.Accounts);
        
        LinearLayout linear = (LinearLayout) findViewById(R.id.accounts_linear);
        linear.setBackgroundColor(Colors.BACKGROUND);

        adapter = new AccountsAdapter(this);
		
		list = (ListView) findViewById(R.id.accounts_List);
        list.setDividerHeight(0);
        list.setCacheColorHint(0x00000000);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Account account = (Account) adapterView.getItemAtPosition(i);
                int id = account.getId();
                startActivityForResult(new Intent(Accounts.this, AddAccountActivity.class).putExtra("id", id), REQUEST_ACCOUNT);
            }
        });

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
    public void onActivityResult(int request, int result, Intent data) {
        if (request == REQUEST_ACCOUNT && result == RESULT_OK) {
            boolean enabled = data.getBooleanExtra("enabled", true);
            String account = data.getStringExtra("account");
            if (enabled) connectDialog(account);
            else {
                JTalkService service = JTalkService.getInstance();
                if (service.isAuthenticated(account)) {
                    service.disconnect(account);
                    if (service.isAuthenticated()) Notify.updateNotify();
                    else Notify.offlineNotify(service.getGlobalState());
                }
            }
        }
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
	     		startActivityForResult(new Intent(this, AddAccountActivity.class), REQUEST_ACCOUNT);
	     		break;
	     	default:
	     		return false;
	    }
	    return true;
	 }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
        JTalkService service = JTalkService.getInstance();
        int position = ((AdapterContextMenuInfo) info).position;
        String account = ((Account)list.getItemAtPosition(position)).getJid();
        menu.add(Menu.NONE, CONTEXT_VCARD, Menu.NONE, R.string.vcard).setEnabled(service.isAuthenticated(account));
        menu.add(Menu.NONE, CONTEXT_PRIVACY, Menu.NONE, R.string.PrivacyLists).setEnabled(service.isAuthenticated(account));
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
        String jid = account.getJid();

        switch(menuitem.getItemId()) {
            case CONTEXT_VCARD:
                startActivity(new Intent(this, SetVcardActivity.class).putExtra("account", jid));
                break;
            case CONTEXT_PRIVACY:
                startActivity(new Intent(this, PrivacyListsActivity.class).putExtra("account", jid));
                break;
            case CONTEXT_EDIT:
                startActivityForResult(new Intent(this, AddAccountActivity.class).putExtra("id", id), REQUEST_ACCOUNT);
                break;
            case CONTEXT_REMOVE:
                JTalkService service = JTalkService.getInstance();
                if (service.isAuthenticated(account.getJid())) {
                    service.disconnect(account.getJid());
                    if (service.isAuthenticated()) Notify.updateNotify();
                    else Notify.offlineNotify(service.getGlobalState());
                }

                android.accounts.Account acc = new android.accounts.Account(jid, getString(R.string.app_name));
                AccountManager am = AccountManager.get(this);
                am.removeAccount(acc, null, null);

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

    public void connectDialog(final String account) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Connect?");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                JTalkService.getInstance().connect(account);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }
}
