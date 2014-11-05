package core.optimizer;

/**
 * Constants representing the possible states of a session with respect to storage.
 *
 * @author Sebastian Lindholm
 */
public enum SessionPlacement {

    LOCAL,
    REMOTE,
    BOTH,
    DROP

}
