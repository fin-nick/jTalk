/*
 * Copyright (C) 2013, Igor Ustyugov <igor@ustyugov.net>
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

package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.PacketExtension;

public class TunesExtension implements PacketExtension {
    String artist = null;
    String source = null;
    String title  = null;

    public TunesExtension() { }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @Override
    public String getElementName() { return "tune"; }

    @Override
    public String getNamespace() { return "http://jabber.org/protocol/tune"; }

    @Override
    public String toXML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<" + getElementName() + " xmlns=\"" + getNamespace() + "\">");
        sb.append("<artist>" + artist + "</artist>");
        sb.append("<title>" + title + "</title>");
        sb.append("<source>" + source + "</source>");
        sb.append("</" + getElementName() + ">");
        return sb.toString();
    }
}
