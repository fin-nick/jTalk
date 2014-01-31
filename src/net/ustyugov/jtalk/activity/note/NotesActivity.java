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

package net.ustyugov.jtalk.activity.note;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.*;
import com.jtalk2.R;
import com.viewpagerindicator.TitlePageIndicator;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.adapter.MainPageAdapter;
import net.ustyugov.jtalk.adapter.note.NotesAdapter;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.service.JTalkService;
import org.jivesoftware.smackx.note.Note;
import org.jivesoftware.smackx.note.NoteManager;

import java.util.ArrayList;

public class NotesActivity extends Activity {
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
        setTitle(R.string.notes);
        getActionBar().setDisplayHomeAsUpEnabled(true);
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

                View templatePage = inflater.inflate(R.layout.list_activity, null);
                templatePage.setTag(account);
                mPages.add(templatePage);

                ListView list = (ListView) templatePage.findViewById(R.id.list);
                list.setDividerHeight(1);
                list.setCacheColorHint(0x00000000);
//                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                    @Override
//                    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
//                        list.performLongClick();
//                    }
//                });

                list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                        final Note note = (Note) parent.getItemAtPosition(position);
                        CharSequence[] items = new CharSequence[2];
                        items[0] = getString(R.string.Edit);
                        items[1] = getString(R.string.Remove);

                        AlertDialog.Builder builder = new AlertDialog.Builder(NotesActivity.this);
                        builder.setTitle(R.string.Actions);
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        createDialog(account, note);
                                        break;
                                    case 1:
                                        try {
                                            NoteManager nm = NoteManager.getNoteManager(service.getConnection(account));
                                            nm.removeNote(note);
                                            updateNotes();
                                        } catch (Exception ignored) { }
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
                if (intent.getBooleanExtra("notes", false)) {
                    updateNotes();
                }
            }
        };
        registerReceiver(updateReceiver, new IntentFilter(Constants.UPDATE));

        updateNotes();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(errorReceiver);
        unregisterReceiver(updateReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.templates, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String account = (String) mPages.get(mPager.getCurrentItem()).getTag();
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.add:
                createDialog(account, null);
                break;
            case R.id.refresh:
                try {
                    NoteManager nm = NoteManager.getNoteManager(service.getConnection(account));
                    nm.updateNotes();
                } catch (Exception ignored) { }
                updateNotes();
                break;
        }
        return true;
    }

    private void updateNotes() {
        for (View view : mPages) {
            ProgressBar progress = (ProgressBar) view.findViewById(R.id.progress);
            ListView list = (ListView) view.findViewById(R.id.list);
            String account = (String) view.getTag();
            new Init(account, list, progress).execute();
        }
    }

    private void createDialog(final String account, final Note note) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.note_dialog, (ViewGroup) findViewById(R.id.note_linear));

        String text = "";
        String title = "";
        String tag = "";
        if (note != null) {
            text = note.getText();
            title = note.getTittle();
            tag = note.getTag();
        }

        final EditText textEdit = (EditText) layout.findViewById(R.id.text_edit);
        if (text != null) textEdit.setText(text);

        final EditText titleEdit = (EditText) layout.findViewById(R.id.title_edit);
        if (title != null) titleEdit.setText(title);

        final EditText tagEdit = (EditText) layout.findViewById(R.id.tag_edit);
        if (tag != null) tagEdit.setText(tag);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        builder.setTitle(R.string.Add);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String text = textEdit.getText().toString();
                String title = titleEdit.getText().toString();
                String tag = tagEdit.getText().toString();
                if (text != null && text.length() > 0) {
                    try {
                        NoteManager nm = NoteManager.getNoteManager(service.getConnection(account));
                        if (note != null) {
                            nm.removeNote(note);
                        }
                        nm.addNote(new Note(title, text, tag));
                        updateNotes();
                    } catch (Exception ignored) {}
                }
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
        String account;
        NotesAdapter adapter;
        ListView list;
        ProgressBar progress;

        public Init(String account, ListView list, ProgressBar progress) {
            this.account = account;
            this.list = list;
            this.progress = progress;
        }

        @Override
        protected Void doInBackground(String... params) {
            adapter = new NotesAdapter(NotesActivity.this, account);
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
