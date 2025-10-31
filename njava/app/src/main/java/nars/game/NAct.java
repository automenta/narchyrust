package nars.game;

import jcog.Fuzzy;
import jcog.Is;
import jcog.TODO;
import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.pid.MiniPID;
import jcog.signal.FloatRange;
import nars.*;
import nars.game.Actions.ActionControl;
import nars.game.Actions.PushButtonControl;
import nars.game.action.AbstractAction;
import nars.game.action.AbstractGoalAction;
import nars.game.action.BiPolarAction;
import nars.game.action.GoalAction;
import nars.game.util.UnipolarMotor;
import nars.term.Compound;
import nars.term.atom.Int;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

import static nars.Op.GOAL;

/**
 * Created by me on 9/30/16.
 */
@Is({"Actuator", "Muscle", "Switch"})
public interface NAct {

    Term POS =
            $.atomic("\"+\"");
    //Int.the(+1);
    Term NEG =
            $.atomic("\"-\"");
    //Int.the(-1);

    FloatToFloatFunction ifGoalUnknownThenZero = x -> 0;
    FloatToFloatFunction ifGoalUnknownThenUnknown = x -> Float.NaN;

    /**
     * latches previous value
     */
    FloatToFloatFunction ifGoalUnknownThenPrev = x -> x;


//    /**
//     * TODO make BooleanPredicate version for feedback
//     */
//    default void actionToggle(Term t, float thresh, float defaultValue /* 0 or NaN */, float momentumOn, Runnable on, Runnable off) {
//
//
//        float[] last = {0};
//        actionUnipolar(t, (f) -> {
//
//            float f1 = f;
//            boolean unknown = (f1 != f1) || (f1 < thresh && (f1 > (1f - thresh)));
//            if (unknown) {
//                f1 = defaultValue == defaultValue ? defaultValue : last[0];
//            }
//
//            if (last[0] > 0.5f)
//                f1 = Util.lerp(momentumOn, f1, last[0]);
//
//            boolean positive = f1 > 0.5f;
//
//
//            if (positive) {
//                on.run();
//                return last[0] = 1f;
//            } else {
//                off.run();
//                return last[0] = 0f;
//            }
//        });
//    }


//    default @Nullable Truth actionToggle(@Nullable Truth d, Runnable on, Runnable off, boolean next) {
//        float freq;
//        if (next) {
//            freq = +1;
//            on.run();
//        } else {
//            freq = 0f;
//            off.run();
//        }
//
//        return $.t(freq,
//
//                nar().confDefault(BELIEF) /*d.conf()*/);
//    }

//    default NAR nar() {
//        return focus().nar;
//    }

    Focus focus();

    <A extends FocusLoop<Game>> A addAction(A c);


//    default @Nullable AbstractAction actionPWM(Term s, IntConsumer i) {
//        AbstractAction m = new GoalActionConcept(s, (b, d) -> {
//
//
//            int ii;
//            if (d == null) {
//                ii = 0;
//            } else {
//                float f = d.freq();
//                if (f == 1f) {
//                    ii = +1;
//                } else if (f == 0) {
//                    ii = -1;
//                } else if (f > 0.5f) {
//                    ii = nar().random().nextFloat() <= ((f - 0.5f) * 2f) ? +1 : 0;
//                } else if (f < 0.5f) {
//                    ii = nar().random().nextFloat() <= ((0.5f - f) * 2f) ? -1 : 0;
//                } else
//                    ii = 0;
//            }
//
//            i.accept(ii);
//
//            float f = switch (ii) {
//                case 1 -> 1f;
//                case 0 -> 0.5f;
//                case -1 -> 0f;
//                default -> throw new RuntimeException();
//            };
//
//            return f;
//        }, nar());
//        return addAction(m);
//    }

    default void actionPushButton(Term s, Runnable r) {
        actionPushButton(s, b -> {
            if (b)
                r.run();
        }, thresholdDefault());
    }

    default AbstractGoalAction actionPushButton(Term t, BooleanProcedure on) {
        return actionPushButton(t, on, thresholdDefault());
    }

    default AbstractAction actionPushButton(Term t, BooleanProcedure on, FloatSupplier thresh) {
        return actionPushButton(t, on, x -> thresh.asFloat());
    }

    default AbstractGoalAction actionPushButton(Term t, BooleanProcedure on, FloatToFloatFunction thresh) {
        return actionPushButton(t, x -> {
            on.value(x);
            return x;
        }, thresh);
    }


    default BiPolarAction actionToggle(Term l, Term r, BooleanProcedure L, BooleanProcedure R) {
        return actionToggle(l, r,
                x -> {
                    L.value(x);
                    return true;
                },
                x -> {
                    R.value(x);
                    return true;
                });
    }

    default BiPolarAction actionToggle(Term l, Term r, Runnable L, Runnable R) {
        return actionToggle(l, r,
                x -> {
                    if (x) L.run();
                },
                x -> {
                    if (x) R.run();
                }
        );
    }

    default BiPolarAction actionToggle(Term l, Term r, BooleanSupplier L, BooleanSupplier R) {
        return actionToggle(l, r,
                x -> x && L.getAsBoolean(),
                x -> x && R.getAsBoolean()
        );
    }

    /**
     * adjusts increment/decrement rate according to provided curve;
     * ex: short press moves in small steps, but pressing longer scans faster
     */
    default AbstractAction[] actionDial(Term down, Term up, FloatRange x, FloatToFloatFunction dursPressedToIncrement) {
        throw new TODO();
    }

    default BiPolarAction actionDial(Term down, Term up, FloatRange x, int steps) {
        return actionDial(down, up, x, x::set, x.min, x.max, steps);
    }

//
//    /**
//     *
//     *  determines a scalar value in range 0..1.0 representing the 'q' motivation for
//     *  the given belief and goal truth
//     */
//    @FunctionalInterface @Skill("Q_Learning") interface QFunction {
//        float q(@Nullable Truth belief, @Nullable Truth goal);
//
//        QFunction GoalFreq = (b,g)-> g != null ? g.freq() : 0f;
//        QFunction GoalFreqPosOnly = (b, g)-> {
//            if (g != null) {
//                float f = g.freq();
//                if (f >= 0.5f)
//                    return f;
//            }
//            return 0;
//        };
//
//        QFunction GoalExp = (b,g)-> g != null ? g.expectation() : 0f;
//
////        QFunction GoalExpMinBeliefExp = (b,g)-> {
////            if (b==null) {
////                return GoalExp.q(b, g);
////            } else {
////                if (g == null) {
////                    return 0; //TODO this could also be a way to introduce curiosity
////                } else {
////                    return Util.unitize((g.expectation() - b.expectation())/2f + 0.5f);
////                }
////            }
////        };
//
//    }

    /**
     * discrete rotary dial:
     * a pair of up/down buttons for discretely incrementing and decrementing a value within a given range
     *
     * @return
     */
    default BiPolarAction actionDial(Term down, Term up, FloatSupplier x, FloatProcedure y, float min, float max, int steps) {
        float delta = 1f / steps * (max - min);
        return actionStep(down, up, (c) -> {
            float before = x.asFloat();
            float next = Util.clamp(before + c * delta, min, max);
            y.value(next);
            float actualNext = x.asFloat();
            /** a significant change */
            return !Util.equals(before, actualNext, delta / 4);
        });

    }

    private static FloatToFloatFunction nanIfZero(FloatToFloatFunction f) {
        return (x) -> {
            float y = f.valueOf(x);
            if (Util.equals(y, 0, NAL.truth.FREQ_EPSILON))
                return Float.NaN;
            return y;
        };
    }

    default FloatToFloatFunction thresholdDefault() {
        return f -> NAL.BUTTON_THRESHOLD_DEFAULT;
    }

    default BiPolarAction actionToggle(Term tl, Term tr, BooleanPredicate L, BooleanPredicate R) {
        return actionToggle(tl, tr,
                () -> NAL.BUTTON_THRESHOLD_DEFAULT
                //2/3f
                //3/4f
                , L, R);
    }

    /**
     * the predicates return true if the value (whether true OR false) was accepted.  if false,
     * it indicates the value was rejected
     */
    default BiPolarAction actionToggle(Term tl, Term tr, FloatSupplier thresh, BooleanPredicate L, BooleanPredicate R) {
        BiPolarAction lr = actionBipolar(x -> x ? tr : tl,
            new BiPolarAction.Analog()
            //new BiPolarAction.DecidePolarization()
            //new BiPolarAction.PWMDecision()
            //new BiPolarAction.PowerXOR( 0.5f)
            //new BiPolarAction.Greedy()
            //new BiPolarAction.PowerXOR( 1/3f)
            //new BiPolarAction.XOR()
            //new BiPolarAction.AnalogFade(1, 0.2f)
            //new BiPolarAction.PWMThresh()
                , f -> {

                    float t = thresh.asFloat();
                    //TODO shuffled order?
                    if (f <= -t) {
                        R.accept(false);
                        return L.accept(true) ? -1 : 0;
                    } else if (f >= +t) {
                        L.accept(false);
                        return R.accept(true) ? +1 : 0;
                    } else {
                        L.accept(false);
                        R.accept(false);
                        return 0;
                    }

//                boolean lb, rb;
//                if (f <= -t) {
//                    lb = true; rb = false;
//                } else if (f >= +t) {
//                    rb = true; lb = false;
//                } else {
//
//                    lb = rb = false;
//                }
//                int lv = L.accept(lb) ? -1 : 0;
//                int rv = R.accept(rb) ? +1 : 0;
//                return lb ? lv + rv;

                }
        );
        //TODO
//        lr.pos().beliefDefault($.t(0, 0.0001f), nar());
//        lr.neg().beliefDefault($.t(0, 0.0001f), nar());

//        lr.resolution(1);
//        lr.pos().resolution(1); //HACK
//        lr.neg().resolution(1); //HACK

        return lr;
    }

    default AbstractGoalAction actionPosHalf(Term id, FloatToFloatFunction motor) {
        return action(id, Fuzzy.relu(motor));
    }

    default AbstractGoalAction actionPosHalf(Term id, FloatProcedure motor) {
        return action(id, Fuzzy.relu(x->{
            motor.value(x);
            return x;
        }));
    }

    default BiPolarAction actionBipolar(Term template, FloatToFloatFunction motor) {
        return actionBipolar(template,
                //new BiPolarAction.SimplePolarization()
                //new BiPolarAction.DecidePolarization()
                new BiPolarAction.Analog()
                //new BiPolarAction.Greedy()
                , motor);
    }

    default AbstractGoalAction actionPID(Term id, FloatSupplier current, FloatProcedure next, float min, float max) {
        MiniPID pid = new MiniPID(0.5, 0.5, 1).outRange(min, max);
        return action(id, new FloatToFloatFunction() {
            @Override
            public float valueOf(float x) {
                float c = current();
                double t = Util.lerpSafe(x, min, max);
                double n = pid.out(c, t);

                next.value((float) n);

                float cNext = current();
                return Util.normalize(cNext, min, max);
            }

            private float current() {
                return current.asFloat();
            }
        });
    }

    default AbstractGoalAction actionPIDStep(Term id, FloatSupplier current, IntConsumer step, float min, float max, FloatSupplier tolerance) {
        return actionPID(id, current, xNext -> {
            float x = current.asFloat();
            float tol = tolerance.asFloat();
            int s;
            double dx = xNext - x;
            if (dx > tol) {
                s = +1;
            } else if (dx < -tol) {
                s = -1;
            } else {
                s = 0;
            }
            step.accept(s);
        }, min, max);
    }

    /**
     * for pressing keyboard-like keys as boolean values in an array
     */
    default BiPolarAction actionBipolar(Term id, boolean[] keys, int pos, int neg, float thresh) {
        return actionBipolar(id, dy -> {
            if (dy < -thresh) {
                keys[pos] = false;
                keys[neg] = true;
                return -1f;
            } else if (dy > +thresh) {
                keys[pos] = true;
                keys[neg] = false;
                return 1f;
            } else {
                keys[pos] = false;
                keys[neg] = false;
                return 0;
            }
        });
    }

    default BiPolarAction actionBipolar(Term template, BiPolarAction.Polarization model, FloatToFloatFunction motor) {
        if (!(template instanceof Compound) || !template.containsRecursively($.varDep(1))) {
            //throw new TermException("has no query variables", template);
            template = $.inh(template, $.varDep(1));
        }

        Term t = template;
        return actionBipolar(
            posOrNeg -> t.replace($.varDep(1), posOrNeg ?
                Int.ONE : Int.NEG_ONE
                //POS : NEG
            ),
            model, motor
        );
    }

    default BiPolarAction actionBipolar(BooleanToObjectFunction<Term> s, BiPolarAction.Polarization model, FloatToFloatFunction motor) {
        return addAction(new BiPolarAction(s, model, motor));
    }

    /**
     * tri-state implemented as delta version memory of last state.
     * initial state is neutral.
     */
    default BiPolarAction actionTriState(Term cc, IntPredicate i) {

        float deadZoneFreqRadius =
                1 / 6f;

        return actionBipolar(cc, /*new BiPolarAction.PWMDecision(),*/ f -> {

            int s;
            if (f > deadZoneFreqRadius)
                s = +1;
            else if (f < -deadZoneFreqRadius)
                s = -1;
            else
                s = 0;

            if (i.test(s)) {


                //return 0f;
                return switch (s) {
                    case -1 -> -1;
                    case 0 -> 0;
                    case +1 -> +1;
                    default -> throw new RuntimeException();
                };

            }

            return 0f;

        });
//        float res = 0.5f;
//        g[0].resolution(res);
//        g[1].resolution(res);
//        return g;
    }

    default void actionBipolarSteering(Term s, FloatProcedure act) {
        float[] amp = new float[1];
        float dt = 0.1f;
        float max = 1f;
        float decay = 0.9f;
        actionTriState(s, (i) -> {
            float a = amp[0];
            float b = Util.clamp((a * decay) + dt * i, -max, max);
            amp[0] = b;

            act.value(b);

            return !Util.equals(a, b);
        });


    }

//    /**
//     * selects one of 2 states until it shifts to the other one. suitable for representing
//     * push-buttons like keyboard keys. by default with no desire the state is off.   the off procedure will not be called immediately.
//     */
//    default void actionTriState(Term s, IntConsumer i) {
//        actionTriState(s, (v) -> {
//            i.accept(v);
//            return true;
//        });
//    }

    default AbstractAction actionPushButton(Term t, BooleanPredicate on) {
        return actionPushButton(t, on, thresholdDefault());
    }

    default AbstractGoalAction actionPushButton(Term t, BooleanPredicate on, FloatToFloatFunction thresh) {
        return action(t, true, ifGoalUnknownThenZero, x -> {
            boolean posOrNeg = x >= thresh.valueOf(x);
            return on.accept(posOrNeg) ? 1 : 0;
        });
    }


    /**
     * suspect
     */
    @Deprecated
    default AbstractAction action(Term s, BiConsumer<Truth, Truth> bg) {
        return addAction(new GoalAction(s, (B, G) -> {
            bg.accept(B, G);
            return G != null ? G.freq() : Float.NaN;
        }));
    }

    default AbstractGoalAction action(Term t, FloatRange f) {
        return action(t, (FloatProcedure) f::setLerp);
    }

    default AbstractGoalAction action(Term t, FloatProcedure update) {
        return action(t, x -> {
            update.value(x);
            return x;
        });
    }


//    /** maps the action range 0..1.0 to the 0.5..1.0 positive half of the frequency range.
//     *  goal values <= 0.5 are squashed to zero.
//     *  TODO make a negative polarity option
//     */
//    default GoalActionConcept actionHemipolar(Term s, FloatToFloatFunction update) {
//        float epsilon = NAL.truth.TRUTH_EPSILON/2;
//        return actionUnipolar(s, (raw)->{
//            if (raw==raw) {
//
//                if (raw > 0.5f + epsilon) {
//                    float feedback = update.valueOf((raw - 0.5f) * 2);
//                    return feedback > (0.5f + epsilon) ? 0.5f + feedback / 2 : 0;
//                } else {
//                    float feedback = update.valueOf( 0);
//                    return 0; //override
//                }
//
//            }
//            return Float.NaN;
//        });
//    }

    default AbstractGoalAction action(Term s, FloatToFloatFunction update) {
        return action(s, true, ifGoalUnknownThenPrev, update);
    }

    default BiPolarAction actionStep(Term down, Term up, IntConsumer each) {
        return actionStep(down, up, e -> {
            each.accept(e);
            return true;
        });
    }

    default BiPolarAction actionStep(Term down, Term up, IntPredicate each) {
        return actionToggle(
                down, up,
                ifNeg -> ifNeg && each.test(-1),
                ifPos -> ifPos && each.test(+1)
        );
//        return actionTriStateContinuous(down, each);
    }

    /**
     * update function receives a value in 0..1.0 corresponding directly to the present goal frequency
     */
    default AbstractGoalAction action(Term s, boolean freqOrExp, FloatToFloatFunction ifGoalMissing, FloatToFloatFunction update) {
        return addAction(new GoalAction(s,
            new UnipolarMotor(freqOrExp, ifGoalMissing, update)
        ));
    }

    default AbstractGoalAction action(Term s, FloatToFloatFunction update, FloatToFloatFunction post) {
        return addAction(new GoalAction(s, new UnipolarMotor(
                true, ifGoalUnknownThenPrev, update
        )) {
            @Override
            protected float goal(float m) {
                return post.valueOf(m);
            }
        });
    }

    default BooleanPredicate debounce(Runnable f, float durations) {
        return debounce(x -> {
            if (x) f.run();
        }, durations);
    }

    default BooleanPredicate debounce(BooleanProcedure f, float durations) {
        return debounce(x -> {
            f.value(x);
            return x;
        }, durations);
    }

    default BooleanPredicate debounce(BooleanPredicate f, float durations) {
        return debounce(f, () -> durations);
    }

    default BooleanPredicate debounce(BooleanPredicate f, FloatSupplier durations) {
        return debounce(f, durations, false);
    }

    /**
     * TODO control as min/max duty duration
     */
    default BooleanPredicate debounce(BooleanPredicate f, FloatSupplier durations, boolean defaultValue) {

        long[] reEnableAt = {Long.MIN_VALUE};

        return x -> {
            float dd = durations.asFloat();
            boolean y;
            if (dd > 0) {
                y = defaultValue;
                if (x != defaultValue) {
                    Game g = (Game) this;
                    long now = g.time();
                    if (now >= reEnableAt[0]) {
                        float period = dd * g.dur();// w.durPhysical();
                        reEnableAt[0] = now + Math.round(period);
                        y = !defaultValue;
                    }
                }
            } else {
                y = x; //unaffected
            }
            return f.accept(y);
        };
    }



    default AbstractGoalAction actionControl(AbstractAction aa, ActionControl c) {
        var game = (Game) this;
        var a = (GoalAction)aa;
        game.actions.overrides.o.put(c, a);
        a.setMotor(c.motor(a.motor, a, game));
        return a;
    }
    default BiPolarAction actionControl(BiPolarAction a, ActionControl n, ActionControl p) {
        var game = (Game) this;
        game.actions.overrides.o.put(p, a);
        game.actions.overrides.o.put(n, a);
        var modelInner = a.model;
        a.model = (pos, neg, prev, now) -> {
            if (p.enabled)
                pos = $.t(((PushButtonControl)p).pressed ? 1 : 0, game.nar.confDefault(GOAL));
            if (n.enabled)
                neg = $.t(((PushButtonControl)n).pressed ? 1 : 0, game.nar.confDefault(GOAL));
            return modelInner.update(pos, neg, prev, now);
        };
        return a;
    }
    //    /**
//     * the supplied value will be in the range -1..+1. if the predicate returns false, then
//     * it will not allow feedback through. this can be used for situations where the action
//     * hits a limit or boundary that it did not pass through.
//     * <p>
//     * TODO make a FloatToFloatFunction variation in which a returned value in 0..+1.0 proportionally decreasese the confidence of any feedback
//     */
//
//    default GoalActionConcept action(String s, GoalActionConcept.MotorFunction update) throws Narsese.NarseseException {
//        return action($.$(s), update);
//    }


//    default BeliefActionConcept react( Term s,  Consumer<Truth> update) {
//        return addAction(new BeliefActionConcept(s, nar(), update));
//    }
//    /**
//     * supplies values in range -1..+1, where 0 ==> expectation=0.5
//     */
//    default GoalActionConcept actionExpUnipolar(Term s, FloatToFloatFunction update) {
//        final float[] x = {0f}, xPrev = {0f};
//
//        return action(s, (b, d) -> {
//            float o = (d != null) ?
//
//                    d.expectation() - 0.5f
//                    : xPrev[0];
//            float ff;
//            if (o >= 0f) {
//
//
//                float fb = update.valueOf(o /*y.asFloat()*/);
//                if (fb != fb) {
//
//                    return null;
//                } else {
//                    xPrev[0] = fb;
//                }
//                ff = (fb / 2f) + 0.5f;
//            } else {
//                ff = 0f;
//            }
//            return $.t(unitize(ff), nar().confDefault(BELIEF));
//        });
//    }

    /** register an action with a keypress */
    default void actionKey(AbstractAction action, char c) {
        actionControl(action, new PushButtonControl(c));
    }
    default void actionKey(BiPolarAction action, char n, char p) {
        actionControl(action, new PushButtonControl(n), new PushButtonControl(p));
    }


}