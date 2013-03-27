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

import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.Template;
import net.ustyugov.jtalk.adapter.TemplatesAdapter;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.db.TemplatesDbHelper;
import net.ustyugov.jtalk.service.JTalkService;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;

public class TemplatesActivity extends SherlockActivity implements OnItemClickListener {
	private static final int CONTEXT_EDIT = 1;
	private static final int CONTEXT_REMOVE = 2;
	
	private JTalkService service;
	private ProgressBar progress;
	private ListView list;
	private Init task;
	private TemplatesAdapter adapter;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
		service = JTalkService.getInstance();
		
		setContentView(R.layout.list_activity);
		setTitle(R.string.Templates);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		LinearLayout linear = (LinearLayout) findViewById(R.id.linear);
    	linear.setBackgroundColor(Colors.BACKGROUND);
    	
		progress = (ProgressBar) findViewById(R.id.progress);
        
        list = (ListView) findViewById(R.id.list);
        list.setOnItemClickListener(this);
        list.setDividerHeight(0);
        list.setCacheColorHint(0x00000000);
        registerForContextMenu(list);
        
        init();
	}
	
	private void init() {
		if (task != null && task.getStatus() == AsyncTask.Status.RUNNING) task.cancel(true);
  		task = new Init();
  		task.execute(null, null, null);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		service.resetTimer();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Template item = (Template) parent.getItemAtPosition(position);
		setResult(RESULT_OK, new Intent(this, Chat.class).putExtra("text", item.getText()));
		finish();
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
	    		createDialog();
	    		break;
	    }
	    return true;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
		menu.add(Menu.NONE, CONTEXT_EDIT, Menu.NONE, R.string.Edit);
		menu.add(Menu.NONE, CONTEXT_REMOVE, Menu.NONE, R.string.Remove);
		menu.setHeaderTitle(getString(R.string.Actions));
	 	super.onCreateContextMenu(menu, v, info);
	}
	
	@Override
	 public boolean onContextItemSelected(android.view.MenuItem item) {
		 AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	     Template template = (Template) list.getItemAtPosition(info.position);
	     int id = template.getId();
	     String text = template.getText();
	     
	     switch(item.getItemId()) {
	     	case CONTEXT_EDIT:
	     		createDialog(id, text);
	            break;
	        case CONTEXT_REMOVE:
	        	getContentResolver().delete(JTalkProvider.TEMPLATES_URI, "_id = '" + id + "'", null);
	        	init();
	           	break;
	     }
	     return true;
	}
	
	private void createDialog() {
		createDialog(-1, null);
	}
	
	private void createDialog(final int id, String text) {
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.set_nick_dialog, (ViewGroup) findViewById(R.id.set_nick_linear));
		
		final EditText edit = (EditText) layout.findViewById(R.id.nick_edit);
		edit.setLines(4);
		if (text != null) edit.setText(text);
	    
	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		builder.setTitle(R.string.Add);
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String message = edit.getText().toString();
				if (message != null && message.length() > 0) {
					ContentValues values = new ContentValues();
	 	            values.put(TemplatesDbHelper.TEXT, message);
	 	            if (id < 0) getContentResolver().insert(JTalkProvider.TEMPLATES_URI, values);
	 	            else getContentResolver().update(JTalkProvider.TEMPLATES_URI, values, "_id = '" + id + "'", null);
				}
				init();
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}
	
	private class Init extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			adapter = new TemplatesAdapter(TemplatesActivity.this);
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
