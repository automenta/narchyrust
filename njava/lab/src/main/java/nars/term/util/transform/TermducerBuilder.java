package nars.term.util.transform;

import jcog.data.set.ArrayHashSet;
import nars.Term;
import nars.io.IO;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.ByteSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.FSTCompiler;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * using FST finite state transducer to transform terms (experiment)
 * see http://citeseerx.ist.psu.edu/viewdoc/download;jsessionid=433691F581BC2E624185F79465C5329?doi=10.1.1.24.3698&rep=rep1&type=pdf
 * <p>
 * not thread safe
 * <p>
 * TODO use either this or the ByteSeek stuff
 */
public class TermducerBuilder {

    final ArrayHashSet<Pair<IntsRef, Twin<Term>>> xy = new ArrayHashSet<>();


    public void put(Term x, Term y) {
        xy.add(pair(Termducer.ints(x), Tuples.twin(x, y)));
    }

    public Termducer build() throws IOException {

        var b = new FSTCompiler.Builder(
                FST.INPUT_TYPE.BYTE1, ByteSequenceOutputs.getSingleton()
        ).build();

        xy.list.sort(Comparators.byFunction(Pair::getOne));

        for (Pair<IntsRef, Twin<Term>> z : xy.list) {
            try {
                b.add(z.getOne(), bytes(z.getTwo().getTwo()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new Termducer(FST.fromFSTReader(b.compile(), b.getFSTReader()));
    }


    public static BytesRef bytes(Term y) {
        return new BytesRef(IO.termToBytes(y));
    }


    public record Termducer(FST<BytesRef> fst) {

        @Nullable
        public Term get(IntsRef x) {
            try {
                final BytesRef b = _get(x);
                return b != null ? IO.bytesToTerm(b.bytes) : null;
            } catch (IOException e) {
                //e.printStackTrace();
                return null;
            }
        }

        private BytesRef _get(IntsRef x) throws IOException {
            return org.apache.lucene.util.fst.Util.get(fst, x);
        }

        public Term get(Term x) {
            return get(ints(x));
        }

        private static IntsRef ints(Term x) {
            IntsRefBuilder b = new IntsRefBuilder();
            b.copyUTF8Bytes(bytes(x));
            return b.toIntsRef();
        }

    }
}