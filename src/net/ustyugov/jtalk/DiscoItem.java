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

public class DiscoItem {
	private String jid = null;
	private String name;
	private String node;
	private String type;
	private String category;
	private boolean register = false;
	private boolean muc = false;
	private boolean vcard = false;
	
	public DiscoItem() {
		
	}
	
	public String getJid() { return jid; }
	public String getName() { return name; }
	public String getNode() { return node; }
	public String getType() { return type; }
	public String getCategory() { return category; }
	public boolean isRegister() { return register; }
	public boolean isMUC() { return muc; }
	public boolean isVCard() { return vcard; }
	
	public void setJid(String jid) { this.jid = jid; }
	public void setName(String name) { this.name = name; }
	public void setNode(String node) { this.node = node; }
	public void setType(String type) { this.type = type; }
	public void setCategory(String category) { this.category = category; }
	public void setRegister(boolean register) { this.register = register; }
	public void setMUC(boolean muc) { this.muc = muc; }
	public void setVCard(boolean vcard) { this.vcard = vcard; }
}
