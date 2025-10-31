package nars.term.util.transform;

import nars.Term;
import nars.term.atom.Int;
import org.apache.lucene.util.IntsRef;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TermducerBuilderTest {

    @Test void test() throws IOException {

        TermducerBuilder t = new TermducerBuilder();
        t.put($$("(a-->b)"), $$("(1-->2)"));
        t.put($$("(a-->(b,c))"), $$("(1-->(2,3))"));
        t.put($$("add(1,1))"), Int.i(2));

        TermducerBuilder.Termducer f = t.build();

        for (Pair<IntsRef, Twin<Term>> x : t.xy.list) {
            //System.out.println(x.getTwo() + " " + x.getOne() + f.get(x.getOne()));
            assertEquals(x.getTwo().getTwo(), f.get(x.getOne()));
            //System.out.println(x.getTwo() + " " + Termducer.ints(x.getTwo().getOne()) + " " + f.get(x.getTwo().getOne()));
        }

        System.out.println( f.get($$("((a-->(b,c)) &&+1 z)")) ); //TODO
    }
}