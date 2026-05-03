#!/bin/bash
# to run it via gradle:
#gradle runBenchmark --info; cat build/benchmark/benchmark.csv
# run the pre-build jar:
# Be warned: the textual .aut models are created during the docker built and not shipped with the jar
java -jar ./state-elimination-problem/build/libs/state-elimination-problem-1.0-SNAPSHOT-test.jar; cat build/benchmark/benchmark.csv
