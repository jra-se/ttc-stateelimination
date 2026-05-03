package ttc.stateelimination.aut;

import automata.AutomataMill;
import automata._ast.ASTAutomaton;
import automata._ast.ASTTransition;
import com.google.common.base.Stopwatch;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ttc.stateelimination.AutStateElimination;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DebuggingTest {
  
  public static String MODEL_PATH = "../models-aut";
  public static String MODEL_SUFFIX = ".aut";
  
  @ParameterizedTest
  @ValueSource(strings = { "easy_theory"})
  public void test(String modelName) {
    String modelPath = MODEL_PATH + "/" + modelName + MODEL_SUFFIX;
    
    ASTAutomaton ast = setup(Path.of(modelPath));
    
    AutStateElimination.DEBUG = false;
    AutStateElimination se = new AutStateElimination(ast);
    
    Stopwatch stopwatch = Stopwatch.createStarted();
    se.run();
    stopwatch.stop();
    
    se.checkResult(stopwatch);
    
    List<ASTTransition> transitions = ast.getTransitionList();
    
    assertEquals(1, transitions.size(),
        "Expected exactly one transition, found " + transitions.size());
    
    String regex = transitions.getFirst().getInput();
    
    System.out.println("Regex: " + regex);
    //assertEquals("ea((b|aa)a)*(e(a)*e|ab(a)*e)", regex);
    assertEquals("e(a(b|aa))*a(e|ab)(a)*e", regex);
  }
  
  private static ASTAutomaton setup(Path modelPath) {
    AutomataMill.init();
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
}
