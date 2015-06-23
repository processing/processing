package processing.app.contrib;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import processing.app.Base;
import processing.core.PApplet;
import processing.data.StringDict;
import processing.data.StringList;
import static processing.app.contrib.ContributionType.EXAMPLES;


public class ExamplesContribution extends LocalContribution {
  private StringList modeList;


  static public ExamplesContribution load(File folder) {
    return new ExamplesContribution(folder);
  }


  private ExamplesContribution(File folder) {
    super(folder);

    if (properties != null) {
      modeList = parseModeList(properties);
    }
  }


  static private StringList parseModeList(StringDict properties) {
    String unparsedModes = properties.get(MODES_PROPERTY);
    StringList outgoing = new StringList();
    if (unparsedModes != null) {
      outgoing.append(PApplet.trim(PApplet.split(unparsedModes, ',')));
    }
    return outgoing;
  }


  /**
   * Function to determine whether or not the example present in the
   * exampleLocation directory is compatible with the current mode.
   *
   * @param base
   * @param exampleFolder
   * @return true if the example is compatible with the mode of the currently
   *         active editor
   */
  static public boolean isCompatible(Base base, File exampleFolder) {
    String currentIdentifier = base.getActiveEditor().getMode().getIdentifier();
    File propertiesFile =
      new File(exampleFolder, EXAMPLES.getPropertiesName());
    if (propertiesFile.exists()) {
      StringList compatibleList =
        parseModeList(Base.readSettings(propertiesFile));
      if (compatibleList.size() == 0) {
        return true;  // if no mode specified, just include everywhere
      }
      for (String c : compatibleList) {
        if (c.equals(currentIdentifier)) {
          return true;
        }
      }
    }
    return false;
  }


  static public void loadMissing(Base base) {
    File examplesFolder = Base.getSketchbookExamplesFolder();
    List<ExamplesContribution> contribExamples = base.getExampleContribs();

    Map<File, ExamplesContribution> existing = new HashMap<File, ExamplesContribution>();
    for (ExamplesContribution contrib : contribExamples) {
      existing.put(contrib.getFolder(), contrib);
    }
    File[] potential = EXAMPLES.listCandidates(examplesFolder);
    // If modesFolder does not exist or is inaccessible (folks might like to
    // mess with folders then report it as a bug) 'potential' will be null.
    if (potential != null) {
      for (File folder : potential) {
        if (!existing.containsKey(folder)) {
          contribExamples.add(new ExamplesContribution(folder));
        }
      }
    }
  }


  @Override
  public ContributionType getType() {
    return EXAMPLES;
  }


  public StringList getModeList() {
    return modeList;
  }
}
