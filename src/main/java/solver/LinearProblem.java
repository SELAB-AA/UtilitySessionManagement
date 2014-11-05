package solver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class representing a linear programming problem.
 *
 * @author Sebastian Lindholm
 */
public abstract class LinearProblem {

    protected Map<Object, Integer> variables = new HashMap<>();
    protected Set<Object> isInteger = new HashSet<>();
    protected Set<Object> isBoolean = new HashSet<>();
    protected Set<LinearConstraint> constraints = new HashSet<>();
    protected LinearObjective objective;


    /**
     * Add an objective function to this problem, overwriting any previous ones.
     *
     * @param objective The new objective function.
     * @return A reference to this object, useful for chaining methods.
     */
    public LinearProblem addObjective(LinearObjective objective) {
        this.objective = objective;
        return this;
    }

    /**
     * Add a constraint to this problem. A problem may contain any number of constraints.
     *
     * @param constraint The constraint to add.
     * @return A reference to this object, useful for chaining methods.
     */
    public LinearProblem addConstraint(LinearConstraint constraint) {
        this.constraints.add(constraint);
        return this;
    }

    /**
     * Mark a variable as an integer variable.
     *
     * @param variable  The variable.
     * @param isInteger true if the variable is to be set as an integer variable.
     * @return A reference to this object, useful for chaining methods.
     */
    public LinearProblem setInteger(Object variable, boolean isInteger) {
        if (isInteger)
            this.isInteger.add(variable);
        else
            this.isInteger.remove(variable);

        return this;
    }

    /**
     * Mark a variable as an integer variable.
     *
     * @param variable  The variable.
     * @param isBoolean true if the variable is to be set as an integer variable.
     * @return A reference to this object, useful for chaining methods.
     */
    public LinearProblem setBoolean(Object variable, boolean isBoolean) {
        if (isBoolean)
            this.isBoolean.add(variable);
        else
            this.isBoolean.remove(variable);

        return this;
    }

    /**
     * Tries to solve this problem.
     *
     * @return true if an optimal solution is found, else false.
     * @throws LinearProblemException
     */
    public abstract void solve() throws LinearProblemException;

    /**
     * Get the variable values after a successful solve.
     *
     * @return A mapping between variable name and value.
     */
    public abstract Map<Object, Double> getVariableResults();

    /**
     * Get the value of the objective function after a successful solve.
     *
     * @return The value of the function.
     */
    public abstract Double getObjectiveResult();

    @SuppressWarnings("serial")
    public class LinearProblemException extends Exception {
        public LinearProblemException(String msg) {
            super(msg);
        }
    }

}
