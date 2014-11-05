package solver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class representing a linear programming objective function.
 *
 * @author Sebastian Lindholm
 */
public class LinearObjective {

    private Map<Object, Double> values = new HashMap<>();

    ;
    private Goal goal = Goal.MIN;

    /**
     * Adds a new variable to the objective function, overwriting
     * any previous values associated to that variable.
     *
     * @param var   The variable.
     * @param value The coefficient of the variable.
     * @return A reference to this object, useful for chaining methods.
     */
    public LinearObjective putVariable(Object var, Double value) {
        values.put(var, value);
        return this;
    }

    /**
     * Get the coefficient value for a variable.
     *
     * @param var The variable.
     * @return The value, or null if not found.
     */
    public Double getVariable(Object var) {
        Double val = values.get(var);
        if (val == null)
            return 0.0;
        else
            return val;
    }

    /**
     * Get the goal of the objective function.
     *
     * @return A goal constant.
     */
    public Goal getGoal() {
        return goal;
    }

    /**
     * Set the goal of the objective function.
     *
     * @param goal A goal constant.
     * @return A reference to this object, useful for chaining methods.
     */
    public LinearObjective setGoal(Goal goal) {
        this.goal = goal;
        return this;
    }

    /**
     * Get all the variables in the objective function.
     *
     * @return A Set of variable names.
     */
    public Set<Object> getVariables() {
        return new HashSet<>(values.keySet());
    }

    /**
     * Constants indicating whether the goal is to
     * minimize or maximize the objective function.
     *
     * @author Sebastian Lindholm
     */
    public enum Goal {
        MIN, MAX
    }

}
