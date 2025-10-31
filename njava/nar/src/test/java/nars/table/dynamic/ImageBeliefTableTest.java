package nars.table.dynamic;

import nars.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.term.util.Testing.assertEq;

class ImageBeliefTableTest {

    @Test
    void testInferTransform1() {
        Term t = $$("((tetris-->((--,score)&&density))-->(wonder,\"-ñô99EJÏDb\",/))");
        ImageBeliefTable i = new ImageBeliefTable(t, true);
        assertEq(t, i.term);
        assertEq("wonder(\"-ñô99EJÏDb\",(tetris-->((--,score)&&density)))", i.resolve(i.term));

//        assertEq("", i.transformFromTemplate($$("((tetris-->((--,score)&&density))-->(wonder,\"-ñô99EJÏDb\",/))")));
    }

}