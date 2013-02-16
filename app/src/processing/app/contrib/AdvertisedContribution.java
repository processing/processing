package processing.app.contrib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import processing.app.Base;
import processing.app.Editor;
import processing.core.PApplet;

class AdvertisedContribution implements Contribution {
  protected final String name;             // "pdf" or "PDF Export"
  protected final ContributionType type;   // Library, tool, etc.
  protected final String category;         // "Sound"
  protected final String authorList;       // [Ben Fry](http://benfry.com/)
  protected final String url;              // http://processing.org
  protected final String sentence;         // Write graphics to PDF files.
  protected final String paragraph;        // <paragraph length description for site>
  protected final int version;             // 102
  protected final String prettyVersion;    // "1.0.2"
  protected final String link;             // Direct link to download the file

  
  public AdvertisedContribution(ContributionType type, HashMap<String, String> exports) {

    this.type = type;
    name = exports.get("name");
    category = ContributionListing.getCategory(exports.get("category"));
    authorList = exports.get("authorList");

    url = exports.get("url");
    sentence = exports.get("sentence");
    paragraph = exports.get("paragraph");

    int v = 0;
    try {
      v = Integer.parseInt(exports.get("version"));
    } catch (NumberFormatException e) {
    }
    version = v;

    prettyVersion = exports.get("prettyVersion");

    this.link = exports.get("download");
  }
  
  
  /**
   * @param contribArchive
   *          a zip file containing the library to install
   * @param ad
   *          the advertised version of this library, if it was downloaded
   *          through the Contribution Manager. This is used to check the type
   *          of library being installed, and to replace the .properties file in
   *          the zip
   * @param confirmReplace
   *          true to open a dialog asking the user to confirm removing/moving
   *          the library when a library by the same name already exists
   * @return
   */
  public InstalledContribution install(Editor editor, File contribArchive,
                                       boolean confirmReplace,
                                       ErrorWidget statusBar) {
    
    // Unzip the file into the modes, tools, or libraries folder inside the 
    // sketchbook. Unzipping to /tmp is problematic because it may be on 
    // another file system, so move/rename operations will break.
    File sketchbookContribFolder = type.getSketchbookContribFolder();
    File tempFolder = null; 
    
    try {
      tempFolder = 
        Base.createTempFolder(type.toString(), "tmp", sketchbookContribFolder);
    } catch (IOException e) {
      statusBar.setErrorMessage("Could not create a temporary folder to install.");
      return null;
    }
    ContributionManager.unzip(contribArchive, tempFolder);

    // Now go looking for a legit contrib inside what's been unpacked.
    File contribFolder = null;
    
    // Sometimes contrib authors place all their folders in the base directory 
    // of the .zip file instead of in single folder as the guidelines suggest. 
    if (InstalledContribution.isCandidate(tempFolder, type)) {
      contribFolder = tempFolder;
    }

    if (contribFolder == null) {
      // Find the first legitimate looking folder in what we just unzipped
      contribFolder = InstalledContribution.findCandidate(tempFolder, type);
    }
    
    InstalledContribution installedContrib = null;

    if (contribFolder == null) {
      statusBar.setErrorMessage("Could not find a " + type + " in the downloaded file.");
      
    } else {
      File propFile = new File(contribFolder, type + ".properties");

      if (!writePropertiesFile(propFile)) {        
        // 1. contribFolder now has a legit contribution, load it to get info. 
        InstalledContribution newContrib =
          ContributionManager.load(editor.getBase(), contribFolder, type);
        
        // 2. Check to make sure nothing has the same name already, 
        // backup old if needed, then move things into place and reload.
        installedContrib = 
          newContrib.moveAndLoad(editor, confirmReplace, statusBar);
        
      } else {
        statusBar.setErrorMessage("Error overwriting .properties file.");
      }
    }

    // Remove any remaining boogers
    if (tempFolder.exists()) {
      Base.removeDir(tempFolder);
    }
    return installedContrib;
  }

  
  public boolean isInstalled() {
    return false;
  }

  public ContributionType getType() {
    return type;
  }

  public String getTypeName() {
    return type.toString();
  }
  
  public String getCategory() {
    return category;
  }

  public String getName() {
    return name;
  }

  public String getAuthorList() {
    return authorList;
  }

  public String getUrl() {
    return url;
  }

  public String getSentence() {
    return sentence;
  }

  public String getParagraph() {
    return paragraph;
  }

  public int getVersion() {
    return version;
  }

  public String getPrettyVersion() {
    return prettyVersion;
  }

  
  public boolean writePropertiesFile(File propFile) {
    try {
      if (propFile.delete() && propFile.createNewFile() && propFile.setWritable(true)) {
        //BufferedWriter bw = new BufferedWriter(new FileWriter(propFile));
        PrintWriter writer = PApplet.createWriter(propFile);

        writer.println("name=" + getName());
        writer.println("category=" + getCategory());
        writer.println("authorList=" + getAuthorList());
        writer.println("url=" + getUrl());
        writer.println("sentence=" + getSentence());
        writer.println("paragraph=" + getParagraph());
        writer.println("version=" + getVersion());
        writer.println("prettyVersion=" + getPrettyVersion());

        writer.flush();
        writer.close();
      }
      return true;

    } catch (FileNotFoundException e) {
      e.printStackTrace();

    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }
}