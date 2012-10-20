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

import net.ustyugov.jtalk.service.JTalkService;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

public class SoundTask extends AsyncTask<String, Integer, Integer> {
	MediaPlayer mp;
	
	@Override
	protected Integer doInBackground(String... params) {
		JTalkService service = JTalkService.getInstance();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
		String path = prefs.getString("ringtone_conferences", "");
		if (path.length() > 0) {
			Log.i("SoundTask", "path.length = " + path);
			try {
				mp = new MediaPlayer();
				mp.reset();
				mp.setDataSource(service, Uri.parse(path));
				mp.prepare();
	    		mp.start();
	    		while (mp.isPlaying()) { }
	    		mp.stop();
	    		mp.release();
			} catch (Exception e) {
				Log.i("SoundTask", "Exception! " + e.getLocalizedMessage());
			}
		}
		return null;
	}
	
	@Override
	protected void onPostExecute(Integer result) {
//		if (mp != null) {
//			mp.stop();
//			Log.i("SoundTask", "MP stop");
//			mp.release();
//			Log.i("SoundTask", "MP release");
//		}
	}
}
