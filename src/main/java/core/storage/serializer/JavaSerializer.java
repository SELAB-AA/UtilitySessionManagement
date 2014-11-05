package core.storage.serializer;

import core.storage.SessionData;
import core.storage.StoredSession;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Session serializer using default java serialization.
 *
 * @author Sebastian Lindholm
 */
public class JavaSerializer implements SessionSerializer {

    @Override
    public List<StoredSession> readStoredSessions(InputStream in) {
        List<StoredSession> objects = new ArrayList<>();

        try (
                ObjectInputStream objectIn = new ObjectInputStream(in)
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
                ObjectInputStream objectIn = new ObjectInputStream(in)
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
    public void writeSessionData(SessionData data, OutputStream out) {
        try (
                ObjectOutputStream objectOut = new ObjectOutputStream(out)
        ) {
            objectOut.writeObject(data);
            objectOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeStoredSessions(List<StoredSession> data, OutputStream out) {
        try (
                ObjectOutputStream objectOut = new ObjectOutputStream(out)
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

}
