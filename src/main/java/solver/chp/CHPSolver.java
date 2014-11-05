package solver.chp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CHPSolver {

    private static final Logger logger = LoggerFactory.getLogger(CHPSolver.class);

    private final List<List<Configuration>> input;
    private final long[] constraints;
    private final int setCutoff;

    public static class Configuration implements Comparable<Configuration> {
        public double value = 0;
        public Integer group = null;
        public long[] resources;
        public long aggregateResources = 0;
        public Map<Integer, Configuration> groups = new HashMap<>();
        private Double varProduct = null;

        public Configuration(double value, long[] resources) {
            this.value = value;
            this.resources = resources;
            for (int i = 0; i < resources.length; i++) {
                aggregateResources += resources[i];
            }
        }


        public void setGroup(int group){
            this.group = group;
            groups.put(group, this);
        }


        public Configuration(Configuration a, Configuration b) {
            this.value = a.value + b.value;
            if (a.resources.length == b.resources.length) {
                this.resources = new long[a.resources.length];
                for (int i = 0; i < a.resources.length; i++) {
                    this.resources[i] = a.resources[i] + b.resources[i];
                }
            } else {
                throw new CHPException("Attempting to merge configurations with different amount of resource constraints.");
            }
            this.aggregateResources = a.aggregateResources + b.aggregateResources;
            this.groups.putAll(a.groups);
            this.groups.putAll(b.groups);
            /*
            Set<Integer> intersection = new HashSet<>(a.groups.keySet());
            intersection.retainAll(b.groups.keySet());

            for(int group : intersection){
                Configuration conf = a.groups.get(group);
                this.value -= conf.value;
                this.aggregateResources -= conf.aggregateResources;
                for (int i = 0; i < this.resources.length; i++) {
                    this.resources[i] -= conf.resources[i];
                }
            }
            */
        }

        @Override
        public int compareTo(Configuration o) {
            if(this.value > o.value && this.aggregateResources<o.aggregateResources)
                return -1;
            if(this.value < o.value && this.aggregateResources>o.aggregateResources)
                return 1;

            return 0;
        }

        public double getVARProduct(){
            if(varProduct==null){
                varProduct = value*aggregateResources;
            }

            return varProduct;
        }

        @Override
        public String toString(){
            StringBuilder builder = new StringBuilder();

            builder.append("(");
            builder.append(value);
            builder.append(",");
            for(long resource:resources) {
                builder.append(resource);
                builder.append(",");
            }
            builder.append("[");
            builder.append(aggregateResources);
            builder.append(",");
            builder.append(getVARProduct());
            builder.append("])");

            return builder.toString();
        }
    }

    private static class CHPException extends RuntimeException {
        public CHPException(String msg){
            super(msg);
        }
    }

    public CHPSolver(List<List<Configuration>> input, long[] constraints, int setCutoff){
        this.input = input;
        this.constraints = constraints;
        this.setCutoff = setCutoff;
    }

    public List<Configuration> solve(){
        // Step 1: Minimize input set
        int group = 0;
        for(List<Configuration> set : input){
            minimize(set);

            for(Configuration root : set) {
                root.setGroup(group);
            }
            group++;

        }

        // Step 2: Preprocess
        sortBySize(input);

        // Step 5: Initialize set of partial solutions
        List<Configuration> partialSolutions;
        if(input.size()==0){
            return null;
        }
        else {
            partialSolutions = input.get(0);
            input.remove(partialSolutions);
        }

        // Step 6: The compositional computations
        for(List<Configuration> set : input) {
            filter(partialSolutions, setCutoff);
            filter(set, setCutoff);
            partialSolutions = combine(partialSolutions, set, constraints);
        }

        // Step 7: Postprocessing
        //greedyPostProcessing(partialSolutions);

        return partialSolutions;
    }

    private void filter(List<Configuration> confs, int cardinality) {
        Comparator<Configuration> varComparator = (o1, o2) -> {
            double value = o1.getVARProduct()-o2.getVARProduct();
            if(value > 0)
                return 1;
            if(value < 0)
                return -1;

            return 0;
        };

        Collections.sort(confs, varComparator);

        /*
        System.out.println("Sorting values:");
        for(Configuration conf : confs){
            System.out.println(conf);
        }
        */

        List<Configuration> result = new ArrayList<>();
        if (confs.size() > 0) {
            Configuration first = confs.get(0);
            Configuration last = confs.get(confs.size()-1);
            double minVal = first.getVARProduct();
            double maxVal = last.getVARProduct();
            double distance = maxVal - minVal;
            result.add(first);
            result.add(last);
            confs.remove(first);
            confs.remove(last);

            if (cardinality > 2) {
                double step = distance / (cardinality - 1);
                int numSteps = cardinality - 2;
                double currentValue = minVal;
                double[] values = new double[numSteps];

                for(int i=0; i<values.length; i++){
                    currentValue += step;
                    values[i] = currentValue;
                }

                for(double value : values) {
                    if(!confs.isEmpty()) {
                        Configuration nearest = binarySearch(confs, value);
                        result.add(nearest);
                        confs.remove(nearest);
                    } else {
                        break;
                    }
                }
            }
        }

        /*
        System.out.println("Picked " + result.size() + " values:");
        for(Configuration conf : result){
            System.out.println(conf);
        }
        */
        confs.addAll(result);
        confs.retainAll(result);
    }

    private Configuration binarySearch(List<Configuration> list, double value){
        int first = 0;
        int last = list.size()-1;

        /*
        System.out.println("Searching for "+ value + " in:");
        for(Configuration conf : list)
            System.out.println(conf);
        */

        while(first<last-1){
            int middle = (first+last)/2;
            Configuration a = list.get(first);
            Configuration b = list.get(last);
            Configuration c = list.get(middle);
            //System.out.println("Found configuration " + c + " with index " + middle);

            if(value<=a.getVARProduct()){
                last = first;
            }

            else if(value>=b.getVARProduct()){
                first = last;
            }

            else if(value <= c.getVARProduct()){
                last = middle;
            }

            else {
                first = middle;
            }
        }

        Configuration a = list.get(first);
        Configuration b = list.get(last);
        double distanceA = Math.abs(a.getVARProduct()-value);
        double distanceB = Math.abs(b.getVARProduct()-value);

        Configuration result;
        if(distanceA<=distanceB)
            result = a;
        else
            result = b;

        //System.out.println("Result: " + result);

        return result;
    }

    private List<Configuration> combine(List<Configuration> a, List<Configuration> b, long[] constraints){
        List<Configuration> results = new ArrayList<>();
        for(Configuration confA : a){
            for(Configuration confB : b){
                Configuration compound = new Configuration(confA,confB);
                //System.out.println(confA + " + " + confB + " = " + compound);

                if(isFeasible(compound)){
                    boolean isDominated = false;

                    List<Configuration> tempResults = new ArrayList<>(results);
                    for(Configuration result : tempResults){
                        int comparison = compound.compareTo(result);
                        if(comparison<0){
                            //System.out.println(compound+" dominates "+result);
                            results.remove(result);
                        }
                        else if(comparison>0){
                            //System.out.println(result+" dominates "+compound);
                            isDominated = true;
                            break;
                        }
                    }

                    if(!isDominated)
                        results.add(compound);
                }
            }
        }

        return results;
    }

    private void minimize(List<Configuration> confs){
        List<Configuration> results = new ArrayList<>();

        for(Configuration conf : confs){
            boolean isDominated = false;
            List<Configuration> tempResults = new ArrayList<>(results);
            for(Configuration result : tempResults) {
                int comparison = conf.compareTo(result);
                if (comparison < 0) {
                    //System.out.println(conf+" dominates "+result);
                    results.remove(result);
                } else if (comparison > 0) {
                    //System.out.println(result+" dominates "+conf);
                    isDominated = true;
                    break;
                }
            }

            if(!isDominated)
                results.add(conf);
        }

        //System.out.println("Minimizing set: " + confs.size() +" => " + results.size());

        confs.retainAll(results);
    }

    private boolean isFeasible(Configuration conf){
        if(conf.resources.length==constraints.length){
            for(int i=0; i<constraints.length; i++){
                if(conf.resources[i]>constraints[i])
                    return false;
            }
            return true;
        }
        else {
            throw new CHPException("Encountered configuration with invalid amount of resources.");
        }
    }

    private void sortBySize(List<List<Configuration>> list){
        Comparator<List<Configuration>> sizeComparator = new Comparator<List<Configuration>>() {
            @Override
            public int compare(List<Configuration> o1, List<Configuration> o2) {
                return o1.size()-o2.size();
            }
        };

        Collections.sort(list, sizeComparator);
    }

    /*
    private void greedyPostProcessing(List<Configuration> input){
        List<Configuration> results = new ArrayList<>(input);

        Comparator<Configuration> greedyComparator = new Comparator<Configuration>() {
            @Override
            public int compare(Configuration o1, Configuration o2) {
                double val;
                if(o1.aggregateResources<=0){
                    if(o2.aggregateResources<=0){
                        val = o1.value-o2.value;
                    }
                    else return -1;
                }
                else if(o2.aggregateResources<=0){
                    return 1;
                }
                else {
                    val = (o1.value/(double)o1.aggregateResources)-(o2.value/(double)o2.aggregateResources);
                }

                if(val > 0)
                    return -1;
                else if(val < 0)
                    return 1;
                else
                    return 0;
            }
        };

        Set<Configuration> confs = new HashSet<>();

        for(Configuration conf : input){
            confs.addAll(conf.groups.values());
        }

        List<Configuration> sortedConfs = new ArrayList<>(confs);
        Collections.sort(sortedConfs,greedyComparator);

        for(Configuration upgrade : sortedConfs){
            List<Configuration> tempInput = new ArrayList<>(input);
            for(Configuration solution : tempInput){
                Configuration newSolution = new Configuration(solution, upgrade);
                if(isFeasible(newSolution) && newSolution.value>solution.value){
                    //System.out.println("Replacing "+solution.groups.get(upgrade.group)+" in "+solution+" with "+upgrade+" => "+newSolution);
                    input.remove(solution);
                    input.add(newSolution);
                }
            }
        }
    }
    */
}
