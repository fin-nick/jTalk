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

import java.util.*;

import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Presence;


public class SortList {
	
    public static List<HashMap<String, RosterEntry>> sortGroupedContacts(String account, List<HashMap<String, RosterEntry>> list) {
    	JTalkService service = JTalkService.getInstance();
    	
    	List<HashMap<String, RosterEntry>> online  = new ArrayList<HashMap<String, RosterEntry>>();
    	List<HashMap<String, RosterEntry>> chat    = new ArrayList<HashMap<String, RosterEntry>>();
    	List<HashMap<String, RosterEntry>> away    = new ArrayList<HashMap<String, RosterEntry>>();
    	List<HashMap<String, RosterEntry>> xa      = new ArrayList<HashMap<String, RosterEntry>>();
    	List<HashMap<String, RosterEntry>> dnd     = new ArrayList<HashMap<String, RosterEntry>>();
    	List<HashMap<String, RosterEntry>> offline = new ArrayList<HashMap<String, RosterEntry>>();
    	
    	for (HashMap<String, RosterEntry> hm : list) {
    		String jid = hm.get("entry").getUser();
    		Presence.Mode presenceMode = service.getMode(account, jid);
    		Presence.Type presenceType = service.getType(account, jid);
  			
    		if (presenceType == Presence.Type.available) {
    			if (presenceMode == Presence.Mode.chat) {
        			chat.add(hm);
        		} else if (presenceMode == Presence.Mode.away) {
        			away.add(hm);
        		} else if (presenceMode == Presence.Mode.xa) {
        			xa.add(hm);
        		} else if (presenceMode == Presence.Mode.dnd) {
        			dnd.add(hm);
        		} else {
        			online.add(hm);
        		}
    		} else {
    			offline.add(hm);
    		}
    	}
    	
    	List<HashMap<String, RosterEntry>> result = new ArrayList<HashMap<String, RosterEntry>>();
    	result.addAll(chat);
    	result.addAll(chat.size(), online);
    	result.addAll(chat.size() + online.size(), away);
    	result.addAll(chat.size() + online.size() + away.size(), xa);
    	result.addAll(chat.size() + online.size() + away.size() + xa.size(), dnd);
    	result.addAll(chat.size() + online.size() + away.size() + xa.size() + dnd.size(), offline);
		return result;
    }

    public static List<String> sortSimpleContacts(String account, List<String> list) {
        JTalkService service = JTalkService.getInstance();

        List<String> online  = new ArrayList<String>();
        List<String> chat    = new ArrayList<String>();
        List<String> away    = new ArrayList<String>();
        List<String> xa      = new ArrayList<String>();
        List<String> dnd     = new ArrayList<String>();
        List<String> offline = new ArrayList<String>();

        for (String jid : list) {
            Presence.Mode presenceMode = service.getMode(account, jid);
            Presence.Type presenceType = service.getType(account, jid);

            if (presenceType == Presence.Type.available) {
                if (presenceMode == Presence.Mode.chat) {
                    chat.add(jid);
                } else if (presenceMode == Presence.Mode.away) {
                    away.add(jid);
                } else if (presenceMode == Presence.Mode.xa) {
                    xa.add(jid);
                } else if (presenceMode == Presence.Mode.dnd) {
                    dnd.add(jid);
                } else {
                    online.add(jid);
                }
            } else {
                offline.add(jid);
            }
        }

        List<String> result = new ArrayList<String>();
        result.addAll(chat);
        result.addAll(chat.size(),online);
        result.addAll(chat.size()+online.size(), away);
        result.addAll(chat.size()+online.size()+away.size(), xa);
        result.addAll(chat.size()+online.size()+away.size()+xa.size(), dnd);
        result.addAll(chat.size()+online.size()+away.size()+xa.size()+dnd.size(), offline);
        return result;
    }

    public static List<String> sortParticipantsInChat(String account, String group, List<String> list) {
    	JTalkService service = JTalkService.getInstance();

    	List<String> online  = new ArrayList<String>();
    	List<String> chat    = new ArrayList<String>();
    	List<String> away    = new ArrayList<String>();
    	List<String> xa      = new ArrayList<String>();
    	List<String> dnd     = new ArrayList<String>();

    	if (service.getConferencesHash(account).containsKey(group)) {
    		for (String nick : list) {
        		Presence p = service.getConferencesHash(account).get(group).getOccupantPresence(group + "/" + nick);
        		if (p != null) {
        			Presence.Type t = p.getType();
            		String mode;
            		try {
            			Presence.Mode m = p.getMode();
            			if (m == null) mode = "available";
            			else mode = m.name();
            		} catch(NullPointerException e) {
            			mode = "available";
            		}
            		if (t == Presence.Type.available) {
            			if (mode.equals("available")) online.add(nick);
                		else if (mode.equals("chat")) chat.add(nick);
                		else if (mode.equals("away")) away.add(nick);
                		else if (mode.equals("xa"))   xa.add(nick);
                		else if (mode.equals("dnd"))  dnd.add(nick);
            		}
        		}
        	}
    	}

        Collections.sort(chat, new StringComparator());
        Collections.sort(online, new StringComparator());
        Collections.sort(away, new StringComparator());
        Collections.sort(xa, new StringComparator());
        Collections.sort(dnd, new StringComparator());

    	List<String> result = new ArrayList<String>();
    	result.addAll(chat);
    	result.addAll(chat.size(),online);
    	result.addAll(chat.size()+online.size(), away);
    	result.addAll(chat.size()+online.size()+away.size(), xa);
    	result.addAll(chat.size()+online.size()+away.size()+xa.size(), dnd);
		return result;
    }

    public static class StringComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            return s1.toLowerCase().compareTo(s2.toLowerCase());
        }
    }
}
