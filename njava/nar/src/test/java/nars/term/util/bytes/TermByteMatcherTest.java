package nars.term.util.bytes;

import nars.io.IO;
import net.byteseek.compiler.CompileException;
import net.byteseek.compiler.matcher.SequenceMatcherCompiler;
import net.byteseek.matcher.multisequence.MultiSequenceMatcher;
import net.byteseek.matcher.sequence.SequenceMatcher;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TermByteMatcherTest {

    @Test
    void test1() {
        {
            MultiSequenceMatcher z = TermByteMatcher.any(Set.of($$("a"), $$("b")));
            System.out.println(z);
        }
        {
            SequenceMatcher z = TermByteMatcher.eq($$("a"));
            System.out.println(z.toRegularExpression(false));
            try {
                z = SequenceMatcherCompiler.compileFrom(z.toRegularExpression(false));
            } catch (CompileException e) {
                e.printStackTrace();
            }

            assertTrue(z
                    .matches(IO.termToBytes($$("a")), 0));
        }
    }
}