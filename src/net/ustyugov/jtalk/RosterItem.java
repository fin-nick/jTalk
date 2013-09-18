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

public class RosterItem {
	private String name;
    private String jid;
    private String state = "unavailable";
    private String status = "";
	private String account;
	private Type type;
	private boolean isCollapsed = false;
	private Object object;
	
	public enum Type {account, group, entry, self, muc}

    public RosterItem(String account, Type type, String jid, String state, String status) {
		this.account = account; 
		this.type = type;
        this.jid = jid;
        this.state = state;
        this.status = status;
	}

    public RosterItem(String account, Type type, String jid) {
        this.account = account;
        this.type = type;
        this.name = jid;
        this.jid = jid;
    }

	public boolean isGroup() {return type == Type.group;}
	public boolean isAccount() {return type == Type.account;}
	public boolean isEntry() {return type == Type.entry;}
	public boolean isSelf() {return type == Type.self;}
	public boolean isMuc() {return type == Type.muc;}

    public boolean isCollapsed() {return this.isCollapsed;}
	public void setCollapsed(boolean isExpanded) {this.isCollapsed= isExpanded;}

    public void setName(String name) {
        if (name != null && name.length() > 0) this.name = name;
    }
	public String getName() { return name; }

    public String getAccount() { return this.account; }

    public void setJid(String jid) { this.jid = jid; }
    public String getJid() { return jid; }

    public void setState(String state) { this.state = state; }
    public String getState() { return state; }

    public void setStatus(String status) {
        if (status != null && status.length() > 0) this.status = status;
    }
    public String getStatus() {return status; }


	public void setObject(Object object) { this.object = object; }
    public Object getObject() { return object; }
}
