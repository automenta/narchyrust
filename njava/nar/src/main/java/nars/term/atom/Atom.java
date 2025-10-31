package nars.term.atom;

import jcog.The;
import nars.term.util.TermException;

import static nars.Op.*;

/**
 * default Atom implementation: wraps a String instance as closely as possible.
 * ideally this string is stored encoded in UTF8 byte[]'s
 */
public class Atom extends Atomic implements The {


//    /** use with caution */
//    public Atom(byte[] b) {
//        super(b);
//    }

    public Atom(String id) {
        super((byte) 0 /* = ATOM.id */, id.getBytes());
    }

    static String validateAtomID(String id) {
        if (id.isEmpty())
            throw new UnsupportedOperationException("Empty Atom ID");

        switch (id.charAt(0)) {
            case '+', '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '^', '?', '%', '#', '$' ->
                throw new TermException("invalid " + Atom.class + " name \"" + id + "\": leading character imitates another operation type");
        }

        return id;
    }

    public static boolean validAtomChar(char x) {
        return switch (x) {
            case
                ' ', '\t', '\n', '\r',
                ARGUMENT_SEPARATOR,
                BELIEF, GOAL, QUESTION, QUEST, COMMAND,
                '<', '>',
                '~', '=',
                '+', '-',
                '*',
                '|', '&',
                COMPOUND_OPEN, COMPOUND_CLOSE,
                '[', ']', '{', '}',
                '%', '#', '$',
                ':', '`',
                '/', '\\',
                '\"', '\'',
                DeltaChar,
                0 -> false;
            default -> true;
        };
    }

    /**
     * determines if the string is invalid as an unquoted target according to the characters present
     * assumes len > 0
     */
    static boolean quoteable(CharSequence t) {

        char t0 = t.charAt(0);

        if (Character.isDigit(t0))
            return true;

        int len = t.length();
        if ((t0 == '\"') && (t.charAt(len - 1) == '\"'))
            return false;

        for (int i = 0; i < len; i++) {
            if (!validAtomChar(t.charAt(i)))
                return true;
        }
        return false;
    }

//    public static int hashCode(int id, Op o) {
//        return (id + o.id) * 31;
//    }

    @Override
    public final byte opID() {
        return ATOM.id;
    }


    @Override
    public final boolean hasVarDep() {
        return false;
    }

    @Override
    public final boolean hasVarQuery() {
        return false;
    }

    @Override
    public final boolean hasVarIndep() {
        return false;
    }

    @Override
    public final boolean hasVarPattern() {
        return false;
    }


//    public boolean startsWith(byte... prefix) {
//        byte[] b = bytes();
//        int o = 3; //skip op byte + 2 len bytes
//        int P = prefix.length;
//        if (b.length - o >= P) {
//            return IntStream.range(0, P).noneMatch(i -> b[i + o] != prefix[i]);
//        }
//        return false;
//    }


}