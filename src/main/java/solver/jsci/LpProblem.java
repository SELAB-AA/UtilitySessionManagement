package solver.jsci;

import com.cflex.util.lpSolve.LpModel;
import com.cflex.util.lpSolve.LpSolver;
import com.cflex.util.lpSolve.SolverListener;
import solver.LinearConstraint;
import solver.LinearObjective.Goal;
import solver.LinearProblem;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An implementaion of a LinearProblem using
 * the lpsolver written in java from JSci
 * (http://sourceforge.net/p/jsci/lpsolve/ci/default/tree/)
 *
 * @author Sebastian Lindholm
 */
public class LpProblem extends LinearProblem {

    private LpModel model;
    private LpSolver solver;

    private Map<Object, Double> result;

    private boolean valid = false;


    @Override
    public synchronized void solve() throws LinearProblemException {
        init();

        solver.startSolver();

        try {
            // The solver runs in a separate thread, calling thread waits.
            wait();
        } catch (InterruptedException e) {
            valid = false;
        }

        if (!valid) {
            throw new LinearProblemException("No solution found.");
        }
    }

    @Override
    public Map<Object, Double> getVariableResults() {
        if (result == null) {
            result = new HashMap<>();
            Set<Object> variableNames = variables.keySet();

            for (Object variable : variableNames) {
                try {
                    result.put(variable, model.getResult(variables.get(variable)));
                } catch (Exception e) {

                }
            }
        }
        return result;
    }

    @Override
    public Double getObjectiveResult() {
        return model.getBestSolution(0);
    }

    /**
     * This method gets called when the solver is ready, waking the calling thread.
     *
     * @param success Whether an optimal solution was found.
     */
    private synchronized void ready(boolean success) {
        valid = success;
        notify();
    }

    /**
     * Assemble the problem.
     *
     * @throws LinearProblemException
     */
    private void init() throws LinearProblemException {

        // Add variables
        addVariables(objective.getVariables());
        for (LinearConstraint constraint : constraints) {
            addVariables(constraint.getVariables());
        }
        double[] values = new double[variables.size() + 1];

        // Construct a new model
        try {
            model = new LpModel(constraints.size() + 1, variables.size());
        } catch (Exception e1) {
            throw new LinearProblemException("Failed to create a model of dimensions (" +
                    constraints.size() + 1 + "," + variables.size() + ").");
        }

        for (Object variable : variables.keySet()) {
            if (isInteger.contains(variable))
                try {
                    model.setInt(variables.get(variable), LpModel.TRUE);
                } catch (Exception e) {
                    throw new LinearProblemException("Failed to set variable to integer.");
                }
        }

        // Set the objective function
        try {
            if (objective.getGoal() == Goal.MAX) {
                model.setMaximum();
            }

            Set<Object> variableNames = variables.keySet();
            for (Object variable : variableNames) {
                values[variables.get(variable)] = objective.getVariable(variable);
            }
            model.setObjFn(values);
        } catch (Exception e) {
            throw new LinearProblemException("Failed to add objective function to problem.");
        }

        // Set the constraints
        Set<Object> variableNames = variables.keySet();
        for (LinearConstraint constraint : constraints) {

            for (Object variable : variableNames) {
                values[variables.get(variable)] = constraint.getVariable(variable);
            }

            int sign = LpModel.LE;
            switch (constraint.getSign()) {

                case GTEQ:
                    sign = LpModel.GE;
                    break;

                case LTEQ:
                    sign = LpModel.LE;
                    break;

                case EQ:
                    sign = LpModel.EQ;
                    break;

                default:
                    break;

            }

            try {
                model.addConstraint(values, sign, constraint.getLimit());
            } catch (Exception e) {
                throw new LinearProblemException("Failed to add constraint to problem.");
            }
        }

        solver = new LpSolver(model);
        LpListener listener = new LpListener();
        model.viewer = listener;
        solver.viewer = listener;

    }

    /**
     * Adds a set of variables to the problem mapping them to unique integer identifiers.
     *
     * @param vars Set containing variable names.
     */
    private void addVariables(Set<Object> vars) {
        int count = variables.size() + 1;
        for (Object variable : vars) {
            if (!variables.containsKey(variable)) {
                variables.put(variable, count);
                count++;
            }
        }
    }

    /**
     * Class listening to the solver.
     *
     * @author Sebastian Lindholm
     */
    private class LpListener implements SolverListener {

        public void error(Exception arg0) {

        }

        public void errorMessage(String arg0) {

        }

        public void finished(int status) {
            switch (status) {

                case LpModel.OPTIMAL:
                    ready(true);
                    break;

                default:
                    ready(false);
                    break;

            }
        }

        public void message(String arg0) {

        }

        public void messageln(String arg0) {

        }

        public void stateChanged() {

        }

        public void stepUpdate(long arg0) {

        }
    }

}
