//package nars.experiment.buy;
//
//import nars.$;
//import nars.NAR;
//import nars.NARS;
//import nars.Term;
//import nars.table.BeliefTables;
//import nars.table.dynamic.SeriesBeliefTable;
//import nars.table.dynamic.TaskSeriesSeriesBeliefTable;
//import nars.util.RingIntervalSeries;
//import nars.Truth;
//import tech.tablesaw.api.Row;
//import tech.tablesaw.api.Table;
//
//import java.io.IOException;
//import java.io.StringReader;
//import java.util.function.Consumer;
//
//import static nars.$.$$;
//
//public class CSVTemporalBeliefTable {
//
//    static void loadCSV(String csv, NAR n, long START, long period) {
//        assert (period > 0);
//        final int cap = 100;
//        float conf = n.beliefConfDefault.floatValue();
//        float pri = n.beliefPriDefault.pri();
//
//
//        Table t = Table.create().read().csv(new StringReader(csv));
//        t.forEach(new Consumer<>() {
//            SeriesBeliefTable[] ss;
//            Term[] concepts;
//
//            {
//                concepts = t.columns().stream().map(z -> $$(z.name())).toArray(Term[]::new);
//                ss = new SeriesBeliefTable[concepts.length];
//                for (int i = 0, conceptsLength = concepts.length; i < conceptsLength; i++) {
//                    Term c = concepts[i];
//
//                    var s = new TaskSeriesSeriesBeliefTable(c, true, new RingIntervalSeries<>(cap));
//                    s.sharedStamp = n.evidence();
//                    ss[i] = s;
//
//                    ((BeliefTables) n.conceptualize(c).beliefs()).add(s);
//                }
//            }
//
//            long start = START;
//
//            @Override
//            public void accept(Row row) {
//                float dur = n.dur();
//                long end = start + period;
//                for (int c = 0; c < concepts.length; c++) {
//                    ss[c].add($.tt((float) row.getDouble(c), conf),
//                                    start, end, dur)
//                            .pri(pri);
//                }
//                start += period;
//            }
//        });
//
//    }
//
//    public static void main(String[] args) {
//        NAR n = NARS.tmp();
//
//        loadCSV("""
//                 a,  b,  c
//                0.1,0.2,0.3
//                0.2,0.3,0.3
//                0.8,0.4,0.7
//                """, n, 0, 10);
//
//        n.tasks().forEach(System.out::println);
//
//
//        //mean
//        belief("a", 0, 30, n);
//        belief("b", 0, 30, n);
//
//        //fuzzy boolean set intersection
//        belief("(  a &   b)", 0, 30, n);
//        belief("(  a & --b)", 0, 30, n);
//        belief("(--a &   b)", 0, 30, n);
//        belief("(--a & --b)", 0, 30, n);
//        belief("(  a |   b)", 0, 30, n); //same as --(--a & --b)
//
//        //fuzzy boolean set intersection (temporal)
//        belief("(  a &&+10   b)", 0, 30, n);
//        belief("(  a &&+10 --b)", 0, 30, n);
//        belief("(--a &&+10   b)", 0, 30, n);
//        belief("(--a &&+10 --b)", 0, 30, n);
//
//        //implications
//        belief("(  a ==> b)", 0, 30, n);
//        belief("(--a ==> b)", 0, 30, n);
//        belief("(  b ==> a)", 0, 30, n);
//        belief("(--b ==> a)", 0, 30, n);
//
//        //deltas
//        belief("/\\a", 0, 30, n);
//        belief("/\\(a & b)", 0, 30, n);
//        belief("/\\(a | b)", 0, 30, n);
//        belief("(/\\a & /\\b)", 0, 30, n);
//        belief("(/\\a | /\\b)", 0, 30, n);
//
//    }
//
//    private static Truth belief(String query, int start, int end, NAR n) {
//        final Term x = $$(query);
//        Truth y = n.beliefTruth(x, start, end);
//        System.out.println(y + "\t" + x + " " + start + ".." + end);
//        return y;
//    }
//}