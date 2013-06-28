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

public class LocationExtension implements PacketExtension {
    private String lat, lon;

    public LocationExtension() { }

    public String getLat() { return lat; }
    public void setLat(String lat) { this.lat = lat; }

    public String getLon() { return lon; }
    public void setLon(String lon) { this.lon = lon; }

    @Override
    public String getElementName() {
        return "geoloc";
    }

    @Override
    public String getNamespace() {
        return "http://jabber.org/protocol/geoloc";
    }

    @Override
    public String toXML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<" + getElementName() + " xmlns=\"" + getNamespace() + "\">");
        if (lat != null) sb.append("<lat>" + lat + "</lat>");
        if (lon != null) sb.append("<lon>" + lon + "</lon>");
        sb.append("</" + getElementName() + ">");
        return sb.toString();
    }
}
