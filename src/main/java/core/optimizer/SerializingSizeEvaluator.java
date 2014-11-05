package core.optimizer;

import core.BasicSession;
import core.storage.SessionStorage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class SerializingSizeEvaluator implements SizeEvaluator {

    @Override
    public long evaluateLocal(BasicSession session) {
        long size;

        try (
                ByteArrayOutputStream baos = new ByteArrayOutputStream(51200);
                ObjectOutputStream out = new ObjectOutputStream(baos)
        ) {
            out.writeObject(session.getSessionData());
            size = baos.size();
        } catch (IOException e) {
            size = 0;
        }

        return size;
    }

    @Override
    public long evaluateRemote(BasicSession session, SessionStorage storage) {
        long size;

        try (
                ByteArrayOutputStream baos = new ByteArrayOutputStream(51200);
        ) {
            storage.getSerializer().writeSessionData(session.getSessionData(), storage.getCompressor().compress(baos));
            size = baos.size();
        } catch (IOException e) {
            size = 0;
        }

        return size;
    }
}
