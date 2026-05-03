package de.monticore.tf;

import de.monticore.ast.ASTNode;
import de.monticore.generating.templateengine.GlobalExtensionManagement;

public class AUTUniformFinal extends AUTUniformFinalTOP {
  public AUTUniformFinal(GlobalExtensionManagement glex, ASTNode astNode) {
    super(glex, astNode);
  }

  @Override
  protected void setupReporting() {
    // Disable reporting (performance issues in 7.9.0-SNAPSHOT
  }
}
