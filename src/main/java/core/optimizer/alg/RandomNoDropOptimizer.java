package core.optimizer.alg;

import core.optimizer.OptimizationData;
import core.optimizer.SessionOptimizerSolution;
import core.optimizer.SessionPlacement;
import core.storage.SessionStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Iterates through the set of sessions:
 * Selects a random SessionPlacement from the set of valid ones
 * (valid meaning the session will fit).
 * This optimizer does not allow dropping of sessions unless they don't fit anywhere.
 *
 * @author Sebastian Lindholm
 */
public class RandomNoDropOptimizer extends UtilityBasedOptimizer {

    private OptimizationData data;
    private SessionStorage storage;

    @Override
    public SessionOptimizerSolution optimize(OptimizationData data) {
        if (data != null) {
            this.data = data;
        } else {
            return null;
        }

        storage = data.getStorages().iterator().next();

        return generateSolution();
    }

    private SessionOptimizerSolution generateSolution() {
        Random rng = new Random();
        SessionOptimizerSolution solution = new SessionOptimizerSolution();
        double value = 0;
        long localCapacity = data.localCapacity;
        long remoteCapacity = data.getStorageProperties(storage).capacity;
        List<SessionPlacement> validPlacements = new ArrayList<SessionPlacement>();

        for (String session : data.getSessions()) {
            SessionPlacement placement;

            boolean localValid = localCapacity >= data.getSessionProperties(session).localSize;
            boolean remoteValid = remoteCapacity >= data.getSessionProperties(session).remoteSize;

            //validPlacements.add(SessionPlacement.DROP);
            if (localValid) {
                validPlacements.add(SessionPlacement.LOCAL);
            }
            if (remoteValid) {
                validPlacements.add(SessionPlacement.REMOTE);
                if (localValid) {
                    validPlacements.add(SessionPlacement.BOTH);
                }
            }

            if (validPlacements.size() > 0)
                placement = validPlacements.get(rng.nextInt(validPlacements.size()));
            else
                placement = SessionPlacement.DROP;

            switch (placement) {

                case BOTH:
                    localCapacity = localCapacity - data.getSessionProperties(session).localSize;
                    remoteCapacity = remoteCapacity - data.getSessionProperties(session).remoteSize;
                    break;
                case REMOTE:
                    remoteCapacity = remoteCapacity - data.getSessionProperties(session).remoteSize;
                    break;
                case LOCAL:
                    localCapacity = localCapacity - data.getSessionProperties(session).localSize;
                    break;
                default:
                    placement = SessionPlacement.DROP;
                    break;

            }

            value = value + evaluateUtility(session, placement, storage, data);
            solution.putNewPlacement(session, placement);
            validPlacements.clear();
        }

        solution.setValue(value);

        return solution;
    }

}
