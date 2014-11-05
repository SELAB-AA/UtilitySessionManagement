package core.storage;

import core.storage.compressor.StreamCompressor;
import core.storage.serializer.SessionSerializer;

/**
 * Convenience class that provides some getters
 * and setters for some SessionStorage attributes.
 *
 * @author Sebastian Lindholm
 */
public abstract class AbstractSessionStorage implements SessionStorage {

    private SessionSerializer serializer;
    private StreamCompressor compressor;

    @Override
    public SessionSerializer getSerializer() {
        return serializer;
    }

    @Override
    public void setSerializer(SessionSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public StreamCompressor getCompressor() {
        return compressor;
    }

    @Override
    public void setCompressor(StreamCompressor compressor) {
        this.compressor = compressor;
    }
}
