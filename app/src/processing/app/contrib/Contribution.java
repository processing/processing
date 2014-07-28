/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013 The Processing Foundation
  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along 
  with this program; if not, write to the Free Software Foundation, Inc.
  59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package processing.app.contrib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import processing.app.Language;
import processing.app.Base;
import processing.core.PApplet;


abstract public class Contribution {
  static final List validCategories = 
    Arrays.asList("3D", "Animation", "Data", "Geometry", "GUI", "Hardware", 
                  "I/O", "Math", "Simulation", "Sound", "Typography", 
                  "Utilities", "Video & Vision", "Other");

  //protected String category;      // "Sound"
  protected List<String> categories;  // "Sound", "Typography"
  protected String name;          // "pdf" or "PDF Export"
  protected String authorList;    // Ben Fry
  protected String url;           // http://processing.org
  protected String sentence;      // Write graphics to PDF files.
  protected String paragraph;     // <paragraph length description for site>
  protected int version;          // 102
  protected String prettyVersion; // "1.0.2"
  protected long lastUpdated;     //  1402805757
  protected TreeMap<Integer, Integer> compatibleVersions; // 216,220,226-229
  
  
  // "Sound"
//  public String getCategory() {
//    return category;
//  }

  
  // "Sound", "Utilities"... see valid list in ContributionListing
  protected List<String> getCategories() {
    return categories;
  }
  
  
  protected String getCategoryStr() {
    StringBuilder sb = new StringBuilder();
    for (String category : categories) {
      sb.append(category);
      sb.append(',');
    }
    sb.deleteCharAt(sb.length()-1);  // delete last comma
    return sb.toString();
  }
  
  
  protected boolean hasCategory(String category) {
    if (category != null) {
      for (String c : categories) {
        if (category.equalsIgnoreCase(c)) {
          return true;
        }
      }
    }
    return false;
  }


  // "pdf" or "PDF Export"
  public String getName() {
    return name;
  }


  // "[Ben Fry](http://benfry.com/)"
  public String getAuthorList() {
    return authorList;
  }


  // "http://processing.org"
  public String getUrl() {
    return url;
  }


  // "Write graphics to PDF files."
  public String getSentence() {
    return sentence;
  }


  // <paragraph length description for site>
  public String getParagraph() {
    return paragraph;
  }


  // 102
  public int getVersion() {
    return version;
  }


  // "1.0.2"
  public String getPrettyVersion() {
    return prettyVersion;
  }
  
  // 1402805757
  public long getLastUpdated() {
    return lastUpdated;
  }


  public String getCompatibleVersionsStr() {
    if (compatibleVersions == null)
      return "";
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<Integer, Integer> range : compatibleVersions.entrySet()) {
      if (range.getKey().equals(range.getValue())) {
        sb.append(range.getKey());
        sb.append(",");
      }
      else {
        sb.append(range.getKey());
        sb.append("-");
        sb.append(range.getValue());
        sb.append(",");
      }
    }
    sb.deleteCharAt(sb.length()-1);  // delete last comma
    return sb.toString();
  }


  public TreeMap<Integer, Integer> getCompatibleVersions() {
    return compatibleVersions;
  }


  public boolean isCompatible(int versionNum) {
    if (compatibleVersions != null) {
      if (compatibleVersions.floorEntry(versionNum) != null
        && versionNum <= compatibleVersions.floorEntry(versionNum).getValue()) {
        return true; }
    }
    return false;
  }


  abstract public ContributionType getType();
  
  
  public String getTypeName() {
    return getType().toString();
  }
  
  
  abstract public boolean isInstalled();
  
  
//  /** 
//   * Returns true if the type of contribution requires the PDE to restart
//   * when being added or removed. 
//   */
//  public boolean requiresRestart() {
//    return getType() == ContributionType.TOOL || getType() == ContributionType.MODE;
//  }
  

  boolean isRestartFlagged() {
    return false;
  }
  
  
  /** Overridden by LocalContribution. */
  boolean isDeletionFlagged() {
    return false;
  }

  
  boolean isUpdateFlagged() {
    return false;
  }


  /** 
   * @return a single element list with "Unknown" as the category.
   */
  static List<String> defaultCategory() {
    List<String> outgoing = new ArrayList<String>();
    outgoing.add("Unknown");
    return outgoing;
  }
  
  
  /**
   * @return the list of categories that this contribution is part of
   *         (e.g. "Typography / Geometry"). "Unknown" if the category null.
   */
  static List<String> parseCategories(String categoryStr) {
    List<String> outgoing = new ArrayList<String>();
    
    if (categoryStr != null) {
      String[] listing = PApplet.trim(PApplet.split(categoryStr, ','));
      for (String category : listing) {
        if (validCategories.contains(category)) {
          outgoing.add(category);
        }
      }
    }
    if (outgoing.size() == 0) {
      return defaultCategory();
    }
    return outgoing;
  }


  /**
   * @param compVerStr
   *          </br>A string consisting of a comma separated list of numbers.
   *          Ranges may be indicated by hyphens between 2 numbers. Open ranges
   *          may be indicated by leaving the right side of the last hyphen
   *          blank. 
   *          </br>&emsp;&emsp;&emsp;For example, "222,225,227-229,230-"
   *          is valid.
   * @return A TreeMap consisting of integer-integer key-value pairs that
   *         represent ranges for which the contribution has been
   *         tested.</br>&emsp;&emsp;&emsp;The example above would return a
   *         TreeMap with the <key,value> pairs 
   *         <222,222>, <225,225>, <227,229>, 
   *         </br>&emsp;&emsp;&emsp;<230,(present_release_number)>.
   */
  static TreeMap<Integer, Integer> parseCompatibleVersions(String compVerStr)
    throws NumberFormatException {
    if (compVerStr == null || compVerStr.equals(""))
      return null;
    String[] ranges = compVerStr.split(",");
    TreeMap<Integer, Integer> compatibleTM = new TreeMap<Integer, Integer>();
    for (String range : ranges) {
      range = range.trim();
      if (range.indexOf("-") != -1) {
        int key = Integer.parseInt(range.substring(0, range.indexOf("-"))
          .trim());
        int value;
        if (((range.indexOf("-") + 1) >= range.length())
          || range.substring(range.indexOf("-") + 1).trim().isEmpty()) {
          value = Base.getRevision();
        }
        else
          value = Integer.parseInt(range.substring(range.indexOf("-") + 1)
            .trim());
        compatibleTM.put(key, value);
      } else
        compatibleTM.put(Integer.parseInt(range), Integer.parseInt(range));
    }
    return compatibleTM;
  }
}
