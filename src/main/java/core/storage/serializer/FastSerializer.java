package core.storage.serializer;

import core.storage.SessionData;
import core.storage.StoredSession;
import de.ruedigermoeller.serialization.FSTObjectInput;
import de.ruedigermoeller.serialization.FSTObjectOutput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Session serializer based on the FST library.
 * (http://ruedigermoeller.github.io/fast-serialization/)
 *
 * @author Sebastian Lindholm
 */
public class FastSerializer implements SessionSerializer {

    @Override
    public List<StoredSession> readStoredSessions(InputStream in) {
        List<StoredSession> objects = new ArrayList<>();

        try (
                FSTObjectInput objectIn = new FSTObjectInput(in)
        ) {
            int items = objectIn.readInt();

            for (int i = 0; i < items; i++) {
                try {
                    Object object = objectIn.readObject();
                    if (object instanceof StoredSession)
                        objects.add((StoredSession) object);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return objects;
    }

    @Override
    public SessionData readSessionData(InputStream in) {
        SessionData data = null;
        try (
                FSTObjectInput objectIn = new FSTObjectInput(in)
        ) {
            Object object = objectIn.readObject();
            if (object instanceof SessionData)
                data = (SessionData) object;

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return data;
    }

    @Override
    public void writeStoredSessions(List<StoredSession> data, OutputStream out) {
        try (
                FSTObjectOutput objectOut = new FSTObjectOutput(out)
        ) {
            objectOut.writeInt(data.size());

            for (StoredSession session : data) {
                objectOut.writeObject(session);
            }

            objectOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void writeSessionData(SessionData data, OutputStream out) {
        try (
                FSTObjectOutput objectOut = new FSTObjectOutput(out)
        ) {
            objectOut.writeObject(data);
            objectOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
