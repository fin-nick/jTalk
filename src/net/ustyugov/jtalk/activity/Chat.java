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
import java.util.List;

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.MessageItem;
import net.ustyugov.jtalk.Notify;
import net.ustyugov.jtalk.Smiles;
import net.ustyugov.jtalk.activity.vcard.VCardActivity;
import net.ustyugov.jtalk.adapter.ChatAdapter;
import net.ustyugov.jtalk.adapter.ChatsSpinnerAdapter;
import net.ustyugov.jtalk.adapter.MucChatAdapter;
import net.ustyugov.jtalk.adapter.MucUserAdapter;
import net.ustyugov.jtalk.adapter.OpenChatsAdapter;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.db.MessageDbHelper;
import net.ustyugov.jtalk.dialog.ChangeChatDialog;
import net.ustyugov.jtalk.dialog.ContactMenuDialogs;
import net.ustyugov.jtalk.dialog.MessageMenuDialog;
import net.ustyugov.jtalk.dialog.MucDialogs;
import net.ustyugov.jtalk.dialog.SendToResourceDialog;
import net.ustyugov.jtalk.dialog.UsersDialog;
import net.ustyugov.jtalk.service.JTalkService;
import net.ustyugov.jtalk.view.MyListView;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.jtalk2.R;

public class Chat extends SherlockActivity implements View.OnClickListener, OnLongClickListener, OnScrollListener {
	public static final int REQUEST_TEMPLATES = 1;
	
	private boolean isMuc = false;
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
	private List<MessageItem> msgList;
	private EditText messageInput;
	private Button sendButton;
	
	private String  jid;
	private boolean compose = false;
	
	private BroadcastReceiver textReceiver;
	private BroadcastReceiver finishReceiver;
	private BroadcastReceiver msgReceiver;
	private BroadcastReceiver receivedReceiver;
	private BroadcastReceiver presenceReceiver;
	private BroadcastReceiver composeReceiver;
	
	private JTalkService service;
	private Smiles smiles;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		service = JTalkService.getInstance();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		setTheme(prefs.getBoolean("DarkColors", false) ? R.style.AppThemeDark : R.style.AppThemeLight);
		
		chatsSpinnerAdapter = new ChatsSpinnerAdapter(this);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(chatsSpinnerAdapter, new OnNavigationListener() {
			  @Override
			  public boolean onNavigationItemSelected(int position, long itemId) {
			    String j = (String) chatsSpinnerAdapter.getItem(position);
			    if (!j.equals(jid)) {
			    	Intent intent = new Intent();
			    	intent.putExtra("jid", j);
			    	setIntent(intent);
			    	onPause();
			    	onResume();
				}
			    return true;
			  }
		});
		
		setContentView(R.layout.chat);
		
       	LinearLayout linear = (LinearLayout) findViewById(R.id.chat_linear);
       	linear.setBackgroundColor(prefs.getBoolean("DarkColors", false) ? 0xFF000000 : 0xFFFFFFFF);
		
		smiles = new Smiles(this);
		
		sidebar = (LinearLayout) findViewById(R.id.sidebar);
		
		chatsAdapter = new OpenChatsAdapter(this, false);
		chatsList = (ListView) findViewById(R.id.open_chat_list);
		chatsList.setCacheColorHint(0x00000000);
		chatsList.setDividerHeight(0);
        chatsList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {
				if (position > 0) {
					String j = (String) parent.getItemAtPosition(position);
					if (!j.equals(jid)) {
				    	Intent intent = new Intent();
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
					String j = (String) parent.getItemAtPosition(position);
					if (service.getConferencesHash().containsKey(j)) {
						MucDialogs.roomMenu(Chat.this, j);
					} else {
						RosterEntry entry = service.getRoster().getEntry(j);
						if (entry != null) ContactMenuDialogs.ContactMenu(Chat.this, entry);
						else {
							if (service.getConferencesHash().containsKey(StringUtils.parseBareAddress(j))) {
								String group = StringUtils.parseBareAddress(j);
								String nick = StringUtils.parseResource(j);
								MucDialogs.userMenu(Chat.this, group, nick);
							}
						}
					}
					return true;
				} else return false;
			}
        	
        });
		
        listAdapter = new ChatAdapter(this, smiles);
		listView = (MyListView) findViewById(R.id.chat_list);
		listView.setCacheColorHint(0x00000000);
		listView.setOnScrollListener(this);
		listView.setDividerHeight(0);
		listView.setAdapter(listAdapter);
        
		nickList = (ListView) findViewById(R.id.muc_user_list);
		nickList.setCacheColorHint(0x00000000);
		nickList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {
				if (position > 0) {
					String separator = prefs.getString("nickSeparator", ", ");
					String item = (String) parent.getAdapter().getItem(position);
					String text = messageInput.getText().toString();
					if (text.length() > 0) {
						text += " " + item + separator;
					} else {
						text = item + separator;
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
				if (position > 0) {
					String nick = (String) parent.getItemAtPosition(position);
					MucDialogs.userMenu(Chat.this, jid, nick);
					return true;
				} else return false;
			}
		});
		
		messageInput = (EditText)findViewById(R.id.messageInput);
		if (prefs.getBoolean("SendOnEnter", false)) {
			messageInput.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_ENTER) {
						if (event.isShiftPressed()) messageInput.append("\n");
						else onClick(sendButton);
						return true;
					} else return false;
				}
			});
		}
		
		sendButton  = (Button)findViewById(R.id.SendButton);
		sendButton.setEnabled(false);
		sendButton.setOnClickListener(this);
		sendButton.setOnLongClickListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		compose = false;
		jid = getIntent().getStringExtra("jid");
		
		service.setCurrentJid(jid);
		service.removeHighlight(jid);
		
		listView.setOnItemLongClickListener(new MessageMenuDialog(this, jid));
		
		usersAdapter = new MucUserAdapter(this, jid);
		nickList.setAdapter(usersAdapter);
		chatsList.setAdapter(chatsAdapter);
		
		if (service.getConferencesHash().containsKey(jid)) {
			isMuc = true;
			muc = service.getConferencesHash().get(jid);
			
			if (service.getMucMessagesHash().containsKey(jid)) {
				msgList = service.getMucMessagesHash().get(jid);
			} else msgList = new ArrayList<MessageItem>();
			
			listMucAdapter = new MucChatAdapter(this, jid, muc.getNickname(), smiles);
			listMucAdapter.update(msgList);
			listView.setAdapter(listMucAdapter);
			listView.setScroll(true);
		} else {
			isMuc = false;
			muc = null;
			
			if (service.getMessagesHash().containsKey(jid)) {
				msgList = service.getMessagesHash().get(jid);
			} else {
				msgList = new ArrayList<MessageItem>();
				loadStory(false);
			}
			String j = listAdapter.getJid();
			listAdapter.search("");
			listAdapter.update(jid, msgList);
			if (listView.getAdapter() instanceof MucChatAdapter) {
				listView.setAdapter(listAdapter);
				listView.setScroll(true);
			}
			else {
				if (j != null && j.equals(jid)) listView.setScroll(false); else listView.setScroll(true);
			}
		}
		
		messageInput.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) { 
				int length = s.length();
				if (length > 0) sendButton.setEnabled(true); else sendButton.setEnabled(false);
				
				if (!isMuc) {
					if (length > 0 && !compose) {
						compose = true;
						service.setChatState(jid, ChatState.composing);
					} else if (length == 0 && compose) {
						compose = false;
						service.setChatState(jid, ChatState.active);
					}
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			public void onTextChanged(CharSequence s, int start, int before, int count) { }
		});
		
		if (messageInput.getText().length() < 1) {
			String text = service.getText(jid);
			messageInput.setText(text);
			messageInput.setSelection(text.length());
		}
		
		int unreadMessages = service.getMessagesCount(jid);
		if (unreadMessages > 1) {
			listView.setScroll(false);
			listView.setSelection(listView.getCount() - (unreadMessages + 1));
		} else {
			if (listView.isScroll()) listView.setSelection(listView.getCount());
		}
		service.removeMessagesCount(jid);
		
    	while (service.getMessagesList().contains(jid)) service.getMessagesList().remove(jid);
    	if (service.isAuthenticated()) {
			Notify.updateNotify();
		} else {
			Notify.offlineNotify(service.getState());
		}

    	updateChats();
    	updateUsers();
    	updateStatus();

		registerReceivers();
		service.resetTimer();
		
		if (!isMuc) service.setChatState(jid, ChatState.active);
		createOptionMenu();
		
		int position = chatsSpinnerAdapter.getPosition(jid);
		getSupportActionBar().setSelectedNavigationItem(position);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		unregisterReceivers();
		compose = false;
		if (!isMuc)  service.setChatState(jid, ChatState.active);
		service.setCurrentJid("me");
		service.setText(jid, messageInput.getText().toString());
	}
	
	@Override 
	public void onDestroy() {
		super.onDestroy();
		jid = null;
	}
	
//	@Override
//	public boolean onKeyUp(int key, KeyEvent event) {
//		if (event.getKeyCode() == KeyEvent.KEYCODE_SEARCH) {
//			MenuItem item = menu.findItem(R.id.search);
//			item.expandActionView();
//			return true;
//		} else return false;
//	}
	
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
	        }
	        
	        if (Build.VERSION.SDK_INT >= 14) {
	        	OnActionExpandListener listener = new OnActionExpandListener() {
	                @Override
	                public boolean onMenuItemActionCollapse(MenuItem item) {
	                	if (!isMuc) {
	                		listAdapter.search("");
	                	} else {
	                		
	                	}
						createOptionMenu();
	                    return true;
	                }

	                @Override
	                public boolean onMenuItemActionExpand(MenuItem item) {
	                	inflater.inflate(R.menu.find_menu, menu);
	                	menu.removeItem(R.id.sidebar);
	                	menu.removeItem(R.id.smile);
	                    return true;
	                }
	            };
	        	
	        	MenuItem item = menu.findItem(R.id.search);
	        	item.setOnActionExpandListener(listener);
	        	
	        	final SearchView searchView = (SearchView) item.getActionView();
	        	searchView.setQueryHint(getString(android.R.string.search_go));
	        	searchView.setSubmitButtonEnabled(false);
	        	searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
					@Override
					public boolean onQueryTextChange(String newText) {
						if (!isMuc) {
							listAdapter.search(newText);
						} else {
							listMucAdapter.search(newText);
						}
						return true;
					}
					@Override
					public boolean onQueryTextSubmit(String query) {
						return true;
					}
	        	});
	        } else {
	        	menu.removeItem(R.id.search); 
	        }
	        
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
				finish();
				break;
			case R.id.smile:
				smiles.showDialog();
				break;
			case R.id.nick:
				new UsersDialog(this, jid).show();
				break;
			case R.id.subj:
				MucDialogs.subjectDialog(this, jid);
				break;
			case R.id.templates:
				startActivityForResult(new Intent(this, TemplatesActivity.class), REQUEST_TEMPLATES);
				break;
			case R.id.sidebar:
				boolean showSidebar = prefs.getBoolean("EnabledSidebar", true);
				if (showSidebar) service.setPreference("EnabledSidebar", false);
				else service.setPreference("EnabledSidebar", true);
				updateChats();
				break;
	    	case R.id.info:
	    		Intent infoIntent = new Intent(this, VCardActivity.class);
        		infoIntent.putExtra("jid", jid);
        		startActivity(infoIntent);
  	    		break;
	    	case R.id.invite:
				MucDialogs.inviteDialog(this, jid);
				break;
  	    	case R.id.history:
  	    		loadStory(true);
  	    		updateList();
  	    		break;
  	    	case R.id.delete_history:
  	    		getContentResolver().delete(JTalkProvider.CONTENT_URI, "jid = '" + jid + "'", null);
  	    		msgList.clear();
  	    		if (service.getMessagesHash().containsKey(jid)) {
  	    			service.getMessagesHash().remove(jid);
  	  	    		updateList();
  	    		}
  	    		break;
  	    	case R.id.chats:
  	    		ChangeChatDialog.show(this);
  	    		break;
  	    	case R.id.clear:
  	    		msgList.clear();
  	    		if (service.getMessagesHash().containsKey(jid)) {
  	    			service.getMessagesHash().remove(jid);
  	    		}
  	    		if (service.getMucMessagesHash().containsKey(jid)) {
  	    			service.getMucMessagesHash().remove(jid);
  	    		}
  	    		updateList();
  	    		break;
  	    	case R.id.close:
  	    		if (!isMuc) service.setChatState(jid, ChatState.gone);
  	    		msgList.clear();
  	    		if (service.getMessagesHash().containsKey(jid)) {
  	    			service.getMessagesHash().remove(jid);
  	    		}
  	    		finish();
  	    		break;
  	    	case R.id.leave:
  	    		finish();
  	    		service.leaveRoom(jid);
  	    		break;
  	    	case R.id.prev:
  	    		int prevPosition = -1;
  	    		if (!isMuc) {
  	    			prevPosition = listAdapter.prevSearch();
  	    		} else {
  	    			prevPosition = listMucAdapter.prevSearch();
  	    		}
  	    		if (prevPosition >= 0) listView.setSelection(prevPosition);
  	    		break;
  	    	case R.id.next:
  	    		int nextPosition = -1;
  	    		if (!isMuc) {
  	    			nextPosition = listAdapter.nextSearch();
  	    		} else {
  	    			nextPosition = listMucAdapter.nextSearch();
  	    		}
  	    		if (nextPosition >= 0) listView.setSelection(nextPosition);
  	    		break;
  	    	default:
  	    		return false;
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
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(messageInput.getWindowToken(), 0, null);
			}
		}
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
		
		if (isMuc) {
			if (service.getMucMessagesHash().containsKey(jid)) {
				msgList = service.getMucMessagesHash().get(jid);
			} else msgList = new ArrayList<MessageItem>();
			
			listMucAdapter.update(msgList);
			listMucAdapter.notifyDataSetChanged();
		} else {
			if (service.getMessagesHash().containsKey(jid)) {
				msgList = service.getMessagesHash().get(jid);
			}
			listAdapter.update(jid, msgList);
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
		else ab.setLogo(service.getIconPicker().getDrawableByPresence(service.getPresence(jid)));
	}
	
	private void registerReceivers() {
		textReceiver = new BroadcastReceiver() {
    		@Override
    		public void onReceive(Context c, Intent i) {
    			String text = i.getExtras().getString("text");
    			if (i.getBooleanExtra("jubo", false)) {
    				Intent intent = new Intent();
    				intent.putExtra("jid", "juick@juick.com");
    				setIntent(intent);
    				onPause();
    				onResume();
    			}
    			messageInput.setText(messageInput.getText() + text);
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
        		listAdapter.notifyDataSetChanged();
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
    	registerReceiver(msgReceiver, new IntentFilter(Constants.NEW_MUC_MESSAGE));
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
			} catch (Exception e) {}
		}
		else {
			service.sendMessage(jid, message);
		}
		updateList();
	}
	
	public boolean onLongClick(View v) {
		String message = messageInput.getText().toString();
		new SendToResourceDialog(this, jid, message).show();
		return true;
	}
	
	public void onScrollStateChanged(AbsListView view, int scrollState) { }
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (firstVisibleItem + visibleItemCount == totalItemCount) listView.setScroll(true);
		else listView.setScroll(false);
	}
	
	private void loadStory(boolean all) {
		msgList.clear();
		Cursor cursor;
		if (all) {
			cursor = getContentResolver().query(JTalkProvider.CONTENT_URI, null, "jid = '" + jid + "'", null, MessageDbHelper._ID);
			if (cursor != null && cursor.getCount() > 0) cursor.moveToFirst();
		}
		else {
			cursor = getContentResolver().query(JTalkProvider.CONTENT_URI, null, "jid = '" + jid + "' AND type = 'message'", null, MessageDbHelper._ID);
			if (cursor != null) {
				if (cursor.getCount() > 5) {
					cursor.moveToLast();
					cursor.move(-5);
				} else cursor.moveToFirst();
			}
		}
		if (cursor != null && cursor.getCount() > 0) {
			do {
				String id = cursor.getString(cursor.getColumnIndex(MessageDbHelper.ID));
				String nick = cursor.getString(cursor.getColumnIndex(MessageDbHelper.NICK));
				String type = cursor.getString(cursor.getColumnIndex(MessageDbHelper.TYPE));
				String stamp = cursor.getString(cursor.getColumnIndex(MessageDbHelper.STAMP));
				String body = cursor.getString(cursor.getColumnIndex(MessageDbHelper.BODY));
				boolean received = Boolean.valueOf(cursor.getString(cursor.getColumnIndex(MessageDbHelper.RECEIVED)));
				
				MessageItem item = new MessageItem();
				item.setId(id);
				item.setName(nick);
				item.setType(MessageItem.Type.valueOf(type));
				item.setTime(stamp);
				item.setBody(body);
				item.setReceived(received);
				
				msgList.add(item);
			} while (cursor.moveToNext());
		}
		service.getMessagesHash().put(jid, msgList);
	}
}
