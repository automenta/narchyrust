package nars.lang;

import alice.tuprolog.PrologParser;
import jcog.TODO;
import jcog.data.list.Lst;
import jcog.pri.bag.util.Bagregate;
import jcog.pri.op.PriMerge;
import nars.*;
import nars.action.transform.Inperience;
import nars.time.clock.RealTime;
import org.jetbrains.annotations.Nullable;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nars.Op.*;

/**
 * NARchy (Natural) Language Model interface
 * --English to NARchy
 * --NARchy to English
 * --NARchy output prioritization, buffering/chunking
 * --Includes basic REPL for console usage. [inner class]
 * --Includes basic "OpenAI" HTTP/JSON server for
 * LM client application usage. [inner class]
 * <p>
 * Adds long-term learning, reasoning, & memory to
 * Language Model applications.
 * <p>
 * a. User inputs natural language statements or queries.
 * b. NARLang translates the input into NARchy syntax.
 * c. NARS processes the translated input, performing reasoning and inference.
 * d. Results from NARS are translated back to natural language using NARLang.
 * e. The LM provides additional context, explanations, or suggestions based on the NARS output.
 * f. The system presents the final output to the user in a readable format.
 * <p>
 * See: https://github.com/opennars/NARS-GPT
 * See: https://github.com/opennars/NARS-GPT/blob/master/Prompts.py
 */
public class NARLang0 {

    final Bagregate<NALTask> narOut = new Bagregate<>(16, PriMerge.max);

    private final LM lmChat, lmTranslate;
    private final NAR nar;
    private final Focus focus;

    public NARLang0(Focus f, LM lm) {
        this(f, lm, lm);
    }

    public NARLang0(Focus f, LM lmChat, LM lmTranslate) {
        this.lmChat = lmChat; this.lmTranslate = lmTranslate;
        this.focus = f;
        this.nar = f.nar;

        f.onTask(t -> narOut.put((NALTask) t));
    }

    public static void main(String[] args) {
        var nar = nar();


        var l = new NARLang0(nar.main(), lm());

//        System.out.println(nl.prologParse(nl.nlToPrologStr("""
//        ...
//        """)));

        l.repl();
        l.nar.stop();
        System.exit(0);
    }

    public static LM lm() {
        /* ollama */
        return new LM("http://localhost:11434/api/generate",
            "llamablit"
            //"llama3.2:1b"
            //"llama3.2:3b"
            //"yi-coder:9b"
            //"mistral-small:22b-instruct-2409-q2_K"
            //"llama3.2:3b-instruct-fp16"
        );
    }

    static NAR nar() {
        var durFPS = 1;
        var nar = new NARS.DefaultNAR(8, true)
//            .exe(new WorkerExec(4, (n) ->
//                Derivers.nal(1, 8).core().stm().images()
//                    .temporalInduction().compile(n)))
            .time(new RealTime.MS().durFPS(durFPS)).get();
        nar.timeRes.set(100);
        nar.complexMax.set(96);
        return nar;
    }

    private void repl() {
        try {
            Terminal terminal = terminal();
            if (terminal==null)
                return;

            DefaultHistory history = new DefaultHistory();

            LineReader lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(new AggregateCompleter(
                            new StringsCompleter("/exit", "/help", "/clear"),
                            new NARLangCompleter()
                    ))
                    .option(LineReader.Option.ERASE_LINE_ON_FINISH, true)
                    .option(LineReader.Option.AUTO_FRESH_LINE, true)
                    .option(LineReader.Option.HISTORY_BEEP, false)
                    .option(LineReader.Option.HISTORY_IGNORE_SPACE, false)
                    .history(history)
                    .highlighter(new NARLangHighlighter())
                    .build();

//            Map<String, String> aliases = new HashMap<>();
//            aliases.put("q", "exit");
//            aliases.put("?", "help");

            AttributedStringBuilder asb = new AttributedStringBuilder();
            String coloredPrompt = asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                    .append(">")
                    .toAnsi();

            while (true) {
                String input = null;
                try {
                    input = lineReader.readLine(coloredPrompt);
                } catch (UserInterruptException e) {
                    // Ctrl-C
                    continue;
                } catch (EndOfFileException e) {
                    // Ctrl-D
                    break;
                }

                if (input == null)
                    break;

                input = input.trim();
                if (input.isEmpty())
                    continue;

                if (input.equalsIgnoreCase("/clear")) {
                    chat = "";
                } else if (input.equalsIgnoreCase("/exit")) {
                    break;
                } else if (input.equalsIgnoreCase("/help")) {
                    terminal.writer().println(asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                            .append("Available commands: /exit, /help, /multi")
                            .toAnsi());
                    continue;
                } else if (input.equalsIgnoreCase("/multi")) {
                    StringBuilder multiLineInput = new StringBuilder();
                    String line;
                    while (!(line = lineReader.readLine("... ", null, (MaskingCallback) null, null)).equals("")) {
                        multiLineInput.append(line).append("\n");
                    }
                    input = multiLineInput.toString();
                }

                String I = input;
                showSpinner(terminal, () -> {
                    String output = this.input(I);
                    terminal.writer().println(
                        asb.style(AttributedStyle.DEFAULT/*.foreground(AttributedStyle.WHITE)*/)
                            .append('\n')
                            .append(output)
                            .toAnsi()
                    );
                });
            }

            history.save();
        } catch (IOException e) {
            System.err.println(new AttributedStringBuilder()
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                    .append("JLine Error: ")
                    .toAnsi() + e.getMessage());
        }
    }

    @Nullable private static Terminal terminal() {
        try {
            return TerminalBuilder.builder().system(true).build();
        } catch (IOException e) {
            //System.out.println("WARNING: Unable to create a system terminal, falling back to a dumb terminal");
            try {
                return new DumbTerminal(System.in, System.out);
            } catch (IOException ex) {
                System.err.println("Error creating dumb terminal: " + ex.getMessage());
                return null;
            }
        }
    }

    private static class NARLangCompleter implements Completer {
        private final List<String> narTerms = Arrays.asList("belief", "goal", "question", "quest");

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            String word = line.word().toLowerCase();
            narTerms.stream()
                    .filter(term -> term.startsWith(word))
                    .forEach(term -> candidates.add(new Candidate(term)));
        }
    }

    private static class NARLangHighlighter extends DefaultHighlighter {
        @Override
        public AttributedString highlight(LineReader reader, String buffer) {
            AttributedStringBuilder asb = new AttributedStringBuilder();

            String[] tokens = buffer.split("\\s+");
            for (String token : tokens) {
                if (isKeyword(token)) {
                    asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN)).append(token).append(" ");
                } else {
                    asb.style(AttributedStyle.DEFAULT).append(token).append(" ");
                }
            }

            return asb.toAttributedString();
        }

        private boolean isKeyword(String token) {
            return Arrays.asList("belief", "goal", "question", "quest").contains(token.toLowerCase());
        }
    }

    private static void showSpinner(Terminal terminal, Runnable task) {
        Thread spinnerThread = new Thread(() -> {
            String[] spinner = {"|", "/", "-", "\\"};
            int i = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    terminal.writer().print("\r" + spinner[i % spinner.length] + " ...");
                    terminal.flush();
                    Thread.sleep(100);
                    i++;
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        spinnerThread.start();
        try {
            task.run();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            spinnerThread.interrupt();
            try {
                spinnerThread.join();
            } catch (InterruptedException e) {
                // Ignore
            } finally {
                // Erase the spinner line
                String eraseLine = "\r" + " ".repeat(10);
                terminal.writer().print(eraseLine);
                terminal.writer().print("\r\n");
                terminal.flush();
            }
        }


    }

//    private static void repl(NARLang nl) {
//        var scanner = new java.util.Scanner(System.in);
//        while (true) {
//            System.out.print("> ");
//            var input = scanner.nextLine();
//            if (input.equalsIgnoreCase("exit"))
//                break;
//
//            nl.input(input);
//        }
//    }

    private String chat = "";

    public String input(String input) {
        //contextRepo.addUserInput(input);
        chat += input + "\n";

        var response = lmChat.query(chat);

        chat += response + "\n";

        String responseNars = translateToNarsese(response);
//        if (responseNars.isEmpty()) {
//            return "Translation failed";
//        }

        Stream.of(responseNars.split("\n\n")).forEach(responseSection-> {
            var tasks = parseNarsese(responseSection);
            if (tasks.isEmpty()) {
                //System.err.println("No valid tasks generated from the input");
            } else
                processTasks(tasks);
        });

        String narsOutput = narsOut();
        //contextRepo.addNarsInference(narsOutput);

        String narsNL = translateToEnglish(narsOutput);
        //String explanation = generateExplanation(narsOutput);

        if (narsNL.isEmpty()) {
            //System.out.println("Failed to translate Narsese output.");
        } else {
            //contextRepo.addExplanation(explanation);
            chat += narsNL + "\n";
        }

        String o =
//            input + "\n" +
            response + "\n" +
            narsNL;
        //+ "\n" + explanation;
        return o;
    }

    private void processTasks(List<NALTask> tasks) {
        for (var task : tasks) {
            try {
                //System.out.println("\t" + task);
                focus.accept(task);
            } catch (RuntimeException e) {
                //..
            }
        }
    }

    private String output(String x) {
        var y = translateToEnglish(x);
        //System.out.println(x + "\n" + y);
        return y;
    }

    private final Inperience.BeliefInperience reifyBelief = new Inperience.BeliefInperience(BELIEF, 0);
    private final Inperience.BeliefInperience reifyGoal = new Inperience.BeliefInperience(GOAL, 0);
    private final Inperience.QuestionInperience reifyQuestion = new Inperience.QuestionInperience(QUESTION);
    private final Inperience.QuestionInperience reifyQuest = new Inperience.QuestionInperience(QUEST);

    @Deprecated private static final Term I = $.atomic("I");

    private String narsOut() {
        return narOut.bag.stream()
            .map(_z -> {
                var z = _z.get();
                switch (z.punc()) {
                    case BELIEF -> {return reifyBelief.reify(z, I);}
                    case GOAL -> {return reifyGoal.reify(z, I);}
                    case QUESTION -> {return reifyQuestion.reify(z, I);}
                    case QUEST -> {return reifyQuest.reify(z, I);}
                }
                return null;
            })
            .filter(Objects::nonNull)
            .distinct()
            .map(Object::toString) //TODO better task -> string
            .collect(Collectors.joining("\n"));
    }

    private List<alice.tuprolog.Term> prologParse(String input) {
        List<alice.tuprolog.Term> theory = new Lst<>();


        //Stream.of(input.split("\\.\n")).forEach(line -> { //parse lines separately in case of syntax errors
            try {
                new PrologParser(input).iterator()
                    .forEachRemaining(theory::add);
            } catch (Throwable t) {
                //TODO handle
                System.err.println("Prolog: " + input + "\n\t" + t);
            }
        //});
        return theory;
    }

    private String nlToPrologStr(String input) {
        String sys = """
        Translate into Prolog code.
        Only write Prolog code. Write no comments or other feedback.
        Continue using previously-written identifiers.
        """;
        //Use the most fundamental atoms to clearly describe involved concepts.

        return lmTranslate.query(input, sys);
    }

    /** use the Prolog parser */
    private List<NALTask> prologToNars(List<alice.tuprolog.Term> theory) {
        throw new TODO();
    }

    private String narsToProlog(String input) {
        throw new TODO();
    }

    private String translateToNarsese(String input) {
        String sys =
        """               
        Translate English into Logic facts!
         * Use short, reusable, & meaningful identifiers.
         * Clearly describe concepts in their elementary components.
         * Use these predicates:
             is(x, y).   // "X is a Y."
             sim(x, y).  // "X is similar to Y."
             not(x).     // "Not X."
             and(x, y).  // "X and Y."
             or(x, y).   // "X or Y."
             impl(x, y). // "X implies Y."
         * Preserve the original meaning of the input.
         * Output a flat list of Logic facts ONLY!  No comments, no questions.
         * Use no special symbols, write the complete predicates.
        """;
        var r = lmTranslate.query(input, sys);
        return post(r);
    }

    private String translateToEnglish(String narsese) {
        String sys =
        """                
        Translate Logic into English!
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
        """;
        var r = lmTranslate.query(narsese, sys);
        return r;
    }

    private String post(String r) {
//        // Remove any language specifier after the opening code block
//        r = r.replaceAll("```\\w*", "```");
//
//        // Extract content between code blocks if present
//        if (r.contains("```")) {
//            int s = r.indexOf("```");
//            int e = r.indexOf("```", s + 3);
//            if (e != -1) {
//                r = r.substring(s + 3, e).trim();
//            }
//        }
//
////        // Remove common prefixes that LMs might add
////        String[] prefixesToRemove = {
////                "Here is the", "Here's the", "The translation is:",
////                "Translated code:", "Output:", "Result:"
////        };
////        for (String prefix : prefixesToRemove) {
////            if (r.startsWith(prefix)) {
////                r = r.substring(prefix.length()).trim();
////                break;
////            }
////        }
//
//        // Remove any remaining leading or trailing punctuation and whitespace
//        r = r.replaceAll("^[\\s\\p{Punct}]+|[\\s\\p{Punct}]+$", "");

        return r;
    }

    private String processNARSOutput(String translatedOutput) {
        String sys =
        """
        Analyze and summarize the AI reasoning output, providing:
        1. A brief summary of the main points
        2. Any new conclusions or inferences
        3. Identification of any conflicts or uncertainties
        4. Suggestions for further inquiry or clarification
        """;
        return lmTranslate.query(translatedOutput, sys);
    }

    private List<NALTask> parseNarsese(String narsese) {
        List<Task> tasks = new Lst<>();
        List<Narsese.NarseseException> errors = new Lst<>();

        try {
            Narsese.tasks(narsese, tasks, nar);
        } catch (Narsese.NarseseException e) {
            errors.add(e);
        }

        return tasks.stream()
                .filter(z -> z instanceof NALTask) //filter AbstractCommandTasks for now
                .map(z -> (NALTask)z).toList();
    }

}
//    public void input0(String input) {
//        chat = chat + input + "\n";
//
//        var response = lmChat.query(chat);
//
//        chat = chat + response + "\n";
//
//        var narsese = translateToNarsese(response);
//
//        //TODO optional refinement step
//
//        var tasks = parseNarsese(narsese);
//        if (tasks.isEmpty()) {
//            System.out.println("untranslated: " + narsese);
//        } else {
//            processTasks(tasks);
//        }
//
//        chat = chat + output(narsOut()) + "\n";
//    }
