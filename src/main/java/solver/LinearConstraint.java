package solver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A linear programming problem constraint.
 *
 * @author Sebastian Lindholm
 */
public class LinearConstraint {

    private Map<Object, Double> constraints = new HashMap<>();
    private Sign sign = Sign.EQ;
    private Double limit;


    /**
     * Construct a new constraint with default values and with no variables.
     */
    public LinearConstraint() {

    }

    /**
     * Construct a new constraint with the given sign and right-hand-side value.
     * The constraint contains no variables.
     *
     * @param sign  The sign of this constraint.
     * @param limit The right-hand-side value of this constraint.
     */
    public LinearConstraint(Sign sign, Double limit) {
        this.setSign(sign);
        this.setLimit(limit);
    }

    /**
     * Get the sign of this constraint.
     *
     * @return A Sign constant.
     */
    public Sign getSign() {
        return sign;
    }

    /**
     * Set the sign of this constraint.
     *
     * @param sign A Sign constant.
     * @return A reference to this object, useful for chaining methods.
     */
    public LinearConstraint setSign(Sign sign) {
        this.sign = sign;
        return this;
    }

    /**
     * Get the right-hand-side value of this constraint.
     *
     * @return The value.
     */
    public Double getLimit() {
        return limit;
    }

    /**
     * Set the right-hand-side value of this constraint.
     *
     * @param limit The value.
     * @return A reference to this object, useful for chaining methods.
     */
    public LinearConstraint setLimit(Double limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Adds a new variable to this constraint, overwriting any previous values associated with.
     *
     * @param var   The variable.
     * @param value The coefficient of the variable.
     * @return A reference to this object, useful for chaining methods.
     */
    public LinearConstraint putVariable(Object var, Double value) {
        constraints.put(var, value);
        return this;
    }

    /**
     * Get the coefficient associated with a given variable.
     *
     * @param var The variable
     * @return The coefficient value, or null if no value can be found.
     */
    public Double getVariable(Object var) {
        Double val = constraints.get(var);
        if (val == null)
            return 0.0;
        else
            return val;
    }

    /**
     * Get all variables in this constraint.
     *
     * @return A Set of variables.
     */
    public Set<Object> getVariables() {
        return new HashSet<>(constraints.keySet());
    }

    /**
     * Constants representing Equality, Less-than-or-equal and Greater-than-or-equal
     *
     * @author Sebastian Lindholm
     */
    public enum Sign {
        EQ, LTEQ, GTEQ
    }
}
