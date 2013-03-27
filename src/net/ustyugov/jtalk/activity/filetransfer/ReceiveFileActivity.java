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

package net.ustyugov.jtalk.activity.filetransfer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.jtalk2.R;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.Notify;
import net.ustyugov.jtalk.activity.RosterActivity;
import net.ustyugov.jtalk.service.JTalkService;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;

import java.io.File;

public class ReceiveFileActivity extends SherlockActivity implements View.OnClickListener {
    private static final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download";
    private Button ok, cancel;
    private FileTransferRequest request;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
        setContentView(R.layout.receive_file);
        setTitle(R.string.AcceptFile);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        LinearLayout linear = (LinearLayout) findViewById(R.id.linear);
        linear.setBackgroundColor(Colors.BACKGROUND);

        JTalkService service = JTalkService.getInstance();
        if (service != null && service.getIncomingRequests().size() > 0) {
            request = service.getIncomingRequests().remove(0);
            if(service.getIncomingRequests().isEmpty()) Notify.cancelFileRequest();

            TextView from = (TextView) findViewById(R.id.from);
            from.setText(request.getRequestor()+"");

            TextView name = (TextView) findViewById(R.id.name);
            name.setText(request.getFileName()+"");

            TextView size = (TextView) findViewById(R.id.size);
            size.setText(request.getFileSize()+"");

            TextView mime = (TextView) findViewById(R.id.mime);
            mime.setText(request.getMimeType()+"");

            TextView desc = (TextView) findViewById(R.id.desc);
            desc.setText(request.getDescription()+"");

            ok = (Button) findViewById(R.id.ok);
            ok.setOnClickListener(this);

            cancel = (Button) findViewById(R.id.cancel);
            cancel.setOnClickListener(this);
        } else finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                request.reject();
                startActivity(new Intent(this, RosterActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        if (view == ok) {
            try {
                File f = new File(path);
                f.mkdirs();
                f = new File(path + "/" + request.getFileName());

                IncomingFileTransfer in = request.accept();
                in.recieveFile(f);
                String name = request.getFileName();

                while (!in.isDone()) {
                    FileTransfer.Status status = in.getStatus();
                    Notify.incomingFileProgress(name, status);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) { }
                }
                Notify.incomingFileProgress(name, in.getStatus());
            } catch (Exception e) {
                Notify.incomingFileProgress(request.getFileName(), FileTransfer.Status.error);
            }
            finish();
        } else if (view == cancel) {
            request.reject();
            finish();
        }
    }
}
