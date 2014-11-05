package core.optimizer.alg;


import core.optimizer.OptimizationData;
import core.optimizer.SessionOptimizerSolution;
import core.optimizer.SessionPlacement;
import core.storage.SessionStorage;

import java.util.Arrays;

/**
 * Places all sessions in the remote store.
 * If the remote is full, sessions will be dropped.
 * Sessions with higher utility/size ratio will be prioritized.
 */
public class RemoteOptimizer extends UtilityBasedOptimizer {

    @Override
    public SessionOptimizerSolution optimize(OptimizationData data) {
        if (data == null)
            return null;

        SessionStorage storage = data.getStorages().iterator().next();
        SessionOptimizerSolution solution = new SessionOptimizerSolution();
        SessionLotteryValue[] lotteryValues = new SessionLotteryValue[data.getSessions().size()];

        double value = 0;
        long remoteCapacity = data.getStorageProperties(storage).capacity;
        int i = 0;
        for (String session : data.getSessions()) {
            lotteryValues[i] = new SessionLotteryValue(session, SessionPlacement.REMOTE, data.getSessionProperties(session).value);
            i++;
        }

        Arrays.parallelSort(lotteryValues);

        for (SessionLotteryValue lotteryValue : lotteryValues) {
            long size = data.getSessionProperties(lotteryValue.session).remoteSize;
            SessionPlacement placement = SessionPlacement.DROP;

            if (remoteCapacity >= size) {
                remoteCapacity = remoteCapacity - size;
                placement = lotteryValue.placement;
            }

            value = value + evaluateUtility(lotteryValue.session, placement, storage, data);
            solution.putNewPlacement(lotteryValue.session, placement);
        }

        solution.setValue(value);

        return solution;
    }
}
