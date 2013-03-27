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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.adapter.MainPageAdapter;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.packet.VCard;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;
import com.viewpagerindicator.TitlePageIndicator;

public class SetVcardActivity extends SherlockActivity implements OnClickListener {
	private static final int IMAGE = 1;
	
	private String account;
	private EditText nick, first, last, middle, bday, url, about, ctry, locality, street, emailHome, phoneHome, org, unit, role, emailWork, phoneWork;
	private ImageView av;
	private Button load, clear;
	private JTalkService service = JTalkService.getInstance();
	private VCard vcard = new VCard();
	private byte[] bytes = null;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
		setContentView(R.layout.paged_activity);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setTitle("vCard");
		
       	LinearLayout linear = (LinearLayout) findViewById(R.id.linear);
       	linear.setBackgroundColor(Colors.BACKGROUND);
       	
       	account = getIntent().getStringExtra("account");
       	vcard = service.getVCard(account);
       	
		LayoutInflater inflater = LayoutInflater.from(this);
		View aboutPage = inflater.inflate(R.layout.set_vcard_about, null);
		View homePage = inflater.inflate(R.layout.set_vcard_home, null);
		View workPage = inflater.inflate(R.layout.set_vcard_work, null);
		View photoPage = inflater.inflate(R.layout.set_vcard_avatar, null);
		
		first = (EditText) aboutPage.findViewById(R.id.firstname);
		middle = (EditText) aboutPage.findViewById(R.id.middlename);
		last = (EditText) aboutPage.findViewById(R.id.lastname);
		nick = (EditText) aboutPage.findViewById(R.id.nickname);
		bday = (EditText) aboutPage.findViewById(R.id.bday);
		url = (EditText) aboutPage.findViewById(R.id.url);
		about = (EditText) aboutPage.findViewById(R.id.desc);
		
		ctry = (EditText) homePage.findViewById(R.id.ctry);
		locality = (EditText) homePage.findViewById(R.id.locality);
		street = (EditText) homePage.findViewById(R.id.street);
		emailHome = (EditText) homePage.findViewById(R.id.homemail);
		phoneHome = (EditText) homePage.findViewById(R.id.homephone);
		
		org = (EditText) workPage.findViewById(R.id.org);
		unit = (EditText) workPage.findViewById(R.id.unit);
		role = (EditText) workPage.findViewById(R.id.role);
		emailWork = (EditText) workPage.findViewById(R.id.workmail);
		phoneWork = (EditText) workPage.findViewById(R.id.workphone);
		
		av = (ImageView) photoPage.findViewById(R.id.av);
		load = (Button) photoPage.findViewById(R.id.load_button);
		load.setOnClickListener(this);
		clear = (Button) photoPage.findViewById(R.id.clear_button);
		clear.setOnClickListener(this);
		
		aboutPage.setTag(getString(R.string.About));
		homePage.setTag(getString(R.string.Home));
		workPage.setTag(getString(R.string.Work));
		photoPage.setTag(getString(R.string.Photo));
		
		ArrayList<View> mPages = new ArrayList<View>();
	    mPages.add(aboutPage);
	    mPages.add(homePage);
	    mPages.add(workPage);
	    mPages.add(photoPage);
	        
	    MainPageAdapter adapter = new MainPageAdapter(mPages);
	    ViewPager mPager = (ViewPager) findViewById(R.id.pager);
	    mPager.setAdapter(adapter);
	    mPager.setCurrentItem(0);
	        
	    TitlePageIndicator mTitleIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
	    mTitleIndicator.setTextColor(0xFF555555);
	    mTitleIndicator.setViewPager(mPager);
	    mTitleIndicator.setCurrentItem(0);
	    
	    update();
	}
	
	private void update() {
		new Thread() {
			   public void run() {
				   SetVcardActivity.this.runOnUiThread(new Runnable() {
					  public void run() {
						  if (vcard != null) {
							  first.setText(vcard.getFirstName());
							  middle.setText(vcard.getMiddleName());
							  last.setText(vcard.getLastName());
							  nick.setText(vcard.getNickName());
							  bday.setText(vcard.getField("BDAY"));
							  url.setText(vcard.getField("URL"));
							  about.setText(vcard.getField("DESC"));
							  ctry.setText(vcard.getAddressFieldHome("CTRY"));
							  locality.setText(vcard.getAddressFieldHome("LOCALITY"));
							  street.setText(vcard.getAddressFieldHome("STREET"));
							  emailHome.setText(vcard.getEmailHome());
							  phoneHome.setText(vcard.getPhoneHome("VOICE"));
							  org.setText(vcard.getOrganization());
							  unit.setText(vcard.getOrganizationUnit());
							  role.setText(vcard.getField("ROLE"));
							  emailWork.setText(vcard.getEmailWork());
							  phoneWork.setText(vcard.getPhoneWork("VOICE"));
							  
							  bytes = vcard.getAvatar();
							  if (bytes != null) {
									av.setImageBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length), 240, 240, true));
							  }
						  }
					  }
				   });
			   }
		   }.start();
	}
	
	public void publish() {
		Toast.makeText(SetVcardActivity.this, getString(R.string.PleaseWait), Toast.LENGTH_LONG);
		finish();
  		new Thread() {
  			public void run() {
  				try {
  					vcard.setFirstName(first.getText().toString());
			    		vcard.setMiddleName(middle.getText().toString());
			    		vcard.setLastName(last.getText().toString());
			    		vcard.setNickName(nick.getText().toString());
			    		vcard.setField("BDAY", bday.getText().toString());
			    		vcard.setField("URL", url.getText().toString());
			    		vcard.setField("DESC", about.getText().toString());
					   
			    		vcard.setOrganization(org.getText().toString());
			    		vcard.setOrganizationUnit(unit.getText().toString());
			    		vcard.setField("ROLE", role.getText().toString());
			    		vcard.setEmailWork(emailWork.getText().toString());
			    		vcard.setPhoneWork("VOICE", phoneWork.getText().toString());
					   
			    		vcard.setAddressFieldHome("CTRY", ctry.getText().toString());
						vcard.setAddressFieldHome("LOCALITY", locality.getText().toString());
						vcard.setAddressFieldHome("STREET", street.getText().toString());
						vcard.setEmailHome(emailHome.getText().toString());
						vcard.setPhoneHome("VOICE", phoneHome.getText().toString());
					   
						vcard.setAvatar(bytes);
					
						vcard.setType(IQ.Type.SET);
						vcard.save(service.getConnection(account));
						service.setVCard(account, vcard);
						sendBroadcast(new Intent(net.ustyugov.jtalk.Constants.ERROR).putExtra("error", "vCard updated!"));
  				} catch (XMPPException e) {
  					sendBroadcast(new Intent(net.ustyugov.jtalk.Constants.ERROR).putExtra("error", e.getLocalizedMessage()));
  				}
  			}
  		}.start();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.vcard, menu);
        return true;
    }
   
   @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case android.R.id.home:
    			finish();
    			break;
  	    	case R.id.publish:
  	    		publish();
  	    		break;
  	    	default:
  	    		return false;
    	}
    	return true;
    }

   @Override
   public void onClick(View v) {
	   if (v.equals(clear)) {
		   bytes = null;
		   av.setImageBitmap(null);
	   } else if (v.equals(load)) {
		   Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
           intent.setType("image/*");
           startActivityForResult(Intent.createChooser(intent, null), IMAGE);
	   }
   }
   
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			if (requestCode == IMAGE && data != null) {
				Uri uri = Uri.parse(data.getDataString());
				if (uri != null) {
	                try {
	                	FileInputStream fileInput = getContentResolver().openAssetFileDescriptor(uri, "r").createInputStream();
						bytes = new byte[fileInput.available()];
						fileInput.read(bytes);
						Bitmap bm = Bitmap.createScaledBitmap(BitmapFactory.decodeFileDescriptor(fileInput.getFD()), 240, 240, true);
						av.setImageBitmap(bm);
						fileInput.close();
					} catch (FileNotFoundException e) {
					} catch (IOException e) { }
	            }
			}
		}
	}
}
