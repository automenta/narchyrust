package nars.term.util.bytes;


import nars.Term;
import nars.io.IO;
import net.byteseek.matcher.multisequence.ListMultiSequenceMatcher;
import net.byteseek.matcher.multisequence.MultiSequenceMatcher;
import net.byteseek.matcher.sequence.ByteSequenceMatcher;
import net.byteseek.matcher.sequence.SequenceMatcher;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

public enum TermByteMatcher {;

    public static MultiSequenceMatcher any(Set<Term> t) {
        return new ListMultiSequenceMatcher(t.stream().map(TermByteMatcher::eq).collect(toSet()));
    }

    public static SequenceMatcher eq(Term z) {
        return new ByteSequenceMatcher(IO.termToBytes(z));
    }

}