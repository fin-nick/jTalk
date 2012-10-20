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

import com.jtalk2.R;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class WidgetDbHelper extends SQLiteOpenHelper implements BaseColumns {
	public static final String DB_NAME = "widget.db";
	public static final String TABLE_NAME = "widget";
	public static final String MODE = "mode";
	public static final String COUNTER = "count";
	private Context context;
	
	public WidgetDbHelper(Context context) {
		super(context, DB_NAME, null, 1);
		this.context = context;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_NAME
				+ " ( _id integer primary key autoincrement, "
				+ MODE + " TEXT, "
				+ COUNTER + " TEXT)");
		
		ContentValues values = new ContentValues();
        values.put(WidgetDbHelper.MODE, context.getString(R.string.NotConnected));
        values.put(WidgetDbHelper.COUNTER, "0");
		db.insert(TABLE_NAME, MODE, values);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
//		onCreate(db);
	}
}
