package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.xmlpull.v1.XmlPullParser;

/**
* The DiscoverInfoProvider parses Service Discovery information packets.
*
* @author Gaston Dombiak
*/
public class DiscoverInfoProvider implements IQProvider {

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        DiscoverInfo discoverInfo = new DiscoverInfo();
        boolean done = false;
        DiscoverInfo.Feature feature = null;
        DiscoverInfo.Identity identity = null;
        String category = "";
        String name = "";
        String type = "";
        String variable = "";
        discoverInfo.setNode(parser.getAttributeValue("", "node"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("identity")) {
                    // Initialize the variables from the parsed XML
                    category = parser.getAttributeValue("", "category");
                    name = parser.getAttributeValue("", "name");
                    type = parser.getAttributeValue("", "type");
                }
                else if (parser.getName().equals("feature")) {
                    // Initialize the variables from the parsed XML
                    variable = parser.getAttributeValue("", "var");
                }
                // Otherwise, it must be a packet extension.
                else {
                    discoverInfo.addExtension(PacketParserUtils.parsePacketExtension(parser
                            .getName(), parser.getNamespace(), parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("identity")) {
                    // Create a new identity and add it to the discovered info.
                    identity = new DiscoverInfo.Identity(category, name);
                    identity.setType(type);
                    discoverInfo.addIdentity(identity);
                }
                if (parser.getName().equals("feature")) {
                    // Create a new feature and add it to the discovered info.
                    discoverInfo.addFeature(variable);
                }
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }
        }

        return discoverInfo;
    }
}