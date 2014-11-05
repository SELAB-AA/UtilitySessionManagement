package core.storage.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.esotericsoftware.kryo.io.UnsafeOutput;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * SessionSerializer based on the Kryo serialization library.
 * This version uses sun.misc.Unsafe based IO.
 * (https://github.com/EsotericSoftware/kryo)
 *
 * @author Sebastian Lindholm
 */
public class KryoUnsafeSerializer extends KryoSerializer {

    @Override
    protected Kryo getKryo() {
        Kryo kryo = super.getKryo();
        kryo.setAsmEnabled(false);
        return kryo;
    }

    @Override
    protected Output getOutput(OutputStream out) {
        Output output = localOutput.get();
        if (output == null) {
            output = new UnsafeOutput(out, BUFFER_SIZE);
            localOutput.set(output);
        } else {
            output.setOutputStream(out);
        }

        return output;
    }

    @Override
    protected Input getInput(InputStream in) {
        Input input = localInput.get();
        if (input == null) {
            input = new UnsafeInput(in, BUFFER_SIZE);
            localInput.set(input);
        } else {
            input.setInputStream(in);
        }

        return input;
    }

}
