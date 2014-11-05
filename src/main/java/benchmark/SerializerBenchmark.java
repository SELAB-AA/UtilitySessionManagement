package benchmark;

import core.storage.SessionData;
import core.storage.serializer.*;
import org.perf4j.StopWatch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SerializerBenchmark {

    private static final int ITERATIONS = 100;
    private static final Payload payload = new WordsMegaPayload();

    public static void main(String[] args) {
        SerializerBenchmark test = new SerializerBenchmark();
        try {

            System.gc();
            test.testKryo();
            System.gc();
            test.testKryoUnsafe();
            System.gc();
            test.testFast();
            System.gc();
            test.testJava();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testKryo() throws IOException {
        serialize(new KryoSerializer());
    }

    public void testJava() throws IOException {
        serialize(new JavaSerializer());
    }

    public void testKryoUnsafe() throws IOException {
        serialize(new KryoUnsafeSerializer());
    }

    public void testFast() throws IOException {
        serialize(new FastSerializer());
    }

    private void serialize(SessionSerializer serializer) throws IOException {
        SessionData[] objects = new SessionData[ITERATIONS];

        for (int i = 0; i < ITERATIONS; i++) {
            SessionData data = new SessionData();
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("data", payload.getPayload());
            data.setAttributes(attributes);
            data.setClusterId("abc");
            objects[i] = data;
        }

        long serSum = 0;
        long deserSum = 0;

        for (int i = 0; i < ITERATIONS; i++) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(16 * 1024);
            StopWatch ser = new StopWatch();
            serializer.writeSessionData(objects[i], out);
            ser.stop();

            out.flush();
            InputStream in = new ByteArrayInputStream(out.toByteArray());
            out.close();

            StopWatch deser = new StopWatch();
            SessionData data = serializer.readSessionData(in);
            deser.stop();

            in.close();

            serSum += ser.getElapsedTime();
            deserSum += deser.getElapsedTime();
        }

        System.out.println(serializer.getClass().getSimpleName() + " serialization: " + serSum + " ms.");
        System.out.println(serializer.getClass().getSimpleName() + " deserialization: " + deserSum + " ms.");
    }
}
