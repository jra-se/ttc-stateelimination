package ttc.stateelimination.aut;

import automata.AutomataMill;
import automata._ast.ASTAutomaton;
import automata._ast.ASTTransition;
import com.google.common.base.Stopwatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ttc.stateelimination.AutStateElimination;
import ttc.stateelimination.shared.TestFramework;
import ttc.stateelimination.shared.TestFrameworkWordResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AutCorrectnessTest extends TestFramework {
  
  @BeforeEach
  void setUp() {
    AutomataMill.reset();
    AutomataMill.init();
  }
  
  @ParameterizedTest
  @ValueSource(strings = {
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
      "leader6_4",
      "leader6_5",
      "leader6_6"
  })
  @Timeout(value = 10, unit = TimeUnit.MINUTES, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
  public void test(String modelName) {
    ASTAutomaton ast = setup(getModelPath(modelName));
    
    AutStateElimination.DEBUG = false;
    AutStateElimination se = new AutStateElimination(ast);
    
    Stopwatch stopwatch = Stopwatch.createStarted();
    se.run();
    stopwatch.stop();
    
    se.checkResult(stopwatch);
    
    List<ASTTransition> transitions = ast.getTransitionList();
    
    assertEquals(2, ast.sizeStates(), "Expected exactly tweo states, found " + ast.sizeStates());
    assertEquals(1, transitions.size(),
        "Expected exactly one transition, found " + transitions.size());
    
    String regex = transitions.getFirst().getInput();
    
    System.out.println(
        "# time: " + stopwatch.elapsed(java.util.concurrent.TimeUnit.MILLISECONDS) + "ms");
    System.out.println("# regex size: " + getRegexSize(regex));
    
    TestFrameworkWordResult acceptedWordsResult = testWords(regex, getPositiveDataPath(modelName), true);
    TestFrameworkWordResult rejectedWordsResult = testWords(regex, getNegativeDataPath(modelName), false);
    System.out.println(
        "# accepted words: " + acceptedWordsResult.passed() + "/" + acceptedWordsResult.total());
    System.out.println(
        "# rejected words: " + rejectedWordsResult.passed() + "/" + rejectedWordsResult.total());
    assertEquals(acceptedWordsResult.total(), acceptedWordsResult.passed(),
        "Not all positive words were accepted");
    assertEquals(rejectedWordsResult.total(), rejectedWordsResult.passed(),
        "Not all negative words were rejected");
    
  }
  
  private static ASTAutomaton setup(Path modelPath) {
    try {
      Optional<ASTAutomaton> opt =
          AutomataMill.parser().parse(modelPath.toAbsolutePath().toString());
      // Build ST
      AutomataMill.scopesGenitorDelegator().createFromAST(opt.get());
      return opt.get();
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  
  @Override
  protected String getModelBasePath() {
    return "../emf-to-automaton/build/testdata/task-main";
  }
  
  @Override
  protected String getPositiveDataBasePath() {
    return "build/resources/test/testdata/acceptedWords";
  }
  
  @Override
  protected String getNegativeDataBasePath() {
    return "build/resources/test/testdata/notAcceptedWords";
  }
  
  @Override
  protected String getModelSuffix() {
    return ".aut";
  }
}

