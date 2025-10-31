package nars.lang;

import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;
import jdk.jshell.VarSnippet;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class JShellLM {
    private final JShell jshell;
    private final Deque<String> commandHistory;
    private static final int MAX_HISTORY = 5;

    private final LM lm;

    public JShellLM(String api,String model) {
        lm = new LM(api, model);
        this.jshell = JShell.create();
        this.commandHistory = new LinkedList<>();
    }

    public void run() {
        var scanner = new Scanner(System.in);
        while (true) {
            try {
                System.out.print("jshell> ");
                var input = scanner.nextLine();
                if (input.equals("/exit")) break;

                var events = jshell.eval(input);
                var output = processEvents(events);
                System.out.println(output);

                updateCommandHistory(input);
                var feedback = lm.query(prompt(input, output));
                System.out.println(/*"LM Feedback: " + */feedback);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        jshell.close();
    }

    private String processEvents(List<SnippetEvent> events) {
        return events.stream()
            .map(this::eventToString)
            .collect(Collectors.joining("\n"));
    }

    private String eventToString(SnippetEvent event) {
        if (event.exception() != null)
            return "Exception: " + event.exception().getMessage();

        var value = event.value();
        return value != null ? value.toString() : "(empty)";
    }

    private void updateCommandHistory(String command) {
        commandHistory.addFirst(command);
        if (commandHistory.size() > MAX_HISTORY)
            commandHistory.removeLast();
    }

    private String prompt(String input, String output) {
        var variables = getDetailedVariableInfo();
        var history = String.join("\n", commandHistory);
        return """
               Analyze this Java JShell context & code evaluation:

               Input History:
               %s

               Input: %s
               Output: %s

               Variables:
               %s

               Answer any of these questions if it would be informative to the user:
               1. Explanation of the latest command and its output
               2. Potential improvements or best practices
               3. Suggestions for next steps or related concepts to explore
               4. The cause of errors explained, and how to fix

               Respond concise, focusing on important points.  Don't state the obvious, or uninformative (ex: 'there is no error').
               """.formatted(history, input, output, variables);
    }

    private String getDetailedVariableInfo() {
        return jshell.variables()
                .map(this::formatVariableInfo)
                .collect(Collectors.joining("\n"));
    }

    private String formatVariableInfo(VarSnippet var) {
        var value = jshell.eval(var.name())
                .stream().findFirst()
                .map(SnippetEvent::value)
                .map(Object::toString)
                .orElse("N/A");
        return "%s %s = %s".formatted(var.typeName(), var.name(), value);
    }

    public static void main(String[] args) {
        new JShellLM(
                "http://localhost:11434/api/generate",
            "llamablit"
        ).run();
    }

}

/*
This is a natural language to logic syntax translation task.  Follow these example pairs:

is(bird, fly)  # "Birds fly."
sim(cat, tiger)  # "Cats are similar to tigers."
conj(apple, red)  # "Apples are red."
impl(lighting, fire)  # "Lighting implies fire."
is(x, y)?  # "What is the relationship between x and y?"
is(agent, achieve_goal)!  # "The agent's goal is to achieve the goal."
not(is(bird, fly))  # "Birds do not fly."
conj(not(is(bird, fly)), is(bird, swim))  # "Birds neither fly nor swim."
comp(bird, has, wing)  # "Birds have wings."
comp(bird, chase, mouse)  # "Birds chase mice."
conj(is(bird, fly), comp(bird, has, wing))  # "Birds fly and have wings."
*/
/*
This is a natural language to logic syntax translation task.  Follow these example pairs:

```
is(bird, animal).  # "Birds are animals."
not(cat, bird).  # "Cats are not birds."
sim(cat, tiger).  # "Cats are similar to tigers."
conj(raining, cloudy).  # "Raining and cloudy.
conj(raining, not(snowing)).  # "Raining and not snowing.
impl(cause, effect).  # "Cause implies effect."
impl(not(cause), effect).  # "Not cause implies effect."
impl(conj(a, b), effect).  # "A and B implies effect."
impl(conj(a, not(b)), effect).  # "A and not B implies effect."
impl(cause, conj(a, b)).  # "cause implies A and B."

is(x, y)?  # "What is the relationship between X and Y?"
is(?something, y)?  #  "What is a Y?"
impl(?x, effect)?  #  "What implies effect?"
impl(?x, effect)?  #  "What implies effect?"
```

Instructions:
 * Follow the logic syntax as described.
 * Output as many sentences as necessary, referencing common subterms, using common patterns, to connect their concepts.
 * Preserve the original meaning.
 * Provide only the translation - no additional comments.

Translate the following:
```
NARS is a logic reasoning system.  We use NARS to solve problems.  If not NARS, then what should we use?
```

```
"Entities are attributes."
(entity -> attribute).

"Concepts maybe challenging."
(concept -> challenging). %0.5%
"It is X."
X.
"It is not X."
--X.

"if X then Y."
(x => y).

"if not X then Y."
(--x => y).

"if X then not Y."
(x => --y).

"X?"
X?
```

Additional phrase patterns, useful in recursive subterms:
```
"X and Y"
(X & Y)

"X and not Y"
(X & --Y)

"X or Y"
(X | Y)
```
Instructions:
 * Output as many sentences as necessary, referencing common subterms to connect their concepts.
 * Follow the logic syntax as described.
 * Preserve the original meaning.
 * Provide only the translation - no additional comments.

Now, translate the following:
```
NARS is a logic reasoning system.  We use NARS to solve problems.  If not NARS, then what should we use?
```
*/
/*
 * To refine the intermediate representation language and syntax for maximum effectiveness and comprehension by a language model, we should consider the following factors:
 *
 * Simplicity and Consistency: The syntax should be as simple and consistent as possible, with a minimal number of unique constructs. This makes it easier for the language model to learn and apply the rules.
 * Familiarity: Where possible, use constructs and symbols that are familiar to language models, such as Prolog-like predicates and logical operators. This helps the model leverage its existing knowledge.
 * Unambiguity: Ensure that the syntax is unambiguous, with clear mappings between the natural language and the intermediate representation. Ambiguity can lead to translation errors.
 * Expressiveness: The intermediate representation should be able to capture the full range of Narsese constructs, including beliefs, goals, questions, and various logical and semantic relations. This ensures comprehensive coverage.
 * Modularity: Break down the representation into smaller, modular components that can be learned and applied independently. This can improve the model's ability to generalize and handle more complex inputs.
 * Contextual Information: Consider incorporating contextual information, such as speaker, time, and location, into the representation. This can help the model better understand the semantics of the input.
 * Examples and Documentation: Provide a comprehensive set of examples and clear documentation to help the language model understand the syntax and semantics of the intermediate representation.
 * Iterative Refinement: Continuously evaluate the performance of the language model on translation tasks and refine the intermediate representation based on the results. This can lead to incremental improvements in translation quality.
 *
 * Some key factors that will impact the translation quality include:
 *
 * Language Model Capabilities: The specific architecture, training data, and learning algorithms of the language model will influence its ability to understand and translate the intermediate representation.
 * Alignment between Representation and Language: The closer the intermediate representation is to the natural language constructs and semantics, the easier it will be for the language model to perform accurate translations.
 * Robustness to Noise and Ambiguity: The representation should be designed to handle common sources of noise and ambiguity in natural language, such as typos, idioms, and context-dependent meanings.
 * Handling of Edge Cases: The representation should be able to handle rare or complex Narsese constructs, ensuring that the language model can translate a wide range of inputs accurately.
 * Availability of Training Data: The quality and quantity of training data, which maps natural language to the intermediate representation, will be crucial for the language model's performance.
 *
 * By considering these factors, you can refine the intermediate representation language and syntax to create a highly effective translation system between natural language and Narsese.
 */