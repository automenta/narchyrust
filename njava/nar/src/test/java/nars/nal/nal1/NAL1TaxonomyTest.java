package nars.nal.nal1;

import nars.NALTask;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * tests inheritance-based taxonomical inference
 */
public class NAL1TaxonomyTest {

    @Disabled
    @Test
    void testCommonAncestor() throws Narsese.NarseseException {
//        NAL.DEBUG = true;
//        NAL.causeCapacity.set(4);
        NAR n = NARS.tmp(1);
        n.believe("(a-->b)");
        n.believe("(b-->C)");
        n.believe("(y-->C)");
        n.believe("(z-->y)");

        //n.log();
        n.main().onTask(t -> NAR.proofPrint((NALTask) t));
        n.run(1000);

    }
}