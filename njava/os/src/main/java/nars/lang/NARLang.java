package nars.lang;

import com.google.common.util.concurrent.RateLimiter;
import jcog.Log;
import jcog.TODO;
import jcog.data.list.Lst;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.decide.MutableRoulette;
import jcog.event.ListTopic;
import jcog.pri.bag.util.Bagregate;
import jcog.pri.op.PriMerge;
import nars.Focus;
import nars.NALTask;
import nars.Narsese;
import nars.Task;
import org.jetbrains.annotations.Nullable;
import org.jline.reader.*;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NARLang {

    private static final int NAR_OUT_CAPACITY = 16;
    public final Focus focus;
    public final Graph graph;
    public final Attention attention;

    private final LM lmChat, lmTranslate;
    private final ExecutorService executor = Executors.newWorkStealingPool();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final jcog.event.Topic<String> notice = new ListTopic<>();

    private final RateLimiter inputRate, addRate;
    private final Bagregate<NALTask> narOut = new Bagregate<>(NAR_OUT_CAPACITY, PriMerge.max);
    private final List<Operation> operations = new Lst<>();
    private final static int stampCapacity = 8;
    private static final Logger logger = Log.log(NARLang.class);

    public NARLang(Focus focus, Attention attention,
                   LM lmChat, LM lmTranslate,
                   int inputRate, float updateRate,
                   int graphCapacity, long maxMemoryMB) {
        this.focus = focus;
        this.attention = attention;
        this.lmChat = lmChat;
        this.lmTranslate = lmTranslate;
        this.graph = new Graph(graphCapacity);

        this.inputRate = RateLimiter.create(inputRate);
        this.addRate = RateLimiter.create(inputRate);

        operations.add(new NarseseToEnglish());
        operations.add(new ToNarsese());
        operations.add(new ToProlog());

        scheduler.scheduleAtFixedRate(this::updateCycle, 0, Math.round(1f / updateRate), TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws IOException {

        var nar = NARLang0.nar();
        var lmTranslate = NARLang0.lm();
        var lmChat = NARLang0.lm();


        var l = new NARLang(
            nar.main(), new CompoundAttention(
                    new RecentAttention(),
                    new SourceAttention()
                    //new CentralityAttention(),
            ),

            lmChat, lmTranslate, 5, 0.1f, 8, 100
        );
        l.new REPL().start();
        l.shutdown();
    }

    private void updateCycle() {
        if (graph.isEmpty())
            return;
        attention.update(graph); // Allow the attention mechanism to learn/adapt
        updateNextNode();
        reason();
        think(2);
    }

    private void updateNextNode() {
        var n = graph.nodes.getRandom(ThreadLocalRandom.current());
        if (n != null) update(n);
    }

    private void reason() {
        add(new Node(narsOut(), "narsese"));
    }

    private void update(Node n) {
        var applicableOps = operations.stream()
                .filter(op -> op.applicability(n, this) > 0)
                .toList();

        if (!applicableOps.isEmpty())
            applicableOps.get(ThreadLocalRandom.current().nextInt(applicableOps.size())).apply(n, this);
    }

    public Node input(String x, String lang) {
        inputRate.acquire();
        var n = new Node(x, lang);
        accept(n);
        return n;
    }

    protected void accept(Node x) {
        add(x);
    }

    public enum ThinkOperation {
        EXPLAIN("explain", (input) -> "explain, briefly: " + input),
        COMPARE("compare", (input) -> "compare and contrast, briefly: " + input),
        WONDER("wonder", (input) -> "speculate about, briefly: " + input),
        IMAGINE("imagine", (input) -> "imagine a scenario where, briefly: " + input),
        FEELING("feeling", (input) -> "describe the emotional context, briefly: " + input),
        PREDICT_FUTURE("after", (input) -> "predict what happens next, briefly: " + input),
        PREDICT_PAST("before", (input) -> "predict what happened before, briefly: " + input),
        SUMMARIZE("summarize", (input) -> "summarize the key points, briefly: " + input);

        private final String verb;
        private final Function<String, String> promptGenerator;

        ThinkOperation(String verb, Function<String, String> promptGenerator) {
            this.verb = verb;
            this.promptGenerator = promptGenerator;
        }

        public String getVerb() {
            return verb;
        }

        public String generatePrompt(String input) {
            return promptGenerator.apply(input);
        }

        public static ThinkOperation random() {
            return values()[ThreadLocalRandom.current().nextInt(values().length)];
        }
    }
//    /** TODO move to its own Transform */
//    @Deprecated private void think(int seedNodes) {
//        var verb = switch (ThreadLocalRandom.current().nextInt(8)) {
//            case 0 -> "explain";
//            case 1 -> "compare";
//            case 2 -> "wonder";
//            case 3 -> "imagine";
//            case 4 -> "feeling";
//            case 5 -> "after"; //predict future
//            case 6 -> "before"; //predict past
//            case 7 -> "summarize";
//            default -> throw new UnsupportedOperationException();
//        };
//
//        executor.submit(() -> {
//            var selectedNodes = attention.select(graph, seedNodes);
//            if (selectedNodes.isEmpty()) return;
//
//            logger.info("{}: {}", verb, selectedNodes.stream().map(Node::logMessage).toList());
//
//            var stamps = new LinkedHashSet<String>(selectedNodes.size());
//            var prompt = selectedNodes.stream().peek(z -> stamps.addAll(z.stamp())).map(Node::content)
//                    .collect(Collectors.joining("\n\n"));
//            while (stamps.size() > stampCapacity) stamps.remove(stamps.iterator().next());
//
//            lmChat.queryAsync(prompt, verb + ", briefly.")
//                .thenAccept(s -> add(new Node(s, "en", stamps)))
//                .exceptionally(e -> {
//                    logger.error("Output generation error", e);
//                    return null;
//                });
//        });
//    }
    private void think(int seedNodes) {
        executor.submit(() -> {
            var selectedNodes = attention.select(graph, seedNodes);
            if (selectedNodes.isEmpty()) return;

            ThinkOperation operation = ThinkOperation.random();
            logger.info("{}: {}", operation.getVerb(),
                    selectedNodes.stream().map(Node::logMessage).toList());

            var stamps = new LinkedHashSet<String>(selectedNodes.size());
            var prompt = selectedNodes.stream()
                    .peek(z -> stamps.addAll(z.stamp()))
                    .map(Node::content)
                    .collect(Collectors.joining("\n\n"));
            while (stamps.size() > stampCapacity)
                stamps.remove(stamps.iterator().next());

            String generatedPrompt = operation.generatePrompt(prompt);
            lmChat.queryAsync(prompt, generatedPrompt).thenAccept(response -> {
                    logger.info("LM:{}\n{}", generatedPrompt, response);
                    add(new Node(response, "en", stamps));
                }).exceptionally(e -> {
                    logger.error("Output generation error", e);
                    return null;
                });
        });
    }

    private Node add(Node x) {
        if (x.content.isEmpty()) return null;

        addRate.acquire();
        return graph.add(x.log("add"));
    }

    public void shutdown() {
        executor.shutdown();
        scheduler.shutdown();
    }

    public int graphSize() {
        return graph.nodeCount();
    }

    private String narsOut() {
        return narOut.bag.stream().map(t -> t.get().toString()).distinct().collect(Collectors.joining("\n"));
    }

    private Stream<NALTask> parseNarsese(String narsese) {
        List<Task> tasks = new Lst<>();
        try {
            Narsese.tasks(narsese, tasks, focus.nar);
        } catch (Narsese.NarseseException e) {
            logger.error("Narsese parsing error:", e);
        }

        return tasks.stream().filter(z -> z instanceof NALTask).map(z -> (NALTask) z);
    }

    public void save(String filename) {
        try {
            graph.save(filename + ".graph");
        } catch (IOException e) {
            logger.error("Saving error:", e);
        }
    }

    public void load(String filename) {
        try {
            graph.load(filename + ".graph");
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Loading error:", e);
        }
    }

    public interface Attention {
        List<Node> select(Graph graph, int limit);

        default void update(Graph graph) { }
    }

    public interface Operation {
        void apply(Node n, NARLang l);

        double applicability(Node n, NARLang l);
    }

    /** TODO */
    private static class DecomposeOperation implements Operation {

        @Override
        public void apply(Node x, NARLang l) {
            throw new TODO("decompose 'x' content into N new children nodes");
        }

        @Override
        public double applicability(Node x, NARLang l) {
            throw new TODO("determine if 'x' has enough content to decompose");
        }
    }

    public record Node(UUID id, String content, String language, Set<String> stamp, Instant when) {
        Node(String c, String l) {
            this(c, l, Set.of(UUID.randomUUID().toString()));
        }

        Node(String c, String l, Set<String> stamps) {
            this(UUID.randomUUID(), c.trim(), l, stamps, Instant.now());
        }

        public Node log(String verb) {
            logger.info("{} {}:\"{}\"", verb, language, logMessage());
            return this;
        }

        private String logMessage() {
            var maxContentLen = 256;
            var msg = content;
            if (msg.length() > maxContentLen) msg = msg.substring(0, maxContentLen) + "..";
            return msg.replace('\n', ' ');
        }
    }

    record Edge(Node from, String type, Node to) {
    }

    public static class Graph { //TODO: use MapNodeGraph<>
        private final ConcurrentFastIteratingHashMap<UUID, Node> nodes = new ConcurrentFastIteratingHashMap<>(new Node[0]);
        private final List<Edge> edges = new CopyOnWriteArrayList<>();
        private final int capacity;

        Graph(int cap) {
            this.capacity = cap;
        }

        Node add(Node n) {
            return nodes.computeIfAbsent(n.id(), k -> n);
        }

        void link(Node from, String rel, Node to) {
            edges.add(new Edge(from, rel, to));
        }

        Stream<Node> related(Node n, String type) {
            return edges.stream().filter(e -> e.from.equals(n) && e.type().equals(type)).map(Edge::to);
        }

        void reduce(int targetSize) {
            var toRemove = nodes.size() - targetSize;
            if (toRemove > 0) {
                nodes.values().stream().sorted(Comparator.comparing(Node::when)).limit(toRemove).map(Node::id).forEach(nodes::remove);
                edges.removeIf(e -> !nodes.containsKey(e.from().id()) || !nodes.containsKey(e.to().id()));
            }
        }

        Stream<Node> stream() {
            return nodes.values().stream();
        }

        int nodeCount() {
            return nodes.size();
        }

        long charCount() {
            return nodes.values().stream().map(Node::content).mapToLong(String::length).sum();
        }

        void commit() {
            if (nodeCount() > capacity) reduce(capacity);
        }

        void clear() {
            nodes.clear();
            edges.clear();
        }

        public void save(String filename) throws IOException {
            try (var oos = new ObjectOutputStream(new FileOutputStream(filename))) {
                oos.writeObject(nodes);
                oos.writeObject(edges);
            }
        }

        public void load(String filename) throws IOException, ClassNotFoundException {
            try (var ois = new ObjectInputStream(new FileInputStream(filename))) {
                nodes.putAll((Map<UUID, Node>) ois.readObject());
                edges.addAll((List<Edge>) ois.readObject());
            }
        }

        public boolean isEmpty() {
            return nodes.isEmpty();
        }
    }

    abstract static class TranslationOperation implements Operation {
        public final String targetLanguage;

        TranslationOperation(String targetLanguage) {
            this.targetLanguage = targetLanguage;
        }

        @Override
        public double applicability(Node n, NARLang l) {
            return n.language.equals(targetLanguage) ? 0 : 1;
        }

        @Nullable
        public abstract String translate(Node n, NARLang l);

        @Override
        public void apply(Node x, NARLang l) {
            var y = translate(x, l);
            if (y != null)
                l.graph.link(x, "translate", l.add(new Node(y, targetLanguage)));
        }
    }

    public abstract static class NumericAttention implements Attention {
        protected final Map<Node, Double> scores = new ConcurrentHashMap<>();

        protected NumericAttention() {
        }

        @Override
        public void update(Graph graph) {
            var newScores = score(graph);
            scores.clear();
            scores.putAll(newScores);
        }

        @Override
        public List<Node> select(Graph graph, int limit) {
            return MutableRoulette.roulette(scores, limit, ThreadLocalRandom.current());
//            return scores.entrySet().stream()
//                    .sorted(Comparator.comparingDouble(Map.Entry::getValue))
//                    .map(Map.Entry::getKey)
//                    .limit(limit)
//                    .toList();
        }


        // Abstract method to compute scores for all nodes
        protected abstract Map<Node, Double> score(Graph graph);
    }

    public static class CentralityAttention extends NumericAttention {
        private final double dampingFactor;
        private final int iterations;
        private static final boolean maxOrMin = true;

        public CentralityAttention() {
            this(0.85, 20);
        }

        public CentralityAttention(double dampingFactor, int iterations) {
            super();
            this.dampingFactor = dampingFactor;
            this.iterations = iterations;
        }

        @Override
        protected Map<Node, Double> score(Graph graph) {
            Map<Node, Double> newScores = new HashMap<>();

            // Initialize scores
            graph.stream().forEach(node ->
                    newScores.put(node, 1.0 / graph.nodeCount()));

            // Iterate to converge
            for (var i = 0; i < iterations; i++) {
                Map<Node, Double> currentScores = new HashMap<>(newScores);
                graph.stream().forEach(node -> {
                    var score = (1 - dampingFactor) / graph.nodeCount();
                    score += graph.edges.stream()
                            .filter(e -> e.to().equals(node))
                            .mapToDouble(e -> currentScores.get(e.from()) / graph.edges.stream()
                                    .filter(outEdge -> outEdge.from().equals(e.from()))
                                    .count())
                            .sum() * dampingFactor;

                    if (!maxOrMin)
                        score = 1/(1+score);

                    newScores.put(node, score);
                });
            }

            return newScores;
        }
    }
    /**
     * Measures centrality based on the ratio of incoming to total connections.
     * Values range from 0 (only outgoing) to 1 (only incoming).
     * 0.5 indicates balanced incoming/outgoing flow.
     *    0 = Only outgoing connections (source)
     *    0.5 = Balanced incoming/outgoing (intermediary)
     *    1 = Only incoming connections (sink)
     * Good for understanding information flow patterns
     */
    public class FlowCentralityAttention extends NumericAttention {
        final double targetRatio;

        public FlowCentralityAttention(double ratio) {
            targetRatio = ratio;
        }

        @Override
        protected Map<Node, Double> score(Graph graph) {
            Map<Node, Double> newScores = new HashMap<>();

            graph.stream().forEach(node -> {
                int inDegree = (int)graph.edges.stream()
                        .filter(e -> e.to().equals(node))
                        .count();

                int outDegree = (int)graph.edges.stream()
                        .filter(e -> e.from().equals(node))
                        .count();


                int totalDegree = inDegree + outDegree;
                double v;
                if (totalDegree==0) v = 0;
                else {
                    double flowRatio = ((double)inDegree) / totalDegree;
                    v = 1 / (1 + Math.abs(targetRatio - flowRatio));
                }
                newScores.put(node, v);
            });

            return newScores;
        }
    }

    public static class SourceAttention extends NumericAttention {

        final double temperature = 1;

        @Override
        protected Map<Node, Double> score(Graph graph) {
            Map<Node, Double> newScores = new HashMap<>();

            // Count direct outgoing edges for each node
            graph.stream().forEach(node -> {
                long outDegree = graph.edges.stream()
                        .filter(e -> e.from().equals(node))
                        .count();

                newScores.put(node, (double) outDegree);
            });
            newScores.replaceAll((n,v)->temperature+v);

            // Normalize scores to 0-1 range
            double maxScore = newScores.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .max()
                    .orElse(1.0);

            if (maxScore > 1)
                newScores.replaceAll((k, v) -> v / maxScore);

            return newScores;
        }
    }

    public static class RecentAttention implements Attention {
        @Override
        public List<Node> select(Graph graph, int limit) {
            var now = System.currentTimeMillis();
            return graph.stream()
                    .sorted(Comparator.comparingLong(n -> Math.abs(n.when.toEpochMilli() - now)))
                    .limit(limit)
                    .toList();
        }
    }


    class EvolutionaryAttention implements Attention {

        // TODO Evolutionary algorithm, genome representation,
        // fitness evaluation, selection, mutation, crossover ...

        @Override
        public List<Node> select(Graph graph, int limit) {
            throw new TODO();
        }
    }

    public class ActivityAttention extends NumericAttention {
        private final Duration windowSize;

        public ActivityAttention(Duration windowSize) {
            super();
            this.windowSize = windowSize;
        }

        @Override
        protected Map<Node, Double> score(Graph graph) {
            Map<Node, Double> newScores = new HashMap<>();
            var cutoff = Instant.now().minus(windowSize);

            // Count recent edges for each node
            graph.stream().forEach(node -> {
                double activity = graph.edges.stream()
                        .filter(e -> (e.from().equals(node) || e.to().equals(node))
                                && e.from().when().isAfter(cutoff))
                        .count();
                newScores.put(node, activity);
            });

            return newScores;
        }
    }

    public static class CompoundAttention implements Attention {
        private final List<Attention> attentions;
        private final double[] weights;

        public CompoundAttention(List<Attention> attentions) {
            if (attentions.isEmpty())
                throw new UnsupportedOperationException();
            double[] w = new double[attentions.size()]; Arrays.fill(w, 1);
            this(attentions, w);
        }

        public CompoundAttention(List<Attention> attentions, double[] weights) {
            if (attentions.size() != weights.length)
                throw new IllegalArgumentException("Number of attentions must match number of weights");

            this.attentions = attentions;
            this.weights = weights;
        }

        public CompoundAttention(Attention... a) {
            this(List.of(a));
        }

        @Override
        public void update(Graph graph) {
            attentions.forEach(attention1 -> attention1.update(graph));
        }

        @Override
        public List<Node> select(Graph graph, int limit) {
            // Create a scoring map for Pareto-like selection
            Map<Node, Double> combinedScores = new HashMap<>();

            // Get selections from each attention mechanism
            IntStream.range(0, attentions.size()).forEach(i -> {
                var selectedNodes = attentions.get(i).select(graph, limit);
                // Apply weight and update scores
                for (var j = 0; j < selectedNodes.size(); j++) {
                    var node = selectedNodes.get(j);
                    var score = weights[i] * (1.0 - (double) j / selectedNodes.size());
                    combinedScores.merge(node, score, Double::sum);
                }
            });

            // Select top nodes based on combined scores
            return combinedScores.entrySet().stream()
                    .sorted(Map.Entry.<Node, Double>comparingByValue().reversed())
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .toList();
        }
    }

    class REPL {

        private final Terminal terminal;
        private final LineReader lineReader;
        private final Map<String, Consumer<String>> commands = new HashMap<>();

        REPL() throws IOException {
            terminal = TerminalBuilder.builder().system(true).build();
            lineReader = LineReaderBuilder.builder().terminal(terminal)
                    .completer(new AggregateCompleter(new StringsCompleter(commands.keySet()),
                            (reader, line, candidates) -> {
                                var word = line.word().toLowerCase();
                                Stream.of("belief", "goal", "question", "quest")
                                        .filter(t -> t.startsWith(word)).forEach(t -> candidates.add(new Candidate(t)));
                            }))
                    .option(LineReader.Option.MOUSE, true)
                    .option(LineReader.Option.HISTORY_BEEP, false)
                    .option(LineReader.Option.HISTORY_IGNORE_SPACE, false)
                    .history(new DefaultHistory()).build();


            commands.put("/exit", s -> System.exit(0));
            commands.put("/help", this::printHelp);
            commands.put("/clear", s -> graph.clear());
            commands.put("/stats", this::printStats);
            commands.put("/load", NARLang.this::load);
            commands.put("/save", NARLang.this::save);

            notice.on(x -> {
                terminal.writer().println(new AttributedStringBuilder().style(AttributedStyle.DEFAULT).append('\n').append(x).toAnsi());
                terminal.writer().flush();
            });
        }

        void start() {
            var prompt = new AttributedStringBuilder().style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)).append(">").toAnsi();

            while (true) {
                String input;
                try {
                    input = lineReader.readLine(prompt);
                } catch (UserInterruptException e) {
                    continue;
                } catch (EndOfFileException e) {
                    break;
                }

                if (input != null && !input.trim().isEmpty()) {
                    input = input.trim();
                    if (commands.containsKey(input)) commands.get(input).accept(input);
                    else input(input, "en");
                }
            }
        }

        private void printHelp(String s) {
            terminal.writer().println("Commands: " + String.join(", ", commands.keySet()));
        }

        private void printStats(String s) {
            terminal.writer().printf("Graph size: %d, Content size: %d bytes%n", graphSize(), graph.charCount());
        }
    }

    private static class NarseseToEnglish extends TranslationOperation {
        NarseseToEnglish() {
            super("en");
        }

        @Override
        public double applicability(Node n, NARLang l) {
            return n.language.equals("narsese") ? 1 : 0;
        }

        @Override
        public String translate(Node n, NARLang l) {

            var systemPrompt = """
                    Translate to English: Preserve meaning. Output fluent sentences. Expand identifiers.
                    Combine facts into statements. Interpret variables and compounds. Provide translation only.
                    Predicates: is(x, y) "X is a Y." sim(x, y) "X is similar to Y." not(x) "Not X."
                    conj(x, y) "X and Y." impl(x, y) "X implies Y." [x,y] list/vector/datapoint (x,y).""" +
                    onlyTranslate;

            return l.lmTranslate.query(n.content, systemPrompt);
        }
    }

    private static final String onlyTranslate = "\nWrite ONLY the translation, and NO introductory or additional text.\nINPUT\n----\n\n";

    private static class ToNarsese extends TranslationOperation {

        ToNarsese() {
            super("narsese");
        }

        @Override
        public String translate(Node n, NARLang l) {
            var systemPrompt = """
                     Translate to Logic:
                     Predicates: is(x, y). sim(x, y). not(x). and(x, y). or(x, y). impl(x, y).
                    """ + onlyTranslate;
            return l.lmTranslate.query(n.content, systemPrompt);
        }
    }

    private static class ToProlog extends TranslationOperation {
        ToProlog() {
            super("prolog");
        }

        @Override
        public String translate(Node n, NARLang l) {
            var systemPrompt = "Translate to Prolog: Reuse identifiers. Fundamental atoms. " + onlyTranslate;
            return l.lmTranslate.query(n.content, systemPrompt);
        }
    }

}