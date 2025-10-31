/*
 * Copyright (C) 2014 me
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:
 */
package nars.func.kif;

import com.google.common.collect.ForwardingSet;
import com.google.common.collect.Streams;
import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import jcog.data.list.Lst;
import jcog.data.set.ArrayHashSet;
import jcog.util.ArrayUtil;
import nars.$;
import nars.NALTask;
import nars.Task;
import nars.Term;
import nars.func.MathFunc;
import nars.task.AbstractCommandTask;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.util.TermException;
import nars.term.var.Variable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nars.$.$$;
import static nars.Op.*;
import static nars.func.rdfowl.NQuadsRDF.equi;
import static nars.term.atom.Bool.Null;

/**
 * https:
 * http:
 * https:
 * https:
 * https://github.com/TeamSPoon/sigma_ace/blob/master/engine/sigma_functions.pl
 * <p>
 * holdsDuring(ImmediateFutureFn(WhenFn($1)),$2) is
 * when($1, ?W) & immediateFuture(?W,?P) & holdsDurrent(?P, $2)
 * <p>
 * https://en.wikipedia.org/wiki/Linear_temporal_logic#Equivalences
 **/
public class KIF implements Iterable<Task> {

    private static final Logger logger = LoggerFactory.getLogger(KIF.class);
    static final Predicate<Term> invalidAssertion = b -> {
        if (b.hasAny(BOOL))
            return true;

        Term bb = b.unneg().normalize();
        if (!NALTask.TASKS(bb.unneg(), BELIEF)) {
            logger.error("invalid task target: {}\n\t{}", b, bb);
            return true;
        }
        return false;
    };
    private static final Term SYMMETRIC_RELATION = $$("SymmetricRelation");
    private static final Term ASYMMETRIC_RELATION = $$("AsymmetricRelation");

//    public static MemoryExternal.BytesToTasks load = new MemoryExternal.BytesToTasks("kif") {
//
//        @Override
//        public Stream<Task> apply(InputStream i) {
//            try {
//                return new KIF(i).tasks();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return null;
//        }
//    };
    private static final boolean includePredArgCounts = false;
    private static final Set<Term> predExclusions = java.util.Set.of(


            $$("UnaryPredicate"),
            $$("BinaryPredicate"), $$("TernaryPredicate"),
            $$("QuaternaryPredicate"), $$("QuintaryPredicate"),

            $$("UnaryFunction"), $$("BinaryFunction"), $$("TernaryFunction"),
            $$("QuaternaryRelation"),
            $$("QuintaryRelation"),
            $$("SingleValuedRelation"), $$("TotalValuedRelation"),
            $$("AsymmetricRelation")
    );
    public final ArrayHashSet<Term> assertions = new ArrayHashSet<>();
    private final KIFParser kif = new KIFParser();
    private final transient Map<Term, FnDef> fn = new HashMap<>();
    private final Set<Term> symmetricRelations = new HashSet<>();

    {
        symmetricRelations.add(SYMMETRIC_RELATION);
    }

    public KIF() {

    }

    public KIF(String s) {
        kif.read(new StringReader(s));
        process();
    }

    public KIF(InputStream is) throws IOException {
        kif.read(is);
        process();
    }

    public static List<Task> tasks(String... modules) {
        assert (modules.length > 0);
        return List.of(modules)
                //.stream()
                .parallelStream()
                .map(x -> "/home/me/sumo/" + x + ".kif")
                .flatMap(x -> {
                            try {
                                return Streams.stream(file(x));
                            } catch (IOException e) {
                                e.printStackTrace();
                                return Stream.empty();
                            }
                        }
                ).toList();
    }

    private static Term atomic(String sx) {
        if (sx.startsWith("@")) return Null; //HACK ignore @ROW

        return $.$$$(sx.replace("?", "#"));
    }

    private static Function<Term, Term> domainRangeMerger(Term type) {
        return x -> x.equals(type) ? x : SETi.the(List.of(x, type));
    }

    public static KIF file(String filePath) throws IOException {
        return new KIF(new FileInputStream(filePath));
    }

    private static Term disjoint(List<Term> args, Term v0) {
        return INH.the(
                DISJ(args.toArray(EmptyTermArray)), v0
        ).neg();
    }

    public static Term impl(Term a, Term b, boolean implOrEquiv) {


        Term tmp = IMPL.the(a, b);
        if (!tmp.unneg().IMPL()) {
            logger.warn("un-impl: {} ==> {} ", a, b);
            return Null;
        }
        boolean negated = tmp instanceof Neg;

        tmp = tmp.unneg();
        a = tmp.sub(0);
        b = tmp.sub(1);

        MutableSet<Term> _aVars = getVars(a);
        MutableSet<Term> _bVars = getVars(b);

        Map<Term, Term> remap = new HashMap<>();

        MutableSet<Term> common = _aVars.intersect(_bVars);
        if (!common.isEmpty()) {
            common.forEach(t -> {
                Variable u = $.v(
                        VAR_INDEP,


                        t.toString().substring(1));
                if (!t.equals(u) && !remap.containsKey(u))
                    remap.put(t, u);
            });
        }
        for (MutableSet<Term> ab : new MutableSet[]{_aVars, _bVars}) {
            for (Term aa : ab) {
                if (aa.VAR_INDEP() && !common.contains(aa)) {
                    String str = aa.toString().substring(1);
                    Variable bb = $.v(VAR_DEP, str);
                    if (!remap.containsKey(bb))
                        remap.put(aa, bb);
                }
            }
        }

        if (!remap.isEmpty()) {
            a = a.replace(remap);
            if (a == null)
                throw new NullPointerException("transform failure");

            b = b.replace(remap);
            if (b == null)
                throw new NullPointerException("transform failure");
        }

        try {
            return
                    (implOrEquiv ?
                            IMPL.the(a, b) :
                            equi(a, b)).negIf(negated)
                    ;
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
        return null;
    }

    private static MutableSet<Term> getVars(Term a) {
        MutableSet<Term> _aVars = new UnifiedSet<>(0);
        Set<Term> aVars = new VarOnlySet(_aVars);
        if (a instanceof Compound A)
            A.recurseSubtermsToSet(Variables, aVars, true);
        else if (a instanceof Variable)
            aVars.add(a);
        return _aVars;
    }

    public Stream<Task> tasks() {
        return assertions.stream().map(
                //Op::believe
                t -> new AbstractCommandTask($.func("task",t)).withPri(0.5f) //HACK
        );
    }

    private void process() {
        KB kb = KBmanager.manager.addKB("preprocess");

        kif.formulaMap.values().stream().flatMap(xx -> FormulaPreprocessor.preProcess(xx, kb).stream()).forEach(x -> {
            if (x.car().equals("<=>")) {
                tryAddEquiv(x);
            } else {
                tryAdd(x);
            }
        });


        for (Map.Entry<Term, FnDef> entry : fn.entrySet()) {
            FnDef s = entry.getValue();
            int ds = s.domain.isEmpty() ? 0 : s.domain.keySet().max();
            Term[] vt = Util.arrayOf(i -> $.varDep(1 + i), 0, ds, Term[]::new);
            Term v = null;
            if (s.range != null) {
                v = $.varDep("R");
                vt = ArrayUtil.add(vt, v);
            }
            int[] k = {1};
            Term[] typeConds = Util.arrayOf(i ->
                    INH.the($.varDep(1 + i),
                            s.domain.getIfAbsent(1 + i, () -> $.varDep(k[0]++))), 0, ds, Term[]::new);

            if (s.range != null)
                typeConds = ArrayUtil.add(typeConds, INH.the(v, s.range));

            Term f = entry.getKey();
            Term fxy = impl(INH.the($.p(vt), f), CONJ.the(typeConds), true);
            if (fxy != null) {
                if (fxy instanceof Bool) {
                    logger.error("bad function {} {} {}", f, s.domain, s.range);
                } else {
                    assertions.add(fxy);
                }
            }
        }

        if (symmetricRelations.size() > 1 /*SymmetricRelation exists in the set initially */) {

            Lst<Term> toAdd = new Lst(0);

            if (assertions.removeIf(belief -> {
                if (belief.INH()) {
                    Term fn = belief.sub(1);
                    if (symmetricRelations.contains(fn)) {

                        Term ab = belief.sub(0);
                        if (!ab.PROD())
                            return false;

                        assert (ab.subs() == 2);
                        Term a = ab.sub(0);
                        Term b = ab.sub(1);

                        toAdd.add(
                                INH.the(CONJ.the(PROD.the(a, b), PROD.the(b, a)), fn)
                        );
                        return true;
                    }
                }
                return false;
            }))
                assertions.addAll(toAdd);

        }

        assertions.removeIf(invalidAssertion);
    }

    private void tryAddEquiv(Formula x) {
        //equiv emulation
        try {
            Term a = formulaToTerm(x.getArgument(1), 0);
            if (a != Null) {
                Term b = formulaToTerm(x.getArgument(2), 0);
                if (b != Null) {
                    assertions.add(IMPL.the(a, b));
                    assertions.add(IMPL.the(b, a));
                    return;
                }
            }
            throw new TermException(x + " untranslated");
        } catch (TermException t) {
            t.printStackTrace();
        }
    }

    private void tryAdd(Formula x) {
        try {
            Term y = formulaToTerm(x, 0);
            if (y != null && y.TASKABLE())
                assertions.add(y);
//            else
//                throw new RuntimeException("untranslated: " + x); //HACK

        } catch (Exception e) {
            logger.error("{} {}", x, e.getMessage());
            e.printStackTrace();
        }
    }

    public static String formulaToProlog(String sx) {
        return new Formula(sx).toProlog();
    }

    public Term formulaToTerm(String sx, int level) {
        return formulaToTerm(new Formula(sx.replace("?", "#")), level);
    }

    private Term formulaToTerm(Formula x, int level) {
        Term result = Null;


        int l = x.listLength();
        //            if (x.theFormula.contains("(")) {
        //                if (!x.listP())
        //                    return null;
        //            } else
        switch (l) {
            case -1 -> result = atomic(x.theFormula);
            case +1 -> result = $.p(formulaToTerm(x.getArgument(0), level + 1));
            case 0  -> throw new WTF();
            default -> {
                List<String> sargs = IntStream.range(1, l).mapToObj(x::getArgument).toList();
                List<Term> args = sargs.stream().map(sarg -> formulaToTerm(sarg, level + 1)).toList();
                if (args.contains(null) || args.isEmpty()) {
                    result = Null;
                } else { /**
                 *
                 *
                 * https:
                 * def special_link_type(predicate):
                 mapping = {
                 '=>':types.ImplicationLink,
                 '<=>':types.EquivalenceLink,
                 'and':types.AndLink,
                 'or':types.OrLink,
                 'not':types.NotLink,
                 'instance':types.MemberLink,
                 'attribute':types.InheritanceLink,
                 'member':types.MemberLink,
                 'subclass':types.InheritanceLink,
                 'exists':types.ExistsLink,
                 'forall':types.ForAllLink,
                 'causes':types.PredictiveImplicationLink
                 *
                 */String xCar = x.car();
                    Term y = null;
                    boolean includeDoc = false;
                    boolean finished = false;
                    switch (xCar) {
                        case "ListFn":
                            y = $.p(args);
                            break;


                        case "exhaustiveAttribute": {
                            y = INH.the(args.get(0), SETi.the(args.subList(1, args.size())));
                            break;
                        }


                        case "subclass":
                        case "instance":
                        case "attribute":
                        case "subrelation":
                        case "rangeSubclass":
                        case "subAttribute":

                            if (args.size() != 2) {
                                throw new RuntimeException("instance expects 2 arguments");
                            } else {
                                Term pred = args.get(1);

                                Term subj = args.get(0);

                                if (symmetricRelations.contains(pred)) {
                                    symmetricRelations.add(subj);
                                }

                                if (pred.equals(SYMMETRIC_RELATION) && !subj.hasVars()) {
                                    finished = true;
                                    break;
                                }

                                if (!includePredArgCounts && predExclusions.contains(pred)) {
                                    finished = true;
                                    break;
                                }

                                if ("instance".equals(xCar))
                                    y = $.inst(subj, pred);
                                else
                                    y = INH.the(subj, pred);

                                if (y instanceof Bool) {
                                    result = y;
                                    finished = true;
                                    break;
                                }
                            }

                            break;

                        case "relatedInternalConcept":
                            /*(documentation relatedInternalConcept EnglishLanguage "Means that the two arguments are related concepts within the SUMO, i.e. there is a significant similarity of meaning between them. To indicate a meaning relation between a SUMO concept and a concept from another source, use the Predicate relatedExternalConcept.")            */
                            boolean includeRelatedInternalConcept = true;
                            if (includeRelatedInternalConcept) {
                                if (args.size() != 2) {
                                    throw new UnsupportedOperationException("relatedInternalConcept expects 2 arguments");
                                } else {
                                    y = SIM.the(args.get(0), args.get(1));
                                }
                            }
                            break;

                        case "greaterThan":
                            if (args.size() == 2)
                                y = $.func("cmp", args.get(0), args.get(1), Int.i(+1));
                            break;
                        case "lessThan":
                            if (args.size() == 2)
                                y = $.func("cmp", args.get(0), args.get(1), Int.NEG_ONE);
                            break;

                        case "equal":
                            y = EQ.the(args.get(0), args.get(1));
                            break;


                        case "<=>":

                            y = impl(args.get(0), args.get(1), false);
                            break;

                        case "holdsDuring":
                            y = CONJ.the(args.get(0), args.get(1));
                            assert (args.size() == 2);
                            break;
                        case "entails":
                            y = IMPL.the(args.get(0), args.get(1));
                            assert (args.size() == 2);
                            break;

                        //                if (!(args.get(0).hasVars() || args.get(1).hasVars())) {
//
//                    //y = impl(args.get(0), args.get(1), false);
//                    y = SIM.the(args.get(0), args.get(1));
//                } else {
//
//                }


                        case "forall":
                            String forVar = sargs.get(0);
                            if (!forVar.isEmpty() && forVar.charAt(0) == '(') {
                                forVar = forVar.substring(1, forVar.length() - 1);
                            }
                            String[] forVars = forVar.split(" ");
                            boolean missingAParamVar = Arrays.stream(forVars).anyMatch(vv -> !sargs.get(1).contains(vv));
                            y = !missingAParamVar ? args.get(1) : impl(args.get(0), args.get(1), true);
                            break;
                        case "exists":
                            y = args.get(1);
                            break;
                        case "=>":
                            y = impl(args.get(0), args.get(1), true);
                            break;


//            case "causes":
//                y = IMPL.the(args.get(0), 1, args.get(1));
//                break;

                        case "cooccur":
                        case "during":
                            y = CONJ.the(args.get(0), args.get(1));
                            break;
//            case "meetsTemporally":
//                y = CONJ.the(args.get(0), +1, args.get(1));
//                break;

                        case "domain":

                            if (level == 0) {
                                if (args.size() >= 3) {
                                    Term subj = (args.get(0));
                                    Term arg = (args.get(1));
                                    Term type = (args.get(2));
                                    if (type.equals(ASYMMETRIC_RELATION)) {
                                        finished = true;
                                        break;
                                    }
                                    FnDef d = fn.computeIfAbsent(subj, s -> new FnDef());

                                    d.domain.updateValue(Int.i(arg), () -> type, domainRangeMerger(type));

                                } else {
                                    throw new UnsupportedOperationException("unrecognized domain spec");
                                }
                                finished = true;
                                break;
                            }
                            break;
                        case "range":
                            if (level == 0) {
                                if (args.size() == 2) {
                                    Term subj = args.get(0);
                                    Term range = args.get(1);
                                    if (range.equals(ASYMMETRIC_RELATION)) {
                                        finished = true;
                                        break;
                                    }
                                    FnDef d = fn.computeIfAbsent(subj, (s) -> new FnDef());
                                    d.range = range;
                                } else {
                                    throw new UnsupportedOperationException("unrecognized range spec");
                                }
                                finished = true;
                                break;
                            }
                            break;

                        case "AdditionFn":
                            if (args.size() == 2)
                                y = MathFunc.add(args.get(0), args.get(1));
                            else
                                throw new TODO();
                            break;

                        case "MultiplicationFn":
                            if (args.size() == 2)
                                y = MathFunc.mul(args.get(0), args.get(1));
                            else
                                throw new TODO();
                            break;

//                if (args.size() > 2)
//                    y = disjoint(args.subList(1, args.size()), args.get(0));
//                else
//                    y = Equal.the(args.get(0), args.get(1)).neg();
//                break;

                        case "disjointDecomposition":
                        case "partition":
                        case "disjointRelation":
                        case "disjoint":
                        case "inverse":
                        case "contraryAttribute":

//                if (args.size() > 2)
                            y = $.inh(VarAuto, SETe.the(args));
//                else
//                    y = Equal.the(args.get(0), args.get(1)).neg();

                            break;

                        case "comment":
                        case "documentation":
                        case "externalImage":
                            if (includeDoc) {
                                if (args.size() == 3) {
                                    Term subj = args.get(0);
                                    Term lang = args.get(1);
                                    Term desc = $.quote(args.get(2));
                                    try {
                                        y = INH.the($.p(subj, desc), lang);
                                    } catch (Exception e) {
                                        y = null;
                                    }
                                } else {
                                    throw new UnsupportedOperationException();
                                }
                            } else {
                                finished = true;
                                break;
                            }
                            break;

                        case "termFormat": {
                            if (includeDoc) {
                                String language = args.get(0).toString();
                                language = language.replace("Language", "");

                                Term term = args.get(1);
                                Term string = args.get(2);
                                y = INH.the($.p($.atomic(language), string), term);
                            } else {
                                finished = true;
                                break;
                            }
                            break;
                        }


                        default:

                            break;
                    }
                    if (!finished) {
                        if (y == null) {


                            Term z = formulaToTerm(xCar, level + 1);

                            if (z != null) {
                                switch (z.toString()) {
                                    case "and":
                                        y = CONJ.the(args);
                                        break;
                                    case "or":
                                        y = DISJ(args.toArray(EmptyTermArray));
                                        break;
                                    case "not":
                                        y = args.get(0).neg();
                                        break;
                                    default:
                                        if (!(z instanceof Variable))
                                            y = INH.the($.p(args), z);
                                        else {
                                            args.add(0, z);
                                            y = $.p(args);
                                        }
                                        break;
                                }

                            }


                        }
                        if (y instanceof Bool) {
                            logger.warn("{} Bool singularity: args={}", x, args);
                            result = Null;
                        } else {
                            result = y;
                        }
                    }
                }
            }
        }


        return result;
    }

    @Override
    public Iterator<Task> iterator() {
        return tasks().iterator();
    }


    static class FnDef {
        final IntObjectHashMap<Term> domain = new IntObjectHashMap<>();
        Term range;
    }


    private static class VarOnlySet extends ForwardingSet<Term> {
        private final MutableSet<Term> _aVars;

        VarOnlySet(MutableSet<Term> _aVars) {
            this._aVars = _aVars;
        }

        /**
         * HACK because recurseTermsToSet isnt designed to check only Op
         */
        @Override
        public boolean add(Term key) {
            return !(key instanceof Variable) || super.add(key);
        }

        @Override
        protected java.util.Set<Term> delegate() {
            return _aVars;
        }
    }
}