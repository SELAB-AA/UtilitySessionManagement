package core.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream that counts the bytes readSessionData from it.
 *
 * @author Sebastian Lindholm
 */
public class ByteCountingInputStream extends InputStream {

    private long byteCount = 0;
    private InputStream stream;

    public ByteCountingInputStream(InputStream stream) {
        this.stream = stream;
    }

    @Override
    public int read() throws IOException {
        int buffer = stream.read();
        if (buffer != -1)
            byteCount++;
        return buffer;
    }

    public long getByteCount() {
        return byteCount;
    }

    public void resetByteCount() {
        byteCount = 0;
    }
}
