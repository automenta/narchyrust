package nars.nal.nal6;

import nars.Deriver;
import nars.NAR;
import nars.NARS;
import nars.TruthFunctions;
import nars.deriver.impl.SerialDeriver;
import nars.deriver.reaction.Reactions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled class XORTest extends AbstractNAL6Test {

    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(1);

        Reactions xor = new Reactions().files("xor.nal", "equal.nal");
        Deriver d = new SerialDeriver(xor.compile(n), n); //new BagDeriver(xor, n);
        d.everyCycle(n.main());

        return n;
    }

    @Test
    void xor_fwd() {
        test.volMax(15).confMin(0.4f)
                .believe("((x && --y)-->a)")
                .believe("((y && --x)-->a)")
                .mustBelieve(cycles, "(({x,y}-->xor)-->a)", 1f, 0.81f);

//        test.volMax(9).confMin(0.4f)
//                .believe("(x && --y)")
//                .believe("(y && --x)")
//                .mustBelieve(cycles, "xor:{x,y}", 1f, 0.81f);
    }

    @Test
    void xor_rev_x_pos() {
        test.volMax(9).confMin(0.4f).believe("xor:{x,y}")
                .believe("x").mustBelieve(cycles, "--y", 1f, 0.81f);
    }
    @Test
    void xor_rev_x_neg() {
        test.volMax(9).confMin(0.4f).believe("xor:{x,y}")
                .believe("--x").mustBelieve(cycles, "y", 1f, 0.81f);
    }

    @Test void conjAND_OR_mix() {
        assertEquals(0.9f, TruthFunctions.e2c(TruthFunctions.c2e(0.81f)+ TruthFunctions.c2e(0.81f)), 0.01f);
        test.volMax(5)
                .believe("(x && y)")
                .believe("(--x && --y)")
                .mustBelieve(cycles, "eqv:{x,y}", 1f, 0.81f)
                .mustNotBelieve(cycles,"x", 0.5f, 0.9f)
                .mustNotBelieve(cycles,"y", 0.5f, 0.9f);
    }
}