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

import android.content.*;
import android.database.Cursor;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import com.viewpagerindicator.TitlePageIndicator;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.RosterItem;
import net.ustyugov.jtalk.activity.Chat;
import net.ustyugov.jtalk.adapter.MainPageAdapter;
import net.ustyugov.jtalk.adapter.MucRosterAdapter;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.dialog.ChangeChatDialog;
import net.ustyugov.jtalk.dialog.MucDialogs;
import net.ustyugov.jtalk.dialog.RosterDialogs;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.RosterEntry;

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
import org.jivesoftware.smack.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class Muc extends SherlockActivity implements OnKeyListener {
    List<MucRosterAdapter> adapters = new ArrayList<MucRosterAdapter>();
    private BroadcastReceiver updateReceiver;
    private BroadcastReceiver messageReceiver;
    private JTalkService service;
    private ViewPager mPager;
    ArrayList<View> mPages = new ArrayList<View>();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        service = JTalkService.getInstance();
        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
        setTitle(R.string.MUC);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.paged_activity);

        LinearLayout linear = (LinearLayout) findViewById(R.id.linear);
        linear.setBackgroundColor(Colors.BACKGROUND);

        MainPageAdapter adapter = new MainPageAdapter(mPages);
        LayoutInflater inflater = LayoutInflater.from(this);

        Cursor cursor = service.getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '" + 1 + "'", null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                final String account = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID)).trim();

                View page = inflater.inflate(R.layout.list_activity, null);
                page.setTag(account);
                mPages.add(page);

                ListView list = (ListView) page.findViewById(R.id.list);
                list.setDividerHeight(0);
                list.setCacheColorHint(0x00000000);
                list.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                        RosterItem item = (RosterItem) parent.getItemAtPosition(position);
                        String name = item.getName();
                        String account = item.getAccount();

                        if (item.isGroup()) {
                            if (item.isCollapsed()) {
                                while (service.getCollapsedGroups().contains(name)) service.getCollapsedGroups().remove(name);
                                item.setCollapsed(false);
                            } else {
                                service.getCollapsedGroups().add(name);
                                item.setCollapsed(true);
                            }
                            updateList();
                        } else if (item.isEntry()) {
                            RosterEntry re = item.getEntry();
                            String jid = re.getUser();
                            Intent i = new Intent(Muc.this, Chat.class);
                            i.putExtra("account", account);
                            i.putExtra("jid", jid);
                            startActivity(i);
                        } else if (item.isMuc()) {
                            Intent i = new Intent(Muc.this, Chat.class);
                            i.putExtra("account", account);
                            i.putExtra("jid", item.getName());
                            startActivity(i);
                        }
                    }
                });

                list.setOnItemLongClickListener(new OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                        RosterItem item = (RosterItem) parent.getItemAtPosition(position);
                        if (item.isGroup()) {
                            //
                        } else if (item.isEntry()) {
                            String j = item.getEntry().getUser();
                            String group = StringUtils.parseBareAddress(j);
                            String nick = StringUtils.parseResource(j);
                            MucDialogs.userMenu(Muc.this, item.getAccount(), group, nick);
                        } else if (item.isMuc()) {
                            MucDialogs.roomMenu(Muc.this, item.getAccount(), item.getName());
                        }
                        return true;
                    }
                });

                MucRosterAdapter mra = new MucRosterAdapter(this, account);
                list.setAdapter(mra);
                adapters.add(mra);

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
        registerReceiver(messageReceiver, new IntentFilter(Constants.NEW_MESSAGE));
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
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.join:
                String account = (String) mPages.get(mPager.getCurrentItem()).getTag();
                MucDialogs.joinDialog(this, account, null, null);
                break;
            case R.id.bookmarks:
                startActivity(new Intent(this, Bookmarks.class));
                break;
            case R.id.chats:
                ChangeChatDialog.show(this);
                break;
            default:
                return false;
        }
        return true;
    }

    private void updateList() {
        for (MucRosterAdapter adapter : adapters) {
            adapter.update();
            adapter.notifyDataSetChanged();
        }
    }

    public boolean onKey(View view, int code, KeyEvent event) {
        if (KeyEvent.KEYCODE_SEARCH == code) {
            Intent sIntent = new Intent(this, MucSearch.class);
            startActivity(sIntent);
        }
        return false;
    }
}