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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import net.ustyugov.jtalk.Constants;

import java.io.*;

public class JTalkProvider  extends ContentProvider {
	public static final Uri ACCOUNT_URI = Uri.parse("content://com.jtalk2/account");
	public static final Uri CONTENT_URI = Uri.parse("content://com.jtalk2/message");
	public static final Uri TEMPLATES_URI = Uri.parse("content://com.jtalk2/template");
	public static final Uri WIDGET_URI = Uri.parse("content://com.jtalk2/widget");
	
	private SQLiteDatabase msg_db;
	private SQLiteDatabase wdg_db;
	private SQLiteDatabase acc_db;
	private SQLiteDatabase tmp_db;
	
	@Override
	public boolean onCreate() {
        String path = "msg.db";
        String state = Environment.getExternalStorageState();
        if (Build.VERSION.SDK_INT > 7 && Environment.MEDIA_MOUNTED.equals(state)) {
            path = Constants.PATH_LOG + "msg.db";
            try {
                File file = new File(Constants.PATH_LOG);
                file.mkdirs();
                file = new File(path);
                if (!file.exists()) {
                    InputStream input = new FileInputStream("/data/data/com.jtalk2/databases/msg.db");
                    OutputStream output = new FileOutputStream(path);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = input.read(buffer)) > 0) { output.write(buffer, 0, length); }
                    output.flush();
                    output.close();
                    input.close();
                }
            } catch (Exception ignored) { }
        }

		msg_db = new MessageDbHelper(getContext(), path).getWritableDatabase();
		wdg_db = new WidgetDbHelper(getContext()).getWritableDatabase();
		acc_db = new AccountDbHelper(getContext()).getWritableDatabase();
		tmp_db = new TemplatesDbHelper(getContext()).getWritableDatabase();
		return (msg_db != null);
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Cursor c = null;
		if (uri.equals(CONTENT_URI)) c = msg_db.query(MessageDbHelper.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
		else if (uri.equals(WIDGET_URI)) c = wdg_db.query(WidgetDbHelper.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
		else if (uri.equals(ACCOUNT_URI)) c = acc_db.query(AccountDbHelper.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
		else if (uri.equals(TEMPLATES_URI)) c = tmp_db.query(TemplatesDbHelper.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}
	
	@Override
	public Uri insert(Uri url, ContentValues values) {
		if (url.equals(CONTENT_URI)) {
			long rowId = msg_db.insert(MessageDbHelper.TABLE_NAME, MessageDbHelper.ID, values);
			
			if (rowId > 0) {
				Uri uri = ContentUris.withAppendedId(CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(uri, null);
				return uri;
			} else {
				throw new SQLException("Failed to insert row into " + url);
			}
		} else if (url.equals(WIDGET_URI)) {
			long rowId = wdg_db.insert(WidgetDbHelper.TABLE_NAME, WidgetDbHelper.MODE, values);
			
			if (rowId > 0) {
				Uri uri = ContentUris.withAppendedId(WIDGET_URI, rowId);
				getContext().getContentResolver().notifyChange(uri, null);
				return uri;
			} else {
				throw new SQLException("Failed to insert row into " + url);
			}
		} else if (url.equals(ACCOUNT_URI)) {
			long rowId = acc_db.insert(AccountDbHelper.TABLE_NAME, AccountDbHelper.JID, values);
			
			if (rowId > 0) {
				Uri uri = ContentUris.withAppendedId(ACCOUNT_URI, rowId);
				getContext().getContentResolver().notifyChange(uri, null);
				return uri;
			} else {
				throw new SQLException("Failed to insert row into " + url);
			}
		} else if (url.equals(TEMPLATES_URI)) {
			long rowId = tmp_db.insert(TemplatesDbHelper.TABLE_NAME, TemplatesDbHelper.TEXT, values);
			
			if (rowId > 0) {
				Uri uri = ContentUris.withAppendedId(TEMPLATES_URI, rowId);
				getContext().getContentResolver().notifyChange(uri, null);
				return uri;
			} else {
				throw new SQLException("Failed to insert row into " + url);
			}
		} else return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int retVal = 0;
		if (uri.equals(CONTENT_URI)) retVal = msg_db.update(MessageDbHelper.TABLE_NAME, values, selection, selectionArgs);
		else if (uri.equals(WIDGET_URI)) retVal = wdg_db.update(WidgetDbHelper.TABLE_NAME, values, selection, selectionArgs);
		else if (uri.equals(ACCOUNT_URI)) retVal = acc_db.update(AccountDbHelper.TABLE_NAME, values, selection, selectionArgs);
		else if (uri.equals(TEMPLATES_URI)) retVal = tmp_db.update(TemplatesDbHelper.TABLE_NAME, values, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return retVal;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int retVal = 0;
		if (uri.equals(CONTENT_URI)) retVal = msg_db.delete(MessageDbHelper.TABLE_NAME, selection, selectionArgs);
		else if (uri.equals(WIDGET_URI)) retVal = wdg_db.delete(WidgetDbHelper.TABLE_NAME, selection, selectionArgs);
		else if (uri.equals(ACCOUNT_URI)) retVal = acc_db.delete(AccountDbHelper.TABLE_NAME, selection, selectionArgs);
		else if (uri.equals(TEMPLATES_URI)) retVal = tmp_db.delete(TemplatesDbHelper.TABLE_NAME, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return retVal;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}
}
