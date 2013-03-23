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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jivesoftware.smackx.packet.BobExtension;
import org.jivesoftware.smackx.packet.DataForm;

public class MessageItem {
    private String account;
    private String jid;
	private String time = null;
	private String body = "";
	private String subj = "";
	private String stamp = null;
	private String name = null;
	private String id = null;
    private String baseId = null;
	private Type type = Type.message;
	private boolean edited = false;
	private boolean received = false;
	private boolean collapsed = false;
	private boolean captcha = false;
	private DataForm form = null;
	private BobExtension bob = null;
    private boolean selected = false;
	
//	public MessageItem(String account, String time, String body, String name, String id ) {
//		Date d = new Date();
//	    DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
//	    this.stamp = df.format(d);
//	    this.account = account;
//		this.time = time;
//		this.body = body;
//		this.name = name;
//		this.id = id;
//		this.edited = false;
//		this.received = false;
//		this.collapsed = false;
//	}
	
	public MessageItem(String account, String jid) {
        this.account = account;
        this.jid = jid;
		Date d = new Date();
	    DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	    this.stamp = df.format(d);
        this.edited = false;
		this.received = false;
		this.collapsed = false;
	}

    public void setBaseId(String id) { this.baseId = id; }
    public String getBaseId() { return this.baseId; }
    public void setName(String name) { this.name = name; }
    public void setId(String id) { this.id = id; }
    public void setReceived(Boolean r) { this.received = r;}
    public void setEdited(Boolean e) { this.edited = e; }
    public String getTime() { return this.time; }
    public String getBody() { return this.body; }
    public String getSubject() { return this.subj; }
    public String getName() { return this.name; }
    public void setAccount(String account) { this.account = account; }
    public String getAccount() { return account; }
    public String getId() { return this.id; }
    public void setJid(String jid) { this.jid = jid; }
    public String getJid() { return jid; }
	public boolean isCollapsed() { return this.collapsed; }
	public boolean isReceived() { return this.received; }
	public boolean isEdited() { return this.edited; }
	public boolean containsCaptcha() { return this.captcha; }
	public void setCaptcha(boolean c) { this.captcha = c; }
	public void setForm(DataForm form) { this.form = form; }
	public DataForm getForm() { return this.form; }
	public void setBob(BobExtension bob) { this.bob = bob; }
	public BobExtension getBob() { return this.bob; }
	public void setCollapsed(boolean collapsed) { this.collapsed = collapsed; }
	public void setType(Type type) { if (type != null) this.type = type; }
	public Type getType() { return this.type; }
	public void setTime(String time) { this.time = time; }

    public void select(boolean isSelected) { this.selected = isSelected; }
    public boolean isSelected() {return selected;}

    public void setBody(String body) {
		this.body = body.replaceAll("&lt;", "<");
		this.body = body.replaceAll(";amp;", "&");
	}
	
	public void setSubject(String subject) {
		if (subject != null) {
			this.subj = subject.replaceAll("&lt;", "<");
			this.subj = subject.replaceAll(";amp;", "&");
		}
	}

	public String toXml() {
		StringBuilder sb = new StringBuilder();
		String message = getBody().replaceAll("<", "&lt;").replaceAll("&", ";amp;");
		sb.append("<" + getType().name() + " from='" + name + "' stamp='" + stamp + "'>");
		sb.append(message);
		sb.append("</" + getType().name() + ">\n");
		return sb.toString();
	}
	
	public String toString() {
		return stamp + " " + name + "\n" + body + "\n\n";
	}
	
	public enum Type {
		message,
		status;
	}
}
