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

package net.ustyugov.jtalk.dialog;

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.service.JTalkService;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;

import com.jtalk2.R;

public class JuickMessageMenuDialog implements OnClickListener {
//	private static final String JUICK = "juick@juick.com";
	private static final String JUBO  = "jubo@nologin.ru";
	private static final String PSTO  = "psto@psto.net";
    private Context context;
    private JTalkService service;
    private String jid;
    private String text;
	private CharSequence[] items = null;


    public JuickMessageMenuDialog(Context context, String text) {
        this.context = context;
        this.service = JTalkService.getInstance();
        this.jid = service.getCurrentJid();
        this.text = text;
    }

    public void show() {
    	if (text.startsWith("#")) {
    		items = new CharSequence[6];
    	    items[0] = context.getString(R.string.Reply);
    	    items[1] = context.getString(R.string.ViewComments);
    	    items[2] = context.getString(R.string.RecommendPost);
    	    items[3] = context.getString(R.string.Subscribe);
    	    items[4] = context.getString(R.string.Unsubscribe);
    	    items[5] = context.getString(R.string.Remove);
    	} else if (text.startsWith("@")) {
    		items = new CharSequence[5];
    	    items[0] = text;
    	    items[1] = context.getString(R.string.PM);
    	    items[2] = context.getString(R.string.Subscribe);
    	    items[3] = context.getString(R.string.Unsubscribe);
    	    items[4] = context.getString(R.string.ToBL);
    	}
    	
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.Actions);
        builder.setItems(items, this);
        builder.create().show();
    }

    public void onClick(DialogInterface dialog, int which) { 
    	Intent intent = new Intent(Constants.PASTE_TEXT);
		if (jid.equals(JUBO)) intent.putExtra("jubo", true);
        if (text.startsWith("#")) {
        	String id = null;
    		if (text.indexOf("/") > 0) id = text.substring(0, text.indexOf("/"));
    		else id = text;
        	
        	switch(which) {
        		case 0:
        			intent.putExtra("text", text);
        			break;
        		case 1:
            		intent.putExtra("text", id + "+");
        			break;
        		case 2:
        			intent.putExtra("text", "! " + id);
        			break;
        		case 3:
        			intent.putExtra("text", "S " + id);
        			break;
        		case 4:
        			intent.putExtra("text", "U " + id);
        			break;
        		case 5:
        			intent.putExtra("text", "D " + id);
        			break;
        		default:
        			break;
        	}
        } else if (text.startsWith("@")) {
        	switch(which) {
    			case 0:
    				intent.putExtra("text", text);
    				break;
    			case 1:
    				if (jid.equals(PSTO)) intent.putExtra("text", "P " + text); else intent.putExtra("text", "PM " + text);
    				break;
    			case 2:
    				intent.putExtra("text", "S " + text);
    				break;
    			case 3:
    				intent.putExtra("text", "U " + text);
    				break;
    			case 4:
    				intent.putExtra("text", "BL " + text);
    				break;
    			default:
    				break;
        	}
        }
        context.sendBroadcast(intent);
    }
}
