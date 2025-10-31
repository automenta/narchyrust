package nars.deriver.util;

import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoize;
import nars.$;
import nars.Deriver;
import nars.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import org.eclipse.collections.api.list.primitive.ImmutableByteList;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public abstract class PremiseTermAccessor implements Function<Deriver, Term>, Termed {

    /** root component id: 0=task, 1=belief ... others could be defined later */
    public final int rootID;
    private final Atom root;

    protected PremiseTermAccessor(int id, Atom root) {
        this.rootID = id;
        this.root = root;
    }

    @Override
    public Term term() {
        return root;
    }

    @Override
    public final String toString() { return root.toString(); }

    final Memoize<ImmutableByteList,SubRootTermAccessor> subRoots =
            new HijackMemoize<>(SubRootTermAccessor::new,
                    512, 3);

    public Function<Deriver, Term> path(byte[] path) {
        return path.length == 0 ? this :
                subRoots.apply(ByteLists.immutable.of(path));
                //new SubRootTermAccessor(path);
    }

    private final class SubRootTermAccessor implements Function<Deriver, Term>, Termed {

        private final byte[] path;
        private final Term pathID;

        SubRootTermAccessor(ImmutableByteList path) {
            this(path.toArray());
        }

        SubRootTermAccessor(byte... path) {
            assert(path.length>0);
            this.pathID = $.inh($.p(path), root);
            this.path = path;
        }

        @Override
        public String toString() {
            return pathID.toString();
        }

        @Override
        public int hashCode() {
            return pathID.hashCode();
        }

        @Override
        public boolean equals(Object x) {
            return this == x || x instanceof SubRootTermAccessor s && pathID.equals(s.pathID);
        }

        @Override
        @Nullable
        public Term apply(Deriver d) {
            return PremiseTermAccessor.this.apply(d).subSafe(path);
        }

        @Override
        public Term term() {
            return pathID;
        }
    }
}