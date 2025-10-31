# Narsese - The Language of NARS

Narsese is the formal language used for input and output in Non-Axiomatic Reasoning Systems (NARS), including NARchy. It allows the system to receive tasks (beliefs, goals, questions) and communicate its derived conclusions.

**Tasks** can arrive at any time. There are no restrictions on their content as far as they can be expressed in Narsese. By default, NARS makes *no assumptions* about the meaning or truth value of input beliefs and goals. How to choose proper inputs and interpret possible outputs for each application is an *open problem* to be solved by its users.

A Narsese task generally follows this structure:
`[budget] <term> <punct> [occurrence] [truth]`

## Punctuation Marks

Punctuation indicates the type of the task:

*   `.`  **Belief**: Represents factual evidence to revise existing knowledge and derive new conclusions.
*   `!`  **Goal**: A state to be realized, potentially leading to system operations.
*   `?`  **Question**: A query about the system's current belief state regarding a term.
*   `@`  **Quest**: A query about the system's goal state, seeking procedural answers or ways to achieve a goal.

## Term Syntax

A term is the core content of a Narsese statement.

```
               <term> ::=
                        | <atom>                             // an atomic constant term; Unicode string in an arbitrary alphabet
                        | <integer>                          // integer number
                        | <variable>                         // an atomic variable term
                        | <compound>                         // a term with internal structure

           <compound> ::=
                        | "(--," <term> ")"                  // negation
                        | "--" <term>                          // negation shorthand

                        | "(" <term> {","<term>} ")"         // product (ie. un-ordered vector or list, length >= 0)
                        | "{" <term> {","<term>} "}"         // extensional set (ordered, all unique, length >= 1)
                        | "[" <term> {","<term>} "]"         // intensional set (ordered, all unique, length >= 1)

                        | "("<term> "-->" <term>")"          // inheritance
                        |    <term> ":" <term>               // reverse-inheritance (shorthand)

                        | "("<term> "<->" <term>")"          // similarity (commutative)
                        | "("<term> "<~>" <term>")"          // instance (commutative, NARchy specific, check NARchy docs for exact semantics)

                        | "("<term> "==>" <term>")"          // implication
                        | "("<term> "==>"<dt> <term>")"      // implication with temporal difference (dt)
                        | "("<term> "=|>" <term>")"          // concurrent implication (commutative, dt=0)

                        | "(&&," <term> {","<term>} ")"      // conjunction (commutative)
                        |   "("<term> "&&" <term>")"           // conjunction (commutative, shorthand for size=2)
                        |   "("<term> "&&"<dt> <term>")"       // sequential conjunction (size=2 only, preserving time direction)
                        |   "(&|," <term> {","<term>} ")"      // parallel conjunction (shorthand for && with dt=0)
                        |   "(&/," <term> {","<term>} ")"      // sequential conjunction (internally converted, NARchy specific)

                        | "(||," <term> {","<term>} ")"      // disjunction (commutative, NARchy converts this)
                        |   "("<term> "||" <term>")"           // disjunction (commutative, shorthand for size=2)


                        | "("<term> "-{-" <term>")"          // instance membership (e.g., {bird} --> animal means 'bird is an instance of animal')
                        | "("<term> "-]-" <term>")"          // property (e.g., bird --> [has_wings] means 'bird has the property of having wings')
                        | "("<term> "{-]" <term>")"          // instance-property (e.g., {Tweety} --> [is_yellow])

                        | "(&," <term> {","<term>} ")"       // extensional intersection (e.g., (water & liquid) )
                        | "(|," <term> {","<term>} ")"       // intensional intersection (e.g., (bird | flyer) )
                        | "(-," <term> "," <term> ")"        // extensional difference (e.g., (bird - penguin) )
                        | "(~," <term> "," <term> ")"        // intensional difference (e.g., (animal ~ mammal) )

                        | <term>"("<term> {","<term>} ")"    // operation (functional form, e.g., triangle(A,B,C) internally ((A,B,C)-->triangle) )


                 <dt> ::= [+|-]<number>                      // delta-time amount (frames/steps); positive = future, negative = past, +0 = simultaneous
                        | [+|-]<number>["ms"|"s"|"min"|"hr"|"day"] // delta-time with time units (NARchy specific)
                  // note: <dt> is stored as a 32-bit signed integer in NARchy

```

**Note:**
*   This grammar provides a general overview. Specific NARS implementations like NARchy may have variations, extensions, or restrictions. Refer to NARchy's specific documentation for precise details on its Narsese dialect.
*   Built-in 'Functors' (operations) can be executed inline during term processing. See NARchy's `BuiltIn.java` or equivalent.
*   Input terms may undergo transformations and reductions. See NARchy's `Op.java` or equivalent.

## Truth Values

Truth values in NARS are typically represented by a pair of numbers: frequency and confidence.

`<truth> ::= "%"<frequency>[";"<confidence>]"%" `

*   **Frequency** `[0.0, 1.0]`:
    *   `0.0`: Represents "never" or complete falsehood.
    *   `0.5`: Represents "maybe" or uncertainty.
    *   `1.0`: Represents "always" or complete truth.
*   **Confidence** `(0.0, 1.0]`:
    *   Indicates the system's confidence in the assigned frequency. A value closer to `1.0` means higher confidence.
    *   NARchy uses `(0,1]` where `confidence=1.0` can trigger a locked axiomatic belief.

## Occurrence Time

Occurrence time specifies when a task is relevant, either relatively or absolutely.

*   `[occurrence]` is often a 64-bit integer.
*   If unspecified, it might default to "eternal" or the current processing time, depending on the NARS implementation and task type. NARchy has specific handling for temporal events.

## Budget Values

Budget values manage the system's attention allocation.

`<budget> ::= "$"<priority>[";"<durability>]"%" ` (General NARS format)
`<budget> ::= "$"<priority> ` (Commonly used in NARchy for input)


*   **Priority** `[0.0, 1.0]`:
    *   Quantifies the demand for attention relative to other items.
    *   If unspecified, a default priority is assigned based on punctuation, truth, or system configuration.
*   **Durability** (less commonly specified on input):
    *   Influences how long an item persists in memory.

## Variables

Narsese supports different types of variables for pattern matching and inference:

*   `$X`, `$Y`, ... : **Independent variables**. Typically must span a statement (appear on both sides of a relation).
*   `#X`, `#Y`, ... : **Dependent variables**. Their value depends on the instantiation of independent variables.
*   `?X`, `?Y`, ... : **Query variables**. Used in questions to indicate what the question is asking for.
*   `%X`, `%Y`, ... : **Pattern variables** (NARchy specific or less common). A general variable type for meta-NAL to match terms, including other variables.


For more comprehensive and up-to-date information on Narsese, please refer to the official OpenNARS documentation and specific documentation for the NARS implementation you are using (like NARchy).
