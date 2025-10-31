package nars.deriver.op.time;

import jcog.data.set.ArrayHashSet;
import jcog.decide.Roulette;
import nars.Deriver;
import nars.NAL;
import nars.Op;
import nars.Term;
import nars.deriver.op.DerivedOccurrence;
import nars.premise.NALPremise;
import nars.term.atom.Bool;
import nars.term.util.Image;
import nars.time.Tense;
import nars.time.TimeGraph;
import nars.truth.MutableTruthInterval;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.random.RandomGenerator;

import static java.lang.Math.abs;
import static nars.NAL.occSolver.TIMEGRAPH_DITHER_EVENTS_INTERNALLY;
import static nars.Op.TIMELESS;
import static nars.Op.different;
import static nars.deriver.op.DerivedOccurrence.Belief;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

public final class UltimateOccSolver implements OccSolver {

    private final Deriver d;
    private final int timeResolution;

    public UltimateOccSolver(Deriver d) {
        this.d = d;
        this.timeResolution = d.timeRes();
    }

    public @Nullable Pair<Term, MutableTruthInterval> solve(NALPremise premise, Term x, MutableTruthInterval w, byte punc, DerivedOccurrence solver) {
        return new OptimizedTimeGraph(d.rng).solve(premise, x, w, punc, solver);
    }

    private class OptimizedTimeGraph extends TimeGraph {

        private int minPatternVolume, ttl;

        OptimizedTimeGraph(RandomGenerator rng) {
            super(rng);
        }

        // Calculates a score for an event based on its properties and relevance to the target
        private static float eventScore(Event event, int idealVolume, int idealStructure) {
            float goodness = 1;
            if (event instanceof Absolute) goodness += 1;
            if (!event.id.TEMPORAL_VAR()) goodness += 0.5f;
            float badness = abs(idealVolume - event.id.complexity()) + different(idealStructure, event.id.struct());
            return ((1 + goodness) / (1 + 0.5f * badness));
        }

        private void learnTB(Term T, Term B, @Nullable MutableTruthInterval truth, boolean taskOrBelief) {
            if (taskOrBelief)
                learn(T, B, truth);
            else
                learn(B, T, truth);
        }

        /**
         * Solves for the occurrence time of a term using the graph
         */
        private Pair<Term, MutableTruthInterval> solve(NALPremise p, Term x, MutableTruthInterval w, byte punc, DerivedOccurrence solver) {
            Term T = p.from(), B = p.to();
            //var pn = new short[2];
//            var negWrap = false; //OccSolver.negWrap(x, T, B, pn);
//            if (negWrap)
//                x = x.neg();

            var taskOrBelief = solver.taskOrBelief(d);
            this.minPatternVolume = (int) (NAL.occSolver.TIMEGRAPH_DEGENERATE_SOLUTION_THRESHOLD_FACTOR * x.complexity());
            if (solver.relative()) {
                var Y = solveRelative(x, T, B, w, taskOrBelief);
                if (Y != null) {
                    var y = Y.getOne();
                    var rw = Y.getTwo();
                    if (rw != null && rw.s != TIMELESS && termValid(y, punc))
                        return Y; //return negWrap ? pair(y.neg(), rw) : Y;

//                    x = y;
                }
            } else {
                var y = solveAbsolute(x, T, B, w, solver);
                if (y != null && !(y instanceof Bool)) {
                    if (termValid(y, punc))
                        return pair(y, w); //return pair(negWrap ? y.neg() : y, w);

//                    x = y;
                }
            }
            //return pair(x, w);
            return null; //TODO catch failures here -> new TimeGraphTest cases
        }

        private static boolean termValid(Term x, byte punc) {
            return Op.QUESTION_OR_QUEST(punc) || !x.TEMPORAL_VAR();
        }

        /** Solves for the occurrence time of a term in the relative case */
        private @Nullable Pair<Term, MutableTruthInterval> solveRelative(Term x, Term T, Term B, MutableTruthInterval truth, boolean taskOrBelief) {
            var s = solve(x, T, B, taskOrBelief, truth, true);
            if (s != null) {
                var i = s.getTwo();
                if (i == null)
                    return pair(s.getOne(), null);
                if (solveRelativeReoccurr(truth, i))
                    return pair(s.getOne(), truth);
            }
            return null;
        }

        private boolean solveRelativeReoccurr(MutableTruthInterval truth, long[] i) {
            return i[0] != TIMELESS && truth.reoccurr(i[0], i[1], d.now(), d.nar);
        }

        /**
         * resolves a unique Term, or null
         */
        @Nullable
        private Term solveAbsolute(Term x, Term T, Term B, @Nullable MutableTruthInterval truth, DerivedOccurrence solver) {
            var s = solve(x, T, B, solver != Belief, truth, false);
            if (s != null) {
                var ss = s.getOne();
                if (!ss.equals(x)) return ss;
            }
            return null;
        }

        /**
         * Learns about the occurrence times of terms based on their temporal properties and truth values
         */
        private void learn(Term firstTerm, Term secondTerm, @Nullable MutableTruthInterval w) {
            Event e = w != null ? know(firstTerm, w.s, w.e) : know(firstTerm);
            rewrite(e);

            if (!firstTerm.equals(secondTerm))
                rewrite(know(secondTerm));
        }

        /**
         * Finds a solution for the occurrence time of a term using the graph-based approach
         */
        private @Nullable Pair<Term, long[]> solve(Term x, Term T, Term B, boolean taskOrBelief, MutableTruthInterval truth, boolean termOnly) {
            learnTB(T, B, truth, taskOrBelief);

            rewrite(know(x));

            return when(x, termOnly);
        }

        /**
         * Selects an event from a set of events based on a calculated score
         */
        private Event selectEvent(ArrayHashSet<Event> events) {
            var target = this.target;
            var idealVolume = target != null ? target.complexity() : 1;
            var idealStructure = target != null ? target.struct() : 0;
            int count = events.size();
            var scores = new float[count];
            for (var i = 0; i < count; i++)
                scores[i] = eventScore(events.get(i), idealVolume, idealStructure);

            return events.get(Roulette.selectRoulette(rng, scores));
        }

        // Propagates knowledge about a term's occurrence time in the graph

        // Creates an alias between an event and a term in the graph
        private boolean alias(Event e, Term t) {
            if (t.CONDABLE() && !t.equals(e.id)) {
                link(e, t);
                return true;
            }
            return false;
        }

        /**
         * Determines the occurrence time of a term based on the graph
         */
        private @Nullable Pair<Term, long[]> when(Term x, boolean termOnly) {
            this.ttl = NAL.occSolver.TIMEGRAPH_ITERATIONS;
            solve(x, termOnly);
            var e = solution(x);
            if (termOnly && e == null) return null;
            return pair(e != null ? e.id : x, termOnly ?
                new long[]{e.start(), e.end()} : null);
        }

        private @Nullable Event solution(Term x) {
            var ss = this.solutions;
            return switch (ss.size()) {
                case 0 -> null;
                case 1 -> ss.first();
                default -> selectEvent(ss);
            };
        }

        /**
         * Rewrites an event in the graph using term transformations
         */
        private void rewrite(Event e) {
            var x = e.id;
            alias(e, d.unify.retransform(x).normalize());
            alias(e, Image.imageNormalize(x));
        }

        @Override
        public long t(long time) {
            return TIMEGRAPH_DITHER_EVENTS_INTERNALLY ? Tense.dither(time, timeResolution) : time;
        }

        @Override
        protected long tExternal(long time) {
            return NAL.occSolver.TIMEGRAPH_DITHER_EVENTS_EXTERNALLY ? Tense.dither(time, timeResolution) : time;
        }

        @Override
        protected int tExternal(int time) {
            return NAL.occSolver.TIMEGRAPH_DITHER_EVENTS_EXTERNALLY ? Tense.dither(time, timeResolution) : time;
        }

        @Override
        protected boolean solution(Event e) {
            return --ttl > 0;
        }

        @Override
        protected boolean solutionValid(Event e) {
            if (!super.solutionValid(e)) return false;
            var v = e.id.complexity();
            return v >= minPatternVolume && v <= d.complexMax;
        }

        @Override
        protected int occToDT(long x) {
            return Tense.dither(super.occToDT(x), timeResolution);
        }
    }

}