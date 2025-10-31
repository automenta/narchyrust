package nars.nar;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import nars.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaGoalTest {
    @Disabled @Test void causesAppliedToDerivations() throws Narsese.NarseseException {

        //test causes of inputs (empty) and derivations (includes all necessary premise construction steps)
        Multimap<Term, NALTask> tasks = MultimapBuilder.hashKeys().linkedHashSetValues().build();
        NAR n = NARS.tmp(1);
//        n.main().onTask(t -> {
//            Term why = t.why();
//            if (why!=null)
//                tasks.put(why, (NALTask) t);
//        });
        n.input("(x-->y).");
        n.input("(y-->z).");
        int cycles = 128;
        n.run(cycles);


        n.causes.why.forEach(w -> System.out.println(w.id + " " + w));
        tasks.forEach((c,t)-> System.out.println(c + "\t" + t));

        assertTrue(tasks.size() > 2);
        Collection<NALTask> tt = tasks.values();
        Predicate<NALTask> isDerived = x -> !x.isInput();
        long count = tt.stream().filter(isDerived).count();
        assertTrue(count >= 1);

//        assertTrue(tt.stream().allMatch(x -> {
//            int ww = new ShortHashSet(x.why().complexity()).size();
//            if (x.stamp().length == 1) {
//                //input
//                System.out.print("IN ");
//                if (ww!=0)
//                    return false;
//            } else {
//                System.out.print("DE ");
//                if (ww < 3)
//                    return false;
//            }
//            System.out.println(ww + "\t" + x);
//            return true;
//        }));

    }



    private static void analyzeCauses(NAR n) {

        SortedMap<String, Object> x = n.stats(true, true);
        for (Map.Entry<String, Object> entry : x.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();
            System.out.println(k + '\t' + v);
        }
    }
}