package core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Thread that generates crash-events according to an exponential distribution.
 * Used in simulations.
 *
 * @author Sebastian Lindholm
 */
public class CrashGenerator extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(CrashGenerator.class);
    private static final long seed = 5754853343L;

    private Random rng = new Random(seed);
    private double meanTimeToFailure;
    private CrashGeneratorListener crashable;
    private boolean isStopping = false;


    /**
     * Creates a new CrashGenerator thread.
     *
     * @param crashable         The CrashGeneratorListener to receive events from this thread.
     * @param meanTimeToFailure Expected time between crashes, in seconds.
     */
    public CrashGenerator(CrashGeneratorListener crashable, double meanTimeToFailure) {
        super("crash-gen");
        this.crashable = crashable;
        this.meanTimeToFailure = meanTimeToFailure;
    }

    @Override
    public void run() {

        double u;
        double log;
        long nextCrash;

        while (!isStopping) {
            u = rng.nextDouble();

            if (u < 1E-3)
                continue;

            log = -Math.log(u);
            nextCrash = (long) (meanTimeToFailure * log * 1000);
            logger.info("Scheduled next crash of {} in approx. {} s.", crashable.getClass().getName(), nextCrash / 1000L);
            try {
                Thread.sleep(nextCrash);
            } catch (InterruptedException e) {
                logger.warn("Premature crash!", e);
            }

            logger.info("Crashing {}..", crashable.getClass().getSimpleName());
            crashable.crash();
        }

    }

    /**
     * Tries to gracefully shut down this thread.
     */
    public void shutdown() {
        isStopping = true;
    }

}
