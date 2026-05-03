package ttc.stateelimination.shared;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class TestFramework {
  
  protected abstract String getModelBasePath();
  
  protected abstract String getPositiveDataBasePath();
  
  protected abstract String getNegativeDataBasePath();
  
  protected abstract String getModelSuffix();
  
  protected String getPositiveSuffix() {
    return "-positive.data";
  }
  
  protected String getNegativeSuffix() {
    return "-negative.data";
  }
  
  protected Path getModelPath(String modelName) {
    return Path.of(getModelBasePath(), modelName + getModelSuffix());
  }
  
  protected Path getPositiveDataPath(String modelName) {
    return Path.of(getPositiveDataBasePath(), modelName + getPositiveSuffix());
  }
  
  protected Path getNegativeDataPath(String modelName) {
    return Path.of(getNegativeDataBasePath(), modelName + getNegativeSuffix());
  }
  
  protected int getRegexSize(String regex) {
    int size = 0;
    
    for (int i = 0; i < regex.length(); i++) {
      if (regex.charAt(i) == 's') {
        size++;
      }
    }
    
    return size;
  }
  
  protected String formatRegex(String regex) {
    regex = regex.replace('+', '|');  //java uses '|' as the or symbol
    regex = regex.replaceAll("\\[.*?\\]", "");  //remove probability
    regex = regex.replaceAll(":", "");  //':' is concatenation
    regex = regex.replaceAll("e", "");
    return regex;
  }
  
  protected TestFrameworkWordResult testWords(String regex, Path acceptedWordsFile, boolean accept) {
    int totalWords = 0;
    int passed = 0;
    String word = "";
    BufferedReader reader;
    
    assertTrue(Files.exists(acceptedWordsFile), "File " + acceptedWordsFile + " does not exist");
    
    regex = formatRegex(regex);
    
    Pattern pattern = Pattern.compile(regex);
    try {
      reader = new BufferedReader(new FileReader(acceptedWordsFile.toFile()));
      while ((word = reader.readLine()) != null) {
        totalWords++;
        if (pattern.matcher(word).matches() == accept) {
          passed++;
        }
      }
      reader.close();
    }
    catch (Exception ignored) {
    
    }
    return new TestFrameworkWordResult(passed, totalWords);
  }
}
