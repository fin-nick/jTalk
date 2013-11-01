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

package net.ustyugov.jtalk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MyExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Context context;

    public MyExceptionHandler(Context context) {
        this.context = context;
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable throwable) {
        String version = "none";
        int build = 0;
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            version = packageInfo.versionName;
            build = packageInfo.versionCode;
        } catch (Exception ignored) { }

        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder
                .append("\n\n\n")
                .append(String.format("Version: %s (%d)\n", version, build))
                .append(thread.toString()).append("\n");
                processThrowable(throwable, reportBuilder);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("BUG", true);
        editor.commit();

        File f = new File(Constants.PATH_LOG + "log.txt");
        f.getParentFile().mkdirs();
        FileWriter writer = null;
        try {
            writer = new FileWriter(f, false);
            writer.write(reportBuilder.toString());
        } catch (IOException ignored) {
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException ignored) { }
        }
        System.exit(0);
    }

    private void processThrowable(Throwable exception, StringBuilder builder) {
        if(exception == null) return;
        StackTraceElement[] stackTraceElements = exception.getStackTrace();
        builder
                .append("\nException: ").append(exception.getClass().getName()).append("\n")
                .append("Message: ").append(exception.getMessage()).append("\n\nStacktrace:\n");
        for(StackTraceElement element : stackTraceElements) {
            builder.append("\t").append(element.toString()).append("\n");
        }
        processThrowable(exception.getCause(), builder);
    }
}
