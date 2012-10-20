package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

public class ReplaceExtension implements PacketExtension {
	private String id;

    public ReplaceExtension(String id) {
        this.id = id;
    }

    public String getElementName() {
        return "replace";
    }

    public String getNamespace() {
        return "urn:xmpp:message-correct:0";
    }
    
    public String getId() {
    	return id;
    }

    public String toXML() {
        return "<" + getElementName() + " id=\"" + getId() + "\" xmlns=\"" + getNamespace() + "\" />";
    }

    public static class Provider implements PacketExtensionProvider {
        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        	String rid = parser.getAttributeValue(null, "id");
            return new ReplaceExtension(rid);
        }
    }
}
