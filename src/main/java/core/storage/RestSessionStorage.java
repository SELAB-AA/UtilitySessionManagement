package core.storage;

import core.util.ByteCountingInputStream;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves sessions in a RESTfullish web service.
 * This SessionStorage is intended for benchmarking purposes.
 *
 * @author Sebastian Lindholm
 */
public class RestSessionStorage extends AbstractSessionStorage {

    private static final Logger logger = LoggerFactory.getLogger(RestSessionStorage.class);
    private static final Logger bandwidthLogger = LoggerFactory.getLogger("bandwidth");
    private static final Logger perf4jLogger = LoggerFactory.getLogger("org.perf4j.TimingLogger");
    private static final String address = "http://cloud2:8080";
    private static final int BUFFER_SIZE = 16 * 1024;

    private WebTarget resource;


    public RestSessionStorage() {
        ClientConfig config = new ClientConfig();

        config.property(ClientProperties.CONNECT_TIMEOUT, 3000);
        config.property(ClientProperties.READ_TIMEOUT, 5000);

        Client client = ClientBuilder.newClient(config);
        resource = client.target(address);
    }

    @Override
    public boolean store(SessionData session) {
        boolean success = false;

        try (ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE)) {
            StopWatch serializaion = new Slf4JStopWatch("SER", perf4jLogger);
            getSerializer().writeSessionData(session, getCompressor().compress(out));
            serializaion.stop();

            byte[] data = out.toByteArray();
            InputStream in = new ByteArrayInputStream(data);

            StopWatch store = new StopWatch("STORE");
            WebTarget target = resource.path("sessions").path(session.getClusterId());
            Response response = target.request(MediaType.TEXT_PLAIN).put(Entity.entity(in, MediaType.APPLICATION_OCTET_STREAM));

            if (response.getStatus() == 200) {
                store.stop("STORE_OK");
                bandwidthLogger.info(", STORE, {}, {}", data.length, serializaion.getElapsedTime());
                success = true;
            } else {
                store.stop("STORE_FAIL");
            }

            in.close();

            response.close();

        } catch (IOException e) {
            logger.warn("Error storing session.", e);
        }

        return success;
    }

    @Override
    public void remove(String id) {
        WebTarget target = resource.path("sessions").path(id);
        Response response = target.request().delete();
        response.close();
    }

    @Override
    public SessionData load(String id) {
        StopWatch load = new StopWatch();
        WebTarget target = resource.path("sessions").path(id);
        Response response = target.request(MediaType.APPLICATION_OCTET_STREAM).get();

        SessionData data = null;
        if (response.getStatus() == 200) {
            InputStream responseData = response.readEntity(InputStream.class);
            try (
                    ByteCountingInputStream bytes = new ByteCountingInputStream(responseData);
                    InputStream in = getCompressor().decompress(bytes)
            ) {
                StopWatch deserialization = new Slf4JStopWatch("DESER", perf4jLogger);
                data = getSerializer().readSessionData(in);
                load.stop();
                if (data != null) {
                    deserialization.stop("DESER_DATA");
                    bandwidthLogger.info(", LOAD, {}, {}", bytes.getByteCount(), load.getElapsedTime());
                } else {
                    deserialization.stop("DESER_NODATA");
                }
            } catch (IOException e) {
                logger.warn("Failure while loading session!", e);
            }
        }

        return data;
    }

    @Override
    public List<StoredSession> stored() {
        WebTarget target = resource.path("sessions").path("stored");
        Response response = target.request(MediaType.APPLICATION_OCTET_STREAM).get();
        InputStream responseData = response.readEntity(InputStream.class);
        List<StoredSession> data = null;

        if (responseData != null) {
            try (
                    InputStream in = getCompressor().decompress(responseData);
            ) {
                data = getSerializer().readStoredSessions(in);
                in.close();
            } catch (IOException e) {
                logger.warn("Error while reading stored sessions!", e);
            }
        } else {
            data = new ArrayList<>();
        }

        return data;
    }

    @Override
    public long capacity() {
        WebTarget target = resource.path("sessions").path("capacity");
        Response response = target.request(MediaType.TEXT_PLAIN).get();
        return response.readEntity(Long.class);
    }
}
