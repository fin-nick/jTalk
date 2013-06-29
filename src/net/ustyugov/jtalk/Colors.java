package net.ustyugov.jtalk;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileReader;

public class Colors {
    public static boolean isLight = true;
    public static String currentTheme = "Light";
    public static int BACKGROUND = 0xFFFFFFFF;
    public static int ACCOUNT_BACKGROUND = 0xEE999999;
    public static int GROUP_BACKGROUND = 0xEECCCCCC;
    public static int ENTRY_BACKGROUND = 0xEEEFEFEF;
    public static int PRIMARY_TEXT = 0xFF000000;
    public static int SECONDARY_TEXT = 0xFF666666;
    public static int HIGHLIGHT_TEXT = 0xFFCC0000;
    public static int SEARCH_BACKGROUND = 0xFFAAAA66;
    public static int LINK = 0xFF2323AA;
    public static int INBOX_MESSAGE = 0xFF2323AA;
    public static int OUTBOX_MESSAGE = 0xFFAA2323;
    public static int SELECTED_MESSAGE = 0xFFCCCCCC;
    public static int STATUS_MESSAGE = 0xFF239923;
    public static int STATUS_AWAY = 0xFF22bcef;
    public static int STATUS_XA = 0xFF3c788c;
    public static int STATUS_DND = 0xFFee0000;
    public static int STATUS_CHAT = 0xFF008e00;
    public static int ROLE_MODERATOR = 0xFFFF8800;
    public static int ROLE_VISITOR = 0xFF777777;

    public static void updateColors(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = prefs.getString("ColorTheme", "Light");
        if (theme.equals(currentTheme)) return;
        else if (theme.equals("Light")) {
            isLight = true;
            BACKGROUND = 0xFFFFFFFF;
            ACCOUNT_BACKGROUND = 0xEE999999;
            GROUP_BACKGROUND = 0xEECCCCCC;
            ENTRY_BACKGROUND = 0xEEEFEFEF;
            PRIMARY_TEXT = 0xFF000000;
            SECONDARY_TEXT = 0xFF666666;
            HIGHLIGHT_TEXT = 0xFFCC0000;
            SEARCH_BACKGROUND = 0xFFAAAA66;
            LINK = 0xFF2323AA;
            INBOX_MESSAGE = 0xFF2323AA;
            OUTBOX_MESSAGE = 0xFFAA2323;
            SELECTED_MESSAGE = 0xFFCCCCCC;
            STATUS_MESSAGE = 0xFF239923;
            STATUS_AWAY = 0xFF22bcef;
            STATUS_XA = 0xFF3c788c;
            STATUS_DND = 0xFFee0000;
            STATUS_CHAT = 0xFF008e00;
            ROLE_MODERATOR = 0xFFFF8800;
            ROLE_VISITOR = 0xFF777777;
            currentTheme = "Light";
        } else if (theme.equals("Dark")) {
            isLight = false;
            BACKGROUND = 0xFF000000;
            ACCOUNT_BACKGROUND = 0x77888888;
            GROUP_BACKGROUND = 0x77404040;
            ENTRY_BACKGROUND = 0x55222222;
            PRIMARY_TEXT = 0xFFFFFFFF;
            SECONDARY_TEXT = 0xFF999999;
            HIGHLIGHT_TEXT = 0xFFCC0000;
            SEARCH_BACKGROUND = 0xFFAAAA66;
            LINK = 0xFF5180b7;
            INBOX_MESSAGE = 0xFF5180b7;
            OUTBOX_MESSAGE = 0xFFAA2323;
            SELECTED_MESSAGE = 0xFF444444;
            STATUS_MESSAGE = 0xFF239923;
            STATUS_AWAY = 0xFF22bcef;
            STATUS_XA = 0xFF3c788c;
            STATUS_DND = 0xFFee0000;
            STATUS_CHAT = 0xFF008e00;
            ROLE_MODERATOR = 0xFFFF8800;
            ROLE_VISITOR = 0xFF777777;
            currentTheme = "Dark";
        } else {
            try {
                XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                parser.setInput(new FileReader(Constants.PATH_COLORS + theme));

                boolean end = false;
                while(!end) {
                    int eventType = parser.next();
                    if (eventType == XmlPullParser.START_TAG) {
                        if (parser.getName().equals("theme")) {
                            isLight = Boolean.valueOf(parser.getAttributeValue("", "isLight"));
                            do {
                                eventType = parser.next();
                                if (eventType == XmlPullParser.START_TAG && parser.getName().equals("color")) {
                                    String name = parser.getAttributeValue("", "name");
                                    int parserDepth = parser.getDepth();
                                    while (!(parser.next() == XmlPullParser.END_TAG && parser.getDepth() == parserDepth)) {
                                        String text = parser.getText();
                                        Long i = Long.parseLong(text, 16);
                                        int color = i.intValue();
                                        if (name.equals("ACCOUNT_BACKGROUND")) {
                                            ACCOUNT_BACKGROUND = color;
                                        } else if (name.equals("BACKGROUND")) {
                                            BACKGROUND = color;
                                        } else if (name.equals("GROUP_BACKGROUND")) {
                                            GROUP_BACKGROUND = color;
                                        } else if (name.equals("ENTRY_BACKGROUND")) {
                                            ENTRY_BACKGROUND = color;
                                        } else if (name.equals("SEARCH_BACKGROUND")) {
                                            SEARCH_BACKGROUND = color;
                                        } else if (name.equals("PRIMARY_TEXT")) {
                                            PRIMARY_TEXT = color;
                                        } else if (name.equals("SECONDARY_TEXT")) {
                                            SECONDARY_TEXT = color;
                                        } else if (name.equals("LINK")) {
                                            LINK = color;
                                        } else if (name.equals("HIGHLIGHT_TEXT")) {
                                            HIGHLIGHT_TEXT = color;
                                        } else if (name.equals("INBOX_MESSAGE")) {
                                            INBOX_MESSAGE = color;
                                        } else if (name.equals("OUTBOX_MESSAGE")) {
                                            OUTBOX_MESSAGE = color;
                                        } else if (name.equals("STATUS_MESSAGE")) {
                                            STATUS_MESSAGE = color;
                                        } else if (name.equals("SELECTED_MESSAGE")) {
                                            SELECTED_MESSAGE = color;
                                        } else if (name.equals("ROLE_MODERATOR")) {
                                            ROLE_MODERATOR = color;
                                        } else if (name.equals("ROLE_VISITOR")) {
                                            ROLE_VISITOR = color;
                                        } else if (name.equals("STATUS_CHAT")) {
                                            STATUS_CHAT = color;
                                        } else if (name.equals("STATUS_DND")) {
                                            STATUS_DND = color;
                                        } else if (name.equals("STATUS_XA")) {
                                            STATUS_XA = color;
                                        } else if (name.equals("STATUS_AWAY")) {
                                            STATUS_AWAY = color;
                                        }
                                    }
                                }
                            }
                            while (eventType != XmlPullParser.END_TAG);
                        }
                    } else if (eventType == XmlPullParser.END_DOCUMENT) {
                        end = true;
                    }
                }
            } catch (Exception e) {
                Log.e("COLORS", e.getLocalizedMessage());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("ColorTheme", "Light");
                editor.commit();
                updateColors(context);
            }
        }
    }
}
