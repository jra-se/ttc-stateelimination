# A MontiCore solution to the TTC 2017 State Elimination case

# Introduction

This repository contains our solution based
on [MontiCore](https://github.com/MontiCore/monticore) for
the [TTC 2017 State Elimination Case](https://transformationtoolcontest.github.io/2017/solutions_stateElimination.html).

We use the [Automaton](https://github.com/MontiCore/automaton)
language to represent finite state automata. The provided example automata must
therefore first be converted from the EMF-compliant XMI files into AUT files.
The state elimination algorithm was implemented using model transformations
written in the automatically generated domain-specific transformation language (
DSTL) for the automaton language and ultimately composed using hand-written Java
code.

# Running the Solution using Docker

To run the solution using docker (without any other prerequisites):

1. run the image: `docker run --rm ghcr.io/jra-se/ttc-stateelimination:main`
2. inspect the results. They are:
   * printed to the console and
   * written to the `build/benchmark/benchmark.csv` file (in the container)

# Prerequisites

* Gradle 8.14.4 (Gradle 9.x or higher are currently unsupported)
* Java 21 or higher

# Running the Solution

We have two options for conducting the evaluation.

* In the JUnit test class
  [`AutCorrectnessTest`](state-elimination-problem/src/test/java/ttc/stateelimination/aut/AutCorrectnessTest.java)
  the focus is on the correctness of the algorithm and the computed regular
  expression. For each model, the runtime and the resulting RegEx length are
  printed. In addition, the provided positive and negative words are matched
  against the resulting regular expression to verify its correctness.
  (run with ```gradle runSolution```)
* The implementation of the
  [`Benchmark`](state-elimination-problem/src/test/java/ttc/stateelimination/aut/Benchmark.java)
  class, on the other hand, focuses on performance evaluation. In this case, a
  warmup is performed first by applying the algorithm multiple times to a
  smaller model. This prevents class loading and the JVM restart from negatively
  affecting the runtime required by the first model.
  (run with ```gradle runBenchmark```)
  * This task outputs the `build/benchmark/benchmark.csv` file


# Structure of the Implementation

### Folder `models`
This folder contains the sample automata provided by the case authors
that are used for evaluation. These include
an Ecore metamodel as well as the corresponding XMI files.

### Subproject `emf-to-automaton`

This subproject contains the implementation for converting EMF files
to AUT files. The conversion is initiated by calling the `main`
method in the `EmfToAUT` class. Alternatively, you can run the Gradle task
```gradle runEmfToAUT```. The converted models are
then located in the `emf-to-automaton/build/testdata` folder.

### Subproject  `state-elimination-problem`

This subproject contains the actual implementation of the state elimination
algorithm. This includes, on the one hand, the transformation definitions and, on the
other hand, hand-written Java code that handles the orchestration.

#### Transformationen

The solution is fundamentally based on transformations written in the DSTL, which is part of the Automaton
language.

The six necessary transformations are listed and briefly explained below:

* [
  `AUTCreateInitialDummyIfNeeded.mtr`](state-elimination-problem/src/main/transformations/aut/AUTCreateInitialDummyIfNeeded.mtr):
  Creates a new initial state and a transition to the old
  initial state if the old initial state has an incoming transition.
* [
  `AUTCreateFinalDummyIfNeeded.mtr`](state-elimination-problem/src/main/transformations/aut/AUTCreateFinalDummyIfNeeded.mtr):
  Creates a new final state and a transition from the old final
  state if the old final state has an outgoing transition.
* [
  `AUTFindStateAndTransitions.mtr`](state-elimination-problem/src/main/transformations/aut/AUTFindStateAndTransitions.mtr):
  Find an eliminable state (a state that is neither the initial nor the final state)
  and all its incoming and outgoing transitions.
* [
  `AUTFixTransitions.mtr`](state-elimination-problem/src/main/transformations/aut/AUTFixTransitions.mtr):
  Create a new transition that can replace a pair of incoming and outgoing
  transitions of a state.
* [
  `AUTDeleteState.mtr`](state-elimination-problem/src/main/transformations/aut/AUTDeleteState.mtr):
  Delete a state and all its incoming and outgoing transitions.
* [
  `AUTUnifyTransitionsMulti.mtr`](state-elimination-problem/src/main/transformations/aut/AUTUnifyTransitionsMulti.mtr):
  Merge two parallel transitions.

When executing, MontiCore generates a Java class for each transformation
definition, which contains a pattern-matching implementation as well as the
transformation implementation.

#### Java Implementation

The orchestration of state elimination transformations is handled by the
[`AutStateElimination`](state-elimination-problem/src/main/java/ttc/stateelimination/AutStateElimination.java)
Java class. Its behavior can be controlled via three static flags.

| Flag                 | Description                                                                                                                               |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| UNIFORMIZE_AUTOMATON | The preprocessing is modified so that models with multiple initial or final states can also be considered.                                |
| DEBUG                | Enables detailed output that allows you to track every single step of the state elimination process and the model changes that were made. |
| SHOW_PROGRESS        | Enables the printing of the number of remaining states and transitions after each eliminated state.                                       |
