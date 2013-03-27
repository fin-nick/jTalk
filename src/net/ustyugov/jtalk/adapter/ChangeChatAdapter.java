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

package net.ustyugov.jtalk.adapter;

import java.util.Enumeration;

import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.RosterItem;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.util.StringUtils;

import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jtalk2.R;

public class ChangeChatAdapter extends ArrayAdapter<RosterItem> {
	private JTalkService service;
	
	public ChangeChatAdapter(Context context) {
		super(context, R.layout.selector);
        this.service = JTalkService.getInstance();
        
        clear();
		Cursor cursor = service.getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '" + 1 + "'", null, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			do {
				String account = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID)).trim();
				XMPPConnection connection = service.getConnection(account);

                for (String jid : service.getActiveChats(account)) {
                    if (!service.getConferencesHash(account).containsKey(jid)) {
                        RosterEntry entry = null;
                        Roster roster = service.getRoster(account);
                        if (roster != null) entry = roster.getEntry(jid);
                        if (entry == null) entry = new RosterEntry(jid, jid, RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem item = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(item);
                    }
                }

				Enumeration<String> groupEnum = service.getConferencesHash(account).keys();
				while(groupEnum.hasMoreElements()) {
					String name = groupEnum.nextElement();
					RosterItem item = new RosterItem(account, RosterItem.Type.muc, null);
					item.setName(name);
					add(item);
				}
			} while (cursor.moveToNext());
			cursor.close();
		}
    }
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
        View v = convertView;
        RosterItem item = getItem(position);
        String account = item.getAccount();
        
        String jid = "";
        if (item.isMuc()) jid = item.getName();
        else if (item.isEntry()) jid = item.getEntry().getUser();
        String name = item.getName();
        
        IconPicker ip = service.getIconPicker();
        
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.selector, null);
        }
        
        TextView label = (TextView) v.findViewById(R.id.item);
        if (service.isHighlight(account, jid)) label.setTextColor(Colors.HIGHLIGHT_TEXT);
        else {
        	if (Build.VERSION.SDK_INT >= 11) {
            	label.setTextColor(Colors.PRIMARY_TEXT);
            } else label.setTextColor(0xFF232323);
        }
        
		ImageView msg  = (ImageView) v.findViewById(R.id.msg);
		msg.setImageBitmap(ip.getMsgBitmap());
		msg.setVisibility(service.getMessagesCount(account, jid) > 0 ? View.VISIBLE : View.GONE);
		
		ImageView icon = (ImageView) v.findViewById(R.id.status);
        if (service.getJoinedConferences().containsKey(jid)) {
        	icon.setImageBitmap(ip.getMucBitmap());
        } else {
        	if (service.getJoinedConferences().containsKey(StringUtils.parseBareAddress(jid))) {
        		name = StringUtils.parseResource(jid);
        	} else {
        		name = jid;
        		RosterEntry re = JTalkService.getInstance().getRoster(account).getEntry(jid);
                if (re != null && re.getName() != null && re.getName().length() > 0) name = re.getName();
        	}
            
            Presence presence = service.getPresence(account, jid);
        	icon.setImageBitmap(ip.getIconByPresence(presence));
        }
        
        label.setText(name);
        return v;
    }
}
