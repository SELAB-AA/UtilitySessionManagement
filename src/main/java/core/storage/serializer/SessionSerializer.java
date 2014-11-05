package core.storage.serializer;

import core.storage.SessionData;
import core.storage.StoredSession;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Stream-based serialization interface.
 *
 * @author Sebastian Lindholm
 */
public interface SessionSerializer {

    /**
     * Deserializes a list of StoredSession objects from an InputStream.
     *
     * @param in The InputStream to read from.
     * @return A List containing the StoredSessions read (can be empty).
     */
    public List<StoredSession> readStoredSessions(InputStream in);

    /**
     * Deserializes a SessionData object from an InputStream.
     *
     * @param in The InputStream to read from.
     * @return The SessionData object read.
     */
    public SessionData readSessionData(InputStream in);

    /**
     * Serializes a list of StoredSession objects to an OutputStream.
     *
     * @param data The list to serialize (can be empty).
     * @param out  The OutputStream to write to.
     */
    public void writeStoredSessions(List<StoredSession> data, OutputStream out);

    /**
     * Serializes a SessionData object to an OutputStream.
     *
     * @param data The SessionData object to serialize.
     * @param out  The OutputStream to write to.
     */
    public void writeSessionData(SessionData data, OutputStream out);

}
