package processing.app.contrib;

import java.io.File;

/**
 * A local contribution that comes with references.
 */
public abstract class LocalContribWithReference extends LocalContribution {
  
  protected File referenceFile;   // shortname/reference/index.html is one possible path

  public LocalContribWithReference(File folder) {
    super(folder);
    referenceFile = loadReferenceIndexFile(folder);
  }
  
  /**
   * @param folder
   *          The file object representing the base folder of the contribution
   * @return Returns a file object representing the index file of the reference
   */
  protected File loadReferenceIndexFile(File folder) {
    final String potentialFileList[] = {
      "reference/index.html", "reference/index.htm",
      "documentation/index.html", "documentation/index.htm", "docs/index.html",
      "docs/index.htm", "documentation.html", "documentation.htm",
      "reference.html", "reference.htm", "docs.html", "docs.htm", "readme.txt" };

    int i = 0;
    File potentialRef = new File(folder, potentialFileList[i]);
    while (!potentialRef.exists() && ++i < potentialFileList.length) {
      potentialRef = new File(folder, potentialFileList[i]);
    }
    return potentialRef;
  }

  /**
   * Returns the object stored in the referenceFile field, which contains an
   * instance of the file object representing the index file of the reference
   * 
   * @return referenceFile
   */
  public File getReferenceIndexFile() {
    return referenceFile;
  }

  /**
   * Tests whether the reference's index file indicated by referenceFile exists.
   * 
   * @return true if and only if the file denoted by referenceFile exists; false
   *         otherwise.
   */
  public boolean hasReference() {
    return referenceFile.exists();
  }
}
