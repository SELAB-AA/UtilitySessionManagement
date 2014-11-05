package core.storage.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.vaadin.event.ListenerMethod;
import com.vaadin.ui.ConnectorTracker;
import core.storage.SessionData;
import core.storage.StoredSession;
import org.objenesis.strategy.SerializingInstantiatorStrategy;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SessionSerializer based on the Kryo serialization library.
 * (https://github.com/EsotericSoftware/kryo)
 *
 * @author Sebastian Lindholm
 */
public class KryoSerializer implements SessionSerializer {

    protected static final int BUFFER_SIZE = 16 * 1024;

    protected static ThreadLocal<Kryo> localKryo = new ThreadLocal<>();
    protected static ThreadLocal<Output> localOutput = new ThreadLocal<>();
    protected static ThreadLocal<Input> localInput = new ThreadLocal<>();

    protected Kryo getKryo() {
        Kryo kryo = localKryo.get();
        if (kryo == null) {
            kryo = new Kryo();
            ((Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy()).setFallbackInstantiatorStrategy(new SerializingInstantiatorStrategy());

            // ASM backend disabled due to apparently being incompatible with the Vaadin framework.
            //kryo.setAsmEnabled(true);

            kryo.register(StoredSession.class);
            kryo.register(SessionData.class);
            Serializer vaadinSerializer = new JavaSerializer();
            kryo.register(ReentrantLock.class, vaadinSerializer);
            kryo.register(ListenerMethod.class, vaadinSerializer);
            kryo.register(ConnectorTracker.class, vaadinSerializer);

            localKryo.set(kryo);
        }

        return kryo;
    }

    protected Output getOutput(OutputStream out) {
        Output output = localOutput.get();
        if (output == null) {
            output = new Output(out, BUFFER_SIZE);
            localOutput.set(output);
        } else {
            output.setOutputStream(out);
        }

        return output;
    }

    protected Input getInput(InputStream in) {
        Input input = localInput.get();
        if (input == null) {
            input = new Input(in, BUFFER_SIZE);
            localInput.set(input);
        } else {
            input.setInputStream(in);
        }

        return input;
    }

    @Override
    public List<StoredSession> readStoredSessions(InputStream in) {
        Kryo kryo = getKryo();
        List<StoredSession> objects = new ArrayList<>();

        try (
                Input input = getInput(in)
        ) {
            int items = input.readInt();
            for (int i = 0; i < items; i++) {
                StoredSession object = kryo.readObject(input, StoredSession.class);
                if (object != null)
                    objects.add(object);
            }
        } catch (KryoException e) {
            e.printStackTrace();
        }

        return objects;
    }

    @Override
    public SessionData readSessionData(InputStream in) {
        Kryo kryo = getKryo();
        SessionData data = null;
        try (
                Input input = getInput(in)
        ) {
            data = kryo.readObject(input, SessionData.class);
        } catch (KryoException e) {
            e.printStackTrace();
        }

        return data;
    }

    @Override
    public void writeStoredSessions(List<StoredSession> data, OutputStream out) {
        Kryo kryo = getKryo();

        try (
                Output output = getOutput(out)
        ) {
            output.writeInt(data.size());

            for (StoredSession s : data) {
                kryo.writeObject(output, s);
            }

            output.flush();
        } catch (KryoException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeSessionData(SessionData data, OutputStream out) {
        Kryo kryo = getKryo();
        try (
                Output output = getOutput(out);
        ) {
            kryo.writeObject(output, data);
            output.flush();
        } catch (KryoException e) {
            e.printStackTrace();
        }
    }
}
