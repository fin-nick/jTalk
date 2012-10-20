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

package net.ustyugov.jtalk.dialog;

import java.io.File;

import net.ustyugov.jtalk.Notify;

import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;

import com.jtalk2.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Environment;

public class IncomingFileDialog {
	private static final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/download";
	private Context context;
	private FileTransferRequest request;
	
	public IncomingFileDialog(Context context, FileTransferRequest request) {
		this.context = context;
		this.request = request;
	}
	
	public void show() {
		String str =  "Name: " + request.getFileName() + "\nFrom: " + request.getRequestor() + "\nSize: " + request.getFileSize() 
		+ "\nMime: " + request.getMimeType() + "\nDescription: " + request.getDescription();
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(context.getString(R.string.AcceptFile));
        builder.setMessage(str);
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				new Thread() {
					@Override
					public void run() {
						try {
							File f = new File(path);
							f.mkdirs();
							f = new File(path + "/" + request.getFileName());
							
							IncomingFileTransfer in = request.accept();
							in.recieveFile(f);
							String name = request.getFileName();
							
							while (!in.isDone()) {
								Status status = in.getStatus();
								Notify.incomingFileProgress(name, status);
								try {
									Thread.sleep(1000);
								} catch (InterruptedException ex) { }
							}
							Notify.incomingFileProgress(name, in.getStatus());
						} catch (Exception e) {
							Notify.incomingFileProgress(request.getFileName(), Status.error);
						}
					}
				}.start();
			}
        });
        builder.setNegativeButton("No", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				request.reject();
				dialog.dismiss();
			}
        });
        builder.create().show();
	}
}
