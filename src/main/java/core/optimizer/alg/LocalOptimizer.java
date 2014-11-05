package core.optimizer.alg;


import core.optimizer.OptimizationData;
import core.optimizer.SessionOptimizerSolution;
import core.optimizer.SessionPlacement;
import core.storage.SessionStorage;

import java.util.Arrays;

/**
 * Optimizer that sorts sessions according to their value/size ratio
 * and allocates them to the local store as long as they fit.
 *
 * @author Sebastian Lindholm
 */
public class LocalOptimizer extends UtilityBasedOptimizer {

    @Override
    public SessionOptimizerSolution optimize(OptimizationData data) {
        if (data == null)
            return null;

        SessionStorage storage = data.getStorages().iterator().next();
        SessionOptimizerSolution solution = new SessionOptimizerSolution();
        SessionLotteryValue[] lotteryValues = new SessionLotteryValue[data.getSessions().size()];

        double value = 0;
        long localCapacity = data.localCapacity;
        int i = 0;
        for (String session : data.getSessions()) {
            lotteryValues[i] = new SessionLotteryValue(session, SessionPlacement.LOCAL, data.getSessionProperties(session).value);
            i++;
        }

        Arrays.parallelSort(lotteryValues);

        for (SessionLotteryValue lotteryValue : lotteryValues) {
            long size = data.getSessionProperties(lotteryValue.session).localSize;
            SessionPlacement placement = SessionPlacement.DROP;

            if (localCapacity >= size) {
                localCapacity = localCapacity - size;
                placement = lotteryValue.placement;
            }

            value = value + evaluateUtility(lotteryValue.session, placement, storage, data);

            solution.putNewPlacement(lotteryValue.session, placement);

        }

        solution.setValue(value);

        return solution;
    }

}
