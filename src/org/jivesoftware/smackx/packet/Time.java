package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.IQ;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Time extends IQ {

    private static SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private String utc = null;
    private String tzo = null;

    /**
     * Creates a new Time instance with empty values for all fields.
     */
    public Time() {
        
    }

    /**
     * Creates a new Time instance using the specified calendar instance as
     * the time value to send.
     *
     * @param cal the time value.
     */
    public Time(Calendar cal) {
        TimeZone timeZone = cal.getTimeZone();
        String z = new SimpleDateFormat("Z").format(new Date());
        tzo = z.substring(0,3) + ":" + z.substring(3);
        utc = utcFormat.format(new Date(cal.getTimeInMillis() - timeZone.getOffset(cal.getTimeInMillis())));
    }

    /**
     * Returns the local time or <tt>null</tt> if the time hasn't been set.
     *
     * @return the lcocal time.
     */
    public Date getTime() {
        if (utc == null) {
            return null;
        }
        Date date = null;
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(utcFormat.parse(utc).getTime() + cal.getTimeZone().getOffset(cal.getTimeInMillis())));
            date = cal.getTime();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * Sets the time using the local time.
     *
     * @param time the current local time.
     */
    public void setTime(Date time) {
        // Convert local time to UTC time.
        utc = utcFormat.format(new Date(time.getTime() - TimeZone.getDefault().getOffset(time.getTime())));
    }

    /**
     * Returns the time as a UTC formatted String using the format CCYYMMDDThh:mm:ss.
     *
     * @return the time as a UTC formatted String.
     */
    public String getUtc() {
        return utc;
    }

    /**
     * Sets the time using UTC formatted String in the format CCYYMMDDThh:mm:ss.
     *
     * @param utc the time using a formatted String.
     */
    public void setUtc(String utc) {
        this.utc = utc;

    }

    /**
     * Returns the time zone.
     *
     * @return the time zone.
     */
    public String getTzo() {
        return tzo;
    }

    /**
     * Sets the time zone.
     *
     * @param tz the time zone.
     */
    public void setTzo(String tzo) {
        this.tzo = tzo;
    }

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<time xmlns=\"urn:xmpp:time\">");
        if (utc != null) {
            buf.append("<utc>").append(utc).append("</utc>");
        }
        if (tzo != null) {
            buf.append("<tzo>").append(tzo).append("</tzo>");
        }
        buf.append("</time>");
        return buf.toString();
    }
}