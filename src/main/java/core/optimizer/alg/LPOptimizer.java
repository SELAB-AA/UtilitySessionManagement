package core.optimizer.alg;

import core.optimizer.OptimizationData;
import core.optimizer.SessionOptimizerSolution;
import core.optimizer.SessionPlacement;
import core.storage.SessionStorage;
import solver.LinearConstraint;
import solver.LinearConstraint.Sign;
import solver.LinearObjective;
import solver.LinearObjective.Goal;
import solver.LinearProblem;
import solver.LinearProblem.LinearProblemException;
import solver.jsci.LpProblem;

import java.util.Arrays;
import java.util.Map;

/**
 * SessionOptimizer that solves a linear programming problem
 * based on a utility function, and relocates sessions accordingly.
 *
 * @author Sebastian Lindholm
 */
public class LPOptimizer extends UtilityBasedOptimizer {

    private static final double SCALING_FACTOR = 1E3;

    private OptimizationData data;
    private SessionStorage storage;
    private SessionLotteryValue[] lotteryValues;

    @Override
    public SessionOptimizerSolution optimize(OptimizationData data) {
        if (data != null) {
            this.data = data;
        } else {
            return null;
        }

        // Temporary solution: get the first session storage.
        storage = data.getStorages().iterator().next();

        LinearProblem problem = makeProblem();

        try {
            problem.solve();
            return parseSolution(problem);
        } catch (LinearProblemException e) {

            return null;
        }

    }

    /**
     * Assemble the problem.
     *
     * @return LinearProblem with appropriate objective function and constraints.
     */
    private LinearProblem makeProblem() {
        LinearProblem problem = new LpProblem();

        // Objective function: Maximize
        LinearObjective objective = new LinearObjective();
        objective.setGoal(Goal.MAX);

        // Constraints: Neither the local or remote storage can exceed their respective capacities.
        LinearConstraint localResources = new LinearConstraint(Sign.LTEQ, (double) data.localCapacity);
        LinearConstraint remoteResources = new LinearConstraint(Sign.LTEQ, (double) data.getStorageProperties(storage).capacity);

        lotteryValues = new SessionLotteryValue[data.getSessions().size() * SessionPlacement.values().length];

        int i = 0;
        for (String session : data.getSessions()) {
            Double localSize = (double) data.getSessionProperties(session).localSize;
            Double remoteSize = (double) data.getSessionProperties(session).remoteSize;

            // Constraint: For each session, exactly one placement has to be chosen.
            LinearConstraint oneLottery = new LinearConstraint(Sign.EQ, 1.0D);

            for (SessionPlacement placement : SessionPlacement.values()) {
                double util = evaluateUtility(session, placement, storage, data);

                lotteryValues[i] = new SessionLotteryValue(session, placement, util);

                objective.putVariable(lotteryValues[i], util * SCALING_FACTOR);

                // Depending on the placement, resources have to be allocated accordingly
                switch (placement) {

                    case BOTH:
                        localResources.putVariable(lotteryValues[i], localSize);
                        remoteResources.putVariable(lotteryValues[i], remoteSize);
                        break;

                    case LOCAL:
                        localResources.putVariable(lotteryValues[i], localSize);
                        break;

                    case REMOTE:
                        remoteResources.putVariable(lotteryValues[i], remoteSize);
                        break;

                    default:
                        break;

                }

                oneLottery.putVariable(lotteryValues[i], 1.0D);
                i++;
            }
            problem.addConstraint(oneLottery);
        }

        problem.addConstraint(localResources);
        problem.addConstraint(remoteResources);
        problem.addObjective(objective);


        return problem;
    }

    /**
     * Creates a SessionOptimizerSolution object given a solved problem.
     *
     * @param problem A problem that has been solve():d.
     * @return A SessionOptimizerSolution based on the results from the linear problem.
     */
    private SessionOptimizerSolution parseSolution(LinearProblem problem) {
        SessionOptimizerSolution solution = new SessionOptimizerSolution();
        Map<Object, Double> result = problem.getVariableResults();
        double value = 0;
        long localCapacity = data.localCapacity;
        long remoteCapacity = data.getStorageProperties(storage).capacity;

        Arrays.parallelSort(lotteryValues, (o1, o2) -> {
            double val = result.get(o1) - result.get(o2);
            if (val > 0)
                return -1;
            else if (val < 0)
                return 1;
            else
                return 0;
        });

        for (SessionLotteryValue lottery : lotteryValues) {
            if (!solution.contains(lottery.session)) {

                long localSize = data.getSessionProperties(lottery.session).localSize;
                long remoteSize = data.getSessionProperties(lottery.session).remoteSize;

                switch (lottery.placement) {

                    case DROP:
                        solution.putNewPlacement(lottery.session, SessionPlacement.DROP);
                        value = value + lottery.value;
                        break;

                    case LOCAL:
                        if (localCapacity >= localSize) {
                            solution.putNewPlacement(lottery.session, SessionPlacement.LOCAL);
                            localCapacity = localCapacity - localSize;
                            value = value + lottery.value;
                        }
                        break;

                    case REMOTE:
                        if (remoteCapacity >= remoteSize) {
                            solution.putNewPlacement(lottery.session, SessionPlacement.REMOTE);
                            remoteCapacity = remoteCapacity - remoteSize;
                            value = value + lottery.value;
                        }
                        break;

                    case BOTH:
                        if (localCapacity >= localSize && remoteCapacity >= remoteSize) {
                            solution.putNewPlacement(lottery.session, SessionPlacement.BOTH);
                            localCapacity = localCapacity - localSize;
                            remoteCapacity = remoteCapacity - remoteSize;
                            value = value + lottery.value;
                        }
                        break;

                    default:
                        solution.putNewPlacement(lottery.session, SessionPlacement.DROP);
                        break;
                }

            }
        }

        solution.setValue(value);

        return solution;
    }

}
