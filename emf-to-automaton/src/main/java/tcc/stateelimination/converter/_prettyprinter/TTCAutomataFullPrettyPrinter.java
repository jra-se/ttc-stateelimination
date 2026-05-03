package tcc.stateelimination.converter._prettyprinter;

import automata._prettyprint.AutomataFullPrettyPrinter;
import de.monticore.prettyprint.IndentPrinter;

public class TTCAutomataFullPrettyPrinter extends AutomataFullPrettyPrinter {
  
  public TTCAutomataFullPrettyPrinter(IndentPrinter printer) {
    super(printer);
  }
  
  @Override
  protected void initializeTraverser(boolean printComments) {
    TTCAutomataPrettyPrinter automata = new TTCAutomataPrettyPrinter(getPrinter(), printComments);
    getTraverser().setAutomataHandler(automata);
    getTraverser().add4Automata(automata);
    
    de.monticore.mcbasics._prettyprint.MCBasicsPrettyPrinter mCBasics =
        new de.monticore.mcbasics._prettyprint.MCBasicsPrettyPrinter(getPrinter(), printComments);
    getTraverser().setMCBasicsHandler(mCBasics);
    getTraverser().add4MCBasics(mCBasics);
  }
}
