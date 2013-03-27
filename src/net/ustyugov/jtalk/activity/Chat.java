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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.widget.*;
import com.actionbarsherlock.widget.SearchView;
import net.ustyugov.jtalk.*;
import net.ustyugov.jtalk.activity.filetransfer.SendFileActivity;
import net.ustyugov.jtalk.activity.vcard.VCardActivity;
import net.ustyugov.jtalk.adapter.*;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.db.MessageDbHelper;
import net.ustyugov.jtalk.dialog.*;
import net.ustyugov.jtalk.service.JTalkService;
import net.ustyugov.jtalk.view.MyListView;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.jtalk2.R;

public class Chat extends SherlockActivity implements View.OnClickListener, OnScrollListener, OnItemLongClickListener {
    public static final int REQUEST_TEMPLATES = 1;

    private boolean isMuc = false;
    private boolean isPrivate = false;
    private boolean showStatuses = false;
    private MultiUserChat muc;

    private SharedPreferences prefs;
    private Menu menu;

    private LinearLayout sidebar;
    private ChatAdapter  listAdapter;
    private MucChatAdapter listMucAdapter;
    private OpenChatsAdapter chatsAdapter;
    private ChatsSpinnerAdapter chatsSpinnerAdapter;
    private MucUserAdapter usersAdapter;
    private MyListView listView;
    private ListView chatsList;
    private ListView nickList;
    private List<MessageItem> msgList = new ArrayList<MessageItem>();
    private List<String> selectedMessages = new ArrayList<String>();
    private EditText messageInput;
    private Button sendButton;

    private String jid;
    private String account;
    private String resource;
    private String searchString = "";
    private boolean compose = false;
    private int maxCount = 0;
    private int maxMucCount = 0;

    private BroadcastReceiver textReceiver;
    private BroadcastReceiver finishReceiver;
    private BroadcastReceiver msgReceiver;
    private BroadcastReceiver receivedReceiver;
    private BroadcastReceiver presenceReceiver;
    private BroadcastReceiver composeReceiver;

    private JTalkService service;
    private Smiles smiles;

    private RosterItem rosterItem;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        service = JTalkService.getInstance();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        showStatuses = prefs.getBoolean("ShowStatus", false);
        try {
            maxCount = Integer.parseInt(prefs.getString("MaxLogMessages", "0"));
            maxMucCount = Integer.parseInt(prefs.getString("MaxMucMessages", "0"));
        } catch (NumberFormatException ignored) {	}

        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);

        chatsSpinnerAdapter = new ChatsSpinnerAdapter(this);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(chatsSpinnerAdapter, new OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int position, long itemId) {
                RosterItem item = chatsSpinnerAdapter.getItem(position);
                String a = item.getAccount();
                String j = jid;
                if (rosterItem != null && item != rosterItem) {
                    rosterItem = item;
                    if (item.isEntry() || item.isSelf()) j = item.getEntry().getUser();
                    else if (item.isMuc()) j = item.getName();
                    Intent intent = new Intent();
                    intent.putExtra("jid", j);
                    intent.putExtra("account", a);
                    setIntent(intent);
                    onPause();
                    onResume();
                }
                return true;
            }
        });

        setContentView(R.layout.chat);

        LinearLayout linear = (LinearLayout) findViewById(R.id.chat_linear);
        linear.setBackgroundColor(Colors.BACKGROUND);

        smiles = new Smiles(this);

        sidebar = (LinearLayout) findViewById(R.id.sidebar);

        chatsAdapter = new OpenChatsAdapter(this, false);
        chatsList = (ListView) findViewById(R.id.open_chat_list);
        chatsList.setCacheColorHint(0x00000000);
        chatsList.setDividerHeight(0);
        chatsList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {
                if (position > 0) {
                    RosterItem item = (RosterItem) parent.getItemAtPosition(position);
                    String j = null;
                    if (item.isEntry()) j = item.getEntry().getUser();
                    else if (item.isMuc()) j = item.getName();
                    if (j != null && !j.equals(jid)) {
                        Intent intent = new Intent();
                        intent.putExtra("account", item.getAccount());
                        intent.putExtra("jid", j);
                        setIntent(intent);
                        onPause();
                        onResume();
                    }
                } else {
                    service.setSidebarMode("users");
                    updateChats();
                }
            }
        });
        chatsList.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                if (position > 0) {
                    RosterItem item = (RosterItem) parent.getItemAtPosition(position);
                    if (item.isEntry()) {
                        RosterEntry entry = item.getEntry();
                        if (entry != null) {
                            String j = entry.getUser();
                            if (service.getConferencesHash(item.getAccount()).containsKey(j)) {
                                String group = StringUtils.parseBareAddress(j);
                                String nick = StringUtils.parseResource(j);
                                MucDialogs.userMenu(Chat.this, item.getAccount(), group, nick);
                            } else RosterDialogs.ContactMenuDialog(Chat.this, item);
                        }
                    } else if (item.isMuc()) {
                        MucDialogs.roomMenu(Chat.this, item.getAccount(), item.getName());
                    }
                }
                return true;
            }
        });

        listAdapter = new ChatAdapter(this, smiles);
        listMucAdapter = new MucChatAdapter(this, smiles);
        listView = (MyListView) findViewById(R.id.chat_list);
        listView.setFocusable(true);
        listView.setCacheColorHint(0x00000000);
        listView.setOnScrollListener(this);
        listView.setDividerHeight(0);
        listView.setAdapter(listAdapter);

        nickList = (ListView) findViewById(R.id.muc_user_list);
        nickList.setCacheColorHint(0x00000000);
        nickList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {
                RosterItem item = (RosterItem) parent.getItemAtPosition(position);
                if (item.isEntry()) {
                    String separator = prefs.getString("nickSeparator", ", ");

                    String nick = item.getName();
                    String text = messageInput.getText().toString();
                    if (text.length() > 0) {
                        text += " " + nick + separator;
                    } else {
                        text = nick + separator;
                    }
                    messageInput.setText(text);
                    messageInput.setSelection(messageInput.getText().length());
                } else {
                    service.setSidebarMode("chats");
                    updateChats();
                }
            }
        });
        nickList.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                RosterItem item = (RosterItem) parent.getItemAtPosition(position);
                if (item.isEntry()) {
                    String nick = item.getName();
                    MucDialogs.userMenu(Chat.this, account, jid, nick);
                    return true;
                } else return false;
            }
        });

        messageInput = (EditText)findViewById(R.id.messageInput);
        messageInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (prefs.getBoolean("SendOnEnter", false)) {
                        if (keyEvent.isShiftPressed() || keyEvent.isAltPressed()) messageInput.append("\n");
                        else onClick(sendButton);
                        return true;
                    }
                }
                return false;
            }
        });

        sendButton  = (Button)findViewById(R.id.SendButton);
        sendButton.setEnabled(false);
        sendButton.setOnClickListener(this);
        sendButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!isPrivate && !isMuc) {
                    String message = messageInput.getText().toString();
                    new SendToResourceDialog(Chat.this, account, jid, message).show();
                }
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        compose = false;
        jid = getIntent().getStringExtra("jid");
        account = getIntent().getStringExtra("account");

        if (service.getConferencesHash(account).containsKey(jid)) {
            isMuc = true;
            muc = service.getConferencesHash(account).get(jid);
            messageInput.setHint("From " + StringUtils.parseName(account));

            String group = listMucAdapter.getGroup();
            if (listView.getAdapter() instanceof ChatAdapter) {
                listView.setAdapter(listMucAdapter);
                listView.setScroll(true);
            }
            else {
                if (group != null && group.equals(jid)) listView.setScroll(false); else listView.setScroll(true);
            }
        } else {
            isMuc = false;
            muc = null;
            resource = StringUtils.parseResource(jid);

            if (!service.getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid))) {
                jid = StringUtils.parseBareAddress(jid);
                isPrivate = false;
            } else isPrivate = true;

            if (resource == null || resource.equals("")) resource = service.getResource(account, jid);

            if (resource != null && !resource.equals("")) {
                messageInput.setHint("To " + resource + " from " + StringUtils.parseName(account));
            } else messageInput.setHint("From " + StringUtils.parseName(account));

            String j = listAdapter.getJid();
            listAdapter.update(jid, msgList, searchString);
            if (listView.getAdapter() instanceof MucChatAdapter) {
                listView.setAdapter(listAdapter);
                listView.setScroll(true);
            }
            else {
                if (j != null && j.equals(jid)) listView.setScroll(false); else listView.setScroll(true);
            }

            if (!service.getActiveChats(account).contains(jid)) {
                setLastMessagesCounter();
                service.addActiveChat(account, jid);
            }
        }

        service.setCurrentJid(jid);
        service.removeUnreadMesage(account, jid);
        service.removeHighlight(account, jid);

        listView.setOnItemLongClickListener(this);

        usersAdapter = new MucUserAdapter(this, account, jid);
        nickList.setAdapter(usersAdapter);
        chatsList.setAdapter(chatsAdapter);

        messageInput.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                int length = s.length();
                if (length > 0) {
                    if (!isMuc) {
                        if (!compose) {
                            compose = true;
                            service.setChatState(account, jid, ChatState.composing);
                        }
                    }
                    sendButton.setEnabled(true);
                } else {
                    if (!isMuc) {
                        if (compose) {
                            compose = false;
                            service.setChatState(account, jid, ChatState.active);
                        }
                    }
                    sendButton.setEnabled(false);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        messageInput.setText(service.getText(jid));
        messageInput.setSelection(messageInput.getText().length());
        if (!prefs.getBoolean("NoMaxLines", true)) messageInput.setMaxLines(3);

        if (service.isAuthenticated()) Notify.updateNotify();
        else Notify.offlineNotify(service.getGlobalState());

        updateChats();
        updateUsers();
        updateStatus();

        registerReceivers();
        service.resetTimer();

        if (!isMuc) service.setChatState(account, jid, ChatState.active);
        createOptionMenu();

        int position = chatsSpinnerAdapter.getPosition(account, jid);
        getSupportActionBar().setSelectedNavigationItem(position);
        rosterItem = chatsSpinnerAdapter.getItem(position);

        if (searchString.length() > 0) {
            if (menu != null) {
                MenuItem item = menu.findItem(R.id.search);
                item.expandActionView();
            }
        }

        updateList();

        int unreadMessages = service.getMessagesCount(account, jid);
        int lastPosition = service.getLastPosition(jid);
        if (lastPosition >= 0) {
            listView.setScroll(false);
            listView.setSelection(lastPosition);
        } else {
            if (unreadMessages > 1) {
                listView.setScroll(false);
                listView.setSelection(listView.getCount() - (unreadMessages + 1));
            } else {
                if (listView.isScroll()) listView.setSelection(listView.getCount());
            }
        }

        if (account.equals(jid)) {
            service.removeMessagesCountForJid(account, jid);
        }
        service.removeMessagesCount(account, jid);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceivers();
        compose = false;
        if (!isMuc)  {
            service.setChatState(account, jid, ChatState.active);
            service.setResource(account, jid, resource);
            if (msgList.isEmpty()) service.removeActiveChat(account, jid);
        }
        service.setCurrentJid("me");
        service.setText(jid, messageInput.getText().toString());
        if (!listView.isScroll()) service.addLastPosition(jid, listView.getFirstVisiblePosition());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (msgList.isEmpty()) {
            if (!isMuc) service.setChatState(account, jid, ChatState.gone);
        }
        msgList.clear();
        jid = null;
        account = null;
    }

    @Override
    public boolean onKeyUp(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_SEARCH && Build.VERSION.SDK_INT >= 8) {
            MenuItem item = menu.findItem(R.id.search);
            item.expandActionView();
        }
        return super.onKeyUp(key, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        createOptionMenu();
        return true;
    }

    private void createOptionMenu() {
        if (menu != null) {
            menu.clear();
            final MenuInflater inflater = getSupportMenuInflater();
            if (isMuc) {
                inflater.inflate(R.menu.muc_chat, menu);
            } else {
                inflater.inflate(R.menu.chat, menu);
                if (isPrivate) menu.findItem(R.id.resource).setVisible(false);
                else menu.findItem(R.id.resource).setVisible(true);
            }

            if (Build.VERSION.SDK_INT >= 8) {
                OnActionExpandListener listener = new OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        searchString = "";
                        updateList();
                        createOptionMenu();
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        return true;
                    }
                };

                SearchView searchView = new com.actionbarsherlock.widget.SearchView(getSupportActionBar().getThemedContext());
                searchView.setQueryHint(getString(android.R.string.search_go));
                searchView.setSubmitButtonEnabled(false);
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextChange(String newText) {
                        return true;
                    }
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        searchString = query;
                        updateList();
                        return true;
                    }
                });

                MenuItem item = menu.findItem(R.id.search);
                item.setActionView(searchView);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
                item.setOnActionExpandListener(listener);
            } else menu.removeItem(R.id.search);

            if (prefs.getBoolean("InMUC", false)) menu.removeItem(R.id.sidebar);
            else {
                MenuItem item = menu.findItem(R.id.sidebar);
                item.setTitle(prefs.getBoolean("EnabledSidebar", true) ? R.string.HideSidebar : R.string.ShowSidebar);
            }

            if (!prefs.getBoolean("ShowSmiles", true)) menu.removeItem(R.id.smile);
            super.onCreateOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                startActivity(new Intent(this, RosterActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
                break;
            case R.id.smile:
                smiles.showDialog();
                break;
            case R.id.nick:
                final UsersAdapter adapter = new UsersAdapter(this, account, jid);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.PasteNick);
                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Chat.this);
                        String separator = prefs.getString("nickSeparator", ", ");
                        String nick = adapter.getItem(which).getName();

                        messageInput.setText(messageInput.getText() + nick + separator);
                        messageInput.setSelection(messageInput.getText().length());
                    }
                });
                builder.create().show();
                break;
            case R.id.subj:
                MucDialogs.subjectDialog(this, account, jid);
                break;
            case R.id.templates:
                startActivityForResult(new Intent(this, TemplatesActivity.class), REQUEST_TEMPLATES);
                break;
            case R.id.resource:
                final List<String> list = new ArrayList<String>();
                list.add("Auto");
                Iterator<Presence> it =  service.getRoster(account).getPresences(jid);
                while (it.hasNext()) {
                    Presence p = it.next();
                    if (p.isAvailable()) list.add(StringUtils.parseResource(p.getFrom()));
                }

                CharSequence[] array = new CharSequence[list.size()];
                for (int i = 0; i < list.size(); i++) array[i] = list.get(i);

                AlertDialog.Builder b = new AlertDialog.Builder(this);
                b.setTitle(R.string.SelectResource);
                b.setItems(array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String res = "" ;
                        if (which > 0) {
                            res = list.get(which);
                        }
                        if (res.length() > 0) res = jid + "/" + res;
                        else res = jid;
                        resource = "";
                        Intent intent = new Intent();
                        intent.putExtra("account", account);
                        intent.putExtra("jid", res);
                        setIntent(intent);
                        onPause();
                        onResume();
                    }
                });
                b.create().show();
                break;
            case R.id.sidebar:
                boolean showSidebar = prefs.getBoolean("EnabledSidebar", true);
                if (showSidebar) service.setPreference("EnabledSidebar", false);
                else service.setPreference("EnabledSidebar", true);
                updateChats();
                break;
            case R.id.info:
                Intent infoIntent = new Intent(this, VCardActivity.class);
                infoIntent.putExtra("account", account);
                infoIntent.putExtra("jid", jid);
                startActivity(infoIntent);
                break;
            case R.id.file:
                Intent intent = new Intent(this, SendFileActivity.class);
                intent.putExtra("account", account);
                intent.putExtra("jid", jid);
                startActivity(intent);
                break;
            case R.id.invite:
                MucDialogs.inviteDialog(this, account, jid);
                break;
            case R.id.history:
                loadStory();
                updateList();
                break;
            case R.id.delete_history:
                getContentResolver().delete(JTalkProvider.CONTENT_URI, "jid = '" + jid + "'", null);
                msgList.clear();
                updateList();
                break;
            case R.id.chats:
                ChangeChatDialog.show(this);
                break;
            case R.id.clear:
                clearChat();
                break;
            case R.id.close:
                closeChat();
                break;
            case R.id.leave:
                finish();
                service.leaveRoom(account, jid);
                break;
            case R.id.search:
                if (!item.isActionViewExpanded()) {
                    menu.removeItem(R.id.sidebar);
                    menu.removeItem(R.id.smile);
                    item.expandActionView();
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_TEMPLATES) {
            String text = data.getStringExtra("text");
            String oldtext = service.getText(jid);
            messageInput.setText(oldtext + text);
            messageInput.setSelection(messageInput.getText().length());
        }
    }

    @Override
    public void onClick(View v) {
        if (v == sendButton) {
            if (messageInput.getText().length() > 0) {
                service.resetTimer();
                sendMessage();
                messageInput.setText("");
                if (prefs.getBoolean("HideKeyboard", true)) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(messageInput.getWindowToken(), 0, null);
                }
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long idx) {
        CharSequence[] items;
        final MessageItem message = (MessageItem) parent.getAdapter().getItem(position);

        if (message.getName().equals(getResources().getString(R.string.Me))) {
            items = new CharSequence[6];
            items[4] = getString(R.string.Edit);
        }
        else if (message.containsCaptcha()) {
            items = new CharSequence[6];
            items[4] = "Captcha";
        }
        else if (service.getConferencesHash(account).containsKey(jid)) {
            items = new CharSequence[6];
            items[4] = getString(R.string.Reply);
        }
        else items = new CharSequence[5];

        if (message.isSelected()) items[0] = getString(R.string.DeselectMessage);
        else items[0] = getString(R.string.SelectMessage);
        items[1] = getString(R.string.Quote);
        items[2] = getString(R.string.Copy);
        items[3] = getString(R.string.SelectText);
        items[items.length-1] = getString(R.string.DeselectAllMessages);

        AlertDialog.Builder builder = new AlertDialog.Builder(Chat.this);
        builder.setTitle(R.string.Actions);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        String baseId = message.getBaseId();
                        if (selectedMessages.contains(baseId)) selectedMessages.remove(baseId);
                        else selectedMessages.add(baseId);
                        updateList();
                        break;
                    case 1:
                        MessageDialogs.QuoteDialog(Chat.this, msgList, position);
                        break;
                    case 2:
                        MessageDialogs.CopyDialog(Chat.this, msgList, position);
                        break;
                    case 3:
                        MessageDialogs.SelectTextDialog(Chat.this, message);
                        break;
                    case 4:
                        if (message.containsCaptcha()) {
                            String id = message.getId();

                            JTalkService.getInstance().addDataForm(id, message.getForm());
                            Intent in = new Intent(Chat.this, DataFormActivity.class);
                            in.putExtra("id", id);
                            in.putExtra("cap", true);
                            in.putExtra("jid", message.getName());
                            in.putExtra("bob", message.getBob().getData());
                            in.putExtra("cid", message.getBob().getCid());
                            startActivity(in);
                            break;
                        } else if (message.getName().equals(getString(R.string.Me))) {
                            MessageDialogs.EditMessageDialog(Chat.this, account, message, jid);
                            break;
                        } else if (service.getConferencesHash(account).containsKey(jid)) {
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Chat.this);
                            String separator = prefs.getString("nickSeparator", ", ");

                            Intent intent = new Intent(Constants.PASTE_TEXT);
                            intent.putExtra("text", message.getName() + separator);
                            sendBroadcast(intent);
                            break;
                        } else {

                        }
                    case 5:
                        selectedMessages.clear();
                        updateList();
                        break;
                }
            }
        });
        builder.create().show();
        return true;
    }

    private void updateMessage(String id, String body) {
        for (MessageItem item : msgList) {
            if (item.getType() == MessageItem.Type.message) {
                if (id.equals(item.getId())) {
                    item.setBody(body);
                    item.setEdited(true);
                    listAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void updateList() {
        boolean scroll = listView.isScroll();
        int count = service.getMsgCount(jid);
        if (isMuc) {
            if (maxMucCount > 0) count = maxMucCount;
        } else {
            if (maxCount > 0) count = maxCount;
        }

        msgList.clear();
        Cursor cursor;
        if (showStatuses) {
            cursor = getContentResolver().query(JTalkProvider.CONTENT_URI, null, "jid = '" + jid + "'", null, MessageDbHelper._ID);
        } else {
            cursor = getContentResolver().query(JTalkProvider.CONTENT_URI, null, "jid = '" + jid + "' AND type = 'message'", null, MessageDbHelper._ID);
        }

        if (cursor != null && cursor.getCount() > 0 && count > 0) {
            if (cursor.getCount() > count) {
                cursor.moveToPosition(cursor.getCount()-count);
            } else cursor.moveToFirst();

            do {
                String baseId = cursor.getString(cursor.getColumnIndex(MessageDbHelper._ID));
                String id = cursor.getString(cursor.getColumnIndex(MessageDbHelper.ID));
                String nick = cursor.getString(cursor.getColumnIndex(MessageDbHelper.NICK));
                String type = cursor.getString(cursor.getColumnIndex(MessageDbHelper.TYPE));
                String stamp = cursor.getString(cursor.getColumnIndex(MessageDbHelper.STAMP));
                String body = cursor.getString(cursor.getColumnIndex(MessageDbHelper.BODY));
                boolean received = Boolean.valueOf(cursor.getString(cursor.getColumnIndex(MessageDbHelper.RECEIVED)));

                MessageItem item = new MessageItem(account, jid);
                item.setBaseId(baseId);
                item.setId(id);
                item.setName(nick);
                item.setType(MessageItem.Type.valueOf(type));
                item.setTime(stamp);
                item.setBody(body);
                item.setReceived(received);
                item.select(selectedMessages.contains(baseId));

                msgList.add(item);
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (isMuc) {
            listMucAdapter.update(jid, muc.getNickname(), msgList, searchString);
            listMucAdapter.notifyDataSetChanged();
        } else {
            listAdapter.update(jid, msgList, searchString);
            listAdapter.notifyDataSetChanged();
        }

        if (prefs.getBoolean("AutoScroll", true)) {
            if (scroll && listView.getCount() >= 1) {
                listView.setSelection(listView.getCount());
            }
        }
    }

    private void updateChats() {
        chatsSpinnerAdapter.update();
        chatsSpinnerAdapter.notifyDataSetChanged();

        if (prefs.getBoolean("InMUC", false) && isMuc) {
            sidebar.setVisibility(View.VISIBLE);
            nickList.setVisibility(View.VISIBLE);
            chatsList.setVisibility(View.GONE);
        } else if (prefs.getBoolean("InMUC", false) && !isMuc) {
            sidebar.setVisibility(View.GONE);
        } else {
            if (prefs.getBoolean("EnabledSidebar", true)) {
                sidebar.setVisibility(View.VISIBLE);
                chatsAdapter.update();
                chatsAdapter.notifyDataSetChanged();

                if (isMuc && service.getSidebarMode().equals("users")) {
                    nickList.setVisibility(View.VISIBLE);
                    chatsList.setVisibility(View.GONE);
                } else {
                    nickList.setVisibility(View.GONE);
                    chatsList.setVisibility(View.VISIBLE);
                }
            } else {
                sidebar.setVisibility(View.GONE);
            }
        }
    }

    private void updateUsers() {
        if (sidebar.getVisibility() == View.VISIBLE) {
            usersAdapter.update();
            usersAdapter.notifyDataSetChanged();
        }
    }

    private void updateStatus() {
        chatsSpinnerAdapter.notifyDataSetChanged();

        ActionBar ab = getSupportActionBar();
        ab.setDisplayUseLogoEnabled(true);
        if (isMuc) ab.setLogo(service.getIconPicker().getMucDrawable());
        else ab.setLogo(service.getIconPicker().getDrawableByPresence(service.getPresence(account, jid)));
    }

    private void registerReceivers() {
        textReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                String text = i.getExtras().getString("text");
                if (i.getBooleanExtra("jubo", false)) {
                    Intent intent = new Intent();
                    intent.putExtra("account", account);
                    intent.putExtra("jid", "juick@juick.com");
                    setIntent(intent);
                    onPause();
                    onResume();
                }

                int pos = messageInput.getSelectionEnd();
                String oldText = messageInput.getText().toString();
                String newText = oldText.substring(0, pos) + text + oldText.substring(pos);
                messageInput.setText(newText);
                messageInput.setSelection(messageInput.getText().length());
            }
        };

        msgReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String user = intent.getExtras().getString("jid");
                boolean clear = intent.getBooleanExtra("clear", false);
                boolean edit = intent.getBooleanExtra("edit", false);
                if (user.equals(jid)) {
                    if (edit) {
                        String id = intent.getStringExtra("id");
                        String body = intent.getStringExtra("body");
                        if (id != null && body != null) updateMessage(id, body);
                    } else {
                        updateList();
                    }
                } else {
                    updateUsers();
                    updateChats();
                }
                if (clear) messageInput.setText("");
            }
        };

        receivedReceiver =  new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!isMuc) updateList();
            }
        };

        composeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateStatus();
                updateChats();
            }
        };

        presenceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateChats();
                updateUsers();
                if (isMuc) {
                    updateList();
                    updateStatus();
                } else {
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        String j = extras.getString("jid");
                        if (j != null && jid.equals(j)) {
                            updateStatus();
                            updateList();
                        }
                    }
                }
            }
        };

        finishReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };

        registerReceiver(finishReceiver, new IntentFilter(Constants.FINISH));
        registerReceiver(textReceiver, new IntentFilter(Constants.PASTE_TEXT));
        registerReceiver(msgReceiver, new IntentFilter(Constants.NEW_MESSAGE));
        registerReceiver(receivedReceiver, new IntentFilter(Constants.RECEIVED));
        registerReceiver(composeReceiver, new IntentFilter(Constants.UPDATE));
        registerReceiver(presenceReceiver, new IntentFilter(Constants.PRESENCE_CHANGED));
    }

    private void unregisterReceivers() {
        unregisterReceiver(textReceiver);
        unregisterReceiver(finishReceiver);
        unregisterReceiver(msgReceiver);
        unregisterReceiver(receivedReceiver);
        unregisterReceiver(composeReceiver);
        unregisterReceiver(presenceReceiver);
    }

    private void sendMessage() {
        String message = messageInput.getText().toString();
        if (isMuc) {
            try {
                muc.sendMessage(message);
            } catch (Exception ignored) {}
        }
        else {
            String to = jid;
            if (isPrivate) to = jid;
            else if (resource.length() > 0) to = jid + "/" + resource;
            service.sendMessage(account, to, message);
        }
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) { }
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (firstVisibleItem + visibleItemCount == totalItemCount) listView.setScroll(true);
        else listView.setScroll(false);
    }

    private void loadStory() {
        Cursor cursor;
        if (showStatuses) {
            cursor = getContentResolver().query(JTalkProvider.CONTENT_URI, null, "jid = '" + jid + "'", null, MessageDbHelper._ID);
        } else {
            cursor = getContentResolver().query(JTalkProvider.CONTENT_URI, null, "jid = '" + jid + "' AND type = 'message'", null, MessageDbHelper._ID);
        }
        if (cursor != null && cursor.getCount() > 0) {
            service.setMsgCounter(jid, cursor.getCount());
            cursor.close();
        }
        updateList();
    }

    private void setLastMessagesCounter() {
        int index = 5;
        int i = 0;
        Cursor cursor = getContentResolver().query(JTalkProvider.CONTENT_URI, null, "jid = '" + jid + "'", null, MessageDbHelper._ID);
        if (cursor != null) {
            if (cursor.getCount() > 5) {
                cursor.moveToLast();

                do {
                    String type = cursor.getString(cursor.getColumnIndex(MessageDbHelper.TYPE));
                    if (MessageItem.Type.valueOf(type) == MessageItem.Type.status) index++;
                    else i++;
                    if (i == 5) {
                        cursor.close();
                        service.setMsgCounter(jid, index);
                        return;
                    }
                } while (cursor.moveToPrevious());
                cursor.close();
            } else {
                cursor.close();
                service.setMsgCounter(jid, cursor.getCount());
            }
        }
    }

    private void clearChat() {
        service.setMsgCounter(jid, 0);
//        if (service.getMucMessagesHash(account).containsKey(jid)) {
//            service.getMucMessagesHash(account).remove(jid);
//        }
        updateList();
    }

    private void closeChat() {
        service.removeActiveChat(account, jid);
        finish();
    }
}
