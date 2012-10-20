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
	private String name = null;
	private RosterEntry entry;
	private boolean isCollapsed = false;
	private boolean isGroup = false;
	
	public RosterItem() { }
	
	public boolean isGroup() {return this.isGroup;}
	public boolean isCollapsed() {return this.isCollapsed;}
	
	public void setCollapsed(boolean isExpanded) {this.isCollapsed= isExpanded;}
	public void setGroup(boolean isGroup) {this.isGroup = isGroup;}
	
	public void setEntry(RosterEntry re) {this.entry = re;}
	public RosterEntry getEntry() {return this.entry;}
	
	public void setName(String name) {this.name = name;}
	public String getName() {return this.name;}

}
