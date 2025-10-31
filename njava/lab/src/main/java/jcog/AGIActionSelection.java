package jcog;

import jcog.data.bit.MetalBitSet;
import jcog.data.list.Lst;

import java.util.List;

/** from: https://gist.github.com/PtrMan/6606fafe8fa074de8bbbecd2089091cf#file-agiactionselection-java-L1 */
public class AGIActionSelection {
    
    private static int asInt(boolean b) {
        return b ? 1 : -1;
    }

    // is the goal in the state fullfilled?
    private static boolean fulfilled(Goal goal, State state) {
        double threshold = 0.1; // threshold above which the sdr overlap of the goal and world state is archived

        double sdrOverlapRatio = Sdr.overlap(goal.worldStateSdr, state.worldStateSdr);
        return sdrOverlapRatio >= threshold;
    }

    public static void main(String[] args) {
        Predictor predictor0 = new Predictor();
        predictor0.predictions = 5;
        predictor0.predictionsPlus = 2;

        System.out.println(predictor0);

        List<State> states = List.of(
            new State(new Sdr(true, true, false, false, true)),
            new State(new Sdr(false, false, false, true, true))
        );

        List<Goal> goals = new Lst<>();

        Goal g = new Goal();
        g.priority = 0.1;
        g.deadline = 5.0; // is time - TODO< add time class >
        g.worldStateSdr = new Sdr(false, false, true, true, true);
        goals.add(g);


        ///////////////
        // search for best state which has to be realized
        //
        // done by searching for the highest expectation

        double highestStateUtility = Double.NEGATIVE_INFINITY;
        int bestState = -1; // index of the state with the highest utility - is chosen to be realized in the next step

        int nStates = states.size();
        for (int stateIdx = 0; stateIdx < nStates; stateIdx++) {
            State iState = states.get(stateIdx);

            double maxH = 10.0f; // maximal horizon
            double sum = 0.0;

            System.out.println("state: " + iState);

            for (Goal iGoal : goals) { // compute archived*utility for each goal

                double timeOf = 4.0f;

                double horizon = iGoal.deadline - timeOf;
                double urgency = horizon / maxH;

                double utility = iGoal.priority * urgency;

                double archived = asInt(fulfilled(iGoal, iState));
                sum += archived * utility;

                System.out.println("\t" + iGoal + " util=" + utility);
            }

            // compute product of likelihood by predictors
            double productOfLikelihoods = 1.0;
            {
                // TODO enumerate over all predictors
                productOfLikelihoods *= predictor0.retLikelihood();
            }

            double expectedValue = productOfLikelihoods * sum;

            System.out.println("\t\tvalue= " + expectedValue);

            //TODO Decider (ex: Softmax, Epsion Greedy etc
            if (expectedValue > highestStateUtility) {
                // found new state which has to get realized by the system
                bestState = stateIdx;
            }
        }

        System.out.println("bestState= " + states.get(bestState));
        // TODO< realize state with ops >
    }

    /** predictor as described in paper "Predictive Heuristics for Decision-Making   in Real-World Environments" page 7 */
    static class Predictor {
        long predictionsPlus; // number of successful predictions
        long predictions; // number of predictions

        public double retFreq() {
            return (double) predictionsPlus / predictions;
        }

        double retConf() {
            return predictions / (predictions + 1.0);
        }

        public double retLikelihood() {
            return retConf() * (retFreq() - 0.5) + 0.5;
        }

        @Override
        public String toString() {
            return "Predictor{" +
                    "freq=" + retFreq() +
                    ", conf=" + retConf() +
                    ", lkli=" + retLikelihood()
                    + "}";
        }
    }

    public static class Goal {
        public double priority; // between 0.0 and 1.0

        public double deadline;


        // sdr which contains the worldstate to be archived
        Sdr worldStateSdr;

        @Override
        public String toString() {
            return worldStateSdr +
                    "(priority=" + priority +
                    ", deadline=" + deadline +
                    ')';
        }
    }

    // represents state which has to be archived by the proto-AGI system
    static class State {
        final Sdr worldStateSdr; // world representation as Sdr of this state

        State(Sdr worldStateSdr) {
            this.worldStateSdr = worldStateSdr;
        }

        @Override
        public String toString() {
            return worldStateSdr.toString();
        }
    }

    static class Sdr {
        public final MetalBitSet bits;

        Sdr(boolean... bits) {
            this(MetalBitSet.bits(bits));
        }

        Sdr(MetalBitSet bits) {
            this.bits = bits;
        }

        public static Sdr and(Sdr a, Sdr b) {
            return new Sdr(a.bits.and(b.bits));
        }

        public static double overlap(Sdr a, Sdr b) {
            return (double) and(a, b).bits.count(true) / a.bits.cardinality();
        }

        @Override
        public String toString() {
            return bits.toString();
        }
    }
    
    /* commented because not used
    
    // commented because not used
    //static interface Stringfiable {
    //    String retNalString();
    //}
    
    enum EnumTermType {
        COMPOUND,
        ATOM, // atomic - like a named term or interval
    }
    
    static interface Term {
        EnumTermType retTermType();
    }
    
    static interface AtomicTerm extends Term {}
    
    static interface AbstractNamedAtomTerm extends AtomicTerm {
        String retName();
    }
    
    static interface AbstractCompoundTerm extends Term {
        // returns type as string, example is &/ for sequence of NAL
        String retType();
        
        Term at(int idx);
    }
    
    static class CompoundTerm implements AbstractCompoundTerm {
        public CompoundTerm(String type, List<Term> components) {
            this.type = type;
            this.components = components;
        }
        
        public String retType() {
            return type;
        }
        
        public Term at(int idx) {
            return components.get(idx);
        }
        
        public EnumTermType retTermType() {
            return EnumTermType.COMPOUND;
        }
        
        private final String type;
        private final List<Term> components;
    }
    
    static class NamedAtomTerm implements AbstractNamedAtomTerm {
        public NamedAtomTerm(String name) {
            this.name = name;
        }
        
        public EnumTermType retTermType() {
            return EnumTermType.ATOM;
        }
        
        public String retName() {
            return name;
        }
        
        private String name;
    }
    
    static class SdrAtomTerm implements AtomicTerm {
        public SdrAtomTerm(Sdr sdr) {
            this.sdr = sdr;
        }
        
        public EnumTermType retTermType() {
            return EnumTermType.ATOM;
        }
        
        public String retName() {
            return "SDR";
        }
        
        private final Sdr sdr;
    }
    
    */

}