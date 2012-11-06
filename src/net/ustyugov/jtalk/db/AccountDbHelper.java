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

public class AccountDbHelper extends SQLiteOpenHelper implements BaseColumns {
	public static final int VERSION = 3;
	public static final String DB_NAME = "accounts.db";
	public static final String TABLE_NAME = "account";
	public static final String ENABLED = "enabled";
	public static final String JID = "jid";
	public static final String PASS = "password";
	public static final String RESOURCE = "resource";
	public static final String SERVER = "server";
	public static final String PORT = "port";
	public static final String TLS = "tls";
	public static final String SASL = "sasl";

	public AccountDbHelper(Context context) {
		super(context, DB_NAME, null, VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_NAME
				+ " ( _id integer primary key autoincrement, "
				+ JID + " TEXT, "
				+ PASS + " TEXT, "
				+ RESOURCE + " TEXT, "
				+ SERVER + " TEXT, "
				+ PORT + " TEXT, "
				+ ENABLED + " TEXT, "
				+ TLS + " TEXT, "
				+ SASL + " TEXT)");
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (newVersion == 3 && oldVersion < 3) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);
		}
	}
}
