package nars.term;

import jcog.Hashed;
import jcog.TODO;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.compound.SeparateSubtermsCompound;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.Nullable;


public class ProxyCompound extends SeparateSubtermsCompound  /* shouldnt this be separate subterms? */ implements Hashed /* optimistic */ {

    public final Compound ref;

    public ProxyCompound(Compound x) {
        super(x.opID, x.subterms());
        this.ref = x;

//        if (x instanceof ProxyCompound)
//            throw new TermException("instanceof ProxyTerm; caught attempt to proxy a proxy in " + getClass(), x);
//        if (x instanceof Neg && !(this instanceof Neg))
//            throw new TermException(ProxyCompound.class.getSimpleName() + " can not wrap NEG", x);
    }


    @Override
    public final Subterms subtermsDirect() {
        return ref.subtermsDirect();
    }

    @Override
    public final int dt() {
        return ref.dt();
    }

//    final Term ifDifferentElseThis(Term u) {
//		//continue proxying
//		return u == ref ? this : u;
//    }

    @Override
    public @Nullable Term replaceAt(ByteList path, int depth, Term replacement) {
        throw new TODO();
    }

    //    @Override
//    public int structureSurface() {
//        return ref.structureSurface();
//    }
//
//    @Override public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o instanceof ProxyCompound)
//            o = ((ProxyCompound)o).ref;
////        if (o instanceof Termed)
////            o = ((Termed)o).term();
//        return ref.equals(o);
//    }

    @Override
    public final int compareTo(Term t) {
        return ref.compareTo(t);
    }

    @Override
    public final int hashCode() {
//        int h = this.hash;
//        return h == 0 ? (this.hash = ref.hashCode()) : h;
        return ref.hashCode();
    }


}