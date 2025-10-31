//package nars.func.language;
//
//import edu.stanford.nlp.simple.Sentence;
//import edu.stanford.nlp.trees.Tree;
//import jcog.Util;
//import jcog.data.list.Lst;
//import jcog.data.map.UnifriedMap;
//import jcog.pri.PLink;
//import jcog.pri.bag.impl.PLinkArrayBag;
//import jcog.pri.op.PriMerge;
//import nars.$;
//import nars.NAR;
//import nars.Op;
//import nars.Term;
//import nars.subterm.TmpTermList;
//import nars.NALTask;
//import nars.term.atom.Atomic;
//import nars.Truth;
//import org.eclipse.collections.api.list.primitive.ByteList;
//import org.eclipse.collections.api.list.primitive.ImmutableByteList;
//import org.eclipse.collections.api.tuple.Pair;
//import org.eclipse.collections.api.tuple.Twin;
//import org.eclipse.collections.impl.tuple.Tuples;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ThreadLocalRandom;
//import java.util.function.BiPredicate;
//
///**
// * https://stanfordnlp.github.io/CoreNLP/index.html
// * https://gist.github.com/patham9/15157c8f388405200941ad8e2056e23d
// * https://github.com/opennars/OpenNARS-for-Applications/blob/master/english_to_narsese.py
// * https://github.com/opennars/OpenNARS-for-Applications/blob/master/misc/Python/english_to_narsese_gpt3.py
// */
//public class NLPInput {
//
//    final NAR nar;
//
//    record NLExample(String nl, Term parse) {
//    }
//
//    static private final int REVERSE_CAPACITY = 128;
//    public final PLinkArrayBag<NLExample> reverse = new PLinkArrayBag<>(PriMerge.plus, REVERSE_CAPACITY);
//
//    public NLPInput(NAR nar) {
//        this.nar = nar;
////        Arrays.stream(examplesDefault).parallel().forEach(x -> addExample(x[0], x[1]));
////        examples.forEach(System.out::println);
//    }
//
//    /**
//     * find best match
//     */
//    public List<NALTask> parse(String x) {
//        Sentence s = new Sentence(x);
////        s.dependencyGraph(ENHANCED_PLUS_PLUS);
////        var g = s.dependencyGraph();
////        System.out.println(g.toCompactString(true));
////        System.out.println(g.toFormattedString());
//
////        var c = s.coref();
//
//        Tree p = s.parse().skipRoot();
//        Term np = node(p);
//
//        saveReverse(x, np);
//
//        byte punc = punc(p);
//        Truth truth = truth(p, punc);
//        long[] se = when(p);
//        float pri = pri(p, punc);
//        return List.of(NALTask.task(np, punc, truth, se[0], se[1], nar.evidence()).withPri(pri));
//
////        Top<NLPInputExample> best = new Top<>(z -> z.match(p));
////        for (var e : examples)
////            best.add(e);
//        //return best.the.adapt(X);
//    }
//
//    private void saveReverse(String nl, Term parse) {
//        reverse.put(new PLink<>(new NLExample(nl, parse), 1f/ Util.sqrt(reverse.capacity())));
//    }
//
//    private float pri(Tree p, byte punc) {
//        return nar.priDefault(punc);
//    }
//
//    private Truth truth(Tree p, byte punc) {
//        return Op.BELIEF_OR_GOAL(punc) ? $.t(1, nar.confDefault(punc)) /* TODO */ : null;
//    }
//
//    private long[] when(Tree p) {
//        //return new long[]{ETERNAL, ETERNAL};
//        return nar.time.present();
//    }
//
//    private byte punc(Tree p) {
//        //TODO
//        return Op.BELIEF;
//    }
//
//    @Nullable
//    public static Term node(Tree p) {
//        int c = p.numChildren();
//
//        String labelStr = p.label().toString();
//        if (labelStr.length() == 1) {
//            if (!Character.isAlphabetic(labelStr.charAt(0)))
//                return null;
//        }
//
//        var label = switch (labelStr) {
//            case "S", "SBARQ" -> null;
//            default -> Atomic.the(labelStr);
//        };
//
//        if (c == 0) {
//            return label;
//        } else {
//            TmpTermList l = new TmpTermList(c);
//            for (int i = 0; i < c; i++) {
//                Term yi = node(p.getChild(i));
//                if (yi != null)
//                    l.add(yi);
//            }
//            Term[] ll = l.arrayTake();
//            return label != null ? $.func(label, ll) : (ll.length > 1 ? $.p /* SET? */ (ll) : ll[0]);
//        }
//    }
//
//    public String generate(NALTask xt) {
//        Term x = xt.term();
//        var xe = x.equals();
//        int xc = x.complexity();
//        Lst<NLExample> exact = new Lst<>();
//        Lst<NLExample> sameComplexity = new Lst<>();
//        for (var r : reverse) {
//            NLExample R = r.get();
//            Term rp = R.parse;
//            if (xe.test(rp)) {
//                exact.add(R);
//            } else if (rp.complexity() == xc) {
//                sameComplexity.add(R);
//            }
//        }
//
//        if (!exact.isEmpty()) {
//            return exact.get(ThreadLocalRandom.current()).nl;
//        }
//
//        if (!sameComplexity.isEmpty()) {
//            //compare sameComplexity for 1-difference that can be substituted
////            var xs = x.toString();
//            Map<ImmutableByteList,Term> X = new UnifriedMap<>();
//            BiPredicate<ByteList, Term> xs = (l, xx)->{
//                X.put(l.toImmutable(), xx);
//                return true;
//            };
//            x.pathsTo(t->t.ATOM(), t->true, xs);
//
//            for (var y : sameComplexity) {
////                var ys = y.parse.toString();
//                //EditDistance
////                LevenshteinDistance
//                //((Compound)x).ANDrecurse()
//                List<Pair<ImmutableByteList,Twin<Term>>> diff = new Lst<>();
//                BiPredicate<ByteList, Term> each = (l, yy)->{
//                    ImmutableByteList ll = l.toImmutable();
//                    var xx = X.get(ll);
//                    if (xx.equals(yy)) {
//                        //same
//                    } else {
//                        diff.add(Tuples.pair(ll, Tuples.twin(xx,yy)));
//                    }
//                    return true;
//                };
//                y.parse.pathsTo(t->t.ATOM(), t->true, each);
//                if (diff.size() == 1) {
//                    //apply the substitution
//                    //TODO buffer and choose from possibles
//                    var d0 = diff.get(0).getTwo();
//                    String ynl = y.nl;
//                    String yRep = ynl.replace(d0.getTwo().toString(), d0.getOne().toString());
//                    if (!yRep.equals(ynl))
//                        return yRep;
//                } else {
//                    //TODO
//                }
//            }
//        }
//
//        //last resort:
//        return xt.toString();
//    }
//
////    class NLPInputExampleMatch {
////        final NLPInputExample example;
////        final Map<Object,Object> replacements = new HashMap<>();
////
////        NLPInputExampleMatch(NLPInputExample example) {
////            this.example = example;
////        }
////
//////        public NALTask parse(String x) {
//////            throw new TODO();
//////        }
////    }
//
////    final List<NLPInputExample> examples = new CopyOnWriteArrayList<>();
////    private static final String[][] examplesDefault = {
////            new String[] { "cat is animal",    "<cat-->animal>."},
////            new String[] { "cats are animals", "<cats-->animals>."},
////
////            new String[] { "cat is not animal",    "--<cat-->animal>."},
////
////            new String[] { "what is cat?",      "<?1 --> cat>?"},
////            new String[] { "what is a cat?",    "<?1 --> cat>?"},
////            new String[] { "cat is what?",      "<cat --> ?1>?"},
////            new String[] { "cat is a what?",    "<cat --> ?1>?"},
////
////            new String[] { "cat is in garden.", "in(cat,garden)."},
////            new String[] { "cats are in garden.", "in(cats,garden)."},
////            new String[] { "cat eats food.", "eat(cat,food)."},
////
////            new String[] { "if x then y.", "(x ==> y)."},
////            new String[] { "if not x then y.", "(--x ==> y)."},
////            new String[] { "if not x then not y.", "(--x ==> --y)."},
////            new String[] { "if x then not y.", "(x ==> --y)."},
////    };
////
////    public void addExample(String nl, String narsese) {
////        examples.add(new NLPInputExample(nl, narsese));
////    }
//
////
////
////    /** input NLP -> narsese tasks */
////    class NLPInputExample {
////
////        private final String nlString;
////        private final String narsese;
////        private final Sentence nlSentence;
////        private final Tree nlParse;
////        private final NALTask narsTask;
////
////        public NLPInputExample(String nlSentence, String narsese) {
//////            var nlDoc = new Document(nl);
//////            List<Sentence> sentences = nlDoc.sentences();
//////            if (sentences.size() != 1)
//////                throw new UnsupportedOperationException(nl + " parsed to " + sentences.size() + "!=1 sentences");
//////            sentence = sentences.get(0);
////
////            //Parse NL
////            this.nlSentence = new Sentence(nlSentence);
////            this.nlParse = this.nlSentence.parse();
////            this.nlString = nlSentence;
////
////            //Parse Narsese
////            this.narsese = narsese;
////            try {
////                this.narsTask = Narsese.task(narsese, nar);
////            } catch (Narsese.NarseseException e) {
////                throw new UnsupportedOperationException("narsese parse error: ", e);
////            }
////        }
////
////        public float match(Tree y) {
////            return -distance(this.nlParse, y);
////        }
////
////        private static float distance(Tree x, Tree y) {
////            if (x.equals(y)) return 0;
////            int xc = x.numChildren(), yc = y.numChildren();
////            if (xc!=yc) return Float.POSITIVE_INFINITY;
////            Label xl = x.label();
////            if (!xl.equals(y.label()))
////                return Float.POSITIVE_INFINITY;
////            String xls = xl.toString();
////            if (xc == 1 && xls.equals("NP")) {
////                //allow noun phrase difference
////                //TODO store string remap
////                return 0.5f;
////            }
////            //TODO verb, lemma comparison
////
////            if (xc == 0) {
////                return 0;
////            } else {
////                double d = 0;
////                for (int i = 0; i < xc; i++) {
////                    float di = distance(x.getChild(i), y.getChild(i));
////                    if (di == Float.POSITIVE_INFINITY) return Float.POSITIVE_INFINITY;
////                    d += di;
////                }
////                return (float) d;
////            }
////        }
////
////        @Override
////        public String toString() {
////            return nlParse + " |-\n\t" + narsTask;
////        }
////    }
//}
