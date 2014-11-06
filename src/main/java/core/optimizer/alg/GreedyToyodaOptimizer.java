package core.optimizer.alg;


import core.optimizer.*;
import core.storage.SessionStorage;

import java.util.*;

/**
 * Greedy based optimizer that uses the aggregate resource consumption
 * concept suggested by Toyoda.
 *
 * TODO Work in progress: Modify the Greedy optimizer so that it uses
 * the notion of aggregate resource consumption instead of plain
 * session size.
 *
 * @author Sebastian Lindholm
 */
public class GreedyToyodaOptimizer extends UtilityBasedOptimizer {

    private OptimizationData data;
    private SessionLotteryContainer[] containers;
    private SessionStorage storage;

    private static class SessionLotteryContainer implements Comparable<SessionLotteryContainer>{
        public String id;
        public ToyodaLotteryValue[] lotteries;
        public double score = 0D;

        @Override
        public int compareTo(SessionLotteryContainer o) {
            double diff = o.score-this.score;
            if(diff>0)
                return 1;
            else if(diff<0)
                return -1;
            else
                return 0;
        }
    }

    private static class ToyodaLotteryValue extends SessionLotteryValue {

        double square = 0D;
        double weightedValue = 0D;

        public ToyodaLotteryValue(String session, SessionPlacement placement, double value) {
            super(session, placement, value);
        }

        public static class ToyodaComparator implements Comparator<ToyodaLotteryValue>{
            @Override
            public int compare(ToyodaLotteryValue o1, ToyodaLotteryValue o2) {
                double diff = o2.weightedValue-o1.weightedValue;
                if(diff>0)
                    return 1;
                else if(diff<0)
                    return -1;
                else
                    return 0;
            }
        }

    }

    @Override
    public SessionOptimizerSolution optimize(OptimizationData data) {
        if (data != null) {
            this.data = data;
        } else {
            return null;
        }

        // Temporary solution: get the first session storage.
        storage = data.getStorages().iterator().next();

        return evaluateSessions();
    }

    private SessionOptimizerSolution evaluateSessions() {
        Set<String> sessions = data.getSessions();
        containers = new SessionLotteryContainer[sessions.size()];

        int i = 0;
        for (String session : sessions) {
            SessionProperties properties = data.getSessionProperties(session);
            double localSize = (double) properties.localSize;
            SessionLotteryContainer container = new SessionLotteryContainer();
            container.id = session;
            containers[i] = container;
            i++;

            container.lotteries = new ToyodaLotteryValue[SessionPlacement.values().length];
            int j = 0;
            for(SessionPlacement placement : SessionPlacement.values()) {
                double weight = 0;

                if (localSize > 0) {
                    weight = evaluateUtility(session, placement, storage, data);
                    container.score += weight;
                }

                container.lotteries[j] = new ToyodaLotteryValue(session, placement, weight);
                j++;
            }
        }

        // Sort lottery containers in descending order
        Arrays.parallelSort(containers);

        SessionOptimizerSolution solution = new SessionOptimizerSolution();
        StorageProperties storageProperties = data.getStorageProperties(storage);
        final long remoteCapacity = storageProperties.capacity;
        long remoteConsumed = 0L;
        final long localCapacity = data.localCapacity;
        long localConsumed = 0L;
        long sumOfSquares = 0L;
        double value = 0D;

        for (SessionLotteryContainer container : containers) {
            SessionProperties sessionProperties = data.getSessionProperties(container.id);

            // Update the lotteries with aggregate resource consumption
            for(ToyodaLotteryValue lottery : container.lotteries){

                double norm;

                switch(lottery.placement){

                    case LOCAL:
                        lottery.square = Math.pow(sessionProperties.localSize, 2D);
                        norm = Math.sqrt(sumOfSquares+lottery.square);

                        if(norm!=0){
                            lottery.weightedValue = lottery.value/((sessionProperties.localSize*localConsumed)/norm);
                        }
                        break;

                    case REMOTE:
                        lottery.square = Math.pow(sessionProperties.remoteSize, 2D);
                        norm = Math.sqrt(sumOfSquares+lottery.square);

                        if(norm!=0){
                            lottery.weightedValue = lottery.value/((sessionProperties.remoteSize*remoteConsumed)/norm);
                        }
                        break;

                    case BOTH:
                        lottery.square = Math.pow(sessionProperties.localSize, 2D) + Math.pow(sessionProperties.remoteSize, 2D);
                        norm = Math.sqrt(sumOfSquares+lottery.square);

                        if(norm!=0){
                            lottery.weightedValue = lottery.value/((sessionProperties.remoteSize*remoteConsumed+sessionProperties.localSize*localConsumed)/norm);
                        }
                        break;

                    case DROP:
                        lottery.weightedValue = lottery.value;
                        break;

                }
            }

            // Sort lotteries in the container in descending order
            Arrays.sort(container.lotteries, new ToyodaLotteryValue.ToyodaComparator());

            // Pick a viable lottery with the highest value
            SessionPlacement chosenPlacement = null;
            for(ToyodaLotteryValue lottery : container.lotteries){
                switch(lottery.placement){

                    case LOCAL:
                        if(localCapacity>=localConsumed+sessionProperties.localSize){
                            chosenPlacement = SessionPlacement.LOCAL;
                            localConsumed += sessionProperties.localSize;
                            sumOfSquares += lottery.square;
                            value+=lottery.value;
                        }
                        break;

                    case REMOTE:
                        if(remoteCapacity>=remoteConsumed+sessionProperties.remoteSize){
                            chosenPlacement = SessionPlacement.REMOTE;
                            remoteConsumed += sessionProperties.remoteSize;
                            sumOfSquares += lottery.square;
                            value+=lottery.value;
                        }
                        break;

                    case BOTH:
                        if(localCapacity>=localConsumed+sessionProperties.localSize && remoteCapacity>=remoteConsumed+sessionProperties.remoteSize){
                            chosenPlacement = SessionPlacement.BOTH;
                            localConsumed += sessionProperties.localSize;
                            remoteConsumed += sessionProperties.remoteSize;
                            sumOfSquares += lottery.square;
                            value+=lottery.value;
                        }
                        break;

                    case DROP:
                        chosenPlacement = SessionPlacement.DROP;
                        value+=lottery.value;
                        break;

                }

                if(chosenPlacement!=null)
                    break;

            }

            solution.putNewPlacement(container.id, chosenPlacement);
        }

        solution.setValue(value);

        return solution;
    }

}
