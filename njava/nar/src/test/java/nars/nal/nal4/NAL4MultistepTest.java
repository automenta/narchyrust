package nars.nal.nal4;

import nars.NAR;
import nars.NARS;
import nars.Term;
import nars.test.NALTest;
import org.junit.jupiter.api.Test;

import static nars.$.$$;

class NAL4MultistepTest extends NALTest {
    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(6);
//        n.confMin.setAt(0.25f);
        n.complexMax.set(16);
        return n;
    }


    @Test
    void nal4_everyday_reasoning() {

        test.nar.complexMax.set($$("(likes(cat,[blue]) <-> likes({tom},[blue]))").complexity()+2);

        //tester.nar.freqResolution.setAt(0.05f);
//        tester.nar.questionPriDefault.pri(0.5f);
        test.confMin(0.2f);
        test.confTolerance(0.4f);

        test.input("({sky} --> [blue]).");
        test.input("({tom} --> cat).");
        test.input("likes({tom},{sky}).");

        test.input("likes(cat,[blue])?");

        int time = 100;
        test.mustBelieve(time, "(likes(cat,[blue]) <-> likes(cat,{sky}))",1f,0.45f);
        test.mustBelieve(time, "(likes(cat,[blue]) <-> likes({tom},[blue]))",1f,0.45f);
        test.mustBelieve(time, "(likes(cat,[blue]) <-> likes({tom},{sky}))",1f,0.45f);

        test.mustBelieve(time, "likes(cat,[blue])",
                1f,
                0.45f);
    }

    @Test
    void nal4_everyday_reasoning_easiest() {


        int cycles = 100;
        test
            .confMin(0.2f)
            .volMax(15)
            .believe("blue:sky", 1.0f, 0.9f)
            .believe("likes:sky", 1.0f, 0.9f)
            .question("likes:blue")
            .mustBelieve(cycles, "(likes-->blue)", 1, 0.45f)
            .mustBelieve(cycles, "(blue-->likes)", 1, 0.45f)
            .mustBelieve(cycles, "likes:blue", 1.0f, 0.45f /* 0.45? */);

    }

    @Test void nal4_everyday_reasoning_easier_sim() {

        test.confTolerance(0.1f);
        test.confMin(0.2f);
        test.volMax(12);
//        test.nar.freqResolution.setAt(0.25f);
//        test.nar.confResolution.setAt(0.1f);

//        Term cat = $$("cat");
//        Term blue = $$("blue");
        Term answer = $$("likes(cat,blue)");

        int cycles = 100;
        test.believe("(blue<->sky)")
            .believe("(cat<->tom)")
            .believe("likes(tom,sky)")
            .question(answer.toString())
            .mustBelieve(cycles, answer.toString(), 1.0f, 0.1f)
//            .mustBelieve(time, Image.imageExt(answer, cat).toString() /* (cat-->(likes,/,blue))  */, 1.0f, 0.27f /*0.45f*/)
//            .mustBelieve(time, Image.imageExt(answer, blue).toString() /* (blue-->(likes,cat,/)) */, 1.0f, 0.27f /*0.45f*/)
        ;

    }
    @Test void nal4_everyday_reasoning_easier_inh() {

        test.confTolerance(0.3f);
        test.confMin(0.25f);
        test.volMax(8);
//        test.nar.freqResolution.setAt(0.25f);
//        test.nar.confResolution.setAt(0.1f);

//        Term cat = $$("cat");
//        Term blue = $$("blue");
        Term answer = $$("likes(cat,blue)");

        int time = 150;
        test.believe("blue:sky")
            .believe("cat:tom")
            .believe("likes(tom,sky)")
            .question("likes(cat,blue)")
            .mustBelieve(time, answer.toString(), 1.0f, 0.3f)
//            .mustBelieve(time, Image.imageExt(answer, cat).toString() /* (cat-->(likes,/,blue))  */, 1.0f, 0.27f /*0.45f*/)
//            .mustBelieve(time, Image.imageExt(answer, blue).toString() /* (blue-->(likes,cat,/)) */, 1.0f, 0.27f /*0.45f*/)
        ;

    }


}