package de.monticore.tf;

import de.monticore.ast.ASTNode;
import de.monticore.generating.templateengine.GlobalExtensionManagement;

public class AUTCreateFinalDummyIfNeeded extends AUTCreateFinalDummyIfNeededTOP {

  public AUTCreateFinalDummyIfNeeded(GlobalExtensionManagement glex, ASTNode astNode) {
    super(glex, astNode);
  }

  @Override
  protected void setupReporting() {
    // Disable reporting (performance issues in 7.9.0-SNAPSHOT
  }

}
