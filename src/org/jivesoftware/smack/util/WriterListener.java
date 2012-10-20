package org.jivesoftware.smack.util;

/**
 * Interface that allows for implementing classes to listen for string writing
 * events. Listeners are registered with ObservableWriter objects.
 *
 * @see ObservableWriter#addWriterListener
 * @see ObservableWriter#removeWriterListener
 * 
 * @author Gaston Dombiak
 */
public interface WriterListener {

    /**
     * Notification that the Writer has written a new string.
     * 
     * @param str the written string
     */
    public abstract void write(String str);

}
