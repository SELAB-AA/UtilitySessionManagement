package core.optimizer.alg;

import core.optimizer.OptimizationData;
import core.optimizer.SessionOptimizerSolution;
import core.optimizer.SessionPlacement;
import core.storage.SessionStorage;

import java.util.Arrays;

/**
 * Optimizer that sorts sessions according to their value/size ratio and
 * allocates them to both local and remote storage as long there is enough space.
 * If a store runs out space the optimizer continues to allocate sessions to a single store.
 *
 * @author Sebastian Lindholm
 */
public class BothOptimizer extends UtilityBasedOptimizer {

    @Override
    public SessionOptimizerSolution optimize(OptimizationData data) {
        if (data == null)
            return null;

        SessionStorage storage = data.getStorages().iterator().next();
        SessionOptimizerSolution solution = new SessionOptimizerSolution();
        SessionLotteryValue[] lotteryValues = new SessionLotteryValue[data.getSessions().size()];

        double value = 0;
        long localCapacity = data.localCapacity;
        long remoteCapacity = data.getStorageProperties(storage).capacity;
        int i = 0;
        for (String session : data.getSessions()) {
            lotteryValues[i] = new SessionLotteryValue(session, SessionPlacement.BOTH, data.getSessionProperties(session).value);
            i++;
        }

        Arrays.parallelSort(lotteryValues);

        for (SessionLotteryValue lotteryValue : lotteryValues) {
            long localSize = data.getSessionProperties(lotteryValue.session).localSize;
            long remoteSize = data.getSessionProperties(lotteryValue.session).remoteSize;
            boolean localValid = localCapacity >= localSize;
            boolean remoteValid = remoteCapacity >= remoteSize;
            SessionPlacement placement = SessionPlacement.DROP;

            if (localValid) {
                localCapacity = localCapacity - localSize;
                placement = SessionPlacement.LOCAL;
            }
            if (remoteValid) {
                remoteCapacity = remoteCapacity - remoteSize;
                if (localValid)
                    placement = SessionPlacement.BOTH;
                else
                    placement = SessionPlacement.REMOTE;
            }

            solution.putNewPlacement(lotteryValue.session, placement);
            value = value + evaluateUtility(lotteryValue.session, placement, storage, data);
        }

        solution.setValue(value);

        return solution;
    }

}
