package nars.term.builder;

import jcog.WTF;
import jcog.data.map.BucketByteMap8;
import jcog.data.map.LazyMap;
import jcog.memoize.ByteKey;
import jcog.memoize.ByteKeyExternal;
import jcog.signal.meter.SafeAutoCloseable;
import nars.NAL;
import nars.Op;
import nars.Term;
import nars.subterm.ByteCachedSubterms;
import nars.subterm.Subterms;
import nars.subterm.TmpTermList;
import nars.term.Compound;

import java.util.function.Function;

import static nars.Op.*;

public class MultiInterningTermBuilder extends InterningTermBuilder {

    private final Function<ByteKeyExternal, InternedArraySubterms> memo;

    @Deprecated public MultiInterningTermBuilder(TermBuilder _builder) {
        this(_builder, NAL.term.interningComplexityMax);
    }

    public MultiInterningTermBuilder(TermBuilder _builder, int volMax) {
        this(_builder, atomCapacityDefault, compoundCapacityDefault, volMax);
    }

    private MultiInterningTermBuilder(TermBuilder _builder, int atomCap, int compoundCap, int compoundVolMax) {
        super(_builder, atomCap, compoundVolMax);

        memo = memoizer(InternedArraySubterms.class.getSimpleName(), j ->
                        new InternedArraySubterms(
                                j instanceof Intermed.InternedTermArray ji ?
                                        _subtermsNew(ji.subs) :
                                        //new TmpTermList(((Intermed.InternedTermArray) j).subs) :

//                    subtermBuilder.apply(array(((Intermed.SubtermsKey) j).subs))
                                        ((Intermed.SubtermsKey) j).subs
                                , j)
                , compoundCap);
    }

    @Override
    public Term compound1New(Op o, Term x) {
        if (internable(x)) {
            var c = _subtermsInterned(x);
            return c.data.computeIfAbsent((byte) (InternedArraySubterms.COMPOUND_1 + o.id), () ->
                o.build(termBuilder, c.sub(0))
            );
        } else {
            return termBuilder.compound1New(o, x);
        }
    }

    @Override
    @Deprecated public final Term compoundNew(Op o, int dt, Term... t) {
        return compoundNew(o, dt, new TmpTermList(t));
    }

    @Override
    public Term compoundNew(Op o, int dt, Subterms subs) {
        var ss = Subterms.array(subs);
        return internableTerms(subs) ?
            _compoundNewInterned(o, dt, ss) :
            _compoundNew(o, dt, _subtermsNew(ss));
    }

    private Term _compoundNewInterned(Op o, int dt, Term[] ss) {
        var subsFinal = _subtermsInterned(ss);
        return intern(dt) ?
                compoundNInterned(o, dt, subsFinal) //direct=o!=CONJ && !subs.hasVars() /* HACK */
                :
                _compoundNew(o, dt, subsFinal);
    }

    private Term _compoundNew(Op o, int dt, Subterms subs) {
        return termBuilder.compoundNew(o, dt, subs instanceof InternedArraySubterms sis ? sis.ref : subs);
    }

    @Override
    protected Term statementInterned(Op o, int dt, Term S, Term P) {
        Term[] sp;
        return intern(dt) && internableTerms(sp = new Term[] { S, P }) ?
            compoundNInterned(o, dt, _subtermsInterned(sp)) :
            termBuilder.statementNew(o, dt, S, P);
    }

    private Term compoundNInterned(Op o, int dt, InternedArraySubterms target) {
        return compoundNInterned(o, dt, target, false);
    }

    private Term compoundNInterned(Op o, int dt, InternedArraySubterms target, boolean direct) {
        return target.data.computeIfAbsent((byte) (switch (dt) {
            case DTERNAL -> InternedArraySubterms.COMPOUND_N;
            case XTERNAL -> InternedArraySubterms.COMPOUND_N_XTERNAL;
            default -> throw new UnsupportedOperationException();
        } + o.id),
            () -> direct ?
                termBuilder.compoundNew(o, dt, target) : o.build(termBuilder, dt, target)
        );
    }

    @Override
    protected Term conjIntern(int dt, Term[] u) {
        var s = _subtermsInterned(u);
        return s.data.computeIfAbsent(conjInternCode(dt),
            () -> _conjNew(dt, new TmpTermList(s.ref)));
    }

    private static byte conjInternCode(int dt) {
        return (byte) (((dt == DTERNAL) ? InternedArraySubterms.COMPOUND_N : InternedArraySubterms.COMPOUND_N_XTERNAL) + CONJ.id);
    }

    @Override
    public Compound normalize(Compound x, byte varOffset) {
        return normalizeInterned(x, varOffset) ? normalizeInterned(x) : super.normalize(x, varOffset);
    }

    private boolean normalizeInterned(Compound x, byte varOffset) {
        return varOffset == 0 && x.dt() == DTERNAL && internable(x);
    }

    private Compound normalizeInterned(Compound x) {
        return (Compound) subtermsInternMap(x).computeIfAbsent(
                (byte) (InternedArraySubterms.NORMALIZE + x.opID), () ->
                        super.normalize(x, (byte) 0)
        );
    }

    @Override
    public Term root(Compound x) {
        var dt = x.dt();
        return intern(dt) && internable(x) ?
            rootInterned(x, dt)
            :
            super.root(x);
    }

    private Term rootInterned(Compound x, int dt) {
        return subtermsInternMap(x).computeIfAbsent(rootCode(x, dt),
            () -> super.root(x)
        );
    }

    private static byte rootCode(Compound x, int dt) {
        return (byte) (x.opID + (dt == DTERNAL ? InternedArraySubterms.ROOT : InternedArraySubterms.ROOT_XTERNAL));
    }

    private static boolean intern(int dt) {
        return dt == DTERNAL || dt == XTERNAL;
    }

    @Override
    protected Subterms subtermsInterned(Term[] t) {
        return _subtermsInterned(t).ref;
    }

    private InternedArraySubterms _subtermsInterned(Term... t) {
        return memo.apply(new Intermed.InternedTermArray(t));
    }

    private LazyMap<Byte, Term> subtermsInternMap(Compound c) {
        return subtermsInterned(c.subtermsDirect()).data;
    }

    private InternedArraySubterms subtermsInterned(Subterms t) {
        return memo.apply(new Intermed.SubtermsKey(t));
    }

    /**
     * TODO make this extends ArrayCachedSubterms
     */
    private static class InternedArraySubterms extends ByteCachedSubterms implements SafeAutoCloseable {

        private static final byte ops = (byte) values().length;

        //new BucketBucketByteMap<>(8, 8, EmptyTermArray);
        //new CompactArrayMap()
        //new SynchroNiceByteMap(4);
        //new CompleteByteMap(new Object[maxOps]);
        private static final byte COMPOUND_N = 0;
        private static final byte COMPOUND_1 = (byte) (ops + COMPOUND_N);
        private static final byte COMPOUND_N_XTERNAL = (byte) (ops + COMPOUND_1);
        private static final byte ROOT = (byte) (ops + COMPOUND_N_XTERNAL);  //DTERNAL ONLY
        private static final byte ROOT_XTERNAL = (byte) (ops + ROOT);  //DTERNAL ONLY
        private static final byte NORMALIZE = (byte) (ops + ROOT_XTERNAL); //DTERNAL ONLY
        private static final int _maxOps = NORMALIZE + ops;
        static {
            if (_maxOps > Byte.MAX_VALUE) throw new WTF("problem in " + MultiInterningTermBuilder.class);
        }

        private final LazyMap<Byte, Term> data = new BucketByteMap8<>();

        private InternedArraySubterms(Subterms s, ByteKey k) {
            super(s, k.array());
        }

        @Override
        public void close() {
            data.clear();
        }

    }

}