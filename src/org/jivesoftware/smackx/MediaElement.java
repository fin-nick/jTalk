package org.jivesoftware.smackx;

public class MediaElement {
	private String type;
	private String id;
	
	public MediaElement(String type, String id) {
		this.type = type;
		this.id = id;
	}
	
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	
	public String getType() { return type; }
	public void setType(String type) { this.type = type; }
	
	public String toXML() {
		StringBuilder buf = new StringBuilder();
        buf.append("<media xmlns= \"urn:xmpp:media-element\">");
        buf.append("<uri");
        if (getType() != null) buf.append(" type=\"" + getType() + "\"");
        buf.append(">");
        if (getId() != null) buf.append(getId());
        buf.append("</uri></media>");
        return buf.toString();
	}
}
