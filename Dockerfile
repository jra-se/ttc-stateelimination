FROM gradle:8.14.4-jdk21

WORKDIR /home/gradle/src

ADD models models
ADD emf-to-automaton emf-to-automaton
ADD state-elimination-problem state-elimination-problem
ADD gradle.properties gradle.properties
ADD settings.gradle settings.gradle
ADD run_benchmark.sh run_benchmark.sh

# build the models & pre-build jar
RUN	gradle state-elimination-problem:assemble

ENTRYPOINT ["sh", "run_benchmark.sh"]
