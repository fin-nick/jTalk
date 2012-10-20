package org.jivesoftware.smackx.packet;

import org.jivesoftware.smackx.Receipt;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

public class ReceiptExtension implements PacketExtension {

    private Receipt receipt;

    /**
     * Default constructor. The argument provided is the state that the extension will represent.
     *
     * @param state the state that the extension represents.
     */
    public ReceiptExtension(Receipt receipt) {
        this.receipt = receipt;
    }

    public String getElementName() {
        return receipt.name();
    }

    public String getNamespace() {
        return "urn:xmpp:receipts";
    }

    public String toXML() {
        return "<" + getElementName() + " xmlns=\"" + getNamespace() + "\" />";
    }

    public static class Provider implements PacketExtensionProvider {

        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            Receipt receipt;
            try {
                receipt = Receipt.valueOf(parser.getName());
            }
            catch (Exception ex) {
                receipt = Receipt.received;
            }
            return new ReceiptExtension(receipt);
        }
    }
}
