package nars.concept;

import jcog.util.ArrayUtil;
import nars.*;
import nars.eval.Evaluation;
import nars.eval.TaskEvaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static nars.Op.*;

/**
 * Operator interface specifically for goal and command punctuation
 * Allows dynamic handling of argument list like a functor,
 * but at the task level

 * TODO implement generic Task reaction interface
 */
@Deprecated public final class Operator extends Functor {

    private static final String LOG = String.valueOf(Character.valueOf((char) 8594));

    public final BiFunction<Task, NAR, Task> model;

    private Operator(Atom name, BiFunction<Task, NAR, Task> model) {
        super(name);
        this.model = model;
    }


    public static Functor simple(String name, BiConsumer<Task, NAR> exe) {
        return simple(name, (x,y)->{
            exe.accept(x, y);
            return null;
        });
    }

    public static Functor simple(String name, BiFunction<Task, NAR, Task> exe) {
        return simple(atom(name), exe);
    }

    public static Functor simple(Atom name, BiFunction<Task, NAR, Task> exe) {
         return new Operator(name, new SimpleOperatorModel(exe));
    }

    public static Task error(Task x, Throwable error, long when) {
        
        
        return command("error", when, $.quote(x.toString()),
                
                error!=null ? $.quote(error.getMessage()!=null ? error.getMessage() : error.toString()) : $.atomic("?")
        );
    }


    public static NALTask command(Term content, long when) {
        return NALTask.taskUnsafe(content, COMMAND, null, when, when, ArrayUtil.EMPTY_LONG_ARRAY);
    }

    public static NALTask log(long when, Object content) {
        return command(LOG, when, $.the(content));
    }

    private static NALTask command(String func, long now,  Term... args) {
        return command($.func(func, args), now);
    }

    public static Term arg(Term operation, int sub) {
        return operation.sub(0).subterms().sub(sub);
    }

    @Override
    public final Term apply(Evaluation E, Subterms args) {
        if (E instanceof TaskEvaluation e) {
            Task t = e.task();
            byte punc = t.punc();
            if (punc == GOAL || punc == COMMAND) {

                Atomic tf = func(t.term());
                if (tf==null || !tf.equals(this))
                    return null;

                Task y = model.apply(t, e.nar());
                if (y != null && y != t)
                    e.accept(y);
            }
        }
        return null;
    }


    private static class SimpleOperatorModel implements BiFunction<Task, NAR, Task> {

        private final BiFunction<Task, NAR, Task> exe;

        /** future time that tasks can be scheduled in the future for, in durs */
        private float scheduleDurs = 2;

        SimpleOperatorModel(BiFunction<Task, NAR, Task> exe) {
            this.exe = exe;
        }

        @Override
        public Task apply(Task x, NAR nar) {
            //default handler
            if (x.COMMAND())
                return exe.apply(x, nar);
            else {
                NALTask t = (NALTask)x;
                assert(t.GOAL());
                if (executeGoal(t)) {
                    long s = t.start();
                    if (s == ETERNAL)
                        return exe.apply(x, nar);

                    long now = nar.time();
                    float dur = nar.dur();
                    if (s >= now - dur / 2) {
                        if (s > now + dur / 2) {
                            //delayed
                            if (s <= now + scheduleDurs * dur) {
                                nar.runAt(s, () -> {
                                    exe.apply(x, nar); //TODO return value to input Focus?
                                });
                                //nar.runAt(s, () -> nar.input(exe.apply(x, nar)));
                            }

                        } else {
                            return exe.apply(x, nar);
                        }
                    }
                }

                return null;
            }
        }

        static boolean executeGoal(NALTask goal) {
            return goal.expectation() > 0.5f;
        }
    }

}