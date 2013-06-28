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

package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.packet.LocationExtension;
import org.xmlpull.v1.XmlPullParser;

public class LocationProvider implements PacketExtensionProvider {

    @Override
    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        String lat = null;
        String lon = null;

        boolean done = false;
        LocationExtension extension = new LocationExtension();

        while(!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();
                if (name.equals("lat")) {
                    parser.next();
                    lat = parser.getText();
                }
                else if (name.equals("lon")) {
                    parser.next();
                    lon = parser.getText();
                }
            }

            if(parser.getEventType()==XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase(extension.getElementName())){
                done = true;
            }
        }

        if (lat != null) extension.setLat(lat);
        if (lon != null) extension.setLon(lon);
        return extension;
    }
}
