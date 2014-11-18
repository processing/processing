package processing.app.contrib;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import processing.app.Base;
import processing.core.PApplet;

public class ExamplesPackageContribution extends LocalContribution {

  private ArrayList<String> compatibleModesList;

  static public ExamplesPackageContribution load(File folder) {
    return new ExamplesPackageContribution(folder);
  }

  private ExamplesPackageContribution(File folder) {
    super(folder);
    compatibleModesList = parseCompatibleModesList(properties
      .get("compatibleModesList"));
  }

  private static ArrayList<String> parseCompatibleModesList(String unparsedModes) {
    ArrayList<String> modesList = new ArrayList<String>();
    if (unparsedModes == null || unparsedModes.isEmpty())
      return modesList;
    String[] splitStr = PApplet.trim(PApplet.split(unparsedModes, ','));//unparsedModes.split(",");
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
  public static boolean isExamplesPackageCompatible(Base base,
                                            File exampleLocationFolder) {
    File propertiesFile = new File(exampleLocationFolder,
                                   ContributionType.EXAMPLES_PACKAGE.toString()
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
    File examplesFolder = Base.getSketchbookExamplesPackagesFolder();
    ArrayList<ExamplesPackageContribution> contribExamples = base.getExampleContribs();

    HashMap<File, ExamplesPackageContribution> existing = new HashMap<File, ExamplesPackageContribution>();
    for (ExamplesPackageContribution contrib : contribExamples) {
      existing.put(contrib.getFolder(), contrib);
    }
    File[] potential = ContributionType.EXAMPLES_PACKAGE.listCandidates(examplesFolder);
    // If modesFolder does not exist or is inaccessible (folks might like to 
    // mess with folders then report it as a bug) 'potential' will be null.
    if (potential != null) {
      for (File folder : potential) {
        if (!existing.containsKey(folder)) {
          contribExamples.add(new ExamplesPackageContribution(folder));
        }
      }
    }
  }

  @Override
  public ContributionType getType() {
    return ContributionType.EXAMPLES_PACKAGE;
  }

  public ArrayList<String> getCompatibleModesList() {
    return compatibleModesList;
  }

}
