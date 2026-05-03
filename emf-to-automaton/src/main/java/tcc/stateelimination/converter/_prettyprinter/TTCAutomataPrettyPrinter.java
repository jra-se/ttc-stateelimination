package tcc.stateelimination.converter._prettyprinter;

import automata._ast.ASTState;
import automata._ast.ASTTransition;
import automata._prettyprint.AutomataPrettyPrinter;
import de.monticore.prettyprint.IndentPrinter;

public class TTCAutomataPrettyPrinter extends AutomataPrettyPrinter {
  
  public TTCAutomataPrettyPrinter(IndentPrinter printer, boolean printComments) {
    super(printer, printComments);
  }
  
  public void handle(automata._ast.ASTAutomaton node) {
    if (this.isPrintComments()) {
      de.monticore.prettyprint.CommentPrettyPrinter.printPreComments(node, getPrinter());
    }
    
    getPrinter().print("automaton ");
    
    getPrinter().print(node.getName() + " ");
    
    getPrinter().println("{ ");
    getPrinter().indent();
    
    for (ASTState state : node.getStateList()) {
      state.accept(getTraverser());
    }
    
    for (ASTTransition transition : node.getTransitionList()) {
      transition.accept(getTraverser());
    }
    
    getPrinter().unindent();
    getPrinter().println();
    getPrinter().println("} ");
    
    if (this.isPrintComments()) {
      de.monticore.prettyprint.CommentPrettyPrinter.printPostComments(node, getPrinter());
    }
    
  }
  
  public void handle(automata._ast.ASTState node) {
    if (this.isPrintComments()) {
      de.monticore.prettyprint.CommentPrettyPrinter.printPreComments(node, getPrinter());
    }
    
    getPrinter().print("state ");
    
    getPrinter().print(node.getName() + " ");
    if (node.isInitial()) {
      getPrinter().stripTrailing();
      getPrinter().print("<<");
      getPrinter().print("initial ");
      getPrinter().stripTrailing();
      getPrinter().print(">>");
    }
    if (node.isFinal()) {
      getPrinter().stripTrailing();
      getPrinter().print("<<");
      getPrinter().print("final ");
      getPrinter().stripTrailing();
      getPrinter().print(">>");
    }
    getPrinter().stripTrailing();
    getPrinter().println(";");
    
    if (this.isPrintComments()) {
      de.monticore.prettyprint.CommentPrettyPrinter.printPostComments(node, getPrinter());
    }
    
  }
}
