package core.storage;

import java.io.Serializable;

/**
 * Object holding information about a stored session.
 *
 * @author Sebastian Lindholm
 */
public class StoredSession implements Serializable {

    private String sessionId;
    private long remoteSize;

    public StoredSession() {

    }

    public StoredSession(String sessionId, long remoteSize) {
        this.sessionId = sessionId;
        this.remoteSize = remoteSize;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getRemoteSize() {
        return remoteSize;
    }

    public void setRemoteSize(long remoteSize) {
        this.remoteSize = remoteSize;
    }
}
