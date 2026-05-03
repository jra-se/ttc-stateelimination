package ttc.stateelimination;

import automata.AutomataMill;
import automata._ast.ASTAutomaton;
import automata._ast.ASTState;
import automata._ast.ASTTransition;
import com.google.common.base.Stopwatch;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.tf.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class AutStateElimination implements Runnable {
  
  public static boolean DEBUG = true;
  public static boolean SHOW_PROGRESS = false;
  public static boolean UNIFORMIZE_AUTOMATON = false;
  
  private final ASTAutomaton model;

  // Shared instance to avoid too many object creations
  protected final GlobalExtensionManagement sharedGlex = new GlobalExtensionManagement();
  
  public AutStateElimination(ASTAutomaton model) {
    this.model = model;
  }
  
  @Override
  public void run() {
    if (UNIFORMIZE_AUTOMATON) {
      debug("== Uniformize automaton");
      uniformizeAutomaton();
      debug("== Automaton fully uniformized");
    }
    else {
      debug("== Starting preprocessing");
      applyPreprocessing();
      debug("== Preprocessing completed");
    }
    
    debug("== Starting state elimination");
    repeatEliminateOverAll();
    debug("== Elimination completed");
    //printResult();
  }
  
  public static void debug(String formatString, Object... args) {
    if (DEBUG) {
      System.out.printf((formatString) + "%n", args);
    }
  }
  
  public void printResult() {
    debug(AutomataMill.prettyPrint(this.model, false));
  }
  
  public void printProgress() {
    if (SHOW_PROGRESS) {
      System.out.printf("[Progress] %d states and %d transitions remaining%n", this.model.sizeStates(),
          this.model.sizeTransitions());
    }
  }

  public void repeatEliminateOverAll() {
    printProgress();
    var trafo = new AUTFindStateAndTransitions(this.sharedGlex, this.model);

    List<PreProcessState> statesToElim = new ArrayList<>();
    
    TrafoUtil.executeTrafoRepeated(() -> trafo, t -> {
      int transitionFactor = trafo.get_transition_1().size() * trafo.get_transition_2().size();
      statesToElim.add(new PreProcessState(trafo.get_$S(), transitionFactor));
    }, f -> {});

    // sort by incoming*outgoing transition size
    statesToElim.sort(Comparator.comparingInt(o -> o.transitionFactor));

    for (var step : statesToElim) {
      debug("  - Selected node to eliminate: %s", step.state.getName());
      printProgress();
      eliminate(step.state);
    }
    printResult();

  }

  record PreProcessState (ASTState state, int transitionFactor ){
  }
  
  public void eliminate(ASTState node) {
    fixTransitions(node);
    delete(node);
    unifyParallelTransitions();
    //printResult();
  }
  
  public void fixTransitions(ASTState node) {
    AUTFixTransitions trafo = new AUTFixTransitions(this.sharedGlex, this.model);
    trafo.set_state_1(node);
    TrafoUtil.executeTrafoRepeated(() -> trafo,
        t -> debug("   ! Create new transition: %s - %s > %s", t.get_$NEWT().getFrom(),
            t.get_$NEWT().getInput(), t.get_$NEWT().getTo()), f -> {});
  }
  
  public void delete(ASTState node) {
    debug("   X Delete node: %s", node.getName());
    AUTDeleteState trafo = new AUTDeleteState(this.sharedGlex, this.model);
    trafo.set_state_1(node);
    TrafoUtil.executeTrafo(trafo);
    for (ASTTransition transition : trafo.get_transition_1()) {
      debug("   X Delete transition: %s - %s > %s", transition.getFrom(),
          transition.getInput(), transition.getTo());
    }
    for (ASTTransition transition : trafo.get_transition_2()) {
      debug("   X Delete transition: %s - %s > %s", transition.getFrom(),
          transition.getInput(), transition.getTo());
    }
  }
  
  public void unifyParallelTransitions() {
    TrafoUtil.executeTrafoRepeated(() -> new AUTUnifyTransitionsMulti(this.sharedGlex, this.model),
       trafo -> {
            debug("   & UnifyTransitionMulti success");
            debug("   & Delete transition: %s - %s > %s",
                trafo.get_transition_1().getFrom(), trafo.get_transition_1().getInput(),
                trafo.get_transition_1().getTo());
        }, f -> {});
  }
  
  public void uniformizeAutomaton() {
    debug("== Starting preprocessing");
    AUTUniformCreateStates trafo1 = new AUTUniformCreateStates(this.sharedGlex, this.model);
    trafo1.set_$initialName("xInitial");
    trafo1.set_$finalName("xFinal");
    if (TrafoUtil.executeTrafo(trafo1)) {
      debug("  * Created new initial and final states");
    }
    else {
      debug("  * Failed to create new initial and final states");
    }
    
    TrafoUtil.executeTrafoRepeated(() -> {
      AUTUniformInitial trafo = new AUTUniformInitial(this.sharedGlex, this.model);
      trafo.set_state_1(trafo1.get_state_1());
      return trafo;
    }, trafo -> {
        debug("   * Remove initial modifier from %s", trafo.get_state_2().getName());
        debug("   * Add transition: %s - %s > %s", trafo.get_transition_1().getFrom(),
            trafo.get_transition_1().getInput(), trafo.get_transition_1().getTo());
    }, f -> {});
    
    TrafoUtil.executeTrafoRepeated(() -> {
      AUTUniformFinal trafo = new AUTUniformFinal(this.sharedGlex, this.model);
      trafo.set_state_1(trafo1.get_state_2());
      return trafo;
    }, trafo -> {
        debug("   * Remove final modifier from %s", trafo.get_state_2().getName());
        debug("   * Add transition: %s - %s > %s", trafo.get_transition_1().getFrom(),
            trafo.get_transition_1().getInput(), trafo.get_transition_1().getTo());
    }, f -> {});
  }
  
  public void applyPreprocessing() {
    debug("== Starting preprocessing");
    if (TrafoUtil.executeTrafo(new AUTCreateInitialDummyIfNeeded(this.sharedGlex, this.model))) {
      debug("  * Created dummy initial state");
    }
    else {
      debug("  * No need to create dummy initial state");
    }
    
    if (TrafoUtil.executeTrafo(new AUTCreateFinalDummyIfNeeded(this.sharedGlex, this.model))) {
      debug("  * Created dummy final state");
    }
    else {
      debug("  * No need to create dummy final state");
    }
  }
  
  public void checkResult(Stopwatch stopwatch) {
    long numStates = this.model.sizeStates();
    long numTransitions = this.model.sizeTransitions();
    System.out.println("=== Check result");
    System.out.println("  # states: " + numStates);
    System.out.println("  # transitions: " + numTransitions);
    System.out.println("  # runtime: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
    if (numStates == 2 && numTransitions == 1) {
      System.out.println("   ### RESULT LOOKS GOOD ###");
    }
    else {
      throw new RuntimeException("State Eliminations seems to have failed!");
    }
  }
}
