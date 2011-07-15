package processing.app;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import processing.app.Contribution.ContributionInfo.Author;

public abstract class Contribution {
  
  abstract ContributionInfo getInfo();
  
  abstract File getFolder();
  
  public static void readProperties(HashMap<String, String> propTable,
                                    ContributionInfo info) {
    
    info.category = "Unknown";
    
    info.name = propTable.get("name");
    
    String authors = propTable.get("authorList");
    info.authorList = new ArrayList<Author>();
    if (authors != null) {
      String[] authorNames = authors.split(";");
      for (String authorName : authorNames) {
        Author author = new Author();
        author.name = authorName.trim(); 
        
        info.authorList.add(author);
      }
    }
    
    info.url = propTable.get("url");
    info.sentence = propTable.get("sentence");
    info.paragraph = propTable.get("paragraph");
    
    try {
      info.version = Integer.parseInt(propTable.get("version"));
    } catch (NumberFormatException e) {
    }
    info.prettyVersion = propTable.get("prettyVersion");
    
  }
  
  public static abstract class ContributionInfo implements Comparable<ContributionInfo> {
    
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

    public abstract boolean isInstalled();

    /**
     * @return the contribution associated with this data, or null if it is not
     *         installed
     */
    public abstract Contribution getContribution();
    
  }
  
}
