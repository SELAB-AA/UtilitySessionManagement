package core.predictor;

public class SimplePredictor implements Predictor {

    @Override
    public long predictNext(long current) {
        return current;
    }

}
