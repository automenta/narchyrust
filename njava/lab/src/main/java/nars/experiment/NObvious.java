package nars.experiment;

import jcog.Fuzzy;
import jcog.Util;
import nars.$;
import nars.NAL;
import nars.Term;
import nars.game.Game;
import nars.game.reward.Reward;
import nars.task.DerivedTask;
import nars.term.atom.Int;

import java.util.Arrays;
import java.util.function.Predicate;

import static nars.Op.GOAL;

public class NObvious extends Game {

    float momentum = 0;

    private static final boolean printGood = true, printBad = true;
    private static final boolean TRACE = printGood || printBad;

    final float[] target, current;

    public NObvious(String id, float... target) {
        super(id);
        assert(target.length > 0);
        this.target = target;
        this.current = new float[target.length];
        Arrays.fill(current, Float.NaN);
    }
    @Override
    protected void init() {
        if (TRACE) {
            NAL.DEBUG = true;
            //NAL.DEBUG_DERIVED_NALTASKS = true;
        }

//        focus().input = new DirectTaskBuffer();
        int n = target.length;
        for (int i = 0; i < n; i++) {
            int I = i;
            Term x = n> 1 ? $.p(id, Int.i(i)) : $.atomic(id + "a");
            action(x, v -> {
                if (v == v) {
                    if (current[I]!=current[I])
                        return current[I] = v;
                    else
                        return current[I] = Util.lerpSafe(momentum, v, current[I]);
                } else
                    return Float.NaN;
            });

            if (TRACE) {
                int dursBefore = 8;
                int dursAfter = 8;
                float fThresh =
                        //0.5f;
                        0.75f;

                Predicate<Term> xEquals = x.equals();
                focus().onTask(t -> {
                    //if (t instanceof DerivedTask T) {
                    if (t instanceof DerivedTask T) {
                        if (!T.isInput()) {
                            //long now = nar.time();
                            //long dur = (long) dur();
                            /*if (T.intersects(now - dur * dursBefore, now + dur * dursAfter))*/ {
                            //if (T.end() >= now - dur * dursBefore) {
                                if (xEquals.test(t.term())) {
                                    float s = similarity(target[I], T.freq());

                                    boolean good = s >= fThresh;
                                    boolean bad = s < fThresh;
                                    if (good || bad) {
                                        synchronized (this) {
                                            if ((good && printGood) || (bad && printBad)) {
                                                System.out.println(good ? "GOOD" : "BAD");
                                                nar.proofPrint(T);
                                                System.out.println();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }, GOAL);
            }
        }

        Reward R = reward($.atomic(id + "r"), this::similarity);


    }

    /**
     * Hamming distance, with NaN handling
     */
    private float similarity() {
        int n = target.length;
        float sim = 0;
        for (int i = 0; i < n; i++)
            sim += similarity(i);
        return sim / n;
    }

    private float similarity(int i) {
        float c = current[i];
        return (c!=c) ? 0 : similarity(target[i], c);
    }

    protected float similarity(float t, float c) {
        return (float) Fuzzy.equals(t, c);
        //return (float) (Util.lerp(polarity(t), 0.5f, 1) * (float) Fuzzy.equals(t, c));
    }


}