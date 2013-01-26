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

import org.jivesoftware.smack.RosterEntry;

public class RosterItem {
	private String name;
	private RosterEntry entry;
	private String account;
	private Type type;
	private boolean isCollapsed = false;
	private Object object;
	
	public enum Type {account, group, entry, self, muc}

    public RosterItem(String account, Type type, RosterEntry entry) {
		this.account = account; 
		this.type = type;
		this.entry = entry;
	}

	public boolean isGroup() {return type == Type.group;}
	public boolean isAccount() {return type == Type.account;}
	public boolean isEntry() {return type == Type.entry;}
	public boolean isSelf() {return type == Type.self;}
	public boolean isMuc() {return type == Type.muc;}
	
	public boolean isCollapsed() {return this.isCollapsed;}
	
	public void setCollapsed(boolean isExpanded) {this.isCollapsed= isExpanded;}
	
	public RosterEntry getEntry() {return this.entry;}
	
	public void setName(String name) { this.name = name; }
	public String getName() { 
		if (entry == null) return name;
		if (entry.getName() != null && entry.getName().length() > 0) return entry.getName();
		else return entry.getUser();
	}

	public String getAccount() { return this.account; }
	public Object getObject() { return object; }
	public void setObject(Object object) { this.object = object; } 
}
