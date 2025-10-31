# Plan for Translating njava/nar to Rust

This document outlines a plan for translating the `njava/nar` Java project to Rust. The translation will be done in stages to ensure a manageable and organized process.

## Guiding Principles

*   **Idiomatic Rust:** Strive to write clean, idiomatic Rust code. This includes leveraging Rust's powerful features like the ownership model, enums, and pattern matching.
*   **Code Quality:**
    *   Use `rustfmt` to maintain a consistent code style across the project.
    *   Regularly run `clippy` to catch common mistakes and improve code quality.
*   **Test-Driven Development (TDD):** Where possible, follow a TDD approach. Write tests for new functionality before implementing it. This ensures that the code is correct and that regressions are caught early.
*   **Clarity and Documentation:** Write clear and concise code. Add documentation where necessary to explain complex logic.

## Stage 1: Project Setup and Core Data Structures

1.  **Initialize Cargo Project:**
    *   Set up the `Cargo.toml` file with necessary dependencies.
    *   Create the initial directory structure in `src/`.

2.  **Translate Core Data Structures:**
    *   `Term.java` -> `src/term/mod.rs`: Translate the base `Term` class and its subclasses (e.g., `CompoundTerm`, `AtomicTerm`).
    *   `Truth.java` -> `src/truth/mod.rs`: Translate the `Truth` value representation.
    *   `Task.java` -> `src/task/mod.rs`: Translate the `Task` class, which represents a unit of work.
    *   `Concept.java` -> `src/concept/mod.rs`: Translate the `Concept` class, a core component of the NARS memory.

## Stage 2: Memory Implementation

1.  **Translate Memory Structures:**
    *   `memory/` -> `src/memory/mod.rs`: Implement the main memory structures, including the concept bag and task buffers.
    *   Focus on translating `nars.memory` and related classes.

## Stage 3: Control and Derivation

1.  **Translate the Control Loop:**
    *   `control/` -> `src/control/mod.rs`: Implement the main control loop of the NARS engine.
    *   This will involve translating classes from `nars.control`.

2.  **Translate the Derivation Engine:**
    *   `deriver/` -> `src/deriver/mod.rs`: Translate the inference rules and the derivation mechanism.
    *   This is a complex stage and will require careful attention to the logic in `nars.deriver`.

## Stage 4: Actions and Operators

1.  **Translate Actions:**
    *   `action/` -> `src/action/mod.rs`: Translate the various actions that the system can perform.

2.  **Translate Operators:**
    *   `operator/` -> `src/operator/mod.rs`: Translate the operators that can be executed by the system.

## Stage 5: I/O and Narsese Parser

1.  **Translate Narsese Parser:**
    *   `Narsese.java` -> `src/parser/mod.rs`: Implement a parser for the Narsese language.

2.  **Translate I/O:**
    *   `io/` -> `src/io/mod.rs`: Handle input and output for the system.

## Stage 6: Testing

1.  **Unit Tests:**
    *   Write unit tests for each translated module to ensure correctness.
    *   Use the existing Java tests as a reference.

2.  **Integration Tests:**
    *   Create integration tests that test the entire system.
    *   These tests should cover common use cases and scenarios.

## Stage 7: Refinement and Documentation

1.  **Code Refinement:**
    *   Review and refactor the translated code to be more idiomatic Rust.
    *   Improve performance and memory safety.

2.  **Documentation:**
    *   Write comprehensive documentation for the Rust codebase.
    *   Include examples and usage instructions.
