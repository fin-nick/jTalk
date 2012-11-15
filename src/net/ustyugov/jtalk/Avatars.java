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

package net.ustyugov.jtalk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Hashtable;
import java.util.Iterator;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.packet.VCard;

import net.ustyugov.jtalk.service.JTalkService;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

public class Avatars {

	public static void loadAvatar(final Activity activity, final String jid, final ImageView image) {
		new Thread() {
			@Override
			public void run() {
				activity.runOnUiThread(new Runnable() {
					public void run() {
						JTalkService service = JTalkService.getInstance();
						Bitmap bitmap = null;
						
						final Hashtable<String, Bitmap> avHash = service.getAvatarsHash();
						if (avHash.containsKey(jid)) {
							bitmap = avHash.get(jid);
						} else {
							try {
								String fname = Constants.PATH + "/" + jid;
								File file = new File(fname);
								if (file.exists()) {
									bitmap = BitmapFactory.decodeFile(fname);
						    		avHash.put(jid, bitmap);
								}
					    	} catch (Exception ignored) { }
						}
						
						if (bitmap != null && image != null) {
							image.setImageBitmap(bitmap);
							image.setVisibility(View.VISIBLE);
						} else {
                            if (image != null) image.setVisibility(View.INVISIBLE);
                        }
					}
				});
			}
		}.start();
	}
	
	public static void loadAllAvatars(XMPPConnection connection, String group) {
		new LoadAllAvatars(connection, group).execute();
	}
	
	private static class LoadAllAvatars extends AsyncTask<Void, Void, Void> {
		private String group;
		private XMPPConnection connection;
		
		public LoadAllAvatars(XMPPConnection connection, String group) {
			this.connection = connection;
			this.group = group;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			File file = new File(Constants.PATH);
			file.mkdir();
			if (file.list(new Filter(group)).length < 1) {
				JTalkService service = JTalkService.getInstance();
				Iterator<Presence> it = service.getRoster(connection.getUser()).getPresences(group);
				while (it.hasNext()) {
					try {
						Presence p = it.next();
						String jid = p.getFrom();
						
						VCard vcard = new VCard();
						vcard.load(connection, jid);
						byte[] buffer = vcard.getAvatar();
						
						if (buffer != null) {
							FileOutputStream fos = new FileOutputStream(Constants.PATH + "/" + jid.replace("/", "%"));
							fos.write(buffer);
							fos.close();
						}
					} catch (Exception ignored) { }
				}
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			JTalkService.getInstance().sendBroadcast(new Intent(Constants.PRESENCE_CHANGED));
		}
		
		private class Filter implements FilenameFilter {
			private String group;
			
			public Filter(String group) {
				this.group = group;
			}

			@Override
			public boolean accept(File dir, String filename) {
                return filename.contains(group);
			}
			
		}
	}
}
