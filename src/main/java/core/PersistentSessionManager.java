package core;

import core.storage.FileSessionStorage;
import core.storage.SessionData;
import core.storage.SessionStorage;
import core.storage.compressor.NoCompressor;
import core.storage.compressor.StreamCompressor;
import core.storage.serializer.JavaSerializer;
import core.storage.serializer.SessionSerializer;
import core.util.ClassLoadingFactory;
import core.util.PingPongLogger;
import org.eclipse.jetty.util.thread.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Class that extends the BasicSessionManager
 * with support for session persistence.
 *
 * @author Sebastian Lindholm
 */
public class PersistentSessionManager extends BasicSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(PersistentSessionManager.class);

    private static final String SESSION_STORAGE_PARAM = "session-storage";
    private static final String DEFAULT_SESSION_STORAGE = FileSessionStorage.class.getName();

    private static final String SERIALIZER_PARAM = "session-serializer";
    private static final String DEFAULT_SERIALIZER = JavaSerializer.class.getName();

    private static final String STREAM_COMPRESSOR_PARAM = "session-compressor";
    private static final String DEFAULT_STREAM_COMPRESSOR = NoCompressor.class.getName();

    private static final String PERSIST_PERIOD_PARAM = "session-persist-period";
    private static final int DEFAULT_PERSIST_PERIOD = 20;

    private long persistPeriod = 0;
    private Scheduler.Task persistTask;
    private SessionStorage storage = null;


    /**
     * Get a session from the manager.
     */
    @Override
    public BasicSession getSession(String id) {
        BasicSession session = super.getSession(id);

        if (session == null && storage != null) {
            logger.debug("Session with id {} not found in local memory. Checking remote session storage.", id);
            SessionData in = storage.load(id);
            if (in != null) {
                session = loadSession(in);
                logger.debug("Found session {} in remote storage.", id);
            } else {
                logger.debug("Could not find the session {} in remote storage.", id);
            }
        }

        return session;
    }

    /**
     * Periodically saves sessions.
     */
    protected void persist() {
        logger.info("Running persist.");

        Set<BasicSession> sessions = super.getSessionSet();
        if (storage != null) {
            for (BasicSession session : sessions) {
                if (session.isValid())
                    storage.store(session.getSessionData());
            }
        } else {
            logger.warn("No session storage defined.");
        }

        logger.info("Persist completed.");
    }

    /**
     * Executed on server startup.
     */
    @Override
    public void doStart() throws Exception {
        super.doStart();

        logger.info("Starting {}.", PersistentSessionManager.class.getName());

        String persistPeriodValue = this.getContext().getInitParameter(PERSIST_PERIOD_PARAM);
        int persistPeriod;
        if (persistPeriodValue != null) {
            try {
                persistPeriod = Integer.parseInt(persistPeriodValue);
                logger.debug("Found parameter {} with value {}.", PERSIST_PERIOD_PARAM, persistPeriod);
            } catch (NumberFormatException e) {
                persistPeriod = DEFAULT_PERSIST_PERIOD;
                logger.warn("Unable to parse {}: {}. Defaulting to {}.", PERSIST_PERIOD_PARAM, persistPeriodValue, DEFAULT_PERSIST_PERIOD);
            }
        } else {
            persistPeriod = DEFAULT_PERSIST_PERIOD;
            logger.debug("No {} defined, defaulting to {}.", PERSIST_PERIOD_PARAM, DEFAULT_PERSIST_PERIOD);
        }

        synchronized (this) {
            setPersistPeriod(persistPeriod);
        }

        String sessionStorageValue = this.getContext().getInitParameter(SESSION_STORAGE_PARAM);
        storage = ClassLoadingFactory.tryLoadClass(sessionStorageValue, DEFAULT_SESSION_STORAGE, SessionStorage.class);
        //storage = new PingPongLogger(storage);

        String serializerValue = this.getContext().getInitParameter(SERIALIZER_PARAM);
        storage.setSerializer(ClassLoadingFactory.tryLoadClass(serializerValue, DEFAULT_SERIALIZER, SessionSerializer.class));

        String compressorValue = this.getContext().getInitParameter(STREAM_COMPRESSOR_PARAM);
        storage.setCompressor(ClassLoadingFactory.tryLoadClass(compressorValue, DEFAULT_STREAM_COMPRESSOR, StreamCompressor.class));

        logger.info("Started {}.", PersistentSessionManager.class.getName());
    }


    /**
     * Executed on server stop.
     */
    @Override
    public void doStop() throws Exception {
        logger.info("Stopping {}.", PersistentSessionManager.class.getName());

        synchronized (this) {
            if (persistTask != null) {
                persistTask.cancel();
            }
        }

        logger.info("Stopped {}.", PersistentSessionManager.class.getName());

        super.doStop();
    }

    /**
     * Gets the period at which persist is called.
     *
     * @return The period in seconds.
     */
    public int getPersistPeriod() {
        if (persistPeriod <= 0)
            return 0;
        else
            return (int) (persistPeriod / 1000);
    }

    /**
     * Sets the period at which persist will be called.
     *
     * @param seconds The period in seconds.
     */
    public void setPersistPeriod(int seconds) {
        long old_period = persistPeriod;
        long period = seconds * 1000L;
        if (period < 0)
            period = 0;

        persistPeriod = period;

        if (scheduler != null && (period != old_period || persistTask == null)) {
            synchronized (this) {
                if (persistTask != null) {
                    logger.info("Cancelling the running persistor instance.");
                    persistTask.cancel();
                    persistTask = null;
                }
                if (persistPeriod > 0) {
                    logger.info("Creating a new persistor with a period of {} seconds.", period / 1000L);
                    persistTask = scheduler.schedule(new Persistor(), persistPeriod, TimeUnit.MILLISECONDS);
                } else {
                    logger.info("Session persistence disabled.");
                }
            }
        }
    }

    /**
     * Restores a BasicSession from a SessionData object.
     *
     * @param data The SessionData object to readSessionData from.
     * @return The BasicSession restored from the data object.
     */
    protected BasicSession loadSession(SessionData data) {

        logger.debug("Loading session from SessionData object with id {}.", data.getClusterId());

        long accessed = System.currentTimeMillis(); // Substitute old access time with new
        BasicSession session = new BasicSession(this, data.getCreated(), accessed, data.getClusterId());
        session.setRequests(data.getRequests());
        session.setMaxInactiveInterval(data.getMaxIdle());

        for (String key : data.getAttributes().keySet()) {
            session.setAttribute(key, data.getAttributes().get(key));
        }

        synchronized (this) {
            super.addSession(session, false);
            session.didActivate();
        }

        return session;
    }

    protected SessionStorage getSessionStorage() {
        return storage;
    }

    /**
     * Class that runs in a separate thread, calling persist periodically.
     *
     * @author Sebastian Lindholm
     */
    protected class Persistor implements Runnable {
        public void run() {
            try {
                persist();
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
            } finally {
                if (scheduler != null && scheduler.isRunning())
                    scheduler.schedule(this, persistPeriod, TimeUnit.MILLISECONDS);
            }
        }
    }
}
