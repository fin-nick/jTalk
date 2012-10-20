package org.jivesoftware.smackx.pubsub.util;

public class XmlUtils {
	static public void appendAttribute(StringBuilder builder, String att, String value) {
		builder.append(" ");
		builder.append(att);
		builder.append("='");
		builder.append(value);
		builder.append("'");
	}
}
