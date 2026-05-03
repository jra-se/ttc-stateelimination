package ttc.stateelimination.aut;

import automata.AutomataMill;
import automata._ast.ASTAutomaton;
import com.google.common.base.Stopwatch;
import ttc.stateelimination.AutStateElimination;
import ttc.stateelimination.shared.TestFramework;
import ttc.stateelimination.shared.TestFrameworkWordResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public class Benchmark extends TestFramework {
  
  public static void main(String[] args) throws IOException {
    // Using the first model to warm up the JVM
    int warmupRuns = 20;
    List<String> warmupModelNames = List.of("leader3_2");
    List<String> modelNames =
        List.of(
            "leader3_2",
            "leader3_3",
            "leader3_4",
            "leader3_5",
            "leader3_6",
            "leader3_8",
            "leader4_2",
            "leader5_2",
            "leader6_2",
            "leader4_3",
            "leader4_4",
            "leader5_3",
            "leader4_5",
            "leader6_3",
            "leader4_6",
            "leader5_4",
            "leader5_5",
            "leader6_4"
            //"leader6_5",
            //"leader6_6"
            );
    new Benchmark().doRunBenchmark(warmupModelNames, warmupRuns, modelNames);
  }
  
  public void doRunBenchmark(List<String> warmupModelNames, int warmupRuns, List<String> modelNames)
      throws IOException {
    
    for (int i = 0; i < warmupRuns; i++) {
      for (String warmupModelName : warmupModelNames) {
        run(warmupModelName, true, null);
      }
    }
    
    StringBuilder resultData = new StringBuilder();
    
    resultData.append("modelname,");
    resultData.append("size of the regular expression,");
    resultData.append("time to transform (ms),");
    resultData.append("correctly accepted words,");
    resultData.append("correctly not accepted words");
    resultData.append("\n");
    
    File resultFile = new File("build/benchmark/benchmark.csv");
    System.out.println("Writing results continuously to " + resultFile.getAbsolutePath());
    resultFile.getParentFile().mkdirs();
    Files.writeString(resultFile.toPath(), resultData.toString(), StandardOpenOption.CREATE);
    
    for (String modelName : modelNames) {
      System.out.println("=== Model " + modelName + " ===");
      try {
        CompletableFuture.supplyAsync(() -> {
          run(modelName, false, resultData);
          return null;
        }).orTimeout(1, TimeUnit.HOURS).join();
      }
      catch (CompletionException e) {
        System.err.println("=== Failed Model " + modelName + " ===");
        e.printStackTrace();
        resultData.append(modelName).append(",");
        resultData.append("x,");
        resultData.append("x,");
        resultData.append("x,");
        resultData.append("x,");
        resultData.append("\n");
      }
      Files.writeString(resultFile.toPath(), resultData.toString(), StandardOpenOption.CREATE);
    }
    System.out.println("Done. Writing results to " + resultFile.getAbsolutePath());
  }
  
  protected void run(String modelName, boolean warmup, StringBuilder resultData) {
    AutomataMill.reset();
    AutomataMill.init();
    ASTAutomaton ast = setup(getModelPath(modelName));
    
    AutStateElimination.DEBUG = false;
    AutStateElimination se = new AutStateElimination(ast);
    
    Stopwatch stopwatch = Stopwatch.createStarted();
    se.run();
    stopwatch.stop();
    
    if (!warmup) {
      se.checkResult(stopwatch);
      
      assert ast.getTransitionList().size() == 1;
      
      System.out.println("Checking regex against known words...");
      String regex = ast.getTransitionList().getFirst().getInput();
      TestFrameworkWordResult acceptedWordsResult =
          testWords(regex, getPositiveDataPath(modelName), true);
      TestFrameworkWordResult rejectedWordsResult =
          testWords(regex, getNegativeDataPath(modelName), false);
      
      resultData.append(modelName).append(",");
      resultData.append(getRegexSize(regex)).append(",");
      resultData.append(stopwatch.elapsed(TimeUnit.MILLISECONDS)).append(",");
      resultData.append(acceptedWordsResult.passed()).append("/")
          .append(acceptedWordsResult.total()).append(",");
      resultData.append(rejectedWordsResult.passed()).append("/")
          .append(rejectedWordsResult.total());
      resultData.append("\n");
    }
  }
  
  private static ASTAutomaton setup(Path modelPath) {
    try {
      Optional<ASTAutomaton> opt =
          AutomataMill.parser().parse(modelPath.toAbsolutePath().normalize().toString());
      // Do NOT create a symbol table (as it is unused / creates overhead)
      return opt.get();
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  @Override
  protected String getModelBasePath() {
    return "emf-to-automaton/build/testdata/task-main";
  }
  
  @Override
  protected String getPositiveDataBasePath() {
    return "state-elimination-problem/src/test/resources/testdata/acceptedWords";
  }
  
  @Override
  protected String getNegativeDataBasePath() {
    return "state-elimination-problem/src/test/resources/testdata/notAcceptedWords";
  }
  
  @Override
  protected String getModelSuffix() {
    return ".aut";
  }
  
}
