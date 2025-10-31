package nars.test;

import nars.NAR;
import nars.NARS;
import org.junit.jupiter.api.AfterEach;
import org.opentest4j.AssertionFailedError;

import java.lang.reflect.Method;

public abstract class NALTest {

    public TestNAR test;

    protected NALTest() {
        test = new TestNAR(nar());
    }

    public static NALTest test(TestNAR tt, Method m) {
        NALTest t = null;
        try {
            t = (NALTest) ((Class) m.getDeclaringClass()).getConstructor().newInstance();
            t.test = (tt);

            m.invoke(t);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            return t;
        }

        try {
            t.test.run();
        } catch (AssertionFailedError ae) {
          //ignore
        } catch (RuntimeException ee) {
          ee.printStackTrace();
        }
        return t;
    }

    protected NAR nar() {
        return NARS.tmp();
    }

    @AfterEach
    public final void end() {
        try {
            test.run();
        } finally {
            var n = test.nar;
            test = null;
            n.delete();
        }
    }

}