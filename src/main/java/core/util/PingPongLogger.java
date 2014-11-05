package core.util;

import core.storage.SessionData;
import core.storage.SessionStorage;
import core.storage.StoredSession;
import core.storage.compressor.StreamCompressor;
import core.storage.serializer.SessionSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PingPongLogger implements SessionStorage {

    private static final Logger logger = LoggerFactory.getLogger("pingpong");

    private final SessionStorage storage;

    public PingPongLogger(SessionStorage storage) {
        this.storage = storage;
    }

    @Override
    public boolean store(SessionData session) {
        logger.info(", {}, STORE", session.getClusterId());
        return storage.store(session);
    }

    @Override
    public void remove(String id) {
        storage.remove(id);
    }

    @Override
    public SessionData load(String id) {
        SessionData data = storage.load(id);

        if (data != null)
            logger.info(", {}, LOAD", id);

        return data;
    }

    @Override
    public List<StoredSession> stored() {
        return storage.stored();
    }

    @Override
    public long capacity() {
        return storage.capacity();
    }

    @Override
    public SessionSerializer getSerializer() {
        return storage.getSerializer();
    }

    @Override
    public void setSerializer(SessionSerializer serializer) {
        storage.setSerializer(serializer);
    }

    @Override
    public StreamCompressor getCompressor() {
        return storage.getCompressor();
    }

    @Override
    public void setCompressor(StreamCompressor compressor) {
        storage.setCompressor(compressor);
    }
}
