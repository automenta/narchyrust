package nars.term;

import nars.$;
import nars.Term;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static nars.$.$$;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TermPathTest {

    @Test
    void test1() {
        assertEq(
                "(#1-->f)",
                $$("f(x)").replaceAt(ByteLists.immutable.of((byte)0), $.varDep(1))
        );
    }

    @Test
    void test2() {
        assertEq(
                "f(#1)",
                $$("f(x)").replaceAt(ByteLists.immutable.of((byte)0, (byte)0), $.varDep(1))
        );
    }
    @Test
    void test3() {
        assertEq(
                "f(x,#1)",
                $$("f(x,y)").replaceAt(ByteLists.immutable.of((byte)0, (byte)1), $.varDep(1))
        );
    }
    @Test
    void testUnmodified() {
        Term x = $$("x");
        Term p = $.p(x);
        assertSame(
                p,
                p.replaceAt(ByteLists.immutable.of((byte)0), x)
        );
    }

    @Test void pathsDirect() {
        Compound x = $.$$c("(y)");
        List<byte[]> p = x.pathsToList(x);
        assertEquals(1, p.size());
        assertEquals(0, p.getFirst().length);
    }

    @Test void pathsTo1() {
        Compound x = $.$$c("((x &&+5 y) ==>+5 y)");
        List<byte[]> p = x.pathsToList($$("y"));
        //assertEquals("[[0, 1], [1]]", p.toString());
        assertEquals(2, p.size());
        assertEquals("[0, 1]", Arrays.toString(p.get(0)));
        assertEquals("[1]", Arrays.toString(p.get(1)));
    }
    @Test void pathsTo2() {
        Compound x = $.$$c("((a,b),(x,(a,b)))");
        List<byte[]> p = x.pathsToList($$("(a,b)"));
        assertEquals(2, p.size());
        assertEquals("[0]", Arrays.toString(p.get(0)));
        assertEquals("[1, 1]", Arrays.toString(p.get(1)));

        assertEquals(2, x.pathsToList($$("a")).size());
        assertEquals(2, x.pathsToList($$("b")).size());
        assertEquals(1, x.pathsToList($$("x")).size());
    }

}