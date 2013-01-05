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

package net.ustyugov.jtalk.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import net.ustyugov.jtalk.Constants;

public class MessageDbHelper extends SQLiteOpenHelper implements BaseColumns {
	public static final String TABLE_NAME = "msg";
	public static final String ID = "id";
	public static final String JID = "jid";
	public static final String STAMP = "stamp";
	public static final String NICK = "nick";
	public static final String TYPE = "type";
	public static final String BODY = "body";
	public static final String FORM = "form";
	public static final String BOB = "bob";
	public static final String RECEIVED = "received";
	public static final String COLLAPSED = "collapsed";

	public MessageDbHelper(Context context, String path) {
        super(context, path, null, 1);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME
                + " ( _id integer primary key autoincrement, "
                + ID + " TEXT, "
                + JID + " TEXT, "
                + STAMP + " TEXT, "
                + NICK + " TEXT, "
                + TYPE + " TEXT, "
                + FORM + " LONGTEXT, "
                + BOB + " LONGTEXT, "
                + RECEIVED + " TEXT, "
                + COLLAPSED + " TEXT, "
                + BODY + " LONGTEXT)");
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
//		onCreate(db);
	}
}
