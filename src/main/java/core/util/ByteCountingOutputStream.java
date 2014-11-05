package core.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream that counts the bytes written to it.
 *
 * @author Sebastian Lindholm
 */
public class ByteCountingOutputStream extends OutputStream {

    private long byteCount = 0;
    private OutputStream stream;

    public ByteCountingOutputStream(OutputStream stream) {
        this.stream = stream;
    }

    @Override
    public void write(int b) throws IOException {
        byteCount++;
        stream.write(b);
    }

    public long getByteCount() {
        return byteCount;
    }

    public void resetByteCount() {
        byteCount = 0;
    }
}
