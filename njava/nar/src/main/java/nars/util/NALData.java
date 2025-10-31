package nars.util;

import jcog.Str;
import jcog.data.list.Lst;
import jcog.table.ARFF;
import jcog.table.DataTable;
import nars.*;
import nars.term.var.NormalizedVariable;
import nars.term.var.UnnormalizedVariable;
import org.jetbrains.annotations.Nullable;
import tech.tablesaw.api.Table;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static nars.Op.*;

/**
 * used to translate a schema+data table to a set of beliefs and questions
 *
 * defines a data mapping and interpretation strategy for rendering
 * input as NAL tasks.
 */
public enum NALData {
    ;

//    /** 1d "db row" */
//    static class RowData extends NALData {
//        //TODO
//    }
//    /** 2d "db table" */
//    static class RowColData extends NALData {
//        //TODO
//    }



    public static void believe(DataTable data, BiFunction<Term, Term[], Term> pointGenerator, NAR n) {
        n.input(
                Stream.concat(metaBeliefs(data, pointGenerator, n), beliefs(data, pointGenerator, n))
        );
    }

    public static void ask(Table data, BiFunction<Term, Term[], Term> pointGenerator, NAR n) {
        n.input(
                data(n, data, QUESTION, pointGenerator)
        );
    }

//    /** raw product representation of the row */
//    public static Function<Term[], Term> raw = $::p;
//
//    /** all elements except a specified row become the subj of an impl to the element in the specified column*/
//    public static BiFunction<Term, Term[], Term> predictsNth(int column) {
//        return (tt) -> {
//            Term[] subj = ArrayUtils.remove(tt, column);
//            Term pred = tt[column];
//            return $.inh($.p(subj), pred);
//        };
//    }
    /** all elements except a specified row become the subj of an impl to the element in the specified column*/
    static final BiFunction<Term, Term[], Term> predictsLast = (ctx, tt) -> {
        int lastCol = tt.length - 1;
        Term[] subj = Arrays.copyOf(tt, lastCol);
        Term pred = tt[lastCol];
        return $.inh($.p($.p(subj), pred), ctx);
    };



    /** beliefs representing the schema's metadata */
    private static Stream<NALTask> metaBeliefs(DataTable a, BiFunction<Term, Term[], Term> pointGenerator, NAR nar) {

        int n = a.columnCount();

        List<Term> meta = new Lst(n*2);

        Term pattern = pointGenerator.apply(
            name(a),
            IntStream.range(0, n).mapToObj(i -> $.varDep(i + 1)).toArray(Term[]::new)
        );
        for (int i = 0; i < n; i++) {
            String ai = a.attrName(i);
            Term attr = attrTerm(ai);

            meta.add(
//                    IMPL.the(pattern.replace($.varDep(i+1), $.varIndep(i+1)),
//                            $.inh($.varIndep(i+1), attr))

                    CONJ.the(pattern, $.inh($.varDep(i+1), attr))
            );

//            String[] nom = a.categories(ai);
//            if (nom!=null) {
//                meta.add(INH.the(SETe.the($.$(nom)), attr));
//            }

        }

        return meta.stream().map(t -> (NALTask)NALTask.task(t, BELIEF, $.t(1, nar.confDefault(BELIEF)),
                ETERNAL, ETERNAL, nar.evidence()).pri(nar)
        );
    }

    private static Term attrTerm(String ai) {
        return $.$$(Str.unquote(ai));
    }

    public static Stream<NALTask> beliefs(Table a, BiFunction<Term, Term[], Term> pointGenerator, NAR n) {
        return data(n, a, (byte)0, pointGenerator);
    }

    /** if punc==0, then automatically decides whether belief or question
     * according to presence of a Query variable in a data point.
     *
     * the pointGenerator transforms the raw components of a row
     * into a compound target (task content). how this is done
     * controls the semantics of the data point, with regard
     * to the application: prediction, optimization, etc.
     *
     * for example,
     *      (a,b,c,d) -> ((a,b,c)==>d)
     *          is different from:
     *      (a,b,c,d) -> ((a,b)==>(c,d))
     *          and different from
     *      (a,b,c,d) -> ((a,b,c)-->d)
     *
     */
    public static Stream<NALTask> data(NAR n, Table a, byte punc, BiFunction<Term, Term[], Term> pointGenerator) {
        long now = n.time();
        return terms(a, pointGenerator).map(point->{

            byte p = punc != 0 ?
                    punc 
                    :
                    (point.hasAny(VAR_QUERY) ? QUESTION : BELIEF);

            @Nullable Truth truth = p==QUESTION || p == QUEST ? null :
                $.t(1f, n.confDefault(p));
            return (NALTask)NALTask.taskUnsafe(point.normalize(), p, truth, ETERNAL, ETERNAL, n.evidence()).pri(n);
        });
    }

    public static Stream<Term> terms(Table a, BiFunction<Term, Term[], Term> generator) {
        Term ctx = name(a);
        return StreamSupport.stream(a.spliterator(),false).map(point ->{
            //ImmutableList point = instance.data;
            int n = point.columnCount();
            Term[] t = new Term[n];
            for (int i = 0; i < n; i++) {
                Object x = point.getObject(i);
                Term y;
                if (x instanceof String S) {
                    if (S.equals("_"))
                        y = $.varDep(i+1);
                    else
                        y = attrTerm(S);
                } else if (x instanceof Number) {
                    y = $.the((Number)x);
                } else {
                    throw new UnsupportedOperationException();
                }
                t[i] = y;
            }
            return generator.apply(ctx, t);
        });
    }

    public static Term name(Table a) {
        String r = a != null ? ((ARFF)a).getRelation() : a.toString();
        if (r == null)
            r = ("ARFF_" + System.identityHashCode(a));
        return $.atomic(r);
    }


    /** any (query) variables are qualified by wrapping in conjunction specifying their type in the data model */
    public static Function<Term[], Term> typed(DataTable dataset, Function<Term[], Term> pointGenerator) {
        return x -> {
            Term y = pointGenerator.apply(x);
            if (y.hasAny(VAR_QUERY)) {
                Term[] typing = y.subterms().collect(s->s.opID()==VAR_QUERY.id, new Lst<>()).stream().map(q -> {
                    if (q instanceof UnnormalizedVariable) {
                        String col = q.toString().substring(1);
                        return INH.the(q, attrTerm(col));
                    } else {
                        assert (q instanceof NormalizedVariable);
                        int col = q.hashCode() - 1;
                        return INH.the(q, attrTerm(dataset.attrName(col)));
                    }
                }).toArray(Term[]::new);
                y = CONJ.the(y, CONJ.the(typing));
            }
            return y;
        };
    }
}