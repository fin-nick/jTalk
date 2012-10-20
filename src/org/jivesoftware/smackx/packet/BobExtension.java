package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;

public class BobExtension implements PacketExtension {
	private String data;
	private String cid;

	public BobExtension(String cid, String data) { 
		this.cid  = cid;
		this.data = data;
	}
	
	public void setData(byte[] data) {
		this.data = StringUtils.encodeBase64(data);
	}
	
	public byte[] getData() { 
		if (data != null) {
			return StringUtils.decodeBase64(data); 
		}
		else return null;
	}
	public String getCid() { return cid; }
	public String getElementName() { return "data"; }
	public String getNamespace() { return "urn:xmpp:bob"; }

	public String toXML() {
		StringBuilder buf = new StringBuilder();
		buf.append("<data xmlns=\"urn:xmpp:bob\">");
		if (getData() != null) buf.append(getData());
		buf.append("</data>");
		return buf.toString();
	}
	
	public static class Provider implements PacketExtensionProvider {
        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        	String cid = parser.getAttributeValue("", "cid");
        	String data = parser.nextText();
            return new BobExtension(cid, data);
        }
    }
}
