package core.storage.compressor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * StreamCompressor based on a snappy-java (https://github.com/xerial/snappy-java).
 *
 * @author Sebastian Lindholm
 */
public class SnappyCompressor implements StreamCompressor {

    private static final Logger logger = LoggerFactory.getLogger(SnappyCompressor.class);

    @Override
    public OutputStream compress(OutputStream out) {
        try {
            return new SnappyOutputStream(out);
        } catch (IOException e) {
            logger.warn("Error initialising SnappyOutputStream.", e);
            return out;
        }
    }

    @Override
    public InputStream decompress(InputStream in) {
        try {
            return new SnappyInputStream(in);
        } catch (IOException e) {
            logger.warn("Error initialising SnappyInputStream.", e);
            return in;
        }
    }

}
