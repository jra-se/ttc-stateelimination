package tcc.stateelimination.converter;

import automata.AutomataMill;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class EmfToAUT {

  public static void main(String[] args) throws IOException {
    Path INPUT_RESOURCES = Path.of("models");
    Path OUTPUT_RESOURCES = Path.of("emf-to-automaton", "build", "testdata");
    Path metaModelPath = INPUT_RESOURCES.resolve("TransitionGraph.ecore");
    Path modelDir = INPUT_RESOURCES;
    List<Path> models = new ArrayList<>();
    try (Stream<Path> paths = Files.walk(modelDir)) {
      paths.filter(Files::isRegularFile).filter(x -> x.toString().endsWith(".xmi"))
              .forEach(models::add);
    }

    de.nexus.emfutils.EMFLoader loader = new de.nexus.emfutils.EMFLoader();
    EPackage metaMode = loader.loadResourceAsPackage(metaModelPath);
    if (metaMode == null) {
      throw new IllegalStateException("Could not load metamodel");
    }

    AutomataMill.init();

    for (Path model : models) {
      Resource xmiModel = loader.loadResource(model);
      Path relativeFilePath = modelDir.relativize(model);
      relativeFilePath = Path.of(relativeFilePath.toString().replace(".xmi", ".aut"));
      Path outFile = OUTPUT_RESOURCES.resolve(relativeFilePath);
      new AUTBuilder(metaMode, xmiModel, outFile);
    }
  }
}
