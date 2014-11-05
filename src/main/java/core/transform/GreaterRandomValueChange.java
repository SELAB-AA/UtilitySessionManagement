package core.transform;

import core.optimizer.OptimizationData;
import core.optimizer.SessionProperties;

import java.util.Random;

/**
 * Changes the value of each session randomly according to the following:
 * 50% chance of no change.
 * 50% chance of a change:
 * 50% chance of a 10% value increase.
 * 50% chance of a 10% value decrease.
 * <p>
 * Transforms are run prior to every optimization.
 *
 * @author Sebastian Lindholm
 */
public class GreaterRandomValueChange implements Transform {

        @Override
        public void doTransform(OptimizationData data) {
            Random rng = new Random();
            double val;

            for (String session : data.getSessions()) {
                SessionProperties properties = data.getSessionProperties(session);

                val = rng.nextDouble();

                if (val < 0.50) {
                    val = rng.nextDouble();
                    if (val < 0.5) {
                        properties.value = properties.value * 1.1;
                    } else {
                        properties.value = properties.value * 0.9;
                    }
                }
            }
        }


}
