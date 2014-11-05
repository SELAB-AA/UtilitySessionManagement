package core;

import core.util.CrashGeneratorListener;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.AbstractSession;
import org.eclipse.jetty.server.session.AbstractSessionManager;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * A basic, in-memory session manager implementation
 * based on the standard HashSessionManager.
 *
 * @author Sebastian Lindholm
 */
public class BasicSessionManager extends AbstractSessionManager implements CrashGeneratorListener {

    private static final String SCAVENGE_PERIOD_PARAM = "session-scavenge-period";
    private static final int DEFAULT_SCAVENGE_PERIOD = 30;
    private static final Logger logger = LoggerFactory.getLogger(BasicSessionManager.class);

    private final ConcurrentMap<String, BasicSession> sessions = new ConcurrentHashMap<>();
    protected Scheduler scheduler;
    private long scavengePeriod;
    private Scheduler.Task scavengerTask;
    private boolean doStopScheduler = false;

    public BasicSessionManager() {
        super();
    }

    /**
     * Scavenge takes a snapshot of the sessions currently in memory,
     * iterates through them and removes inactive ones.
     */
    protected void scavenge() {

        logger.info("Running scavenge.");

        if (isStopping() || isStopped())
            return;

        Thread thread = Thread.currentThread();
        ClassLoader oldLoader = thread.getContextClassLoader();

        int scavenged = 0;
        try {
            if (_loader != null) {
                thread.setContextClassLoader(_loader);
            }

            scavenged = scavengeAction();

        } catch (Exception e) {
            logger.warn("Failed to scavenge sessions.", e);
        } finally {
            thread.setContextClassLoader(oldLoader);
        }

        logger.info("Scavenged {} sessions.", scavenged);

    }

    protected int scavengeAction() {
        Set<BasicSession> _sessions = getSessionSet();
        int scavengeCount = 0;
        for (BasicSession session : _sessions) {
            if (session.isScavengable()) {
                logger.debug("Removing inactive session {}", session.getClusterId());
                synchronized (session) {
                    session.timeoutSession();
                    scavengeCount++;
                }
            }
        }
        return scavengeCount;
    }

    @Override
    public void doStart() throws Exception {
        super.doStart();

        logger.info("Starting {}.", BasicSessionManager.class.getName());

        doStopScheduler = false;
        scheduler = getSessionHandler().getServer().getBean(Scheduler.class);

        if (scheduler == null) {
            logger.debug("No server scheduler found, using context scheduler.");
            ServletContext context = ContextHandler.getCurrentContext();

            if (context != null)
                scheduler = (Scheduler) context.getAttribute("org.eclipse.jetty.server.session.timer");
        }

        if (scheduler == null) {
            logger.debug("No context scheduler found, using internal scheduler.");
            doStopScheduler = true;
            scheduler = new ScheduledExecutorScheduler();
            scheduler.start();
        }

        String scavengePeriodValue = this.getContext().getInitParameter(SCAVENGE_PERIOD_PARAM);

        int scavengePeriod;
        if (scavengePeriodValue != null) {
            try {
                scavengePeriod = Integer.parseInt(scavengePeriodValue);
                logger.debug("Found parameter {} with value {}.", SCAVENGE_PERIOD_PARAM, scavengePeriod);
            } catch (NumberFormatException e) {
                logger.warn("Unable to parse {}: {}. Defaulting to {}.", SCAVENGE_PERIOD_PARAM, scavengePeriodValue, DEFAULT_SCAVENGE_PERIOD);
                scavengePeriod = DEFAULT_SCAVENGE_PERIOD;
            }
        } else {
            scavengePeriod = DEFAULT_SCAVENGE_PERIOD;
            logger.debug("No {} defined, defaulting to {}.", SCAVENGE_PERIOD_PARAM, DEFAULT_SCAVENGE_PERIOD);
        }

        synchronized (this) {
            setScavengePeriod(scavengePeriod);
        }

        logger.info("Started {}.", BasicSessionManager.class.getName());
    }

    @Override
    public void doStop() throws Exception {
        logger.info("Stopping {}.", BasicSessionManager.class.getName());
        synchronized (this) {
            if (scavengerTask != null) {
                scavengerTask.cancel();
            }
            if (scheduler != null && doStopScheduler) {
                scheduler.stop();
            }
        }

        sessions.clear();

        logger.info("Stopped {}.", BasicSessionManager.class.getName());

        super.doStop();
    }

    @Override
    protected void addSession(AbstractSession session) {
        if (session instanceof BasicSession) {
            BasicSession utilitySession = (BasicSession) session;
            addSession(utilitySession);
        } else {
            logger.warn("Failed to add session {} to the manager, session not of type BasicSession.", session.getId());
        }
    }

    protected void addSession(BasicSession session) {
        if (isRunning()) {
            sessions.put(session.getId(), session);
            logger.debug("Added session with id {} to the session mapping.", session.getId());
        } else {
            logger.warn("Failed to add session with id {} to the session mapping. Manager not running.", session.getId());
        }
    }

    @Override
    public BasicSession getSession(String id) {
        BasicSession session = sessions.get(id);
        if (session == null || !session.isValid())
            return null;
        else
            return session;
    }

    @Override
    protected AbstractSession newSession(HttpServletRequest request) {
        BasicSession session = new BasicSession(this, request);
        logger.debug("Created a new session with id {}.", session.getId());
        return session;
    }

    @Override
    protected boolean removeSession(String id) {
        boolean success = sessions.remove(id) != null;

        if (success) {
            logger.debug("Removed session with id {} from the session mapping.", id);
        } else {
            logger.debug("Could not remove session with id {} from the session mapping. Session not found.", id);
        }

        return success;
    }

    @Override
    protected void shutdownSessions() throws Exception {
        logger.info("Shutting down sessions.");
        Set<BasicSession> _sessions = getSessionSet();

        int iterations = 100;

        while (_sessions.size() > 0 && iterations-- > 0) {
            if (isStopping()) {
                for (BasicSession session : _sessions) {
                    sessions.remove(session.getClusterId());
                }
            } else {
                /*
                for (BasicSession session : _sessions) {
                    session.invalidate();
                }
                */
            }
            _sessions = getSessionSet();
        }

        logger.info("Session shutdown complete.");
    }

    /**
     * Takes a snapshot of sessions in memory, and returns them in a set.
     *
     * @return A set of sessions currently in memory.
     */
    protected Set<BasicSession> getSessionSet() {
        return new HashSet<>(sessions.values());
    }

    @Override
    public void setMaxInactiveInterval(int seconds) {
        super.setMaxInactiveInterval(seconds);
        if (_dftMaxIdleSecs > 0 && scavengePeriod > _dftMaxIdleSecs * 1000L)
            setScavengePeriod((_dftMaxIdleSecs + 9) / 10);
        logger.info("Set the max inactive interval to {} seconds.", seconds);
    }

    /**
     * Gets the period of the Scavenger.
     *
     * @return The period in seconds.
     */
    public int getScavengePeriod() {
        if (scavengePeriod <= 0)
            return 0;
        else
            return (int) (scavengePeriod / 1000);
    }

    /**
     * Sets the period of the Scavenger.
     * This method will cancel the previous Scavenger
     * if one is present, and create a new  one with the updated period.
     *
     * @param seconds The period in seconds.
     */
    public void setScavengePeriod(int seconds) {
        long old_period = scavengePeriod;
        long period = seconds * 1000L;
        if (period < 0) {
            period = 0;
        }
        scavengePeriod = period;

        if (scheduler != null && (period != old_period || scavengerTask == null)) {
            synchronized (this) {
                if (scavengerTask != null) {
                    logger.info("Canceling the running scavenger instance.");
                    scavengerTask.cancel();
                    scavengerTask = null;
                }
                if (period > 0) {
                    logger.info("Creating a new scavenger with a period of {} seconds.", period / 1000L);
                    scavengerTask = scheduler.schedule(new Scavenger(), scavengePeriod, TimeUnit.MILLISECONDS);
                } else {
                    logger.info("Scavenging of sessions disabled.");
                }
            }
        }
    }

    @Override
    public void crash() {
        sessions.clear();
    }


    public boolean stored(String session) {
        return sessions.containsKey(session);
    }

    /**
     * Class that runs in a separate thread, removing invalid sessions from memory.
     */
    protected class Scavenger implements Runnable {
        public void run() {
            try {
                scavenge();
            } finally {
                if (scheduler != null && scheduler.isRunning())
                    scheduler.schedule(this, scavengePeriod, TimeUnit.MILLISECONDS);
            }
        }
    }

}
