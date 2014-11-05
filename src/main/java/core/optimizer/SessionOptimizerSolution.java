package core.optimizer;

import java.util.HashMap;
import java.util.Map;

/**
 * Class representing the result of a SessionOptimizer.
 * It contains mappings between Session ids and SessionPlacement objects.
 *
 * @author Sebastian Lindholm
 */
public class SessionOptimizerSolution {

    private double value;
    private Map<String, SessionPlacement> sessionPlacement = new HashMap<>();

    /**
     * Adds a new mapping, overwriting any previous mappings for the session.
     *
     * @param session   The session to map.
     * @param placement The SessionPlacement for the session.
     */
    public void putNewPlacement(String session, SessionPlacement placement) {
        sessionPlacement.put(session, placement);
    }

    /**
     * Gets the SessionPlacement mapping for a session.
     *
     * @param session The session id.
     * @return A SessionPlacement object if the mapping exists, otherwise null.
     */
    public SessionPlacement getNewPlacement(String session) {
        return sessionPlacement.get(session);
    }

    /**
     * Gets the total utility value for this solution.
     *
     * @return The value.
     */
    public double getValue() {
        return value;
    }

    /**
     * Sets the total utility value for this solution.
     *
     * @param value The value.
     */
    public void setValue(double value) {
        this.value = value;
    }

    public boolean contains(String session) {
        return sessionPlacement.containsKey(session);
    }

    public int size() {
        return sessionPlacement.size();
    }
}
