package core.optimizer;

/**
 * Generic interface to an optimizer.
 *
 * @author Sebastian Lindholm
 */
public interface SessionOptimizer {

    /**
     * Constructs an allocation plan in the form of a SessionOptimizerSolution,
     * given relevant OptimizationData. This method is run once every persist period.
     *
     * @param data Data from which the solution is to be derived.
     * @return The solution, assigning each session to a SessionPlacement.
     */
    public SessionOptimizerSolution optimize(OptimizationData data);

}
