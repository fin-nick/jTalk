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

import java.util.Iterator;

import android.view.ViewGroup;
import com.actionbarsherlock.view.MenuItem;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.adapter.OptionsSpinnerAdapter;
import net.ustyugov.jtalk.service.JTalkService;
import net.ustyugov.jtalk.view.MyEditText;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.MediaElement;
import org.jivesoftware.smackx.commands.AdHocCommand;
import org.jivesoftware.smackx.commands.AdHocCommandManager;
import org.jivesoftware.smackx.commands.RemoteCommand;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.BobExtension;
import org.jivesoftware.smackx.packet.CaptchaExtension;
import org.jivesoftware.smackx.packet.DataForm;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.jtalk2.R;

public class DataFormActivity extends SherlockActivity implements OnClickListener {
	private DataForm form;
	private JTalkService service = JTalkService.getInstance();
	private LinearLayout layout;
	private String jid;
	private String account;
	private boolean reg = false;
	private boolean cap = false;
	private boolean com = false;
	private boolean muc = false;
	private MultiUserChat conference;
	private ProgressBar progress;
	private ScrollView scroll;
	private LinearLayout buttonBar;
	private Button okButton;
	private Button cancelButton;
	private BobExtension bob = null;
	private RemoteCommand rc;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
		setContentView(R.layout.data_form);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
       	LinearLayout linear = (LinearLayout) findViewById(R.id.data_form);
       	linear.setBackgroundColor(Colors.BACKGROUND);
		
       	account = getIntent().getStringExtra("account");
		jid = getIntent().getStringExtra("jid");
		reg = getIntent().getBooleanExtra("reg", false);
		cap = getIntent().getBooleanExtra("cap", false);
		com = getIntent().getBooleanExtra("com", false);
		muc = getIntent().getBooleanExtra("muc", false);

		layout = (LinearLayout) findViewById(R.id.data_form_linear);
		
		okButton = (Button) findViewById(R.id.data_form_ok);
		okButton.setOnClickListener(this);
		
		cancelButton = (Button) findViewById(R.id.data_form_cancel);
		cancelButton.setOnClickListener(this);
		
		progress = (ProgressBar) findViewById(R.id.progress);
		scroll = (ScrollView) findViewById(R.id.data_form_scroll);
		buttonBar = (LinearLayout) findViewById(R.id.data_form_button_bar);
		
		if (reg) {
            new RegistrationTask().execute(null, null, null);
		} else if (cap) {
            new CaptchaTask().execute(null, null, null);
		} else if (com) {
            String node = getIntent().getStringExtra("node");
            new CommandTask().execute(node, null, null);
		} else if (muc) {
            new MucTask().execute(null, null, null);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		service.resetTimer();
	}

    @Override
    public void onPause() {
        super.onPause();
        if (rc != null && rc.getStatus() == RemoteCommand.Status.executing) {
            try {
                rc.cancel();
            } catch (XMPPException ignored) { }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @Override
	public void onClick(View view) {
		if (view.equals(cancelButton)) {
			finish();
		} else if (view.equals(okButton)) {
			new Thread() {
				public void run() {
					DataFormActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							String id = System.currentTimeMillis()+"";
							
							DataForm f = new DataForm("submit");
							for (int i = 0; i < layout.getChildCount(); i++) {
								View v = layout.getChildAt(i);
								
								if(v.getClass() == MyEditText.class) {
									MyEditText et = (MyEditText) v;
									FormField ff = new FormField(et.getVar());
									ff.setType(et.getType());
									ff.addValue(et.getText().toString());
									
									if ((i + 1) < layout.getChildCount()) {
										View image = layout.getChildAt(i + 1);
										if (image.getClass() == ImageView.class) {
											MediaElement me = new MediaElement("image/png", (String)image.getTag());
											ff.addMedia(me);
										}
									}
									f.addField(ff);
								} else if (v.getClass() == Spinner.class) {
									Spinner spinner = (Spinner) v;
									FormField ff = new FormField((String) spinner.getTag());
									ff.setType("list-single");
									FormField.Option val = (FormField.Option) spinner.getSelectedItem();
									ff.addValue(val.getValue());
									f.addField(ff);
								} else if (v.getClass() == CheckBox.class) {
									CheckBox cb = (CheckBox) v;
									FormField ff = new FormField((String) cb.getTag());
									ff.setType("boolean");
									ff.addValue(cb.isChecked() ? "1" : "0");
									f.addField(ff);
								} else if (v.getClass() == LinearLayout.class) {
									LinearLayout linear = (LinearLayout) v;
									FormField ff = new FormField((String) linear.getTag());
									ff.setType("list-multi");
									for (int j = 0; j < linear.getChildCount(); j++) {
										CheckBox cb = (CheckBox) linear.getChildAt(j);
										if (cb.isChecked()) ff.addValue((String) cb.getTag());
									}
									f.addField(ff);
								}
							}
							
							if (reg) {
								Registration reg = new Registration();
								reg.setPacketID(id);
								reg.setType(IQ.Type.SET);
								reg.setTo(jid);
								reg.addExtension(f);

                                XMPPConnection connection = service.getConnection(account);
								PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(id));
								connection.sendPacket(reg);
								
								IQ result = (IQ) collector.nextResult(5000);
								if (result != null) {
									XMPPError error = result.getError();
									if (error != null) {
                                        int code = error.getCode();
                                        String condition = error.getCondition();
                                        String message = error.getMessage();
                                        Toast.makeText(DataFormActivity.this, "["+code+"] " + condition + ": " + message, Toast.LENGTH_LONG).show();
                                    }
									else finish();
								}
							} else if (cap) {
								CaptchaExtension captcha = new CaptchaExtension(f);
								
								IQ iq = new IQ() {
									public String getChildElementXML() {
										return getExtensionsXML();
				                    }
								};
								iq.setPacketID(id);
								iq.setType(IQ.Type.SET);
								iq.setTo(jid);
								iq.addExtension(captcha);

                                XMPPConnection connection = service.getConnection(account);
								PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(id));
								connection.sendPacket(iq);

								IQ result = (IQ) collector.nextResult(5000);
								if (result != null) {
									XMPPError error = result.getError();
									if (error != null ) Toast.makeText(DataFormActivity.this, error.getMessage(), Toast.LENGTH_LONG).show(); 
									else finish();
								}
							} else if (com) {
								IQ iq = new IQ() {
									public String getChildElementXML() {
										return getExtensionsXML();
				                    }
								};
								iq.addExtension(f);
								
								try {
									rc.execute(Form.getFormFrom(iq));
									AdHocCommand.Status status = rc.getStatus();
									if (status == AdHocCommand.Status.executing) {
										if (rc.getForm() != null) {
											form = rc.getForm().getDataFormToSend();
											createForm();
										}
									} else if (status == AdHocCommand.Status.completed || status == AdHocCommand.Status.canceled) finish();
								} catch (XMPPException e) {	finish(); }
							} else if (muc) {
								IQ iq = new IQ() {
									public String getChildElementXML() {
										return getExtensionsXML();
				                    }
								};
								iq.addExtension(f);
								
								try {
									conference.sendConfigurationForm(Form.getFormFrom(iq));
									finish();
								} catch (XMPPException e) { finish(); }
							}
 						}
					});
				}
			}.start();
		}
	}
	
	private EditText createEditText(String type, String var, String text, String label) {
		MyEditText et = new MyEditText(this);
        et.setHint(label);
        et.setVar(var);
        et.setType(type);
        if (text != null && text.length() > 0) et.setText(text);
       
        if (type.equals("text-single")) {
        	et.setSingleLine(true);
        	et.setMinLines(1);
        } else if (type.equals("text-private")) {
        	et.setSingleLine(true);
        	et.setMinLines(1);
        } else if (type.equals("text-multi")) {
        	et.setSingleLine(false);
        	et.setMinLines(3);
        } else if (type.equals("hidden")) {
        	et.setVisibility(View.GONE);
        }
        return et;
	}
	
	private TextView createTextView(String text) {
		TextView tv = new TextView(this);
		tv.setTextColor(Colors.isLight ? 0xFF000000 : 0xFFFFFFFF);
		tv.setText(text);
        return tv;
	}
	
	private ImageView createImageView(String cid, byte[] data) {
		Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);
		ImageView image = new ImageView(this);
        image.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
		image.setTag("cid:" + cid);
		if (b != null) {
			image.getLayoutParams().height = b.getHeight();
			image.getLayoutParams().width = b.getWidth();
			image.setImageBitmap(b);
		}
		return image;
	}
	
	private CheckBox createCheckBox(String label, String var, String value) {
		CheckBox cb = new CheckBox(this);
		cb.setTextColor(Colors.isLight ? 0xFF000000 : 0xFFFFFFFF);
        cb.setText(label);
        cb.setTag(var);
        if (value.equals("0")) cb.setChecked(false); else cb.setChecked(true);
        return cb;
	}
	
	private Spinner createListSingle(String var, Iterator<FormField.Option> options, String value) {
		int position = 0;
		
		OptionsSpinnerAdapter adapter = new OptionsSpinnerAdapter(this, options);
		for (int i = 0; i < adapter.getCount(); i++) {
			if (adapter.getItem(i).getValue().equals(value)) position = i;
		}
		
		Spinner spinner = new Spinner(this);
		spinner.setTag(var);
		spinner.setAdapter(adapter);
		spinner.setSelection(position);
		return spinner;
	}
	
	private LinearLayout createListMulti(String var, Iterator<FormField.Option> options) {
		LinearLayout linear = new LinearLayout(this);
		linear.setTag(var);
		linear.setOrientation(LinearLayout.VERTICAL);
		while(options.hasNext()) {
			FormField.Option option = options.next();
			linear.addView(createCheckBox(option.getLabel(), option.getValue(), "0"));
		}
		return linear;
	}
	
	private void createForm() {
		new Thread() {
			public void run() {
				DataFormActivity.this.runOnUiThread(new Runnable() {
					public void run() {
						if (form != null) {
							setTitle(form.getTitle());
							
							Iterator<String> instructions = form.getInstructions();
							while(instructions.hasNext()) {
								layout.addView(createTextView(instructions.next()));
							}
							
							Iterator<FormField> it = form.getFields();
							while(it.hasNext()) {
								FormField ff = it.next();
								String type = ff.getType();
								if (type == null) type = "text-single";
								String label = ff.getLabel();
								String value = ff.getValue();
								String var = ff.getVariable();

                                if (type.equals("fixed")) {
                                    layout.addView(createTextView(value));
                                }
                                else if(type.equals("hidden")) {
                                    layout.addView(createEditText(type, var, value, label));
                                }
                                else if (type.contains("text")) {
                                    layout.addView(createEditText(type, var, value, label));
                                }
                                else if (type.equals("boolean")) {
                                    layout.addView(createCheckBox(label, var, value));
                                }
                                else if (type.equals("list-single")) {
                                    layout.addView(createTextView(label));
                                    layout.addView(createListSingle(var, ff.getOptions(), value));
                                }
                                else if (type.equals("list-multi")) {
                                    layout.addView(createTextView(label));
                                    layout.addView(createListMulti(var, ff.getOptions()));
                                }

								Iterator<MediaElement> mi = ff.getMedia();
								while(mi.hasNext()) {
									MediaElement mediaElement = mi.next();
									String mediaType = mediaElement.getType();
									String mediaId = mediaElement.getId();
									if (mediaType.equals("image/png")) {
										
										if (bob != null && mediaId.contains(bob.getCid())) {
											byte[] data = bob.getData();
											if (data != null && data.length > 0) layout.addView(createImageView(bob.getCid(), data));
										}
									}
								}
							}
						}
					}
				});
			}
		}.start();
	}
	
	private class RegistrationTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			String id = System.currentTimeMillis()+"";
			Registration reg = new Registration();
			reg.setPacketID(id);
			reg.setType(IQ.Type.GET);
			reg.setTo(jid);

            XMPPConnection connection = service.getConnection(account);
			PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(id));
			connection.sendPacket(reg);
			
			IQ result = (IQ) collector.nextResult(5000);
			try {
				if (result != null && result.getType() == IQ.Type.RESULT) {
					form = (DataForm) result.getExtension("jabber:x:data");
					bob = (BobExtension) result.getExtension("data","urn:xmpp:bob");
				}
			} catch (ClassCastException ignored) { }
			createForm();
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v) {
			super.onPostExecute(v);
		    scroll.setVisibility(View.VISIBLE);
		    buttonBar.setVisibility(View.VISIBLE);
		    progress.setVisibility(View.GONE);
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			scroll.setVisibility(View.GONE);
			buttonBar.setVisibility(View.GONE);
			progress.setVisibility(View.VISIBLE);
		}
	}
	
	private class CaptchaTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			byte[] d = getIntent().getByteArrayExtra("bob");
			String cid = getIntent().getStringExtra("cid");
			String id = getIntent().getStringExtra("id");
			form = service.getDataForm(id);
			bob = new BobExtension(cid, null);
			bob.setData(d);
			createForm();
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v) {
			super.onPostExecute(v);
		    scroll.setVisibility(View.VISIBLE);
		    buttonBar.setVisibility(View.VISIBLE);
		    progress.setVisibility(View.GONE);
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			scroll.setVisibility(View.GONE);
			buttonBar.setVisibility(View.GONE);
			progress.setVisibility(View.VISIBLE);
		}
	}
	
	private class CommandTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			String node = params[0];
			AdHocCommandManager ahcm = AdHocCommandManager.getAddHocCommandsManager(service.getConnection(account));
			if (ahcm != null && node != null) {
				try {
					rc = ahcm.getRemoteCommand(jid, node);
					rc.execute();
					AdHocCommand.Status status = rc.getStatus();
					if (status == AdHocCommand.Status.executing) {
						if (rc.getForm() != null) {
							form = rc.getForm().getDataFormToSend();
							createForm();
						}
					} else if (status == AdHocCommand.Status.completed || status == AdHocCommand.Status.canceled) {
                        if (rc.getForm() != null) {
                            form = rc.getForm().getDataFormToSend();
                            createForm();
//                        finish();
                        }
                    }
				} catch (XMPPException e) { finish(); }
			} else finish();
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v) {
			super.onPostExecute(v);
		    scroll.setVisibility(View.VISIBLE);
		    buttonBar.setVisibility(View.VISIBLE);
		    progress.setVisibility(View.GONE);
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			scroll.setVisibility(View.GONE);
			buttonBar.setVisibility(View.GONE);
			progress.setVisibility(View.VISIBLE);
		}
	}
	
	private class MucTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			String group = getIntent().getStringExtra("group");
			conference = service.getConferencesHash(account).get(group);
			
			if (conference != null) {
				try {
					Form f = conference.getConfigurationForm();
					if (f != null) {
						form = f.getDataFormToSend();
						createForm();
					}
				} catch (XMPPException e) { finish(); }
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v) {
			super.onPostExecute(v);
		    scroll.setVisibility(View.VISIBLE);
		    buttonBar.setVisibility(View.VISIBLE);
		    progress.setVisibility(View.GONE);
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			scroll.setVisibility(View.GONE);
			buttonBar.setVisibility(View.GONE);
			progress.setVisibility(View.VISIBLE);
		}
	}
}
