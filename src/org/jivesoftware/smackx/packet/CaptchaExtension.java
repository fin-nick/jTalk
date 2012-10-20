package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.xmlpull.v1.XmlPullParser;

public class CaptchaExtension implements PacketExtension {
	private DataForm form;
	
	public CaptchaExtension(DataForm form) {
		this.form = form;
	}
	
	public DataForm getForm() { return this.form; }
	public String getElementName() { return "captcha"; }
	public String getNamespace() { return "urn:xmpp:captcha"; }

	public String toXML() {
		StringBuilder buf = new StringBuilder();
		buf.append("<captcha xmlns=\"urn:xmpp:captcha\">");
		if (getForm() != null) buf.append(getForm().toXML());
		buf.append("</captcha>");
		return buf.toString();
	}
	
	public static class Provider implements PacketExtensionProvider {
        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        	DataForm form = (DataForm) new DataFormProvider().parseExtension(parser);
            return new CaptchaExtension(form);
        }
    }

}
