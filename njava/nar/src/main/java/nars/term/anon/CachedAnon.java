package nars.term.anon;

import jcog.data.list.Lst;
import jcog.data.map.UnifriedMap;
import nars.Term;
import nars.term.Compound;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;

/** TODO implement these as CachedTransform-wrapped sub-implementations of the Anon.GET and PUT transforms, each with their own cache */
public class CachedAnon extends Anon {

    final UnifriedMap<Compound,Term> putCache =
            new UnifriedMap();
    final UnifriedMap<Compound,Term> getCache =
            new UnifriedMap();

    public CachedAnon() {
        super();
    }

    public CachedAnon(int cap) {
        super(cap);
    }

    @Override
    public void clear() {
        super.clear();
        invalidate();
    }

    public void rollback(int toUniques) {
        if (toUniques == 0) {
            clear();
        } else {
            int max;
            if (toUniques < (max = uniques())) {
                ObjectByteHashMap<Term> termToId = map.termToId;
                Lst<Term> idToTerm = map.idToTerm;
                for (int i = toUniques; i < max; i++)
                    termToId.removeKey(idToTerm.get(i));
                idToTerm.removeAbove(toUniques);

                invalidate(); //not full clear() just invalidate()
            }
        }
    }

    protected void invalidate() {
        putCache.clear();
        getCache.clear();
    }

    @Override
	public final Term applyCompound(Compound x) {
        return putOrGet ?
            putCache.computeIfAbsent(x, this::putCache)
            :
            getCache.computeIfAbsent(x, this::getCache);
    }

//    /** whether a target is cacheable */
//    protected static boolean cache(Compound x, boolean putOrGet) {
//        return true;
//    }

    private Term putCache(Compound x) {
        Term y = super.applyCompound(x);
        if (y instanceof Compound)
            getCache.put((Compound) y, x);
        return y;
    }

    private Term getCache(Compound xx) {
        return super.applyCompound(xx);
    }
}