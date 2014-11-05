package core;

import core.optimizer.*;
import core.optimizer.alg.LPOptimizer;
import core.predictor.IntegratingPredictor;
import core.predictor.Predictor;
import core.storage.SessionStorage;
import core.storage.StoredSession;
import core.transform.RandomValueChange;
import core.transform.Transform;
import core.util.ClassLoadingFactory;
import core.util.CrashGenerator;
import core.util.PropertyParser;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The OptimizingSessionManager is an extension of the
 * PersistentSessionManager, that distributes sessions between local
 * and remote storage according to a SessionOptimizerSolution.
 * The solution is obtained from an implementation of the
 * SessionOptimizer interface, such as the LPOptimizer.
 *
 * @author Sebastian Lindholm
 */
public class OptimizingSessionManager extends PersistentSessionManager implements SessionListener {

    private static final Logger logger = LoggerFactory.getLogger(OptimizingSessionManager.class);
    private static final Logger perf4jLogger = LoggerFactory.getLogger("org.perf4j.TimingLogger");
    private static final Logger stats = LoggerFactory.getLogger("statistics");

    private static final String STORAGE_CONFIG = "storage.conf";
    private static final String LOCAL_MTTF_PARAM = "localMTTF";
    private static final String REMOTE_MTTF_PARAM = "remoteMTTF";
    private static final String READ_COST_PARAM = "readCost";
    private static final String WRITE_COST_PARAM = "writeCost";
    private static final String STORAGE_COST_PARAM = "storageCost";

    private static final String OPTIMIZER_PARAM = "session-optimizer";
    private static final String DEFAULT_OPTIMIZER = LPOptimizer.class.getName();
    private static final String TRANSFORM_PARAM = "session-data-transform";
    private static final String DEFAULT_TRANSFORM = RandomValueChange.class.getName();
    private static final String INITIAL_VALUE_PARAM = "session-initial-value";
    private static final double DEFAULT_INITIAL_VALUE = 1.0D;

    private final OptimizationData data = new OptimizationData();
    private final ConcurrentMap<String, AtomicInteger> accessBuffer = new ConcurrentHashMap<>();
    private final SizeEvaluator sizeEvaluator = new SerializingSizeEvaluator();
    private final Predictor predictor = new IntegratingPredictor();
    private long lastRun = 0;
    private double lastUtility = 0;
    private SessionOptimizer optimizer;
    private Set<Transform> transforms = new LinkedHashSet<>();
    private Set<String> rubbishBin = new HashSet<>();
    private ConcurrentMap<String, Lock> locks = new ConcurrentHashMap<>();
    private CrashGenerator crashGenerator;


    @Override
    public void addSession(BasicSession session) {
        if (session != null) {
            locks.putIfAbsent(session.getClusterId(), new ReentrantLock(true));
            super.addSession(session);
        } else {
            logger.warn("Trying to add null session to the manager. Ignoring.");
        }
    }

    @Override
    public BasicSession getSession(String id) {
        Lock lock = locks.get(id);
        BasicSession session = null;
        if (lock == null) {
            locks.putIfAbsent(id, new ReentrantLock(true));
            lock = locks.get(id);
        }

        lock.lock();
        try {
            session = super.getSession(id);
        } finally {
            lock.unlock();
        }

        return session;
    }

    @Override
    public boolean removeSession(String id) {
        Lock lock = locks.get(id);
        boolean success;
        if (lock != null) {
            lock.lock();
            try {
                success = super.removeSession(id);
            } finally {
                lock.unlock();
            }
        } else {
            success = super.removeSession(id);
        }

        return success;
    }

    @Override
    protected int scavengeAction() {
        long now = System.currentTimeMillis();
        Set<String> sessions = new HashSet<>(data.getSessions());
        int scavengeCount = 0;

        for (String sessionId : sessions) {
            SessionProperties properties = data.getSessionProperties(sessionId);

            long max = (long) getMaxInactiveInterval() * 1000L;
            if (now - properties.lastAccess > max) {
                Lock lock = locks.get(sessionId);
                if (lock != null) {
                    lock.lock();
                    try {
                        BasicSession session = getSession(sessionId);
                        if (session != null) {
                            session.timeoutSession();
                            getSessionStorage().remove(sessionId);
                            removeSession(sessionId);
                            scavengeCount++;
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            }
        }

        return scavengeCount;
    }

    @Override
    protected void persist() {
        logger.info("Running persist.");
        if (getSessionStorage() != null) {
            StopWatch optim = new StopWatch();
            StopWatch load = new Slf4JStopWatch("LOAD", perf4jLogger);
            readStoredSessions();
            load.stop();
            doTransformChain();
            executeOptimization();
            optim.stop();

            lastRun = optim.getElapsedTime();
            data.optimizerPeriod = (int) predictor.predictNext((int) (lastRun / 1000L) + getPersistPeriod());

        } else {
            logger.warn("No session storage defined.");
        }
        logger.info("Persist completed.");
    }

    @Override
    public void setPersistPeriod(int seconds) {
        synchronized (data) {
            data.optimizerPeriod = seconds;
        }
        super.setPersistPeriod(seconds);
    }

    @Override
    public void doStart() throws Exception {
        super.doStart();
        logger.info("Starting {}.", OptimizingSessionManager.class.getName());

        parseConfig();

        // Try to load an appropriate SessionOptimizer
        String optimizerValue = this.getContext().getInitParameter(OPTIMIZER_PARAM);
        optimizer = ClassLoadingFactory.tryLoadClass(optimizerValue, DEFAULT_OPTIMIZER, SessionOptimizer.class);

        // Try loading a Transform
        String transformValue = this.getContext().getInitParameter(TRANSFORM_PARAM);
        Transform transform = ClassLoadingFactory.tryLoadClass(transformValue, DEFAULT_TRANSFORM, Transform.class);
        if(transform!=null)
            transforms.add(transform);

        // Load the session initial value
        String initialValue = this.getContext().getInitParameter(INITIAL_VALUE_PARAM);
        double initial = DEFAULT_INITIAL_VALUE;
        if(initialValue!=null){
            try {
                initial = Double.parseDouble(initialValue);
                logger.info("Setting initial session value to {}.", initial);
            }
            catch(NumberFormatException e){
                logger.warn("Invalid number format, using default initial session value {}.", initial);
            }
        }
        else {
            logger.info("No initial session value configured, using default: {}.", initial);
        }
        //SessionProperties.initialValue = 10 * 0.0054018;
        SessionProperties.initialValue = initial;

        // Generate some "crashes" according to the local storage MTTF..
        if (data.localMTTF > 0) {
            crashGenerator = new CrashGenerator(this, data.localMTTF);
            crashGenerator.start();
        }
    }

    /**
     * Reads the storage configuration file, placing relevant data in the OptimizationData object.
     */
    private void parseConfig() {
        Map<String, Double> settings = new HashMap<>();

        settings.put(LOCAL_MTTF_PARAM, 0D);
        settings.put(REMOTE_MTTF_PARAM, 0D);
        settings.put(READ_COST_PARAM, 0D);
        settings.put(WRITE_COST_PARAM, 0D);
        settings.put(STORAGE_COST_PARAM, 0D);

        // Try to parse the STORAGE_CONFIG
        try (
                PropertyParser parser = new PropertyParser(Paths.get("WebContent", "UTIL-CONF", STORAGE_CONFIG))
        ) {
            Map<String, String> config = parser.parse();

            for (String key : settings.keySet()) {
                String value = null;
                try {
                    if (config.containsKey(key)) {
                        value = config.get(key);
                        settings.put(key, Double.parseDouble(value));
                        logger.info("Found {} entry: {} = {}.", STORAGE_CONFIG, key, value);
                    }
                } catch (NumberFormatException ex) {
                    logger.warn("Could not parse {} entry: {} = {}.", STORAGE_CONFIG, key, value);
                }
            }
        } catch (Exception e) {
            logger.warn("Error while parsing storage configuration file.", e);
        }

        // Save them in the OptimizationData object.

        data.optimizerPeriod = getPersistPeriod();
        data.localMTTF = settings.get(LOCAL_MTTF_PARAM);

        StorageProperties properties = new StorageProperties();
        properties.MTTF = settings.get(REMOTE_MTTF_PARAM);
        properties.readCost = settings.get(READ_COST_PARAM);
        properties.writeCost = settings.get(WRITE_COST_PARAM);
        properties.storageCost = settings.get(STORAGE_COST_PARAM);

        data.putStorageProperties(getSessionStorage(), properties);
    }

    @Override
    public void doStop() throws Exception {
        logger.info("Stopping {}.", OptimizingSessionManager.class.getName());

        if (crashGenerator != null)
            crashGenerator.shutdown();

        synchronized (data) {
            data.clearSessions();
        }

        logger.info("Stopped {}.", OptimizingSessionManager.class.getName());
        super.doStop();
    }

    /**
     * Execute any attached Transforms.
     */
    private void doTransformChain() {
        for (Transform transform : transforms) {
            transform.doTransform(data);
        }
    }

    /**
     * Reads the ids of stored sessions and updates the OptimizationData object.
     */
    private void readStoredSessions() {
        logger.info("Reading stored sessions..");

        Set<BasicSession> local = getSessionSet();
        List<StoredSession> remote = getSessionStorage().stored();

        // Mark all sessions for deletion
        for (String sessionId : data.getSessions()) {
            data.getSessionProperties(sessionId).oldPlacement = SessionPlacement.DROP;
        }

        double value = 0;
        double costs = 0;
        int sessions = 0;

        // Sessions found on remote storage
        for (StoredSession session : remote) {
            SessionProperties properties;

            if (!data.containsSession(session.getSessionId())) {
                properties = new SessionProperties();
                data.putSessionProperties(session.getSessionId(), properties);
            } else {
                properties = data.getSessionProperties(session.getSessionId());
            }

            if (accessBuffer.containsKey(session.getSessionId())) {
                properties.accesses = accessBuffer.get(session.getSessionId()).getAndSet(0);
            } else {
                properties.accesses = 0;
            }

            properties.oldPlacement = SessionPlacement.REMOTE;

            properties.remoteSize = session.getRemoteSize();
            if(properties.localSize == null){
                BasicSession concreteSession = getSession(session.getSessionId());
                if(concreteSession!=null) {
                    properties.localSize = sizeEvaluator.evaluateLocal(concreteSession);
                }
                else {
                    logger.warn("Session {} marked as stored, but could not be found. Marking for drop.");
                    properties.oldPlacement = SessionPlacement.DROP;
                }
            }

            value += properties.value;
            costs += evaluateCosts(session.getSessionId(), getSessionStorage(), lastRun / 1000L);
            sessions++;
        }

        // Sessions found in local storage
        for (BasicSession session : local) {
            Lock lock = locks.get(session.getClusterId());
            if (lock != null) {
                lock.lock();
                try {
                    if (session.isValid()) {
                        SessionProperties properties;

                        if (!data.containsSession(session.getId())) {
                            properties = new SessionProperties();
                            data.putSessionProperties(session.getId(), properties);
                        } else {
                            properties = data.getSessionProperties(session.getId());
                        }

                        Long oldSize = properties.localSize;
                        properties.localSize = sizeEvaluator.evaluateLocal(session);
                        if(oldSize==null || oldSize.equals(properties.localSize) || properties.remoteSize==null)
                            properties.remoteSize = sizeEvaluator.evaluateRemote(session, getSessionStorage());

                        switch (properties.oldPlacement) {

                            case REMOTE:
                                properties.oldPlacement = SessionPlacement.BOTH;
                                break;

                            default:
                                properties.oldPlacement = SessionPlacement.LOCAL;

                                if (accessBuffer.containsKey(session.getClusterId())) {
                                    properties.accesses = accessBuffer.get(session.getClusterId()).getAndSet(0);
                                } else {
                                    properties.accesses = 0;
                                }

                                value += properties.value;
                                sessions++;
                                break;

                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

        double valueWithCosts = value - costs;

        stats.info(", {}, {}, {}, {}", sessions, lastUtility, value, valueWithCosts);

        accessBuffer.clear();

        logger.info("Stored sessions: {} LOCAL, {} REMOTE ({} UNIQUE).", local.size(), remote.size(), sessions);

        logger.info("Session values: {} TOTAL, {} WITH_COSTS.", value, valueWithCosts);

        // Update capacities
        long localMemory = (long) (0.25 * Runtime.getRuntime().totalMemory());
        //long localMemory = 100 * 1048576; // 100 MB
        long remoteMemory = getSessionStorage().capacity();

        data.localCapacity = localMemory;
        data.getStorageProperties(getSessionStorage()).capacity = remoteMemory;

        logger.info("Usable memory: {} LOCAL, {} REMOTE ({} TOTAL).", localMemory, remoteMemory, localMemory + remoteMemory);

    }

    /**
     * Runs the optimizer and relocates sessions based on the results.
     */
    private void executeOptimization() {

        logger.info("Executing optimization..");

        Set<String> sessions = data.getSessions();

        SessionOptimizerSolution solution;

        StopWatch opt = new Slf4JStopWatch("OPTIMIZATION", perf4jLogger);

        synchronized (data) {
            solution = optimizer.optimize(data);
        }

        if (solution == null) {
            logger.warn("No solution found.");
            opt.stop("OPTIMIZATION_FAIL");
            return;
        } else {
            opt.stop("OPTIMIZATION_OK");
            lastUtility = solution.getValue();
            logger.info("Found solution: {} UTILITY.", lastUtility);
        }

        StopWatch rel = new Slf4JStopWatch("RELOCATION", perf4jLogger);

        int dropCount = 0;
        int localCount = 0;
        int bothCount = 0;
        int remoteCount = 0;

        logger.info("Relocating sessions..");

        for (String sessionId : sessions) {

            switch (solution.getNewPlacement(sessionId)) {

                case DROP: {
                    executeDrop(sessionId);
                    dropCount++;
                    break;
                }

                case LOCAL: {
                    executeLocal(sessionId);
                    localCount++;
                    break;
                }

                case BOTH: {
                    executeBoth(sessionId);
                    bothCount++;
                    break;
                }

                case REMOTE: {
                    executeRemote(sessionId);
                    remoteCount++;
                    break;
                }

                default: {
                    executeDrop(sessionId);
                    break;
                }

            }

        }

        emptyRubbishBin();

        logger.info("Optimization finished: {} DROP, {} LOCAL, {} REMOTE, {} BOTH.",
                dropCount, localCount, remoteCount, bothCount);

        rel.stop();
    }

    /**
     * Places a session in local storage.
     *
     * @param sessionId Id of the session to be moved.
     */
    private void executeLocal(String sessionId) {
        BasicSession session;
        SessionStorage storage = getSessionStorage();
        SessionPlacement placement = data.getSessionProperties(sessionId).oldPlacement;
        Lock lock = locks.get(sessionId);

        if (lock != null) {
            lock.lock();
            try {
                boolean storedLocally = stored(sessionId);
                switch (placement) {

                    case BOTH:
                        if (!storedLocally) {
                            session = getSession(sessionId);
                            if (session != null) {
                                if (session.isValid())
                                    addSession(session);
                                else
                                    executeDrop(sessionId);
                            } else
                                rubbishBin.add(sessionId);
                        }

                        storage.remove(sessionId);
                        break;

                    case LOCAL:
                        if (!storedLocally)
                            rubbishBin.add(sessionId);
                        else {
                            session = getSession(sessionId);
                            if (!session.isValid())
                                executeDrop(sessionId);
                        }
                        break;

                    case REMOTE:
                        if (!storedLocally) {
                            session = getSession(sessionId);
                            if (session != null) {
                                if (session.isValid()) {
                                    addSession(session);
                                    storage.remove(sessionId);
                                } else
                                    executeDrop(sessionId);
                            } else
                                rubbishBin.add(sessionId);
                        } else {
                            storage.remove(sessionId);
                        }


                        break;

                    case DROP:
                        rubbishBin.add(sessionId);

                        break;

                    default:
                        logger.debug("Session {} missing old placement value.", sessionId);
                        break;

                }
            } finally {
                lock.unlock();
            }
        } else {
            rubbishBin.add(sessionId);
        }

    }

    /**
     * Places a session in remote storage.
     *
     * @param sessionId Id of the session to be moved.
     */
    private void executeRemote(String sessionId) {
        BasicSession session;
        SessionStorage storage = getSessionStorage();
        SessionPlacement placement = data.getSessionProperties(sessionId).oldPlacement;
        Lock lock = locks.get(sessionId);

        if (lock != null) {
            lock.lock();
            try {
                boolean storedLocally = stored(sessionId);
                switch (placement) {

                    case BOTH:
                        if (storedLocally) {
                            session = getSession(sessionId);
                            if (session != null) {
                                storage.store(session.getSessionData());
                                removeSession(sessionId);
                            } else {
                                executeDrop(sessionId);
                            }
                        }
                        break;

                    case LOCAL:
                        if (storedLocally) {
                            session = getSession(sessionId);
                            if (session != null) {
                                storage.store(session.getSessionData());
                                removeSession(sessionId);
                            } else {
                                executeDrop(sessionId);
                            }
                        } else {
                            rubbishBin.add(sessionId);
                        }
                        break;

                    case REMOTE:
                        if (storedLocally) {
                            session = getSession(sessionId);
                            if (session != null) {
                                storage.store(session.getSessionData());
                                removeSession(sessionId);
                            } else {
                                executeDrop(sessionId);
                            }
                        }
                        break;

                    case DROP:
                        rubbishBin.add(sessionId);

                        break;

                    default:
                        logger.debug("Session {} missing old placement value.", sessionId);
                        break;

                }
            } finally {
                lock.unlock();
            }
        } else {
            rubbishBin.add(sessionId);
        }

    }

    /**
     * Places a session in both local and remote storage.
     *
     * @param sessionId Id of the session to be moved.
     */
    private void executeBoth(String sessionId) {
        BasicSession session;
        SessionStorage storage = getSessionStorage();
        SessionPlacement placement = data.getSessionProperties(sessionId).oldPlacement;
        Lock lock = locks.get(sessionId);

        if (lock != null) {
            lock.lock();
            try {
                boolean storedLocally = stored(sessionId);
                switch (placement) {

                    case BOTH:
                        session = getSession(sessionId);
                        if (session != null) {
                            if (storedLocally) {
                                storage.store(session.getSessionData());
                            } else {
                                addSession(session);
                            }
                        } else {
                            executeDrop(sessionId);
                        }
                        break;

                    case LOCAL:
                        if (storedLocally) {
                            session = getSession(sessionId);
                            if (session != null) {
                                storage.store(session.getSessionData());
                            } else
                                executeDrop(sessionId);
                        } else {
                            executeDrop(sessionId);
                        }
                        break;

                    case REMOTE:
                        session = getSession(sessionId);
                        if (session != null) {
                            if (storedLocally) {
                                storage.store(session.getSessionData());
                            } else {
                                addSession(session);
                            }
                        } else {
                            executeDrop(sessionId);
                        }
                        break;

                    case DROP:
                        rubbishBin.add(sessionId);

                        break;

                    default:
                        logger.debug("Session {} missing old placement value.", sessionId);
                        break;

                }
            } finally {
                lock.unlock();
            }
        } else {
            rubbishBin.add(sessionId);
        }

    }

    private void executeDrop(String sessionId) {
        BasicSession session;
        SessionStorage storage = getSessionStorage();
        Lock lock = locks.get(sessionId);

        if (lock != null) {
            lock.lock();
            try {
                session = getSession(sessionId);
                if (session != null) {
                    session.invalidate();
                    storage.remove(sessionId);
                    removeSession(sessionId);
                }
            } finally {
                lock.unlock();
            }
        }

        rubbishBin.add(sessionId);
    }


    /**
     * Removes session in the bin from the data object
     */
    private int emptyRubbishBin() {
        for (String id : rubbishBin) {
            data.removeSession(id);
            locks.remove(id);
            accessBuffer.remove(id);
        }

        int size = rubbishBin.size();

        rubbishBin.clear();

        return size;
    }

    @Override
    public void sessionAccessed(BasicSession session) {
        long now = System.currentTimeMillis();
        accessBuffer.putIfAbsent(session.getClusterId(), new AtomicInteger(0));
        accessBuffer.get(session.getClusterId()).incrementAndGet();
        SessionProperties properties = data.getSessionProperties(session.getClusterId());
        if (properties != null) {
            properties.lastAccess = now;
        }
    }

    private double evaluateCosts(String session, SessionStorage storage, long duration) {
        StorageProperties storageProperties = data.getStorageProperties(storage);

        double size = ((double) data.getSessionProperties(session).remoteSize) / 1048576.0D;

        double readCost = storageProperties.readCost;
        double writeCost = storageProperties.writeCost;
        double storageCost = storageProperties.storageCost;

        return readCost + writeCost + duration * size * storageCost;
    }

}
