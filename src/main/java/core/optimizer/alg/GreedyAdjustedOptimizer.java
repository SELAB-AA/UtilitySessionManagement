package core.optimizer.alg;


import core.optimizer.OptimizationData;
import core.optimizer.SessionOptimizerSolution;
import core.optimizer.SessionPlacement;
import core.storage.SessionStorage;

import java.util.Arrays;
import java.util.Set;

/**
 * Optimizer that sorts sessions according to their value/size ratio
 * and allocates them to the SessionPlacement with the highest value
 * in which they will fit. The adjusted version only uses a weighted
 * value for the LOCAL and BOTH lotteries.
 *
 * @author Sebastian Lindholm
 */
public class GreedyAdjustedOptimizer extends UtilityBasedOptimizer {

    private OptimizationData data;
    private SessionLotteryValue[] lotteryValues;
    private SessionStorage storage;


    @Override
    public SessionOptimizerSolution optimize(OptimizationData data) {
        if (data != null) {
            this.data = data;
        } else {
            return null;
        }

        // Temporary solution: get the first session storage.
        storage = data.getStorages().iterator().next();

        evaluateSessions();
        return constructSolution();
    }

    private void evaluateSessions() {
        Set<String> sessions = data.getSessions();
        lotteryValues = new SessionLotteryValue[sessions.size() * SessionPlacement.values().length];

        int i = 0;
        for (String session : sessions) {
            double localSize = (double) data.getSessionProperties(session).localSize;
            for (SessionPlacement placement : SessionPlacement.values()) {
                double utility = 0;
                if(localSize>0){
                    switch(placement){

                        case LOCAL:
                            utility = evaluateUtility(session, placement, storage, data) / localSize;
                            break;
                        case BOTH:
                            utility = evaluateUtility(session, placement, storage, data) / localSize;
                            break;
                        default:
                            utility = evaluateUtility(session, placement, storage, data);

                    }

                }
                lotteryValues[i] = new SessionLotteryValue(session, placement, utility);
                i++;
            }
        }

        Arrays.parallelSort(lotteryValues);
    }

    private SessionOptimizerSolution constructSolution() {
        SessionOptimizerSolution solution = new SessionOptimizerSolution();
        long localCapacity = data.localCapacity;
        long remoteCapacity = data.getStorageProperties(storage).capacity;
        double value = 0;

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
                            value = value + lottery.value * localSize;
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
                            value = value + lottery.value * localSize;
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
