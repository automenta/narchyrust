# Features
NARchy is a derivative of OpenNARS (Non-Axiomatic Reasoning System, a general-purpose experience-driven logic processor).

 * Beliefs
 * Goals
 * Questions (and Quests)

## n-ary temporal truth revision / projection
 * tasks have either eternal or temporal occurrence
 * temporal occurrence is defined as a start,end interval (not limited to time-points)
 * evidence 'curves' fade beyond the time before and after a task, allowing inexact mixing in proportion to temporal proximities
 * task confidence represents the mean confidence of a task across its occurrence interval.
 * precise integral calculated in projections
 * several models including: inverse-square, inverse-linear
 * partial eternalization parameter
 * variable precision and evidence thresholds

## real-time performance and precision
ready for robotics and embedded applications

## functors and evaluation
term reductions, prolog-like inline functor evaluation and variable binding

## attention composed of multiple focuses
* individually prioritizable
* individially configurable (capacity, volMax, etc..)
* focuses can share concepts, otherwise focus bags do not leak into each other.  this supports memory management and staged pipelining of processes like a laboratory of reaction chambers..  termlink bags, and other plugins are generally focus-local by default and thus also maintain the 'closure'.

## parallel
* Multithreaded focus sampling.
* Bag implementations: ArrayBag and HijackBag
* TemporalBeliefTable
    * merge/compresson heuristics
* SeriesBeliefTable

## balanced truth extrema
freq(0) = "never" = not "always"
freq(1) = "always" = not "never"
User ultimately defines the meaning of "always" or "never".

## memory efficiency
*Strategies for deduplication of terms, tasks, concepts, memoized functions, etc..
* conj InhBundling and factoring

## dynamic task truth models

## temporal sequence term encoding
temporally precise sequence reasoning, with "intermpolation".

## reinforcement learning API
sensors + actions + rewards
* for fluent, declarative implementations to control reality, or simulations (Games) of it
* mix in(stinct) or kickstart with "classical" RL ex: DQN
* adjustable training strength allows a unique reverse-mode operation for using RL like a sponge to absorb reasoner activity

## prediction and projection API
time-series forecasting with Predictor's and differentiable DeltaPredictors
from/to any sets of concepts from/to any time(s).

## metagame homonculus
for maximizing system meta-goals, ex: dexterity (conf in action), while minimizing system costs (ex: cpu usage)

## plugin API
compose a runtime with collections of "Part"/"Parts" instances.  enablement of each can be separately toggled on/off without add/remove

## reflection API
proxy instrumentation and scaffolding for reasoning about and controlling other JVM programs

## testing API
for ensuring expected behavior and measuring performance
* implemented with JUnit 5

## configuration
* opportunities for endless experimentation.  contribute your own implementations
* evolutionary search through numeric and combinatorial parameter spaces

## memory transfer
compact binary serialization codec to/from disk, network, and off-heap

## InterNARchy
decentralized mesh network
* udp transport, with optional compression
* ask/tell semantics
* enables teams of instances to collaborate
* LAN multicast discovery

## delta operator Δ aka /\\ 
* approximate differentiability
* truth calculation according to task occurrence

## equality operator =
* symbolic expressions (forward and reverse solving)

## arithmetic functors
* forward and backward solvers
* integer domain
* add(x,y)=z
* mul(x,y)=z
* cmp(a,b)=z for z={-1, 0, +1} calculation of numeric / natural ordering
* 'arithmeticize' arithmetic introduction
* vector arithmetic (vector+vector, vector*scalar) for number-containing product terms of compatible arity 

## explode functor
* reversible recursive substitution deduplication compression

## member functor

## enhanced images
* repeats
* negated images

## new truth functions
* Conduct
* SemiConduct
* Biduct
* Divide
* Similarity
* Suppose
* (others)