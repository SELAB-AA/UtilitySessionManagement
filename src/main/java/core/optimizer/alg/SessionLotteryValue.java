package core.optimizer.alg;

import core.optimizer.SessionPlacement;

/**
 * A comparable (Session, Placement, Value) tuple.
 * The comparator orders tuples in descending order.
 *
 * @author Sebastian Lindholm
 */
public class SessionLotteryValue implements Comparable<SessionLotteryValue> {

    public String session;
    public SessionPlacement placement;
    public double value;

    public SessionLotteryValue(String session, SessionPlacement placement, double value) {
        this.session = session;
        this.placement = placement;
        this.value = value;
    }

    @Override
    public int compareTo(SessionLotteryValue o) {
        if (this.value == o.value)
            return 0;
        else if (this.value > o.value)
            return -1;
        else
            return 1;
    }
}
