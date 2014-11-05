package core.optimizer;

import core.storage.SessionStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class made to hold relevant optimization data.
 *
 * @author Sebastian Lindholm
 */
public class OptimizationData {

    private final Map<SessionStorage, StorageProperties> storages = new HashMap<>();
    private final Map<String, SessionProperties> sessions = new HashMap<>();

    public int optimizerPeriod = 0;
    public long localCapacity = 0;
    public double localMTTF = 0;

    public StorageProperties getStorageProperties(SessionStorage storage) {
        return storages.get(storage);
    }

    public void putStorageProperties(SessionStorage storage, StorageProperties properties) {
        storages.put(storage, properties);
    }

    public boolean containsStorage(SessionStorage storage) {
        return storages.containsKey(storage);
    }

    public Set<SessionStorage> getStorages() {
        return storages.keySet();
    }

    public SessionProperties getSessionProperties(String sessionId) {
        return sessions.get(sessionId);
    }

    public void putSessionProperties(String sessionId, SessionProperties properties) {
        sessions.put(sessionId, properties);
    }

    public boolean containsSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    public void clearSessions() {
        sessions.clear();
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public Set<String> getSessions() {
        return sessions.keySet();
    }

}
