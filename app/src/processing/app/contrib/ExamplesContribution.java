package processing.app.contrib;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import processing.app.Base;
import processing.app.Mode;
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


  static public boolean isCompatible(Base base, StringDict props) {
    return isCompatible(base.getActiveEditor().getMode(), props);
  }


  /**
   * Function to determine whether or not the example present in the
   * exampleLocation directory is compatible with the current mode.
   * @return true if compatible with the Mode of the currently active editor
   */
  static public boolean isCompatible(Mode mode, StringDict props) {
    String currentIdentifier = mode.getIdentifier();
    StringList compatibleList = parseModeList(props);
    if (compatibleList.size() == 0) {
      if (mode.requireExampleCompatibility()) {
        // for p5js (and maybe Python), examples must specify that they work
        return false;
      }
      // if no Mode specified, assume compatible everywhere
      return true;
    }
    return compatibleList.hasValue(currentIdentifier);
  }


  static public boolean isCompatible(Base base, File exampleFolder) {
    StringDict props = loadProperties(exampleFolder, EXAMPLES);
    if (props != null) {
      return isCompatible(base, props);
    }
    // Require a proper .properties file to show up
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
