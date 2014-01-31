/*
 * Copyright (C) 2014, Igor Ustyugov <igor@ustyugov.net>
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

package org.jivesoftware.smackx.note;

import org.jivesoftware.smackx.packet.PrivateData;
import org.jivesoftware.smackx.provider.PrivateDataProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Notes implements PrivateData {
    public static String NAMESPACE = "http://miranda-im.org/storage#notes";

    private List<Note> notes;

    public Notes() { notes = new ArrayList<Note>(); }

    public void addNote(Note note) { notes.add(note); }
    public void removeNote(Note note) { notes.remove(note); }
    public void clearNotes() { notes.clear(); }
    public List<Note> getNotes() {
        return notes;
    }
    public String getElementName() { return "storage"; }
    public String getNamespace() { return NAMESPACE; }

    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<" + getElementName() + " xmlns=\"" + getNamespace() + "\">");

        final Iterator<Note> notes = getNotes().iterator();
        while (notes.hasNext()) {
            Note note = notes.next();
            buf.append("<note tags=\"").append(note.getTag()).append("\">");
            buf.append("<title>").append(note.getTittle()).append("</title>");
            buf.append("<text>").append(note.getText()).append("</text>");
            buf.append("</note>");
        }
        buf.append("</" + getElementName() + ">");
        return buf.toString();
    }

    public static class Provider implements PrivateDataProvider {
        public Provider() { super(); }

        public PrivateData parsePrivateData(XmlPullParser parser) throws Exception {
            Notes storage = new Notes();

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG && "note".equals(parser.getName())) {
                    final Note noteStorage = getNoteStorage(parser);
                    if (noteStorage != null) {
                        storage.addNote(noteStorage);
                    }
                }
                else if (eventType == XmlPullParser.END_TAG && "storage".equals(parser.getName())) {
                    done = true;
                }
            }

            return storage;
        }
    }

    private static Note getNoteStorage(XmlPullParser parser) throws IOException, XmlPullParserException {
        String tag = parser.getAttributeValue("", "tags");
        String title = "";
        String text = "";

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG && "title".equals(parser.getName())) {
                title = parser.nextText();
            }
            else if (eventType == XmlPullParser.START_TAG && "text".equals(parser.getName())) {
                text = parser.nextText();
            }
            else if (eventType == XmlPullParser.END_TAG && "note".equals(parser.getName())) {
                done = true;
            }
        }

        Note note = new Note(title, text, tag);
        return note;
    }
}
