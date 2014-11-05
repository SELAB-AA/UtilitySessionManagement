package core.optimizer.alg;

import org.junit.Test;
import solver.LinearConstraint;
import solver.LinearObjective;
import solver.LinearProblem;
import solver.chp.CHPSolver;
import solver.jsci.LpProblem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

public class CHPSolverTest {

    private static final int L = 6;

    private static final long[][][] RESOURCES = {
            {
                    {1, 3, 1, 1, 6},
                    {1, 4, 9, 9, 3},
                    {4, 3, 9, 8, 2},
                    {4, 5, 8, 0, 6},
                    {6, 8, 3, 0, 7}
            }, {
            {0, 0, 4, 4, 2},
            {0, 0, 1, 8, 7},
            {1, 1, 6, 0, 6},
            {9, 1, 2, 2, 4},
            {8, 7, 0, 8, 2}
    }, {
            {2, 0, 5, 5, 5},
            {2, 3, 2, 6, 2},
            {3, 1, 6, 4, 7},
            {6, 7, 5, 6, 9},
            {9, 5, 9, 2, 2}
    }, {
            {0, 1, 3, 8, 0},
            {2, 2, 7, 0, 8},
            {5, 5, 6, 1, 9},
            {6, 3, 6, 9, 1},
            {7, 9, 7, 2, 3}
    }, {
            {4, 0, 7, 0, 2},
            {4, 8, 9, 0, 0},
            {5, 2, 7, 2, 0},
            {5, 5, 9, 5, 2},
            {9, 2, 2, 2, 3}
    }
    };

    private static final double[][] VALUES = {
            {7.00, 17.00, 25.00, 35.00, 36.00},
            {9.00, 10.00, 10.00, 39.00, 44.00},
            {15.00, 19.00, 20.00, 44.00, 50.00},
            {5.00, 25.00, 32.00, 37.00, 37.00},
            {24.00, 30.00, 32.00, 43.00, 44.00}
    };

    private static final long[] CONSTRAINTS = {
            25, 25, 25, 25, 25
    };

    @Test
    public void testSolveCHP(){
        List<List<CHPSolver.Configuration>> input = new ArrayList<>();
        for(int group=0; group<RESOURCES.length; group++){
            List<CHPSolver.Configuration> configSet = new ArrayList<>();
            for(int lottery=0; lottery<RESOURCES[0].length; lottery++){
                CHPSolver.Configuration config = new CHPSolver.Configuration(VALUES[group][lottery], RESOURCES[group][lottery]);
                configSet.add(config);
            }
            input.add(configSet);
        }

        CHPSolver solver = new CHPSolver(input, CONSTRAINTS, L);
        List<CHPSolver.Configuration> result = solver.solve();

        if(result!=null){
            if(result.size()>0){
                CHPSolver.Configuration best = null;
                System.out.println("Solutions:");
                for(CHPSolver.Configuration config : result){
                    System.out.println(config);
                    if(best == null){
                        best = config;
                    }
                    else if(best.value<config.value){
                        best = config;
                    }
                }

                System.out.println("Best solution: " + best);

            }
            else {
                fail("Result is empty!");
            }
        }
        else {
            fail("No result!");
        }
    }

    @Test
    public void testSolveLP(){
        LinearProblem problem = new LpProblem();
        LinearObjective objective = new LinearObjective();
        objective.setGoal(LinearObjective.Goal.MAX);
        problem.addObjective(objective);

        LinearConstraint[] resourceConstraints = new LinearConstraint[CONSTRAINTS.length];
        for(int i=0; i<CONSTRAINTS.length; i++){
            resourceConstraints[i] = new LinearConstraint(LinearConstraint.Sign.LTEQ, (double) CONSTRAINTS[i]);
            problem.addConstraint(resourceConstraints[i]);
        }

        for(int group=0; group<RESOURCES.length; group++){
            LinearConstraint oneLottery = new LinearConstraint(LinearConstraint.Sign.EQ, 1.0);
            for(int lottery=0; lottery<RESOURCES[0].length; lottery++){
                String var = String.valueOf(group)+":"+String.valueOf(lottery);
                problem.setInteger(var, true);
                objective.putVariable(var, VALUES[group][lottery]);
                oneLottery.putVariable(var, 1.0);
                for(int resource=0; resource<RESOURCES.length; resource++){
                    resourceConstraints[resource].putVariable(var, (double) RESOURCES[group][lottery][resource]);
                }
            }
            problem.addConstraint(oneLottery);
        }

        try {
            problem.solve();
        } catch (LinearProblem.LinearProblemException e) {
            e.printStackTrace();
            fail("Could not solve LP.");
        }

        System.out.println("LP solution:" + problem.getObjectiveResult());
    }
}
