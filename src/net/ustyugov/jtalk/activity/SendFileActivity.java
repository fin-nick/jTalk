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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.ustyugov.jtalk.Notify;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.jtalk2.R;

public class SendFileActivity extends SherlockActivity implements OnClickListener {
	private static final int RESULT = 1;
	private String jid;
	private Boolean muc = false;
	private EditText description;
	private Button select;
	private Button ok;
	private Button cancel;
	private Spinner spinner;
	private File file;
	private JTalkService service;
	private List<String> resources = new ArrayList<String>();
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		setTheme(prefs.getBoolean("DarkColors", false) ? R.style.AppThemeDark : R.style.AppThemeLight);
		setContentView(R.layout.send_file);
		setTitle(R.string.SendFile);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		service = JTalkService.getInstance();
		jid = getIntent().getStringExtra("jid");
		muc = getIntent().getBooleanExtra("muc", false);
    	
		Iterator<Presence> it = service.getRoster().getPresences(jid);
		while (it.hasNext()) {
			Presence p = it.next();
			if (p.getType() != Presence.Type.unavailable) {
				resources.add(StringUtils.parseResource(p.getFrom()));
			}
		}
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, resources);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		description = (EditText) findViewById(R.id.description);
		
		TextView tv = (TextView) findViewById(R.id.resource);
		tv.setText(muc ? R.string.Nick : R.string.SelectResource);
		
		spinner = (Spinner) findViewById(R.id.file_spinner);
		spinner.setAdapter(adapter);
		
		select = (Button) findViewById(R.id.select_file);
		select.setText(R.string.SelectFile);
		select.setOnClickListener(this);
		
		ok = (Button) findViewById(R.id.ok);
		ok.setOnClickListener(this);
		
		cancel = (Button) findViewById(R.id.cancel);
		cancel.setOnClickListener(this);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK && requestCode == RESULT && data != null) {
			String path = "none";
			Uri uri = data.getData();
			String scheme = uri.getScheme();

			if (scheme.equals("file")) {
				path = uri.getPath();
			} else if (scheme.equals("content")) {
				try {
					String[] proj = { MediaStore.Files.FileColumns.DATA };
				    Cursor cursor = managedQuery(uri, proj, null, null, null);
				    if (cursor != null && cursor.getCount() != 0) {
				        int columnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
				        cursor.moveToFirst();
				        path = cursor.getString(columnIndex);
				    }
				} catch(Exception e) { path = "none"; }
				
			}

			file = new File(path);
			select.setText(path);
		}
	}
	
	public void onClick(View v) {
		if (v == select) {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.SelectFile)), RESULT);
		} else if (v == ok) {
			sendFile();
			finish();
		} else if (v == cancel) {
			finish();
		}
	}
	
  	public void sendFile() {
  		final String fullJid = jid + "/" + (String) spinner.getSelectedItem();
  		final String desc = description.getText().toString();
  		
  		if (file != null && file.exists()) {
  			final String name = file.getName();
  			new Thread() {
				@Override
				public void run() {
					try {
						FileTransferManager ftm = service.getFileTransferManager();
						OutgoingFileTransfer out = ftm.createOutgoingFileTransfer(fullJid);
						out.sendFile(file, desc);
		  				
						while (!out.isDone()) {
							Status status = out.getStatus();
							Notify.fileProgress(name, status);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException ex) { }
						}
						Notify.fileProgress(name, out.getStatus());
		  			} catch (XMPPException e) {
		  				Notify.fileProgress(name, Status.error);
		  			} catch (IllegalArgumentException ie) {
		  				Notify.fileProgress(name, Status.error);
		  			}
				}
			}.start();
  		}
  	}
}
