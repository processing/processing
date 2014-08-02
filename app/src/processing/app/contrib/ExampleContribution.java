package processing.app.contrib;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import processing.app.Base;

public class ExampleContribution extends LocalContribution {

  static public ExampleContribution load(File folder) {
    return new ExampleContribution(folder);
  }

  private ExampleContribution(File folder) {
    super(folder);
  }


  static public void loadMissing(Base base) {
    File examplesFolder = Base.getSketchbookExamplesFolder();
    ArrayList<ExampleContribution> contribExamples = base.getExampleContribs();

    HashMap<File, ExampleContribution> existing = new HashMap<File, ExampleContribution>();
    for (ExampleContribution contrib : contribExamples) {
      existing.put(contrib.getFolder(), contrib);
    }
    File[] potential = ContributionType.EXAMPLE.listCandidates(examplesFolder);
    // If modesFolder does not exist or is inaccessible (folks might like to 
    // mess with folders then report it as a bug) 'potential' will be null.
    if (potential != null) {
      for (File folder : potential) {
        if (!existing.containsKey(folder)) {
          contribExamples.add(new ExampleContribution(folder));
        }
      }
    }
  }


  @Override
  public ContributionType getType() {
    return ContributionType.EXAMPLE;
  }

}
