package org.jivesoftware.smackx.packet;

import org.jivesoftware.smackx.Receipt;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

public class ReceiptExtension implements PacketExtension {

    private Receipt receipt;
    private String id;

    public ReceiptExtension(Receipt receipt, String id) {
        this.receipt = receipt;
        this.id = id;
    }

    public String getElementName() {
        return receipt.name();
    }

    public String getNamespace() {
        return "urn:xmpp:receipts";
    }

    public String getId() {
        return id;
    }

    public String toXML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<" + getElementName() + " xmlns=\"" + getNamespace() + "\" ");
        if (id != null && !id.isEmpty()) sb.append("id=\"" + id + "\"");
        sb.append("/>");
        return sb.toString();
    }

    public static class Provider implements PacketExtensionProvider {

        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            Receipt receipt = Receipt.received;
            receipt = Receipt.valueOf(parser.getName().toLowerCase());
            String id = "";
            id = parser.getAttributeValue(null, "id");
            return new ReceiptExtension(receipt, id);
        }
    }
}
