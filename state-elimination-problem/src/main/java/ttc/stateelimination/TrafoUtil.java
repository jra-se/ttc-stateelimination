package ttc.stateelimination;

import de.monticore.tf.runtime.ODRule;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TrafoUtil {
  
  public static <T extends ODRule> boolean executeTrafo(T rule) {
    if (rule.doPatternMatching()) {
      rule.doReplacement();
      return true;
    }
    return false;
  }

  public static <T extends ODRule> Optional<T> executeTrafoOpt(T rule) {
    if (rule.doPatternMatching()) {
      rule.doReplacement();
      return Optional.of(rule);
    }
    return Optional.empty();
  }
  
  public static <T extends ODRule> void executeTrafoRepeated(Supplier<T> rule,
      BiConsumer<T, Boolean> callback) {
    boolean continueSearch = true;
    while (continueSearch) {
      T trafo = rule.get();
      boolean result = executeTrafo(trafo);
      continueSearch = result;
      callback.accept(trafo, result);
    }
  }
  
  public static <T extends ODRule> void executeTrafoRepeated(Supplier<T> rule,
      Consumer<T> successCallback, Consumer<T> failureCallback) {
    boolean continueSearch = true;
    while (continueSearch) {
      T trafo = rule.get();
      boolean result = executeTrafo(trafo);
      continueSearch = result;
      if (result) {
        successCallback.accept(trafo);
      }
      else {
        failureCallback.accept(trafo);
      }
    }
  }
}
