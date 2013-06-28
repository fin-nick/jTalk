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

import android.util.Log;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.packet.TunesExtension;
import org.xmlpull.v1.XmlPullParser;

public class TunesProvider implements PacketExtensionProvider {

    @Override
    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        String artist = null;
        String source = null;
        String title  = null;

        boolean done = false;
        TunesExtension extension = new TunesExtension();

        while(!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();
                if (name.equals("artist")) {
                    parser.next();
                    artist = parser.getText();
                }
                else if (name.equals("source")) {
                    parser.next();
                    source = parser.getText();
                }
                else if (name.equals("title")) {
                    parser.next();
                    title = parser.getText();
                }
            }

            if(parser.getEventType()==XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase(extension.getElementName())){
                done = true;
            }
        }

        if (artist != null) extension.setArtist(artist);
        if (title != null) extension.setTitle(title);
        if (source != null) extension.setSource(source);
        return extension;
    }
}
