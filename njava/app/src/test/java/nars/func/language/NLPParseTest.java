//package nars.func.language;
//
//
//import nars.NAR;
//import nars.NARS;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//class NLPParseTest {
//
//    @Test void nlpInputTest() {
//        NAR n = NARS.tmp();
//        n.complexMax.set(64);
//        var f = n.main();
//        f.log();
//        var i = new NLPInput(n);
//        f.acceptAll(i.parse("banana is yellow and will be brown."));
//        f.acceptAll(i.parse("cat is animal."));
//        f.acceptAll(i.parse("cat is an animal.")); //-> any(animal)
//        f.acceptAll(i.parse("cat is the animal.")); //-> the(animal)
//        f.acceptAll(i.parse("what is a cat?"));
//    }
//
//    @Test
//    void backticks1() {
//        NLPParse p = new NLPParse("(`this sentence` --> `refers to itself`)");
//        System.out.println(p);
//    }
//
//    @Test
//    void backticks2() {
//        System.out.println(new NLPParse("(--`raining outside` ==>+1 `shut windows`)"));
//        System.out.println(new NLPParse("((`warm outside` && `not raining outside`) ==>+1 `open windows`)"));
//    }
//
//    @Test void nlgen1() {
//        NAR n = NARS.tmp();
//        n.complexMax.set(64);
//        var f = n.main();
//        var i = new NLPInput(n);
//        var i2 = new NLPInput(n);
//
//        {
//            String xi = "cat is an animal.";
//            assertEquals(xi, i.generate(i.parse(xi).get(0)));
//        }
//        {
//            String xi = "dog is an animal.";
//            assertEquals(xi, i.generate(i2.parse(xi).get(0)));
//        }
//    }
//
//}