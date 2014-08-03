package processing.app.contrib;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import processing.app.Base;

public class ExampleContribution extends LocalContribution {

  private ArrayList<String> compatibleModesList;

  static public ExampleContribution load(File folder) {
    return new ExampleContribution(folder);
  }

  private ExampleContribution(File folder) {
    super(folder);
    compatibleModesList = parseCompatibleModesList(properties
      .get("compatibleModesList"));
  }

  private static ArrayList<String> parseCompatibleModesList(String unparsedModes) {
    ArrayList<String> modesList = new ArrayList<String>();
    if (unparsedModes == null || unparsedModes.isEmpty())
      return modesList;
    String[] splitStr = unparsedModes.split(",");
    for (String mode : splitStr)
      modesList.add(mode.trim());
    return modesList;
  }

  /**
   * Function to determine whether or not the example present in the
   * exampleLocation directory is compatible with the present mode.
   * 
   * @param base
   * @param exampleLocationFolder
   * @return true if the example is compatible with the mode of the currently
   *         active editor
   */
  public static boolean isExampleCompatible(Base base,
                                            File exampleLocationFolder) {
    File propertiesFile = new File(exampleLocationFolder,
                                   ContributionType.EXAMPLE.toString()
                                     + ".properties");
    if (propertiesFile.exists()) {
      ArrayList<String> compModesList = parseCompatibleModesList(Base
        .readSettings(propertiesFile).get("compatibleModesList"));
      for (String c : compModesList) {
        if (c.equalsIgnoreCase(base.getActiveEditor().getMode().getIdentifier())) {
          return true;
        }
      }
    }
    return false;
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

  public ArrayList<String> getCompatibleModesList() {
    return compatibleModesList;
  }

}
