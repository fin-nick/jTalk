package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

public class PingExtension implements PacketExtension {

    public PingExtension() {
    }

    public String toXML() {
        return "<ping xmlns=\"urn:xmpp:ping\" />";
    }
    
	public String getElementName() {
		return "ping";
	}

	public String getNamespace() {
		return "urn:xmpp:ping";
	}

    public static class Provider implements PacketExtensionProvider {

        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            return new PingExtension();
        }
    }
}
