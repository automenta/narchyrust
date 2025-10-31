# NARchy - A Non-Axiomatic Reasoning System

NARchy is an open-source reasoning system derived from [OpenNARS](https://github.com/opennars). It implements a form of Non-Axiomatic Logic (NAL), designed for systems with insufficient knowledge and resources, allowing them to learn from experience and adapt to their environment.

## Purpose and Relationship to OpenNARS

NARchy builds upon the foundations of OpenNARS, aiming to explore and extend its capabilities in several key areas, particularly:

*   **Advanced Temporal Reasoning**: NARchy introduces significant enhancements to NAL's temporal logic (NAL7), allowing for continuous-time representation and reasoning about events with arbitrary temporal resolutions.
*   **Scalability and Performance**: It incorporates features like multithreaded execution, optimized data structures (e.g., HijackBag, CurveBag), and a concurrent concept index to improve performance and scalability.
*   **Sensorimotor Integration**: NARchy emphasizes the integration of the reasoning core with sensorimotor capabilities, facilitating the development of autonomous agents that can perceive and act in complex environments (e.g., via the NAgent API).
*   **Inter-Agent Communication**: Explores multi-agent communication and learning through the "InterNARchy" protocol.

While OpenNARS provides a robust and well-documented core NAL engine, NARchy serves as a platform for research and development into these and other advanced AGI topics, sometimes diverging in implementation details while adhering to the core principles of NAL.

## High-Level Architecture

NARchy's architecture, like NARS, revolves around a few key components:

1.  **Memory**: Manages concepts, tasks (beliefs, goals, questions), and their relationships. NARchy employs optimized and concurrent data structures for this.
2.  **Inference Engine (Deriver)**: Applies NAL inference rules to derive new knowledge from existing tasks and beliefs. NARchy's deriver is enhanced for temporal reasoning and includes features like inline term rewriting.
3.  **Control System**: Manages the system's attention allocation and processing cycle. This includes selecting tasks and concepts for processing, managing budgets (priority and durability), and triggering actions. NARchy features include pressurized auto-balanced forgetting.
4.  **Narsese I/O**: Uses Narsese as its input/output language. Tasks are fed into the system, and results (answers to questions, achieved goals) are reported. (See [./docs/NARSESE.md](./docs/NARSESE.md) for a detailed Narsese grammar reference).
5.  **Operational Components**: Modules that connect the core reasoner to the external world, including user interfaces, sensor/motor wrappers, and network interfaces.

## Submodules

NARchy is organized into the following primary Maven submodules:

*   **`nar`**: The core Non-Axiomatic Logic (NAL) reasoning engine. It contains the fundamental data structures for terms, concepts, tasks, the inference rules, and memory management. This is where the primary logic of NARS is implemented.
*   **`os`**: Provides the "Operating System" or operational shell for NARchy. This includes user interfaces (GUI via SpaceGraph, TUI), multimedia input/output handling (audio, video), main application entry points (`NARchy.java`), and web interfaces.
*   **`app`**: Contains various applications, tools, and integrations built on the NARchy platform. This includes the NAgent sensor/motor API for building autonomous agents, game interaction frameworks, and interfaces for external systems or languages (e.g., IRC, KIF, Prolog).
*   **`lab`**: A laboratory for experimental research, prototypes, and testing. This module houses integrations with external projects (e.g., Quake II via `jake2`, Arcade Learning Environment via `ale`), various AI/ML experiments, and advanced visualization tools using `spacegraph`.

The broader NARchy ecosystem also relies heavily on `jcog` (utilities for cognitive software) and `spacegraph` (fractal GUI), which provide foundational utilities and visualization capabilities.

## Key Features and Enhancements

NARchy introduces several notable features and improvements over earlier NARS versions, focusing on advanced reasoning and performance:

*   **Advanced Temporal Reasoning**:
    *   **Continuous-Time NAL7**: Employs numeric time differences for more expressive and flexible temporal logic.
    *   **Temporal Belief Tables**: Sophisticated mechanisms for evaluating concept truth values changing over time.
*   **Performance and Scalability**:
    *   **Multithreaded Execution**: Enables parallel processing for improved throughput.
    *   **Optimized Data Structures**: Utilizes custom concurrent collections like `HijackBag` (lock-free) and `CurveBag` (sorted).
    *   **Concurrent Concept Index**: Efficiently manages active and inactive concepts, with support for persistence.
    *   **Pressurized Auto-balanced Forgetting**: Dynamically adjusts memory forgetting rates based on system load.
    *   **Binary I/O Codec**: A compact serialization format for terms and tasks, reducing overhead.
    *   **Adaptive Concept Allocation**: Policies for managing concept data structure capacities dynamically.
*   **Core Logic Enhancements**:
    *   **Full-Spectrum Negation**: Integrates negation directly within concepts, rather than treating them as separate entities.
    *   **Enhanced Deriver**: Features inline term rewriting and advanced temporalization of derived knowledge.
    *   **Virtual Disjunctions**: Transforms disjunctions into negated conjunctions to better preserve temporal information.
*   **Agent and System Integration**:
    *   **NAgent Sensor/Motor API**: A structured framework for connecting NARS to external environments, crucial for reinforcement learning and robotics.
    *   **InterNARchy**: A peer-to-peer protocol designed for multi-agent communication and collaborative learning.

These features collectively aim to provide a more powerful, flexible, and scalable NARS implementation.

## Application Possibilities

NARchy's design makes it suitable for a range of applications, including:

*   **Autonomous Agents**: Developing intelligent agents that can learn and adapt in dynamic environments (e.g., game AI, robotics).
*   **Knowledge Representation and Reasoning**: Building systems that can manage and reason with uncertain, incomplete, and contradictory information.
*   **Natural Language Understanding**: Processing and understanding human language in context.
*   **Predictive Systems**: Learning patterns and making predictions based on observed data.
*   **Semantic Web and Data Integration**: Reasoning over distributed and heterogeneous knowledge sources.

## Potential Future Development

Future development of NARchy could focus on:

*   **Improved Learning Efficiency**: Enhancing the speed and robustness of learning from sparse data.
*   **Advanced Cognitive Architectures**: Integrating NARS with other cognitive faculties like attention, motivation, and emotion.
*   **User-Friendly Tooling**: Developing better tools for interacting with, debugging, and visualizing the reasoning process.
*   **Real-World Applications**: Applying NARchy to solve complex real-world problems.
*   **Distributed and Decentralized NARS**: Further development of the InterNARchy concept for large-scale distributed intelligence.

## Getting Started

*   **Requirements**: JDK 25+ and Apache Maven.
*   **Build**: Navigate to the NARchy project directory and use Maven to build the project:
    ```bash
    mvn clean install
    ```
*   **Running**: The `os` submodule (`narchy/os`) typically contains the main entry points for executing NARchy (e.g., `NARchy.java`). Refer to that module for specific instructions on launching different interfaces (GUI, TUI, web).

## References and Further Reading

*   **OpenNARS Project**: [https://github.com/opennars/opennars](https://github.com/opennars/opennars)
*   **NARS Theoretical Background**:
    *   Wang, P. (2006). *Rigid Flexibility: The Logic of Intelligence*. Springer.
    *   Wang, P. (2013). *Non-Axiomatic Logic: A Model of Intelligent Reasoning*. World Scientific.
    *   [Dr. Pei Wang's Publications](http://www.cis.temple.edu/~pwang/papers.html)
*   **Narsese Grammar**: See [./docs/NARSESE.md](./docs/NARSESE.md) in this repository.

This README provides a high-level overview of NARchy. For more in-depth information, please refer to the source code, specific module documentation (if available), and the references above.
