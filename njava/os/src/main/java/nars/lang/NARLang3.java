package nars.lang;

import com.google.common.util.concurrent.RateLimiter;
import jcog.Log;
import jcog.TODO;
import jcog.Util;
import jcog.data.list.Lst;
import jcog.data.map.ConcurrentFastIteratingHashMap;
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

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NARLang3 {

    public static final int narOutCapacity = 16;

    private final jcog.event.Topic<String> notice = new ListTopic<>();

    private int stampCapacity = 8;

    record Node(UUID id, String content, String language, Set<String> stamp, Instant when) {
        Node(String c, String l) {
            this(c, l, Set.of(UUID.randomUUID().toString()));
        }
        Node(String c, String l, Set<String> stamps) {
            this(UUID.randomUUID(), c.trim(), l, stamps, Instant.now());
        }
        public final Node log(String verb) {
            logger.info("{} {}:\"{}\"", verb, language, logMsg());
            return this;
        }

        private String logMsg() {
            var maxContentLen = 256;
            var msg = content;
            if (msg.length() > maxContentLen)
                msg = msg.substring(0, maxContentLen) + "..";
            msg = msg.replace('\n', ' '); //erase newlines
            return msg;
        }
    }

    record Edge(Node from, String type, Node to) {}

    /** TODO use MapNodeGraph<> */
    private static class Graph {
        private final ConcurrentFastIteratingHashMap<UUID, Node> nodes = new ConcurrentFastIteratingHashMap<>(new Node[0]);
        private final List<Edge> edges = new CopyOnWriteArrayList<>();
        private final int capacity;

        Graph(int cap) { this.capacity = cap; }

        Node add(Node n) { return nodes.computeIfAbsent(n.id(), k -> n); }

        void link(Node f, String rel, Node t) { edges.add(new Edge(f, rel, t)); }

        Stream<Node> related(Node n, String y) {
            return edges.stream()
                .filter(e -> e.from().equals(n))
                .filter(e -> e.type().equals(y))
                .map(Edge::to);
        }

        void reduce(int targetSize) {
            var toRemove = nodeCount() - targetSize;
            if (toRemove > 0) {
                nodes.values().stream()
                    .sorted(Comparator.comparing(Node::when))
                    .limit(toRemove)
                    .map(Node::id)
                    .forEach(nodes::remove);
                edges.removeIf(e -> !nodes.containsKey(e.from().id()) || !nodes.containsKey(e.to().id()));
            }
        }
        Stream<Node> stream() { return nodes.values().stream(); }
        int nodeCount() { return nodes.size(); }
        long charCount() { return nodes.values().stream().map(Node::content).mapToLong(String::length).sum(); }
        void commit() { if (nodeCount() > capacity) reduce(capacity); }
        void clear() { nodes.clear(); edges.clear(); }
    }

    interface Attention { List<Node> select(Stream<Node> nodes, int limit); }

    interface Operation {
        void apply(Node n, NARLang3 g);

        default double applies(Node n, NARLang3 g) {
            return 1;
        }
    }

    abstract static class TranslationOperation implements Operation {

        /** target language */
        public final String target;

        TranslationOperation(String targetLanguage) {
            this.target = targetLanguage;
        }

        @Override
        public double applies(Node n, NARLang3 g) {
            return n.language.equals(target) ? 0 : 1;
        }

        @Nullable public abstract String translate(Node n, NARLang3 g);

        @Override
        public void apply(Node x, NARLang3 g) {
            var y = translate(x, g);
            if (y==null) return;

            var Y = g.add(new Node(y, target));
            g.graph.link(x, "translate", Y);
        }
    }

    private final Focus focus;
    private final Graph graph;
    private final ExecutorService exe;
    private final ScheduledExecutorService scheduler;
    private final Attention attention;

    private final LM lmChat, lmTranslate;
    private final RateLimiter inputRate, addRate;
    private final Bagregate<NALTask> narOut = new Bagregate<>(narOutCapacity, PriMerge.max);

    private final List<Operation> ops = new Lst<>();
    {
        ops.add(new NarseseToEnglish());
        ops.add(new ToNarsese());
        ops.add(new ToProlog());
        /*
        Summarize Operation: Implement a summarize operation that condenses the content of nodes or a selection of nodes into a concise summary.
        Conceptualize: Develop logic to form new concepts based on existing nodes, possibly by generalizing or abstracting shared features.
        Associate: Create an operation that finds and strengthens connections between related nodes.
        Abstract: Implement functionality to extract abstract representations from specific instances.
        Analogize: Add logic to find analogies between different concepts or situations represented in the graph.
        Hypothesize: Enable the system to generate hypotheses based on existing knowledge, possibly by inferring new nodes.
        Validate: Introduce a mechanism to verify the correctness or relevance of nodes or edges.
        Reflect: Implement self-reflection capabilities, allowing the system to analyze its reasoning processes.
        Prioritize: Add logic to prioritize certain nodes or tasks based on criteria like recency, frequency, or relevance.
        Forget: Implement a forgetting mechanism to remove less useful or outdated information from the graph.
         */
//        ops.put("summarize", (n, g) -> { });
//        ops.put("conceptualize", (n, g) -> {/* Conceptualization  */});
//        ops.put("associate", (n, g) -> {/* Association  */});
//        ops.put("abstract", (n, g) -> {/* Abstraction  */});
//        ops.put("analogize", (n, g) -> {/* Analogizing  */});
//        ops.put("hypothesize", (n, g) -> {/* Hypothesis generation  */});
//        ops.put("validate", (n, g) -> {/* Validation  */});
//        ops.put("reflect", (n, g) -> {/* Reflection  */});
//        ops.put("prioritize", (n, g) -> {/* Prioritization  */});
//        ops.put("forget", (n, g) -> {/* Forgetting  */});
    }
    
    public NARLang3(Focus f, Attention attention, LM lmChat, LM lmTranslate,
                    int inputRate, float updateRate, int graphCapacity, long maxMemoryMB) {
        this.focus = f;
        this.attention = attention;
        this.lmChat = lmChat;
        this.lmTranslate = lmTranslate;
        this.graph = new Graph(graphCapacity);
        this.exe = Executors.newWorkStealingPool();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.inputRate = RateLimiter.create(inputRate);
        this.addRate = RateLimiter.create(inputRate);
        scheduler.scheduleAtFixedRate(()->update(),
                0, Math.round(1f/updateRate), TimeUnit.SECONDS);

    }

    @Deprecated private void update() {
        var n = graph.nodes.getRandom(ThreadLocalRandom.current());
        if (n==null) return;

        update(n);
        reason();
        speak();
    }

    private void reason() {
        add(new Node(narsOut(), "narsese"));
    }

    private void speak() {
        generateOutputs(2);
    }
    private void update(Node n) {
        var oo = new Lst<Operation>();
        for (var o : ops) {
            var a = o.applies(n, this);
            if (a > 0)
                oo.add(o);
        }
        if (oo.isEmpty()) return; //nothing

        oo.get(ThreadLocalRandom.current()).apply(n, this);
    }


    public Node input(String x, String lang) {
        inputRate.acquire();
        var n = new Node(x, lang);
        accept(n);
        return n;
    }

    protected void accept(Node x) {
        add(x);

//        return translator.translate(input, "narsese")
//            .thenCompose(narsese -> completedFuture(parseNarsese(narsese.content()).toList()))
//            .thenCompose(tasks -> {
//                focus.acceptAll(tasks);
//                return completedFuture(tasks);
//            })
//            .thenCompose(tasks -> {
//
//                    var o = narsOut();
//                    return completedFuture(o);
//            })
//            .thenCompose(p -> p==null ? null : translator.translate(new Node(p, "narsese"), "en"))
//            .thenCompose(output -> allOf(
//                runAsync(() -> {
//                    if (output!=null) {
//                        add(output);
//                        graph.link(input, "process", output);
//                    }
//                })
//            )).thenRun(graph::commit)
//            .exceptionally(e -> {
//                logger.error("Error in accept", e);
//                return null;
//            });
    }

    private void generateOutputs(int seedNodes) {
        String verb = switch (ThreadLocalRandom.current().nextInt(8)) {
            case 0 -> "explain";
            case 1 -> "compare";
            case 2 -> "wonder";
            case 3 -> "imagine";
            case 4 -> "feeling";
            case 5 -> "predict future";
            case 6 -> "predict past";
            case 7 -> "evaluate";
            default -> throw new UnsupportedOperationException();
        };
        var sysPrompt = verb + ", briefly.";
        exe.submit(() -> {
            var selectedNodes = attention.select(graph.stream(), seedNodes);
            if (selectedNodes.isEmpty())
                return;

            logger.info("{}: {}", verb, selectedNodes.stream().map(Node::logMsg).toList());

            var stamps = new LinkedHashSet<String>();
            var prompt = selectedNodes.stream()
                .peek(z -> stamps.addAll(z.stamp))
                .map(Node::content)
                .collect(Collectors.joining("\n\n"));
            while (stamps.size() > stampCapacity)
                stamps.removeFirst();

            lmChat.queryAsync(prompt, sysPrompt)
                .thenAccept(s -> add(new Node(s, "en", stamps)))
                .exceptionally(e -> {
                    logger.error("Error generating output", e);
                    return null;
                });
        });
    }

    private Node add(Node x) {
        if (x.content.isEmpty())
            return null;
        addRate.acquire();
        return graph.add(x.log("add"));
    }

    public void shutdown() {
        exe.shutdown();
        scheduler.shutdown();
    }

    public int graphSize() {
        return graph.nodeCount();
    }

    private static final Logger logger = Log.log(NARLang3.class);

    class REPL {
        private final Terminal terminal;
        private final LineReader lineReader;
        private final Map<String, Consumer<String>> commands = new HashMap<>();

        REPL() throws IOException {
            this.terminal = TerminalBuilder.builder().system(true).build();
            this.lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new AggregateCompleter(
                    new StringsCompleter(commands.keySet()), (reader, line, candidates) -> {
                        var word = line.word().toLowerCase();
                        Stream.of("belief", "goal", "question", "quest")
                            .filter(t -> t.startsWith(word))
                            .forEach(t -> candidates.add(new Candidate(t)));
                    }))
                //.option(LineReader.Option.ERASE_LINE_ON_FINISH, true)
                //.option(LineReader.Option.AUTO_FRESH_LINE, true)
                .option(LineReader.Option.HISTORY_BEEP, false)
                .option(LineReader.Option.HISTORY_IGNORE_SPACE, false)
                .history(new DefaultHistory())
                .build();

            commands.put("/exit", s -> System.exit(0));
            commands.put("/help", this::printHelp);
            commands.put("/clear", s -> graph.clear());
            commands.put("/stats", this::printStats);
            commands.put("/load", NARLang3.this::load);
            commands.put("/save", NARLang3.this::save);

            notice.on(x -> {
                terminal.writer().println(
                    new AttributedStringBuilder().style(AttributedStyle.DEFAULT)
                        .append('\n').append(x)
                        .toAnsi()
                );
                terminal.writer().flush();
            });
        }

        void start() {
            var asb = new AttributedStringBuilder();
            var prompt = asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append(">")
                .toAnsi();

            while (true) {
                String input;
                try {
                    input = lineReader.readLine(prompt);
                } catch (UserInterruptException e) {
                    continue;
                } catch (EndOfFileException e) {
                    break;
                }

                if (input == null || input.trim().isEmpty()) continue;

                input = input.trim();
                if (commands.containsKey(input)) {
                    commands.get(input).accept(input);
                } else {
                    if (!input.isEmpty())
                        input(input, "en");
                }
            }

        }

        private void printHelp(String s) {
            terminal.writer().println("Available commands: " + String.join(", ", commands.keySet()));
        }

        private void printStats(String s) {
            terminal.writer().printf("Graph size: %d, Total content size: %d bytes%n",
                                     graphSize(), graph.charCount());
        }


    }

    public void save(String filename) {
        synchronized(NARLang3.this) {
            throw new TODO();
        }
    }

    public void load(String filename) {
        synchronized(NARLang3.this) {
            throw new TODO();
        }
    }

//    protected CompletableFuture<String> processAsync(String input) {
//        var inputNode = add(new Node(input, "en"));
//        return supplyAsync(() -> lmChat.queryAsync(input /* + context? */)
//            .thenCompose(answer -> {
//                var answerNode = add(new Node(answer, "en"));
//                graph.link(inputNode, "answer", answerNode);
//                //appendChat(answer + "\n");
//                return translateToNarsese(answer);
//            }).thenCompose(responseNars ->
//                allOf(
//                    Stream.of(responseNars.split("\n\n"))
//                        .flatMap(section -> parseNarsese(section))
//                        .map(this::processTask)
//                        .toArray(CompletableFuture[]::new)
//                ).thenApply(v -> responseNars)
//            )
//            .thenCompose(v -> {
//                var narsNL = translateToEnglish(narsOut());
//                //appendChat(narsNL + "\n");
//                return narsNL;
//            }), exe)
//            .thenCompose(Function.identity()) //??
//        ;
//    }

    private static void think() {
        var thinkingTimeMS = 500;
        Util.sleepMS(thinkingTimeMS);
    }

    private String narsOut() {
        think();
        return narOut.bag.stream()
            .map(t -> t.get().toString())
            .distinct()
            .collect(Collectors.joining("\n"));
    }

    private Stream<NALTask> parseNarsese(String narsese) {
        List<Task> tasks = new Lst<>();
        List<Narsese.NarseseException> errors = new Lst<>();

        try {
            Narsese.tasks(narsese, tasks, focus.nar);
        } catch (Narsese.NarseseException e) {
            errors.add(e);
        }

        return tasks.stream()
                .filter(z -> z instanceof NALTask) //ignore AbstractCommandTasks for now
                .map(z -> (NALTask)z);
    }


    public static void main(String[] args) throws IOException {
        var nar = NARLang0.nar();

        var lmTranslate = NARLang0.lm();
        var lmChat = lmTranslate;

        var l = new NARLang3(
            nar.main(),
            (nodes, limit) -> nodes.sorted(Comparator.comparing(Node::when).reversed()).limit(limit).toList(),
            lmChat, lmTranslate, 5, 0.1f,
                8, 100
        );

        l.new REPL().start();
        l.shutdown();
    }

    private static class ToNarsese extends TranslationOperation {

        ToNarsese() {
            super("narsese");
        }

        @Override
        public String translate(Node n, NARLang3 g) {
            var sys =
            """               
            Translate into Logic facts!
             * Use short, reusable, & meaningful identifiers
             * Clearly describe concepts in their elementary components
             * Use predicates
                 is(x, y).   // "X is a Y."
                 sim(x, y).  // "X is similar to Y."
                 not(x).     // "Not X."
                 and(x, y).  // "X and Y."
                 or(x, y).   // "X or Y."
                 impl(x, y). // "X implies Y."
             * Preserve the original meaning of the input
             * Use no special symbols, write the complete predicates
             * Write ONLY the code and nothing else
            """;
            return g.lmTranslate.query(n.content, sys);
        }
    }
    private static class ToProlog extends TranslationOperation {

        ToProlog() {
            super("prolog");
        }

        @Override
        public String translate(Node n, NARLang3 g) {
            var sys = """
            Translate into Prolog code.
            Write ONLY the code and nothing else.
            """;
            //Continue using previously-written identifiers.
            //Use the most fundamental atoms to clearly describe involved concepts.
            return g.lmTranslate.query(n.content, sys);
        }
    }
    private static class NarseseToEnglish extends TranslationOperation {

        NarseseToEnglish() {
            super("en");
        }

        @Override
        public double applies(Node n, NARLang3 g) {
            return n.language.equals("narsese") ? 1 : 0;
        }

        @Override
        public String translate(Node n, NARLang3 g) {
            var sys =
            """                
            Translate into English!
             * Preserve the original meaning of the code.
             * Output natural, fluent English sentences.
             * Expand short identifiers into descriptive phrases.
             * Combine related facts into coherent statements.
             * Interpret variables and compounds appropriately.
             * Provide the translation only; NO code or comments.
             * Notice these predicates and their meanings:
              - is(x, y) // "X is a Y."
              - sim(x, y) // "X is similar to Y."
              - not(x) // "Not X."
              - conj(x, y) // "X and Y."
              - impl(x, y) // "X implies Y."
              - [x,y] // Interpret as a list, vector, or datapoint (x,y)
            Write ONLY the code and nothing else.
            """;
            return g.lmTranslate.query(n.content, sys);
        }

    }

}