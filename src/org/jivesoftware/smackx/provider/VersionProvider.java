package org.jivesoftware.smackx.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.packet.Version;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class VersionProvider implements IQProvider {
	private String os;
	private String ver;
	private String soft;
	private int state;

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        try {
            int event = parser.getEventType();
            while (true) {
                switch (event) {
                    case XmlPullParser.TEXT:
                    	String text = parser.getText();
                    	if (state == 1) soft = text;
                    	else if (state == 2) os = text;
                    	else if (state == 3) ver = text; 
                        break;
                    case XmlPullParser.START_TAG:
                    	String name = parser.getName();
                    	if (name.equals("name")) state = 1;
                    	else if (name.equals("os")) state = 2;
                    	else if (name.equals("version")) state = 3;
                        break;
                    case XmlPullParser.END_TAG:
                    	state = 0;
                        break;
                    default:
                }

                if (event == XmlPullParser.END_TAG && "query".equals(parser.getName())) break;

                event = parser.next();
            }
        }
        catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        Version version = new Version();
        version.setName(soft);
        version.setOs(os);
        version.setVersion(ver);
        return version;
    }
}
