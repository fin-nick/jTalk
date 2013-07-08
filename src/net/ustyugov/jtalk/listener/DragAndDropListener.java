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

package net.ustyugov.jtalk.listener;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import net.ustyugov.jtalk.MessageItem;

public class DragAndDropListener implements AdapterView.OnItemLongClickListener {
    private Context context;

    public DragAndDropListener(Context context) {
        this.context = context;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long l) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showtime = prefs.getBoolean("ShowTime", false);

        MessageItem message = (MessageItem) parent.getAdapter().getItem(position);
        String body = message.getBody();

        String time = message.getTime();
        String name = message.getName();
        String t = "(" + time + ")";
        if (showtime) name = t + " " + name;
        String text = "> " + name + ": " + body + "\n";

        ClipData.Item item = new ClipData.Item(text);

        String[] mimes = {"text/plain"};
        ClipData dragData = new ClipData(text, mimes, item);
        View.DragShadowBuilder myShadow = new MyDragShadowBuilder(view);
        view.startDrag(dragData, myShadow, null, 0);

        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(dragData);
        Toast.makeText(context, "Message is copied", Toast.LENGTH_SHORT).show();
        return true;
    }

    private class MyDragShadowBuilder extends View.DragShadowBuilder {
        private Drawable shadow;

        public MyDragShadowBuilder(View v) {
            super(v);
            Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(),Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Drawable bgDrawable = v.getBackground();
            if (bgDrawable != null) bgDrawable.draw(canvas);
            else canvas.drawColor(Color.LTGRAY);
            v.draw(canvas);
            shadow = new BitmapDrawable(context.getResources(), bitmap);
        }

        @Override
        public void onProvideShadowMetrics (Point size, Point touch) {
            int width;
            int height;
            width = getView().getWidth() / 2;
            height = getView().getHeight() / 2;
            shadow.setBounds(0, 0, width, height);
            size.set(width, height);
            touch.set(width / 2, height / 2);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            shadow.draw(canvas);
        }
    }
}
