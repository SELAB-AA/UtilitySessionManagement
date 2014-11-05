package core.storage.compressor;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Stream-based compression interface.
 *
 * @author Sebastian Lindholm
 */
public interface StreamCompressor {

    /**
     * Returns a compressing OutputStream based on another one.
     *
     * @param out The OutputStream to compress.
     * @return The compressing OutputStream.
     */
    public OutputStream compress(OutputStream out);

    /**
     * Returns a decompressing InputStream based on another one.
     *
     * @param in The InputStream to decompress.
     * @return The decompressing InputStream.
     */
    public InputStream decompress(InputStream in);

}
