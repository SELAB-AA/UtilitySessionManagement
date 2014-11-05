package core.optimizer;

/**
 * Optimization data specific to sessions.
 *
 * @author Sebastian Lindholm
 */
public class SessionProperties {

    public static double initialValue = 1;

    public double value = initialValue;
    public SessionPlacement oldPlacement = SessionPlacement.DROP;
    public Long localSize = null;
    public Long remoteSize = null;
    public int accesses = 0;
    public long lastAccess = System.currentTimeMillis();

}
