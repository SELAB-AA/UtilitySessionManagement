package core.predictor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegratingPredictor implements Predictor {

    private static Logger logger = LoggerFactory.getLogger(IntegratingPredictor.class);

    private Long prevPrediction = null;
    private Long error = 0L;

    @Override
    public long predictNext(long currentVal) {
        long nextPrediction, currentError;

        if (prevPrediction == null) {
            currentError = 0;
        } else {
            currentError = prevPrediction - currentVal;
        }

        error += currentError;
        nextPrediction = currentVal - error;

        logger.info("Prediction data: {} LAST, {} PREDICTED, {} ERROR.", currentVal, prevPrediction, currentError);

        prevPrediction = nextPrediction;
        return nextPrediction;
    }
}
