/*
 * Copyright (C) 2013, Igor Ustyugov <igor@ustyugov.net>
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

package net.ustyugov.jtalk.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import net.ustyugov.jtalk.service.JTalkService;

public class TunesReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("PlayingNow", false)) {
            String artist = null;
            String album  = null;
            String track  = null;

            if (intent.getBooleanExtra("playing", false)) {
                artist = intent.getStringExtra("artist");
                album = intent.getStringExtra("album");
                track = intent.getStringExtra("track");
            }

            JTalkService.getInstance().sendTunes(artist, track, album);
        }
    }
}