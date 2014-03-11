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

package net.ustyugov.jtalk.activity.muc;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.*;
import com.jtalk2.R;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.service.JTalkService;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;

public class SubjectActivity extends Activity implements View.OnClickListener {
    String account;
    String group;
    String subject;
    TextView textView;
    EditText editText;
    Button okButton;
    Button editButton;
    Button cancelButton;
    MultiUserChat muc;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
        setContentView(R.layout.subject);
        setTitle(R.string.Subject);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        LinearLayout linear = (LinearLayout) findViewById(R.id.linear);
        linear.setBackgroundColor(Colors.BACKGROUND);

        editText = (EditText) findViewById(R.id.subject_edit);
        editText.setVisibility(View.GONE);

        textView = (TextView) findViewById(R.id.subject_view);
        textView.setVisibility(View.VISIBLE);
        textView.setTextSize(Integer.parseInt(getResources().getString(R.string.DefaultFontSize)));
        try {
            textView.setTextSize(Integer.parseInt(prefs.getString("RosterSize", getResources().getString(R.string.DefaultFontSize))));
        } catch (NumberFormatException ignored) { }

        okButton = (Button) findViewById(R.id.ok);
        okButton.setVisibility(View.GONE);
        okButton.setOnClickListener(this);

        cancelButton = (Button) findViewById(R.id.cancel);
        cancelButton.setVisibility(View.GONE);
        cancelButton.setOnClickListener(this);

        editButton = (Button) findViewById(R.id.edit);
        editButton.setVisibility(View.VISIBLE);
        editButton.setOnClickListener(this);


        JTalkService service = JTalkService.getInstance();
        account = getIntent().getStringExtra("account");
        group = getIntent().getStringExtra("jid");
        if (service.getConferencesHash(account).containsKey(group)) {
            muc = service.getConferencesHash(account).get(group);
            if (muc.isJoined()) {
                subject = muc.getSubject();
                if (subject == null) subject = "";
                editText.setText(subject);
                textView.setText(subject);
            } else {
                finish();
            }
        }

    }

    @Override
    public void onClick(View view) {
        if (view == editButton) {
            editText.setText(textView.getText().toString());
            textView.setVisibility(View.GONE);
            editButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.VISIBLE);
            editText.setVisibility(View.VISIBLE);
            okButton.setVisibility(View.VISIBLE);
        } else if (view == cancelButton) {
            textView.setVisibility(View.VISIBLE);
            editButton.setVisibility(View.VISIBLE);
            editText.setText(textView.getText().toString());
            editText.setVisibility(View.GONE);
            okButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
        } else if (view == okButton) {
            subject = editText.getText().toString();
            try {
                muc.changeSubject(subject);
            } catch(XMPPException e) {
                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }

            textView.setText(subject);
            textView.setVisibility(View.VISIBLE);
            editButton.setVisibility(View.VISIBLE);
            editText.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
            okButton.setVisibility(View.GONE);
        }
    }
}
