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

package net.ustyugov.jtalk.activity.vcard;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.adapter.MainPageAdapter;
import net.ustyugov.jtalk.adapter.VCardAdapter;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.MUCUser;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.packet.Version;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;
import com.viewpagerindicator.TitlePageIndicator;

public class VCardActivity extends SherlockActivity {
	private JTalkService service;
	private String account;
	private String jid;
	
	private TextView nick, first, last, middle, bday, url, about, ctry, locality, street, emailHome, phoneHome, org, unit, role, emailWork, phoneWork;
	private ProgressBar aboutProgress, homeProgress, workProgress, avatarProgress, statusProgress;
	private ScrollView aboutScroll, homeScroll, workScroll, avatarScroll;
	private ListView list;
	private ImageView av;
	private VCard vCard;
	private VCardAdapter adapter;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		service = JTalkService.getInstance();
		account = getIntent().getStringExtra("account");
		jid = getIntent().getStringExtra("jid");
        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
		setContentView(R.layout.paged_activity);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    setTitle("vCard");
	    getSupportActionBar().setSubtitle(jid);

        if (service.getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid))) {
            Presence p = service.getConferencesHash(account).get(StringUtils.parseBareAddress(jid)).getOccupantPresence(jid);
            MUCUser mucUser = (MUCUser) p.getExtension("x", "http://jabber.org/protocol/muc#user");
            if (mucUser != null) {
                String j = mucUser.getItem().getJid();
                if (j != null && j.length() > 3) getSupportActionBar().setSubtitle(j);
            }
        }

		LinearLayout linear = (LinearLayout) findViewById(R.id.linear);
       	linear.setBackgroundColor(Colors.BACKGROUND);
		
       	LayoutInflater inflater = LayoutInflater.from(this);
		View aboutPage = inflater.inflate(R.layout.vcard_about, null);
		View homePage = inflater.inflate(R.layout.vcard_home, null);
		View workPage = inflater.inflate(R.layout.vcard_work, null);
		View avatarPage = inflater.inflate(R.layout.vcard_avatar, null);
		View statusPage = inflater.inflate(R.layout.list_activity, null);
		
		first = (TextView) aboutPage.findViewById(R.id.firstname);
		middle = (TextView) aboutPage.findViewById(R.id.middlename);
		last = (TextView) aboutPage.findViewById(R.id.lastname);
		nick = (TextView) aboutPage.findViewById(R.id.nickname);
		bday = (TextView) aboutPage.findViewById(R.id.bday);
		url = (TextView) aboutPage.findViewById(R.id.url);
		about = (TextView) aboutPage.findViewById(R.id.desc);
		
		ctry = (TextView) homePage.findViewById(R.id.ctry);
		locality = (TextView) homePage.findViewById(R.id.locality);
		street = (TextView) homePage.findViewById(R.id.street);
		emailHome = (TextView) homePage.findViewById(R.id.homemail);
		phoneHome = (TextView) homePage.findViewById(R.id.homephone);
		
		org = (TextView) workPage.findViewById(R.id.org);
		unit = (TextView) workPage.findViewById(R.id.unit);
		role = (TextView) workPage.findViewById(R.id.role);
		emailWork = (TextView) workPage.findViewById(R.id.workmail);
		phoneWork = (TextView) workPage.findViewById(R.id.workphone);
		
		av = (ImageView) avatarPage.findViewById(R.id.av);
		
		statusProgress = (ProgressBar) statusPage.findViewById(R.id.progress);
		aboutProgress = (ProgressBar) aboutPage.findViewById(R.id.progress);
		homeProgress = (ProgressBar) homePage.findViewById(R.id.progress);
		workProgress = (ProgressBar) workPage.findViewById(R.id.progress);
		avatarProgress = (ProgressBar) avatarPage.findViewById(R.id.progress);
		
		aboutScroll = (ScrollView) aboutPage.findViewById(R.id.scroll);
		homeScroll = (ScrollView) homePage.findViewById(R.id.scroll);
		workScroll = (ScrollView) workPage.findViewById(R.id.scroll);
		avatarScroll = (ScrollView) avatarPage.findViewById(R.id.scroll);
		
		list = (ListView) statusPage.findViewById(R.id.list);
		list.setDividerHeight(0);
        list.setCacheColorHint(0x00000000);
		
		aboutPage.setTag(getString(R.string.About));
		homePage.setTag(getString(R.string.Home));
		workPage.setTag(getString(R.string.Work));
		avatarPage.setTag(getString(R.string.Photo));
		statusPage.setTag(getString(R.string.Status));
		
		ArrayList<View> mPages = new ArrayList<View>();
	    mPages.add(aboutPage);
	    mPages.add(homePage);
	    mPages.add(workPage);
	    mPages.add(avatarPage);
	    mPages.add(statusPage);
	        
	    MainPageAdapter adapter = new MainPageAdapter(mPages);
	    ViewPager mPager = (ViewPager) findViewById(R.id.pager);
	    mPager.setAdapter(adapter);
	    mPager.setCurrentItem(0);
	        
	    TitlePageIndicator mTitleIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
	    mTitleIndicator.setTextColor(0xFF555555);
	    mTitleIndicator.setViewPager(mPager);
	    mTitleIndicator.setCurrentItem(0);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		new LoadTask().execute();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.view_vcard, menu);
        return super.onCreateOptionsMenu(menu);
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				break;
			case R.id.refresh:
				onResume();
				break;
		}
		return true;
	}

	private class LoadTask extends AsyncTask<Integer, Integer, Integer> {
		private Bitmap bitmap = null;
		private RosterEntry re = null;
		private Hashtable<String, String> strings = new Hashtable<String, String>();

		@Override
		protected Integer doInBackground(Integer... arg0) {
			vCard = new VCard();
    		try {
    			vCard.load(service.getConnection(account), jid);
    			byte[] buffer = vCard.getAvatar();
    			
				if (buffer != null) {
					DisplayMetrics metrics = new DisplayMetrics();
					getWindowManager().getDefaultDisplay().getMetrics(metrics);
					float scaleWidth = metrics.scaledDensity;
					float scaleHeight = metrics.scaledDensity;

					Matrix matrix = new Matrix();
					matrix.postScale(scaleWidth, scaleHeight);
					
					bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
					int width = bitmap.getWidth();
					if (width > metrics.widthPixels)  {
						double k = (double)width/(double)metrics.widthPixels;
						int h = (int) (bitmap.getWidth()/k);
						bitmap = Bitmap.createBitmap(bitmap, 0, 0, metrics.widthPixels, h, matrix, true);
						bitmap.setDensity(metrics.densityDpi);
					} else {
						bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
						bitmap.setDensity(metrics.densityDpi);
					}
					
					try {
						String fname = jid.replaceAll("/", "%");
						File f = new File(Constants.PATH);
						f.mkdirs();
						FileOutputStream fos = new FileOutputStream(Constants.PATH + "/" + fname);
						fos.write(buffer);
						fos.close();
					} catch (Throwable t) { }
				}
    		} catch (Exception e) { }
    		
    		// Load info
    		try {
    			re = service.getRoster(account).getEntry(jid);
    			// Load Version
    			if (jid.indexOf("/") == -1) {
    				Iterator<Presence> it =  service.getRoster(account).getPresences(jid);
    				int i = 0;
    				while (it.hasNext()) {
    					i++;
    					Presence p = it.next();
    					if (p.getType() != Presence.Type.unavailable) {
    						String vstr = "";
    						Version versionRequest = new Version();
    						versionRequest.setPacketID(System.currentTimeMillis()+i+"");
    						versionRequest.setType(IQ.Type.GET);
    						versionRequest.setTo(p.getFrom());

    						PacketCollector collector = service.getConnection(account).createPacketCollector(new PacketIDFilter(versionRequest.getPacketID()));
    						service.getConnection(account).sendPacket(versionRequest);
    						 
    						IQ result = (IQ)collector.nextResult(5000);
    						try {
    							if (result != null && result.getType() == IQ.Type.RESULT) {
    							    Version versionResult = (Version) result;
    							    String os = versionResult.getOs();
    							    String ver = versionResult.getVersion();
    							    String name = versionResult.getName();
    							    vstr = name + " " + ver + " (" + os + ")";
    							}
    						} catch (ClassCastException e) { }
    						
    						if (vstr.length() < 3) vstr += "???";
    						
    						String str = getString(R.string.Status) + ": " + p.getStatus() + "\n"
    								+ getString(R.string.Client) + ": " + vstr;
    						
    						strings.put(StringUtils.parseResource(p.getFrom()) + " (" + p.getPriority() + ")", str);
    					}
    				}
    			} else {
    				Version request = new Version();
    				request.setPacketID(System.currentTimeMillis()+"");
    				request.setType(IQ.Type.GET);
    				request.setTo(jid);

    				PacketCollector collector = service.getConnection(account).createPacketCollector(new PacketIDFilter(request.getPacketID()));
    				service.getConnection(account).sendPacket(request);
    					 
    				String vstr = "";
    				IQ result = (IQ)collector.nextResult(5000);
    				try {
    					if (result != null && result.getType() == IQ.Type.RESULT) {
    					    Version versionResult = (Version) result;
    					    String os = versionResult.getOs();
    					    String ver = versionResult.getVersion();
    					    String name = versionResult.getName();
    					    vstr = name + " " + ver + " (" + os + ")";
    					}
    				} catch (ClassCastException e) { }
    					
    				if (vstr.length() < 3) vstr += "???";
    				
    				String str = getString(R.string.Status) + ": " + service.getStatus(account, jid) + "\n"
    						+ getString(R.string.Client) + ": " + vstr;
    				strings.put(StringUtils.parseResource(jid), str);
    			}
    		} catch (Exception e) { }
			return 1;
		}
		
		@Override
		protected void onPreExecute() {
			adapter = new VCardAdapter(VCardActivity.this);
			list.setVisibility(View.GONE);
			
			aboutProgress.setVisibility(View.VISIBLE);
		    homeProgress.setVisibility(View.VISIBLE);
		    workProgress.setVisibility(View.VISIBLE);
		    avatarProgress.setVisibility(View.VISIBLE);
		    statusProgress.setVisibility(View.VISIBLE);
		    
		    aboutScroll.setVisibility(View.GONE);
		    homeScroll.setVisibility(View.GONE);
		    workScroll.setVisibility(View.GONE);
		    avatarScroll.setVisibility(View.GONE);
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			VCardActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
//	    			if (vCard.getField("FN") != null) {
//    				}
					
					if (vCard.getFirstName() != null) {
						first.setText(vCard.getFirstName());
	    			}
	    			
	    			if (vCard.getMiddleName() != null) {
	    				middle.setText(vCard.getMiddleName());
	    			}
	    			
	    			if (vCard.getLastName() != null) {
	    				last.setText(vCard.getLastName());
	    			}
	    			
	    			if (vCard.getNickName() != null) {
	    				nick.setText(vCard.getNickName());
	    			}
	    			
	    			if (vCard.getField("BDAY") != null) {
	    				bday.setText(vCard.getField("BDAY"));
	    			}
	    			
	    			if (vCard.getAddressFieldHome("CTRY") != null) {
	    				ctry.setText(vCard.getAddressFieldHome("CTRY"));
	    			}
	    			
	    			if (vCard.getAddressFieldHome("LOCALITY") != null) {
	    				locality.setText(vCard.getAddressFieldHome("LOCALITY"));
	    			}
	    			
	    			if (vCard.getAddressFieldHome("STREET") != null) {
	    				street.setText(vCard.getAddressFieldHome("STREET"));
	    			}
	    			
	    			if (vCard.getOrganization() != null) {
	    				org.setText(vCard.getOrganization());
	    			}
	    			
	    			if (vCard.getOrganizationUnit() != null) {
	    				unit.setText(vCard.getOrganizationUnit());
	    			}
	    			
	    			if (vCard.getField("ROLE") != null) {
	    				role.setText(vCard.getField("ROLE"));
	    			}
	    			
	    			if (vCard.getEmailHome() != null) {
	    				emailHome.setText(vCard.getEmailHome());
	    			}
	    			
	    			if (vCard.getEmailWork() != null) {
	    				emailWork.setText(vCard.getEmailWork());
	    			}
	    			
	    			if (vCard.getPhoneHome("VOICE") != null) {
	    				phoneHome.setText(vCard.getPhoneHome("VOICE"));
	    			}
	    			
	    			if (vCard.getPhoneWork("VOICE") != null) {
	    				phoneWork.setText(vCard.getPhoneWork("VOICE"));
	    			}
	    			
	    			if (vCard.getField("URL") != null) {
	    				url.setText(vCard.getField("URL"));
	    			}
	    			
	    			if (vCard.getField("DESC") != null) {
	    				about.setText(vCard.getField("DESC"));
	    			}
					
					if (bitmap != null) av.setImageBitmap(bitmap);
					
					if (re != null) {
	    				LinearLayout linear = (LinearLayout) ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.vcard_item, null);
						
						TextView resource = (TextView) linear.findViewById(R.id.resource);
						resource.setText(getString(R.string.Subscribtion) + ":");
						
						TextView value = (TextView) linear.findViewById(R.id.value);
						value.setText(re.getType().name());
						adapter.add(linear);
	    			}
					
					Enumeration<String> keys = strings.keys();
					while (keys.hasMoreElements()) {
						String key = keys.nextElement();
						
						LinearLayout linear = (LinearLayout) ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.vcard_item, null);
						TextView t1 = (TextView) linear.findViewById(R.id.resource);
						t1.setText(key);
						TextView t2 = (TextView) linear.findViewById(R.id.value);
						t2.setText(strings.get(key));
						adapter.add(linear);
					}
					
					list.refreshDrawableState();
				    list.setAdapter(adapter);
				    list.setVisibility(View.VISIBLE);
				    
				    aboutProgress.setVisibility(View.GONE);
				    homeProgress.setVisibility(View.GONE);
				    workProgress.setVisibility(View.GONE);
				    avatarProgress.setVisibility(View.GONE);
				    statusProgress.setVisibility(View.GONE);
				    
				    aboutScroll.setVisibility(View.VISIBLE);
				    homeScroll.setVisibility(View.VISIBLE);
				    workScroll.setVisibility(View.VISIBLE);
				    avatarScroll.setVisibility(View.VISIBLE);
				}
			});
		}
	}
}
