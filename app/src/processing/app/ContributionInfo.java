package processing.app;

import java.util.List;

public abstract class ContributionInfo implements Comparable<ContributionInfo> {
  
  protected String category;         // "Sound"
  protected String name;             // "pdf" or "PDF Export"
  protected List<Author> authorList; // Ben Fry
  protected String url;              // http://processing.org
  protected String sentence;         // Write graphics to PDF files.
  protected String paragraph;        // <paragraph length description for site>
  protected int version;             // 102
  protected String prettyVersion;    // "1.0.2"
  
  protected String link = "";
  
  public static class Author {
    public String name;
    
    public String url;
    
  }
  
  public int compareTo(ContributionInfo o) {
    return name.toLowerCase().compareTo(o.name.toLowerCase());
  }
  
  public abstract ContributionType getType();
  
  public static enum ContributionType {
    LIBRARY, LIBRARY_COMPILATION, TOOL, MODE;
  }
}
