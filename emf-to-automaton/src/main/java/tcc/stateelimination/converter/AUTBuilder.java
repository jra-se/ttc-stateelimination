package tcc.stateelimination.converter;

import automata.AutomataMill;
import automata._ast.ASTAutomaton;
import automata._ast.ASTState;
import automata._ast.ASTTransition;
import de.monticore.prettyprint.IndentPrinter;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import tcc.stateelimination.converter._prettyprinter.TTCAutomataFullPrettyPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class AUTBuilder {
  
  private EPackage ePackage;
  private Resource xmiResource;
  private Path outFile;
  
  public AUTBuilder(EPackage metamodel, Resource xmiResource, Path outFile) {
    this.ePackage = metamodel;
    this.xmiResource = xmiResource;
    this.outFile = outFile;
    
    build();
  }
  
  protected void build() {
    String automataName = this.outFile.getFileName().toString().replace(".aut", "");
    ASTAutomaton automaton = AutomataMill.automatonBuilder().setName(automataName).build();
    
    EClass stateClass = (EClass) this.ePackage.getEClassifier("State");
    EClass transitionClass = (EClass) this.ePackage.getEClassifier("Transition");
    EAttribute stateIdAttr =
        stateClass.getEAllAttributes().stream().filter(x -> x.getName().equals("id")).findFirst()
            .get();
    EAttribute isInitialAttr =
        stateClass.getEAllAttributes().stream().filter(x -> x.getName().equals("isInitial"))
            .findFirst().get();
    EAttribute isFinalAttr =
        stateClass.getEAllAttributes().stream().filter(x -> x.getName().equals("isFinal"))
            .findFirst().get();
    EAttribute transitionLabelAttr =
        transitionClass.getEAllAttributes().stream().filter(x -> x.getName().equals("label"))
            .findFirst().get();
    
    EReference transitionSource =
        transitionClass.getEAllReferences().stream().filter(x -> x.getName().equals("source"))
            .findFirst().get();
    EReference transitionTarget =
        transitionClass.getEAllReferences().stream().filter(x -> x.getName().equals("target"))
            .findFirst().get();
    
    this.xmiResource.getContents().get(0).eContents().forEach(content -> {
      if (content.eClass().getName().equals("State")) {
        String stateId = "x"+content.eGet(stateIdAttr).toString();
        boolean isInitial = content.eGet(isInitialAttr).toString().equals("true");
        boolean isFinal = content.eGet(isFinalAttr).toString().equals("true");
        ASTState state = AutomataMill.stateBuilder().setName(stateId).setInitial(isInitial).setFinal(isFinal).build();
        automaton.addState(state);
      }
      else if (content.eClass().getName().equals("Transition")) {
        String label = content.eGet(transitionLabelAttr).toString();
        String sourceId = "x"+((EObject)content.eGet(transitionSource)).eGet(stateIdAttr).toString();
        String targetId = "x"+((EObject)content.eGet(transitionTarget)).eGet(stateIdAttr).toString();
        ASTTransition transition = AutomataMill.transitionBuilder().setFrom(sourceId).setTo(targetId).setInput(label).build();
        automaton.addTransition(transition);
      }
    });

    TTCAutomataFullPrettyPrinter prettyPrinter = new TTCAutomataFullPrettyPrinter(new IndentPrinter());
    String prettyOut = prettyPrinter.prettyprint(automaton);
    outFile.getParent().toFile().mkdirs();
    try(FileWriter fw = new FileWriter(outFile.toFile())) {
      fw.write(prettyOut);
    }catch (IOException ex) {
      ex.printStackTrace();
    }
  }
}
