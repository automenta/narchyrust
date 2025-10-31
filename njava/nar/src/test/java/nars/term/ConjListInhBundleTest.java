package nars.term;

import nars.Op;
import nars.Term;
import nars.term.util.conj.ConjBuilder;
import nars.term.util.conj.ConjList;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;

@Disabled
public class ConjListInhBundleTest {
    @Test void conj1() {
        ConjList l = new ConjList(2);
        l.add(0L, $$("(--,(g-->((op,\"&&\")&&happy)))"));
        l.add(0L, $$("(--,(g-->(shift&&simple)))"));
        Term y = l.term();
        ConjTest.assertEq("(g-->((--,((op,\"&&\")&&happy))&&(--,(shift&&simple))))", y);
    }
    @Test
    void conjListInhBundle_0_subj() {
        ConjList l = new ConjList(2);
        l.add(0L, $$("(a-->x)"));
        l.add(0L, $$("(a-->y)"));
        l.inhBundle(Op.terms);
        ConjTest.assertEq("(a-->(x&&y))", l.term());
    }

    @Test
    void conjListInhBundle1() {
        ConjList l = new ConjList();
        l.add(0L, $$("(z-->a)"));
        l.add(1L, $$("(x-->a)"));
        l.add(1L, $$("(y-->a)"));
        l.inhBundle(Op.terms);
        ConjTest.assertEq("((z-->a) &&+1 ((x&&y)-->a))", l.term());
    }

    @Test
    void conjListInhBundleAmongOthers() {
        ConjList l = new ConjList();
        l.add(0L, $$("(z-->a)"));
        l.add(1L, $$("(x-->a)"));
        l.add(1L, $$("(y-->a)"));
        l.add(1L, $$("(w-->b)"));
        l.inhBundle(Op.terms);
        ConjTest.assertEq("((z-->a) &&+1 (((x&&y)-->a)&&(w-->b)))", l.term());
    }

    @Test
    void conjListInhBundleDouble() {
        ConjList l = new ConjList();
        l.add(0L, $$("(z-->a)"));
        l.add(1L, $$("(x-->a)"));
        l.add(1L, $$("(y-->a)"));
        l.add(1L, $$("(w-->b)"));
        l.add(1L, $$("(w-->c)"));
        l.inhBundle(Op.terms);
        ConjTest.assertEq("((z-->a) &&+1 (((x&&y)-->a)&&(w-->(b&&c))))", l.term());
    }

    @Test
    void conjListInhBundleDouble2() {
        ConjBuilder x =
                //new ConjTree();
                new ConjList();
        x.add(0L, $$("(z-->a)"));
        x.add(1L, $$("(x-->a)"));
        x.add(1L, $$("(y-->a)"));
        x.add(1L, $$("(w-->b)"));
        x.add(1L, $$("(m-->n)"));
        x.add(1L, $$("v"));
        //l.inhBundle(Op.terms);
        Term y = x.term();
        ConjTest.assertEq("((z-->a) &&+1 (&&,((x&&y)-->a),(m-->n),(w-->b),v))", y);
    }

    @Test
    void conjListInhBundleDoubleNegs() {
        ConjList l = new ConjList();
        l.add(0L, $$("(z-->a)"));
        l.add(1L, $$("--(x-->a)"));
        l.add(1L, $$("(y-->a)"));
        l.add(1L, $$("--(w-->b)"));
        l.add(1L, $$("(w-->c)"));
        l.inhBundle(Op.terms);
        ConjTest.assertEq("((z-->a) &&+1 ((((--,x)&&y)-->a)&&(w-->((--,b)&&c))))", l.term());
    }

    @Test
    void conjListInhBundle2() {
        ConjList l = new ConjList();
        l.add(2L, $$("(z-->a)"));
        l.add(1L, $$("(x-->a)"));
        l.add(1L, $$("(y-->a)"));
        l.inhBundle(Op.terms);
        //assertEquals(2, l.size());
        ConjTest.assertEq("(((x&&y)-->a) &&+1 (z-->a))", l.term());
    }

    @Test
    void conjListInhBundle1_neg() {
        ConjList l = new ConjList();
        l.add(1L, $$("(x-->a)"));
        l.add(1L, $$("--(y-->a)"));
        l.inhBundle(Op.terms);
        ConjTest.assertEq("(((--,y)&&x)-->a)", l.term());
    }

}
