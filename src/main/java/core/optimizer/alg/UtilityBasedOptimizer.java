package core.optimizer.alg;


import core.optimizer.*;
import core.storage.SessionStorage;

/**
 * Provides a utility function for utility based optimizers.
 *
 * @author Sebastian Lindholm
 */
public abstract class UtilityBasedOptimizer implements SessionOptimizer {

    /**
     * Calculates the value of the utility function.
     *
     * @param session   The session to evaluate.
     * @param placement The lottery to evaluate.
     * @return The value of the function.
     */
    public double evaluateUtility(String session, SessionPlacement placement, SessionStorage storage, OptimizationData data) {

        SessionProperties sessionProperties = data.getSessionProperties(session);
        StorageProperties storageProperties = data.getStorageProperties(storage);

        double remoteSize = ((double) sessionProperties.remoteSize) / 1048576.0D;
        double value = 0.0D;
        double localReliability = 1;

        if (data.localMTTF > 0)
            localReliability = Math.exp(-(data.optimizerPeriod / data.localMTTF));

        switch (placement) {

            case DROP:
                break;

            case LOCAL:
                value = sessionProperties.value * localReliability;
                break;

            case BOTH:
                value = sessionProperties.value - (storageProperties.writeCost + storageProperties.readCost * (1.0D - localReliability)) - storageProperties.storageCost * data.optimizerPeriod * remoteSize;
                break;

            case REMOTE:
                value = sessionProperties.value - (storageProperties.readCost + storageProperties.writeCost) - storageProperties.storageCost * data.optimizerPeriod * remoteSize;
                break;

            default:
                System.err.println("Evaluating utility for invalid placement: " + placement + ".");
                break;

        }

        return value;

    }

}
