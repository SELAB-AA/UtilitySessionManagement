package core.storage.compressor;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Dummy StreamCompressor, providing no compression at all.
 */
public class NoCompressor implements StreamCompressor {

    @Override
    public OutputStream compress(OutputStream out) {
        return out;
    }

    @Override
    public InputStream decompress(InputStream in) {
        return in;
    }

}
