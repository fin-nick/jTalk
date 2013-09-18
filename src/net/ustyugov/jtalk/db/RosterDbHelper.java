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

public class RosterDbHelper extends SQLiteOpenHelper implements BaseColumns {
	public static final int VERSION = 1;
	public static final String DB_NAME = "roster";
	public static final String TABLE_NAME = "roster";
	public static final String ACCOUNT = "account";
	public static final String JID = "jid";
    public static final String NAME = "name";
    public static final String GROUP = "groups";
	public static final String STATUS = "status";
	public static final String STATE = "state";
//    public static final String SUBSCRIPTION = "subscription";

	public RosterDbHelper(Context context) {
		super(context, DB_NAME, null, VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_NAME
				+ " ( _id integer primary key autoincrement, "
				+ ACCOUNT + " TEXT, "
				+ JID + " TEXT, "
				+ STATE + " TEXT, "
                + GROUP + " TEXT, "
                + NAME + " TEXT, "
				+ STATUS + " TEXT)");
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (newVersion == 2 && oldVersion < 2) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);
		}
	}
}
