package nars.term.util;

import jcog.data.map.ObjIntHashMap;
import jcog.util.ArrayUtil;
import nars.$;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TmpTermList;
import nars.term.Compound;
import nars.term.var.Variable;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.ListIterator;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;

/**
 * compound explode/implode
 * <p>
 * ex:
 * ((a,(b,c)) --> (b,c))
 * |-
 * (equal((a,(b,c)),(#1,#2)) && ((#1,#2) --> #2))
 */
public class Explode {


    @Nullable public final Term outEqXY;

    @Deprecated static private final boolean conjOrImpl = true;

    public Explode(Compound in, int componentsMax, int copiesMin, int volMax) {

        assert(copiesMin>=2);

        ObjIntHashMap<Term> ss = Terms.subtermScore(in.subterms(),
                x -> x.isAny(Variables | IMG.bit) ? 0 : 1,
                copiesMin);
        if (ss == null) {
            outEqXY = null;
            return;
        }

        MutableList<ObjectIntPair<Term>> FREQ = ss
                .keyValuesView()
                //.select(occurrencesMin > 1 ? x -> x.getTwo() >= occurrencesMin : x -> true)
                .toSortedList(Comparator.comparingInt(t -> -t.getOne().complexity()));
                //.toList();
        int fl = FREQ.size();
        if (fl == 0) {
            outEqXY = null;
            return;
        }

        outer: for (int i = 0; i < fl - 1; i++) {
            Term j = FREQ.get(i).getOne();
            if (!(j instanceof Compound)) continue;
            ListIterator<ObjectIntPair<Term>> ii = FREQ.listIterator(i + 1);
            while (ii.hasNext()) {
                if (j.containsRecursively(ii.next().getOne())) {
                    ii.remove();
                    fl = FREQ.size();
                    if (fl == 1)
                        break outer;
                }
            }

        }

        ObjectIntPair<Term>[] freq = FREQ.toArray(EmptyTermIntPairArray);

        if (freq.length > 1) {
            Arrays.sort(freq, scorer);
            ArrayUtil.shuffleTiered(i->score.applyAsInt(freq[i]), (a, b) -> ArrayUtil.swap(freq, a, b), freq.length);
        }


        TmpTermList XY = new TmpTermList(componentsMax < Integer.MAX_VALUE ? componentsMax : 1);

        int varOffset = (conjOrImpl ? in.varDep() : in.varIndep()) + 1;
        Term out = in;
        int volEq = conjOrImpl ?  0 : 1;
        for (ObjectIntPair<Term> xi : freq) {
            Term xx = xi.getOne();
            Variable yy = (conjOrImpl ? $.varDep(varOffset) : $.varIndep(varOffset));
            Term outNext = out.replace(xx, yy);

            //replacement effective? if not, may already have been applied by a larger compound
            if (out.equals(outNext))
                continue;

            Term xy = EQ.the(xx, yy);
            if (XY.size()==1) volEq++; //for necessary CONJ that wraps them
            volEq += xy.complexity();
            if (volEq + outNext.complexity() > volMax) {
                if (XY.isEmpty()) {
                    outEqXY = Null;
                    return;
                } else {
                    break;
                }
            }

            XY.add(xy);

            varOffset++;
            out = outNext;
            if (XY.size() >= componentsMax)
                break;
        }


        if (conjOrImpl) {
            XY.add(out); outEqXY = CONJ.the((Subterms)XY);
        } else {
            outEqXY = IMPL.the(CONJ.the((Subterms) XY), out);
        }

        //assert(outEqXY.volume() <= volMax);
        //if (outEqXY.volume() > volMax) {
            //throw new WTF();
        //}
    }


    private static final IntFunction<ObjectIntPair<Term>> score = p -> -(p.getTwo() * Math.max(0, (p.getOne().complexity() - 1)));
    private static final Comparator<ObjectIntPair<Term>> scorer = Comparators.byIntFunction(score);

    //byIntFunction(p -> -(p.getTwo() * p.getOne().volume()));

//            <ObjectIntPair<Term>>byIntFunction(ObjectIntPair::getTwo)
//            .thenComparing(z -> z.getOne().volume())
//            .reversed();
}