package nars.term.compound;

import nars.$;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;

import static nars.Op.*;

/** use with extreme caution when op is not PROD */
public final class LightCompound extends SeparateSubtermsCompound  {

    public LightCompound(Op o, Term... s) {
        this(o.id, s);
    }
    public LightCompound(Op o, Subterms s) {
        this(o.id, s);
    }

    public LightCompound(byte o, Term... s) {
        this(o, $.vFast(s));
    }
    public LightCompound(byte o, Subterms s) {
        super(o, s);
    }


    public static Compound the(Op o, Term... s) {
        return switch (s.length) {
            case 0 -> o==PROD ? EmptyProduct : new LightCompound(o, EmptySubterms); //HACK
            case 1 -> new LightUnitCompound(o, s[0]);
            default -> new LightCompound(o, s);
        };
    }

    public static Compound the(Op o, Subterms s) {
        return switch (s.subs()) {
            case 0 -> o==PROD ? EmptyProduct : new LightCompound(o, EmptySubterms); //HACK
			case 1 -> new LightUnitCompound(o, s.sub(0));
			default -> new LightCompound(o, s);
		};
    }


    @Override
    public int dt() {
        return DTERNAL;
    }

}