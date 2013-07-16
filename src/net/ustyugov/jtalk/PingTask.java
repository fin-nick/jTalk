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

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import net.ustyugov.jtalk.listener.ConListener;
import net.ustyugov.jtalk.service.JTalkService;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;

public class PingTask extends AsyncTask<Void, Void, Void> {
    private String account;

    public PingTask(String account) {
        this.account = account;
    }

    @Override
    protected Void doInBackground(Void... params) {
        JTalkService service = JTalkService.getInstance();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
        int timeout = 60000;
        try {
            timeout = Integer.parseInt(prefs.getString("PingTimeout", 60+"")) * 1000;
        } catch (NumberFormatException ignored) { }

        if (service.isAuthenticated(account)) {
            IQ iq = new IQ() {
                @Override
                public String getChildElementXML() {
                    return "<ping xmlns=\"urn:xmpp:ping\" />";
                }
            };
            iq.setPacketID(System.currentTimeMillis()+"");
            iq.setType(IQ.Type.GET);
            iq.setTo(service.getConnection(account).getServiceName());

            PacketCollector collector = service.getConnection(account).createPacketCollector(new PacketIDFilter(iq.getPacketID()));
            service.getConnection(account).sendPacket(iq);

            IQ result = (IQ)collector.nextResult(timeout);
            if (result != null && result.getType() == IQ.Type.RESULT) {
                return null;
            } else {
                Log.e("PingTask", "Pong not received");
                ConListener listener = service.getConnectionListener(account);
                if (listener != null) listener.connectionClosedOnError(null);
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {

    }
}
