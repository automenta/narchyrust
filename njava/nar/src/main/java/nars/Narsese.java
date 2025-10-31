package nars;

import com.github.fge.grappa.buffers.CharSequenceInputBuffer;
import com.github.fge.grappa.buffers.InputBuffer;
import com.github.fge.grappa.matchers.base.Matcher;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.MatchHandler;
import com.github.fge.grappa.run.ParsingResult;
import com.github.fge.grappa.run.context.DefaultMatcherContext;
import com.github.fge.grappa.run.context.MatcherContext;
import com.github.fge.grappa.stack.ArrayValueStack;
import com.github.fge.grappa.stack.ValueStack;
import com.github.fge.grappa.transform.ParserTransformer;
import com.google.common.annotations.VisibleForTesting;
import jcog.Util;
import jcog.data.list.Lst;
import nars.io.NarseseParser;
import nars.task.AbstractCommandTask;
import nars.term.atom.Atomic;
import nars.term.obj.QuantityTerm;
import nars.time.Tense;
import nars.time.Time;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static nars.Op.*;
import static nars.Term.nullIfNull;

/**
 * NARese, syntax and language for interacting with a NAL in NARS.
 * https:
 */
public final class Narsese {

    private static final ThreadLocal<Narsese> parsers;

    static {
        Constructor<? extends NarseseParser> parseCtor;
        try {
            parseCtor = ParserTransformer.transformParser(NarseseParser.class).getConstructor();
//			parseCtor = NarseseParser$$grappa.class.getConstructor();
        } catch (NoSuchMethodException | IOException e) {
            throw new RuntimeException(e);
        }


        parsers = ThreadLocal.withInitial(() -> {
                try {
                    return new Narsese(parseCtor.newInstance());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        );


    }

    private final NarseseParser parser;
    private MyParseRunner inputParser, termParser;

    private Narsese(NarseseParser p) {
        this.parser = p;
    }

    public static Narsese the() {
        return parsers.get();
    }

    /**
     * returns number of tasks created
     */
    public static void tasks(String input, Collection<Task> c, NAL n) throws NarseseException {
        var p = the();

        var parsedTasks = 0;

        var rv = p.inputParser().run(input).getValueStack();

        var size = rv.size();

        for (var i = size - 1; i >= 0; i--) {
            var o = rv.peek(i);

            Object[] y;
            if (o instanceof Task) {
                y = (new Object[]{o});
            } else if (o instanceof Object[] oo) {
                y = oo;
            } else {
                throw new NarseseException("Parse error: " + input);
            }

            c.add(task(y, n));
            parsedTasks++;

        }

        if (parsedTasks == 0)
            throw new NarseseException("nothing parsed: " + input);


    }

    public static List<Task> tasks(String x, NAL n) throws NarseseException {
        var y = new Lst<Task>(1);
        tasks(x, y, n);
        return y;
    }

    /**
     * parse one task
     */
    public static <T extends Task> T task(String input, NAL n) throws NarseseException {
        var tt = tasks(input, n);
        if (tt.size() != 1)
            throw new NarseseException(tt.size() + " tasks parsed in single-task parse: " + input);
        return (T) tt.getFirst();
    }

    static Task task(Object[] x, NAL n) {
        if (x.length == 1 && x[0] instanceof Task t)
            return t;

        var content = ((Term) x[1]);//.normalize();
            /*if (!(content instanceof Compound)) {
                throw new NarseseException("Task target unnormalizable: " + contentRaw);

            } else */

        var px = x[2];

        var punct = px instanceof Byte b ?
            b
            :
            (byte) (((Character) px).charValue());

        return punct == COMMAND ?
            new AbstractCommandTask(content) :
            task(x, n, content, punct);
    }

    private static NALTask task(Object[] x, NAL n, Term content, byte punct) {
        var t = truth(x, n, punct);

        var occ = occurrence(n.time, x[4]);

        var y = NALTask.task(content, punct, t, occ[0], occ[1], n.evidence());

        y.pri(x[0] == null ? n.priDefault(punct) : (Float) x[0]);

        return y;
    }

    @Nullable
    private static Truth truth(Object[] x, NAL n, byte punct) {
        Truth t;
        var _t = x[3];
        if (_t instanceof Truth tt)
            t = tt;
        else if (_t instanceof Float f)
            t = $.t(f, n.confDefault(punct));
        else
            t = null;

        if (t == null && (punct == BELIEF || punct == GOAL))
            t = $.t(1, n.confDefault(punct)); //HACK
        return t;
    }

    private static long[] occurrence(Time t, Object O) {
        switch (O) {
            case null -> {
                return new long[]{ETERNAL, ETERNAL};
            }
            case Tense tense -> {
                var o = t.relativeOccurrence(tense);
                return new long[]{o, o};
            }
            case QuantityTerm quantityTerm -> {
                var qCycles = t.toCycles(quantityTerm.quant);
                var o = t.now() + qCycles;
                return new long[]{o, o};
            }
            case Integer i -> {
                var o = t.now() + i;
                return new long[]{o, o};
            }
            case Object[] objects -> {
                var start = occurrence(t, objects[0]);
                if (start[0] != start[1] || start[0] == ETERNAL || start[0] == TIMELESS)
                    throw new UnsupportedOperationException();
                var end = occurrence(t, objects[1]);
                if (end[0] != end[1] || end[0] == ETERNAL || end[0] == TIMELESS)
                    throw new UnsupportedOperationException();
                if (start[0] <= end[0]) {
                    start[1] = end[0];
                    return start;
                } else {
                    end[1] = start[0];
                    return end;
                }
            }
            case long[] longs -> {
                return longs;
            }
            default -> throw new UnsupportedOperationException("unrecognized occurrence: " + O);
        }
    }

    public static Term term(String s, boolean normalize) throws NarseseException {
        var y = term(s);
        return normalize ? nullIfNull(y.normalize()) : y;
    }

    public static Term term(String s) throws NarseseException {
        return the()._term(s);
    }

    private MyParseRunner inputParser() {
        if (inputParser == null)
            this.inputParser = new MyParseRunner(parser.Input());
        return inputParser;
    }

    private MyParseRunner termParser() {
        if (termParser == null)
            this.termParser = new MyParseRunner(parser.Term());
        return termParser;
    }

    /**
     * parse one target NOT NORMALIZED
     */
    Term _term(String s) throws NarseseException {

        var stack = __term(s);

        var ss = stack.size();
        if (ss==1) {
            var x = stack.pop();
            if (x instanceof String sx)
                return Atomic.atomic(sx);
            else if (x instanceof Term st)
                return st;
        }

        throw new NarseseException("parse fail: " + s + "\n\t" +
                Arrays.toString(Util.arrayOf(stack::peek, 0, ss, Object[]::new)));
    }

    private ValueStack __term(String s) {
        return termParser().run(s).getValueStack();
    }


    static class MyParseRunner<V> implements MatchHandler {

        private final Matcher rootMatcher;
        private ValueStack<V> valueStack;

        /**
         * Constructor
         *
         * @param rule the rule
         */
        MyParseRunner(Rule rule) {
            rootMatcher = (Matcher) rule;
        }

        @Override
        public boolean match(MatcherContext context) {
            return context.getMatcher().match(context);
        }

        public ParsingResult run(String input) {
            return run(new CharSequenceInputBuffer(input));
        }

        public ParsingResult<V> run(InputBuffer inputBuffer) {
            //Objects.requireNonNull(inputBuffer, "inputBuffer");
            resetValueStack();

            var context = createRootContext(inputBuffer, this);

            return createParsingResult(context.runMatcher(), context);
        }

        private void resetValueStack() {
            // TODO: write a "memoizing" API
            if (valueStack == null || !valueStack.isEmpty())
                valueStack = new ArrayValueStack<>();
            else
                valueStack.clear();//Util.nop();
        }

        @VisibleForTesting
        MatcherContext<V> createRootContext(
                InputBuffer inputBuffer, MatchHandler matchHandler) {
            return new DefaultMatcherContext<>(inputBuffer, valueStack,
                    matchHandler, rootMatcher);
        }

        @VisibleForTesting
        ParsingResult<V> createParsingResult(boolean matched,
                                             MatcherContext<V> context) {
            return new ParsingResult<>(matched, valueStack, context);
        }


    }

    /**
     * Describes an error that occurred while parsing Narsese
     */
    public static final class NarseseException extends Exception {

        public final @Nullable ParsingResult result;

        /**
         * An invalid addInput line.
         *
         * @param message type of error
         */
        public NarseseException(String message) {
            super(message);
            this.result = null;
        }

        public NarseseException(String input, Throwable cause) {
            this(input, null, cause);
        }

        public NarseseException(String input, ParsingResult result, Throwable cause) {
            super(input + '\n' + (result != null ? result : cause), cause);
            this.result = result;
        }
    }

}