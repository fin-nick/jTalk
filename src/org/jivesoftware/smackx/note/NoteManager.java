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

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.PrivateDataManager;

import java.util.*;

public class NoteManager {
    private static final Map<Connection, NoteManager> noteManagerMap = new HashMap<Connection, NoteManager>();
    static {
        PrivateDataManager.addPrivateDataProvider("storage", Notes.NAMESPACE,
                new Notes.Provider());
    }

    public synchronized static NoteManager getNoteManager(Connection connection)
            throws XMPPException
    {
        NoteManager manager = noteManagerMap.get(connection);
        if(manager == null) {
            manager = new NoteManager(connection);
            noteManagerMap.put(connection, manager);
        }
        return manager;
    }

    private PrivateDataManager privateDataManager;
    private Notes notes;

    private NoteManager(Connection connection) throws XMPPException {
        if(connection == null || !connection.isAuthenticated()) {
            throw new XMPPException("Invalid connection.");
        }
        this.privateDataManager = new PrivateDataManager(connection);
    }

    public Collection<Note> getNotes() throws XMPPException {
        Notes notes = retrieveNotes();
        return Collections.unmodifiableCollection(notes.getNotes());
    }

    public void addNote(Note note) throws XMPPException {
        notes.addNote(note);
        privateDataManager.setPrivateData(notes);
    }

    public void removeNote(Note note) throws XMPPException {
        Notes notes = retrieveNotes();
        Iterator<Note> it = notes.getNotes().iterator();
        while(it.hasNext()) {
            Note n = it.next();
            if(note == n) {
                it.remove();
                privateDataManager.setPrivateData(notes);
                return;
            }
        }
    }

    private Notes retrieveNotes() throws XMPPException {
        if (notes == null) {
            notes = (Notes) privateDataManager.getPrivateData("storage", Notes.NAMESPACE);
        }
        return notes;
    }

    public void updateNotes() throws XMPPException {
        notes = (Notes) privateDataManager.getPrivateData("storage", Notes.NAMESPACE);
    }
}
