package solver;

import org.junit.Before;
import org.junit.Test;
import solver.LinearConstraint.Sign;
import solver.LinearObjective.Goal;
import solver.LinearProblem.LinearProblemException;
import solver.jsci.LpProblem;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.fail;

public class SolverTest {

    LinearProblem problem;

    @Before
    public void setUp() {
        // Create problem
        problem = new LpProblem()

                // Limit variables to integer values
                .setInteger("x", true)
                .setInteger("y", true)

                        // Set objective
                .addObjective(new LinearObjective()
                                .putVariable("x", 143.0)
                                .putVariable("y", 60.0)
                                .setGoal(Goal.MAX)
                )

                        // Add constraints
                .addConstraint(new LinearConstraint(Sign.LTEQ, 15000.0)
                                .putVariable("x", 120.0)
                                .putVariable("y", 210.0)
                )

                .addConstraint(new LinearConstraint(Sign.LTEQ, 4000.0)
                                .putVariable("x", 110.0)
                                .putVariable("y", 30.0)
                )

                .addConstraint(new LinearConstraint(Sign.LTEQ, 75.0)
                                .putVariable("x", 1.0)
                                .putVariable("y", 1.0)
                );
    }

    @Test
    public void test() {

        try {
            problem.solve();
        } catch (LinearProblemException e) {
            fail("Could not initialise problem.");
        }

        Map<Object, Double> result = problem.getVariableResults();
        Set<Object> variables = result.keySet();
        Double objective = problem.getObjectiveResult();

        for (Object variable : variables) {
            System.out.println(variable + " = " + result.get(variable));
        }

        System.out.println("Objective = " + objective);

    }

}
