package core.storage;

import core.storage.compressor.StreamCompressor;
import core.storage.serializer.SessionSerializer;

import java.util.List;

/**
 * Generic interface to a session storage,
 * used by the PersistentSessionManager
 * to store sessions.
 *
 * @author Sebastian Lindholm
 */
public interface SessionStorage {

    /**
     * Stores a given BasicSession.
     *
     * @param session The session to be stored.
     * @return true if the session was successfully saved,
     * else false.
     */
    public boolean store(SessionData session);

    /**
     * Removes a session from this medium.
     *
     * @param id The id of the session to be removed.
     */
    public void remove(String id);

    /**
     * Returns the SessionData object identified by a given id.
     *
     * @param id String identifying a session.
     * @return A SessionData object if the session exists, or null otherwise.
     */
    public SessionData load(String id);

    /**
     * Returns A List of the sessions stored in this medium.
     *
     * @return A List consisting of (sessionId, sessionSize).
     */
    public List<StoredSession> stored();

    /**
     * The amount of usable space on this medium.
     *
     * @return The capacity in bytes.
     */
    public long capacity();

    public SessionSerializer getSerializer();

    public void setSerializer(SessionSerializer serializer);

    public StreamCompressor getCompressor();

    public void setCompressor(StreamCompressor compressor);

}
