/*
 * Copyright (C) 2014, Igor Ustyugov <igor@ustyugov.net>
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

package net.ustyugov.jtalk.activity.filetransfer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.jtalk2.R;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.RosterItem;
import net.ustyugov.jtalk.activity.Chat;
import net.ustyugov.jtalk.activity.RosterActivity;
import net.ustyugov.jtalk.adapter.SearchAdapter;
import net.ustyugov.jtalk.service.JTalkService;
import org.jivesoftware.smack.RosterEntry;

public class SendToActivity extends Activity {
    private JTalkService service;
    private SearchAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        service = JTalkService.getInstance();
        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
        setContentView(R.layout.send_to);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.SendFile);

        LinearLayout linear = (LinearLayout) findViewById(R.id.linear);
        linear.setBackgroundColor(Colors.BACKGROUND);

        adapter = new SearchAdapter(this);
        TextView tv = (TextView) findViewById(R.id.label);

        final EditText et = (EditText) findViewById(R.id.search);
        et.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { updateList(s.toString()); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        ListView list = (ListView) findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent intent = getIntent();
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                String type = intent.getType();

                RosterItem item = (RosterItem) adapterView.getItemAtPosition(position);
                String name = item.getName();
                String account = item.getAccount();
                if (item.isGroup() || item.isAccount()) {
                    if (item.isCollapsed()) {
                        while (service.getCollapsedGroups().contains(name)) service.getCollapsedGroups().remove(name);
                        item.setCollapsed(false);
                    } else {
                        service.getCollapsedGroups().add(name);
                        item.setCollapsed(true);
                    }
                    updateList(et.getText().toString());
                } else if (item.isEntry()) {
                    RosterEntry re = item.getEntry();
                    String jid = re.getUser();

                    if (type.equals("text/plain")) {
                        service.setText(jid, intent.getStringExtra(Intent.EXTRA_TEXT));
                        Intent i = new Intent(SendToActivity.this, Chat.class);
                        i.putExtra("account", account);
                        i.putExtra("jid", jid);
                        startActivity(i);
                    } else {
                        Uri data = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                        Intent i = new Intent(SendToActivity.this, SendFileActivity.class);
                        i.putExtra("account", account);
                        i.putExtra("jid", jid);
                        i.putExtra("muc", false);
                        i.setAction(Intent.ACTION_SEND);
                        i.setData(data);
                        startActivity(i);
                    }
                    finish();
                } else if (item.isMuc()) {
                    if (type.equals("text/plain")) {
                        service.setText(item.getName(), intent.getStringExtra(Intent.EXTRA_TEXT));
                        Intent i = new Intent(SendToActivity.this, Chat.class);
                        i.putExtra("account", account);
                        i.putExtra("jid", item.getName());
                        startActivity(i);
                        finish();
                    }
                }
            }
        });

        if (service == null || !service.isAuthenticated()) {
            tv.setVisibility(View.VISIBLE);
            list.setVisibility(View.GONE);
        } else {
            tv.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
            updateList("");
        }
    }

    private void updateList(String search) {
        adapter.update(search);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent i = new Intent(this, RosterActivity.class);
                i.setAction(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
                break;
        }
        return true;
    }
}