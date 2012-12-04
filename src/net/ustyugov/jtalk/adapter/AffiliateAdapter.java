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

import java.util.Collection;

import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smackx.muc.Affiliate;

import com.jtalk2.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AffiliateAdapter extends ArrayAdapter<Affiliate> {
	private Context context;
	
	public AffiliateAdapter(Context context) {
		super(context, R.id.item);
		this.context = context;
	}
	
	public void update(Collection<Affiliate> collection) {
		clear();
		for (Affiliate a: collection) add(a);
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
        View v = convertView;
        Affiliate affiliate = getItem(position);
        IconPicker ip = JTalkService.getInstance().getIconPicker();
        String jid = affiliate.getJid();
        String aff = affiliate.getAffiliation();
        
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.selector, null);
        }
        
        TextView label = (TextView) v.findViewById(R.id.item);
        label.setText(jid);
        
      	ImageView icon = (ImageView) v.findViewById(R.id.status);
      	if (aff.equals("outcast")) icon.setImageBitmap(ip.getVisitorBitmap());
      	else if (aff.equals("member")) icon.setImageBitmap(ip.getParticipantBitmap());
      	else icon.setImageBitmap(ip.getModeratorBitmap());
        return v;
    }
}
