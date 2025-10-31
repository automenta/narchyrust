package nars.deriver.reaction;

import com.google.common.base.Splitter;
import jcog.data.set.ArrayHashSet;
import jcog.memoize.CaffeineMemoize;
import nars.Deriver;
import nars.NAR;
import nars.TruthFunctions;
import nars.deriver.reaction.compile.DecisionTreeReactionCompiler;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;

/**
 * reaction metaprogram
 * <p>
 * a set of related rules, forming a module that can be combined with other rules and modules
 * to form customized derivers, compiled together.
 * <p>
 * intermediate representation of a set of compileable Premise Rules
 * TODO remove this class, just use Set<PremiseDeriverProto>'s
 */
public class Reactions {

    private final ArrayHashSet<Reaction<Deriver>> reactions;

    public Reactions(ArrayHashSet<Reaction<Deriver>> r) {
        this.reactions = r;
    }

    public Reactions() {
        this(new ArrayHashSet<>(1024));
    }

    public Reactions rules(String... rules) {
        add(PatternReaction.parse(rules));
        return this;
    }

    private static Stream<Reaction<Deriver>> loadFile(String filename) {
        return loadFile.apply(filename).stream();
    }

    /** Cache<String filename -> Collection<Reaction>> */
    private static final Function<String, List<Reaction<Deriver>>> loadFile = CaffeineMemoize.build((String n) ->
            PatternReaction.parse(load(new String(bytes(n))), TruthFunctions.the).toList()
    , 128, false);

    private static byte[] bytes(String path) {
        try (var nn = NAR.class.getClassLoader().getResourceAsStream(path)) {
            return nn.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<String> load(CharSequence data) {
        return Splitter.on('\n')
            .omitEmptyStrings()
            .trimResults()
            .splitToStream(data)
            .filter(s -> !s.startsWith("//"));
    }

    public int size() {
        return reactions.size();
    }

    public ReactionModel compile(NAR n) {
        var c =
            //new DAGReactionCompiler();
            new DecisionTreeReactionCompiler();
            //new TrieReactionCompiler();
        return c.compile(reactions, n);
    }

    public final Reactions add(MutableReaction r) {
        r.compile();
        if (!this.reactions.add(r))
            throw new UnsupportedOperationException("Duplicate Reaction:" + r);
        return this;
    }

    public final Reactions add(Stream<Reaction<Deriver>> r) {
        r.collect(toCollection(() -> this.reactions));
        return this;
    }

    public final Reactions addAll(MutableReaction... h) {
        for (var hh : h)
            add(hh);
        return this;
    }

    public Reactions files(String... filenames) {
        return add(Stream.of(filenames).flatMap(Reactions::loadFile));
    }
}