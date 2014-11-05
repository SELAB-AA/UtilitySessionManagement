package core.transform;

import core.optimizer.OptimizationData;

/**
 * A Transform manipulates the OptimizationData object prior to each optimization.
 * It can for example adjust the session values according to the amount of accesses.
 *
 * @author Sebastian Lindholm
 */
public interface Transform {

    public void doTransform(OptimizationData data);

}
