package nars.term;

import nars.Term;
import nars.term.util.conj.ConjList;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConjListTest {


    @Test void conjListXternal() {
        ConjList e = ConjList.conds($$("(a &&+- b)"), false, true);
        assertEquals(2, e.size());
    }
    @Test void conjListXternalRepeat() {
        ConjList e = ConjList.conds($$("(a &&+- a)"), false, true);
        assertEquals(2, e.size());
    }
    @Test void conjListXternalInvert() {
        ConjList e = ConjList.conds($$("(a &&+- --a)"), false, true);
        assertEquals(2, e.size());
    }

    @Disabled
    @Test void conjListImage() {
        Term x = $$("((cat&&eats)-->(REPRESENT,/,?1))");
        ConjList e = ConjList.conds(x, 0L, true, false, true);
        assertEquals(2, e.size());
    }

}