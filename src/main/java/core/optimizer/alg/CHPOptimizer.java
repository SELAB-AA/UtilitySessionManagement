package core.optimizer.alg;

import core.optimizer.OptimizationData;
import core.optimizer.SessionOptimizerSolution;
import core.optimizer.SessionPlacement;
import core.optimizer.SessionProperties;
import core.storage.SessionStorage;

import solver.chp.CHPSolver;
import solver.chp.CHPSolver.Configuration;

import java.util.*;

public class CHPOptimizer extends UtilityBasedOptimizer {

    private static final int LOCAL = 0;
    private static final int REMOTE = 1;
    private static final int SET_CUTOFF = 32;

    private Map<Configuration, SessionLotteryValue> lotteries = new HashMap<>();

    @Override
    public SessionOptimizerSolution optimize(OptimizationData data) {
        SessionStorage storage = data.getStorages().iterator().next();

        // Get constraints
        long[] constraints = new long[2];
        constraints[LOCAL] = data.localCapacity;
        constraints[REMOTE] = data.getStorageProperties(storage).capacity;

        // Initialize input data
        List<List<Configuration>> input = new ArrayList<>(data.getSessions().size());
        for(String session : data.getSessions()){
            List<Configuration> confSet = new ArrayList<>(4);
            SessionProperties properties = data.getSessionProperties(session);
            for(SessionPlacement placement : SessionPlacement.values()){
                double utility = evaluateUtility(session, placement, storage, data);
                long[] resourceVector = new long[2];
                switch(placement){
                    case DROP:
                        resourceVector[LOCAL] = 0;
                        resourceVector[REMOTE] = 0;
                        break;
                    case LOCAL:
                        resourceVector[LOCAL] = properties.localSize;
                        resourceVector[REMOTE] = 0;
                        break;
                    case REMOTE:
                        resourceVector[LOCAL] = 0;
                        resourceVector[REMOTE] = properties.remoteSize;
                        break;
                    case BOTH:
                        resourceVector[LOCAL] = properties.localSize;
                        resourceVector[REMOTE] = properties.remoteSize;
                        break;
                    default:
                        break;
                }
                Configuration conf = new Configuration(utility, resourceVector);
                confSet.add(conf);
                lotteries.put(conf, new SessionLotteryValue(session, placement, utility));
            }
            input.add(confSet);
        }

        CHPSolver solver = new CHPSolver(input, constraints, SET_CUTOFF);
        List<Configuration> result = solver.solve();

        return constructSolution(result);
    }

    public SessionOptimizerSolution constructSolution(List<Configuration> configurations){
        SessionOptimizerSolution solution = new SessionOptimizerSolution();

        if(configurations!=null){
            Configuration best = null;
            for(Configuration config : configurations){
                if(best == null)
                    best = config;
                else if(best.value < config.value)
                    best = config;
            }

            if(best != null){
                solution.setValue(best.value);
                for(Configuration conf : best.groups.values()){
                    SessionLotteryValue lottery = lotteries.get(conf);
                    solution.putNewPlacement(lottery.session, lottery.placement);
                }
            }
            else return null;
        }

        return solution;
    }

}
