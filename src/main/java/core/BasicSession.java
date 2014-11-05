package core;

import core.storage.SessionData;
import org.eclipse.jetty.server.session.AbstractSession;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Session class used by the BasicSessionManager.
 *
 * @author Sebastian Lindholm
 */
public class BasicSession extends AbstractSession {

    private Set<SessionListener> listeners = new HashSet<>();

    protected BasicSession(BasicSessionManager utilitySessionManager, HttpServletRequest request) {
        super(utilitySessionManager, request);
        if (utilitySessionManager instanceof SessionListener)
            listeners.add((SessionListener) utilitySessionManager);
    }

    protected BasicSession(BasicSessionManager utilitySessionManager, long created, long accessed, String clusterId) {
        super(utilitySessionManager, created, accessed, clusterId);
        if (utilitySessionManager instanceof SessionListener)
            listeners.add((SessionListener) utilitySessionManager);
    }

    /**
     * Calls timeout(), invalidating the session and removing it from (local) memory.
     */
    public synchronized void timeoutSession() {
        timeout();
    }

    /**
     * Checks whether this BasicSession has
     * exceeded its timeout limit, but does not take any action.
     *
     * @return true if the session is idle,
     * else false.
     */
    public synchronized boolean isScavengable() {
        long now = System.currentTimeMillis();
        long idleTime = getMaxInactiveInterval() * 1000L;

        return (idleTime > 0 && getAccessed() + idleTime < now);
    }

    /**
     * Create a new object representing the data to be serialized in this session.
     *
     * @return SessionData object based on this session.
     */
    public synchronized SessionData getSessionData() {
        SessionData data = new SessionData();

        data.setClusterId(getClusterId());
        data.setCreated(getCreationTime());
        data.setRequests(getRequests());
        data.setMaxIdle(getMaxInactiveInterval());
        data.setAttributes(new HashMap<>(this.getAttributeMap()));

        return data;
    }

    /**
     * Adds a listener to this object, that will receive notifications when the session is accessed.
     *
     * @param listener Object implementing the SessionListener interface.
     */
    public void addListener(SessionListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener from this session
     *
     * @param listener Object implementing the SessionListener interface.
     */
    public void removeListener(SessionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Adds a listener notification mechanic to the default access method.
     *
     * @param time The time of access.
     * @return true if the session was successfully accessed.
     */
    @Override
    public boolean access(long time) {
        boolean success = super.access(time);
        if (success) {
            for (SessionListener listener : listeners) {
                listener.sessionAccessed(this);
            }
        }

        return success;
    }

}
