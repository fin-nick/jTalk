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

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.jtalk2.R;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.Notify;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.service.JTalkService;

public class AddAccountActivity extends AccountAuthenticatorActivity implements View.OnClickListener {
    private EditText jidEdit, passEdit, resEdit, serEdit, portEdit;
    private CheckBox enabled, tls, sasl;
    private Button okButton, cancelButton;
    private int id = -1;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
        setContentView(R.layout.add_account_activity);
        LinearLayout linear = (LinearLayout) findViewById(R.id.account_linear);
        linear.setBackgroundColor(Colors.BACKGROUND);

        jidEdit = (EditText) findViewById(R.id.account_jid);
        passEdit = (EditText) findViewById(R.id.account_password);
        resEdit = (EditText) findViewById(R.id.account_resource);
        serEdit = (EditText) findViewById(R.id.account_server);
        portEdit = (EditText) findViewById(R.id.account_port);
        enabled = (CheckBox) findViewById(R.id.account_active);
        tls = (CheckBox) findViewById(R.id.account_tls);
        sasl = (CheckBox) findViewById(R.id.account_sasl);
        okButton = (Button) findViewById(R.id.account_ok);
        cancelButton = (Button) findViewById(R.id.account_cancel);

        okButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        final LinearLayout optionsLinear = (LinearLayout) findViewById(R.id.options_linear);
        CheckBox options = (CheckBox) findViewById(R.id.options);
        options.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                optionsLinear.setVisibility(b ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        id = getIntent().getIntExtra("id", -1);
        if (id != -1) {
            setTitle(R.string.Edit);
            String username = "";
            String password = "";
            String resource = "";
            String service = "";
            String e = "";
            String t = "";
            String s = "";
            int port = 5222;

            Cursor cursor = getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, "_id = '" + id + "'", null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                username = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID));
                password = cursor.getString(cursor.getColumnIndex(AccountDbHelper.PASS));
                resource = cursor.getString(cursor.getColumnIndex(AccountDbHelper.RESOURCE));
                if (resource == null || resource.length() < 1) resource = "Android";
                service = cursor.getString(cursor.getColumnIndex(AccountDbHelper.SERVER));

                try {
                    port = Integer.parseInt(cursor.getString(cursor.getColumnIndex(AccountDbHelper.PORT)));
                } catch (NumberFormatException ignored) { }

                e = cursor.getString(cursor.getColumnIndex(AccountDbHelper.ENABLED));
                t = cursor.getString(cursor.getColumnIndex(AccountDbHelper.TLS));
                s = cursor.getString(cursor.getColumnIndex(AccountDbHelper.SASL));
            }

            jidEdit.setText(username);
            passEdit.setText(password);
            resEdit.setText(resource);
            serEdit.setText(service);
            portEdit.setText(port+"");
            enabled.setChecked(e.equals("1"));
            tls.setChecked(t.equals("1"));
            sasl.setChecked(s.equals("1"));
        } else setTitle(R.string.Add);
    }

    @Override
    public void onClick(View view) {
        if (view == cancelButton) {
            finish();
        } else if (view == okButton) {
            String jid = jidEdit.getText().toString();
            if (jid == null) jid = "";
            String pass = passEdit.getText().toString();
            if (pass == null) pass = "";
            String res = resEdit.getText().toString();
            if (res == null || res.length() < 1) res = "Android";
            String ser = serEdit.getText().toString();
            if (ser == null) ser = "";
            String port = portEdit.getText().toString();
            if (port == null) port = "5222";
            boolean e = enabled.isChecked();
            boolean t = tls.isChecked();
            boolean s = sasl.isChecked();

            if (jid.length() < 3 || !jid.contains("@")) {
                Toast.makeText(this, "Incorrect JID", Toast.LENGTH_LONG).show();
            } else {
                ContentValues values = new ContentValues();
                values.put(AccountDbHelper.JID, jid);
                values.put(AccountDbHelper.PASS, pass);
                values.put(AccountDbHelper.RESOURCE, res);
                values.put(AccountDbHelper.SERVER, ser);
                values.put(AccountDbHelper.PORT, port);
                if (e) values.put(AccountDbHelper.ENABLED, "1");
                else values.put(AccountDbHelper.ENABLED, "0");
                if (t) values.put(AccountDbHelper.TLS, "1");
                else values.put(AccountDbHelper.TLS, "0");
                if (s) values.put(AccountDbHelper.SASL, "1");
                else values.put(AccountDbHelper.SASL, "0");

                if (id == -1) {
                    Cursor cursor = getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.JID +" = '" + jid + "'", null, null);
                    if (cursor == null || cursor.getCount() == 0) {
                        getContentResolver().insert(JTalkProvider.ACCOUNT_URI, values);

                        JTalkService service = JTalkService.getInstance();
                        if (service.isAuthenticated(jid)) {
                            service.disconnect(jid);
                            if (service.isAuthenticated()) Notify.updateNotify();
                            else Notify.offlineNotify(service.getGlobalState());
                        }

                        Account account = new Account(jid, getString(R.string.app_name));
                        AccountManager am = AccountManager.get(this);
                        boolean accountCreated  = am.addAccountExplicitly(account, null, null);
                        Bundle extras = getIntent().getExtras();
                        if (extras != null && accountCreated) {
                            AccountAuthenticatorResponse response = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
                            Bundle result = new Bundle();
                            result.putString(AccountManager.KEY_ACCOUNT_NAME, jid);
                            result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.app_name));
                            if (response != null) response.onResult(result);
                        }

                        cursor.close();
                    } else {
                        Toast.makeText(this, "Account is already added!", Toast.LENGTH_LONG).show();
                        return;
                    }
                } else getContentResolver().update(JTalkProvider.ACCOUNT_URI, values, "_id = '" + id + "'", null);

                setResult(RESULT_OK, new Intent().putExtra("account", jid).putExtra("enabled", e));
                finish();
            }
        }
    }
}
